# RideMatch — User Manual

This manual describes the current web pages and source-backed flows. The app has not been launched in the review environment, so labels and behavior should be confirmed after the first successful run.

## 1. Install and run

### Prerequisites

- JDK 25
- Maven 3.9 or later
- MySQL 8.0.16 or later
- Optional: Eclipse IDE with Maven integration (m2e)

The current review environment has Java 8 only and does not have Maven on PATH. These steps are for a machine with the prerequisites installed.

### Database and application configuration

1. Create the database by running `database/create-database.sql` in MySQL. It creates `ridematch_db`; it does not create the application tables.
2. Create a least-privilege MySQL application account with rights to create/alter the development schema, using the commented example in the script and a local secret.
3. Configure credentials in the shell used to start the application (PowerShell example):

   ```powershell
   $env:MYSQL_HOST = "localhost"
   $env:MYSQL_PORT = "3306"
   $env:MYSQL_DATABASE = "ridematch_db"
   $env:MYSQL_USER = "ridematch_app"
   $env:MYSQL_PASSWORD = "your-local-password"
   ```

4. If upgrading a database from the earlier membership schema, back it up and run `database/group-management-migration.sql` once. Do not run the non-idempotent `ALTER TABLE` statements more than once. A fresh database does not need that migration.
5. To run from a terminal, execute `mvn spring-boot:run` from the project directory. Hibernate currently uses `ddl-auto=update` for development schema creation/update.
6. To run tests, execute `mvn test`. A test report is in [QA-REPORT.md](QA-REPORT.md); no tests were executed in the reviewed environment.

### Eclipse import

1. Install/select a JDK 25 in **Window → Preferences → Java → Installed JREs**.
2. Choose **File → Import → Maven → Existing Maven Projects** and select the folder containing `pom.xml`.
3. Set the five MySQL environment variables in the Java run configuration for `com.ridematch.RideMatchApplication`.
4. Run the application. Review the output for Spring startup and Hibernate schema messages. Database schema creation has not been runtime-verified in this review.

## 2. First administrator setup

1. Register a normal account at `/auth/register` and sign in.
2. Connect to MySQL using an administrator account. Edit the placeholder email in `database/promote-admin.sql` to match the registered account and execute the script.
3. Sign out and sign back in so Spring Security reloads the ADMIN role.
4. Open `/admin` to register communities and review community membership requests.

Do not expose administrator role assignment through the public registration form.

## 3. Member flow

### Register and join a community

1. Register with full name, email, and a strong password.
2. Sign in and open **Communities**.
3. Submit a membership request for the appropriate college/company.
4. Wait for an administrator to approve the request. Ride features require approved community membership.

### Create a ride

1. From **Communities**, choose **Create a ride** for an approved community.
2. Enter pickup, destination, passenger capacity, and estimated total taxi fare. Capacity includes you as coordinator and first passenger.
3. Choose **Today**, **Tomorrow**, or a custom date. Choose **Now** or one of the listed checkpoint times (10:00, 10:50, 12:00, 12:50), or enter a custom start time.
4. If your departure time is flexible, turn on the time-range option and enter the latest departure time. Members searching within that window can match the ride.
5. Add optional notes and submit. The created ride page links to the group. Other active, approved members of that community receive an in-app notification; the creator is not notified.
4. Arrange the taxi outside RideMatch. The coordinator may record the external arrangement in the group page.

### Find a ride and request a seat

1. Open **Find a ride**.
2. Choose a verified community and enter pickup, destination, date, and desired departure time.
3. Review ranked compatible rides. Matching uses normalized pickup names, exact or configured nearby destination names, and a configurable time difference.
4. Open ride details or submit **Request to join**. This creates a pending request; it does not reserve a confirmed seat.
5. The coordinator's **Requests** inbox lists pending requests. The coordinator approves or declines. A successful approval confirms the seat and updates the estimated contribution.

### Review current rides, notifications, and profile

- **Dashboard** summarizes current upcoming memberships, rides you coordinate, pending join requests, replacement invitations, and cancellation notifications.
- **My rides** opens current upcoming groups and their membership status.
- **Notifications** contains new rides in your approved communities, join, cancellation, and replacement updates. Unread notifications can be marked read.
- **Profile** updates full name and email. Password changes are not available in the current interface.

### Cancel or accept a replacement invitation

- A passenger can cancel their own membership from the ride group. The coordinator cancels the whole ride for all participants.
- A confirmed passenger cancellation triggers a replacement search. RideMatch sends one invitation at a time to verified community members without an existing membership in the ride.
- The invitee must choose **Accept** or **Decline**. Acceptance confirms the seat; decline allows the search to proceed to another candidate.
- The search ends at the configured timeout (default two minutes). The scheduler and restart behavior are not runtime-verified.
- Cancellation policy messaging is informational. RideMatch does not charge a penalty or settle actual fare.

## 4. Administrator flow

1. Open **Admin** at `/admin`.
2. Choose **Add community** to register a college/company community.
3. Open **View requests** to approve or reject pending community membership requests.
4. Choose **View community** on a community card to see its approved members and pending-request count.
5. Administrators do not have a taxi booking or fare collection function.

See [Manual Test Cases](Manual-Test-Cases.md) for step-by-step checks of these flows.

## 5. Common errors

- **No approved community:** submit a membership request and wait for administrator approval.
- **No matching rides:** adjust route spelling/date/time or create a ride. RideMatch does not use map distance.
- **Ride full or request no longer valid:** a coordinator may have accepted another member first; review the group for current availability.
- **Access denied:** confirm that you signed in with the appropriate role and have approved membership in the ride's community.
- **Application/database error:** check application logs and database credentials. The application should not be treated as available until startup completes successfully.

