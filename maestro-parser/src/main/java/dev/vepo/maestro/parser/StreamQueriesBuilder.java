package dev.vepo.maestro.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.parser.model.AggregateFunction;
import dev.vepo.maestro.parser.model.AggregateStage;
import dev.vepo.maestro.parser.model.Assignment;
import dev.vepo.maestro.parser.model.BetweenPredicate;
import dev.vepo.maestro.parser.model.BooleanLiteral;
import dev.vepo.maestro.parser.model.BranchCase;
import dev.vepo.maestro.parser.model.BranchStage;
import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.Duration;
import dev.vepo.maestro.parser.model.Expression;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;
import dev.vepo.maestro.parser.model.FieldReferenceLiteral;
import dev.vepo.maestro.parser.model.FilterStage;
import dev.vepo.maestro.parser.model.FunctionCallExpression;
import dev.vepo.maestro.parser.model.GroupByStage;
import dev.vepo.maestro.parser.model.InPredicate;
import dev.vepo.maestro.parser.model.IsNotNullPredicate;
import dev.vepo.maestro.parser.model.IsNullPredicate;
import dev.vepo.maestro.parser.model.JoinKind;
import dev.vepo.maestro.parser.model.JoinStage;
import dev.vepo.maestro.parser.model.LikePredicate;
import dev.vepo.maestro.parser.model.Literal;
import dev.vepo.maestro.parser.model.LiteralExpression;
import dev.vepo.maestro.parser.model.LogicalExpression;
import dev.vepo.maestro.parser.model.LogicalOperator;
import dev.vepo.maestro.parser.model.MapStage;
import dev.vepo.maestro.parser.model.MathBinaryExpression;
import dev.vepo.maestro.parser.model.MathOperator;
import dev.vepo.maestro.parser.model.NotExpression;
import dev.vepo.maestro.parser.model.NullLiteral;
import dev.vepo.maestro.parser.model.NumberLiteral;
import dev.vepo.maestro.parser.model.ParenthesizedExpression;
import dev.vepo.maestro.parser.model.PatternDefinition;
import dev.vepo.maestro.parser.model.PatternStage;
import dev.vepo.maestro.parser.model.ProcessingStage;
import dev.vepo.maestro.parser.model.ProjectField;
import dev.vepo.maestro.parser.model.ProjectStage;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.RegexPredicate;
import dev.vepo.maestro.parser.model.SessionizeStage;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.parser.model.StringLiteral;
import dev.vepo.maestro.parser.model.ToStage;
import dev.vepo.maestro.parser.model.UnaryMinusExpression;
import dev.vepo.maestro.parser.model.WindowStage;
import dev.vepo.maestro.parser.model.WindowType;

public class StreamQueriesBuilder extends StreamBaseListener {
    private record JoinSourceInfo(JoinKind kind, Optional<String> topic, Optional<Duration> within) {}

    private static final Logger logger = LoggerFactory.getLogger(StreamQueriesBuilder.class);
    private final Deque<ProcessingStage> stages = new LinkedList<>();
    private final Stack<Query> queries = new Stack<>();
    private final Stack<Expression> expressions = new Stack<>();
    private final Stack<Assignment> assignments = new Stack<>();
    private final Stack<PatternDefinition> patternDefs = new Stack<>();
    private final Stack<BranchCase> branchCases = new Stack<>();
    private final Stack<AggregateFunction> aggregateFunctions = new Stack<>();
    private final Stack<ProjectField> projectFields = new Stack<>();
    private final Stack<Duration> durations = new Stack<>();
    private final Deque<ProcessingStage> branchStages = new LinkedList<>();
    private int branchDepth = 0;
    private JoinSourceInfo pendingJoinSource;

    private StreamModel result;

    private void addStage(ProcessingStage stage) {
        if (branchDepth > 0) {
            branchStages.addLast(stage);
        } else {
            stages.addLast(stage);
        }
    }

    private AggregateFunction.AggregateFunctionType aggregateType(StreamParser.AggFunctionContext ctx) {
        if (ctx.COUNT() != null) {
            return AggregateFunction.AggregateFunctionType.COUNT;
        }
        return switch (ctx.aggName().getText().toLowerCase()) {
            case "sum" -> AggregateFunction.AggregateFunctionType.SUM;
            case "avg" -> AggregateFunction.AggregateFunctionType.AVG;
            case "min" -> AggregateFunction.AggregateFunctionType.MIN;
            case "max" -> AggregateFunction.AggregateFunctionType.MAX;
            case "first" -> AggregateFunction.AggregateFunctionType.FIRST;
            case "last" -> AggregateFunction.AggregateFunctionType.LAST;
            default -> throw new IllegalArgumentException("Unknown aggregate: " + ctx.aggName().getText());
        };
    }

    private Literal buildLiteral(StreamParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            return new StringLiteral(unquote(ctx.STRING().getText()));
        }
        if (ctx.NUMBER() != null) {
            return new NumberLiteral(ctx.NUMBER().getText());
        }
        if (ctx.BOOLEAN() != null) {
            return new BooleanLiteral(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        }
        return new NullLiteral();
    }

    private ComparisonOperator comparisonOperator(StreamParser.ComparisonOperatorContext ctx) {
        if (ctx.EQ() != null) {
            return ComparisonOperator.EQ;
        }
        if (ctx.NEQ() != null) {
            return ComparisonOperator.NEQ;
        }
        if (ctx.LT() != null) {
            return ComparisonOperator.LT;
        }
        if (ctx.LTE() != null) {
            return ComparisonOperator.LTE;
        }
        if (ctx.GT() != null) {
            return ComparisonOperator.GT;
        }
        return ComparisonOperator.GTE;
    }

    private List<String> destinationList(StreamParser.DestinationListContext ctx) {
        return ctx.topicName().stream().map(this::topicText).toList();
    }

    @Override
    public void enterBranchCase(StreamParser.BranchCaseContext ctx) {
        branchDepth++;
        branchStages.clear();
    }

    @Override
    public void exitAddSub(StreamParser.AddSubContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        var op = ctx.PLUS() != null ? MathOperator.ADD : MathOperator.SUBTRACT;
        expressions.push(new MathBinaryExpression(left, op, right));
    }

    @Override
    public void exitAggFunction(StreamParser.AggFunctionContext ctx) {
        var type = aggregateType(ctx);
        var field = ctx.fieldName() != null ? fieldText(ctx.fieldName()) : "*";
        Optional<String> alias = ctx.AS() != null ? Optional.of(ctx.IDENTIFIER().getText()) : Optional.empty();
        aggregateFunctions.push(new AggregateFunction(type, field, alias));
    }

    @Override
    public void exitAggregateStage(StreamParser.AggregateStageContext ctx) {
        var funcs = new ArrayList<>(aggregateFunctions);
        aggregateFunctions.clear();
        Collections.reverse(funcs);
        addStage(new AggregateStage(funcs));
    }

    @Override
    public void exitAndExpression(StreamParser.AndExpressionContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        expressions.push(new LogicalExpression(left, LogicalOperator.AND, right));
    }

    @Override
    public void exitAtom(StreamParser.AtomContext ctx) {
        if (ctx.fieldName() != null) {
            expressions.push(new FieldReferenceExpression(fieldText(ctx.fieldName())));
        } else if (ctx.literal() != null) {
            expressions.push(new LiteralExpression(buildLiteral(ctx.literal())));
        } else if (ctx.functionCall() != null) {
            // function call already on stack from exitFunctionCall
        } else {
            // parenthesized math — expression already on stack
        }
    }

    @Override
    public void exitBetweenPredicate(StreamParser.BetweenPredicateContext ctx) {
        expressions.push(new BetweenPredicate(fieldText(ctx.fieldName()), literalFromValue(ctx.value(0)), literalFromValue(ctx.value(1))));
    }

    @Override
    public void exitBranchCase(StreamParser.BranchCaseContext ctx) {
        var caseStages = new ArrayList<>(branchStages);
        branchStages.clear();
        branchDepth--;
        if (ctx.DEFAULT() != null) {
            branchCases.push(BranchCase.defaults(caseStages));
        } else {
            branchCases.push(BranchCase.when(expressions.pop(), caseStages));
        }
    }

    @Override
    public void exitBranchStage(StreamParser.BranchStageContext ctx) {
        var cases = new ArrayList<>(branchCases);
        branchCases.clear();
        Collections.reverse(cases);
        addStage(new BranchStage(cases));
    }

    @Override
    public void exitComparisonPredicate(StreamParser.ComparisonPredicateContext ctx) {
        var right = valueExpression(ctx.value());
        var left = new FieldReferenceExpression(fieldText(ctx.fieldName()));
        expressions.push(new ComparisonExpression(left, comparisonOperator(ctx.comparisonOperator()), right));
    }

    @Override
    public void exitDuration(StreamParser.DurationContext ctx) {
        durations.push(new Duration(Long.parseLong(ctx.NUMBER().getText()), timeUnit(ctx.timeUnit())));
    }

    @Override
    public void exitFieldRefExpr(StreamParser.FieldRefExprContext ctx) {
        expressions.push(new FieldReferenceExpression(fieldText(ctx.fieldName())));
    }

    @Override
    public void exitFilterStage(StreamParser.FilterStageContext ctx) {
        addStage(new FilterStage(expressions.pop()));
    }

    @Override
    public void exitFunctionCall(StreamParser.FunctionCallContext ctx) {
        var args = new ArrayList<Expression>();
        for (int i = ctx.expression().size() - 1; i >= 0; i--) {
            args.addFirst(expressions.pop());
        }
        expressions.push(new FunctionCallExpression(ctx.functionName().getText(), args));
    }

    @Override
    public void exitGroupByStage(StreamParser.GroupByStageContext ctx) {
        addStage(new GroupByStage(fieldList(ctx.fieldList())));
    }

    @Override
    public void exitInPredicate(StreamParser.InPredicateContext ctx) {
        expressions.push(new InPredicate(fieldText(ctx.fieldName()), ctx.valueList()
                                                                        .literal()
                                                                        .stream()
                                                                        .map(this::buildLiteral)
                                                                        .toList()));
    }

    @Override
    public void exitIsNotNullPredicate(StreamParser.IsNotNullPredicateContext ctx) {
        expressions.push(new IsNotNullPredicate(fieldText(ctx.fieldName())));
    }

    @Override
    public void exitIsNullPredicate(StreamParser.IsNullPredicateContext ctx) {
        expressions.push(new IsNullPredicate(fieldText(ctx.fieldName())));
    }

    @Override
    public void exitJoinSource(StreamParser.JoinSourceContext ctx) {
        if (ctx.LOOKUP() != null) {
            pendingJoinSource = new JoinSourceInfo(JoinKind.LOOKUP_TABLE, Optional.of(quotedTopic(ctx.quotedTopic())), Optional.empty());
        } else if (ctx.STREAM_KW() != null) {
            pendingJoinSource = new JoinSourceInfo(JoinKind.STREAM, Optional.of(quotedTopic(ctx.quotedTopic())), Optional.empty());
        } else {
            pendingJoinSource = new JoinSourceInfo(JoinKind.STREAM_WITHIN, Optional.empty(), Optional.of(durations.pop()));
        }
    }

    @Override
    public void exitJoinStage(StreamParser.JoinStageContext ctx) {
        var condition = expressions.pop();
        var target = ctx.joinTarget().IDENTIFIER().getText();
        var info = pendingJoinSource;
        pendingJoinSource = null;
        addStage(new JoinStage(target, condition, info.kind(), info.topic(), info.within()));
    }

    @Override
    public void exitLikePredicate(StreamParser.LikePredicateContext ctx) {
        expressions.push(new LikePredicate(fieldText(ctx.fieldName()), unquote(ctx.STRING().getText())));
    }

    @Override
    public void exitLiteralExpr(StreamParser.LiteralExprContext ctx) {
        expressions.push(new LiteralExpression(buildLiteral(ctx.literal())));
    }

    @Override
    public void exitMapping(StreamParser.MappingContext ctx) {
        assignments.push(new Assignment(fieldText(ctx.fieldName()), expressions.pop()));
    }

    @Override
    public void exitMapStage(StreamParser.MapStageContext ctx) {
        var maps = new ArrayList<Assignment>();
        while (!assignments.isEmpty()) {
            maps.addFirst(assignments.pop());
        }
        addStage(new MapStage(maps));
    }

    @Override
    public void exitMulDiv(StreamParser.MulDivContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        var op = ctx.STAR() != null ? MathOperator.MULTIPLY : MathOperator.DIVIDE;
        expressions.push(new MathBinaryExpression(left, op, right));
    }

    @Override
    public void exitNotExpression(StreamParser.NotExpressionContext ctx) {
        expressions.push(new NotExpression(expressions.pop()));
    }

    @Override
    public void exitOrExpression(StreamParser.OrExpressionContext ctx) {
        var right = expressions.pop();
        var left = expressions.pop();
        expressions.push(new LogicalExpression(left, LogicalOperator.OR, right));
    }

    @Override
    public void exitParenExpression(StreamParser.ParenExpressionContext ctx) {
        expressions.push(new ParenthesizedExpression(expressions.pop()));
    }

    @Override
    public void exitPatternDef(StreamParser.PatternDefContext ctx) {
        var expr = expressions.pop();
        var within = Optional.<Duration>empty();
        var after = Optional.<String>empty();
        var pexpr = ctx.patternExpression();
        if (pexpr.WITHIN() != null) {
            within = Optional.of(durations.pop());
        }
        if (pexpr.AFTER() != null) {
            after = Optional.of(pexpr.IDENTIFIER().getText());
        }
        patternDefs.push(new PatternDefinition(ctx.IDENTIFIER().getText(), expr, within, after));
    }

    @Override
    public void exitPatternStage(StreamParser.PatternStageContext ctx) {
        var defs = new ArrayList<>(patternDefs);
        patternDefs.clear();
        Collections.reverse(defs);
        addStage(new PatternStage(defs, ctx.patternSpec().IDENTIFIER().getText()));
    }

    @Override
    public void exitPipeline(StreamParser.PipelineContext ctx) {
        var pipelineStages = new ArrayList<>(stages);
        stages.clear();
        var sourceTopic = topicText(ctx.topicName());
        var sinkTopics = pipelineStages.stream()
                                       .filter(ToStage.class::isInstance)
                                       .flatMap(s -> ((ToStage) s).topics().stream())
                                       .toList();
        var processing = pipelineStages.stream()
                                       .filter(s -> !(s instanceof ToStage))
                                       .toList();
        var pipeline = new SourcePipeline(new SourceStage(sourceTopic), processing);
        queries.push(new Query(pipeline, sinkTopics));
        logger.debug("Built query from {} with {} stages, sinks={}", sourceTopic, processing.size(), sinkTopics);
    }

    @Override
    public void exitProjectField(StreamParser.ProjectFieldContext ctx) {
        if (ctx.expression() != null) {
            projectFields.push(new ProjectField(fieldText(ctx.fieldName()), Optional.of(expressions.pop())));
        } else {
            projectFields.push(new ProjectField(fieldText(ctx.fieldName())));
        }
    }

    @Override
    public void exitProjectStage(StreamParser.ProjectStageContext ctx) {
        List<ProjectField> fields;
        Optional<String> alias = Optional.empty();
        var spec = ctx.projectSpec();
        if (spec.fieldList() != null) {
            fields = fieldList(spec.fieldList()).stream().map(ProjectField::new).toList();
            if (spec.IDENTIFIER() != null && spec.FIELDS() == null) {
                alias = Optional.of(spec.IDENTIFIER().getText());
            }
        } else {
            fields = new ArrayList<>(projectFields);
            projectFields.clear();
            Collections.reverse(fields);
        }
        addStage(new ProjectStage(alias, fields));
    }

    @Override
    public void exitRegexPredicate(StreamParser.RegexPredicateContext ctx) {
        expressions.push(new RegexPredicate(fieldText(ctx.fieldName()), unquote(ctx.STRING().getText())));
    }

    @Override
    public void exitSessionizeStage(StreamParser.SessionizeStageContext ctx) {
        var spec = ctx.sessionSpec();
        Optional<Duration> timeout = spec.TIMEOUT() != null ? Optional.of(durations.pop()) : Optional.empty();
        var gap = durations.pop();
        addStage(new SessionizeStage(fieldList(spec.fieldList()), gap, timeout));
    }

    @Override
    public void exitStream(StreamParser.StreamContext ctx) {
        result = new StreamModel(queries.toArray(Query[]::new));
    }

    @Override
    public void exitToStage(StreamParser.ToStageContext ctx) {
        addStage(new ToStage(destinationList(ctx.destinationList())));
    }

    @Override
    public void exitUnaryMinus(StreamParser.UnaryMinusContext ctx) {
        expressions.push(new UnaryMinusExpression(expressions.pop()));
    }

    @Override
    public void exitWindowStage(StreamParser.WindowStageContext ctx) {
        var spec = ctx.windowSpec();
        WindowType type;
        Duration size;
        Optional<Duration> advance = Optional.empty();
        if (spec.TUMBLING() != null) {
            type = WindowType.TUMBLING;
            size = durations.pop();
        } else if (spec.HOPPING() != null) {
            type = WindowType.HOPPING;
            advance = Optional.of(durations.pop());
            size = durations.pop();
        } else {
            type = WindowType.SLIDING;
            size = durations.pop();
        }
        addStage(new WindowStage(type, size, advance));
    }

    private List<String> fieldList(StreamParser.FieldListContext ctx) {
        return ctx.fieldName().stream().map(this::fieldText).toList();
    }

    private String fieldText(StreamParser.FieldNameContext ctx) {
        var parts = ctx.IDENTIFIER().stream().map(t -> t.getText()).toList();
        return String.join(".", parts);
    }

    public StreamModel getResult() {
        return result;
    }

    private Literal literalFromValue(StreamParser.ValueContext ctx) {
        if (ctx.literal() != null) {
            return buildLiteral(ctx.literal());
        }
        throw new IllegalArgumentException("BETWEEN bounds must be literals");
    }

    private String quotedTopic(StreamParser.QuotedTopicContext ctx) {
        return unquote(ctx.STRING().getText());
    }

    private dev.vepo.maestro.parser.model.TimeUnit timeUnit(StreamParser.TimeUnitContext ctx) {
        if (ctx.MILLISECONDS() != null) {
            return dev.vepo.maestro.parser.model.TimeUnit.MILLISECONDS;
        }
        if (ctx.SECONDS() != null) {
            return dev.vepo.maestro.parser.model.TimeUnit.SECONDS;
        }
        if (ctx.MINUTES() != null) {
            return dev.vepo.maestro.parser.model.TimeUnit.MINUTES;
        }
        if (ctx.HOUR() != null) {
            return dev.vepo.maestro.parser.model.TimeUnit.HOURS;
        }
        if (ctx.HOURS() != null) {
            return dev.vepo.maestro.parser.model.TimeUnit.HOURS;
        }
        return dev.vepo.maestro.parser.model.TimeUnit.DAYS;
    }

    private String topicText(StreamParser.TopicNameContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            return ctx.IDENTIFIER().getText();
        }
        return quotedTopic(ctx.quotedTopic());
    }

    private String unquote(String text) {
        return text.substring(1, text.length() - 1);
    }

    private Expression valueExpression(StreamParser.ValueContext ctx) {
        if (ctx.literal() != null) {
            return new LiteralExpression(buildLiteral(ctx.literal()));
        }
        return new FieldReferenceExpression(fieldText(ctx.fieldName()));
    }
}
