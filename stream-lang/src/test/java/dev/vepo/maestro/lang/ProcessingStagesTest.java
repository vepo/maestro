package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.Assignment;
import dev.vepo.maestro.lang.model.ComparisonExpression;
import dev.vepo.maestro.lang.model.ComparisonOperator;
import dev.vepo.maestro.lang.model.FieldReferenceExpression;
import dev.vepo.maestro.lang.model.FilterStage;
import dev.vepo.maestro.lang.model.FlattenStage;
import dev.vepo.maestro.lang.model.FunctionCallExpression;
import dev.vepo.maestro.lang.model.LiteralExpression;
import dev.vepo.maestro.lang.model.ProjectStage;
import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;
import dev.vepo.maestro.lang.model.StringLiteral;
import dev.vepo.maestro.lang.model.TransformStage;

class ProcessingStagesTest {
    private StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseProjectStage() {
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("input"),
                                                                  List.of(new ProjectStage(List.of("field1", "field2", "field3")))),
                                               "output")),
                     parser.parse("FROM input |> PROJECT field1, field2, field3 TO output"));
    }

    @Test
    void shouldParseFilterStage() {
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("logs"),
                                                                  List.of(new FilterStage(
                                                                                          new ComparisonExpression(
                                                                                                                   new FieldReferenceExpression("level"),
                                                                                                                   ComparisonOperator.EQ,
                                                                                                                   new LiteralExpression(new StringLiteral("ERROR")))))),
                                               "errors")),
                     parser.parse("FROM logs |> WHERE level = \"ERROR\" TO errors"));
    }

    @Test
    void shouldParseTransformStage() {
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("events"),
                                                                  new TransformStage(new Assignment("full_name",
                                                                                                    new FunctionCallExpression("CONCAT",
                                                                                                                               new FieldReferenceExpression("first_name"),
                                                                                                                               new LiteralExpression(new StringLiteral(" ")),
                                                                                                                               new FieldReferenceExpression("last_name"))),
                                                                                     new Assignment("upper_name",
                                                                                                    new FunctionCallExpression("UPPER",
                                                                                                                               new FieldReferenceExpression("full_name"))))),
                                               "transformed")),
                     parser.parse("""
                                  FROM events
                                  |> TRANSFORM full_name = CONCAT(first_name, \" \", last_name),
                                               upper_name = UPPER(full_name)
                                  TO transformed
                                  """));
    }

    @Test
    void shouldParseFlattenStage() {
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("nested_data"),
                                                                  List.of(new FlattenStage("items"))),
                                               "flattened")),
                     parser.parse("FROM nested_data |> FLATTEN items TO flattened"));
    }

    @Test
    void shouldParseMultipleStages() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("input"),
                                                                  new FilterStage(
                                                                                  new ComparisonExpression(new FieldReferenceExpression("status"),
                                                                                                           ComparisonOperator.EQ,
                                                                                                           new LiteralExpression(new StringLiteral("active")))),
                                                                  new ProjectStage(List.of("id", "name", "email")),
                                                                  new TransformStage(List.of(
                                                                                             new Assignment("display_name",
                                                                                                            new FunctionCallExpression("UPPER",
                                                                                                                                       List.of(new FieldReferenceExpression("name"))))))),
                                               "output")),
                     parser.parse("""
                                  FROM input
                                  |> WHERE status = "active"
                                  |> PROJECT id, name, email
                                  |> TRANSFORM display_name = UPPER(name)

                                  TO output
                                  """));
    }
}
