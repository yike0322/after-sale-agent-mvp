# 浏览器控制台 API 示例

> 示例仅适用于本地 `test` profile 和虚构账号。Bearer Token 由登录响应产生，勿用于任何真实系统。

## 1. 登录

```http
POST /api/auth/login
Content-Type: application/json

{"account":"buyer_li","password":"Buyer#2026"}
```

成功响应 `data` 含 `token`、`userId`、`displayName`、`role` 和 `expiresAt`。前端只在 `sessionStorage` 保存 token 与安全用户资料。

## 2. 客户会话与 SSE 对话

```http
POST /api/sessions
Authorization: Bearer <token>
```

```http
POST /api/chat/stream
Authorization: Bearer <token>
Content-Type: application/json

{"sessionId":"<session-id>","message":"订单O1001使用优惠券C1001为什么不能使用？"}
```

页面消费 `status`、`ticket`、`message`、`error`、`done` 五种 SSE 事件。`message` 可能携带 RAG 来源引用；引用由后端检索结果产生，前端不会解析或伪造。

## 3. 工单与脱敏轨迹

```http
GET /api/tickets/mine
Authorization: Bearer <customer-token>

GET /api/tickets/9102/trace
Authorization: Bearer <customer-token>
```

客户列表不返回 `userId`；详情与轨迹查询都有 owner predicate。主管使用：

```http
GET /api/supervisor/tickets
GET /api/supervisor/tickets/9104/trace
Authorization: Bearer <supervisor-token>
```

## 4. 知识库重建

```http
POST /api/knowledge/reindex
Authorization: Bearer <supervisor-token>
```

该入口仅在 `local,dashscope` profile 以及已配置 DashScope/Qdrant 时启用；测试 profile 会给出“当前环境未启用”的安全提示。
