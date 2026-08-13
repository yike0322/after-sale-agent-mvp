# 智能售后工单 Agent 平台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 在一个月内交付可本地运行、可验证、可演示的电商售后受控 Agent 单体 MVP，包含 RAG、业务 Tool、工单状态机、SSE 和审计链路。

**Architecture:** 单体 Spring Boot 应用使用 ChatOrchestrator 路由请求至 RAG、优惠券分析或退款工单工作流。模型只负责受限分类、解释和摘要；所有业务事实经 Java 领域规则和受治理 Tool 获取。MySQL 持久化业务与审计，Redis 保存短期状态，Qdrant 保存知识向量；SSE 展示过程，异步任务执行退款工单。

**Tech Stack:** Java 21、Maven 3.9+、Spring Boot 3.5.8、Spring MVC、Spring AI 1.1.2、Spring AI Alibaba BOM 1.1.2.0、Spring AI Alibaba Extensions BOM 1.1.2.1、MyBatis-Plus 3.5.17、Flyway、MySQL 8.4、Redis 7、Qdrant、Docker Compose、JUnit 5、MockMvc。

## Global Constraints

- 所有 Java 源码位于 com.yike.aftersaleagent；Maven 坐标为 com.yike:after-sale-agent:0.0.1-SNAPSHOT。
- 只实现三个场景：售后 FAQ、优惠券不可用、退款资格加 WAIT_HUMAN 工单；不实现真实退款、投诉、物流、微服务、MQ 或 MCP Server。
- 模型、Tool 或 Controller 不得直接执行 SQL；仅 Mapper 可以访问 MySQL。
- 不信任请求或模型提供的 userId；用户身份必须来自 X-Demo-User-Id 并写入 CurrentDemoUser。
- 任何退款路径只能创建或更新 WAIT_HUMAN 工单，绝不调用资金、支付或账户修改能力。
- API Key、数据库密码和 .env 绝不提交；所有 profile 只读取环境变量。
- 每项先写失败测试、确认失败、最小实现、确认通过，再提交；每项提交前运行相关测试和 mvnw.cmd test。
- Spring AI 与 Spring AI Alibaba 只通过 BOM 管理；不引入未使用的 Agent Framework、Graph、MCP 或第二种向量数据库。
- Docker Desktop 未准备好时，先完成 H2 test profile 下的基础任务；Redis/Qdrant 实机验证在 Docker 可用后执行。

---

## File Structure

| 路径 | 责任 | 首次任务 |
| --- | --- | --- |
| pom.xml | 依赖、BOM、测试插件和 Java 21 编译 | 2 |
| AGENTS.md、.gitignore、.env.example | 后续 Agent 规则、密钥保护和环境模板 | 1 |
| docker-compose.yml | MySQL、Redis、Qdrant 固定本地依赖 | 2 |
| src/main/java/com/yike/aftersaleagent/common | 响应、错误码、异常、requestId | 2 |
| src/main/java/com/yike/aftersaleagent/identity | Demo 用户上下文和身份拦截 | 3 |
| src/main/java/com/yike/aftersaleagent/chat | 会话、消息、SSE 和编排入口 | 3、4 |
| src/main/java/com/yike/aftersaleagent/ai | 模型网关、Mock/DashScope profile、分类 | 4 |
| src/main/java/com/yike/aftersaleagent/knowledge | 文档索引、RAG、引用和评测 | 5 |
| src/main/java/com/yike/aftersaleagent/tool | Tool Registry、权限校验和审计 | 6 |
| src/main/java/com/yike/aftersaleagent/coupon | 优惠券领域与规则工作流 | 6、7 |
| src/main/java/com/yike/aftersaleagent/ticket | 工单、状态机和异步退款工作流 | 8 |
| src/main/resources/db/migration | Flyway 迁移 | 3 |
| src/main/resources/knowledge | 内置 Markdown 规则文档 | 5 |
| src/main/resources/static | 无框架演示页 | 9 |
| src/test/java/com/yike/aftersaleagent | 单元、MVC、工作流及集成测试 | 2–10 |
| README.md、docs | 启动、评测、演示、面试材料 | 1、5、10 |

## Shared Contracts

后续任务必须保持以下类型名、包名和职责一致。

    package com.yike.aftersaleagent.agent;

    public enum Intent {
        FAQ_QUERY,
        COUPON_ANALYSIS,
        REFUND_ELIGIBILITY,
        UNSUPPORTED
    }

    package com.yike.aftersaleagent.chat.api;

    public record ChatRequest(String sessionId, String message) { }

    public record SourceCitation(String sourceTitle, String sourcePath, String excerpt) { }

    public record ChatOutcome(
            String reply,
            Long ticketId,
            String taskStatus,
            java.util.List<SourceCitation> citations) { }

    package com.yike.aftersaleagent.tool;

    public record AgentExecutionContext(
            String requestId,
            long userId,
            String sessionId,
            String userMessage) { }

    public interface GovernedTool<I, O> {
        String name();
        ToolRisk risk();
        O execute(AgentExecutionContext context, I input);
    }

    package com.yike.aftersaleagent.ticket.domain;

    public enum TicketTaskStatus {
        PENDING, RUNNING, WAIT_HUMAN, FINISHED, FAILED
    }

### Task 1: 准备 Windows 工具链与项目防护规则

**Files:**

- Create: .gitignore
- Create: .env.example
- Create: AGENTS.md
- Create: docs/setup/windows-development.md
- Modify: docs/superpowers/specs/2026-08-13-intelligent-customer-service-agent-mvp-design.md

**Interfaces:**

- Consumes: 当前 Windows 环境（JRE 8、无 javac、无 Maven、无 Docker）。
- Produces: Java 21、Maven 3.9+、Docker Desktop/Compose 可用；后续任务可遵守的项目规则与不含秘密的模板。

- [ ] **Step 1: 记录当前工具链缺口**

Run:

    java -version
    javac -version
    mvn -version
    docker version
    docker compose version

Expected: java 显示 1.8.x，javac、Maven、Docker 至少一项不可用。将摘要写入 docs/setup/windows-development.md 的“初始检查”小节。

- [ ] **Step 2: 经用户批准后安装最小开发工具**

Run each command independently so Windows 安装器的权限与重启提示可见：

    winget install --id EclipseAdoptium.Temurin.21.JDK --exact --accept-source-agreements --accept-package-agreements
    winget install --id Apache.Maven --exact --accept-source-agreements --accept-package-agreements
    winget install --id Docker.DockerDesktop --exact --accept-source-agreements --accept-package-agreements

Do not install database clients、IDE plugins 或其他开发工具。Docker Desktop 提示重启或启用 WSL 时，先请用户完成该操作。

- [ ] **Step 3: 验证安装而不是只相信安装器**

Close and reopen the terminal, then run:

    java -version
    javac -version
    mvn -version
    docker version
    docker compose version
    docker run --rm hello-world

Expected: Java 21、Maven 3.9+、Docker Client/Server 与 Compose 均有版本输出，hello-world 以退出码 0 完成。

- [ ] **Step 4: 写入仓库规则和环境模板**

Create .gitignore with:

    target/
    .idea/
    .vscode/
    .env
    *.log
    *.iml

Create .env.example with:

    MYSQL_DATABASE=after_sale_agent
    MYSQL_USER=agent_app
    MYSQL_PASSWORD=change-me-locally
    MYSQL_ROOT_PASSWORD=change-me-locally
    REDIS_HOST=localhost
    REDIS_PORT=6379
    QDRANT_HOST=localhost
    QDRANT_GRPC_PORT=6334
    DASHSCOPE_API_KEY=

AGENTS.md must state the Java package, Maven commands, no-secret rule, direct-SQL prohibition, Tool governance rules, and required failing-test-first workflow. Update the design technical-baseline row to Spring Boot 3.5.8, Spring AI 1.1.2, SAA BOM 1.1.2.0, Extensions BOM 1.1.2.1.

- [ ] **Step 5: Verify hygiene and commit**

Run:

    git check-ignore .env
    git status --short
    git add .gitignore .env.example AGENTS.md docs/setup/windows-development.md docs/superpowers/specs/2026-08-13-intelligent-customer-service-agent-mvp-design.md
    git commit -m "chore: prepare local development environment"

Expected: .env is ignored, no real secret is staged, and the commit only contains setup guidance.

### Task 2: 建立可启动 Spring Boot 基线与基础设施编排

**Files:**

- Create: pom.xml
- Create: mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
- Create: src/main/java/com/yike/aftersaleagent/AfterSaleAgentApplication.java
- Create: src/main/java/com/yike/aftersaleagent/common/api/ApiResponse.java
- Create: src/main/java/com/yike/aftersaleagent/common/api/ErrorCode.java
- Create: src/main/java/com/yike/aftersaleagent/common/exception/BusinessException.java
- Create: src/main/java/com/yike/aftersaleagent/common/exception/GlobalExceptionHandler.java
- Create: src/main/java/com/yike/aftersaleagent/common/trace/RequestIdFilter.java
- Create: src/main/resources/application.yml
- Create: src/main/resources/application-local.yml
- Create: src/main/resources/application-test.yml
- Create: docker-compose.yml
- Create: src/test/java/com/yike/aftersaleagent/HealthEndpointTest.java

**Interfaces:**

- Consumes: Task 1 toolchain and untracked .env copied from .env.example.
- Produces: GET /actuator/health、统一错误 JSON、X-Request-Id 响应头、可启动 MySQL/Redis/Qdrant Compose 环境。

- [ ] **Step 1: 写健康检查失败测试**

Create before application code:

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @AutoConfigureMockMvc
    class HealthEndpointTest {
        @Autowired MockMvc mockMvc;

        @Test
        void healthEndpointReturnsUpAndRequestId() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(header().exists("X-Request-Id"));
        }
    }

Run: ./mvnw.cmd test -Dtest=HealthEndpointTest  
Expected: FAIL because the Maven project and application class do not exist.

- [ ] **Step 2: 创建 Maven 项目和最小应用**

Use Java release 21 and Spring Boot parent 3.5.8. Add Web、Validation、Actuator、MyBatis-Plus starter 3.5.17、MySQL Connector/J、Flyway Core、Flyway MySQL、Redis Starter、Lombok (provided)、H2 (test)、Spring Boot Test and spring-boot-maven-plugin. Generate Maven Wrapper.

Use entry point:

    @SpringBootApplication
    public class AfterSaleAgentApplication {
        public static void main(String[] args) {
            SpringApplication.run(AfterSaleAgentApplication.class, args);
        }
    }

Define ApiResponse success(data, requestId) and failure(code, message, requestId). RequestIdFilter uses incoming nonblank X-Request-Id or a UUID, adds it to MDC as requestId, then returns it in the response header.

- [ ] **Step 3: 配置 profile 和 Compose 服务**

application-test.yml uses H2 in MySQL mode with Flyway enabled. application-local.yml uses only environment variables for MySQL, Redis and Qdrant.

Create tagged Compose services and named volumes:

    services:
      mysql:
        image: mysql:8.4
        ports: ["3306:3306"]
      redis:
        image: redis:7.4-alpine
        ports: ["6379:6379"]
      qdrant:
        image: qdrant/qdrant:v1.13.4
        ports: ["6333:6333", "6334:6334"]

Use MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD in the MySQL service. Configure health checks for MySQL and Redis and no literal credentials.

- [ ] **Step 4: Verify baseline**

Run:

    Copy-Item .env.example .env
    # Replace the two local change-me values in .env with secrets before running Compose.
    docker compose --env-file .env up -d
    ./mvnw.cmd test -Dtest=HealthEndpointTest
    ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test"

Expected: test passes under H2; application starts and /actuator/health returns UP. Stop the foreground app after this check.

- [ ] **Step 5: Commit baseline**

Run:

    git add pom.xml mvnw mvnw.cmd .mvn docker-compose.yml src/main
    git add src/test/java/com/yike/aftersaleagent/HealthEndpointTest.java
    git commit -m "feat: bootstrap Spring Boot service"

### Task 3: 建立数据模型、Demo 身份和会话 API

**Files:**

- Create: src/main/resources/db/migration/V1__create_mvp_schema.sql
- Create: src/main/java/com/yike/aftersaleagent/identity/CurrentDemoUser.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoUserContext.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoUserInterceptor.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoUserMapper.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoDataInitializer.java
- Create: src/main/java/com/yike/aftersaleagent/chat/domain/UserSession.java
- Create: src/main/java/com/yike/aftersaleagent/chat/domain/ChatMessage.java
- Create: src/main/java/com/yike/aftersaleagent/agent/Intent.java
- Create: src/main/java/com/yike/aftersaleagent/chat/SessionMapper.java
- Create: src/main/java/com/yike/aftersaleagent/chat/SessionService.java
- Create: src/main/java/com/yike/aftersaleagent/chat/SessionController.java
- Create: src/test/java/com/yike/aftersaleagent/chat/SessionControllerTest.java
- Create: src/test/java/com/yike/aftersaleagent/identity/DemoUserInterceptorTest.java

**Interfaces:**

- Consumes: Task 2 common API, requestId, Flyway and H2 profile.
- Produces: CurrentDemoUser、POST /api/sessions、GET /api/sessions/{sessionId}/messages，以及后续任务所需的持久化表。

- [ ] **Step 1: 写身份边界失败测试**

    mockMvc.perform(post("/api/sessions"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("DEMO_USER_REQUIRED"));

    mockMvc.perform(post("/api/sessions").header("X-Demo-User-Id", "99999"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_USER_NOT_FOUND"));

    mockMvc.perform(post("/api/sessions").header("X-Demo-User-Id", "10001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId").isNotEmpty());

Run: ./mvnw.cmd test -Dtest=DemoUserInterceptorTest,SessionControllerTest  
Expected: FAIL because migration、interceptor、mapper and endpoints do not exist.

- [ ] **Step 2: 创建可迁移 schema**

V1 creates demo_user, user_session, chat_message, knowledge_doc, knowledge_chunk, order_info, coupon_info, customer_ticket, ticket_task, agent_step_log and tool_call_log. Every table has a primary key and created_at/updated_at where meaningful. Use VARCHAR for state values, DECIMAL(10,2) for money, TEXT for audit summaries, no database-specific JSON types and no foreign keys.

V1 must define these indexes:

    CREATE UNIQUE INDEX uk_demo_user_id ON demo_user(id);
    CREATE UNIQUE INDEX uk_order_user_no ON order_info(user_id, order_no);
    CREATE UNIQUE INDEX uk_coupon_user_code ON coupon_info(user_id, coupon_code);
    CREATE UNIQUE INDEX uk_ticket_task_idempotency ON ticket_task(idempotency_key);
    CREATE INDEX idx_chat_message_session_created ON chat_message(session_id, created_at);
    CREATE INDEX idx_tool_call_ticket_created ON tool_call_log(ticket_id, created_at);

- [ ] **Step 3: 实现受限 Demo 用户和种子数据**

    public record CurrentDemoUser(long id, String displayName) { }

    public interface DemoUserContext {
        CurrentDemoUser requireCurrentUser();
    }

DemoUserInterceptor reads only X-Demo-User-Id, verifies it using DemoUserMapper, writes CurrentDemoUser to a request attribute and clears request state afterward. DemoDataInitializer seeds only if demo_user is empty: users 10001 and 10002; O1001 for user 10001 with amount 80.00 and coupon C1001 threshold 100.00; O2001 for user 10002, NORMAL product and receivedAt equal to injected Clock minus two days.

- [ ] **Step 4: 实现会话与消息服务**

    public interface SessionService {
        String createSession(CurrentDemoUser user);
        List<ChatMessage> listMessages(CurrentDemoUser user, String sessionId);
        void appendUserMessage(CurrentDemoUser user, String sessionId, String content);
        void appendAssistantMessage(CurrentDemoUser user, String sessionId, String content, Intent intent);
    }

SessionController gets user only from DemoUserContext. Return SESSION_NOT_FOUND when the session is absent or not owned by that user.

- [ ] **Step 5: Verify and commit**

Run:

    ./mvnw.cmd test -Dtest=DemoUserInterceptorTest,SessionControllerTest
    ./mvnw.cmd test
    git add src/main/java/com/yike/aftersaleagent/identity src/main/java/com/yike/aftersaleagent/chat
    git add src/main/resources/db/migration/V1__create_mvp_schema.sql src/test/java/com/yike/aftersaleagent
    git commit -m "feat: add demo identity and chat sessions"

Expected: Flyway applies V1 once and all tests pass.

### Task 4: 实现 SSE 聊天入口、模型抽象和受限意图路由

**Files:**

- Create: src/main/java/com/yike/aftersaleagent/agent/IntentRouter.java
- Create: src/main/java/com/yike/aftersaleagent/tool/AgentExecutionContext.java
- Create: src/main/java/com/yike/aftersaleagent/ai/AiGateway.java
- Create: src/main/java/com/yike/aftersaleagent/ai/MockAiGateway.java
- Create: src/main/java/com/yike/aftersaleagent/ai/DashScopeAiGateway.java
- Create: src/main/java/com/yike/aftersaleagent/chat/api/ChatRequest.java
- Create: src/main/java/com/yike/aftersaleagent/chat/api/ChatOutcome.java
- Create: src/main/java/com/yike/aftersaleagent/chat/api/SourceCitation.java
- Create: src/main/java/com/yike/aftersaleagent/chat/SseEventPublisher.java
- Create: src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java
- Create: src/main/java/com/yike/aftersaleagent/chat/ChatController.java
- Modify: pom.xml
- Create: src/main/resources/application-mock.yml
- Create: src/main/resources/application-dashscope.yml
- Create: src/test/java/com/yike/aftersaleagent/agent/IntentRouterTest.java
- Create: src/test/java/com/yike/aftersaleagent/chat/ChatControllerSseTest.java

**Interfaces:**

- Consumes: CurrentDemoUser、SessionService、AgentExecutionContext、common error handling.
- Produces: POST /api/chat/stream with status and done SSE events; deterministic mock profile; IntentRouter.route(context).

- [ ] **Step 1: 写路由和 SSE 协议失败测试**

    assertThat(intentRouter.route(context("七天无理由退货规则是什么？")))
            .isEqualTo(Intent.FAQ_QUERY);
    assertThat(intentRouter.route(context("优惠券 C1001 为什么不能用？")))
            .isEqualTo(Intent.COUPON_ANALYSIS);
    assertThat(intentRouter.route(context("商品坏了，我要退款")))
            .isEqualTo(Intent.REFUND_ELIGIBILITY);

    mockMvc.perform(post("/api/chat/stream")
                    .header("X-Demo-User-Id", "10001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sessionId\":\"known-session\",\"message\":\"你好\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

Run: ./mvnw.cmd test -Dtest=IntentRouterTest,ChatControllerSseTest  
Expected: FAIL because no router、gateway or SSE endpoint exists.

- [ ] **Step 2: 创建供应商可替换模型边界**

Add Spring AI BOM 1.1.2, Spring AI Alibaba BOM 1.1.2.0, Extensions BOM 1.1.2.1 and spring-ai-alibaba-starter-dashscope to pom.xml. Do not add Agent Framework or Graph.

    public interface AiGateway {
        Intent classifyIntent(String message);
        String explain(String systemInstruction, String facts);
    }

MockAiGateway is active in mock profile and classifies case-insensitive keywords: 优惠券、满减、C1001 → coupon; 退款、退货、商品坏 → refund; 规则、七天、售后 → FAQ; otherwise unsupported. DashScopeAiGateway is active only in dashscope profile, constructs Spring AI ChatClient, and reads DASHSCOPE_API_KEY only from environment.

- [ ] **Step 3: 实现 SSE 编排入口**

    public interface SseEventPublisher {
        void status(String text);
        void message(ChatOutcome outcome);
        void error(String code, String text);
        void done();
    }

    public interface ChatOrchestrator {
        SseEmitter stream(ChatRequest request, CurrentDemoUser user);
    }

ChatOrchestrator saves the user message, emits “正在识别您的问题…”, calls IntentRouter and, for UNSUPPORTED, emits bounded fallback plus done. It never embeds a business rule or calls a Mapper. ChatController validates nonblank sessionId/message and produces text/event-stream.

- [ ] **Step 4: Verify and commit**

Run:

    ./mvnw.cmd test -Dtest=IntentRouterTest,ChatControllerSseTest
    ./mvnw.cmd test
    git add pom.xml src/main/java/com/yike/aftersaleagent/agent src/main/java/com/yike/aftersaleagent/ai
    git add src/main/java/com/yike/aftersaleagent/chat src/main/resources/application-mock.yml src/main/resources/application-dashscope.yml
    git add src/test/java/com/yike/aftersaleagent
    git commit -m "feat: add streaming chat router"

Expected: test,mock profiles pass and every request terminates with a done event.

### Task 5: 构建内置知识库、Qdrant RAG 和可重复评测

**Files:**

- Create: src/main/resources/knowledge/after-sale-rule.md
- Create: src/main/resources/knowledge/coupon-rule.md
- Create: src/main/resources/knowledge/product-rule.md
- Create: src/main/resources/knowledge/faq.md
- Create: src/main/resources/knowledge/refund-rule.md
- Create: src/main/java/com/yike/aftersaleagent/knowledge/KnowledgeIndexer.java
- Create: src/main/java/com/yike/aftersaleagent/knowledge/KnowledgeRetriever.java
- Create: src/main/java/com/yike/aftersaleagent/knowledge/KnowledgeAnswerService.java
- Create: src/main/java/com/yike/aftersaleagent/knowledge/RagEvaluationRunner.java
- Create: src/main/resources/rag-evaluation/questions.json
- Create: docs/rag-evaluation/README.md
- Modify: pom.xml
- Modify: src/main/resources/application-local.yml
- Modify: src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java
- Create: src/test/java/com/yike/aftersaleagent/knowledge/KnowledgeAnswerServiceTest.java
- Create: src/test/java/com/yike/aftersaleagent/knowledge/KnowledgeIndexerTest.java

**Interfaces:**

- Consumes: Intent.FAQ_QUERY、AiGateway、SourceCitation、Qdrant Compose service.
- Produces: KnowledgeAnswerService.answer(context)、POST /api/knowledge/reindex、引用来源与证据不足兜底。

- [ ] **Step 1: 写 Metadata 过滤和无证据兜底失败测试**

    when(retriever.retrieve(any())).thenReturn(List.of(
            evidence("after-sale-rule.md", "REFUND", "普通商品签收后 7 天内…")));

    ChatOutcome outcome = service.answer(context("七天无理由退货规则是什么？"));

    assertThat(outcome.citations()).singleElement()
            .extracting(SourceCitation::sourcePath)
            .isEqualTo("knowledge/after-sale-rule.md");

    when(retriever.retrieve(any())).thenReturn(List.of());
    assertThat(service.answer(context("未知规则问题")).reply()).contains("资料不足");

Run: ./mvnw.cmd test -Dtest=KnowledgeAnswerServiceTest,KnowledgeIndexerTest  
Expected: FAIL because knowledge contracts and service do not exist.

- [ ] **Step 2: 添加 Qdrant 与固定 Metadata**

Add org.springframework.ai:spring-ai-starter-vector-store-qdrant. In application-local.yml configure host, port 6334, collection-name after_sale_knowledge, content-field-name doc_content, use-tls false and initialize-schema true.

Every chunk has this exact key set:

    Map.of(
        "docType", "AFTER_SALE_RULE",
        "scene", "REFUND",
        "productType", "NORMAL",
        "sourceTitle", "售后服务规则",
        "sourcePath", "knowledge/after-sale-rule.md"
    )

Use 500 Chinese-character chunks and 80-character overlap. The five Markdown documents contain only demo rules and no proprietary content.

- [ ] **Step 3: 实现索引、检索和回答**

    public interface KnowledgeRetriever {
        List<SourceCitation> retrieve(Intent intent, String productType, String question, int topK);
    }

    public interface KnowledgeAnswerService {
        ChatOutcome answer(AgentExecutionContext context);
    }

KnowledgeIndexer rebuilds only after_sale_knowledge through the local reindex endpoint. KnowledgeRetriever filters by scene before similarity search and uses topK 3. KnowledgeAnswerService calls AiGateway.explain only after evidence exists, appends retriever citations and never manufactures a citation.

- [ ] **Step 4: 接入 FAQ 并建立评测集**

ChatOrchestrator emits “正在检索售后规则…”, delegates FAQ to KnowledgeAnswerService, emits message then done. questions.json has at least 20 cases, each with question, scene, expectedSourcePath and expectedEvidencePhrase.

RagEvaluationRunner writes an ignored timestamped Markdown report to target/rag-evaluation with case count, Recall@3, citation-correct count and manual answer-relevance column. No result value is written until it has been measured.

- [ ] **Step 5: Verify and commit**

Run deterministic tests:

    ./mvnw.cmd test -Dtest=KnowledgeAnswerServiceTest,KnowledgeIndexerTest

Then, with Docker and DASHSCOPE_API_KEY:

    docker compose --env-file .env up -d qdrant
    ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,dashscope"

Call reindex, ask the FAQ demo question, and confirm after-sale-rule.md is cited. Record only actual aggregate results in docs/rag-evaluation/README.md.

    git add pom.xml src/main/java/com/yike/aftersaleagent/knowledge src/main/resources/knowledge
    git add src/main/resources/rag-evaluation docs/rag-evaluation src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java
    git add src/test/java/com/yike/aftersaleagent/knowledge
    git commit -m "feat: add cited after-sale RAG"

### Task 6: 实现受治理 Tool 层和订单、优惠券查询

**Files:**

- Create: src/main/java/com/yike/aftersaleagent/tool/ToolRisk.java
- Create: src/main/java/com/yike/aftersaleagent/tool/GovernedTool.java
- Create: src/main/java/com/yike/aftersaleagent/tool/ToolRegistry.java
- Create: src/main/java/com/yike/aftersaleagent/tool/ToolAuditService.java
- Create: src/main/java/com/yike/aftersaleagent/agent/BusinessReferenceExtractor.java
- Create: src/main/java/com/yike/aftersaleagent/order/OrderInfo.java
- Create: src/main/java/com/yike/aftersaleagent/order/OrderMapper.java
- Create: src/main/java/com/yike/aftersaleagent/order/OrderQueryTool.java
- Create: src/main/java/com/yike/aftersaleagent/coupon/CouponInfo.java
- Create: src/main/java/com/yike/aftersaleagent/coupon/CouponMapper.java
- Create: src/main/java/com/yike/aftersaleagent/coupon/CouponQueryTool.java
- Create: src/test/java/com/yike/aftersaleagent/tool/ToolRegistryTest.java
- Create: src/test/java/com/yike/aftersaleagent/tool/OrderQueryToolTest.java
- Create: src/test/java/com/yike/aftersaleagent/tool/CouponQueryToolTest.java
- Create: src/test/java/com/yike/aftersaleagent/agent/BusinessReferenceExtractorTest.java

**Interfaces:**

- Consumes: AgentExecutionContext、current-user identity、schema、tool_call_log.
- Produces: audited read-only OrderQueryTool and CouponQueryTool, deterministic business-reference extraction, and workflow-level tool allow list.

- [ ] **Step 1: 写白名单、越权和审计失败测试**

    assertThatThrownBy(() -> registry.requireAllowed(Intent.FAQ_QUERY, "orderQuery"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("TOOL_NOT_ALLOWED");

    assertThatThrownBy(() -> orderQueryTool.execute(contextFor(10002), new OrderQuery("O1001")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ORDER_NOT_FOUND_OR_FORBIDDEN");

    OrderSummary summary = orderQueryTool.execute(contextFor(10001), new OrderQuery("O1001"));
    assertThat(summary.orderNo()).isEqualTo("O1001");
    verify(toolAuditService).recordSuccess(eq("orderQuery"), any(), anyLong());

Run: ./mvnw.cmd test -Dtest=ToolRegistryTest,OrderQueryToolTest,CouponQueryToolTest,BusinessReferenceExtractorTest  
Expected: FAIL because Tools、mappers and audit service do not exist.

- [ ] **Step 2: 定义契约和允许矩阵**

    public enum ToolRisk { READ_ONLY, HUMAN_APPROVAL_REQUIRED }

    public record OrderQuery(String orderNo) { }
    public record OrderSummary(String orderNo, String productName, String productType,
                               BigDecimal amount, String orderStatus, LocalDateTime receivedAt) { }
    public record CouponQuery(String couponCode) { }
public record CouponSummary(String couponCode, BigDecimal thresholdAmount,
                            BigDecimal discountAmount, String status) { }

public interface BusinessReferenceExtractor {
    String requireOrderNo(String message);
    String requireCouponCode(String message);
}

ToolRegistry allows exactly: FAQ → none; coupon → orderQuery and couponQuery; refund → orderQuery, afterSaleRuleQuery and ticketCreate. Any other pair throws TOOL_NOT_ALLOWED before execution.

BusinessReferenceExtractor uses the exact regular expressions `\\bO\\d{4,}\\b` and `\\bC\\d{4,}\\b`. It returns the first match in uppercase and throws BUSINESS_REFERENCE_REQUIRED when the required reference is absent. Add tests for `订单 O1001` and `优惠券 c1001` success cases plus an absent-order failure case.

- [ ] **Step 3: 实现无 direct SQL 的 Tool**

OrderQueryTool and CouponQueryTool query by currentUserId and business key using Mapper methods. Each Tool validates nonblank keys, records sanitized input summary, measures elapsed milliseconds, writes success/failure through ToolAuditService, and accepts no userId argument. Use a fixed two-second operation timeout wrapper; no retry is used for local database reads.

- [ ] **Step 4: Verify and commit**

Run:

    ./mvnw.cmd test -Dtest=ToolRegistryTest,OrderQueryToolTest,CouponQueryToolTest,BusinessReferenceExtractorTest
    ./mvnw.cmd test
    git add src/main/java/com/yike/aftersaleagent/tool src/main/java/com/yike/aftersaleagent/order src/main/java/com/yike/aftersaleagent/coupon
    git add src/main/java/com/yike/aftersaleagent/agent/BusinessReferenceExtractor.java
    git add src/test/java/com/yike/aftersaleagent/tool
    git add src/test/java/com/yike/aftersaleagent/agent/BusinessReferenceExtractorTest.java
    git commit -m "feat: add governed order and coupon tools"

Expected: FAQ cannot invoke Tool, cross-user lookup fails, every successful Tool call creates one audit record.

### Task 7: 实现确定性的优惠券不可用分析工作流

**Files:**

- Create: src/main/java/com/yike/aftersaleagent/coupon/CouponEligibility.java
- Create: src/main/java/com/yike/aftersaleagent/coupon/CouponRuleService.java
- Create: src/main/java/com/yike/aftersaleagent/coupon/CouponAnalysisWorkflow.java
- Modify: src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java
- Create: src/test/java/com/yike/aftersaleagent/coupon/CouponRuleServiceTest.java
- Create: src/test/java/com/yike/aftersaleagent/coupon/CouponAnalysisWorkflowTest.java

**Interfaces:**

- Consumes: OrderQueryTool、CouponQueryTool、ToolRegistry、BusinessReferenceExtractor、AiGateway、ChatOutcome.
- Produces: coupon SSE status sequence and deterministic business conclusion; LLM only phrases the conclusion.

- [ ] **Step 1: 写规则优先于模型失败测试**

    CouponEligibility eligibility = ruleService.evaluate(
            new OrderSummary("O1001", "蓝牙耳机", "NORMAL", new BigDecimal("80.00"), "PAID", null),
            new CouponSummary("C1001", new BigDecimal("100.00"), new BigDecimal("20.00"), "AVAILABLE"));

    assertThat(eligibility.usable()).isFalse();
    assertThat(eligibility.reasonCode()).isEqualTo("ORDER_AMOUNT_BELOW_THRESHOLD");
    assertThat(eligibility.reasonText()).contains("100");
    verify(aiGateway, never()).classifyIntent(anyString());

The workflow test asserts Tool invocation order is order then coupon, and AiGateway.explain receives a facts string containing the rule code rather than a raw entity.

Run: ./mvnw.cmd test -Dtest=CouponRuleServiceTest,CouponAnalysisWorkflowTest  
Expected: FAIL because rule and workflow do not exist.

- [ ] **Step 2: 实现可审查 Java 规则**

    public record CouponEligibility(boolean usable, String reasonCode, String reasonText) { }

    public interface CouponRuleService {
        CouponEligibility evaluate(OrderSummary order, CouponSummary coupon);
    }

Rule order: coupon status not AVAILABLE → COUPON_NOT_AVAILABLE; then amount below threshold → ORDER_AMOUNT_BELOW_THRESHOLD; otherwise → USABLE. This service cannot call LLM、Tool、Mapper or controller.

- [ ] **Step 3: 编排 Tool、规则和受限解释**

CouponAnalysisWorkflow.execute(context) obtains orderNo and couponCode through BusinessReferenceExtractor, checks the ToolRegistry, calls both read-only Tools, calls CouponRuleService, then calls only:

    aiGateway.explain(
        "用简洁客服语言解释已确定的优惠券结果，不得更改结论。",
        factsContainingRuleCode);

It returns ChatOutcome with no ticket ID and no citations. ChatOrchestrator emits in order: “正在查询订单…”, “正在查询优惠券…”, “正在校验优惠券规则…”, message and done.

- [ ] **Step 4: Verify and commit**

Run:

    ./mvnw.cmd test -Dtest=CouponRuleServiceTest,CouponAnalysisWorkflowTest
    ./mvnw.cmd test
    git add src/main/java/com/yike/aftersaleagent/coupon src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java
    git add src/test/java/com/yike/aftersaleagent/coupon
    git commit -m "feat: add coupon analysis workflow"

Expected: user 10001 plus O1001/C1001 yields ORDER_AMOUNT_BELOW_THRESHOLD in trace and a user-facing explanation that the order has not reached 100.00.

### Task 8: 实现退款人工审核工单、状态机和异步步骤审计

**Files:**

- Create: src/main/java/com/yike/aftersaleagent/ticket/domain/TicketTaskStatus.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/domain/TicketTaskStateMachine.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/domain/RefundDecision.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/AfterSaleRuleQueryTool.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/TicketCreateTool.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/TicketMapper.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/TicketTaskMapper.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/AgentStepLogService.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/RefundRuleService.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/RefundTaskExecutor.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/RefundWorkflow.java
- Create: src/main/java/com/yike/aftersaleagent/config/AsyncConfig.java
- Modify: src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/TicketController.java
- Create: src/test/java/com/yike/aftersaleagent/ticket/TicketTaskStateMachineTest.java
- Create: src/test/java/com/yike/aftersaleagent/ticket/RefundRuleServiceTest.java
- Create: src/test/java/com/yike/aftersaleagent/ticket/RefundWorkflowTest.java

**Interfaces:**

- Consumes: order Tool、knowledge evidence、Tool Registry、BusinessReferenceExtractor、task/audit schema、SSE events.
- Produces: asynchronous refund task, WAIT_HUMAN ticket, immutable step audit, GET /api/tickets/{ticketId}, GET /api/tickets/{ticketId}/trace.

- [ ] **Step 1: 写状态机和高风险兜底失败测试**

    assertThat(stateMachine.transition(TicketTaskStatus.PENDING, TicketTaskStatus.RUNNING))
            .isEqualTo(TicketTaskStatus.RUNNING);
    assertThat(stateMachine.transition(TicketTaskStatus.RUNNING, TicketTaskStatus.WAIT_HUMAN))
            .isEqualTo(TicketTaskStatus.WAIT_HUMAN);
    assertThatThrownBy(() -> stateMachine.transition(TicketTaskStatus.FINISHED, TicketTaskStatus.RUNNING))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ILLEGAL_TASK_TRANSITION");

    RefundDecision decision = refundRuleService.evaluate(deliveredTwoDaysAgoNormalOrder(), eligibleRuleEvidence());
    assertThat(decision.requiresHumanReview()).isTrue();
    assertThat(decision.ticketStatus()).isEqualTo(TicketTaskStatus.WAIT_HUMAN);

The workflow test asserts a key Tool failure produces WAIT_HUMAN, writes a failed agent_step_log and never invokes payment/refund client.

Run: ./mvnw.cmd test -Dtest=TicketTaskStateMachineTest,RefundRuleServiceTest,RefundWorkflowTest  
Expected: FAIL because ticket types and workflow do not exist.

- [ ] **Step 2: 实现状态机、Java 退款规则和受控 Tool**

Allowed pairs only:

    PENDING → RUNNING
    RUNNING → WAIT_HUMAN | FINISHED | FAILED

    public record RefundDecision(boolean eligible, boolean requiresHumanReview,
                                 String reasonCode, String reasonText,
                                 TicketTaskStatus ticketStatus) { }

    public interface RefundRuleService {
        RefundDecision evaluate(OrderSummary order, List<SourceCitation> evidence);
    }

Eligible only when orderStatus is DELIVERED, productType is NORMAL, receivedAt is no more than seven days before injected Clock, and evidence exists. An eligible result still returns WAIT_HUMAN; an ineligible result returns FINISHED. AfterSaleRuleQueryTool is read-only and uses KnowledgeRetriever. TicketCreateTool only persists ticket/task rows and declares HUMAN_APPROVAL_REQUIRED risk.

- [ ] **Step 3: 实现有限步骤异步执行和审计**

Configure a named ThreadPoolTaskExecutor: core pool 2, max 4, queue 20. RefundWorkflow.submit(context) obtains orderNo through BusinessReferenceExtractor, creates ticket/task with idempotency key refund:{userId}:{sessionId}:{orderNo}, emits “正在创建退款审核工单…”, then delegates to RefundTaskExecutor.

RefundTaskExecutor records these step names in exact order:

    QUERY_ORDER
    RETRIEVE_AFTER_SALE_RULE
    EVALUATE_REFUND_ELIGIBILITY
    CREATE_OR_UPDATE_HUMAN_REVIEW_TICKET
    GENERATE_USER_SUMMARY

Each step writes start/end time, status, sanitized input/output and error. Maximum step count is five. Key Tool errors transition to WAIT_HUMAN; invalid transition or persistence error transitions to FAILED. This MVP never resumes a task after process restart.

- [ ] **Step 4: 接入 SSE 与查询 API**

For REFUND_ELIGIBILITY, ChatOrchestrator emits a status event as each executor step begins, emits a ticket event after persistence, then message and done. TicketController enforces ownership through CurrentDemoUser and returns chronological agent_step_log and tool_call_log at /api/tickets/{ticketId}/trace.

- [ ] **Step 5: Verify and commit**

Run:

    ./mvnw.cmd test -Dtest=TicketTaskStateMachineTest,RefundRuleServiceTest,RefundWorkflowTest
    ./mvnw.cmd test

Manual demo: user 10002 submits O2001 and sees WAIT_HUMAN plus five ordered step entries and no external mutation. In test profile make AfterSaleRuleQueryTool throw; verify WAIT_HUMAN with failed step and nonempty error.

    git add src/main/java/com/yike/aftersaleagent/ticket src/main/java/com/yike/aftersaleagent/config/AsyncConfig.java
    git add src/main/java/com/yike/aftersaleagent/chat/ChatOrchestrator.java src/test/java/com/yike/aftersaleagent/ticket
    git commit -m "feat: add human review refund workflow"

### Task 9: 完成演示页面、trace 查询和防 XSS 输出

**Files:**

- Create: src/main/resources/static/index.html
- Create: src/main/resources/static/app.js
- Create: src/main/resources/static/styles.css
- Create: src/main/java/com/yike/aftersaleagent/trace/TraceController.java
- Create: src/main/java/com/yike/aftersaleagent/trace/TraceQueryService.java
- Create: src/test/java/com/yike/aftersaleagent/trace/TraceControllerTest.java

**Interfaces:**

- Consumes: chat SSE、ticket trace、requestId/audit data.
- Produces: page showing SSE states, citations, ticket status and trace; GET /api/traces/{requestId}.

- [ ] **Step 1: 写 trace 查询失败测试**

    mockMvc.perform(get("/api/traces/{requestId}", "req-123")
                    .header("X-Demo-User-Id", "10002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestId").value("req-123"))
            .andExpect(jsonPath("$.data.steps[0].stepName").value("QUERY_ORDER"));

Run: ./mvnw.cmd test -Dtest=TraceControllerTest  
Expected: FAIL because trace query types do not exist.

- [ ] **Step 2: 实现最小可观测性查询**

TraceQueryService.findForCurrentUser(requestId, user) returns only rows on the user’s ticket/session path. Include requestId, ticketId, ordered steps, Tool name, elapsed milliseconds, success and sanitized error code. Do not return API keys, raw prompts, full order address or another user’s data.

- [ ] **Step 3: 实现无框架页面**

index.html has demo-user selector, session creation, message input, event timeline, citations, ticket summary and “查看处理轨迹” button. app.js uses fetch with X-Demo-User-Id, parses SSE events, appends data only with textContent, and loads ticket trace after ticket event. Do not add React, Vue, Node tooling or a frontend build system.

- [ ] **Step 4: Verify and commit**

Run:

    ./mvnw.cmd test -Dtest=TraceControllerTest
    ./mvnw.cmd test

Manual checks: run all three demos. Enter <img src=x onerror=alert(1)> as a message; it must appear as text and not execute.

    git add src/main/resources/static src/main/java/com/yike/aftersaleagent/trace
    git add src/test/java/com/yike/aftersaleagent/trace
    git commit -m "feat: add demo UI and trace viewer"

### Task 10: 完成验证、README、评测记录和求职交付物

**Files:**

- Create: README.md
- Create: docs/demo-script.md
- Create: docs/interview-notes.md
- Create: docs/api-examples.http
- Create: docs/rag-evaluation/latest-report.md
- Create: .github/workflows/verify.yml
- Modify: docker-compose.yml
- Modify: AGENTS.md

**Interfaces:**

- Consumes: all previous tasks and working local Docker environment.
- Produces: reproducible startup guide, three-scene script, factual resume/interview claims and CI verification.

- [ ] **Step 1: 写固定验收清单并先执行**

Acceptance is fixed:

    1. ./mvnw.cmd test exits 0.
    2. docker compose --env-file .env up -d reports all dependency containers running.
    3. FAQ response contains a real source citation.
    4. Coupon demo stores two successful Tool audit rows and reports ORDER_AMOUNT_BELOW_THRESHOLD.
    5. Refund demo creates WAIT_HUMAN with ordered step logs and no mutation outside ticket tables.
    6. Browser demo renders untrusted message text safely.

Run the list once before editing delivery docs. Any unmet item is a defect in its owning task; do not weaken this list.

- [ ] **Step 2: Run full build and Compose validation**

    ./mvnw.cmd clean test
    ./mvnw.cmd package
    docker compose --env-file .env config
    docker compose --env-file .env up -d

Expected: Maven exits 0; Compose configuration validates; dependencies are reachable. If Docker is unavailable, document the exact external blocker in docs/setup/windows-development.md and do not claim Compose validation passed.

- [ ] **Step 3: 进行 RAG 评测并保存事实**

Run the 20 cases with live vector store. docs/rag-evaluation/latest-report.md records only actual case count, actual Recall@3, actual citation correctness, actual manual relevance review and links to generated local report. If no model/API Key is available, record “未执行（缺少可用模型凭据）”; do not invent a percentage.

- [ ] **Step 4: 写启动、演示和面试说明**

README includes architecture diagram, prerequisites, .env creation, startup, profile behavior, three demo requests, test commands, limitations and no-secret warning. docs/demo-script.md gives a five-minute narration showing FAQ citation, coupon audit and refund WAIT_HUMAN trace. docs/interview-notes.md answers why Tool avoids direct SQL, why LLM cannot refund, why citations matter, how Tool failure degrades, and why the project is layered Agent orchestration rather than autonomous Multi-Agent/MCP.

- [ ] **Step 5: 添加 CI 并验证干净工作树**

Create .github/workflows/verify.yml:

    name: verify
    on: [push, pull_request]
    jobs:
      test:
        runs-on: ubuntu-latest
        steps:
          - uses: actions/checkout@v4
          - uses: actions/setup-java@v4
            with:
              distribution: temurin
              java-version: '21'
          - run: ./mvnw -B test

Then run:

    ./mvnw.cmd clean test
    ./mvnw.cmd package
    git status --short
    git log --oneline -10

Expected: test/package pass and no secret appears in status.

- [ ] **Step 6: Commit final delivery docs**

    git add README.md docs .github AGENTS.md docker-compose.yml
    git commit -m "docs: add project demo and verification guide"

## Final Delivery Checklist

- [ ] README can onboard a reviewer without private context.
- [ ] mock profile runs without a model key.
- [ ] dashscope profile is documented but never stores a key in Git.
- [ ] Three demos are recorded and every resume fact traces to code, tests or demo artifacts.
- [ ] The project does not claim MCP, autonomous Multi-Agent, production authentication, high-reliability queue, automatic refund or unmeasured metrics.
