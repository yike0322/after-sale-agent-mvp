# 智能售后工单 Agent 平台：一个月求职 MVP 设计

**日期：** 2026-08-13  
**状态：** 已确认，待实施计划  
**定位：** Java 后端 / AI 应用后端求职作品

## 1. 目标与成功标准

在一个月内交付一个可本地运行、可录制演示、可在面试中解释清楚的电商售后 AI 应用后端。系统证明的重点不是“调用了大模型”，而是将模型置于受控的 Java 业务系统中。

目标闭环：

```text
用户咨询
  → 意图路由
  → RAG 检索或受控业务 Tool
  → Java 规则判定
  → 工单状态流转 / 人工审核
  → SSE 进度与结果
  → 请求、检索、工具、步骤审计
```

完成时必须具备以下证据：

1. Docker Compose 可以启动依赖，应用可通过一条本地命令运行。
2. 三个固定演示场景可稳定复现，且具有固定的演示数据。
3. 每个场景均能查看处理步骤、工具调用和最终业务结论。
4. RAG 回答显示引用来源，并有可重复执行的小型评测集。
5. 核心领域规则、工具权限和状态流转具有自动化测试。
6. README 包含架构图、启动步骤、接口文档入口、演示脚本和面试讲解要点。

## 2. 明确范围

### 2.1 一期必须完成

- 单体 Spring Boot 后端及简洁静态演示页。
- 会话与消息持久化、SSE 流式状态输出。
- FAQ / 售后规则 RAG，含 Metadata 过滤、TopK、引用来源和无依据兜底。
- 意图路由：`FAQ_QUERY`、`COUPON_ANALYSIS`、`REFUND_ELIGIBILITY`。
- 订单、优惠券、售后规则、工单创建四类受控 Tool。
- 优惠券分析工作流与退款资格/人工审核工单工作流。
- 任务状态机、步骤日志、工具调用审计与 requestId 链路日志。
- MySQL、Redis、Qdrant 的本地开发环境及演示种子数据。
- 单元测试、集成测试、RAG 评测报告和 Docker 化运行说明。

### 2.2 明确不做或延后

- 真正退款、支付、账户修改、真实物流或人工客服系统集成。
- 自治式多 Agent 协作、自由规划的 Plan-Executor、任意工具自动发现。
- 真正 MCP Server/Client；一期仅实现内部 Tool Registry / Tool Calling 适配层。
- MQ、Redis Stream、分布式锁、微服务、分布式事务和复杂工作流引擎。
- 文档上传后台、多格式 OCR、Reranker、多向量库和自动知识入库。
- 完整登录/权限中心；本地演示使用受限的 Demo 用户上下文，不宣称生产级认证。
- 投诉、物流等额外场景；它们作为二期扩展项。

## 3. 三个演示场景

### Demo 1：售后规则问答

用户提问“七天无理由退货规则是什么？”。系统将其识别为 `FAQ_QUERY`，使用场景 Metadata 过滤后检索售后规则，流式返回带文档标题和片段来源的回答。若证据不足，明确说明无法根据现有资料判断，而不是编造规则。

### Demo 2：优惠券不可用分析

用户询问“我的满 100 减 20 优惠券为什么不能用？”。系统查询当前用户的订单与优惠券，使用 Java 领域规则判定原因，例如订单金额未达到门槛；模型只负责将确定的业务结论解释成自然语言。结果页展示两个 Tool 调用和对应审计记录。

### Demo 3：退款资格判定与人工审核工单

用户提出退款诉求。系统查询订单、检索售后规则，再由 Java 规则服务给出资格判断。无论结果如何，系统不执行退款；需要人工跟进时创建 `WAIT_HUMAN` 工单。SSE 依次展示“查询订单、检索规则、规则判定、创建工单”，并可在工单详情查看状态、步骤和工具审计。

## 4. 架构与职责边界

```text
Static Demo UI / API Client
           │
           ▼
Chat Controller (SSE)
           │
           ▼
Conversation Router
 ┌─────────┼──────────┐
 ▼         ▼          ▼
RAG     Coupon      Refund
Service Workflow    Workflow
 │         │          │
 └─────────┴──────────┘
           │
           ▼
Tool Registry → Domain Service → Repository → MySQL / Redis / Qdrant
           │
           ▼
Audit & Trace Log
```

为避免名词堆砌，代码层将三个“Agent”实现为三个可测试的职责边界，而不是三个不受约束的自治模型：

| 模块 | 职责 | 可访问能力 | 禁止事项 |
| --- | --- | --- | --- |
| Conversation Router | 识别意图、补充信息、路由和统一回复 | 结构化意图识别 | 不访问数据库、不创建工单、不直接调用业务 Tool |
| Knowledge Service | RAG 检索、证据组装、带来源回答 | Retriever | 不判定退款资格、不写业务数据 |
| Ticket Workflow | 固定步骤编排、规则判定、创建待审核工单 | 白名单 Tool 和领域规则服务 | 不允许模型自由增加步骤或执行退款 |

“Multi-Agent”在文档中是职责分层；简历中使用“分层 Agent 编排”。只有后续实现独立协作协议后才使用“自治 Multi-Agent”表述。

## 5. 技术基线与本地环境

| 类别 | 选择 | 原因 |
| --- | --- | --- |
| JDK | Java 21 LTS | 满足 Spring AI Alibaba 的 JDK 17+ 要求，并保留长期支持能力 |
| 构建 | Maven 3.9+ | Java 后端常用、依赖与测试命令清晰 |
| Web | Spring Boot 3.5.8、Spring MVC | 稳定单体后端与 SSE 支持 |
| AI | Spring AI 1.1.2、Spring AI Alibaba BOM 1.1.2.0、Spring AI Alibaba Extensions BOM 1.1.2.1 | 官方推荐的 Spring Boot 3.5.x 兼容线；模型供应商由配置抽象 |
| 持久化 | MySQL 8、MyBatis-Plus 3.5.17 | 保存会话、业务演示数据、工单和审计记录 |
| 缓存 | Redis 7 | 仅用于会话短缓存、任务进度和幂等标记 |
| 向量库 | Qdrant | 仅使用一种向量数据库，支持 Metadata 过滤 |
| 异步 | `@Async` + 持久化任务状态 | 一期单实例足够，避免为简历引入不必要 MQ |
| 部署 | Docker Compose | 一键启动 MySQL、Redis、Qdrant；应用可本地运行或容器化 |

模型层定义接口并提供两个 profile：

- `mock`：确定性返回，用于没有 API Key 的自动化测试和演示兜底。
- `dashscope`：通过 Spring AI Alibaba 接入真实大模型，用于真实 RAG 与自然语言解释。

在创建工程前安装 JDK 21、Maven 和 Docker Desktop；现有 MySQL 可在本地开发阶段复用，最终演示使用 Compose 统一启动。

## 6. 数据与业务模型

一期只建立支撑三个 Demo 的表和集合：

| 存储 | 主要内容 |
| --- | --- |
| `user_session`、`chat_message` | 会话和消息历史 |
| `knowledge_doc`、`knowledge_chunk` | 文档元数据、分块文本；向量存入 Qdrant |
| `order_info`、`coupon_info` | 固定的模拟订单和优惠券数据 |
| `customer_ticket`、`ticket_task` | 工单与异步任务状态 |
| `agent_step_log`、`tool_call_log` | 步骤、输入摘要、输出摘要、耗时、错误和关联 requestId |

工单任务状态机固定为：

```text
PENDING → RUNNING → WAIT_HUMAN
                  ↘ FINISHED
                  ↘ FAILED
```

退款相关路径只允许流转到 `WAIT_HUMAN` 或 `FINISHED`（规则不满足且无需人工跟进）；任何路径都不触发真实退款。

## 7. Tool 调用治理

Agent 和 Tool 都不得直接执行 SQL。每个 Tool 仅调用领域 Service，并统一经过 Tool Registry。Tool 调用最少包含：

- 名称、用途、输入 DTO、输出 DTO、风险等级、超时和允许的调用方。
- Bean Validation 参数校验。
- 当前用户上下文注入；不得信任模型或请求正文中声明的 `userId`。
- 订单归属校验；用户只能查询自己的订单或优惠券。
- 工具白名单和工作流级允许列表。
- 超时、一次可控重试、结构化错误和审计落库。

高风险操作的 Tool 不向模型开放。`TicketCreateTool` 只创建待人工审核工单，不能更改退款、资金或账户状态。

失败收口规则固定如下：

- RAG 未召回足够证据：返回“现有资料不足以判断”的提示，并附可用来源，不生成业务结论。
- 非高风险 Tool 首次超时或可恢复错误：仅重试一次；仍失败则保存失败审计并返回可理解的降级提示。
- 退款工作流任一关键查询失败：创建或更新为 `WAIT_HUMAN` 工单，保留已完成步骤和失败原因，避免模型补全缺失事实。
- 系统性错误、非法状态迁移或不可恢复持久化失败：任务进入 `FAILED`，SSE 发送 `error` 与 `done`，客户端可通过工单详情查询记录。

## 8. RAG 设计

一期知识来源为仓库内置的 5–10 份 Markdown 文档，覆盖售后、优惠券与商品规则。导入流程为：

```text
Markdown → 清洗 → 分块 → Metadata → Embedding → Qdrant
```

每个分块至少带有 `docType`、`scene`、`productType`、`sourceTitle` 和 `sourcePath`。查询时先根据意图/商品类型过滤，再向量检索；默认 `topK=3`，由配置控制。回答必须基于检索片段，并返回来源；召回不足时进入明确的“不确定/转人工”兜底。

建立至少 20 条问题的评测集，记录 `Recall@3`、引用正确性和人工判定的答案相关性。项目只报告实际执行出的结果，不预设或虚构百分比。

## 9. API、SSE 与可观测性

核心接口：

- `POST /api/sessions`：创建会话。
- `GET /api/sessions/{sessionId}/messages`：查看历史消息。
- `POST /api/chat/stream`：接收用户问题并以 SSE 输出状态和最终回复。
- `GET /api/tickets/{ticketId}`：查看工单及当前状态。
- `GET /api/tickets/{ticketId}/trace`：查看步骤和 Tool 调用审计。
- `POST /api/knowledge/reindex`：本地管理员触发内置知识库重建。

SSE 事件仅使用 `status`、`message`、`ticket`、`error`、`done` 五类。复杂任务先持久化 `ticket_task`，再异步执行；客户端断开不取消任务，重新查询工单即可恢复查看状态。

所有入口生成或透传 `requestId`，日志与审计记录关联 `requestId`、`sessionId`、`ticketId`、意图、模型调用耗时、召回数量、Tool 耗时、步骤数和错误码。日志不记录完整敏感提示词、密钥或原始用户隐私数据。

## 10. 测试与验收

必须至少覆盖：

- 规则判定：优惠券门槛、退款资格、退款高风险兜底。
- Tool：参数非法、订单越权、超时/失败日志。
- 状态机：合法和非法状态迁移。
- RAG：场景过滤、引用返回、未召回兜底。
- 工作流：三个 Demo 的正常路径与一个 Tool 失败转人工路径。
- SSE：状态事件顺序与结束事件。

验收采用固定演示脚本，并保存三类佐证：测试结果、RAG 评测报告、页面录屏或截图。简历中的所有指标仅来自这些实际结果。

## 11. 实施顺序

1. 配置 JDK/Maven/Docker，初始化 Git 与 Spring Boot 工程，建立健康检查和 Compose。
2. 完成 MySQL 数据模型、Demo 用户上下文、模拟订单/优惠券数据和会话 API。
3. 完成基础聊天与 SSE，再接入可切换的真实/Mock 模型。
4. 完成内置知识库、RAG、引用来源和评测集。
5. 完成意图路由、Tool Registry、订单/优惠券 Tool 与优惠券分析工作流。
6. 完成退款资格工作流、异步任务、状态机、人工审核工单和审计页。
7. 完成测试、Docker、演示页、README、录屏和简历事实核对。

## 12. 面试表述边界

可以表述为“分层 Agent 编排”“受控 Tool Calling”“RAG 引用溯源”“状态机驱动的人机协同工单”“请求级可观测性”。

不得在未实现时表述为“真正 MCP”“高可靠消息队列”“生产级认证”“自动退款”“自治式 Multi-Agent”或宣称未经实际评测的数据指标。
