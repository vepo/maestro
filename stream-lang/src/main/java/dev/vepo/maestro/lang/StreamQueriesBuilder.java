package dev.vepo.maestro.lang;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.lang.StreamParser.AndExpressionContext;
import dev.vepo.maestro.lang.StreamParser.BetweenPredicateContext;
import dev.vepo.maestro.lang.StreamParser.ComparisonPredicateContext;
import dev.vepo.maestro.lang.StreamParser.FromContext;
import dev.vepo.maestro.lang.StreamParser.InPredicateContext;
import dev.vepo.maestro.lang.StreamParser.IsNotNullPredicateContext;
import dev.vepo.maestro.lang.StreamParser.IsNullPredicateContext;
import dev.vepo.maestro.lang.StreamParser.LiteralContext;
import dev.vepo.maestro.lang.StreamParser.NotExpressionContext;
import dev.vepo.maestro.lang.StreamParser.QueryContext;
import dev.vepo.maestro.lang.StreamParser.ToContext;
import dev.vepo.maestro.lang.StreamParser.TopicNameContext;
import dev.vepo.maestro.lang.StreamParser.ValueContext;
import dev.vepo.maestro.lang.model.Sink;
import dev.vepo.maestro.lang.model.Source;
import dev.vepo.maestro.lang.model.StreamQuery;
import dev.vepo.maestro.lang.model.predicate.AndPredicate;
import dev.vepo.maestro.lang.model.predicate.FieldPredicate;
import dev.vepo.maestro.lang.model.predicate.ListValue;
import dev.vepo.maestro.lang.model.predicate.NoPredicate;
import dev.vepo.maestro.lang.model.predicate.NotPredicate;
import dev.vepo.maestro.lang.model.predicate.NullValue;
import dev.vepo.maestro.lang.model.predicate.NumberValue;
import dev.vepo.maestro.lang.model.predicate.Operator;
import dev.vepo.maestro.lang.model.predicate.Predicate;
import dev.vepo.maestro.lang.model.predicate.RangeValue;
import dev.vepo.maestro.lang.model.predicate.StringValue;
import dev.vepo.maestro.lang.model.predicate.Value;

public class StreamQueriesBuilder extends StreamBaseListener {
    private static final Logger logger = LoggerFactory.getLogger(StreamQueriesBuilder.class);

    private final List<StreamQuery> queries;
    private Context context;

    private class Context {
        private Source from;
        private Sink to;
        private List<Predicate> predicates;

        private Context() {
            this.from = null;
            this.to = null;
            this.predicates = new LinkedList<>();
        }
    }

    public StreamQueriesBuilder() {
        this.queries = new ArrayList<>();
    }

    @Override
    public void enterQuery(QueryContext ctx) {
        logger.info("Starting query...");
        this.context = new Context();
    }

    @Override
    public void exitQuery(QueryContext ctx) {
        logger.info("Query end!");
        requireNonNull(this.context, "'context' cannot be null");
        requireNonNull(this.context.from, "'context.from' cannot be null");
        requireNonNull(this.context.to, "'context.to' cannot be null");
        this.queries.add(new StreamQuery(this.context.from, this.context.to));
    }

    @Override
    public void exitBetweenPredicate(BetweenPredicateContext ctx) {
        logger.info("Exit predicate BETWEEN!");
        this.context.predicates.add(new FieldPredicate(ctx.IDENTIFIER().getText(), Operator.BETWEEN,
                                                       new RangeValue(loadValue(ctx.value(0)), loadValue(ctx.value(1)))));
    }

    @Override
    public void exitNotExpression(NotExpressionContext ctx) {
        logger.info("Exit predicate NOT!");
        this.context.predicates.add(new NotPredicate(this.context.predicates.removeLast()));
    }

    @Override
    public void exitAndExpression(AndExpressionContext ctx) {
        logger.info("Exit predicate AND!");
        var right = this.context.predicates.removeLast();
        var left = this.context.predicates.removeLast();
        this.context.predicates.add(new AndPredicate(left, right));
    }

    @Override
    public void exitIsNullPredicate(IsNullPredicateContext ctx) {
        this.context.predicates.add(new FieldPredicate(ctx.IDENTIFIER().getText(), Operator.EQUAL, new NullValue()));
    }

    @Override
    public void exitIsNotNullPredicate(IsNotNullPredicateContext ctx) {
        this.context.predicates.add(new FieldPredicate(ctx.IDENTIFIER().getText(), Operator.NOT_EQUAL, new NullValue()));
    }

    @Override
    public void exitInPredicate(InPredicateContext ctx) {
        logger.info("Exit predicate IN!");
        this.context.predicates.add(new FieldPredicate(ctx.IDENTIFIER().getText(),
                                                       Operator.IN,
                                                       new ListValue(ctx.valueList()
                                                                        .literal()
                                                                        .stream()
                                                                        .map(this::loadValue)
                                                                        .toList())));
    }

    @Override
    public void exitComparisonPredicate(ComparisonPredicateContext ctx) {
        logger.info("Exit predicate Compare!");
        requireNonNull(this.context, "'context' cannot be null");
        this.context.predicates.add(new FieldPredicate(ctx.IDENTIFIER().getText(),
                                                       Operator.fromString(ctx.comparator().getText()),
                                                       loadValue(ctx.value())));
    }

    private Value loadValue(ValueContext ctx) {
        if (Objects.nonNull(ctx.literal())) {
            return loadValue(ctx.literal());
        }
        return null;
    }

    private Value loadValue(LiteralContext ctx) {
        if (Objects.nonNull(ctx.STRING())) {
            return new StringValue(unescapeString(ctx.STRING().getText()));
        } else if (Objects.nonNull(ctx.NUMBER())) {
            return new NumberValue(ctx.NUMBER().getText());
        }
        return null;
    }

    private String unescapeString(String value) {
        return value.substring(1, value.length() - 1);
    }

    @Override
    public void exitFrom(FromContext ctx) {
        logger.info("Exit predicate FROM!");
        requireNonNull(this.context, "'context' cannot be null");
        this.context.from = new Source(ctx.sourceTopics()
                                          .topicName()
                                          .stream().map(TopicNameContext::IDENTIFIER)
                                          .map(TerminalNode::getText)
                                          .toList(),
                                       this.context.predicates.isEmpty() ? new NoPredicate() : this.context.predicates.getFirst());
    }

    @Override
    public void exitTo(ToContext ctx) {
        logger.info("Exit predicate TO!");
        requireNonNull(this.context, "'context' cannot be null");
        this.context.to = new Sink(ctx.sinkTopics()
                                      .topicName()
                                      .stream().map(TopicNameContext::IDENTIFIER)
                                      .map(TerminalNode::getText)
                                      .toList());
    }

    public List<StreamQuery> getQueries() {
        return queries;
    }

}
