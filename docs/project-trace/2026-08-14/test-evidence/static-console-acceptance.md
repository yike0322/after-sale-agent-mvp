# 静态演示台验收证据

执行日期：2026-08-14。全部账号、订单、工单和规则均为虚构、去标识化演示数据。

## 自动化验证

```powershell
.\mvnw.cmd package
```

结果：Java 21 下 88 tests，0 failures，0 errors，0 skipped；随后成功生成可执行 jar。

静态资源专项覆盖：根路径转发 `index.html`、`index.html` 具有 `login-view` 语义结构、`app.js` 由 Spring Boot 静态资源处理器返回。

## 本地浏览器验收

| 角色 | 操作 | 观察结果 |
| --- | --- | --- |
| CUSTOMER / buyer_li | 登录 | 只显示客户工作台；创建安全会话。 |
| CUSTOMER / buyer_li | `O1001 + C1001` 优惠券问题 | 依次显示识别、订单查询、优惠券查询、规则校验、完成五个 SSE 状态；结论为未达门槛。 |
| CUSTOMER / buyer_li | 点击历史工单 9102 | 只能读取本人详情和脱敏步骤/Tool 摘要。 |
| SUPERVISOR / supervisor_chen | 登录 | 显示 4 条跨用户演示工单，带归属演示用户。 |
| SUPERVISOR / supervisor_chen | 点击工单 9104 | 可查看物流异常详情和跨用户脱敏 Agent/Tool 轨迹。 |
| SUPERVISOR / supervisor_chen | 知识重建按钮 | test profile 安全提示未启用真实 Qdrant/DashScope；不会伪造重建成功。 |

浏览器控制台错误日志：0 条 error。

## 运行时边界

- `test` profile：H2 + Mock AI，用于无外部依赖的界面演示。
- `local,dashscope`：需要 MySQL、Redis、Qdrant、DashScope API Key，才启用真实向量检索与知识重建。
- 退款能力仅创建或推进本地审核工单，绝不调用支付、退款或外部客服系统。
