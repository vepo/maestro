# Java SDK sample

Build the same topology as [filter-active-users](../filter-active-users/) and [project-user-fields](../project-user-fields/) using the fluent API instead of Stream Language text.

## Equivalent to DSL

```text
FROM input_topic
|> FILTER WHERE status = 'active'
|> PROJECT fields: user_id, name, email
|> TO output_topic
```

## Code

```java
import java.util.Map;

import dev.vepo.maestro.api.Maestro;
import dev.vepo.maestro.engine.MaestroApplication;
import dev.vepo.maestro.engine.MaestroConfigs;
import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;
import dev.vepo.maestro.parser.model.LiteralExpression;
import dev.vepo.maestro.parser.model.StringLiteral;

public final class BasicPipeline {
    public static void main(String[] args) {
        var model = Maestro.stream()
            .from("input_topic")
            .filterWhere(new ComparisonExpression(
                new FieldReferenceExpression("status"),
                ComparisonOperator.EQ,
                new LiteralExpression(new StringLiteral("active"))))
            .projectFields("user_id", "name", "email")
            .to("output_topic")
            .build();

        var configs = new MaestroConfigs(Map.of(
            "bootstrap.servers", "localhost:9092",
            "application.id", "sdk-basic-pipeline"));

        try (var app = new MaestroApplication(model, configs)) {
            app.start();
        }
    }
}
```

## Parity test

`SamplesApiParityTest.shouldBuildSameModelAsParserForBasicPipeline` asserts SDK output equals `StreamTopologyParser` output for the same topology.

## When to use the SDK

- Embed Maestro inside an existing Java service
- Generate topologies programmatically from configuration
- Avoid shipping DSL strings when compile-time types are preferred

For CLI and GitOps workflows, prefer `.stream` files or CR `spec.pipeline` strings.
