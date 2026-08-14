# 历史工单与可审计轨迹（演示数据）

所有摘要均为受控字段：不含密码、令牌、原始聊天内容或个人敏感信息。

| 工单 | 归属 | 类型 / 状态 | 用途 |
| --- | --- | --- | --- |
| 9101 | buyer_li | COUPON_ANALYSIS / FINISHED | 未达门槛轨迹 |
| 9102 | buyer_li | COUPON_ANALYSIS / FINISHED | 过期优惠券轨迹 |
| 9103 | buyer_wang | REFUND_REVIEW / WAIT_HUMAN | 五步退款审核的历史结论 |
| 9104 | buyer_wang | LOGISTICS_EXCEPTION / WAIT_HUMAN | 物流异常升级 |

每条历史工单具有 `ticket_task` 状态、`agent_step_log` 路由/决策步骤及 `tool_call_log` 的订单查询摘要。客户接口以 owner predicate 限制；主管接口可读取列表用于演示治理视图。
