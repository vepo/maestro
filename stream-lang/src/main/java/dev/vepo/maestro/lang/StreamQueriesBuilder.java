package dev.vepo.maestro.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.lang.StreamParser.ComparisonOperatorExpressionContext;
import dev.vepo.maestro.lang.StreamParser.FieldNameContext;
import dev.vepo.maestro.lang.StreamParser.LiteralContext;
import dev.vepo.maestro.lang.StreamParser.TopicNameContext;
import dev.vepo.maestro.lang.StreamParser.ValueContext;
import dev.vepo.maestro.lang.model.AggregateFunction;
import dev.vepo.maestro.lang.model.AggregateStage;
import dev.vepo.maestro.lang.model.Assignment;
import dev.vepo.maestro.lang.model.BetweenPredicate;
import dev.vepo.maestro.lang.model.BooleanLiteral;
import dev.vepo.maestro.lang.model.ComparisonExpression;
import dev.vepo.maestro.lang.model.ComparisonOperator;
import dev.vepo.maestro.lang.model.Duration;
import dev.vepo.maestro.lang.model.Expression;
import dev.vepo.maestro.lang.model.FieldReferenceExpression;
import dev.vepo.maestro.lang.model.FieldReferenceLiteral;
import dev.vepo.maestro.lang.model.FilterStage;
import dev.vepo.maestro.lang.model.FlattenStage;
import dev.vepo.maestro.lang.model.FunctionCallExpression;
import dev.vepo.maestro.lang.model.InPredicate;
import dev.vepo.maestro.lang.model.IsNotNullPredicate;
import dev.vepo.maestro.lang.model.IsNullPredicate;
import dev.vepo.maestro.lang.model.JoinCondition;
import dev.vepo.maestro.lang.model.JoinStage;
import dev.vepo.maestro.lang.model.LikePredicate;
import dev.vepo.maestro.lang.model.Literal;
import dev.vepo.maestro.lang.model.LiteralExpression;
import dev.vepo.maestro.lang.model.LogicalExpression;
import dev.vepo.maestro.lang.model.LogicalOperator;
import dev.vepo.maestro.lang.model.NotExpression;
import dev.vepo.maestro.lang.model.NullLiteral;
import dev.vepo.maestro.lang.model.NumberLiteral;
import dev.vepo.maestro.lang.model.ParenthesizedExpression;
import dev.vepo.maestro.lang.model.ProcessingStage;
import dev.vepo.maestro.lang.model.ProjectStage;
import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.RegexPredicate;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;
import dev.vepo.maestro.lang.model.StringLiteral;
import dev.vepo.maestro.lang.model.TimeUnit;
import dev.vepo.maestro.lang.model.TransformStage;
import dev.vepo.maestro.lang.model.UniqueBy;
import dev.vepo.maestro.lang.model.WindowStage;
import dev.vepo.maestro.lang.model.WindowType;

public class StreamQueriesBuilder extends StreamBaseListener {
    private static final Logger logger = LoggerFactory.getLogger(StreamQueriesBuilder.class);
    private final Stack<Object> stack = new Stack<>();
    private final Stack<Query> queries = new Stack<>();
    private final Stack<Expression> expressions = new Stack<>();
    private StreamModel result;

    @Override
    public void exitStreamQueries(StreamParser.StreamQueriesContext ctx) {
        result = new StreamModel(queries);
    }

    @Override
    public void exitQuery(StreamParser.QueryContext ctx) {
        SourcePipeline pipeline = (SourcePipeline) stack.pop();
        List<String> sinkTopics = parseTopicList(ctx.sinkTopics());
        logger.info("Exiting query: pipeline={} sink={}", pipeline, sinkTopics);
        queries.push(new Query(pipeline, sinkTopics));
    }

    @Override
    public void exitSourcePipeline(StreamParser.SourcePipelineContext ctx) {
        List<ProcessingStage> stages = new ArrayList<>();
        while (!stack.isEmpty() && stack.peek() instanceof ProcessingStage) {
            stages.add(0, (ProcessingStage) stack.pop());
        }
        SourceStage sourceStage = (SourceStage) stack.pop();
        stack.push(new SourcePipeline(sourceStage, stages));
    }

    @Override
    public void exitSourceStage(StreamParser.SourceStageContext ctx) {
        List<String> topics = parseTopicList(ctx.sourceTopics());
        Optional<Expression> whereClause = Optional.empty();
        Optional<UniqueBy> uniqueBy = Optional.empty();

        if (ctx.where() != null) {
            whereClause = Optional.of(expressions.pop());
        } else if (ctx.unique() != null) {
            uniqueBy = Optional.of(new UniqueBy(parseFieldList(ctx.unique().fieldList())));
        }

        stack.push(new SourceStage(topics, whereClause, uniqueBy));
    }

    @Override
    public void exitProjectStage(StreamParser.ProjectStageContext ctx) {
        List<String> fields = parseFieldList(ctx.fieldList());
        stack.push(new ProjectStage(fields));
    }

    @Override
    public void exitAggregateStage(StreamParser.AggregateStageContext ctx) {
        List<String> groupByFields = ctx.fieldList() != null ? parseFieldList(ctx.fieldList()) : Collections.emptyList();

        List<AggregateFunction> functions = new ArrayList<>();
        for (var funcCtx : ctx.aggregateFunction()) {
            functions.add((AggregateFunction) stack.pop());
        }
        Collections.reverse(functions);
        stack.push(new AggregateStage(groupByFields, functions));
    }

    @Override
    public void exitAggregateFunction(StreamParser.AggregateFunctionContext ctx) {
        AggregateFunction.AggregateFunctionType type = getAggregateFunctionType(ctx);
        String field = parseField(ctx);
        Optional<String> alias = ctx.AS() != null ? Optional.of(ctx.IDENTIFIER(1).getText()) : Optional.empty();
        stack.push(new AggregateFunction(type, field, alias));
    }

    @Override
    public void exitWindowStage(StreamParser.WindowStageContext ctx) {
        WindowType windowType = WindowType.valueOf(ctx.windowType().getText().toUpperCase());
        Duration windowSize = (Duration) stack.pop();
        Optional<Duration> slideInterval = ctx.slideInterval() != null ? Optional.of((Duration) stack.pop()) : Optional.empty();
        stack.push(new WindowStage(windowType, windowSize, slideInterval));
    }

    @Override
    public void exitDuration(StreamParser.DurationContext ctx) {
        long value = Long.parseLong(ctx.NUMBER().getText());
        TimeUnit unit = TimeUnit.valueOf(ctx.timeUnit().getText().toUpperCase());
        stack.push(new Duration(value, unit));
    }

    @Override
    public void exitJoinStage(StreamParser.JoinStageContext ctx) {
        JoinCondition condition = (JoinCondition) stack.pop();
        List<String> sourceTopics = parseTopicList(ctx.sourceTopics());
        Optional<WindowType> windowType =
                ctx.windowType() != null ? Optional.of(WindowType.valueOf(ctx.windowType().getText().toUpperCase())) : Optional.empty();
        stack.push(new JoinStage(sourceTopics, condition, windowType));
    }

    @Override
    public void exitJoinCondition(StreamParser.JoinConditionContext ctx) {
        List<String> fields = parseFieldList(ctx.fieldName());
        stack.push(new JoinCondition(fields.get(0), fields.get(1)));
    }

    @Override
    public void exitFlattenStage(StreamParser.FlattenStageContext ctx) {
        stack.push(new FlattenStage(ctx.fieldName().getText()));
    }

    @Override
    public void exitFilterStage(StreamParser.FilterStageContext ctx) {
        stack.push(new FilterStage(expressions.pop()));
    }

    @Override
    public void exitTransformStage(StreamParser.TransformStageContext ctx) {
        var assignments = new LinkedList<Assignment>();
        var fields = ctx.fieldName();
        for (int i = fields.size() - 1; i >= 0; --i) {
            assignments.addFirst(new Assignment(fields.get(i).IDENTIFIER().getText(), expressions.pop()));
        }
        logger.info("Building assignments: {}", assignments);
        stack.push(new TransformStage(assignments));
    }

    // Expression handling
    @Override
    public void exitParenExpression(StreamParser.ParenExpressionContext ctx) {
        var expr = expressions.pop();
        expressions.push(new ParenthesizedExpression(expr));
    }

    @Override
    public void exitNotExpression(StreamParser.NotExpressionContext ctx) {
        expressions.push(new NotExpression(expressions.pop()));
    }

    @Override
    public void exitAndExpression(StreamParser.AndExpressionContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        expressions.push(new LogicalExpression(left, LogicalOperator.AND, right));
    }

    @Override
    public void exitOrExpression(StreamParser.OrExpressionContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        expressions.push(new LogicalExpression(left, LogicalOperator.OR, right));
    }

    @Override
    public void exitComparisonOperatorExpression(ComparisonOperatorExpressionContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        ComparisonOperator op = getComparisonOperator(ctx.comparisonOperator());
        logger.info("Adding comparison: left={} operator={} right={}", left, op, right);
        expressions.push(new ComparisonExpression(left, op, right));
    }

    @Override
    public void exitFunctionExpr(StreamParser.FunctionExprContext ctx) {
        // Function call is already on stack from exitFunctionCall
    }

    @Override
    public void exitFunctionCall(StreamParser.FunctionCallContext ctx) {
        List<Expression> args = new ArrayList<>();
        for (int i = 0; i < ctx.expression().size(); i++) {
            args.addFirst(expressions.pop());
        }
        String functionName = ctx.functionName().getText();
        expressions.push(new FunctionCallExpression(functionName, args));
    }

    @Override
    public void exitFieldRefExpr(StreamParser.FieldRefExprContext ctx) {
        expressions.push(new FieldReferenceExpression(ctx.fieldName().getText()));
    }

    @Override
    public void exitLiteralExpr(StreamParser.LiteralExprContext ctx) {
        expressions.push(new LiteralExpression(buildLiteral(ctx.literal())));
    }

    private Literal buildLiteral(ValueContext ctx) {
        if (Objects.nonNull(ctx.fieldName())) {
            return new FieldReferenceLiteral(ctx.fieldName().getText());
        } else {
            return buildLiteral(ctx.literal());
        }
    }

    private Literal buildLiteral(LiteralContext ctx) {
        if (ctx.STRING() != null) {
            String value = ctx.STRING().getText();
            value = value.substring(1, value.length() - 1); // Remove quotes
            return new StringLiteral(value);
        } else if (ctx.NUMBER() != null) {
            return new NumberLiteral(ctx.NUMBER().getText());
        } else if (ctx.BOOLEAN() != null) {
            boolean value = ctx.BOOLEAN().getText().equalsIgnoreCase("true");
            return new BooleanLiteral(value);
        } else if (ctx.NULL() != null) {
            return new NullLiteral();
        } else {
            throw new IllegalStateException("Unknown literal! %s".formatted(ctx.getText()));
        }

    }

    @Override
    public void exitInPredicate(StreamParser.InPredicateContext ctx) {
        String fieldName = ctx.fieldName().getText();
        expressions.push(new InPredicate(fieldName, ctx.valueList()
                                                       .literal()
                                                       .stream()
                                                       .map(this::buildLiteral)
                                                       .toList()));
    }

    @Override
    public void exitBetweenPredicate(StreamParser.BetweenPredicateContext ctx) {
        var upper = buildLiteral(ctx.upper);
        var lower = buildLiteral(ctx.lower);
        String fieldName = ctx.fieldName().getText();
        logger.info("Exiting between! field={} upper={} lower={}", fieldName, lower, upper);
        expressions.push(new BetweenPredicate(fieldName, lower, upper));
    }

    @Override
    public void exitIsNullPredicate(StreamParser.IsNullPredicateContext ctx) {
        expressions.push(new IsNullPredicate(ctx.fieldName().getText()));
    }

    @Override
    public void exitIsNotNullPredicate(StreamParser.IsNotNullPredicateContext ctx) {
        expressions.push(new IsNotNullPredicate(ctx.fieldName().getText()));
    }

    @Override
    public void exitLikePredicate(StreamParser.LikePredicateContext ctx) {
        String pattern = ctx.STRING().getText();
        pattern = pattern.substring(1, pattern.length() - 1); // Remove quotes
        expressions.push(new LikePredicate(ctx.fieldName().getText(), pattern));
    }

    @Override
    public void exitRegexPredicate(StreamParser.RegexPredicateContext ctx) {
        String pattern = ctx.STRING().getText();
        pattern = pattern.substring(1, pattern.length() - 1); // Remove quotes
        expressions.push(new RegexPredicate(ctx.fieldName().getText(), pattern));
    }

    // Helper methods
    private List<String> parseTopicList(StreamParser.SourceTopicsContext ctx) {
        return ctx.topicName()
                  .stream()
                  .map(TopicNameContext::IDENTIFIER)
                  .map(TerminalNode::getText)
                  .toList();
    }

    private List<String> parseTopicList(StreamParser.SinkTopicsContext ctx) {
        return ctx.topicName()
                  .stream()
                  .map(TopicNameContext::IDENTIFIER)
                  .map(TerminalNode::getText)
                  .toList();
    }

    private List<String> parseFieldList(StreamParser.FieldListContext ctx) {
        return ctx.fieldName()
                  .stream()
                  .map(FieldNameContext::IDENTIFIER)
                  .map(TerminalNode::getText)
                  .toList();
    }

    private List<String> parseFieldList(List<StreamParser.FieldNameContext> fields) {
        return fields.stream()
                     .map(StreamParser.FieldNameContext::getText)
                     .toList();
    }

    private String parseField(StreamParser.AggregateFunctionContext ctx) {
        if (ctx.IDENTIFIER(0).getText().equalsIgnoreCase("count") && Objects.isNull(ctx.fieldName())) {
            return "*";
        } else if (Objects.nonNull(ctx.fieldName())) {
            return ctx.fieldName().getText();
        }
        return "";
    }

    private AggregateFunction.AggregateFunctionType getAggregateFunctionType(StreamParser.AggregateFunctionContext ctx) {
        return switch (ctx.IDENTIFIER(0).getText().toLowerCase()) {
            case "count" -> AggregateFunction.AggregateFunctionType.COUNT;
            case "sum" -> AggregateFunction.AggregateFunctionType.SUM;
            case "avg" -> AggregateFunction.AggregateFunctionType.AVG;
            case "min" -> AggregateFunction.AggregateFunctionType.MIN;
            case "max" -> AggregateFunction.AggregateFunctionType.MAX;
            case "first" -> AggregateFunction.AggregateFunctionType.FIRST;
            case "last" -> AggregateFunction.AggregateFunctionType.LAST;
            default -> throw new IllegalArgumentException("Unknown aggregate function: %s".formatted(ctx.IDENTIFIER(0).getText()));
        };
    }

    private ComparisonOperator getComparisonOperator(StreamParser.ComparisonOperatorContext ctx) {
        if (ctx.EQ() != null)
            return ComparisonOperator.EQ;
        if (ctx.NEQ() != null)
            return ComparisonOperator.NEQ;
        if (ctx.LT() != null)
            return ComparisonOperator.LT;
        if (ctx.LTE() != null)
            return ComparisonOperator.LTE;
        if (ctx.GT() != null)
            return ComparisonOperator.GT;
        if (ctx.GTE() != null)
            return ComparisonOperator.GTE;
        throw new IllegalArgumentException("Unknown comparison operator");
    }

    public StreamModel getResult() {
        return result;
    }
}