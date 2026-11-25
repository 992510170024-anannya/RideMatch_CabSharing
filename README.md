<<<<<<< HEAD
# RideMatch

RideMatch is a Java 25, Spring Boot 3, Maven application for coordinating shared taxi journeys within verified college and company communities.

## Project documentation

- [RideMatch SRS](docs/RideMatch-SRS.md) — current requirements and source-status trace.
- [Software Design and UML](docs/RideMatch-Software-Design.md) — current source-oriented architecture and Mermaid diagrams.
- [User Manual](docs/RideMatch-User-Manual.md) — setup and user workflows.
- [QA Report](docs/QA-REPORT.md) — test inventory, execution result, review findings, and remaining risks.
- [Manual Test Cases](docs/Manual-Test-Cases.md) — admin, ride scheduling, notification scenarios, and feature LOC snapshot.

The older `docs/CabSharing-*` documents are historical proposal drafts and do not describe the current source status.

## Prerequisites

- JDK 25
- Maven 3.9 or later
- MySQL 8.0.16 or later (the DDL uses MySQL check constraints and `utf8mb4_0900_ai_ci`)
- Eclipse IDE with Maven integration (m2e)

## Create the database

Connect to MySQL as an administrator and run `database/create-database.sql`. Create an application database account with access limited to `ridematch_db`, following the commented example in that script. Keep the local password out of source control.

## Set local database environment variables

In PowerShell, set these in the terminal used to launch the app:

```powershell
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_DATABASE = "ridematch_db"
$env:MYSQL_USER = "ridematch_app"
$env:MYSQL_PASSWORD = "your-local-database-password"
```

`application.properties` reads credentials from the environment; no password is stored in the project. The password shown above is a placeholder only.

## Import into Eclipse

1. Install a JDK 25 and select it in Eclipse under **Window → Preferences → Java → Installed JREs**.
2. Choose **File → Import → Maven → Existing Maven Projects**.
3. Select the project folder containing `pom.xml`, ensure the project is checked, then choose **Finish**.
4. If dependency resolution does not start automatically, right-click the project and select **Maven → Update Project**.
5. Set the five environment variables above in the run configuration: **Run → Run Configurations → Java Application → Environment**. Select `com.ridematch.RideMatchApplication` as the main class.
6. Run the application. Register a standard account, then promote the first trusted account with `database/promote-admin.sql` to set up the administrator role.

## Run from a terminal

From the project directory, after setting the environment variables:

```powershell
mvn spring-boot:run
```

The initial development configuration uses Hibernate `ddl-auto=update` to create/update mapped tables. After a controlled schema migration workflow is introduced, change this to `validate` so Hibernate checks mappings without changing the schema.

## Initial persistence model

The database tables are mapped from the `domain.entity` classes. All entities inherit `created_at` and `updated_at` audit timestamps.

- `users`: account identity and password hash.
- `communities`: college/company community and verifier.
- `community_memberships`: one verification status per user/community pair.
- `rides`: community, creator, coordinator, route, time, fare estimate, passenger capacity, and lifecycle status.
- `ride_memberships`: one row per user/ride pair; only `CONFIRMED` rows occupy seats.
- `notifications`: user-facing in-app notifications, optionally attached to a ride.
- `cancellation_records`: participation cancellation history and optional actual financial loss record; no penalty collection is represented.
- `replacement_searches`: one deadline and outcome per cancelled confirmed seat.

The ride's `passenger_capacity` must be positive. Capacity enforcement counts only `CONFIRMED` ride memberships, under a transaction that locks the ride row. `RideRepository.findByIdForUpdate` serializes join approvals so simultaneous decisions cannot both consume the last seat. The ride row contains a single coordinator foreign key; ride creation creates the coordinator's confirmed membership in the same transaction. Foreign keys and unique constraints protect relationship integrity and prevent duplicate user/community and user/ride rows.

If you already created tables from the earlier persistence phase, run `database/group-management-migration.sql` once before starting the updated application. A fresh database can be created from the current entity mappings directly.

## Authentication and community administration

New registrations always receive the `USER` role. To create the first administrator, first register a trusted account, then review and run `database/promote-admin.sql` after replacing its placeholder email. Do not expose role assignment through public registration or profile forms. Administrator-only routes under `/admin/**` are protected by Spring Security and repeat role checks in the service layer. Passwords are BCrypt hashes; profile and community response DTOs never contain password fields.

Users request membership from the communities page. Only an administrator can approve or reject a pending request. The community service exposes `requireApprovedMember(email, communityId)` as a mandatory authorization precondition for future ride operations; ride controllers/services must call it before exposing or changing community ride data.

Run the project unit tests with `mvn test` after the Java/Maven prerequisites are installed.

**Current verification status:** `mvn test` has not completed in the review environment because Maven is unavailable and only Java 8 is installed. The required JDK is 25. See [docs/QA-REPORT.md](docs/QA-REPORT.md) for the exact test coverage and remaining verification tasks.

Group membership uses `REQUESTED`, `INVITED`, `CONFIRMED`, `REJECTED`, and `CANCELLED` states. Only `CONFIRMED` counts toward capacity or cost splitting. Accepting a request or invitation locks the ride row and checks confirmed capacity before committing. Replacement invitations are issued one at a time; a persisted search deadline is enforced by the in-process Spring task scheduler and recovered on application startup. The default timeout is two minutes (`ridematch.matching.replacement-search-timeout`).

Cancellation policy uses a configurable 24-hour potential-loss review window for rides marked `TAXI_ARRANGED`. The policy only flags a cancellation for review; it does not assess or collect a penalty. The fare share is recalculated from the estimated total and current confirmed passenger count, and actual fare remains external to RideMatch.

## Ride creation and matching

Verified members can create a ride from their community page or search for rides across their verified communities. The creator is recorded as both the ride creator and its initial coordinator, and a confirmed ride membership is created for that passenger. Capacity includes the creator; matching counts only confirmed memberships.

Matching is implemented in `RideMatchingService`. Pickup names must match after normalization. Destinations match exactly or when both normalized names appear in the same configured nearby-destination group. The default departure window is 15 minutes and is configurable with `ridematch.matching.maximum-departure-difference`. Alias groups can be changed in `application.properties`; no map service is used. Only `OPEN` and `COORDINATING` rides are candidates, and the service excludes full, cancelled, completed, different-community, different-date, and caller-owned rides.

Results score an exact destination at 70 points and a nearby-group destination at 45 points, then add up to 30 points for departure-time closeness. A ride at the exact requested time gets all 30 time points; a ride at the configured 15-minute boundary gets zero time points. Results sort by score descending, then time difference.

## Web interface

The Thymeleaf interface uses shared header, navigation, footer, and Bootstrap script fragments in `templates/fragments/layout.html`. A single responsive stylesheet and small form-interaction script live in `static/css/app.css` and `static/js/app.js`. The `/dashboard` page summarizes upcoming memberships, coordinator join requests, replacement invitations, and cancellation notifications. Ride matching remains explicit: users enter a route and time in the search form because RideMatch does not store route preferences. `/ride-requests` is the coordinator's pending request inbox and `/admin` is the administrator community overview. All displayed actions submit to the existing controller routes; no sample records are embedded in the pages.
=======
# RideMatch_CabSharing
>>>>>>> 3e000d9f5a54ba11dfecbe935d2dca3c9a7494a0
