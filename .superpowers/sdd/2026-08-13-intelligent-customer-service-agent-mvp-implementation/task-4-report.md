# Task 4 Implementation Report

## Status

Implemented the streaming chat entrypoint, replaceable `AiGateway` boundary,
deterministic mock intent routing, DashScope-only gateway, and synchronous
named SSE event publisher. Task 1–3 behavior remains covered by the full suite.

## TDD evidence

### Focused RED

Process-local environment used for every Maven command:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot'
$env:MAVEN_HOME='C:\Users\fyq\tools\apache-maven-3.9.16'
$env:PATH="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"
```

Command:

```powershell
.\mvnw.cmd test '-Dtest=IntentRouterTest,ChatControllerSseTest'
```

Result: expected `BUILD FAILURE` during `testCompile` (exit code 1). The
failure was caused by the deliberately missing production boundary:

```text
package com.yike.aftersaleagent.ai does not exist
package com.yike.aftersaleagent.tool does not exist
cannot find symbol: class IntentRouter
cannot find symbol: class AgentExecutionContext
```

This was a feature-missing failure, not a test syntax or environment failure.

### Intermediate dependency/profile diagnosis

The three required BOMs and the DashScope starter resolved from Maven Central.
The first post-implementation context run exposed a separate safe-profile
problem:

```text
DashScope API key must be set. Use the connection property:
spring.ai.dashscope.api-key or spring.ai.dashscope.agent.api-key property.
```

The starter's packaged `META-INF/additional-spring-configuration-metadata.json`
shows that `spring.ai.dashscope.enabled` defaults to `true`. Disabling only the
chat model therefore left the starter's agent auto-configuration active. The
minimal correction was to set `spring.ai.dashscope.enabled: false` in
`application-mock.yml` and make `test` a profile group containing `mock`.
No dependency version was pinned or changed to work around this.

### Focused GREEN

Command:

```powershell
.\mvnw.cmd test '-Dtest=IntentRouterTest,ChatControllerSseTest'
```

Final focused result: `BUILD SUCCESS`, 7 tests run, 0 failures, 0 errors, 0 skipped.

Covered behavior:

- FAQ, coupon, refund, case-insensitive `c1001`, and unsupported routing.
- Exact initial named `status` event and ordered `status` → `message` → `done` events.
- Terminal named `done` event and normal emitter completion for both supported
  and unsupported input.
- Dispatchable terminal event framing with stable `data:[DONE]`.
- Bounded unsupported response text.
- Real owned session use and persistence of only the user message.
- Foreign-session failure preserves `SESSION_NOT_FOUND`, emits a named `error`,
  and then emits terminal `done` without exposing exception details.
- MVC rejection of blank `sessionId` and `message`.
- Exactly one `MockAiGateway` in the active `test` + `mock` context.

## Implementation

### Model boundary and profiles

- `AiGateway` contains only `classifyIntent` and `explain`.
- `MockAiGateway` is active for `default`, `mock`, and `test`, is completely
  deterministic, and makes no network calls.
- Keyword priority is coupon, then FAQ, then refund. This deliberately makes
  `七天无理由退货规则是什么？` an FAQ even though it also contains `退货`.
- The mock `explain` response is deterministic and derived only from its facts
  argument.
- `DashScopeAiGateway` is active only under `dashscope`, constructs a Spring AI
  `ChatClient`, accepts only exact allowed enum names after strip/uppercase
  normalization, and falls back to `UNSUPPORTED` for all other model output.
- `application-dashscope.yml` maps only `${DASHSCOPE_API_KEY}`; no credential is
  embedded or logged.
- No-profile startup selects `mock`; `test` activates `mock` as a profile group,
  yielding the single usable mock gateway and disabling all DashScope
  auto-configuration.

Dependency tree evidence:

```text
spring-ai-alibaba-starter-dashscope:1.1.2.1
spring-ai-alibaba-autoconfigure-dashscope:1.1.2.1
spring-ai-autoconfigure-model-chat-client:1.1.2
spring-ai-client-chat:1.1.2
```

Only the requested DashScope starter was added as an AI dependency. No Agent
Framework, Graph, MCP, vector store, or direct model SDK dependency was added.

### Orchestration and SSE

- `ChatController` validates `ChatRequest`, obtains identity only through
  `DemoUserContext`, and delegates only to `ChatOrchestrator`.
- `DefaultChatOrchestrator` builds `AgentExecutionContext` from MDC request id,
  verified user id, session id, and message; persists through `SessionService`;
  and routes exclusively through `IntentRouter`.
- Supported intents receive only a neutral routing placeholder. Unsupported
  input receives the exact bounded scope statement. No FAQ/coupon/refund
  policy, ticket creation, refund execution, Tool invocation, or mapper call
  was added.
- Package-local `SseEmitterEventPublisher` is the allowed small concrete
  adapter. It emits named `status`, `message`, `error`, and `done` events. Send
  failures close the emitter safely without recursively attempting another
  event.
- Expected project `BusinessException` failures retain their project code and
  safe message. Other unexpected orchestration exceptions emit
  `COMMON_INTERNAL_ERROR` without raw exception details; both paths are followed
  by terminal `done` when the publisher remains writable.

## Full verification

Command:

```powershell
.\mvnw.cmd test
```

Final result: `BUILD SUCCESS`, 15 tests run, 0 failures, 0 errors, 0 skipped.

Suite counts:

- `IntentRouterTest`: 3
- `ChatControllerSseTest`: 4
- Existing Task 1–3 tests: 8

Additional checks:

- `git diff --check`: no whitespace errors.
- Boundary scan: only `DashScopeAiGateway` references `ChatClient`; new
  Controller/router/orchestrator code has no direct SQL, mapper, or Tool call.
- Secret scan: the only key reference is `${DASHSCOPE_API_KEY}`.
- Maven dependency tree completed successfully with BOM-managed versions.

The build emits existing JDK/Mockito dynamic-agent deprecation warnings; these
do not represent test failures and are outside Task 4 scope.

## Files

Created required production contracts and implementations under:

- `src/main/java/com/yike/aftersaleagent/agent/IntentRouter.java`
- `src/main/java/com/yike/aftersaleagent/tool/AgentExecutionContext.java`
- `src/main/java/com/yike/aftersaleagent/ai/`
- `src/main/java/com/yike/aftersaleagent/chat/api/`
- `src/main/java/com/yike/aftersaleagent/chat/ChatController.java`
- `src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java`
- `src/main/java/com/yike/aftersaleagent/chat/SseEventPublisher.java`

Allowed narrow concrete implementations added:

- `src/main/java/com/yike/aftersaleagent/chat/DefaultChatOrchestrator.java`
- `src/main/java/com/yike/aftersaleagent/chat/SseEmitterEventPublisher.java`

Created profile resources and required tests; modified only `pom.xml` and
`application.yml` outside those files.

## Self-review

- Exact public record component names/order and interface method signatures
  match the brief.
- Identity cannot be supplied in the chat request and always comes from the
  verified interceptor-backed context.
- Controller and orchestration layers do not access SQL/mappers or AI clients
  directly; the AI dependency is behind `AiGateway` and intent routing behind
  `IntentRouter`.
- Only the three bounded future intents plus unsupported exist; no additional
  flow or autonomous behavior was introduced.
- Every ordinary stream path calls named `done`; user-message persistence is
  asserted and assistant-message persistence is intentionally absent.
- No API key, password, `.env`, Docker, WSL, or system configuration change is
  included.

## Unverified external hand-off

`DASHSCOPE_API_KEY` was not set and Docker/WSL is unavailable. Therefore no
real DashScope network call is claimed. To verify later, provide the key only
as the process environment variable, start with the explicit `dashscope`
profile, and exercise a bounded classification plus explanation while checking
that only `DashScopeAiGateway` is selected. Do not commit the key or command
history containing its value.

## Fix round 1

### Finding and root cause

Review found that profile exclusivity was not enforced for combined profiles.
The original `@Profile({"default", "mock", "test"})` uses OR semantics, so
`dashscope,mock` and `dashscope,test` still created `MockAiGateway` alongside
`DashScopeAiGateway`. Also, `application-mock.yml` loaded whenever `mock` was
active and set `spring.ai.dashscope.enabled=false`, so the mock configuration
could suppress DashScope auto-configuration even when `dashscope` was explicit.

### Focused RED

Command, with the same process-local Java 21 and Maven setup documented above:

```powershell
.\mvnw.cmd test '-Dtest=AiProfileSelectionTest'
```

Result: expected `BUILD FAILURE`, 8 tests run, 4 failures, 0 errors. Both
combined-profile component cases found one unexpected `MockAiGateway`, and
both combined-profile config-data cases resolved
`spring.ai.dashscope.enabled=false` instead of allowing DashScope enablement.

The test uses real Spring profile conditions and Boot config-data loading. A
test-only `ChatClient.Builder` double allows component selection to be checked
without constructing a real model client, reading an API key, or making a
network call. Real DashScope execution remains intentionally unverified because
`DASHSCOPE_API_KEY` is not present.

### Fix

- Changed the mock component condition to
  `!dashscope & (default | mock | test)`, so explicit DashScope always excludes
  `MockAiGateway`.
- Added `spring.config.activate.on-profile: "mock & !dashscope"` to
  `application-mock.yml`, so mock-only provider disabling cannot override an
  explicit DashScope profile.
- Added `AiProfileSelectionTest`, which proves exactly one selected gateway for
  no explicit profile, `mock`, `test`, `dashscope`, `dashscope,mock`, and
  `dashscope,test`. It also proves effective DashScope enablement wins for both
  combined-profile configurations.

### Verification

Covering command:

```powershell
.\mvnw.cmd test '-Dtest=AiProfileSelectionTest,ChatControllerSseTest'
```

Result: `BUILD SUCCESS`, 12 tests run, 0 failures, 0 errors, 0 skipped.

Full command:

```powershell
.\mvnw.cmd test
```

Result: `BUILD SUCCESS`, 23 tests run, 0 failures, 0 errors, 0 skipped.

Files changed in this fix round:

- `src/main/java/com/yike/aftersaleagent/ai/MockAiGateway.java`
- `src/main/resources/application-mock.yml`
- `src/test/java/com/yike/aftersaleagent/ai/AiProfileSelectionTest.java`
- `.superpowers/sdd/2026-08-13-intelligent-customer-service-agent-mvp-implementation/task-4-report.md`
