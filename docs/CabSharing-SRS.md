# CabSharing — Software Requirements Specification

> Historical proposal draft. The current RideMatch requirements and implementation status are documented in [RideMatch-SRS.md](RideMatch-SRS.md).

**Version:** 1.0 (Initial proposal)  
**Status:** Requirements baseline; no features are implemented yet  
**Date:** 19 November 2026

## 1. Introduction and project objectives

### 1.1 Purpose

This SRS defines the initial requirements for CabSharing, a community-based web application for helping verified members of the same college or company coordinate shared taxi journeys. It is intended to guide implementation and provide a basis for review and acceptance.

### 1.2 Problem statement

Students, employees, and travelers may book separate taxis despite travelling from the same origin toward similar destinations at approximately the same time. This can increase individual travel costs and make it harder to find fellow travelers.

### 1.3 Objectives

- Help verified members discover and coordinate compatible journeys.
- Support nearby or overlapping destinations and configurable time differences.
- Show an estimated fare and an equal per-passenger share.
- Allow one passenger to coordinate the group and book a taxi externally in the MVP.
- Demonstrate maintainable, validated, layered application design in a college software engineering project.

## 2. Existing system and limitations

In the current problem context, travelers generally arrange taxi journeys individually or coordinate informally through conversations and messaging groups. This can make compatible travelers difficult to find, leave trip details scattered, and require manual fare calculations. This describes the problem context; it is not a claim that all communities use the same existing process.

## 3. Proposed system

CabSharing will provide a web interface where a member can create a ride request with an origin, destination, travel time, and estimated total fare. The system will search for compatible requests within the same verified college or company community, considering nearby destinations and a configurable time difference. Members can coordinate into a ride group. One passenger is the coordinator, who contacts a taxi provider and books outside CabSharing. The application displays an equal estimated fare share and supports cancellation with a replacement search.

Airport travelers enter their arrival time manually. The MVP does not retrieve flight information or book taxis.

## 4. Scope and limitations

### 4.1 MVP scope — planned requirements

- Member registration and college/company community verification.
- Ride request creation, viewing, matching, and coordination among verified community members.
- Configurable time difference and nearby-destination matching.
- One coordinator per ride group; coordinator-led external taxi booking.
- Estimated fare and equal split calculation.
- Cancellation and a replacement search with a configurable two-minute timeout.
- Manual arrival-time entry for airport journeys.
- Input validation and user-readable error handling.

### 4.2 MVP limitations

- CabSharing does not provide or dispatch taxis; the coordinator books externally.
- Fare amounts are estimates, not quotes or charges. The application does not collect fares, deposits, or real cancellation penalties.
- Replacement search cannot guarantee that another passenger will be found.
- Airport arrival times are entered by users; there is no live flight API.
- Matching quality depends on the accuracy and availability of user-entered trip information.

### 4.3 Future enhancements — not MVP commitments

- Optional flight-status integration.
- In-app payment or fare settlement, subject to separate requirements and safeguards.
- Taxi-provider booking integration.
- Notifications and additional matching refinements.

## 5. Stakeholders

| Stakeholder | Interest / responsibility |
|---|---|
| Students and employees | Find and coordinate compatible community journeys. |
| Airport travelers | Coordinate journeys using manually entered arrival times. |
| Ride coordinator | Coordinate the group and arrange the taxi externally. |
| Community verifier/administrator | Verify membership for a college or company community. |
| Project team | Analyze, implement, test, and maintain the application. |
| Taxi provider | Provides the separately booked taxi; is outside CabSharing's system boundary. |

## 6. Functional requirements

All requirements in this document are **planned**. None are represented as implemented.

| ID | Requirement |
|---|---|
| FR-01 | The system shall allow a person to register with the information needed to identify their college or company community. |
| FR-02 | The system shall allow an authorized community verifier to verify or reject a membership request. |
| FR-03 | The system shall prevent unverified users from joining a community ride. |
| FR-04 | The system shall allow a verified member to create a ride request containing an origin, destination, intended travel time, and estimated total fare. |
| FR-05 | For airport journeys, the system shall accept an arrival time entered by the user and shall not require live flight data. |
| FR-06 | The system shall find or display compatible requests from the same community using origin, destination proximity, and a configurable maximum time difference. |
| FR-07 | The system shall allow verified members to coordinate into a ride group for a compatible journey. |
| FR-08 | Each ride group shall have one passenger designated as coordinator. |
| FR-09 | The system shall display the estimated total fare and calculate an equal estimated share for each current passenger. |
| FR-10 | The coordinator shall be able to mark that the taxi is arranged externally; the system shall not book or dispatch a taxi. |
| FR-11 | The system shall allow a passenger to cancel participation in a ride group. |
| FR-12 | On cancellation, the system shall automatically search for a replacement compatible member for the affected group, subject to the configured replacement-search timeout. |
| FR-13 | The default replacement-search timeout shall be configurable and set to two minutes. |
| FR-14 | If no replacement is found before the timeout, the system shall stop the search and show the outcome. Any cancellation consequences shall be determined by the applicable cancellation policy and actual financial loss; CabSharing shall not collect a real penalty. |
| FR-15 | The system shall validate submitted data and provide an understandable outcome when an operation cannot be completed. |

## 7. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-01 Usability | Core tasks—registration, ride request creation, matching, coordination, and cancellation—shall be available through clear, responsive web pages usable on desktop and mobile screens. |
| NFR-02 Security | The system shall restrict ride participation to verified users of the relevant community and shall not expose passwords in pages, logs intended for users, or responses. |
| NFR-03 Privacy | The system shall request only information needed for membership verification and ride coordination, and show ride details only to relevant community members. |
| NFR-04 Reliability | The system shall preserve valid ride and group changes and report failures without silently claiming that a booking or replacement succeeded. |
| NFR-05 Maintainability | The application shall use a layered modular-monolith structure, with responsibilities separated across presentation, service, persistence, and domain/data layers. |
| NFR-06 Validation | User input shall be validated at the application boundary; invalid input shall not be persisted as a valid request. |
| NFR-07 Compatibility | The web interface shall support current mainstream browsers used for the project demonstration. |
| NFR-08 Testability | Business rules, including fare splitting, eligibility, and replacement timeout behavior, shall be structured so they can be tested independently. |

## 8. User roles and permissions

| Role | Permissions |
|---|---|
| Visitor | Register and submit membership details; cannot view or join community rides. |
| Unverified member | View verification status and provide required verification information; cannot join community rides. |
| Verified member | Create and view eligible community ride requests, join compatible ride groups, and cancel their own participation. |
| Coordinator | All verified-member permissions, plus coordinate the ride group and record the externally arranged taxi. The coordinator is one of the passengers. |
| Community verifier/administrator | Review membership requests and mark them verified or rejected. This role does not book taxis or collect penalties. |

## 9. Use-case descriptions

### UC-01: Verify a community member

**Primary actor:** Community verifier  
**Precondition:** A person has submitted membership details.  
**Main flow:** The verifier reviews the submitted details and marks the person verified. The system records the status and makes eligible community ride features available.  
**Alternative flow:** The verifier rejects the request; the person remains unable to join rides and can see that verification was not approved.  
**Postcondition:** The membership has a recorded verification decision.

### UC-02: Create and match a ride request

**Primary actor:** Verified member  
**Precondition:** The member is verified in a college or company community.  
**Main flow:** The member enters origin, destination, travel time, and estimated total fare. For an airport journey, the member enters the arrival time manually. The system validates the request and presents compatible requests from the same community using configured time and destination criteria.  
**Alternative flow:** Invalid or incomplete data is rejected with a correction message; no valid request is created. No compatible request is found; the system reports that result.  
**Postcondition:** A valid request is available for coordination, or the member is informed why it was not created/matched.

### UC-03: Coordinate and arrange a shared taxi

**Primary actor:** Verified member (coordinator)  
**Precondition:** Compatible verified members agree to coordinate.  
**Main flow:** The members form a ride group, one passenger is designated coordinator, and the system displays the equal estimated fare share. The coordinator arranges the taxi directly with an external provider and can record that arrangement in CabSharing.  
**Alternative flow:** If members do not coordinate, no group is formed. If the external taxi arrangement fails, the coordinator can communicate the changed plan to the group; CabSharing does not control the provider.  
**Postcondition:** The group has a coordinator and an estimated per-passenger share; taxi booking remains external.

### UC-04: Cancel and search for a replacement

**Primary actor:** Passenger in a ride group  
**Precondition:** The passenger belongs to an active ride group.  
**Main flow:** The passenger cancels. The system updates the group and automatically searches for a compatible replacement for up to the configured timeout (default two minutes). If a replacement is found, the group and estimated equal share are updated.  
**Alternative flow:** If none is found before timeout, the system ends the search and informs the group. Any cancellation consequence is governed by the applicable policy and actual financial loss; the application does not collect a real penalty.  
**Postcondition:** The cancellation and replacement-search outcome are visible to the affected group.

## 10. Business rules

- **BR-01:** Only verified college/company community members may join a community ride.
- **BR-02:** A ride group has exactly one passenger designated as coordinator at a time.
- **BR-03:** The coordinator arranges the taxi externally in the MVP.
- **BR-04:** Destination proximity and maximum time difference are matching settings, not hard-coded user-facing promises; their values are configurable.
- **BR-05:** The fare share is estimated total fare divided equally among current group passengers. Displayed rounding shall make the displayed shares add up to the displayed total as closely as the currency precision allows.
- **BR-06:** Cancellation starts an automatic replacement search. The default timeout is two minutes and is configurable.
- **BR-07:** If replacement search fails, any consequences depend on the applicable cancellation policy and actual financial loss. CabSharing does not assess or collect real penalties.
- **BR-08:** Airport arrival time is manually entered. Flight status is not inferred or fetched in the MVP.

## 11. Assumptions and constraints

### Assumptions

- Each community has a process or verifier able to decide whether a person belongs to it.
- Members provide sufficiently accurate origins, destinations, travel times, arrival times, and estimated fares.
- Members coordinate with one another and the external taxi provider outside any booking function in CabSharing.
- Cancellation policy details are supplied by the relevant community or group where applicable; the SRS does not define a monetary penalty schedule.

### Constraints

- The planned technology stack is Java 17, Spring Boot 3, Maven, MySQL, Spring Data JPA/Hibernate, Thymeleaf, HTML/CSS/JavaScript, Bootstrap, JUnit 5, and Mockito.
- The application is a layered modular monolith.
- The MVP does not include live flight APIs, in-app taxi booking, payments, or collection of penalties.
- Matching thresholds must be configurable; specific geographic/time values are not fixed by this SRS.

## 12. Functional acceptance criteria

These criteria apply when the corresponding planned MVP feature is implemented.

| ID | Acceptance criterion |
|---|---|
| AC-01 | An unverified account cannot join a ride group; a verified account in that ride's community can proceed. |
| AC-02 | A ride request with missing or invalid required fields is rejected, and the user is told which input needs correction. |
| AC-03 | With matching settings configured, a compatible request from the same community and within the allowed time difference is presented; a request outside the configured time difference is not presented as a match. |
| AC-04 | For a group of *n* passengers with a valid estimated fare, the displayed estimated share is an equal division across those *n* passengers, with rounding handled at currency precision. |
| AC-05 | A ride group has one passenger coordinator, and the coordinator can record an external taxi arrangement without creating an in-app taxi booking. |
| AC-06 | Cancelling a passenger starts replacement search automatically and ends it when a replacement is found or the configured timeout elapses; the default timeout is two minutes. |
| AC-07 | If no replacement is found, the group sees that the search ended without replacement, and no real penalty is charged or collected by CabSharing. |
| AC-08 | An airport request can be created using a manually entered arrival time without calling a flight API. |

## 13. Error and exception scenarios

| Scenario | Expected system behavior |
|---|---|
| Membership is unverified or rejected | Deny ride participation and explain that verification is required. |
| Required field is missing or malformed | Reject submission, identify the field, and retain safely reusable form data where practical. |
| Fare is zero, negative, or not a valid amount | Reject the request and explain that a valid estimated fare is required. |
| No compatible ride is available | Show that no match is currently available; do not imply a ride was arranged. |
| User attempts to join a different community's ride | Deny the operation and leave the ride unchanged. |
| Two users attempt conflicting changes to group membership | Accept only a valid resulting group state and inform the user if their change could not be applied. |
| Replacement search reaches timeout without a match | Stop the search, show that no replacement was found, and apply no real charge or penalty. |
| Database or application service is unavailable | Show a general retry/error message; do not report an unconfirmed update as successful. |
| External taxi cannot be arranged | Allow the coordinator to communicate/update the group plan; do not claim CabSharing can resolve provider availability. |

## 14. MVP and future enhancements summary

| Capability | Classification |
|---|---|
| Community membership verification and verified-only ride participation | MVP — planned |
| Ride request creation and configurable community matching | MVP — planned |
| One passenger coordinator and external taxi booking | MVP — planned |
| Estimated equal fare split | MVP — planned |
| Cancellation and configurable replacement search (two-minute default) | MVP — planned |
| Manual airport arrival-time entry | MVP — planned |
| Live flight information, in-app booking, and payment | Future enhancement; excluded from MVP |
| Real penalty assessment or collection | Excluded; not part of MVP or a stated future enhancement |

**Implementation status:** This document defines requirements only. No application code or feature implementation is included in this phase.
