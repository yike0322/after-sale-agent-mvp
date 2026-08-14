# 演示账号、订单与优惠券

> 全部为虚构、去标识化数据。密码只用于本地 `test` 演示，数据库保存 BCrypt 哈希。

## 账号

| 账号 | 密码 | 角色 | 演示用途 |
| --- | --- | --- | --- |
| `buyer_li` | `Buyer#2026` | CUSTOMER | 优惠券门槛、优惠券过期 |
| `buyer_wang` | `Buyer#2026` | CUSTOMER | 退款审核、未签收、受限品类、物流异常 |
| `supervisor_chen` | `Supervisor#2026` | SUPERVISOR | 跨用户工单列表、知识库重建入口 |

## 订单与优惠券

| 归属 | 编号 | 关键状态 | 金额 | 预期展示 |
| --- | --- | --- | --- | --- |
| buyer_li | `O1001` + `C1001` | AVAILABLE，门槛 100.00 | 80.00 | 未达门槛 |
| buyer_li | `O1002` + `C1002` | EXPIRED | 158.00 | 优惠券已过期 |
| buyer_wang | `O2001` | NORMAL / RECEIVED | 120.00 | 人工退款审核 |
| buyer_wang | `O2002` | NORMAL / PAID | 66.00 | 未签收，不进入退款审核 |
| buyer_wang | `O2003` | RESTRICTED / RECEIVED | 299.00 | 受限品类结束 |
| buyer_wang | `O2004` | NORMAL / LOGISTICS_EXCEPTION | 88.00 | 物流异常人工跟进 |
