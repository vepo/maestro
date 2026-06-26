package dev.vepo.maestro.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import dev.vepo.maestro.parser.model.AggregateFunction;
import dev.vepo.maestro.parser.model.AggregateStage;
import dev.vepo.maestro.parser.model.Assignment;
import dev.vepo.maestro.parser.model.BranchCase;
import dev.vepo.maestro.parser.model.BranchStage;
import dev.vepo.maestro.parser.model.Duration;
import dev.vepo.maestro.parser.model.Expression;
import dev.vepo.maestro.parser.model.FilterStage;
import dev.vepo.maestro.parser.model.GroupByStage;
import dev.vepo.maestro.parser.model.JoinKind;
import dev.vepo.maestro.parser.model.JoinStage;
import dev.vepo.maestro.parser.model.MapStage;
import dev.vepo.maestro.parser.model.PatternDefinition;
import dev.vepo.maestro.parser.model.PatternStage;
import dev.vepo.maestro.parser.model.ProcessingStage;
import dev.vepo.maestro.parser.model.ProjectField;
import dev.vepo.maestro.parser.model.ProjectStage;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.QuerySettings;
import dev.vepo.maestro.parser.model.SessionizeStage;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.parser.model.TimeUnit;
import dev.vepo.maestro.parser.model.ToStage;
import dev.vepo.maestro.parser.model.WindowStage;
import dev.vepo.maestro.parser.model.WindowType;

public final class Maestro {
    public static final class BranchBuilder {
        private final List<BranchCase> cases = new ArrayList<>();

        BranchStage build() {
            return new BranchStage(List.copyOf(cases));
        }

        public BranchBuilder caseWhen(Expression condition, Consumer<StreamBuilder> pipeline) {
            var nested = new StreamBuilder();
            pipeline.accept(nested);
            cases.add(BranchCase.when(condition, nested.pipelineStages()));
            return this;
        }

        public BranchBuilder defaultCase(Consumer<StreamBuilder> pipeline) {
            var nested = new StreamBuilder();
            pipeline.accept(nested);
            cases.add(BranchCase.defaults(nested.pipelineStages()));
            return this;
        }
    }

    public static final class PatternBuilder {
        private final List<PatternDefinition> definitions = new ArrayList<>();

        PatternStage build(String detectAlias) {
            return new PatternStage(List.copyOf(definitions), detectAlias);
        }

        public PatternBuilder define(String name, Expression expression) {
            definitions.add(new PatternDefinition(name, expression, Optional.empty(), Optional.empty()));
            return this;
        }

        public PatternBuilder defineWithinAfter(String name, Expression expression, Duration within, String after) {
            definitions.add(new PatternDefinition(name, expression, Optional.of(within), Optional.of(after)));
            return this;
        }
    }

    public static final class StreamBuilder {
        public static Duration duration(long amount, TimeUnit unit) {
            return new Duration(amount, unit);
        }

        private String sourceTopic;

        private final List<ProcessingStage> stages = new ArrayList<>();

        private QuerySettings settings = QuerySettings.empty();

        public StreamBuilder aggregate(AggregateFunction... functions) {
            stages.add(new AggregateStage(Arrays.asList(functions)));
            return this;
        }

        public StreamBuilder applicationId(String applicationId) {
            return setting("application.id", applicationId);
        }

        public StreamBuilder bootstrapServers(String bootstrapServers) {
            return setting("bootstrap.servers", bootstrapServers);
        }

        public StreamBuilder branch(Consumer<BranchBuilder> config) {
            var builder = new BranchBuilder();
            config.accept(builder);
            stages.add(builder.build());
            return this;
        }

        public StreamModel build() {
            var sinkTopics = stages.stream()
                                   .filter(ToStage.class::isInstance)
                                   .flatMap(s -> ((ToStage) s).topics().stream())
                                   .toList();
            var processing = stages.stream().filter(s -> !(s instanceof ToStage)).toList();
            var query = new Query(new SourcePipeline(new SourceStage(sourceTopic), processing), sinkTopics, settings);
            return new StreamModel(query);
        }

        public StreamBuilder defaultKeySerde(String className) {
            return setting("default.key.serde", className);
        }

        public StreamBuilder defaultValueSerde(String className) {
            return setting("default.value.serde", className);
        }

        public StreamBuilder filterWhere(Expression condition) {
            stages.add(new FilterStage(condition));
            return this;
        }

        public StreamBuilder from(String topic) {
            this.sourceTopic = topic;
            return this;
        }

        public StreamBuilder groupBy(String... fields) {
            stages.add(new GroupByStage(Arrays.asList(fields)));
            return this;
        }

        public StreamBuilder joinLookup(String target, Expression condition, String topic) {
            stages.add(new JoinStage(target, condition, JoinKind.LOOKUP_TABLE, Optional.of(topic), Optional.empty()));
            return this;
        }

        public StreamBuilder joinStream(String target, Expression condition, String topic) {
            stages.add(new JoinStage(target, condition, JoinKind.STREAM, Optional.of(topic), Optional.empty()));
            return this;
        }

        public StreamBuilder joinStreamWithin(String target, Expression condition, Duration within) {
            stages.add(new JoinStage(target, condition, JoinKind.STREAM_WITHIN, Optional.empty(), Optional.of(within)));
            return this;
        }

        public StreamBuilder mapSet(String field, Expression expression) {
            stages.add(new MapStage(List.of(new Assignment(field, expression))));
            return this;
        }

        public StreamBuilder mapSets(Assignment... assignments) {
            stages.add(new MapStage(Arrays.asList(assignments)));
            return this;
        }

        public StreamBuilder pattern(String detectAlias, Consumer<PatternBuilder> config) {
            var builder = new PatternBuilder();
            config.accept(builder);
            stages.add(builder.build(detectAlias));
            return this;
        }

        List<ProcessingStage> pipelineStages() {
            return List.copyOf(stages);
        }

        public StreamBuilder project(String alias, String... fields) {
            stages.add(new ProjectStage(Optional.of(alias),
                                        Arrays.stream(fields).map(ProjectField::new).toList()));
            return this;
        }

        public StreamBuilder projectField(String name, Expression expression) {
            stages.add(new ProjectStage(Optional.empty(), List.of(new ProjectField(name, Optional.of(expression)))));
            return this;
        }

        public StreamBuilder projectFields(String... fields) {
            return project("fields", fields);
        }

        public StreamBuilder projectFieldsOnly(String... fields) {
            stages.add(new ProjectStage(Optional.empty(), Arrays.stream(fields).map(ProjectField::new).toList()));
            return this;
        }

        public StreamBuilder projectNamedField(String field) {
            stages.add(new ProjectStage(Optional.empty(), List.of(new ProjectField(field))));
            return this;
        }

        public StreamBuilder projectOnly(ProjectField... fields) {
            stages.add(new ProjectStage(Optional.empty(), Arrays.asList(fields)));
            return this;
        }

        public StreamBuilder sessionizeBy(Duration gap, Duration timeout, String... fields) {
            stages.add(new SessionizeStage(Arrays.asList(fields), gap, Optional.of(timeout)));
            return this;
        }

        public StreamBuilder sessionizeBy(Duration gap, String... fields) {
            stages.add(new SessionizeStage(Arrays.asList(fields), gap, Optional.empty()));
            return this;
        }

        public StreamBuilder setting(String key, String value) {
            settings = settings.merge(QuerySettings.of(key, value));
            return this;
        }

        public StreamBuilder to(String... topics) {
            stages.add(new ToStage(topics));
            return this;
        }

        public StreamBuilder windowHopping(Duration size, Duration advance) {
            stages.add(new WindowStage(WindowType.HOPPING, size, Optional.of(advance)));
            return this;
        }

        public StreamBuilder windowSliding(Duration size) {
            stages.add(new WindowStage(WindowType.SLIDING, size, Optional.empty()));
            return this;
        }

        public StreamBuilder windowTumbling(Duration size) {
            stages.add(new WindowStage(WindowType.TUMBLING, size, Optional.empty()));
            return this;
        }
    }

    public static StreamBuilder stream() {
        return new StreamBuilder();
    }

    private Maestro() {}
}
