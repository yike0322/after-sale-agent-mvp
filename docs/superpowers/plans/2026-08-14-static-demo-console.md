# Static Demo Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Add a framework-free browser acceptance console for customer and supervisor demonstrations of the current after-sale Agent.

**Architecture:** Spring Boot serves native HTML, CSS, and JavaScript from src/main/resources/static. A demo-auth layer adds BCrypt password verification and short-lived HMAC-signed Bearer tokens while retaining legacy X-Demo-User-Id tests. Role checks protect controller entry points; MyBatis queries continue to enforce ownership.

**Tech Stack:** Java 21, Spring Boot 3.4.2, MyBatis, Flyway, H2/MySQL, Spring Security Crypto, static HTML/CSS/JavaScript, SSE.

## Global Constraints

- Preserve the user-owned Spring Boot 3.4.2 and Lombok pom.xml changes.
- Add only versioned Flyway migrations; never edit V1.
- No Node.js, npm, CDN, frontend framework, browser secret, direct SQL outside mappers, payment, or real refund action.
- Put generated rules, data, API examples, evidence, and manifest updates in docs/project-trace/2026-08-14.
- CUSTOMER reads only own data. SUPERVISOR reads all ticket traces and can reindex knowledge.
- Every implementation task follows focused RED, focused GREEN, Java 21 full tests, and a scoped commit.

---

### Task 1: Add demo accounts, roles, and login tokens

**Files:**
- Create: src/main/resources/db/migration/V2__add_demo_accounts.sql
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoRole.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoAccount.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoAccountMapper.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoTokenService.java
- Create: src/main/java/com/yike/aftersaleagent/identity/DemoAuthService.java
- Create: src/main/java/com/yike/aftersaleagent/identity/AuthController.java
- Create: src/main/java/com/yike/aftersaleagent/identity/api/LoginRequest.java
- Create: src/main/java/com/yike/aftersaleagent/identity/api/LoginResponse.java
- Modify: pom.xml, application-test.yml, application-local.yml, CurrentDemoUser.java, DemoUserMapper.java, DemoDataInitializer.java, DemoUserInterceptor.java, ErrorCode.java
- Test: src/test/java/com/yike/aftersaleagent/identity/DemoAuthServiceTest.java
- Test: src/test/java/com/yike/aftersaleagent/identity/AuthControllerTest.java

**Interfaces:**
- DemoRole has CUSTOMER and SUPERVISOR.
- CurrentDemoUser is (long id, String displayName, DemoRole role) with a two-argument CUSTOMER compatibility constructor.
- DemoAuthService.login(LoginRequest) returns LoginResponse.
- DemoTokenService.issue(CurrentDemoUser) returns String; verify(String) returns CurrentDemoUser.
- POST /api/auth/login returns token, userId, displayName, role, and expiresAt.

- [ ] **Step 1: Write failing tests**

    @Test
    void validCustomerPasswordIssuesVerifiableToken() {
        LoginResponse login = authService.login(new LoginRequest("buyer_li", "Buyer#2026"));
        assertThat(login.role()).isEqualTo("CUSTOMER");
        assertThat(tokenService.verify(login.token()).displayName()).isEqualTo("李晓雨");
    }

    @Test
    void invalidPasswordUsesStableError() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("buyer_li", "wrong")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
            .isEqualTo("AUTH_INVALID_CREDENTIALS");
    }

- [ ] **Step 2: Run RED**

Run: .\mvnw.cmd test '-Dtest=DemoAuthServiceTest,AuthControllerTest'

Expected: compilation fails because account/auth types and the login endpoint do not exist.

- [ ] **Step 3: Implement persistence, password verification, and tokens**

    CREATE TABLE demo_account (
      user_id BIGINT NOT NULL PRIMARY KEY,
      account VARCHAR(64) NOT NULL UNIQUE,
      password_hash VARCHAR(100) NOT NULL,
      role VARCHAR(32) NOT NULL,
      enabled BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

Add Spring Security Crypto without changing the user-owned Boot version. Seed BCrypt hashes for buyer_li, buyer_wang, and supervisor_chen, never plaintext password values. Sign a Base64URL token containing userId, role, and expiration with HMAC-SHA256. Configure a test signing secret and a local environment variable named DEMO_AUTH_HMAC_SECRET.

- [ ] **Step 4: Add controller and interceptor precedence**

    if (authorization != null && authorization.startsWith("Bearer ")) {
        return tokenService.verify(authorization.substring(7));
    }
    return findLegacyDemoUser(request.getHeader("X-Demo-User-Id"));

Return AUTH_INVALID_CREDENTIALS and AUTH_TOKEN_INVALID as 401 errors. Browser traffic sends Bearer tokens; existing header tests keep working.

- [ ] **Step 5: Run GREEN and full suite**

Run:
    .\mvnw.cmd test '-Dtest=DemoAuthServiceTest,AuthControllerTest,DemoUserInterceptorTest'
    .\mvnw.cmd test

Expected: login works for both roles; malformed tokens fail; legacy tests stay green.

- [ ] **Step 6: Commit**

    git add pom.xml src/main/resources/db/migration/V2__add_demo_accounts.sql src/main/java/com/yike/aftersaleagent/identity src/main/resources/application-test.yml src/main/resources/application-local.yml src/main/java/com/yike/aftersaleagent/common/api/ErrorCode.java src/test/java/com/yike/aftersaleagent/identity
    git commit -m "feat: add demo role login"

### Task 2: Add customer/supervisor ticket API boundaries

**Files:**
- Create: src/main/java/com/yike/aftersaleagent/identity/SupervisorAuthorization.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/TicketListItemRecord.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/api/TicketListItemResponse.java
- Create: src/main/java/com/yike/aftersaleagent/ticket/api/TicketListResponse.java
- Modify: TicketMapper.java, TicketQueryService.java, TicketController.java, KnowledgeReindexController.java, ErrorCode.java
- Test: src/test/java/com/yike/aftersaleagent/ticket/TicketRoleControllerTest.java
- Test: src/test/java/com/yike/aftersaleagent/knowledge/KnowledgeReindexRoleTest.java

**Interfaces:**
- SupervisorAuthorization.requireSupervisor(CurrentDemoUser) returns void.
- GET /api/tickets/mine with optional status lists customer tickets.
- GET /api/supervisor/tickets with optional status lists all tickets.
- TicketQueryService adds listOwnedTickets(long,String) and listAllTickets(String).

- [ ] **Step 1: Write failing role tests**

    @Test
    void customerCannotUseSupervisorList() throws Exception {
        mockMvc.perform(get("/api/supervisor/tickets").header("Authorization", customerBearer))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_ROLE_FORBIDDEN"));
    }

    @Test
    void supervisorCanListAndCustomerListHidesOwnerIdentity() throws Exception {
        mockMvc.perform(get("/api/supervisor/tickets").header("Authorization", supervisorBearer))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/mine").header("Authorization", customerBearer))
            .andExpect(jsonPath("$.data.items[0].userId").doesNotExist());
    }

- [ ] **Step 2: Run RED**

Run: .\mvnw.cmd test '-Dtest=TicketRoleControllerTest,KnowledgeReindexRoleTest'

Expected: supervisor authorization and role routes do not exist.

- [ ] **Step 3: Implement role check and mapper queries**

    public void requireSupervisor(CurrentDemoUser user) {
        if (user.role() != DemoRole.SUPERVISOR) {
            throw new BusinessException(ErrorCode.AUTH_ROLE_FORBIDDEN);
        }
    }

    SELECT ct.id AS ticketId, ct.ticket_type AS ticketType, ct.status, ct.priority,
           tt.current_step AS currentStep, tt.total_steps AS totalSteps, ct.updated_at AS updatedAt
    FROM customer_ticket ct JOIN ticket_task tt ON tt.ticket_id = ct.id
    WHERE ct.user_id = #{userId}
      AND (#{status} IS NULL OR ct.status = #{status})
    ORDER BY ct.updated_at DESC, ct.id DESC

Validate status against TicketTaskStatus. Customer response excludes userId, token, requestId, description, raw message, and audit internals. Supervisor authorization gates global list and knowledge reindex.

- [ ] **Step 4: Run GREEN and commit**

Run:
    .\mvnw.cmd test '-Dtest=TicketRoleControllerTest,KnowledgeReindexRoleTest,TicketControllerTest'
    .\mvnw.cmd test

Then:
    git add src/main/java/com/yike/aftersaleagent/identity/SupervisorAuthorization.java src/main/java/com/yike/aftersaleagent/ticket src/main/java/com/yike/aftersaleagent/knowledge/KnowledgeReindexController.java src/main/java/com/yike/aftersaleagent/common/api/ErrorCode.java src/test/java/com/yike/aftersaleagent/ticket/TicketRoleControllerTest.java src/test/java/com/yike/aftersaleagent/knowledge/KnowledgeReindexRoleTest.java
    git commit -m "feat: add role-aware ticket operations"

### Task 3: Add fictional policies and comprehensive demo data

**Files:**
- Create: docs/project-trace/2026-08-14/business-rules/after-sale-policy.md
- Create: docs/project-trace/2026-08-14/business-rules/coupon-policy.md
- Create: docs/project-trace/2026-08-14/business-rules/refund-review-policy.md
- Create: docs/project-trace/2026-08-14/demo-data/accounts-and-orders.md
- Create: docs/project-trace/2026-08-14/demo-data/tickets-and-traces.md
- Modify: DemoDataInitializer.java, DemoUserMapper.java, manifest.md
- Test: src/test/java/com/yike/aftersaleagent/identity/DemoDataInitializerTest.java

**Interfaces:** Seed buyer_li, buyer_wang, supervisor_chen; six fictional order/coupon scenarios; and four historical ticket traces. Cases include threshold miss, coupon expiry, received-normal review, paid/unreceived finish, restricted-product finish, and logistics exception.

- [ ] **Step 1: Write failing seed test**

    @Test
    void seedsRoleAccountsAndScenarioCoverage() {
        assertThat(accountMapper.findByAccount("buyer_li").role()).isEqualTo("CUSTOMER");
        assertThat(accountMapper.findByAccount("supervisor_chen").role()).isEqualTo("SUPERVISOR");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_info", Integer.class)).isGreaterThanOrEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_ticket", Integer.class)).isGreaterThanOrEqualTo(4);
    }

- [ ] **Step 2: Run RED**

Run: .\mvnw.cmd test '-Dtest=DemoDataInitializerTest'

Expected: current seed data lacks accounts and the documented scenarios.

- [ ] **Step 3: Seed and document controlled cases**

Seed O1001 threshold miss, O1002 expired coupon, O2001 received-normal review, O2002 paid/unreceived finish, O2003 restricted-product finish, and O2004 logistics exception. Use fictional names and safe ticket log summaries. Write explicit policy priority and expected Java decision in each business-rule document. List every document in manifest.md and mark it fictional/de-identified.

- [ ] **Step 4: Run GREEN and commit**

Run:
    .\mvnw.cmd test '-Dtest=DemoDataInitializerTest,RefundWorkflowIntegrationTest,CouponAnalysisWorkflowIntegrationTest'
    .\mvnw.cmd test

Then:
    git add docs/project-trace/2026-08-14 src/main/java/com/yike/aftersaleagent/identity/DemoDataInitializer.java src/main/java/com/yike/aftersaleagent/identity/DemoUserMapper.java src/test/java/com/yike/aftersaleagent/identity/DemoDataInitializerTest.java
    git commit -m "feat: add realistic demo scenarios"

### Task 4: Build static browser console

**Files:**
- Create: src/main/resources/static/index.html
- Create: src/main/resources/static/app.css
- Create: src/main/resources/static/app.js
- Create: docs/project-trace/2026-08-14/api-examples/browser-console.md
- Modify: docs/project-trace/2026-08-14/manifest.md
- Test: src/test/java/com/yike/aftersaleagent/web/StaticConsoleTest.java

**Interfaces:** The page consumes login, sessions, POST chat SSE, customer/supervisor ticket lists, ticket detail, ticket trace, and supervisor knowledge reindex. Hash routes are #login, #customer, and #supervisor.

- [ ] **Step 1: Write failing static resource test**

    @Test
    void servesConsoleAndScript() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("售后工单 Agent 控制台")));
        mockMvc.perform(get("/app.js"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/javascript"));
    }

- [ ] **Step 2: Run RED**

Run: .\mvnw.cmd test '-Dtest=StaticConsoleTest'

Expected: static console assets do not exist.

- [ ] **Step 3: Implement HTML and CSS**

Create semantic elements named login-view, customer-view, supervisor-view, chat-form, event-timeline, citation-list, my-ticket-list, ticket-detail, ticket-trace, supervisor-ticket-list, and reindex-button. Use responsive CSS, high-contrast state chips, keyboard-visible focus, and aria-live="polite" for SSE updates.

- [ ] **Step 4: Implement safe browser API/SSE client**

    async function api(path, options = {}) {
      const headers = new Headers(options.headers || {});
      const token = sessionStorage.getItem('demoToken');
      if (token) headers.set('Authorization', 'Bearer ' + token);
      const response = await fetch(path, { ...options, headers });
      const body = await response.json();
      if (!response.ok || !body.success) throw new Error(body.code || 'COMMON_INTERNAL_ERROR');
      return body.data;
    }

    function appendText(node, value) {
      node.textContent = value == null ? '' : String(value);
    }

Use textContent, never innerHTML, for all API, SSE, citation, and trace data. Render only status, ticket, message, error, and done SSE events. Store only token and safe user profile in sessionStorage, then clear both on logout.

- [ ] **Step 5: Write browser API examples, verify, and commit**

Run:
    .\mvnw.cmd test '-Dtest=StaticConsoleTest,AuthControllerTest,ChatControllerSseTest'
    .\mvnw.cmd test

Then:
    git add src/main/resources/static docs/project-trace/2026-08-14/api-examples/browser-console.md docs/project-trace/2026-08-14/manifest.md src/test/java/com/yike/aftersaleagent/web/StaticConsoleTest.java
    git commit -m "feat: add static agent demo console"

### Task 5: Acceptance evidence and README

**Files:**
- Create: docs/project-trace/2026-08-14/test-evidence/static-console-acceptance.md
- Modify: docs/project-trace/2026-08-14/manifest.md and README.md

- [ ] **Step 1: Record manual acceptance matrix**

    | Role | Action | Expected result |
    | --- | --- | --- |
    | CUSTOMER | Login buyer_li | Customer workspace only |
    | CUSTOMER | Coupon O1001 and C1001 | SSE steps and threshold result |
    | CUSTOMER | Refund O2001 | ticket, five steps, WAIT_HUMAN, done |
    | SUPERVISOR | Login supervisor_chen | Cross-user ticket list |
    | SUPERVISOR | Reindex local,dashscope | Source/chunk result |

- [ ] **Step 2: Final verification and documentation**

Run:
    .\mvnw.cmd test
    git diff --check
    git status --short

Update README with browser URL, test-profile startup, demo-only credentials boundary, role capabilities, and local,dashscope prerequisites. Include local variables DEMO_AUTH_HMAC_SECRET and DASHSCOPE_API_KEY. Update manifest with every evidence file.

- [ ] **Step 3: Commit and push**

    git add README.md docs/project-trace/2026-08-14
    git commit -m "docs: add static console acceptance guide"
    git push origin feature/after-sale-agent-mvp

## Plan Self-Review

- Coverage: Task 1 implements login; Task 2 implements role boundaries; Task 3 builds fictional business content; Task 4 exposes every backend capability in the browser; Task 5 records reproducible acceptance evidence.
- Consistency: Task 1 supplies role/token contracts used by Tasks 2 and 4. Task 2 supplies routes consumed by Task 4. Task 3 supplies scenarios rendered by Task 4.
- Scope: Production SSO, real personal data, payment/refund execution, and frontend build tooling are excluded.

