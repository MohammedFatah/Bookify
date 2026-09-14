# Bookify — Project Handoff

A BookMyShow-style booking backend, built as a learning project. Goal: learn concurrency, distributed locking, DB transactions, race conditions, TTL, Redis, Kafka, and system design — by building, not by copying a finished solution.

**Working style the person expects**: small tasks, hints before answers, concepts explained before/alongside code, code reviewed rather than rewritten for them. They want to write the code themselves and be told what's wrong and why, not handed corrected files. Preserve this style when continuing.

## 1. Tech stack

- Java 21, Spring Boot 4.1.1 (Spring Framework 7)
- Maven (group `com.bookify`, artifact `bookify`, base package `com.bookify` — no stutter)
- PostgreSQL 16 (via Docker)
- Redis 7 (via Docker) — for temporary seat locks with TTL
- Kafka (KRaft mode, via Docker) — for booking-confirmed/cancelled events → payment/notification consumers
- Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities and DTOs)
- Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Valid`)
- `application.yaml` (not `.properties`) for config

## 2. Repo structure

```
parent-folder/                  (repo root)
├── docker-compose.yml          (deliberately kept at root, not inside the app — infra is orchestration-level;
│                                 future payment-service/notification-service modules will be siblings of bookify/)
└── bookify/                    (Spring Boot app root)
    ├── src/main/java/com/bookify/
    │   ├── entity/
    │   ├── enums/
    │   ├── repository/
    │   ├── dto/
    │   ├── service/
    │   ├── controller/
    │   └── exception/
    └── src/main/resources/application.yaml
```

## 3. docker-compose.yml (current state)

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:16
    container_name: bookify-postgres
    environment:
      POSTGRES_USER: bookify_user
      POSTGRES_PASSWORD: bookify_pass
      POSTGRES_DB: bookify
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7
    container_name: bookify-redis
    ports:
      - "6379:6379"

  kafka:
    image: apache/kafka:3.7.0
    container_name: bookify-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT

volumes:
  pgdata:
```

**Known gotcha already hit**: Postgres only runs its init script (creating user/db/password from env vars) on a truly empty volume. If you change `POSTGRES_*` env vars later, you must `docker compose down -v` (the `-v` deletes the volume) before `up -d` again, or the old credentials silently persist.

## 4. application.yaml (current state)

```yaml
spring:
  application:
    name: bookify
  datasource:
    url: jdbc:postgresql://localhost:5432/bookify?options=-c%20TimeZone=Asia/Kolkata
    username: bookify_user
    password: bookify_pass
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

**Timezone gotcha already hit and fixed**: the JVM's default timezone resolves to the legacy `Asia/Calcutta` alias on Indian machines, which Postgres rejects (`invalid value for parameter "TimeZone"`). The JDBC URL `options=` parameter alone did **not** reliably fix it (the driver's handshake re-sends the JVM default and clobbers it). What actually worked was a `-Duser.timezone=Asia/Kolkata` JVM flag. **Still outstanding**: make this permanent and portable by adding to the main class instead of relying on an ad-hoc VM flag:
```java
public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    SpringApplication.run(BookifyApplication.class, args);
}
```
This has been explained to the person but not yet confirmed applied — worth checking/doing early.

`ddl-auto: update` is intentionally used for learning speed; the person knows this is unsafe for production and that Flyway/Liquibase is the real answer (mentioned, not yet needed).

## 5. Schema — full design (all 9 tables), with the *why* behind each

Reasoned from scratch with the person (no schema handed to them) — they made every decision themselves after Socratic questioning. Key lessons already internalized by the person, don't re-teach unless they ask:
- Surrogate UUID PKs everywhere (chosen deliberately for a distributed system — Kafka events, no central ID coordination needed)
- FK always lives on the "many" side of a one-to-many
- NULL never equals NULL, so naive `UNIQUE(a,b,c)` with a nullable column doesn't prevent duplicate-NULL rows — this bit them on an early `ShowSeat` design attempt
- Never use float/double for money → `DECIMAL(10,2)` / `BigDecimal`
- Never store phone numbers as numeric types (leading zeros, `+` prefix, no arithmetic ever needed) → `VARCHAR`
- Enums: Java `enum` + `@Enumerated(EnumType.STRING)`, ideally paired with a DB `CHECK`/native enum (not yet added at DB level, only Java-side so far)
- Redis owns *temporary* truth (an active lock, with TTL, auto-expiring — no cleanup code needed), Postgres owns *permanent* truth. `ShowSeat.status` in Postgres only ever has two values: `AVAILABLE` / `BOOKED`. `LOCKED` is a Redis-only concept and must never be written to Postgres (avoids needing a background job to reconcile expired locks).
- A `Seat` is the permanent physical seat (tied to `Screen` forever); a `ShowSeat` is "this seat, for this specific show" — the row that actually gets locked/booked. This distinction was the hardest concept and took several passes to land correctly.

### Tables

**Venue**: `id UUID PK`, `name VARCHAR(50) NOT NULL`, `city VARCHAR(50) NOT NULL`, `created_at`, `updated_at`.

**Screen**: `id UUID PK`, `name VARCHAR(50) NOT NULL`, `type VARCHAR NOT NULL` (enum `ScreenType`), `capacity SMALLINT NOT NULL` — **derived/cached value, see §7**, `venue_id UUID FK NOT NULL`, `created_at`, `updated_at`.

**Seat**: `id UUID PK`, `row_id VARCHAR(1) NOT NULL` (named `row_id` specifically to dodge the `ROW` reserved SQL keyword), `number SMALLINT NOT NULL`, `screen_id UUID FK NOT NULL`, `seat_category VARCHAR NOT NULL` (enum `SeatCategory`: SILVER/GOLD/PLATINUM), `created_at`, `updated_at`. `UNIQUE(screen_id, row_id, number)`.

**Movie**: `id UUID PK`, `title VARCHAR(100) NOT NULL`, `duration_minutes SMALLINT NOT NULL`, `created_at`, `updated_at`.

**Show**: `id UUID PK`, `show_time TIMESTAMP NOT NULL`, `screen_id UUID FK NOT NULL`, `movie_id UUID FK NOT NULL`, `created_at`, `updated_at`. (No `name`/`price` — both correctly identified as derivable/misplaced and removed during design.)

**ShowCategoryPrice**: `id UUID PK`, `show_id UUID FK NOT NULL`, `category VARCHAR NOT NULL` (reuses `SeatCategory` enum — no separate enum needed), `price DECIMAL(10,2) NOT NULL`, `created_at`, `updated_at`. `UNIQUE(show_id, category)`. **Not yet built in code** — see §8 open item.

**ShowSeat** (the core entity the whole locking design revolves around): `id UUID PK`, `show_id UUID FK NOT NULL`, `seat_id UUID FK NOT NULL`, `booking_id UUID FK NULLABLE` (nullable because an available seat has no booking yet — no `nullable=false` on this one `@JoinColumn`, unlike every other FK), `status VARCHAR NOT NULL` (enum `SeatStatus`: **only** `AVAILABLE` / `BOOKED` — no `LOCKED`, that lives in Redis only), `created_at`, `updated_at`. `UNIQUE(show_id, seat_id)` (table name must be `show_seats`, not `show-seats` — hyphens in SQL identifiers are a landmine, already caught and fixed).

**User**: `id UUID PK`, `name VARCHAR(50) NOT NULL`, `phone_number VARCHAR(15) NOT NULL UNIQUE`, `email VARCHAR(50) NULLABLE UNIQUE` (nullable because email is optional/opt-in for notifications, but unique among those who provide one — relies on the NULL≠NULL behavior working *in their favor* here), `created_at`, `updated_at`.

**Booking**: `id UUID PK`, `user_id UUID FK NOT NULL`, `show_id UUID FK NOT NULL`, `status VARCHAR NOT NULL` (enum `BookingStatus`: `PENDING` / `CONFIRMED` / `CANCELLED` / `FAILED` — `PENDING` = checkout started but not yet paid; `FAILED` = never completed, no money moved; `CANCELLED` = was confirmed, then actively undone, possibly needs refund logic), `total_amount DECIMAL(10,2) NOT NULL` (a **snapshot** of what was paid at booking time — deliberately not looked up live via join, so historical bookings don't change if prices update later), `created_at`, `updated_at`.

No direct `Booking↔Movie`, `Booking↔Screen`, or `Movie↔Screen` links — all correctly identified as redundant/derivable through `Show` and removed during design.

## 6. Architectural conventions established (apply consistently to new code)

- **Layering**: Controller (HTTP only) → Service (business logic, FK lookups, transactions) → Repository (DB only). Controllers never inject repositories directly.
- **No service interfaces** for plain CRUD services (Venue/Movie/Screen/Seat/Show) — concrete `@Service` class only, constructor injection, no `@Autowired` needed. Interfaces are reserved for genuinely swappable implementations (e.g. a future `PaymentGateway` with Stripe/Razorpay implementations).
- **Request DTOs reference related entities by ID** (e.g. `ScreenRequest.venueId: UUID`), never by nesting the full related object. Service resolves the ID to the actual entity via its repository, throwing `ResourceNotFoundException` if missing.
- **Response DTOs never expose raw entities.** Nested relationships use small `*Summary` DTOs (e.g. `VenueSummary { id, name }`, `ScreenSummary { id, name, type }`, `MovieSummary { id, title, durationMinutes }`) with only what the caller of *that* endpoint needs — not full entities, not full audit trails on nested objects. Top-level/detail responses (e.g. `VenueResponse`, `ScreenResponse`) do include `createdAt`/`updatedAt`; nested summaries don't.
- **Validation**: Bean Validation annotations on Request DTOs (`@NotNull` for non-string/object fields — `@NotBlank` only works on `CharSequence`, this mistake has been made and caught twice already, watch for it recurring), `@Valid @RequestBody` on controller methods.
- **Exceptions**: two generic, reusable exception classes (not one per entity):
  - `ResourceNotFoundException(String resourceName, Object id)` — builds its own message (`"{resourceName} not found with id: {id}"`), used via `.orElseThrow(() -> new ResourceNotFoundException("Venue", venueId))`
  - `ResourceAlreadyExistsException(String message)` — for pre-insert duplicate checks (chosen over relying on catching Spring's `DataIntegrityViolationException` after the fact)
  - Handled centrally in `GlobalExceptionHandler` (`@ControllerAdvice`), each mapped to the correct HTTP status (404, 409 respectively) and returned as a shared `ErrorResponse` DTO: `{ timestamp, status, error, message }`.
  - `Venue` and `Movie` services still use an older ad-hoc string-message exception from before this pattern was established — **noted as a cleanup task, not yet done**: retrofit them to use `ResourceNotFoundException` for consistency.
- **Transactions**: `@Transactional` (prefer `org.springframework.transaction.annotation.Transactional` over the plain `jakarta.transaction.Transactional` used once so far — Spring's version supports `rollbackFor`, `readOnly`, etc., which will matter soon) on any service method that performs more than one related write that must succeed/fail together. Already applied in `SeatService.addSeat` (seat insert + capacity resync) and needs to be added to `ShowService.addShow` (show insert + N `ShowSeat` inserts) — see bug list below.
- **Custom repository queries** via Spring Data method-name derivation (no SQL/implementation needed): `existsByScreenIdAndRowIdAndNumber(...)`, `findByScreenId(...)`, `countByScreenId(...)` already in use.
- **Naming**: Java camelCase always (Hibernate auto-translates to snake_case columns, no manual `@Column(name=...)` needed for this). `com.bookify.enums` package for plain enums (no JPA annotations on the enum file itself — this mistake was made once, fixed).

## 7. `Screen.capacity` — denormalization decision, resolved

Chose **Option 2: `capacity` is a derived/cached count**, not an enforced cap. Rule: any time the set of seats for a screen changes, recompute via `seatRepository.countByScreenId(screenId)` and overwrite `screen.capacity`, inside the same `@Transactional` boundary as the seat write. Implemented for seat *creation* (`SeatService.addSeat`) — already correctly wired, ordering is save-seat-then-recount (safer than manual +1 arithmetic). **Not yet implemented for seat deletion** (no delete-seat endpoint exists yet) — remember to apply the same resync rule whenever that's built.

## 8. Current implementation status

### Fully done (entity → repository → request/response DTOs incl. Summary DTOs → service → controller, tested via Postman)
- **Venue** — `POST /venues`, `GET /venues/{id}`
- **Movie** — `POST /movies`, `GET /movies/{id}`
- **Screen** — `POST /screens`, `GET /screens/{id}` (FK lookup to Venue, `VenueSummary` in response)
- **Seat** — `POST /seats`, `GET /seats/{id}` (FK lookup to Screen, duplicate check via `existsByScreenIdAndRowIdAndNumber` → `ResourceAlreadyExistsException`, capacity resync on create, `ScreenSummary` in response)

### Entities + repositories written, full stack not yet built
- **User**, **Booking** — entity classes and repositories exist; no DTOs/service/controller yet.

### In progress / has known bugs not yet confirmed fixed
- **Show** (`POST /shows`, `GET /shows/{id}`) — controller and service written, orchestrates automatic `ShowSeat` generation via `ShowSeatService`. **Outstanding bugs identified but not yet confirmed fixed by the person:**
  1. `ShowService.addShow` calls `screenRepository.findAllByScreenId(screenId)` — wrong repository; should be `seatRepository.findByScreenId(screenId)`.
  2. Typo: `new ResourceNotFoundException("Sreen", screenId)` → should be `"Screen"`.
  3. `ShowSeatService` has unused injected `ShowRepository` and `SeatRepository` fields/params — dead weight, remove (the `Show` and `List<Seat>` are already passed in directly).
  4. `ShowSeatService.addShowSeats` loops and calls `.save()` once per seat — should use `.stream().map(...).toList()` + a single `showSeatRepository.saveAll(...)` call for efficiency.
  5. `ShowService.addShow` is **not yet `@Transactional`** — critical gap: if `ShowSeat` generation fails partway through, the `Show` row + partial `ShowSeat` rows are left in an inconsistent state with no rollback. Must add `@Transactional` to `ShowService.addShow`.

### Explicitly designed but not yet built
- **ShowCategoryPrice** — full stack (entity done, everything else pending). **Open design question, not yet resolved with the person**: when a `Show` is created, does the client supply per-category prices in the same request (e.g. `{"SILVER": 150, "GOLD": 250, "PLATINUM": 400}`), or does the system apply some default/template pricing? Needs to be decided before writing `ShowCategoryPriceRequest`/service. Lean toward client-supplied at show-creation time (realistic — pricing varies per show/time slot) but this was flagged to the person as a real decision point, not resolved yet.
- **Bulk seat generation endpoint** — `POST /screens/{screenId}/seats/generate`, taking `seatsPerRow` (int), computing `numberOfRows = ceil(capacity / seatsPerRow)`, labeling rows `A, B, C...` via `(char)('A' + i)`, handling the remainder in the last row, and looping through the **existing** `SeatService.addSeat(...)` per generated seat (so duplicate-check + capacity-resync logic is automatically reused, not reimplemented). The person was mid-way through designing this (the row-letter/remainder math was left as their own exercise) when the handoff request came in — **this is the very next task in progress.**
- **User**, **Booking** full CRUD stacks (DTOs, service, controller) — Booking will need the FK-by-id pattern for both `userId` and `showId`.

## 9. Prioritized task list — pick up here, in order

1. Fix the 5 identified bugs in `ShowService`/`ShowSeatService` (listed in §8) — confirm/apply, don't just re-explain.
2. Finish the bulk seat-generation endpoint (`POST /screens/{screenId}/seats/generate`) — reuse `SeatService.addSeat` per seat; person was actively working out the row-letter + remainder math themselves, let them finish it with a hint-first approach if they're still working on it.
3. Resolve the `ShowCategoryPrice` design question (client-supplied vs default pricing per category at show-creation time), then build its full stack (entity done; repository/DTOs/service/controller pending). Likely needs to be created inside the same `@Transactional` `ShowService.addShow` flow, alongside `ShowSeat` generation.
4. Build full `User` and `Booking` CRUD stacks (entities/repos already exist). `Booking` creation is NOT the real booking flow yet — that's Phase 4/5 below; this is just basic CRUD scaffolding for completeness/testing, consistent with the rest of the pattern.
5. **Phase 4 — Redis distributed seat locking (the core learning goal of this whole project).** Not started. Design already discussed conceptually with the person: `SET lock:show:{showId}:seat:{seatId} {userId} NX EX {ttlSeconds}` — atomic "set if not exists" with auto-expiry, no cleanup code needed. Need: a `SeatLockService` using Spring Data Redis (`RedisConnectionFactory`/`RedisTemplate`, already have Redis health-check working from Phase 0), an endpoint to attempt locking N seats for a show, checking both Redis (no existing lock) and Postgres (`ShowSeat.status = AVAILABLE`) before allowing a lock.
6. **Phase 5 — Booking confirmation.** On successful "payment" (can be simulated/stubbed initially), inside a `@Transactional` block: mark the relevant `ShowSeat.status = BOOKED`, set `ShowSeat.booking_id`, set `Booking.status = CONFIRMED`, and release the Redis lock (delete the key). This is the highest-stakes transaction in the app — must be atomic across all affected `ShowSeat` rows.
7. **Phase 6 — Lock expiration edge cases.** What happens if a user's Redis lock expires mid-payment-flow — the confirm-booking step must re-check the lock/availability before committing, not just trust that locking succeeded earlier.
8. **Phase 7 — Kafka events.** Publish a `booking.confirmed` / `booking.cancelled` event after the Postgres transaction commits; build simple consumer(s) (payment-service, notification-service — can be separate Spring Boot modules/JARs, siblings to `bookify/` at the repo root per §2).
9. **Phase 8 — Cancellation flow.** Reverse of confirmation: `Booking.status = CANCELLED`, `ShowSeat.status = AVAILABLE`, `booking_id = null`, inside a transaction; publish cancellation event.
10. **Phase 9 — Booking history.** `GET /users/{id}/bookings` with pagination.
11. **Phase 10 — Load testing.** Simulate many concurrent requests hitting the same seat/show (e.g. JMeter or a simple concurrent script) to actually *observe* the race condition the naive "check-then-act" approach would have, confirming the Redis lock actually prevents it. This is meant to be the payoff moment of the whole project.
12. **Phase 11 — Scaling considerations.** Connection pooling, Kafka partitioning strategy, Redis cluster/sentinel — conceptual discussion, likely not deep implementation given project scope.

## 10. Teaching style to preserve

The person is learning deliberately and wants to write code themselves. When continuing:
- Give hints before full answers; only give a direct fix when they explicitly ask for it.
- Explain the *why* behind a concept before/alongside showing syntax (e.g. why `NX EX` matters for locks, why `@Transactional` matters here, not just "add this annotation").
- Review pasted code line by line rather than rewriting it wholesale — point out what's wrong and why, let them submit the fix.
- Tie new concepts back to earlier lessons already learned (NULL≠NULL, denormalization tradeoffs, Postgres-vs-Redis truth ownership, etc.) rather than re-teaching from scratch.
- Small, single-focus tasks — don't dump multiple unrelated features into one task.