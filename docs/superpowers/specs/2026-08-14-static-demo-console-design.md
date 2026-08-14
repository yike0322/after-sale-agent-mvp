# Static Demo Console Design

## Goal

Add a browser-based acceptance console for the existing after-sale Agent backend. The console must make the FAQ/RAG, coupon analysis, refund review workflow, SSE progress, ticket trace, and knowledge reindex capabilities visible without adding a Node.js frontend project.

The console is an MVP demo interface, not a production customer-service portal. Refund requests remain local `WAIT_HUMAN` review tickets only.

## Delivery Shape

- Serve one static HTML application from `src/main/resources/static/`.
- Use browser-native HTML, CSS, and JavaScript only; no npm, bundler, framework, or CDN dependency.
- Add a two-role demo login: `CUSTOMER` and `SUPERVISOR`.
- Keep the user's existing `pom.xml` change (Spring Boot 3.4.2 and explicit Lombok version) untouched.
- Add generated business rules, demo data, API examples, test evidence, and a manifest under `docs/project-trace/2026-08-14/`.

## UI

### Login

The root page presents a login form with account, password, and role-aware error messages. Successful login stores the short-lived demo token in `sessionStorage`; logout clears it.

Built-in demo accounts are documented in the trace folder. Passwords are stored as BCrypt hashes, never as browser or database plaintext.

### Customer Workspace

- Conversation composer and SSE event timeline.
- FAQ/RAG citations displayed as source cards.
- Coupon outcome displayed as a deterministic rule-result card.
- Refund outcome displayed as ticket status, five-step progress, and human-review boundary text.
- “My tickets” panel, ticket detail, and sanitized trace views.

### Supervisor Workspace

- Cross-user ticket list with status filters.
- Selected ticket detail and trace.
- Knowledge reindex action and visible source/chunk result.
- No payment, refund approval, account change, or irreversible operation.

## Backend Contract Changes

### Authentication and authorization

- Add demo account and role persistence with BCrypt password hashes.
- `POST /api/auth/login` accepts account/password and returns a signed demo token plus the safe user profile.
- Requests use `Authorization: Bearer <token>` after login.
- Keep `X-Demo-User-Id` compatibility for existing API tests during migration; the browser uses Bearer tokens.
- Resolve the authenticated principal at the interceptor boundary. Request body, JavaScript, model output, and SSE payloads never choose identity or role.
- Customer: own sessions, own chat, own tickets, own trace.
- Supervisor: all ticket list/detail/trace plus knowledge reindex.

### Dashboard APIs

- Add a customer-safe ticket list endpoint.
- Add a supervisor-only ticket list endpoint with bounded status filter.
- Keep existing owner-scoped ticket detail/trace APIs; grant supervisor reads only through explicit mapper predicates.
- Keep `/api/knowledge/reindex` supervisor-only.

### Static assets

- `index.html`: semantic shell and login/workspace regions.
- `app.css`: responsive, accessible layout for desktop demos and narrow windows.
- `app.js`: login, API wrapper, SSE parser, ticket/trace rendering, role-aware navigation, and safe error handling.
- No user raw message, token, password, database error, prompt, stack trace, or internal request ID is rendered into an audit summary.

## Data and Rules

Create plausible but fictional/de-identified e-commerce data:

- two roles, multiple customer accounts, supervisor account;
- normal, restricted, delivered/received, paid, and abnormal orders;
- available, expired, threshold-miss, and exhausted coupons;
- FAQ, refund, return, logistics, complaint, product, and coupon knowledge documents;
- tickets in PENDING, RUNNING, WAIT_HUMAN, FINISHED, and FAILED states with controlled traces.

Business-rule documents describe explicit demo policy boundaries. Java remains the source of truth for coupon and refund conclusions; RAG supplies evidence and citations only.

## Error Handling and Security

- Unauthorized: 401; authenticated but role-forbidden: 403; absent/foreign ticket: unified 404.
- SSE emits stable project error codes and one terminal `done` event.
- Password verification and token validation never log credentials.
- All ticket and trace reads remain owner/role predicates in SQL, not post-query Java filtering.
- Generated data and documents contain no real personal, payment, API-key, or production-order data.

## Verification

- Unit tests for login/password verification/token validation and role checks.
- MVC tests for customer/supervisor API boundaries and IDOR denial.
- Browser-independent integration tests for static asset serving and SSE response contract.
- Full Maven test suite must pass under Java 21.
- Manual acceptance script: login as customer, submit FAQ/coupon/refund, inspect ticket; login as supervisor, inspect cross-user ticket and reindex knowledge.

## Traceability

`docs/project-trace/2026-08-14/manifest.md` lists every generated asset and its purpose. Its sibling folders hold fictional data, rules, API examples, and test evidence. The source implementation stays under `src/` so the application remains buildable using normal Spring Boot conventions.
