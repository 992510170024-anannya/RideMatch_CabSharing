# RideMatch — Software Design Document and UML

**Version:** 1.0 · **Status:** As-built source review · **Runtime verification:** Not completed

This document describes the current source tree. It supersedes the earlier CabSharing proposal where names or classes differ. Diagrams are Mermaid source and should be rendered by a Mermaid-capable Markdown viewer.

## 1. Architecture

RideMatch is a Spring Boot modular monolith. A Thymeleaf/browser request enters a controller, invokes a business service, accesses repositories, and persists through JPA/Hibernate to MySQL. Scheduled replacement deadlines run in-process. Taxi booking remains external.

```mermaid
flowchart LR
  Browser[Desktop or mobile browser]
  Views[Thymeleaf views and fragments]
  Controllers[Spring MVC controllers]
  Services[Business services and domain rules]
  Repositories[Spring Data JPA repositories]
  Entities[JPA entities]
  DB[(MySQL ridematch_db)]
  Scheduler[In-process TaskScheduler]
  Browser --> Views --> Controllers --> Services --> Repositories --> Entities --> DB
  Services --> Scheduler
  Scheduler --> Services
```

## 2. Use-case diagram

```mermaid
flowchart LR
  Visitor((Visitor))
  Member((Authenticated member))
  Coordinator((Ride coordinator))
  Admin((Administrator))
  UC1[Register and sign in]
  UC2[Request community membership]
  UC3[Create or search ride]
  UC4[Request to join]
  UC5[Review ride requests]
  UC6[Cancel membership or ride]
  UC7[Accept or decline replacement invitation]
  UC8[View group and estimated fare]
  UC9[Review community membership]
  UC10[Register community]
  Visitor --> UC1
  Member --> UC2
  Member --> UC3
  Member --> UC4
  Member --> UC6
  Member --> UC7
  Member --> UC8
  Coordinator --> UC5
  Coordinator --> UC6
  Coordinator --> UC8
  Admin --> UC9
  Admin --> UC10
```

## 3. Class diagram

```mermaid
classDiagram
  class User { -Long id; -String fullName; -String email; -String passwordHash; -UserRole role; -boolean active }
  class Community { -Long id; -String name; -CommunityType type; +assignVerifier() }
  class CommunityMembership { -VerificationStatus verificationStatus; +verify(); +reject() }
  class Ride { -String origin; -String destination; -LocalDateTime requestedTime; -BigDecimal estimatedTotalFare; -int passengerCapacity; -RideStatus status; +markTaxiArranged(); +cancel(); +complete() }
  class RideMembership { -RideMembershipStatus status; -LocalDateTime joinedAt; +confirm(); +cancel(); +reject() }
  class ReplacementSearch { -ReplacementSearchStatus status; -LocalDateTime deadline; +markReplaced(); +markFilled(); +timeOut(); +cancel() }
  class CancellationRecord { -CancellationPolicyDecision policyDecision; -BigDecimal actualFinancialLoss }
  class Notification { -NotificationType type; -String title; -String message; +markRead() }
  class RideController
  class RideGroupController
  class RideService
  class RideMatchingService
  class RideGroupService
  class ReplacementSearchService
  class FareSplitService
  class CancellationPolicyService
  class RideRepository
  class RideMembershipRepository
  User "1" --> "*" CommunityMembership
  Community "1" --> "*" CommunityMembership
  Community "1" --> "*" Ride
  User "1" --> "*" Ride : creator/coordinator
  Ride "1" --> "*" RideMembership
  User "1" --> "*" RideMembership
  RideMembership "1" --> "0..*" CancellationRecord
  Ride "1" --> "*" ReplacementSearch
  RideMembership "1" --> "0..1" ReplacementSearch : cancelled seat
  ReplacementSearch "0..1" --> "0..1" RideMembership : filled by
  User "1" --> "*" Notification
  RideController --> RideService
  RideService --> RideMatchingService
  RideGroupController --> RideGroupService
  RideGroupService --> ReplacementSearchService
  RideGroupService --> FareSplitService
  RideGroupService --> CancellationPolicyService
  RideService --> RideRepository
  RideGroupService --> RideMembershipRepository
```

Repositories are Spring Data interfaces; there are no custom repository abstraction layers beyond those interfaces. Services use constructor injection. Entities keep fields private and expose controlled state methods. Inheritance is limited to `AuditableEntity`, a shared persistence timestamp base class.

## 4. Entity relationship diagram

```mermaid
erDiagram
  USERS ||--o{ COMMUNITY_MEMBERSHIPS : requests
  COMMUNITIES ||--o{ COMMUNITY_MEMBERSHIPS : verifies
  USERS ||--o{ RIDES : creates
  USERS ||--o{ RIDES : coordinates
  COMMUNITIES ||--o{ RIDES : contains
  RIDES ||--o{ RIDE_MEMBERSHIPS : includes
  USERS ||--o{ RIDE_MEMBERSHIPS : participates
  RIDE_MEMBERSHIPS ||--o{ CANCELLATION_RECORDS : history
  RIDES ||--o{ REPLACEMENT_SEARCHES : searches
  RIDE_MEMBERSHIPS ||--o| REPLACEMENT_SEARCHES : cancelled_membership
  RIDE_MEMBERSHIPS o|--o{ REPLACEMENT_SEARCHES : filled_membership
  USERS ||--o{ NOTIFICATIONS : receives
  RIDES o|--o{ NOTIFICATIONS : context
  USERS { bigint id PK; varchar email UK; varchar password_hash; varchar user_role; boolean active }
  COMMUNITIES { bigint id PK; varchar name; varchar community_type; bigint verifier_user_id FK }
  COMMUNITY_MEMBERSHIPS { bigint id PK; bigint user_id FK; bigint community_id FK; varchar verification_status; bigint verified_by_user_id FK }
  RIDES { bigint id PK; bigint community_id FK; bigint creator_user_id FK; bigint coordinator_user_id FK; varchar origin; varchar destination; datetime requested_time; decimal estimated_total_fare; int passenger_capacity; varchar status }
  RIDE_MEMBERSHIPS { bigint id PK; bigint ride_id FK; bigint user_id FK; bigint replacement_search_id FK; varchar membership_status; datetime joined_at }
  REPLACEMENT_SEARCHES { bigint id PK; bigint ride_id FK; bigint cancelled_membership_id FK_UK; bigint filled_by_membership_id FK; varchar search_status; datetime deadline }
  CANCELLATION_RECORDS { bigint id PK; bigint ride_membership_id FK; varchar outcome; varchar policy_decision; decimal actual_financial_loss }
  NOTIFICATIONS { bigint id PK; bigint user_id FK; bigint ride_id FK; varchar notification_type; datetime read_at }
```

The schema has unique user/email, community membership user/community, and ride membership ride/user constraints. Replacement search has a unique cancelled-membership reference. Only confirmed membership rows count as seats.

## 5. Sequence — create ride

```mermaid
sequenceDiagram
  actor Member
  participant C as RideController
  participant S as RideService
  participant CS as CommunityService
  participant RR as RideRepository
  participant MR as RideMembershipRepository
  Member->>C: Submit validated ride form
  C->>S: createRide(email, communityId, request)
  S->>CS: requireApprovedMember
  S->>S: Check future departure
  S->>RR: Save ride (creator is coordinator)
  S->>MR: Save creator's CONFIRMED membership
  S-->>C: RideViewResponse
  C-->>Member: Created page with estimated seats/fare
```

## 6. Sequence — request and accept a seat

```mermaid
sequenceDiagram
  actor Member
  actor Coordinator
  participant C as RideGroupController
  participant S as RideGroupService
  participant RR as RideRepository
  participant MR as RideMembershipRepository
  Member->>C: POST join request
  C->>S: requestToJoin
  S->>RR: Lock ride row
  S->>S: Check community, ownership, state, capacity, duplicate
  S->>MR: Save REQUESTED membership
  S-->>Coordinator: In-app join-request notification
  Coordinator->>C: Approve request
  C->>S: acceptJoinRequest
  S->>RR: Lock same ride row
  S->>MR: Count CONFIRMED seats
  S->>MR: Change REQUESTED to CONFIRMED
  S-->>Member: Confirmation and fare update
```

## 7. Sequence — cancellation and replacement

```mermaid
sequenceDiagram
  actor Passenger
  participant C as RideGroupController
  participant GS as RideGroupService
  participant RR as RideRepository
  participant MR as RideMembershipRepository
  participant RS as ReplacementSearchService
  participant DB as MySQL
  participant T as TaskScheduler
  participant Invitee as Verified candidate
  Passenger->>C: Cancel confirmed membership
  C->>GS: cancelOwnMembership
  GS->>RR: Lock ride row
  GS->>MR: Set membership CANCELLED; save cancellation record
  GS->>RS: Start replacement search
  RS->>DB: Persist search/deadline
  RS->>T: Schedule timeout
  RS->>DB: Persist one INVITED membership
  RS-->>Invitee: Replacement invitation
  alt Invitee accepts
    Invitee->>GS: Accept invitation
    GS->>RR: Lock ride
    GS->>MR: Recheck capacity and confirm membership
    GS->>DB: Mark search REPLACED; update estimated split
  else Invitee rejects or deadline expires
    GS->>RS: Invite next candidate or close search
    RS-->>Passenger: Group notification on completion/timeout
  end
```

## 8. Activity — ride lifecycle

```mermaid
flowchart TD
  A([Start]) --> B[Register and sign in]
  B --> C[Request community membership]
  C --> D{Approved?}
  D -- No --> C
  D -- Yes --> E[Create ride or search]
  E --> F{Compatible open ride?}
  F -- No --> E
  F -- Yes --> G[Request seat]
  G --> H{Coordinator accepts and seat remains?}
  H -- No --> E
  H -- Yes --> I[Confirm membership and refresh estimate]
  I --> J[Coordinator books externally]
  J --> K{Passenger cancels?}
  K -- No --> L([Ride proceeds externally])
  K -- Yes --> M[Record cancellation and open timed search]
  M --> N{Candidate accepts before timeout?}
  N -- Yes --> I
  N -- No --> O[Close search and notify group]
  O --> L
```

## 9. State transitions

```mermaid
stateDiagram-v2
  [*] --> OPEN
  OPEN --> COORDINATING: first accepted passenger request
  OPEN --> TAXI_ARRANGED: coordinator records external booking
  COORDINATING --> TAXI_ARRANGED: coordinator records external booking
  OPEN --> CANCELLED: coordinator cancellation
  COORDINATING --> CANCELLED: coordinator cancellation
  TAXI_ARRANGED --> CANCELLED: coordinator cancellation
  OPEN --> COMPLETED: ride completed
  COORDINATING --> COMPLETED: ride completed
  TAXI_ARRANGED --> COMPLETED: ride completed
```

```mermaid
stateDiagram-v2
  [*] --> REQUESTED
  [*] --> CONFIRMED: ride creator membership
  REQUESTED --> CONFIRMED: coordinator approves
  REQUESTED --> REJECTED: coordinator declines
  REQUESTED --> CANCELLED: member withdraws or ride cancelled
  [*] --> INVITED: replacement invitation
  INVITED --> CONFIRMED: invitee accepts
  INVITED --> REJECTED: invitee declines
  INVITED --> CANCELLED: timeout, ride closure, or capacity reached
  CONFIRMED --> CANCELLED: member or coordinator cancels
```

## 10. Component diagram

```mermaid
flowchart TB
  Browser[Browser]
  subgraph Web[Presentation]
    Thymeleaf[Thymeleaf pages and fragments]
    Auth[AuthController]
    Community[CommunityController]
    Ride[RideController]
    Group[RideGroupController]
    Dashboard[DashboardController]
    Notice[NotificationController]
    User[UserController]
  end
  subgraph Domain[Business and domain]
    AuthS[AuthService]
    CommunityS[CommunityService]
    RideS[RideService]
    MatchS[RideMatchingService]
    GroupS[RideGroupService]
    ReplaceS[ReplacementSearchService and timeout handler]
    FareS[FareSplitService]
    PolicyS[CancellationPolicyService]
  end
  Repos[Spring Data repositories]
  JPA[JPA entities / Hibernate]
  MySQL[(MySQL)]
  Browser --> Thymeleaf
  Thymeleaf --> Auth & Community & Ride & Group & Dashboard & Notice & User
  Auth --> AuthS
  Community --> CommunityS
  Ride --> RideS
  Group --> GroupS
  Dashboard --> CommunityS & GroupS
  Notice --> GroupS
  User --> AuthS
  RideS --> MatchS
  GroupS --> ReplaceS & FareS & PolicyS
  AuthS & CommunityS & RideS & MatchS & GroupS & ReplaceS & FareS & PolicyS --> Repos
  Repos --> JPA --> MySQL
```

## 11. OOP, SOLID, and review notes

- **Encapsulation:** `Ride`, `RideMembership`, `ReplacementSearch`, and `CommunityMembership` keep fields private and provide state-changing methods.
- **Abstraction / DIP:** services depend on repository interfaces and Spring's `PasswordEncoder`/`TaskScheduler` contracts. No extra application interface is needed for each service.
- **Polymorphism:** no substantial application-level polymorphic behavior is currently required; Spring interface implementations supply repository/encoder/scheduler implementations.
- **Inheritance:** `AuditableEntity` centralizes timestamp behavior for persisted entities. No user/role inheritance hierarchy exists.
- **SRP:** controllers bind requests; services orchestrate business rules; repositories expose persistence operations; DTOs shape forms/views.
- **OCP:** matching thresholds and alias groups are configuration properties. This is a bounded extension point; the service otherwise contains the current rule-based strategy.
- **LSP / ISP:** no custom inheritance/interface hierarchy is introduced to claim these principles. Repository interfaces remain focused on persistence.

## 12. Transactions and known design gaps

Mutating group operations are transactional. `RideRepository.findByIdForUpdate` locks the ride row before seat counts and membership decisions. This is source-reviewed but not tested under concurrent database transactions. Replacement deadline callbacks lock the ride before the search; early callbacks reschedule themselves. Search candidate selection remains sequential and broad, and MySQL schema migration/restart behavior requires runtime verification.

