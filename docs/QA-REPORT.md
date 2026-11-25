# RideMatch — Quality Assurance and Code Review Report

**Review date:** 19 November 2026  
**Scope:** Source code, tests, Maven configuration, database scripts, templates, and documentation.  
**Overall status:** **Not release-ready / not verified**

## 1A. Admin, schedule, and notification feature handoff

The latest source changes add an admin community detail view, quick/custom ride date and time selection, optional flexible departure windows, and in-app notifications to other active verified community members when a ride is created.

- Manual scenarios and their unexecuted status: [Manual-Test-Cases.md](Manual-Test-Cases.md).
- LOC snapshot in that document counts current physical lines in feature-related files, not added-line diff LOC; there is no tracked pre-change baseline in this workspace.
- A compile-only attempt on 23 November 2026 using mvn -q -DskipTests compile failed before compilation because Maven could not open a lock file under the local .m2 repository. It did not execute the test suite.

## 1. Test execution report

Attempted command: `mvn test` from the project root.

**Result:** Test execution did not start. PowerShell reported that `mvn` is not recognized. The environment contains Corretto 8 only; the project requires Java 17. No Maven wrapper (`mvnw`/`mvnw.cmd`) is present. No test class was compiled or executed, so there is no pass/fail result.

Source scan found **37 JUnit 5 test methods** after this review's additions. They use Mockito for service-layer dependencies and direct Jakarta Bean Validation for registration DTOs. This count is a source inventory, not a count of executed tests.

| Area | Source coverage present | Execution status / gap |
|---|---|---|
| Registration and duplicate email | AuthService unit tests; registration validation tests | Not executed. |
| Invalid login | None | Missing Spring Security integration test. |
| Community verification and admin service checks | CommunityService unit tests | Not executed; no HTTP authorization test. |
| Ride creation | RideService unit test added | Not executed; no controller validation/integration test. |
| Matching | Exact, nearby, time, community, full/cancelled/completed/own, duplicate membership, past departure, ranking tests | Not executed. |
| Join request, full capacity, duplicate, unverified user, approval | RideGroupService unit tests | Not executed; no two-thread/database capacity test. |
| Cancellation and replacement | Service tests for cancellation start, invitation, timeout, and early timer callback | Not executed; no application restart integration test. |
| Fare and policy | Fare split and cancellation policy unit tests | Not executed. |
| Repository/schema | None | Missing MySQL/H2 repository tests and schema smoke test. |
| Controller integration | None | Missing MockMvc or end-to-end test suite. |
| Central exception handling | None | Missing test coverage. |
| Concurrent final-seat requests | None | Missing test against a transactional database. |
| Browser responsiveness/screenshots | None | Application could not be launched; visual review not performed. |

## 2. Code review findings

### Addressed in this review (source changes; still unexecuted)

1. **Past/duplicate matching results:** ride matching now excludes departed rides and rides where the searching user already has a membership. Create/search forms reject past departure times; service methods repeat the guard.
2. **Replacement timeout lock order:** the expired-decline path no longer tries to lock a ride while holding the replacement-search lock. Early scheduler callbacks reschedule at the persisted deadline.
3. **Focused test coverage:** added tests for past/duplicate match exclusion, ride creation, unverified join denial, duplicate ride membership, available-seat approval, and early timeout rescheduling.

### Remaining issues and risks

| Priority | Finding | Impact / next action |
|---|---|---|
| P1 | No Java 17/Maven build or test execution. | Install JDK 17 and Maven, then run `mvn test`; resolve compile/runtime failures before accepting behavior. |
| P1 | No database/repository or concurrency test. | Run schema setup on a disposable MySQL database and test simultaneous acceptance of the last seat with separate transactions. |
| P1 | No controller/authentication integration tests. | Verify invalid login, USER access to `/admin`, ADMIN access, CSRF-protected forms, validation rendering, and exception pages with MockMvc. |
| P1 | Hibernate schema creation/migration is unverified. | Exercise fresh database startup and the one-time migration against a copy of the prior schema; inspect resulting indexes, foreign keys, nullability, and enum values. |
| P2 | Replacement selection is not route/time/availability matching. | Current source invites verified community members who have no ride membership, sequentially. Decide whether this is acceptable for MVP or require an explicit opt-in/route intent before invitations. |
| P2 | Airport support is generic manual date/time only. | The ride creation flow hardcodes `RideKind.STANDARD`; add an explicit airport journey type only if retained as a requirement. |
| P2 | Cancellation actual-loss review has no operational UI/service. | Current policy flags review based on taxi-arranged status and timing; it does not record an actual loss through a workflow. Keep this described as advisory-only. |
| P2 | Community page does not display a user's pending/rejected membership status. | A user may see “Request membership” again and receive a conflict page for an existing request. Add per-community membership status to the view model. |
| P2 | Database configuration uses `ddl-auto=update`. | Appropriate only for development; establish controlled migrations and set production/test schema behavior explicitly. |
| P2 | Scheduler is in-process. | A process outage can delay timeout handling until the application restarts; do not describe the deadline as a hard real-time guarantee. |
| P3 | No screenshots or responsive browser review. | Capture landing, auth, dashboard, rides, requests, group, notifications, profile, and admin pages after a successful run at desktop/mobile sizes. |
| P3 | Historical `CabSharing-*` proposal docs remain alongside RideMatch docs. | Current README should identify the RideMatch docs as current and the older files as historical drafts. |

## 3. Design/code review summary

- **Architecture:** Source follows a controller/service/repository/JPA modular monolith. No unnecessary distributed infrastructure was found.
- **OOP:** Entities use private state and controlled methods. `AuditableEntity` shares timestamp behavior. There is no forced domain inheritance tree.
- **SOLID:** SRP is visible in controllers/services/repositories. Services depend on repository interfaces and framework contracts. OCP is modestly supported by matching properties; there is no broad pluggable matching strategy. LSP/ISP are not artificially claimed.
- **Validation:** DTO constraints exist for names, email, password, fare, capacity, notes, date/time presence; this review added future-date/time guards. Most DTO rules still lack direct validation tests.
- **Security:** BCrypt, role-based admin restrictions, CSRF defaults, and service-level community checks are present in source. These controls are not integration-tested.
- **Database:** JPA relationships, unique constraints, indexes, and pessimistic ride locking are visible in source. Actual MySQL schema has not been verified.
- **Transactions:** Group-mutating service methods are annotated transactional and use ride-row locks. Actual isolation/locking behavior remains unproven without database tests.
- **Exceptions:** Application exceptions map to centralized HTML error views; unexpected exceptions are logged and presented generically. No handler tests exist.
- **Dependencies:** Maven dependencies appear aligned with the stated stack. Lombok is omitted. Maven dependency resolution was not possible here.

## 4. Screenshots and evidence

No screenshots are included. A genuine UI capture requires a successful JDK 17/Maven build, MySQL configuration, application startup, and browser session. No static/template mockups are presented as runtime screenshots.

## 5. Release decision

The codebase is suitable for continued college-project development, but it cannot be signed off as a verified release. Build/test execution, database integration, security/controller integration, concurrent capacity testing, and browser screenshots remain outstanding.

