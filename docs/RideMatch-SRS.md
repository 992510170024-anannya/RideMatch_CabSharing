# RideMatch — Software Requirements Specification

**Version:** 1.1 · **Status:** As-built review snapshot · **Review date:** 19 November 2026

This document replaces the earlier CabSharing requirements draft for the current RideMatch source tree. Source presence is not proof of runtime behavior: the application could not be built or launched in the review environment, so requirements marked “source present” remain execution-unverified.

## 1. Purpose and objectives

RideMatch helps approved members of a college or company find and coordinate shared taxi journeys. Its MVP demonstrates a Java 25, Spring Boot, MySQL, and Thymeleaf layered application with validated forms, community membership verification, rule-based ride matching, group coordination, estimated fare splitting, and replacement invitations.

The coordinator contacts and books a taxi outside RideMatch. Fare amounts are estimates. The application does not collect payment or cancellation penalties.

## 2. Current problem and proposed system

Students, employees, and travelers may book separate taxis despite travelling from the same origin toward similar destinations around the same time. Informal coordination can make compatible travelers harder to find and fare estimates harder to compare.

RideMatch lets an approved member create a ride with a route, date/time, capacity, estimated fare, and optional notes. Another approved member of the same community can search compatible rides and request a seat. The ride coordinator accepts or declines requests. Confirmed members see an equal estimated fare contribution. When a confirmed member cancels, the application starts a time-limited replacement invitation search.

Airport arrival times are user-entered as ordinary ride date/time values. There is no live flight API or dedicated airport workflow in the current creation form.

## 3. Scope and limitations

### 3.1 MVP scope represented in source

- Account registration, form login/logout, BCrypt password hashing, and USER/ADMIN roles.
- College/company community registration by an administrator, membership requests, and administrator approval/rejection.
- Verified-community ride creation, search, coordinator approval of passenger requests, group display, and cancellation.
- Configurable departure difference and nearby destination alias groups.
- Confirmed-seat count, equal estimated fare contribution, in-app notifications, and cancellation history.
- Sequential replacement invitations with a configurable two-minute timeout and user consent.
- Responsive Thymeleaf pages using Bootstrap and shared fragments.

All of the above are **source-present, execution-unverified** in this review snapshot.

### 3.2 Known limitations

- No taxi booking, dispatch, in-app payment, or actual penalty collection.
- Ride matching uses normalized pickup names, configured destination aliases, date, and time. It does not use maps or geospatial distance.
- Replacement invitations go to verified community members without an existing ride membership, sequentially. They are not ranked by saved route intent or availability.
- Route preferences are not stored. The dashboard links users to an explicit search form instead of showing personalized suggestions.
- Airport trips have no distinct arrival/flight workflow; users enter a date and time manually.
- The in-process scheduler requires the application to run. Open deadlines are persisted and rescheduled after restart, but restart behavior has not been exercised.
- The current setup uses Hibernate `ddl-auto=update`; a production migration/validation workflow is not established.

### 3.3 Future scope

Map-based route/distance matching, saved commute preferences, flight status, provider booking, payment, and advanced notifications remain future work and are not implemented requirements.

## 4. Stakeholders and roles

| Role | Current permissions represented in source |
|---|---|
| Visitor | View landing page, register, and open login page. |
| USER | Request community membership; use ride pages only after community approval; manage own profile, ride memberships, and notifications. |
| Ride coordinator | A USER who created the ride; review its join requests, record an external taxi arrangement, and cancel the group ride. |
| ADMIN | Register communities and approve/reject community membership requests. |
| Taxi provider | Outside the RideMatch system boundary. |

Community approval is global ADMIN approval in the current implementation; the source does not model a separate per-community verifier role.

## 5. Functional requirements and implementation trace

| ID | Requirement | Source status |
|---|---|---|
| FR-01 | Register users with validated name/email/password; store a BCrypt hash; assign USER by default. | Source present; unverified. |
| FR-02 | Let an administrator register communities and approve/reject pending membership. | Source present; unverified. |
| FR-03 | Require an approved membership in the ride's community before ride operations. | Source present; unverified. |
| FR-04 | Let verified members create a ride with pickup, destination, future date/time, capacity, estimated fare, and optional notes. | Source present; future-time guards added; unverified. |
| FR-05 | Accept manually entered airport arrival time without flight API access. | Partial: date/time can be entered, but there is no dedicated airport ride kind in the creation form. |
| FR-06 | Match within the same community and date using normalized pickup, exact/configured-nearby destination, configurable time window, open capacity, and exclude past/own/already-member rides. | Source present; matching unit tests exist but are unexecuted. |
| FR-07 | Allow a verified member to request a seat and the coordinator to accept/reject. | Source present; unverified. |
| FR-08 | Keep one coordinator on a ride; the creator starts as coordinator and confirmed passenger. | Source present; unverified. |
| FR-09 | Count only confirmed memberships for capacity and equal estimated fare splitting. | Source present; unverified. |
| FR-10 | Let coordinator mark an external taxi arrangement without booking it in RideMatch. | Source present; unverified. |
| FR-11 | Let members cancel their own participation and coordinators cancel the ride; retain cancellation records and notify affected users. | Source present; unverified. |
| FR-12 | On confirmed passenger cancellation, search verified community members and issue invitations requiring consent. | Partial: invitations are sequential but candidates are not matched to route intent or availability. |
| FR-13 | Use a configurable two-minute default timeout, notify the group, and recover open deadlines on startup. | Source present; scheduler/restart behavior unverified. |
| FR-14 | Assess a potential cancellation consequence from taxi-arranged state and timing; never charge a real penalty. | Partial: source flags a review window; actual-loss review is not available through a user/admin workflow. |
| FR-15 | Validate user inputs and render centralized error pages for application failures. | Source present; error/validation paths not integration-tested. |

## 6. Non-functional requirements

| ID | Requirement | Review note |
|---|---|---|
| NFR-01 Usability | Responsive, readable pages with reusable navigation, loading, empty, validation, and confirmation states. | Responsive CSS and templates are present; no browser layout verification or screenshots are available. |
| NFR-02 Security | Hash passwords, protect admin routes, and require community verification for ride operations. | Source configuration/guards are present; no authentication/authorization integration tests were run. |
| NFR-03 Privacy | Do not expose password hashes; limit ride detail access to approved community members. | DTOs omit hashes and group service checks membership; not runtime-tested. |
| NFR-04 Reliability | Use transactions and capacity locking to protect membership changes. | Pessimistic ride-row locks are present; no concurrent database test exists. |
| NFR-05 Maintainability | Keep controller, service, repository, domain, DTO, and view concerns separate. | Layered packages are present; see SDD/review findings. |
| NFR-06 Validation | Reject invalid or past ride date/time, malformed email, weak passwords, and invalid fare/capacity. | Source constraints/guards are present; only registration DTO validation currently has direct validator tests. |
| NFR-07 Compatibility | Support current mainstream desktop/mobile browsers. | Not browser-tested. |
| NFR-08 Testability | Test business rules independently and protect integration/concurrency behavior. | Unit tests exist; integration, persistence, and concurrency coverage remain missing. |

## 7. Use cases

### UC-01: Verify community membership

An authenticated user requests membership in a community. An administrator approves or rejects it. Only an approved member can proceed to community ride functions.

### UC-02: Create and find a ride

A verified member enters a future trip and estimated fare. The system stores the creator as coordinator and first confirmed passenger. A member searches within the same verified community and reviews ranked compatible rides. No match means the user can adjust the search or create a ride.

### UC-03: Request and confirm a seat

A verified member submits a request. The coordinator accepts or rejects it. Acceptance checks confirmed capacity while locking the ride row. A request does not occupy a seat until accepted.

### UC-04: Cancel and search for replacement

A confirmed non-coordinator passenger cancels. RideMatch records the cancellation and opens a persisted replacement search. It invites eligible verified members one at a time. Acceptance is explicit; rejection moves to the next candidate. The timeout closes the search and notifies the group.

## 8. Business rules

- Only approved members of the ride's community can view/manage the ride group or request a seat.
- The creator is the initial coordinator and a confirmed passenger; capacity includes that seat.
- Only `CONFIRMED` memberships count toward capacity and fare splitting.
- A user/ride pair is unique; the database prevents a second membership row for the same ride/user.
- Search uses configured 15-minute departure difference and destination aliases in the current properties; these are settings, not geospatial guarantees.
- Fare contribution is estimated total divided by confirmed passenger count, rounded to two decimal places. Rounding may leave a cent remainder across shares.
- The coordinator arranges the taxi outside RideMatch.
- The replacement timeout defaults to two minutes. No user is added without acceptance.
- Cancellation policy review is advisory and does not represent a charge or payment.

## 9. Acceptance criteria for remaining verification

Before declaring the MVP verified, run the automated test suite with JDK 25/Maven and verify: account registration and duplicate email, invalid login, verified-only access, successful ride creation, exact/nearby matching and exclusions, request approval/full/duplicate behavior, cancellation history, replacement acceptance/rejection/timeout, fare changes, admin authorization, database schema creation/migration, and simultaneous final-seat requests. Complete a browser review on desktop and mobile and capture real screenshots.

