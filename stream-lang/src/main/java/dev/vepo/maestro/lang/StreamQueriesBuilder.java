package dev.vepo.maestro.lang;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.antlr.v4.runtime.tree.TerminalNode;

import dev.vepo.maestro.lang.StreamParser.ComparisonPredicateContext;
import dev.vepo.maestro.lang.StreamParser.FromContext;
import dev.vepo.maestro.lang.StreamParser.QueryContext;
import dev.vepo.maestro.lang.StreamParser.ToContext;
import dev.vepo.maestro.lang.StreamParser.TopicNameContext;
import dev.vepo.maestro.lang.StreamParser.ValueContext;
import dev.vepo.maestro.lang.model.Sink;
import dev.vepo.maestro.lang.model.Source;
import dev.vepo.maestro.lang.model.StreamQuery;
import dev.vepo.maestro.lang.model.predicate.FieldPredicate;
import dev.vepo.maestro.lang.model.predicate.NoPredicate;
import dev.vepo.maestro.lang.model.predicate.Operator;
import dev.vepo.maestro.lang.model.predicate.Predicate;
import dev.vepo.maestro.lang.model.predicate.StringValue;
import dev.vepo.maestro.lang.model.predicate.Value;

public class StreamQueriesBuilder extends StreamBaseListener {

    private final List<StreamQuery> queries;
    private Context context;

    private class Context {
        private Source from;
        private Sink to;
        private Predicate predicate;

        private Context() {
            this.from = null;
            this.to = null;
            this.predicate = null;
        }
    }

    public StreamQueriesBuilder() {
        this.queries = new ArrayList<>();
    }

    @Override
    public void enterQuery(QueryContext ctx) {
        this.context = new Context();
    }

    @Override
    public void exitQuery(QueryContext ctx) {
        requireNonNull(this.context, "'context' cannot be null");
        requireNonNull(this.context.from, "'context.from' cannot be null");
        requireNonNull(this.context.to, "'context.to' cannot be null");
        this.queries.add(new StreamQuery(this.context.from, this.context.to));
    }

    @Override
    public void exitComparisonPredicate(ComparisonPredicateContext ctx) {
        requireNonNull(this.context, "'context' cannot be null");
        if (Objects.nonNull(this.context.predicate)) {
            throw new IllegalStateException("Comparison with a non-leaf node!");
        }

        this.context.predicate = new FieldPredicate(ctx.IDENTIFIER().getText(),
                                                    Operator.fromString(ctx.comparator().getText()),
                                                    loadValue(ctx.value()));
    }

    private Value loadValue(ValueContext ctx) {
        if (Objects.nonNull(ctx.literal()) && Objects.nonNull(ctx.literal().STRING())) {
            return new StringValue(unescapeString(ctx.literal().STRING().getText()));
        }
        return null;
    }

    private String unescapeString(String value) {
        return value.substring(1, value.length() - 1);
    }

    @Override
    public void exitFrom(FromContext ctx) {
        requireNonNull(this.context, "'context' cannot be null");
        this.context.from = new Source(ctx.sourceTopics()
                                          .topicName()
                                          .stream().map(TopicNameContext::IDENTIFIER)
                                          .map(TerminalNode::getText)
                                          .toList(),
                                       Optional.ofNullable(this.context.predicate)
                                               .orElseGet(NoPredicate::new));
    }

    @Override
    public void exitTo(ToContext ctx) {
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
