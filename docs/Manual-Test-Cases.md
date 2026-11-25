# RideMatch — Manual Test Cases

**Scope:** Admin community management, ride scheduling, flexible departure windows, and ride-created notifications.  
**Execution status:** Not run. Record actual results in the Result column during a manual test session.

## Test setup

- Start RideMatch against a disposable MySQL database using the project setup instructions.
- Confirm Hibernate has updated the rides table to include requested_time_end.
- Have one ADMIN account and three USER accounts available:
  - User A: approved member of Community North.
  - User B: approved member of Community North.
  - User C: pending member of Community North and approved member of Community South.
- Use future dates and times for ride creation and search.

## Cases

| ID | Scenario and steps | Expected result | Result |
|---|---|---|---|
| ADM-01 | Sign in as a USER and open /admin; then sign in as ADMIN and open /admin. | USER receives an access-denied response. ADMIN sees links to add a community, review requests, and view each community. | ☐ |
| ADM-02 | As ADMIN, choose Add community, submit a unique name and type, then return to Admin. | Community is saved and appears in the community list with a View community action. | ☐ |
| ADM-03 | As User C, request membership in a new community. As ADMIN, open the review queue and approve the request. Open that community's admin view. | Request appears as pending before approval. After approval, User C appears in the approved member list with name, email, and request date. | ☐ |
| ADM-04 | Submit another membership request and reject it from the admin review queue. Open the community view. | Rejected applicant is not shown among approved members. Review queue reports the decision completed. | ☐ |
| RIDE-01 | As User A, open Create a ride. Try Today, Tomorrow, and Choose date. | Today and Tomorrow populate the matching calendar date; Choose date focuses the date control and permits a custom date. | ☐ |
| RIDE-02 | Select Now, then each checkpoint: 10:00, 10:50, 12:00, and 12:50. | Start time is populated with the selected checkpoint. Now sets the browser's current time plus five minutes. | ☐ |
| RIDE-03 | Create one ride with a custom date/time and no range; create another with a start and a later latest departure time. | Both rides save. The first displays one departure time; the second displays its start and latest departure times. | ☐ |
| RIDE-04 | Turn on the flexible-time option, set the latest departure earlier than the start, and submit. Then enter a valid range and submit. | Earlier end is rejected with an inline validation message. A valid range is saved and displayed. | ☐ |
| RIDE-05 | Create a ride with a departure window. Search the same community, pickup, and destination for a time inside the window, then search outside the window by more than the configured maximum difference. | The first search can return the ride with the full time window shown. The second does not return that ride. | ☐ |
| RIDE-06 | Attempt to submit a ride with a past date/time, missing required fields, or a past end of range. | Invalid form is not saved; the page shows validation feedback. | ☐ |
| NOT-01 | As User A, create a ride in Community North. As User B, open Notifications. | User B receives a RIDE_CREATED notification naming the community, creator, route, and schedule; its ride link opens the ride group. | ☐ |
| NOT-02 | After the same ride is created, check User A, User C, and a member of Community South. | Creator User A, pending-only User C, and members of other communities do not receive that community ride notification. | ☐ |
| NOT-03 | Mark User B's ride notification as read and reload Notifications. | The notification remains in the list and no longer has the unread indicator. | ☐ |

## LOC snapshot

LOC below means physical lines in the feature-related files in the current checkout, including existing surrounding code. It is **not** an added-line diff count; the workspace does not provide a tracked pre-change baseline for calculating added LOC.

| Layer | Files counted | Current LOC |
|---|---:|---:|
| Java | 12 | 903 |
| Thymeleaf templates | 6 | 166 |
| JavaScript | 1 | 41 |
| **Total** | **19** | **1,110** |

The JavaScript file and admin community-detail template are new files (63 LOC combined). Other counts are the full current file lengths, not LOC added by this feature.
