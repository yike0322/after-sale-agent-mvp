# RAG 检索评估

本目录的 `questions.json` 是可重复执行的 20 条演示检索集。每条问题都有受控的
场景、预期来源路径和证据短语；它只用于核对检索到的引用，不自动判断大模型答案质量。

## 前置条件

先在 Windows 管理员 PowerShell 完成 `wsl --install` 并重启，随后完成 Docker Desktop
初始化；再设置 `.env` 中的基础设施变量和环境变量 `DASHSCOPE_API_KEY`。当前仓库没有
提交任何密钥，也不会在 mock/test 环境连接 Qdrant 或模型服务。

```powershell
docker compose --env-file .env up -d qdrant
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,dashscope" "-Dspring-boot.run.arguments=--aftersale.rag-evaluation.run=true"
```

应用会先按需使用已有知识库向量；若首次执行，请以已验证的 Demo 用户请求
`POST /api/knowledge/reindex`，再显式运行评估。每次显式运行会在
`target/rag-evaluation/` 生成带时间戳的 Markdown 报告。

报告包含案例数、Recall@3、引用路径正确数和一列空白的“人工答案相关性”栏。得分仅依据
当次真正返回的引用及其证据文本，不会捏造 Precision、回答质量或模型评分。

当前尚未完成 Docker/WSL 与 DashScope API Key 的本地联调，因此没有可报告的聚合结果；
这些指标仍是 **unmeasured**，不能当作项目性能数据写入简历。
