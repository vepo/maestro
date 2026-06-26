package dev.vepo.maestro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.StreamTopologyParser;
import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;
import dev.vepo.maestro.parser.model.LiteralExpression;
import dev.vepo.maestro.parser.model.StringLiteral;

class SamplesApiParityTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldBuildSameModelAsParserForBasicPipeline() {
        var dsl = """
                  FROM input_topic
                  |> FILTER WHERE status = 'active'
                  |> PROJECT fields: user_id, name, email
                  |> TO output_topic
                  """;
        var expected = parser.parse(dsl);
        var actual = Maestro.stream()
                            .from("input_topic")
                            .filterWhere(new ComparisonExpression(
                                                                  new FieldReferenceExpression("status"),
                                                                  ComparisonOperator.EQ,
                                                                  new LiteralExpression(new StringLiteral("active"))))
                            .projectFields("user_id", "name", "email")
                            .to("output_topic")
                            .build();
        assertEquals(expected, actual);
    }
}
