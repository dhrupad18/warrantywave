# WarrantyWave Production-Readiness Audit

**Audit date:** 2026-10-01  
**Scope:** Full repository source, dependency manifests/lockfile, application configuration, tests, Docker scripts, and documentation. This is the pre-hardening baseline; statuses below are findings, not a production approval.

## 1. Project Architecture

```text
Angular 17 SPA (same-origin /api; bearer token in localStorage)
  -> Nginx reverse proxy (Docker) or Angular dev-server proxy
  -> Spring Boot 3.3.5 / Java 17 REST API
      -> Spring Security stateless JWT filter + role checks
      -> Controllers -> validated request records / response records
      -> Services + transactional controller methods
      -> Spring Data JPA repositories
      -> MySQL 8.x
```

Registration/login hash passwords with BCrypt and issue signed JWTs. Each authenticated request parses the JWT and looks up the current account/role in MySQL. Customer list/detail access is generally scoped by account email; staff claim and finance review routes do server-side role checks. The application has no refresh/revocation store, external APIs, file uploads, cache, message broker, or separately configured background worker. A scheduled job applies EMI late fees in-process. Error handling maps validation, malformed JSON, and `ResponseStatusException`; other errors fall through to Spring Boot's generic error handling. Logging is framework-default console logging.

Deployment is a PowerShell script that builds two images and starts MySQL/API/Nginx containers on a Docker network. There is no Compose manifest, schema migration tool, CI workflow, production profile, or root `.gitignore`.

## 2. Critical Issues Found

| Priority | Issue | Location | Risk | Status before fixes |
|---|---|---|---|---|
| P0 | Known default JWT signing secret | `backend/src/main/resources/application.properties`, `docker/start-docker.ps1` | Anyone knowing the repository can mint valid JWTs, including `ADMIN`, if deployment omits `JWT_SECRET`. | Confirmed; open |
| P0 | Known/default MySQL credentials and plaintext transport defaults | `application.properties`, `docker/start-docker.ps1` | A deployment can start with predictable DB credentials; DB connection defaults disable TLS and allow public-key retrieval. | Confirmed; open |
| P1 | Vulnerable direct dependencies | `backend/pom.xml`, `frontend/package-lock.json` | Advisory scan found 1 high Maven finding (Spring Boot Actuator 3.3.5) and 18 Angular findings across 4 affected Angular packages at 17.3.12. | Confirmed; remediation required |
| P1 | Customer endpoint records an EMI as paid without payment-provider verification | `WorkflowController.payInstallment`, `Payment`, frontend Pay action | Any authenticated customer can mark their own debt paid without transferring funds; payment record is not evidence of settlement. | Confirmed; provider absent |
| P1 | Automated credit approval trusts client-provided bureau score/income/stability | `FinanceRequest`, `WorkflowController.submitFinanceApplication` | A customer can manipulate underwriting inputs and receive an automatically approved contract/schedule. | Confirmed; verified bureau/income provider absent |
| P1 | No login/register throttling; registration reveals existing accounts | `AuthController`, `SecurityConfiguration` | Credential stuffing/brute-force and email enumeration are not mitigated. | Confirmed; open |
| P1 | Hibernate schema auto-update is active for every profile | `application.properties` | Runtime can mutate production schema without reviewed, ordered, reversible migrations; no migration assets exist. | Confirmed; open |
| P2 | Check-then-act workflows are race-prone | warranty purchase, claim/finance review, EMI payment; repositories have no locks/version fields | Concurrent requests can create duplicate active coverage, conflicting decisions, or an unhandled unique-constraint failure. | Confirmed; open |
| P2 | Unbounded list APIs and finance N+1 reads | `WorkflowController.financeApplications`, list endpoints | Dataset growth causes unbounded response sizes and two additional repository reads per finance application. | Confirmed; open |
| P2 | JWT stored in localStorage; no CSP/security headers on Nginx | `frontend/src/app/api.service.ts`, `docker/nginx.conf` | A future XSS can exfiltrate bearer tokens; browser defense-in-depth is absent. | Confirmed; open |
| P2 | Root container, no health checks/readiness, MySQL exposed on host | Dockerfiles and `start-docker.ps1` | Expanded container impact, brittle startup, and unnecessary public host exposure of the database port. | Confirmed; open |
| P2 | Missing root ignore rules for `.env`, build output, and crash dumps | repository root | Developers can accidentally commit local secrets, generated files, or JVM crash logs. | Confirmed; open |
| P2 | Non-uniform unexpected-error/authorization responses | `ApiExceptionHandler`, security configuration | Generic 500s are not consistently represented; missing/invalid credentials may not use the expected 401 contract. | Confirmed; open |
| P3 | Local VS Code verification tasks are broken | `.vscode/tasks.json` | One task points to a missing script; another registration probe fails under PowerShell argument parsing. | Reproduced; open |

## 3. Security Findings

- **JWT secret fallback (P0):** the source tree contains a valid signing key fallback, and the Docker script repeats it. If `JWT_SECRET` is omitted, an attacker can forge a token with `ROLE_ADMIN`; remove the fallback and reject missing/weak secrets at startup. Side effect: local runs must configure a secret.
- **Database fallback/TLS (P0):** default MySQL user/password and `useSSL=false` are shared by local and production config. Require explicit credentials and separate local configuration from production TLS requirements. Side effect: current local setup needs explicit environment variables.
- **Angular XSS advisories (P1):** locked Angular 17.3.12 packages have multiple current advisories; exploitable contexts depend on use of affected template/SVG/i18n/hydration features, but using affected runtime/compiler versions is still a supply-chain risk. Upgrade the Angular group coherently to a patched supported line and regenerate the lockfile. Side effect: major-version migration and Node/TypeScript compatibility checks.
- **Spring Boot Actuator advisory (P1):** locked Spring Boot 3.3.5 is affected by CVE-2026-22733; the advisory recommends 3.5.12 or later. Upgrade the managed Boot line and retest. The currently exposed actuator surface is only health, which limits this specific attack surface but does not remove the vulnerable dependency.
- **Brute force/account enumeration (P1):** there is no rate limit/lockout, and duplicate registration gives a distinct 409 response. Add edge/application throttling and generic registration responses without causing account-creation ambiguity.
- **Token storage/headers (P2):** bearer JWTs in localStorage are exposed to same-origin script execution. Nginx sets no CSP, nosniff, frame, or referrer policies. Angular interpolation currently escapes values and no unsafe DOM APIs were found, but defense-in-depth is absent. Prefer HttpOnly Secure cookies only with a deliberate CSRF design; add CSP/security headers now.
- **Authorization:** routes are authenticated by default; customer vehicle, warranty, claim, finance, and EMI queries enforce owner scoping. Claim and finance reviews check roles server-side. Admin/customer privilege assignment is not exposed in public registration. No direct IDOR was found in inspected routes. Missing/invalid-token behavior and race cases still need automated security tests.
- **Injection / SSRF / file attacks:** no handwritten SQL, shell execution, external URL fetch, or upload endpoint found. JPA derived queries are parameterized. XSS risk above is dependency/runtime related; templates use Angular interpolation, with no `innerHTML`/bypass sanitizer usage found.

## 4. Functional Bugs

- Payment is a database-only state change, not an actual payment integration; the UI labels it “Pay” and reports success. This must not be presented as real settlement.
- Credit approval and contract creation can occur from self-attested input. Use verified provider data or require staff verification before contract creation.
- Warranty purchase and EMI payment use read/check/write flows without locking. Uniqueness constraints reduce duplicate payment rows but currently surface races as server errors; there is no unique rule for one active warranty per vehicle.
- Claim review allows an already-approved claim to be reviewed again; valid state transitions are not centrally enforced.
- Finance list mapping performs per-row credit-assessment and contract queries; all list endpoints are unpaginated.
- Dates use server-default `LocalDate`/`LocalDateTime` while Hibernate JDBC timezone is UTC; deployment timezone behavior is not explicitly fixed.
- Registration's existence check races with the unique database constraint and may become a generic 500 under concurrent duplicate requests.
- Frontend initial auth uses a `loading` flag not reflected in its template; a saved session can briefly show the sign-in view while `/me` is pending.

## 5. Database Findings

MySQL/JPA entities define primary keys, foreign keys through `@ManyToOne`/`@OneToOne`, unique email/VIN/credit-assessment/contract/payment constraints, and selected query indexes. Owner-scoped queries exist; `open-in-view` is disabled and controller read methods are transactional, so DTO mapping occurs while lazy relations are available. No migration scripts or test database are present. `ddl-auto=update` is global. Several nullable date fields (`VehicleWarranty.startDate/endDate`, `FinanceContract.startDate`) are not marked non-null despite being required by business logic. No optimistic/pessimistic lock strategy is configured. Hikari defaults provide pooling, but pool sizing/timeouts are not explicitly deployment-tuned. N+1 and missing pagination are noted above.

## 6. API Audit

| Method / endpoint | Auth | Authorization / ownership | Validation and response | Findings |
|---|---|---|---|---|
| `POST /api/auth/register` | Public | Always creates CUSTOMER | `Credentials`; session map (token/id/name/email/role), default 200 | Existing-email enumeration; no throttling; concurrent duplicate can become 500 |
| `POST /api/auth/login` | Public | Password hash checked | `LoginRequest`; session map, 401 on bad credentials | No throttling; no refresh/revocation |
| `GET /api/auth/me` | JWT | Current principal only | User map; no request DTO | Protected by catch-all auth rule |
| `GET /api/vehicles` | JWT | Owner email | `VehicleView[]` | Unpaginated |
| `POST /api/vehicles` | JWT | Sets current user as owner | Validated `VehicleRequest`; `VehicleView` | Unique VIN check races; year upper bound in controller |
| `GET /api/warranty/plans` | JWT | Active plans only | `PlanView[]` | Public catalog requires account; unpaginated |
| `GET /api/warranties` | JWT | Vehicle owner | `WarrantyView[]` | Unpaginated; status can be computed as expired without persisting it |
| `POST /api/warranties` | JWT | Requires owned vehicle/active plan | `PurchaseWarrantyRequest`; `WarrantyView` | No actual charge; duplicate concurrent purchases possible |
| `GET /api/claims` | JWT | Owner, or ADJUSTER/ADMIN sees all | `ClaimView[]` | Unpaginated; staff role check is server-side |
| `POST /api/claims` | JWT | Warranty must belong to current user | `ClaimRequest`; `ClaimView` | Eligibility is rule-based and uses current stored mileage |
| `PATCH /api/claims/{id}/review` | JWT | ADJUSTER/ADMIN | `ClaimReviewRequest`; `ClaimView` | Re-review state rules incomplete; race possible |
| `GET /api/finance/applications` | JWT | Owner, or CREDIT_OFFICER/ADMIN sees all | `FinanceView[]` | Unpaginated; N+1 queries |
| `POST /api/finance/applications` | JWT | Requires owned vehicle | `FinanceRequest`; `FinanceView` | Self-attested underwriting can auto-approve and create contract |
| `PATCH /api/finance/applications/{id}/decision` | JWT | CREDIT_OFFICER/ADMIN | `FinanceDecisionRequest`; `FinanceView` | Manual approval creates contract; race possible |
| `GET /api/finance/installments` | JWT | Current customer's contracts only | `EmiView[]` | Unpaginated |
| `POST /api/finance/installments/{id}/pay` | JWT | EMI must belong to current customer | Empty body; `EmiView` | Marks paid without provider proof; concurrency conflict possible |
| `GET /actuator/health` | Public | Health only | Actuator response | No readiness/liveness groups configured |

No duplicate route mapping or handwritten SQL was found. Create endpoints return 200 rather than 201; this is a consistency issue, not an access-control bypass.

## 7. Frontend Findings

Angular is a single standalone component with empty router configuration; `/api` is same-origin through dev proxy or Nginx. The frontend does not embed a localhost API URL, but the Nginx upstream hostname is fixed to `warrantywave-api`, so the image requires that Docker DNS name. JWTs are stored in localStorage. Angular interpolation is used for user content; no direct DOM HTML insertion was found. Form validation exists but server-side validation remains authoritative. The UI displays “all systems operational” without consulting health, labels simulated payment as Pay, and submits user-controlled credit inputs. Only three basic component tests exist; no browser/E2E tests or API contract tests were found. The Angular CLI test runner was not successfully exercised in this audit.

## 8. Dependency Audit

The GitHub Security Advisories scan was run against the declared/resolved direct dependency subset (not the full Maven resolved dependency tree):

- Maven: **1 high finding**, CVE-2026-22733, `spring-boot-starter-actuator@3.3.5`; recommended Spring Boot 3.5.12+.
- npm: **18 findings across 4 affected Angular packages**, primarily `@angular/common`, `@angular/compiler`, `@angular/core`, and `@angular/router` at 17.3.12. Patched recommendations include Angular 20.3.32 for the router advisory and newer supported Angular 19/20/21 patch lines for the others.
- Full transitive Maven scanning and a complete npm advisory audit were not established in this baseline. `jjwt` is pinned at 0.12.6; MySQL Connector/J is BOM-managed and its resolved version was not included in the direct scan.
- Audit JSON evidence is in `docs/audit/maven-direct-cve-report.json` and `docs/audit/npm-direct-cve-report.json`.

## 9. Deployment Readiness

- Backend Maven test task: `BUILD SUCCESS`, but it reported “Nothing to compile” and “No sources to compile” for tests.
- Frontend production build/test: **UNVERIFIED** at baseline.
- Docker build/startup: **UNVERIFIED**; Docker daemon was not assessed.
- The local API health probe failed because nothing was listening on `localhost:8080`.
- Docker script publishes MySQL on host port 3306, uses root and application DB passwords that are identical, hard-codes `FRONTEND_ORIGIN=http://localhost:4200`, starts containers without health checks, and starts the API before confirming MySQL readiness. Existing MySQL containers are merely started, so changed credentials in `.env` do not reconfigure them.
- Backend image runs as root; images use mutable tags; backend image packages with tests skipped. There is no CI/CD workflow, Compose manifest, migration, production profile, TLS/reverse-proxy deployment guide, readiness check, metrics, or structured logging configuration.

## 10. Files Changed Before Remediation

Only this audit artifact and the two scanner evidence files were added before remediation. No application source or configuration was changed while gathering the baseline.

## 11. Verification Performed

- Read all application source packages, API DTOs/controllers/services/repositories, frontend source/config, build files, Docker files/scripts, and project documentation.
- GitHub Security Advisories scan: Maven direct dependencies and selected locked npm dependencies.
- Existing backend task `mvn -f backend/pom.xml test`: exit 0, but incremental compile skipped and there are no Java test sources.
- Frontend `runTests` request: tool reported no tests found for the supplied file.
- `GET /actuator/health`: connection refused at localhost:8080.
- Registration probe task: failed due PowerShell/curl argument parsing, not API behavior.
- Frontend verification task: failed because `.verify-frontend.ps1` does not exist.

## 12. Remaining Risks / UNVERIFIED

Full Maven transitive CVE status, full npm audit status, clean backend build, Angular build/test, app startup against MySQL, integration/browser flows, concurrency under a real database, Docker daemon/build, production TLS/reverse-proxy behavior, rate limits, and real payment/credit-provider integrations are UNVERIFIED. These gaps prevent a production-ready verdict even after repository-level changes.

## 13. Baseline Production Checklist

- [ ] Build (clean build not run)
- [ ] Tests (no backend tests; frontend runner not verified)
- [ ] Authentication (code reviewed; security tests absent)
- [ ] Authorization (ownership/roles reviewed; security tests absent)
- [ ] Input validation (DTO/controller review only)
- [ ] Database (JPA mapping review only; no migrations/integration run)
- [ ] API (route inventory done; live API unavailable)
- [ ] Frontend (source reviewed; build/browser flows unverified)
- [ ] Security (critical secret fallback and dependency findings open)
- [ ] Configuration (unsafe defaults open)
- [ ] Logging (framework default only)
- [ ] Error handling (partial global mapping)
- [ ] Docker (not run; configuration gaps open)
- [ ] Deployment (not verified)
- [ ] Documentation (local setup exists; production guidance incomplete)

## 14. Post-Hardening Update

This section records the workspace state after the baseline audit. The findings above remain useful as the before-fix inventory; statuses below supersede them where stated.

Current framework versions are Spring Boot 3.5.12 / Java 17 and Angular 17.3.x. The backend remains a stateless Spring MVC/JPA monolith and the frontend remains a same-origin Angular SPA.

| Priority | Finding | Current status | Evidence / remaining risk |
|---|---|---|---|
| P0 | Default JWT signing secret | Mitigated in source | `JWT_SECRET` is required and `JwtService` rejects keys shorter than 32 UTF-8 bytes. The Docker launcher also checks it. Secret provisioning/rotation in a production secret manager is UNVERIFIED. |
| P0 | Default database credentials | Partially mitigated | Application datasource credentials are required from environment; the local Docker launcher requires distinct app/root passwords and binds MySQL to loopback. Production TLS and secret-manager configuration are UNVERIFIED. |
| P0 | Production schema mutation | Partially mitigated | `application-prod.properties` uses Hibernate `validate`; the Docker launcher explicitly selects `local`, whose `ddl-auto=update` is for local/demo only. No migration tool or production schema migration process exists, so deployment must not use the local profile. |
| P1 | Simulated EMI payment | Mitigated against false settlement | The authenticated payment route now returns 501 and does not mutate installment state until a provider exists. Real payment integration remains absent. |
| P1 | Self-attested automated finance approval | Mitigated against automatic approval | New applications remain in manual review; client-provided credit inputs are labeled preliminary and no contract is created by submission. Verified underwriting integration remains absent. |
| P1 | Unsafe/uniform API errors | Partially mitigated | Validation, unreadable body, data-integrity, unexpected 500, 401, and 403 paths now return generic JSON detail responses. Authentication/authorization response behavior has not been exercised end-to-end against a database. |
| P1 | Login/register abuse | Partially mitigated | Nginx applies a per-source rate limit to login and registration. Registration still distinguishes a duplicate account; direct exposure outside the Nginx edge and trusted-proxy client-IP handling must be reviewed in production. |
| P2 | Browser security headers | Implemented, UNVERIFIED at runtime | Nginx config adds CSP, nosniff, frame, referrer, and permissions policies. Nginx syntax/runtime verification could not be established in this environment. CSP compatibility with the production Angular bundle has not been browser-tested. |
| P2 | Backend container root user | Implemented, UNVERIFIED at runtime | Backend Dockerfile sets numeric user `10001:10001`; image build could not run because Docker CLI is unavailable. |
| P2 | Local secret/generated-file ignore rules | Implemented | Root `.gitignore` excludes `.env`, backend target, frontend dependencies/build output, and JVM crash logs. Previously tracked generated files are still tracked and were not removed. |
| P1 | Vulnerable dependency lines | Partially mitigated | Spring Boot is now 3.5.12; a CVE check of the resolved-version coordinates supplied for the Boot starters, MySQL connector, and JJWT found no known CVEs requiring fixes. This was not a complete transitive dependency inventory. Angular remains on 17.3.x with the baseline advisories open. The configured Angular 20 install task failed twice with npm `ERESOLVE` while resolving build-angular 17.3.17 against the requested 20.x tree; no Angular manifest or lockfile changes were made. A forced/legacy-peer install was not used. |

### Verification After Changes

- Backend task `mvn -f backend/pom.xml test` after the Boot upgrade: **BUILD SUCCESS**, 3 tests passed (2 JWT-secret tests and 1 no-provider payment test). Maven recompiled all 56 production source files and both test source files against Spring Boot 3.5.12. The Maven `clean` phase was not run.
- Java CVE check of 10 supplied direct/resolved coordinates (Spring Boot starters, MySQL connector, and JJWT): no known CVEs requiring fixes found. Full transitive dependency inventory and npm security rescan are UNVERIFIED.
- Angular patch task (`npm install` targeting Angular 20.3.32/toolchain 20) failed twice with `ERESOLVE`; a lockfile-only retry produced the same resolution conflict. `frontend/package.json` and `frontend/package-lock.json` remained unchanged. No `--force` or `--legacy-peer-deps` bypass was used.
- Frontend production build task `npm --prefix frontend run build`: **PASS**; Angular emitted the production bundle to `frontend/dist/frontend`.
- Frontend test task `npm --prefix frontend test -- --watch=false --browsers=ChromeHeadless`: **PASS**, 3 tests succeeded. ChromeHeadless needed a retry after a 60-second capture timeout and emitted process-shutdown warnings after the successful test result.
- Frontend test runner: no tests found for the supplied Angular spec path; Angular production build and browser behavior are UNVERIFIED.
- Docker/Nginx validation: Docker CLI is unavailable in the terminal, so image build and `nginx -t` are UNVERIFIED.
- Live API, database-backed auth/ownership, race/concurrency behavior, and production deployment remain UNVERIFIED.
- Workspace diagnostics report nullability mismatches in `WorkflowController`. Maven successfully recompiled the sources; request IDs are constrained with `@NotNull`, and `WarrantyPlan.coveredParts` maps to a non-null column. No runtime null failure was reproduced. Existing production-schema consistency is still UNVERIFIED because no live database was available.

### Updated Production Checklist

- [ ] Clean build (`mvn clean` not run)
- [x] Backend compilation (56 production and 2 test source files recompiled successfully)
- [ ] Tests (3 backend unit tests pass; integration/security tests and Angular tests UNVERIFIED)
- [ ] Authentication (secret validation covered by unit tests; end-to-end UNVERIFIED)
- [ ] Authorization (reviewed in source; end-to-end ownership/role tests UNVERIFIED)
- [ ] Input validation (reviewed in DTO/controller source; automated boundary tests UNVERIFIED)
- [ ] Database (entity/schema review only; migrations and live DB verification UNVERIFIED)
- [ ] API (inventory documented; live API verification UNVERIFIED)
- [ ] Frontend (production build and 3 component tests pass; authenticated/API browser flows UNVERIFIED)
- [ ] Security (several source-level mitigations applied; dependency findings and runtime controls remain open)
- [ ] Configuration (required secret/env checks added; production secret provisioning and TLS UNVERIFIED)
- [ ] Logging (generic unexpected-error response added; structured logs/correlation/production levels UNVERIFIED)
- [ ] Error handling (generic JSON handlers and security 401/403 configured; integration behavior UNVERIFIED)
- [ ] Docker (non-root directive added; image build UNVERIFIED)
- [ ] Deployment (production profile/schema migration, TLS, readiness, and startup behavior UNVERIFIED)
- [ ] Documentation (audit findings recorded; full new-developer/production deployment guide remains incomplete)

### Remediation Files

- `backend/pom.xml`: Spring Boot managed dependency line upgraded to 3.5.12.
- `backend/src/main/java/com/warrantywave/security/`: JWT secret startup validation and relocated security components; controller, configuration, and exception handler changes enforce safer payment/finance/error behavior.
- `backend/src/main/resources/`: required environment-backed secrets and distinct local/production profiles; production uses schema validation.
- `backend/src/test/java/`: regression tests for JWT key requirements and refusing to simulate payments.
- `docker/backend.Dockerfile`, `docker/start-docker.ps1`, `frontend/nginx.conf`: non-root backend runtime, required secret checks and loopback port bindings, edge rate limiting and browser security headers.
- `.env.example`, `.gitignore`, `.vscode/tasks.json`, `frontend/src/app/app.component.html`: example environment variables, ignore rules, verification tasks, and UI behavior aligned with unavailable payment/manual credit verification.
- `docs/audit/`: baseline report and dependency advisory evidence, updated with remediation and verification results.

The recursive crash-dump ignore rule was verified: JVM crash files under `backend/` no longer appear as untracked. At audit resume, the worktree also contained deletions under `.idea/` and modifications/deletions under tracked `backend/target/`; these generated/IDE changes were not altered as part of this continuation.
