package dev.vepo.maestro.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.vepo.maestro.parser.model.FilterStage;
import dev.vepo.maestro.parser.model.GroupByStage;
import dev.vepo.maestro.parser.model.ProcessingStage;
import dev.vepo.maestro.parser.model.ProjectField;
import dev.vepo.maestro.parser.model.ProjectStage;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.parser.model.ToStage;

public final class Maestro {
    public static final class StreamBuilder {
        private String sourceTopic;
        private final List<ProcessingStage> stages = new ArrayList<>();

        public StreamModel build() {
            var sinkTopics = stages.stream()
                                   .filter(ToStage.class::isInstance)
                                   .flatMap(s -> ((ToStage) s).topics().stream())
                                   .toList();
            var processing = stages.stream().filter(s -> !(s instanceof ToStage)).toList();
            var query = new Query(new SourcePipeline(new SourceStage(sourceTopic), processing), sinkTopics);
            return new StreamModel(query);
        }

        public StreamBuilder filterWhere(dev.vepo.maestro.parser.model.Expression condition) {
            stages.add(new FilterStage(condition));
            return this;
        }

        public StreamBuilder from(String topic) {
            this.sourceTopic = topic;
            return this;
        }

        public StreamBuilder groupBy(String... fields) {
            stages.add(new GroupByStage(fields));
            return this;
        }

        public StreamBuilder projectFields(String... fields) {
            stages.add(new ProjectStage(Optional.of("fields"),
                                        Arrays.stream(fields).map(ProjectField::new).toList()));
            return this;
        }

        public StreamBuilder to(String... topics) {
            stages.add(new ToStage(topics));
            return this;
        }
    }

    public static StreamBuilder stream() {
        return new StreamBuilder();
    }

    private Maestro() {}
}
