package dev.vepo.maestro.engine.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import dev.vepo.maestro.engine.expression.ExpressionEvaluator;
import dev.vepo.maestro.parser.model.PatternDefinition;
import dev.vepo.maestro.parser.model.PatternStage;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class PatternMatcherProcessor implements Processor<String, Object, String, Object> {
    public static final class PatternState {
        private final Map<String, Long> matches = new HashMap<>();
        private long lastTimestamp;

        void clear() {
            matches.clear();
            lastTimestamp = 0;
        }

        long lastTimestamp() {
            return lastTimestamp;
        }

        Map<String, Long> matches() {
            return matches;
        }

        void recordMatch(String name, long timestamp) {
            if ("_ts".equals(name)) {
                lastTimestamp = timestamp;
                return;
            }
            matches.put(name, timestamp);
            lastTimestamp = timestamp;
        }
    }

    public static final String STATE_STORE_NAME = "maestro-pattern-state";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static StoreBuilder<KeyValueStore<String, PatternState>> stateStoreBuilder() {
        return Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(STATE_STORE_NAME), org.apache.kafka.common.serialization.Serdes.String(),
                                           new PatternStateSerde());
    }

    private final PatternStage patternStage;

    private ProcessorContext<String, Object> context;

    private KeyValueStore<String, PatternState> stateStore;

    public PatternMatcherProcessor(PatternStage patternStage) {
        this.patternStage = patternStage;
    }

    private boolean canEvaluate(PatternDefinition definition, PatternState state, long now) {
        if (definition.afterPattern().isEmpty()) {
            return true;
        }
        var prior = definition.afterPattern().get();
        if (!state.matches().containsKey(prior)) {
            return false;
        }
        var priorTime = state.matches().get(prior);
        if (definition.within().isPresent()) {
            return now - priorTime <= definition.within().get().toMillis();
        }
        return true;
    }

    private void expireStaleMatches(PatternState state, long now) {
        var maxWithin = patternStage.definitions().stream()
                                    .flatMap(d -> d.within().stream())
                                    .mapToLong(d -> d.toMillis())
                                    .max()
                                    .orElse(Long.MAX_VALUE);
        if (!state.matches().isEmpty() && now - state.lastTimestamp() > maxWithin) {
            state.clear();
        }
    }

    @Override
    public void init(ProcessorContext<String, Object> context) {
        this.context = context;
        this.stateStore = context.getStateStore(STATE_STORE_NAME);
    }

    private boolean isSequenceComplete(PatternState state) {
        var ordered = patternStage.definitions().stream().map(PatternDefinition::name).toList();
        if (ordered.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ordered.size(); i++) {
            var name = ordered.get(i);
            if (!state.matches().containsKey(name)) {
                return false;
            }
            var definition = patternStage.definitions().get(i);
            if (definition.afterPattern().isPresent() && definition.within().isPresent()) {
                var prior = definition.afterPattern().get();
                if (!state.matches().containsKey(prior)) {
                    return false;
                }
                var delta = state.matches().get(name) - state.matches().get(prior);
                if (delta > definition.within().get().toMillis()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void process(Record<String, Object> record) {
        var key = record.key();
        var json = MAPPER.valueToTree(record.value());
        var now = context.currentSystemTimeMs();
        var state = Optional.ofNullable(stateStore.get(key)).orElseGet(PatternState::new);

        for (var definition : patternStage.definitions()) {
            if (!canEvaluate(definition, state, now)) {
                continue;
            }
            if (ExpressionEvaluator.matches(json, definition.expression())) {
                state.recordMatch(definition.name(), now);
            }
        }

        expireStaleMatches(state, now);
        if (isSequenceComplete(state)) {
            var detection = MAPPER.createObjectNode();
            detection.put("detected", patternStage.detectAlias());
            var matchesNode = MAPPER.createObjectNode();
            state.matches().forEach(matchesNode::put);
            detection.set("matches", matchesNode);
            context.forward(record.withValue(detection));
            state.clear();
        }
        stateStore.put(key, state);
    }
}
