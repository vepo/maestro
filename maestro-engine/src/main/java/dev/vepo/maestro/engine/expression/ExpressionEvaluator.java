package dev.vepo.maestro.engine.expression;

import java.util.List;
import java.util.Objects;

import dev.vepo.maestro.parser.model.BetweenPredicate;
import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.Expression;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;
import dev.vepo.maestro.parser.model.FunctionCallExpression;
import dev.vepo.maestro.parser.model.InPredicate;
import dev.vepo.maestro.parser.model.IsNotNullPredicate;
import dev.vepo.maestro.parser.model.IsNullPredicate;
import dev.vepo.maestro.parser.model.LikePredicate;
import dev.vepo.maestro.parser.model.Literal;
import dev.vepo.maestro.parser.model.LiteralExpression;
import dev.vepo.maestro.parser.model.LogicalExpression;
import dev.vepo.maestro.parser.model.LogicalOperator;
import dev.vepo.maestro.parser.model.MathBinaryExpression;
import dev.vepo.maestro.parser.model.MathOperator;
import dev.vepo.maestro.parser.model.NotExpression;
import dev.vepo.maestro.parser.model.NullLiteral;
import dev.vepo.maestro.parser.model.NumberLiteral;
import dev.vepo.maestro.parser.model.ParenthesizedExpression;
import dev.vepo.maestro.parser.model.RegexPredicate;
import dev.vepo.maestro.parser.model.StringLiteral;
import dev.vepo.maestro.parser.model.UnaryMinusExpression;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class ExpressionEvaluator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ObjectNode applyAssignments(JsonNode record, dev.vepo.maestro.parser.model.Assignment... assignments) {
        var result = record.isObject() ? ((ObjectNode) record).deepCopy() : MAPPER.createObjectNode();
        for (var assignment : assignments) {
            result.set(assignment.fieldName(), evaluate(record, assignment.expression()));
        }
        return result;
    }

    private static boolean compare(JsonNode left, JsonNode right, ComparisonOperator operator) {
        if (left.isNumber() && right.isNumber()) {
            var l = left.doubleValue();
            var r = right.doubleValue();
            return switch (operator) {
                case EQ -> l == r;
                case NEQ -> l != r;
                case LT -> l < r;
                case LTE -> l <= r;
                case GT -> l > r;
                case GTE -> l >= r;
            };
        }
        var l = left.asString();
        var r = right.asString();
        return switch (operator) {
            case EQ -> Objects.equals(l, r);
            case NEQ -> !Objects.equals(l, r);
            case LT -> l.compareTo(r) < 0;
            case LTE -> l.compareTo(r) <= 0;
            case GT -> l.compareTo(r) > 0;
            case GTE -> l.compareTo(r) >= 0;
        };
    }

    public static JsonNode evaluate(JsonNode record, Expression expression) {
        return switch (expression) {
            case ParenthesizedExpression p -> evaluate(record, p.expression());
            case NotExpression n -> JsonNodeFactory.instance.booleanNode(!matches(record, n.expression()));
            case LogicalExpression l -> evaluateLogical(record, l);
            case ComparisonExpression c -> evaluateComparison(record, c);
            case LiteralExpression lit -> literalNode(lit.value());
            case FieldReferenceExpression f -> fieldValue(record, f.fieldName());
            case FunctionCallExpression fn -> evaluateFunction(record, fn);
            case InPredicate in -> evaluateIn(record, in);
            case BetweenPredicate b -> evaluateBetween(record, b);
            case IsNullPredicate is -> JsonNodeFactory.instance.booleanNode(fieldValue(record, is.fieldName()).isNull());
            case IsNotNullPredicate isn -> JsonNodeFactory.instance.booleanNode(!fieldValue(record, isn.fieldName()).isNull());
            case LikePredicate like -> JsonNodeFactory.instance.booleanNode(fieldValue(record, like.fieldName()).asString()
                                                                                                                .contains(like.pattern().replace("%", "")));
            case RegexPredicate regex -> JsonNodeFactory.instance.booleanNode(fieldValue(record, regex.fieldName()).asString().matches(regex.pattern()));
            case MathBinaryExpression m -> evaluateMath(record, m);
            case UnaryMinusExpression u -> negate(evaluate(record, u.operand()));
            default -> throw new IllegalArgumentException("Unsupported expression: " + expression.getClass().getSimpleName());
        };
    }

    private static JsonNode evaluateBetween(JsonNode record, BetweenPredicate between) {
        var value = fieldValue(record, between.fieldName());
        var lower = literalNode(between.lowerBound());
        var upper = literalNode(between.upperBound());
        if (value.isNumber() && lower.isNumber() && upper.isNumber()) {
            var v = value.doubleValue();
            return JsonNodeFactory.instance.booleanNode(v >= lower.doubleValue() && v <= upper.doubleValue());
        }
        var v = value.asString();
        return JsonNodeFactory.instance.booleanNode(v.compareTo(lower.asString()) >= 0 && v.compareTo(upper.asString()) <= 0);
    }

    private static JsonNode evaluateComparison(JsonNode record, ComparisonExpression comparison) {
        var left = evaluate(record, comparison.left());
        var right = evaluate(record, comparison.right());
        return JsonNodeFactory.instance.booleanNode(compare(left, right, comparison.operator()));
    }

    private static JsonNode evaluateFunction(JsonNode record, FunctionCallExpression fn) {
        if ("to_epoch_ms".equalsIgnoreCase(fn.functionName()) && !fn.arguments().isEmpty()) {
            var arg = evaluate(record, fn.arguments().getFirst());
            if (arg.isNumber()) {
                return JsonNodeFactory.instance.numberNode(arg.longValue());
            }
            if (arg.isTextual()) {
                try {
                    return JsonNodeFactory.instance.numberNode(Long.parseLong(arg.asText()));
                } catch (NumberFormatException ignored) {
                    return JsonNodeFactory.instance.numberNode(System.currentTimeMillis());
                }
            }
            return JsonNodeFactory.instance.numberNode(System.currentTimeMillis());
        }
        if ("CONCAT".equalsIgnoreCase(fn.functionName())) {
            var sb = new StringBuilder();
            for (var arg : fn.arguments()) {
                sb.append(evaluate(record, arg).asString());
            }
            return JsonNodeFactory.instance.textNode(sb.toString());
        }
        if ("UPPER".equalsIgnoreCase(fn.functionName()) && !fn.arguments().isEmpty()) {
            return JsonNodeFactory.instance.textNode(evaluate(record, fn.arguments().getFirst()).asString().toUpperCase());
        }
        return fieldValue(record, fn.functionName());
    }

    private static JsonNode evaluateIn(JsonNode record, InPredicate in) {
        var field = fieldValue(record, in.fieldName()).asString();
        for (Literal literal : in.values()) {
            if (Objects.equals(field, literalValue(literal))) {
                return JsonNodeFactory.instance.booleanNode(true);
            }
        }
        return JsonNodeFactory.instance.booleanNode(false);
    }

    private static JsonNode evaluateLogical(JsonNode record, LogicalExpression logical) {
        var left = matches(record, logical.left());
        var right = matches(record, logical.right());
        var result = logical.operator() == LogicalOperator.AND ? left && right : left || right;
        return JsonNodeFactory.instance.booleanNode(result);
    }

    private static JsonNode evaluateMath(JsonNode record, MathBinaryExpression math) {
        var left = evaluate(record, math.left());
        var right = evaluate(record, math.right());
        double l = left.isNumber() ? left.doubleValue() : Double.parseDouble(left.asString());
        double r = right.isNumber() ? right.doubleValue() : Double.parseDouble(right.asString());
        double result = switch (math.operator()) {
            case ADD -> l + r;
            case SUBTRACT -> l - r;
            case MULTIPLY -> l * r;
            case DIVIDE -> r == 0 ? 0 : l / r;
        };
        return JsonNodeFactory.instance.numberNode(result);
    }

    public static JsonNode fieldValue(JsonNode record, String fieldName) {
        if (record == null || record.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (fieldName.contains(".")) {
            JsonNode current = record;
            for (var part : fieldName.split("\\.")) {
                if (current == null || current.isNull()) {
                    return JsonNodeFactory.instance.nullNode();
                }
                current = current.get(part);
            }
            return current == null ? JsonNodeFactory.instance.nullNode() : current;
        }
        var node = record.get(fieldName);
        return node == null ? JsonNodeFactory.instance.nullNode() : node;
    }

    private static JsonNode literalNode(Literal literal) {
        return switch (literal) {
            case StringLiteral s -> JsonNodeFactory.instance.textNode(s.value());
            case NumberLiteral n -> JsonNodeFactory.instance.numberNode(Double.parseDouble(n.value()));
            case dev.vepo.maestro.parser.model.BooleanLiteral b -> JsonNodeFactory.instance.booleanNode(b.value());
            case NullLiteral ignored -> JsonNodeFactory.instance.nullNode();
            default -> JsonNodeFactory.instance.nullNode();
        };
    }

    private static String literalValue(Literal literal) {
        return switch (literal) {
            case StringLiteral s -> s.value();
            case NumberLiteral n -> n.value();
            case dev.vepo.maestro.parser.model.BooleanLiteral b -> Boolean.toString(b.value());
            case NullLiteral ignored -> null;
            default -> "";
        };
    }

    public static boolean matches(JsonNode record, Expression expression) {
        var value = evaluate(record, expression);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        return !value.asString().isEmpty();
    }

    private static JsonNode negate(JsonNode node) {
        if (node.isNumber()) {
            return JsonNodeFactory.instance.numberNode(-node.doubleValue());
        }
        return JsonNodeFactory.instance.numberNode(-Double.parseDouble(node.asString()));
    }

    public static ObjectNode project(JsonNode record, List<String> fieldNames) {
        var result = MAPPER.createObjectNode();
        for (var field : fieldNames) {
            var value = fieldValue(record, field);
            if (!value.isNull()) {
                result.set(field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field, value);
            }
        }
        return result;
    }

    private ExpressionEvaluator() {}
}
