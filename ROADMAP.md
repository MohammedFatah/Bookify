# Bookify — Roadmap

A learning project, not a race to a finished app. Every step below explains
**what problem it solves, why that problem exists, and how the fix actually
works** — not just "build this." Concepts you haven't touched yet (Redis,
Kafka) get a full explainer placed right before the step that needs them,
rather than dumped up front disconnected from any real use.

Kafka's consumer side (payment-service, notification-service) is split into
separate files — see [PAYMENT_SERVICE_PLAN.md](PAYMENT_SERVICE_PLAN.md) and
[NOTIFICATION_SERVICE_PLAN.md](NOTIFICATION_SERVICE_PLAN.md). Both assume
you've read the Kafka primer in this file first.

This document only tells you what to build and why. It deliberately does
not hand you working code — write it yourself, then bring it back for
review, the same way every earlier phase of this project went.

---

## Step 0 — Environment setup

**What problem this solves:** Postgres, Redis, and Kafka are three separate
server processes your app talks to over the network. They have to actually
be running, and running *correctly*, before any Java code can connect to
them. This step exists because "the app won't start" is almost always an
infrastructure problem, not a code problem, and it's worth being able to
tell the difference immediately.

**What to do:**
1. `docker compose up -d` from the repo root — starts Postgres, Redis, Kafka
   as containers, using the root `docker-compose.yml`.
2. `docker compose ps` — confirm `bookify_postgres` shows `(healthy)`, not
   just `Up`. **Why the distinction matters:** "Up" only means the container's
   process started; Postgres actually does a short shutdown-and-restart cycle
   internally while it initializes on a fresh volume, during which the port
   is open but connections fail. The healthcheck (`pg_isready`) is what
   tells you the difference between "started" and "actually ready."
3. `cd booking-service && ./mvnw spring-boot:run` — confirms the app's
   datasource and Redis connection both succeed on boot.
4. If you ever change a `POSTGRES_*` environment variable in
   `docker-compose.yml`: run `docker compose down -v` first. **Why:**
   Postgres's own startup script only creates the user/database/password
   from those env vars once, on a completely empty data volume. If the
   volume already has data in it (from a previous run), the script skips
   initialization entirely and your old credentials silently keep working
   — changing the YAML does nothing until the volume is wiped (`-v` deletes
   it).

**Status: ✅ done.** Postgres, Redis, Kafka all running via root
`docker-compose.yml`; app boots and connects to all three.

---

## Done so far

- Venue, Movie, Screen (+ bulk seat generation), Seat, Show (+ auto `ShowSeat`
  generation, + client-supplied `ShowCategoryPrice`), User — full CRUD stacks.
- Repo restructured: `booking-service/` holds the app, root holds
  `docker-compose.yml`, ready for sibling services.
- Redis (`spring-boot-starter-data-redis`) and Kafka (`spring-boot-starter-kafka`)
  dependencies are **already in `booking-service/pom.xml`** — nothing to add
  when Step 2 / Step 6 start.

---

## Step 1 — Seat availability for a show

**What problem this solves:** every endpoint from here on assumes the
client already knows *which* `seatId`s it wants to act on. Nothing in the
app currently tells a client what seats exist for a show, or which ones are
free. Without this, "pick a seat" is impossible — there's nothing to pick
from.

**Why this problem exists:** you built `Seat` (permanent, tied to a
`Screen`) and `ShowSeat` (that seat, for this specific show, with a status)
weeks ago, but only as internal tables the server writes to. Nothing reads
them back out to a client yet.

**What to build:**
1. **`GET /shows/{showId}/seats`** — for the given show, return every
   seat's `rowId`, `number`, `seatCategory`, current `seatStatus`
   (`AVAILABLE`/`BOOKED`, straight off `ShowSeat`), and its price (join
   against `ShowCategoryPrice` by category, for this `showId`). This is the
   seat map a real cinema-booking UI would render as clickable boxes; for
   you, it's what tells Postman which `seatId`s exist for the next steps.
2. `ShowSeatRepository.findAllByShowId(UUID showId)` — one query.
3. A new small response DTO (e.g. `ShowSeatSummary`) — not raw entities,
   same rule you've followed everywhere else in this project.

**How "which seat" and "how many seats" both get answered:** the client
picks specific `seatId`s off this list and puts them in a `seatIds` array on
every request from here on. There is deliberately no separate "how many"
field anywhere in this design — the count is just `seatIds.size()`. Adding
a redundant count field would just be a second source of truth for a fact
you can already derive, the same reasoning that kept `Screen.capacity`
derived rather than duplicated in multiple places.

**What this doesn't solve yet, on purpose:** this endpoint reads Postgres
only. A seat someone else locked in Redis a second ago still shows
`AVAILABLE` here — that's fine for now. The real conflict check happens at
the *lock attempt* (Step 3), not here. Showing live lock state on the seat
map is a nice-to-have UI improvement, not a correctness requirement.

---

## Concept: what Redis actually is, and why this project needs it

You need this before Step 2 makes any sense.

**What Redis is.** Redis is an in-memory key-value store — think of it as a
giant, extremely fast hash map that lives in RAM instead of on disk, and is
reachable over the network by any process that knows how to speak its
protocol. "In-memory" is the whole point: reading or writing a key takes
microseconds, not the milliseconds a disk-backed database like Postgres
needs. That speed is exactly why it's the right tool for something that
needs to happen the instant a user clicks a seat, not "eventually, once a
transaction commits."

**The problem it solves here — a race condition.** Imagine you *didn't* use
Redis, and instead tried to lock a seat by just checking Postgres directly:

```
1. Request A reads ShowSeat.status → AVAILABLE
2. Request B reads ShowSeat.status → AVAILABLE   (before A has written anything)
3. Request A writes ShowSeat.status = BOOKED
4. Request B writes ShowSeat.status = BOOKED
```

Both requests saw "available," both proceeded, both succeeded — **two
different people just booked the same seat.** This is a classic
"check-then-act" race condition (sometimes called TOCTOU — time-of-check to
time-of-use): the bug lives in the *gap* between reading a value and acting
on it, a gap where another request can sneak in. This is precisely the kind
of bug this whole project exists to teach you to see and fix.

**Why Redis specifically fixes it.** Redis processes commands one at a time,
single-threaded, for any single key. The operation you'll use —
`SET key value NX EX seconds` — checks "does this key already exist?" and
"if not, create it" as one indivisible instruction. There is no gap between
the check and the act, because they're not two separate steps from Redis's
point of view — they're one atomic command. Two requests racing to lock the
same seat can't both win, because Redis literally cannot interleave their
two `SET NX` calls; it runs one to completion, then the other.

**Reading the command apart:**
- `SET key value` — the basic operation: store a value under a key.
- `NX` ("Not eXists") — only perform the `SET` if the key is **not already
  present**. If it is present, the whole command does nothing and reports
  failure. This is the "claim it only if it's free" behavior.
- `EX seconds` — attach an automatic expiry. After that many seconds, Redis
  deletes the key itself — you write no cleanup code, no scheduled job.
  This is why a lock a user never releases (they closed the tab, their
  phone died) doesn't sit there forever; it just times out.

**Why this matters for something you already decided.** Your own schema
notes say `ShowSeat.status` only ever has two values, `AVAILABLE`/`BOOKED`
— **never** `LOCKED`. That wasn't an arbitrary restriction: it's *because*
Redis's `EX` makes a `LOCKED` state in Postgres unnecessary. If "locked"
lived in Postgres, something would have to notice when a lock expired and
clean it up — a background reconciliation job. By keeping "locked" entirely
inside Redis with a TTL, expiry is automatic and Postgres never has to know
a temporary hold existed at all.

**What Redis is *not* good for.** It's memory, not disk — if the Redis
process restarts or crashes, everything in it is gone (by default; it has
optional persistence, unused here). That's *fine* for a lock, because a
lock is supposed to be temporary and re-creatable. It would be a disaster
for `Booking` or `ShowSeat` data, which must survive forever — which is
exactly why those stay in Postgres. This is the Postgres-owns-permanent-truth
/ Redis-owns-temporary-truth split from your own schema design, now with the
actual mechanical reason behind it.

---

## Step 2 — Phase 4: Redis distributed seat locking

The core learning goal of the whole project.

**What problem this solves:** giving a user a short, exclusive window to
decide and pay for specific seats, without letting anyone else grab them in
that window — using the atomic Redis operation from the primer above,
instead of the racy Postgres check-then-act.

**Why it has to happen before `Booking` creation:** a `Booking` (Step 3) is
about to compute a real price from real seats and commit that permanently to
Postgres. It shouldn't do that for seats someone else might be about to
book. Locking first means by the time `Booking` creation runs, this client
has an exclusive (if temporary) claim on those seats.

**What to build:**
1. **`SeatLockService`** — wraps Spring Data Redis's `RedisTemplate` (or
   `StringRedisTemplate`, since your key/value are both plain strings).
   Core operation, one per seat:
   ```
   SET lock:show:{showId}:seat:{seatId} {userId} NX EX {ttlSeconds}
   ```
   The key encodes exactly what's being locked (this show, this seat); the
   value is *who* holds the lock, which matters later when you need to
   check "is this still *my* lock" at confirmation time.
2. **`POST /shows/{showId}/seats/lock`** — body: `{ userId, seatIds }`. For
   each seat:
   - Check Postgres first: is `ShowSeat.seatStatus == AVAILABLE`? (No point
     attempting a Redis lock on a seat that's already permanently booked.)
   - Attempt the Redis `SET ... NX EX` from above.
3. **All-or-nothing across the batch.** If a user asks to lock 4 seats and
   only 3 succeed, don't leave them holding 3 — that's a confusing half-state
   nobody asked for. Either release any locks you *did* acquire before
   reporting failure, or use a Lua script (Redis can run a small script as
   one atomic unit, covering multiple keys) so all 4 succeed or none do.
   This is the same "all succeed or all roll back" instinct behind
   `@Transactional` — just applied to Redis instead of Postgres, because
   Redis has no transaction concept of its own that spans multiple keys the
   way Postgres does.
4. **Response** — include the expiry time, so the client (and you, testing
   in Postman) know exactly when the claim dies.
5. **Unlock endpoint** (optional but useful for testing) —
   `DELETE /shows/{showId}/seats/lock`, for a user who backs out before the
   TTL naturally expires.
6. **Status codes**: seat already `BOOKED` in Postgres → 409. Seat already
   locked by someone else in Redis → 409. Lock acquired → 200/201.

**How to prove it actually works:** lock the same seat twice, from two
different `userId`s, in two separate Postman requests. The second one must
fail. This is the first real, hands-on proof the atomic `SET NX` is doing
its job — before Step 9's load test proves it under real concurrency rather
than two manual clicks.

---

## Step 3 — `Booking` creation (real, not scaffolding)

**What problem this solves:** turning a set of locked seats into a real,
priced, pending booking record — the thing a user is about to pay for.

**Why price can't be client-supplied:** if the request body included
`totalAmount`, nothing would stop a client from sending `{"totalAmount": 0.01}`.
The price has to be **computed by the server**, from data the server
controls, every time. That in turn means the server needs to know exactly
which seats are being booked *before* it can produce a number — which is
why this step needs `seatIds`, not a price.

**What to build**, inside one `@Transactional` method (why `@Transactional`:
this does several related writes — a price lookup, N `ShowSeat` updates,
one `Booking` insert — that must all succeed together or not happen at all,
exactly the reasoning you already applied to `ShowService.addShow`):

1. **`UserSummary`** and **`ShowSummary`** DTOs — new, small, nested in
   `BookingResponse` the same way `VenueSummary` nests into `ScreenResponse`.
2. **`BookingRequest`**: `userId`, `showId`, `seatIds`. No `totalAmount`
   field exists on this DTO at all.
3. **`BookingService.addBooking`**:
   - FK lookups: `userId` → `User`, `showId` → `Show`.
   - For each `seatId`: read its `Seat.seatCategory`, find the matching
     `ShowCategoryPrice` for this `showId` + that category, sum all of them
     → `totalAmount`. This is the one and only place a booking's price gets
     decided.
   - For each corresponding `ShowSeat` row: check `seatStatus == AVAILABLE`
     **and** `booking_id == null`, then set `booking_id` to point at the new
     `Booking`. Leave `seatStatus` as `AVAILABLE` — it only becomes `BOOKED`
     once payment actually succeeds (Step 4). This isn't inventing new
     behavior: your own schema notes on `ShowSeat.booking_id` already say
     *"nullable because an available seat has no booking yet"* — which
     implies the reverse is also true: once a seat has a booking, even a
     pending one, that field gets populated.
   - Save the `Booking` as `PENDING`.
4. **Why check `booking_id == null` here, when Step 2 already checked Redis?**
   Because they guard two different failure windows. The Redis lock is a
   fast, temporary claim with a TTL — it can expire, or in principle be
   raced by a bug. `booking_id` in Postgres is a real column update that
   only one transaction can ever win, permanently. It's the same
   "belt-and-suspenders, never trust an earlier check to still be true"
   habit you'll apply again in Step 4.
5. **`BookingService.getBooking`** — same `findById().orElseThrow(...)`
   shape as every other service you've built.
6. **`BookingController`** — `POST /bookings`, `GET /bookings/{id}`.

---

## Step 4 — Phase 5: Booking confirmation

**What problem this solves:** the moment payment actually succeeds, several
things have to become true *together, permanently* — the booking is
confirmed, the seats are booked, the temporary Redis claim is released.
This is the single highest-stakes operation in the app, because it's where
simulated money changes hands.

**Why seat-linking is already decided by the time this runs:** Step 3
already set `ShowSeat.booking_id` for every seat this booking covers. This
step isn't deciding *which* seats — it's deciding whether this specific
`PENDING` booking gets to keep the seats it already claimed, or not.

**What to build — `POST /bookings/{id}/confirm`**, inside one
`@Transactional` method:

1. Load the `Booking`, then every `ShowSeat` linked to it
   (`findAllByBookingId`).
2. **Re-verify the Redis lock is still held by this booking's `userId`**,
   for these exact `seatId`s. Do not skip this because "we already checked
   at lock time" — time has passed since then, and the lock could have
   expired. (This re-check is the whole point of Step 5, but it has to
   exist here from the start — Step 5 is about deliberately *testing* the
   edge case, not about writing the guard for the first time.)
3. Simulate "payment" — a stub method that returns success, or fails on a
   specific test input so you can exercise the failure path deliberately.
4. For each linked seat: `ShowSeat.seatStatus = BOOKED`. (`booking_id` is
   already set from Step 3 — nothing to change there.)
5. `Booking.bookingStatus = CONFIRMED`.
6. Delete the Redis lock keys for these seats — their job is done. The TTL
   would eventually clean them up anyway, but there's no reason to leave a
   stale lock sitting around once you know it's no longer needed.
7. **The tricky part: step 6 lives outside the transaction.** `@Transactional`
   only governs your JPA/Postgres work — Redis has no idea a Postgres
   transaction exists. If you delete the Redis keys *before* the Postgres
   commit, and the commit then fails and rolls back, you're left with a
   `Booking` that never confirmed but a lock that's already gone — the seat
   now *looks* free to everyone even though nothing was actually booked, but
   also nobody else's request will find it correctly re-lockable in a clean
   state. Structure the method so the Redis deletion happens strictly
   *after* Postgres has committed — Spring's `@TransactionalEventListener`
   (used again in Step 6) is one clean way to guarantee "only run this after
   commit."

---

## Step 5 — Phase 6: Lock expiration edge cases

**What problem this solves:** proving the re-check you wrote in Step 4
actually does something, by deliberately creating the situations it exists
to catch — plus surfacing one more gap the design has left open until now.

**What to test:**
1. Lock a seat, wait past the TTL (set a short one for testing — 5 seconds
   — rather than waiting out a real 5-minute one), then call confirm.
   Expect a clean, deliberate rejection (409, "lock expired, please
   re-lock") — not a silent success, and not an unhandled exception
   producing a 500.
2. Lock a seat as one user, then have a *different* `userId` try to confirm
   using the same `seatId`s (simulating a guessed or replayed request) —
   must fail even though the seat is still `AVAILABLE` in Postgres, because
   the Redis lock value (the `userId` who holds it) doesn't match.
3. Confirm that a failed confirmation, in either case above, leaves Postgres
   completely untouched — no partially updated seats, no `Booking` stuck in
   a strange in-between state. This is `@Transactional` doing its job; you're
   just verifying it here rather than assuming it.
4. **The gap this exposes**: a `Booking` goes `PENDING`, its seats get
   `booking_id` set — and then the user just closes the tab. Nothing
   currently frees those seats. `booking_id` stays set forever;
   `seatStatus` stays `AVAILABLE`; every future attempt to book that seat
   fails the `booking_id == null` check from Step 3, even though the seat
   is, for all practical purposes, free. Decide one fix, deliberately:
   - **Lazy reclaim** — the *next* booking attempt on that seat notices the
     existing `booking_id`'s Redis lock has expired, nulls out `booking_id`,
     and proceeds. Less machinery, fits a project this size.
   - **Scheduled sweep** — a periodic job that finds `PENDING` bookings past
     some age and marks them `FAILED`, releasing their seats. Closer to what
     a production system would actually run.

---

## Concept: what Kafka actually is, and why this project needs it

You need this before Step 6 makes any sense.

**What Kafka is.** Kafka is a distributed, durable message log. A
**producer** (here, `booking-service`) writes messages to a named
**topic**. One or more **consumers** (here, `payment-service` and
`notification-service`) read from that topic, independently, at their own
pace. The key word is *durable*: unlike calling another service's REST
endpoint directly, a message written to Kafka doesn't vanish if the reader
is temporarily down — it sits in the log until a consumer actually reads
it.

**The problem this solves that a direct HTTP call wouldn't.** Imagine
`booking-service` called `payment-service` and `notification-service`
directly over HTTP the moment a booking confirmed. Now `booking-service`'s
confirm endpoint is *coupled* to both of them being up, right now, and fast
— if `notification-service` is slow or down, does the booking confirmation
itself fail? That's clearly wrong; sending a confirmation email is not
important enough to block a paid booking. Kafka decouples this: the
producer just writes one message and moves on; whether a consumer reads it
in the next millisecond or the next five minutes (because it was
temporarily down) doesn't affect the producer at all.

**Topics, partitions, and ordering.** A topic (e.g. `booking.confirmed`) is
split into **partitions** for parallelism — think of each partition as an
independently-ordered sub-log. Kafka guarantees order *within* a partition,
but makes no promise about order *across* different partitions. Every
message has a **key**; Kafka routes all messages with the same key to the
same partition (by hashing the key). That's why this project keys events by
`showId` — every event about a specific show lands on the same partition
and processes in the order it happened, even though events for a
*different* show might land on a different partition and interleave with
this show's events in wall-clock time. That's fine, because nothing about
one show's booking sequence depends on another show's.

**Consumer groups — how two services both get the same message.** A
consumer subscribes to a topic as part of a named **consumer group**. Kafka
guarantees that within one group, each partition's messages go to exactly
one consumer instance — this is how you'd scale a single service out to
multiple instances without duplicating work. But `payment-service` and
`notification-service` are in **different** groups, so each group gets its
own independent copy of every message. That's the mechanism that lets both
services react to the same `booking.confirmed` event without either one
knowing the other exists.

**"At-least-once" delivery, and why that means idempotency matters.** If a
consumer crashes after processing a message but before telling Kafka it's
done ("committing the offset"), Kafka will redeliver that same message to
whichever consumer picks up next. This is a deliberate design choice
(Kafka prefers "you might see a message twice" over "you might silently
lose one") — but it means a consumer that isn't careful could process the
same `booking.confirmed` event twice and, say, record a duplicate payment.
That's why `payment-service`'s plan calls for checking
`existsByBookingId(bookingId)` before recording anything: the same
"reject a duplicate before doing the work" habit you already used for
`ShowSeat` generation, just applied to a Kafka consumer instead of an HTTP
endpoint.

**Why the app's Kafka container is configured the way it is.** The root
`docker-compose.yml` runs Kafka in **KRaft mode** — Kafka managing its own
metadata internally, rather than depending on a separate ZooKeeper cluster
(the older architecture). You don't need to do anything differently because
of this; it's mentioned so the `KAFKA_PROCESS_ROLES: broker,controller`
line in your compose file isn't just unexplained noise.

---

## Step 6 — Phase 7: Kafka events (producer side)

`booking-service` is the **producer** here. The two consumers are separate
deployables — see the two linked plans, which assume you've read the
primer above.

**What problem this solves:** letting other services (recording a payment,
sending a notification) react to a confirmed or cancelled booking, without
`booking-service` having to know those services exist, call them directly,
or wait for them.

**What to build:**
1. Define event payloads as plain DTOs — e.g. `BookingConfirmedEvent
   { bookingId, userId, showId, totalAmount, confirmedAt }`, and a matching
   `BookingCancelledEvent` (used in Step 7).
2. A `KafkaTemplate<String, BookingConfirmedEvent>` bean, with JSON
   serialization configured in `application.yaml`.
3. **Publish after commit, not before.** Wrap the "publish this event"
   logic in a Spring `ApplicationEvent`, raised from inside the confirm
   method, and handle it with `@TransactionalEventListener(phase = AFTER_COMMIT)`.
   **Why this specific mechanism**: if you called `kafkaTemplate.send(...)`
   directly inside the `@Transactional` confirm method, and the Postgres
   transaction then failed and rolled back for any reason, you'd have
   already told the outside world "this booking is confirmed" — a lie. The
   `AFTER_COMMIT` phase guarantees the Kafka message only goes out once
   Postgres has durably committed the fact it describes.
4. Topic names: `booking.confirmed`, `booking.cancelled`. Partition key:
   `showId`, for the ordering reason explained in the primer.
5. Once this is publishing, the two consumer services are independent
   follow-on work — build them in either order, or in parallel.

---

## Step 7 — Phase 8: Cancellation flow

**What problem this solves:** the reverse of confirmation — a user changes
their mind (or a booking needs to be undone), and the seats need to become
genuinely available again.

**What to build**, mirroring Step 4's transactional-boundary care:
1. `POST /bookings/{id}/cancel`.
2. Inside `@Transactional`: `Booking.bookingStatus = CANCELLED`; for every
   `ShowSeat` linked to this booking, `seatStatus = AVAILABLE`,
   `booking_id = null`.
3. After commit: publish `booking.cancelled` (used by `BookingCancelledEvent`
   from Step 6).
4. **Decide and enforce real rules**, don't assume good client behavior:
   Can a `PENDING` booking be cancelled? Can a `CANCELLED` one be cancelled
   again? What about cancelling after the show has already started? Each
   disallowed case needs an actual check and the right exception — 400 if
   the request itself is invalid, 409 if it conflicts with the booking's
   current state, the same distinction you already applied during the
   seat-generation review.

---

## Step 8 — Phase 9: Booking history

**What problem this solves:** a user wants to see their past bookings.
Returning *all* of them in one response doesn't scale — a user with 200
bookings shouldn't get a 200-row JSON blob every time.

**What to build:**
- `GET /users/{id}/bookings`, paginated.
- Spring Data's `Pageable` + `Page<Booking>` — `BookingRepository` gets
  `findByUserId(UUID userId, Pageable pageable)` for free via method-name
  derivation, the same trick behind `findByScreenId`/`countByScreenId`
  earlier in this project. Spring translates the method name into the
  `WHERE` clause and wires in `LIMIT`/`OFFSET` from the `Pageable` for you.
- Response: a page of a new `BookingSummary` DTO (nested `ShowSummary`, no
  full entity) — not raw `Booking` rows.

---

## Step 9 — Phase 10: Load testing

**What problem this solves:** everything so far has been tested by hand,
one request at a time. That proves the *logic* is right, but not that it
holds up under real concurrency — many requests arriving genuinely at once,
which is the actual scenario the Redis lock exists to defend against. This
step is the payoff: watching the race condition from the Redis primer
actually happen, then watching the fix actually prevent it.

**What to do:**
1. Pick a tool — a simple Java `ExecutorService` firing many concurrent
   lock requests at the *same* seat is enough to demonstrate the effect;
   JMeter, Gatling, or k6 if you want proper load-test reporting on top.
2. **Before trusting the fix, disprove the alternative.** Temporarily strip
   the Redis check out of the lock endpoint, leaving only the Postgres
   check-then-act. Run the concurrent test. Watch it actually double-book a
   seat — this is the bug from the primer, happening for real, not just
   described in the abstract. Don't skip this step; seeing the failure mode
   with your own eyes is the entire point of this phase.
3. Put the Redis check back, rerun the same concurrent test. Confirm exactly
   one request wins per seat and every other one gets a clean 409.

---

## Step 10 — Phase 11: Scaling considerations (discussion, not deep implementation)

A conceptual pass, appropriately scoped for this project — understanding
the tradeoffs matters more here than implementing all of them.

- **Connection pooling.** Spring Boot's default connection pool is HikariCP.
  Every request that touches Postgres borrows a connection from a fixed-size
  pool and returns it when done; if your load test in Step 9 fires more
  concurrent requests than the pool has connections, the excess requests
  simply queue and wait. Look at Hikari's default pool size versus how many
  concurrent requests you actually generated.
- **Kafka partitioning.** You keyed events by `showId` in Step 6 for
  ordering. Discuss what happens when one show (a blockbuster release)
  generates disproportionately more traffic than every other show combined
  — all of *its* events still land on one partition, so that partition
  becomes a bottleneck no matter how many partitions the topic has overall.
  What alternative keying strategies exist, and what ordering guarantee
  would you give up to get better load distribution?
- **Redis Cluster/Sentinel.** A single Redis instance is a single point of
  failure. Discuss: if Redis goes down entirely, what happens to seat
  locking — can bookings still proceed at all, or does the whole flow halt?
  Is that acceptable for this project's scope, and how would Sentinel
  (automatic failover to a replica) or Cluster (sharding across multiple
  Redis nodes) change that failure mode?

No code is required for this step unless a specific piece of it turns out
to be cheap enough to actually demonstrate.
