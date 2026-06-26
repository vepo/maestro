# Branch events

Routes `user_events` to different sink topics based on `event_type`. Demonstrates multi-branch Stream Language syntax.

## Topics

| Topic | Role |
|-------|------|
| `user_events` | Source |
| `purchase_topic` | Purchase branch sink |
| `click_analytics` | Click branch sink |
| `other_events_topic` | Default branch sink |

## Runtime status

**Parser:** supported — `SamplesParseTest.shouldParseMultiBranchPipeline`.

**Engine:** not supported — `UnsupportedStageException` at startup.

**Operator:** `Failed` — see `StreamApplicationReconcilerTest.shouldFailWhenPipelineContainsBranchStage`.

Do not deploy this sample to production until branch stages are wired in `TopologyBuilder`.

## Try parsing only

```java
var model = new StreamTopologyParser().parse(Files.readString(Path.of("samples/branch-events/pipeline.stream")));
```
