# 退款人工审核规则（演示数据）

> 这是安全的“审核流转”演示，不连接支付渠道，不产生真实退款。

| 条件 | 示例订单 | 工单状态 | 预期原因 |
| --- | --- | --- | --- |
| 普通品、已签收 | `O2001` | `WAIT_HUMAN` | `ELIGIBLE_HUMAN_REVIEW` |
| 已付款、未签收 | `O2002` | `FINISHED` | `ORDER_NOT_RECEIVED` |
| 受限品类 | `O2003` | `FINISHED` | `RESTRICTED_PRODUCT` |
| 物流异常 | `O2004` | `WAIT_HUMAN` | `LOGISTICS_ESCALATED` |

退款 Plan-Executor 以订单查询、规则检索、规则判断、工单创建、结果汇总的阶段记录执行状态。证据不足、越权或 Tool 失败时优先收敛到人工处理，禁止补造结论。
