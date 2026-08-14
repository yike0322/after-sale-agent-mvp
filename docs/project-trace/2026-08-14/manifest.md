# 2026-08-14 Generation Manifest

This directory is the audit trail for the static demo-console feature.

| Asset group | Location | Purpose |
| --- | --- | --- |
| Design specification | `../../superpowers/specs/2026-08-14-static-demo-console-design.md` | Approved technical and product design. |
| Business rules | `business-rules/` | Fictional, explicit after-sale policy used by the demo. |
| Demo data | `demo-data/` | De-identified customers, orders, coupons, and tickets. |
| API examples | `api-examples/` | Browser and manual acceptance requests. |
| Test evidence | `test-evidence/` | Commands and expected acceptance outcomes. |

Implementation source files are intentionally kept under `src/` rather than copied here. Each generated source asset will be listed in this manifest when implemented.

## 已生成资产

| 文件 | 分类 | 数据性质 |
| --- | --- | --- |
| `business-rules/after-sale-policy.md` | 售后规则 | 虚构、去标识化 |
| `business-rules/coupon-policy.md` | 优惠券规则 | 虚构、去标识化 |
| `business-rules/refund-review-policy.md` | 退款审核规则 | 虚构、去标识化 |
| `demo-data/accounts-and-orders.md` | 账号与订单 | 虚构、去标识化 |
| `demo-data/tickets-and-traces.md` | 工单与轨迹 | 虚构、去标识化 |
