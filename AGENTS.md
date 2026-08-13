# Repository Guardrails

- Java package: `com.yike.aftersaleagent`.
- Maven coordinates: `com.yike:after-sale-agent:0.0.1-SNAPSHOT`.
- Once the Maven wrapper exists, run Maven build and test commands on Windows with `./mvnw.cmd test`.
- API keys, passwords, `.env`, and real credentials must never be committed.
- Model, Controller, and Tool code must never execute direct SQL. Only Mapper/repository data-access code reaches MySQL.
- Every Tool must have allow-list registration, typed input validation, authorization based on `CurrentDemoUser`, timeout and error handling, and an audit record.
- Refund paths may only create or update a `WAIT_HUMAN` ticket. They must not make payments, issue refunds, change accounts, or perform any other irreversible action.
- Follow failing-test-first: first run and record the focused failing test, implement the minimal change, run the focused green test, run `./mvnw.cmd test`, then commit.
