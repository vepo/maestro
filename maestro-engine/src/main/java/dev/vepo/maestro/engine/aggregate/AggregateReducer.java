package dev.vepo.maestro.engine.aggregate;

import java.util.List;

import dev.vepo.maestro.engine.expression.ExpressionEvaluator;
import dev.vepo.maestro.parser.model.AggregateFunction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class AggregateReducer {
    private static final String SUM_SUFFIX = "__sum";
    private static final String COUNT_SUFFIX = "__count";

    public static ObjectNode finalizeAggregates(ObjectNode aggregateNode, List<AggregateFunction> functions) {
        for (var fn : functions) {
            if (fn.type() == AggregateFunction.AggregateFunctionType.AVG) {
                var alias = fn.alias().orElse("avg");
                aggregateNode.remove(alias + SUM_SUFFIX);
                aggregateNode.remove(alias + COUNT_SUFFIX);
            }
        }
        return aggregateNode;
    }

    public static ObjectNode merge(ObjectNode aggregateNode, JsonNode value, List<AggregateFunction> functions) {
        for (var fn : functions) {
            var alias = fn.alias().orElse(fn.type().name().toLowerCase());
            switch (fn.type()) {
                case COUNT -> {
                    var current = aggregateNode.has(alias) ? aggregateNode.get(alias).asInt() : 0;
                    aggregateNode.put(alias, current + 1);
                }
                case SUM -> {
                    var fieldValue = numericField(value, fn.field());
                    var current = aggregateNode.has(alias) ? aggregateNode.get(alias).asDouble() : 0;
                    aggregateNode.put(alias, current + fieldValue);
                }
                case AVG -> {
                    var fieldValue = numericField(value, fn.field());
                    var sumKey = alias + SUM_SUFFIX;
                    var countKey = alias + COUNT_SUFFIX;
                    var sum = aggregateNode.has(sumKey) ? aggregateNode.get(sumKey).asDouble() : 0;
                    var count = aggregateNode.has(countKey) ? aggregateNode.get(countKey).asInt() : 0;
                    aggregateNode.put(sumKey, sum + fieldValue);
                    aggregateNode.put(countKey, count + 1);
                    aggregateNode.put(alias, (sum + fieldValue) / (count + 1));
                }
                case MIN -> updateExtremum(aggregateNode, alias, value, fn.field(), true);
                case MAX -> updateExtremum(aggregateNode, alias, value, fn.field(), false);
                case FIRST -> {
                    if (!aggregateNode.has(alias)) {
                        aggregateNode.set(alias, ExpressionEvaluator.fieldValue(value, fn.field()));
                    }
                }
                case LAST -> aggregateNode.set(alias, ExpressionEvaluator.fieldValue(value, fn.field()));
                default -> {}
            }
        }
        return aggregateNode;
    }

    private static double numericField(JsonNode value, String field) {
        var node = ExpressionEvaluator.fieldValue(value, field);
        if (node.isNumber()) {
            return node.doubleValue();
        }
        return Double.parseDouble(node.asText());
    }

    private static void updateExtremum(ObjectNode aggregateNode, String alias, JsonNode value, String field, boolean min) {
        var fieldValue = numericField(value, field);
        if (!aggregateNode.has(alias)) {
            aggregateNode.put(alias, fieldValue);
            return;
        }
        var current = aggregateNode.get(alias).asDouble();
        if (min && fieldValue < current) {
            aggregateNode.put(alias, fieldValue);
        } else if (!min && fieldValue > current) {
            aggregateNode.put(alias, fieldValue);
        }
    }

    private AggregateReducer() {}
}
