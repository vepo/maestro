package dev.vepo.maestro.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.engine.expression.ExpressionEvaluator;
import dev.vepo.maestro.parser.model.AggregateFunction;
import dev.vepo.maestro.parser.model.AggregateStage;
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

    private static final Logger logger = LoggerFactory.getLogger(TopologyBuilder.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static KStream<String, Object> applyAggregate(
                                                          KStream<String, Object> stream,
                                                          WindowStage window,
                                                          GroupByStage group,
                                                          AggregateStage aggregate,
                                                          StreamsBuilder builder) {
        if (window == null) {
            window = new WindowStage(WindowType.TUMBLING, new dev.vepo.maestro.parser.model.Duration(1, dev.vepo.maestro.parser.model.TimeUnit.MINUTES));
        }
        var windowed = stream
                             .groupBy((key, value) -> groupKey(asJson(value), group),
                                      Grouped.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter()))
                             .windowedBy(toKafkaWindow(window));
        var agg = windowed.aggregate(
                                     () -> (Object) MAPPER.createObjectNode(),
                                     (key, value, aggregateNode) -> mergeAggregate((ObjectNode) aggregateNode, asJson(value), aggregate.functions()),
                                     Materialized.with(org.apache.kafka.common.serialization.Serdes.String(), new JsonSerdeAdapter()));
        return agg.toStream().map((windowedKey, value) -> new org.apache.kafka.streams.KeyValue<>(windowedKey.key(), value));
    }

    private static KStream<String, Object> applyJoin(KStream<String, Object> stream, JoinStage join, StreamsBuilder builder) {
        var topic = join.sourceTopic().orElse(join.target());
        if (join.kind() == JoinKind.LOOKUP_TABLE || join.kind() == JoinKind.STREAM) {
            var table = builder.<String, Object>globalTable(topic);
            return stream.join(table, (key, left) -> key, (left, right) -> {
                var merged = MAPPER.createObjectNode();
                if (left instanceof ObjectNode leftNode) {
                    merged.setAll(leftNode);
                } else if (left instanceof JsonNode leftJson && leftJson.isObject()) {
                    leftJson.properties().forEach(e -> merged.set(e.getKey(), e.getValue()));
                }
                if (right instanceof JsonNode rightNode) {
                    merged.set("join", rightNode);
                }
                return merged;
            });
        }
        return stream;
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
        for (var query : model.queries()) {
            buildQuery(query, builder);
        }
    }

    private static void buildQuery(Query query, StreamsBuilder builder) {
        var sourceTopic = query.sourcePipeline().sourceStage().topics().getFirst();
        KStream<String, Object> stream = builder.stream(sourceTopic);

        var where = query.sourcePipeline().sourceStage().whereClause();
        if (where.isPresent()) {
            var condition = where.get();
            stream = stream.filter((key, value) -> ExpressionEvaluator.matches(asJson(value), condition));
        }

        WindowStage pendingWindow = null;
        GroupByStage pendingGroup = null;

        for (var stage : query.sourcePipeline().processingStages()) {
            if (stage instanceof FilterStage filter) {
                stream = stream.filter((key, value) -> ExpressionEvaluator.matches(asJson(value), filter.condition()));
            } else if (stage instanceof ProjectStage project) {
                stream = stream.mapValues(value -> projectRecord(asJson(value), project));
            } else if (stage instanceof MapStage map) {
                stream = stream.mapValues(value -> ExpressionEvaluator.applyAssignments(asJson(value),
                                                                                        map.assignments()
                                                                                           .toArray(dev.vepo.maestro.parser.model.Assignment[]::new)));
            } else if (stage instanceof WindowStage window) {
                pendingWindow = window;
            } else if (stage instanceof GroupByStage group) {
                pendingGroup = group;
            } else if (stage instanceof AggregateStage aggregate) {
                stream = applyAggregate(stream, pendingWindow, pendingGroup, aggregate, builder);
                pendingWindow = null;
                pendingGroup = null;
            } else if (stage instanceof JoinStage join) {
                stream = applyJoin(stream, join, builder);
            } else if (stage instanceof BranchStage) {
                throw new UnsupportedStageException("BranchStage");
            } else if (stage instanceof PatternStage) {
                throw new UnsupportedStageException("PatternStage");
            } else if (stage instanceof SessionizeStage) {
                throw new UnsupportedStageException("SessionizeStage");
            } else {
                logger.warn("Skipping unsupported stage: {}", stage.getClass().getSimpleName());
            }
        }

        for (var sink : query.sinkTopics()) {
            stream.to(sink);
        }
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

    private static ObjectNode mergeAggregate(ObjectNode aggregateNode, JsonNode value, List<AggregateFunction> functions) {
        for (var fn : functions) {
            var alias = fn.alias().orElse(fn.type().name().toLowerCase());
            if (fn.type() == AggregateFunction.AggregateFunctionType.COUNT) {
                var current = aggregateNode.has(alias) ? aggregateNode.get(alias).asInt() : 0;
                aggregateNode.put(alias, current + 1);
            } else {
                var field = fn.field();
                var fieldValue = ExpressionEvaluator.fieldValue(value, field);
                if (fn.type() == AggregateFunction.AggregateFunctionType.AVG || fn.type() == AggregateFunction.AggregateFunctionType.MAX
                        || fn.type() == AggregateFunction.AggregateFunctionType.MIN) {
                    aggregateNode.put(alias, fieldValue.asDouble());
                }
            }
        }
        return aggregateNode;
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

    private static TimeWindows toKafkaWindow(WindowStage window) {
        var size = Duration.ofMillis(window.windowSize().toMillis());
        if (window.windowType() == WindowType.HOPPING && window.slideInterval().isPresent()) {
            return TimeWindows.ofSizeAndGrace(size, Duration.ZERO).advanceBy(Duration.ofMillis(window.slideInterval().get().toMillis()));
        }
        return TimeWindows.ofSizeAndGrace(size, Duration.ZERO);
    }

    private TopologyBuilder() {}
}
