package dev.vepo.maestro.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.SessionWindows;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.engine.aggregate.AggregateReducer;
import dev.vepo.maestro.engine.expression.ExpressionEvaluator;
import dev.vepo.maestro.engine.join.JoinKeyPair;
import dev.vepo.maestro.engine.pattern.PatternMatcherProcessor;
import dev.vepo.maestro.parser.model.AggregateStage;
import dev.vepo.maestro.parser.model.BranchCase;
import dev.vepo.maestro.parser.model.BranchStage;
import dev.vepo.maestro.parser.model.FilterStage;
import dev.vepo.maestro.parser.model.GroupByStage;
import dev.vepo.maestro.parser.model.JoinKind;
import dev.vepo.maestro.parser.model.JoinStage;
import dev.vepo.maestro.parser.model.MapStage;
import dev.vepo.maestro.parser.model.PatternStage;
import dev.vepo.maestro.parser.model.ProcessingStage;
import dev.vepo.maestro.parser.model.ProjectField;
import dev.vepo.maestro.parser.model.ProjectStage;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SessionizeStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.parser.model.ToStage;
import dev.vepo.maestro.parser.model.WindowStage;
import dev.vepo.maestro.parser.model.WindowType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class TopologyBuilder {
    private static final class JsonSerdeAdapter implements org.apache.kafka.common.serialization.Serde<Object> {
        @Override
        public org.apache.kafka.common.serialization.Deserializer<Object> deserializer() {
            return (topic, data) -> data == null ? null : MAPPER.readTree(data);
        }

        @Override
        public org.apache.kafka.common.serialization.Serializer<Object> serializer() {
            return (topic, data) -> MAPPER.writeValueAsBytes(data);
        }
    }

    private record WindowContext(WindowStage window, GroupByStage group, SessionizeStage session) {}

    private static final Logger logger = LoggerFactory.getLogger(TopologyBuilder.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static KStream<String, Object> aggregateSessionStream(
                                                                  org.apache.kafka.streams.kstream.SessionWindowedKStream<String, Object> windowed,
                                                                  AggregateStage aggregate) {
        var agg = windowed.aggregate(
                                     () -> (Object) MAPPER.createObjectNode(),
                                     (key, value, aggregateNode) -> AggregateReducer.merge((ObjectNode) aggregateNode, asJson(value), aggregate.functions()),
                                     (key, one, two) -> {
                                         var merged = MAPPER.createObjectNode();
                                         merged.setAll((ObjectNode) one);
                                         AggregateReducer.merge(merged, merged, aggregate.functions());
                                         return merged;
                                     },
                                     Materialized.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter()));
        return agg.toStream().map((windowedKey, value) -> {
            var finalized = AggregateReducer.finalizeAggregates((ObjectNode) value, aggregate.functions());
            return new org.apache.kafka.streams.KeyValue<>(windowedKey.key(), finalized);
        });
    }

    private static KStream<String, Object> aggregateTimeStream(
                                                               org.apache.kafka.streams.kstream.TimeWindowedKStream<String, Object> windowed,
                                                               AggregateStage aggregate) {
        var agg = windowed.aggregate(
                                     () -> (Object) MAPPER.createObjectNode(),
                                     (key, value, aggregateNode) -> AggregateReducer.merge((ObjectNode) aggregateNode, asJson(value), aggregate.functions()),
                                     Materialized.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter()));
        return agg.toStream().map((windowedKey, value) -> {
            var finalized = AggregateReducer.finalizeAggregates((ObjectNode) value, aggregate.functions());
            return new org.apache.kafka.streams.KeyValue<>(windowedKey.key(), finalized);
        });
    }

    private static KStream<String, Object> applyAggregate(
                                                          KStream<String, Object> stream,
                                                          WindowContext context,
                                                          AggregateStage aggregate,
                                                          StreamsBuilder builder) {
        GroupByStage group = context.group();
        if (group == null && context.session() != null) {
            group = new GroupByStage(context.session().fields());
        }
        if (group == null) {
            throw new IllegalStateException("GROUP BY is required before AGGREGATE");
        }
        final var groupBy = group;
        var grouped = stream.groupBy((key, value) -> groupKey(asJson(value), groupBy),
                                     Grouped.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter()));
        if (context.session() != null) {
            var session = context.session();
            var gap = Duration.ofMillis(session.gap().toMillis());
            var grace = session.timeout().map(t -> Duration.ofMillis(t.toMillis())).orElse(gap);
            return aggregateSessionStream(grouped.windowedBy(SessionWindows.ofInactivityGapAndGrace(gap, grace)), aggregate);
        }
        var window = context.window();
        if (window == null) {
            window = new WindowStage(WindowType.TUMBLING, new dev.vepo.maestro.parser.model.Duration(1, dev.vepo.maestro.parser.model.TimeUnit.MINUTES));
        }
        return aggregateTimeStream(grouped.windowedBy(toKafkaWindow(window)), aggregate);
    }

    private static void applyBranch(KStream<String, Object> stream, BranchStage branch, StreamsBuilder builder) {
        var priorConditions = new ArrayList<dev.vepo.maestro.parser.model.Expression>();
        for (var branchCase : branch.cases()) {
            KStream<String, Object> branchStream;
            if (branchCase.condition().isPresent()) {
                var condition = branchCase.condition().get();
                branchStream = stream.filter((key, value) -> ExpressionEvaluator.matches(asJson(value), condition));
                priorConditions.add(condition);
            } else {
                branchStream = stream.filter((key, value) -> priorConditions.stream().noneMatch(c -> ExpressionEvaluator.matches(asJson(value), c)));
            }
            var sinks = sinkTopics(branchCase.stages());
            var processing = processingStages(branchCase.stages());
            var result = processStages(branchStream, processing, builder);
            applySinks(result, sinks);
        }
    }

    private static KStream<String, Object> applyJoin(KStream<String, Object> stream, JoinStage join, StreamsBuilder builder) {
        var topic = join.sourceTopic().orElse(join.target());
        var keys = JoinKeyPair.fromCondition(join.condition());
        if (join.kind() == JoinKind.LOOKUP_TABLE) {
            var table = builder.<String, Object>globalTable(topic);
            return stream.join(table,
                               (key, value) -> joinKey(asJson(value), keys.leftField()),
                               (left, right) -> mergeJoin(left, right, join.target()),
                               Named.as("join-lookup-" + join.target()));
        }
        if (join.kind() == JoinKind.STREAM) {
            var other = builder.<String, Object>stream(topic);
            var leftStream = stream.selectKey((key, value) -> joinKey(asJson(value), keys.leftField()));
            var rightStream = other.selectKey((key, value) -> joinKey(asJson(value), keys.rightField()));
            return leftStream.join(rightStream,
                                   (left, right) -> mergeJoin(left, right, join.target()),
                                   JoinWindows.ofTimeDifferenceAndGrace(Duration.ofMinutes(5), Duration.ZERO),
                                   StreamJoined.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter(), new JsonSerdeAdapter())
                                               .withName("join-stream-" + join.target()));
        }
        var within = join.within().orElseThrow(() -> new IllegalStateException("WITHIN duration required for stream-stream join"));
        var other = builder.<String, Object>stream(topic);
        var joinWindows = JoinWindows.ofTimeDifferenceAndGrace(Duration.ofMillis(within.toMillis()), Duration.ZERO);
        var leftStream = stream.selectKey((key, value) -> joinKey(asJson(value), keys.leftField()));
        var rightStream = other.selectKey((key, value) -> joinKey(asJson(value), keys.rightField()));
        return leftStream.join(rightStream,
                               (left, right) -> mergeJoin(left, right, join.target()),
                               joinWindows,
                               StreamJoined.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter(), new JsonSerdeAdapter())
                                           .withName("join-within-" + join.target()));
    }

    private static KStream<String, Object> applyPattern(KStream<String, Object> stream, PatternStage pattern) {
        return stream.process(() -> new PatternMatcherProcessor(pattern), Named.as("pattern-" + pattern.detectAlias()),
                              PatternMatcherProcessor.STATE_STORE_NAME);
    }

    private static void applySinks(KStream<String, Object> stream, List<String> sinks) {
        for (var sink : sinks) {
            stream.to(sink);
        }
    }

    private static JsonNode asJson(Object value) {
        if (value instanceof JsonNode node) {
            return node;
        }
        if (value instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return MAPPER.valueToTree(value);
    }

    public static void build(StreamModel model, StreamsBuilder builder) {
        builder.addStateStore(PatternMatcherProcessor.stateStoreBuilder());
        for (var query : model.queries()) {
            buildQuery(query, builder);
        }
    }

    private static void buildQuery(Query query, StreamsBuilder builder) {
        var sourceTopic = query.sourcePipeline().sourceStage().topics().getFirst();
        KStream<String, Object> stream = builder.stream(sourceTopic);

        var where = query.sourcePipeline().sourceStage().whereClause();
        if (where.isPresent()) {
            stream = stream.filter((key, value) -> ExpressionEvaluator.matches(asJson(value), where.get()));
        }

        var result = processStages(stream, query.sourcePipeline().processingStages(), builder);
        applySinks(result, query.sinkTopics());
        logger.info("Wired query {} -> {}", sourceTopic, query.sinkTopics());
    }

    private static String groupKey(JsonNode value, GroupByStage group) {
        if (group == null || group.fields().isEmpty()) {
            return "all";
        }
        var parts = new ArrayList<String>();
        for (var field : group.fields()) {
            parts.add(ExpressionEvaluator.fieldValue(value, field).asString());
        }
        return String.join("|", parts);
    }

    private static String joinKey(JsonNode value, String field) {
        var simpleField = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;
        return ExpressionEvaluator.fieldValue(value, simpleField).asText();
    }

    private static Object mergeJoin(Object left, Object right, String targetAlias) {
        var merged = MAPPER.createObjectNode();
        if (left instanceof ObjectNode leftNode) {
            merged.setAll(leftNode);
        } else if (left instanceof JsonNode leftJson && leftJson.isObject()) {
            leftJson.properties().forEach(e -> merged.set(e.getKey(), e.getValue()));
        }
        if (right instanceof JsonNode rightNode) {
            merged.set(targetAlias, rightNode);
        }
        return merged;
    }

    private static List<ProcessingStage> processingStages(List<ProcessingStage> stages) {
        return stages.stream().filter(s -> !(s instanceof ToStage)).toList();
    }

    private static KStream<String, Object> processStages(
                                                         KStream<String, Object> stream,
                                                         List<ProcessingStage> stages,
                                                         StreamsBuilder builder) {
        WindowStage pendingWindow = null;
        GroupByStage pendingGroup = null;
        SessionizeStage pendingSession = null;

        for (var stage : stages) {
            if (stage instanceof FilterStage filter) {
                stream = stream.filter((key, value) -> ExpressionEvaluator.matches(asJson(value), filter.condition()));
            } else if (stage instanceof ProjectStage project) {
                stream = stream.mapValues(value -> projectRecord(asJson(value), project));
            } else if (stage instanceof MapStage map) {
                stream = stream.mapValues(value -> ExpressionEvaluator.applyAssignments(asJson(value),
                                                                                        map.assignments()
                                                                                           .toArray(dev.vepo.maestro.parser.model.Assignment[]::new)));
            } else if (stage instanceof WindowStage window) {
                if (pendingWindow != null || pendingSession != null) {
                    throw new IllegalStateException("Nested window definitions are not supported");
                }
                pendingWindow = window;
            } else if (stage instanceof GroupByStage group) {
                pendingGroup = group;
            } else if (stage instanceof SessionizeStage session) {
                pendingSession = session;
                pendingWindow = null;
            } else if (stage instanceof AggregateStage aggregate) {
                stream = applyAggregate(stream, new WindowContext(pendingWindow, pendingGroup, pendingSession), aggregate, builder);
                pendingWindow = null;
                pendingGroup = null;
                pendingSession = null;
            } else if (stage instanceof JoinStage join) {
                stream = applyJoin(stream, join, builder);
            } else if (stage instanceof BranchStage branch) {
                applyBranch(stream, branch, builder);
                return stream;
            } else if (stage instanceof PatternStage pattern) {
                stream = applyPattern(stream, pattern);
            } else if (!(stage instanceof ToStage)) {
                logger.warn("Skipping unsupported stage: {}", stage.getClass().getSimpleName());
            }
        }

        if (pendingWindow != null || pendingGroup != null || pendingSession != null) {
            throw new IllegalStateException("WINDOW, GROUP BY, or SESSIONIZE must be followed by AGGREGATE");
        }
        return stream;
    }

    private static Object projectRecord(JsonNode record, ProjectStage project) {
        if (project.fields().stream().allMatch(f -> f.expression().isEmpty())) {
            var names = project.fields().stream().map(ProjectField::name).toList();
            return ExpressionEvaluator.project(record, names);
        }
        var result = MAPPER.createObjectNode();
        for (var field : project.fields()) {
            if (field.expression().isPresent()) {
                result.set(field.name(), ExpressionEvaluator.evaluate(record, field.expression().get()));
            } else {
                result.set(field.name(), ExpressionEvaluator.fieldValue(record, field.name()));
            }
        }
        return result;
    }

    private static List<String> sinkTopics(List<ProcessingStage> stages) {
        return stages.stream()
                     .filter(ToStage.class::isInstance)
                     .flatMap(s -> ((ToStage) s).topics().stream())
                     .toList();
    }

    private static TimeWindows toKafkaWindow(WindowStage window) {
        var size = Duration.ofMillis(window.windowSize().toMillis());
        if (window.windowType() == WindowType.HOPPING && window.slideInterval().isPresent()) {
            return TimeWindows.ofSizeAndGrace(size, size).advanceBy(Duration.ofMillis(window.slideInterval().get().toMillis()));
        }
        return TimeWindows.ofSizeAndGrace(size, size);
    }

    private TopologyBuilder() {}
}
