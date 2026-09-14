# payment-service — Plan

**Read the "Concept: what Kafka actually is" section in
[ROADMAP.md](ROADMAP.md) first** — topics, partitions, consumer groups, and
at-least-once delivery are all explained there and assumed known below.
This file only covers what's specific to *this* service.

**Why this service exists as a separate thing at all.** In a real cinema
booking system, "did the booking get made" and "did the money actually
move" are two different concerns with very different failure modes and
compliance requirements — payment processing typically talks to a real
gateway (Stripe, Razorpay), needs retry/reconciliation logic, and often has
regulatory requirements (PCI compliance) that have nothing to do with
seat inventory. Splitting payment recording into its own service, reachable
only through an event rather than a direct call, is a small-scale rehearsal
of that real separation — `booking-service` doesn't need to know or care
*how* payment gets recorded, only that it eventually does.

Depends on [ROADMAP.md](ROADMAP.md) Step 6 (Kafka events) being done in
`booking-service` first — this service is a pure consumer; there's nothing
to build here until `booking.confirmed` is actually being published.

---

## Step 1 — Project setup

**What problem this solves:** giving this service its own build, its own
process, and its own data — so it can be deployed, scaled, or even
rewritten independently of `booking-service`, which is the entire point of
splitting it out in the first place.

1. New Spring Boot project at `payment-service/`, matching
   `booking-service`'s Java 21 / Spring Boot 4.1.1 versions. Package:
   `com.bookify.payment`.
2. Dependencies: `spring-boot-starter-kafka`, `spring-boot-starter-data-jpa`,
   `postgresql`, `lombok`. **Notice what's missing**:
   `spring-boot-starter-validation`. `booking-service` needs it because it
   validates untrusted HTTP request bodies from clients; this service never
   receives an HTTP request from a client at all — its only input is a Kafka
   message from a system you already trust (your own producer). There's
   nothing to validate against, so the dependency would be dead weight.
3. `application.yaml` — its own `spring.application.name`, its own
   datasource.

**The database decision, and why it matters.** Use a **separate Postgres
database**, not the same one `booking-service` uses. This is the
"database-per-service" pattern, and the reasoning behind it is worth
understanding, not just following: if two services shared one database,
`payment-service` could technically write a foreign key referencing
`booking-service`'s `bookings` table directly. That looks convenient, but
it means the two services are no longer actually independent — a schema
migration in `booking-service` could break `payment-service` without
either team knowing why, and you can no longer reason about one service's
data in isolation from the other's. Keeping them in separate databases
makes the boundary *physically* real, not just a polite convention you
could accidentally violate later.

4. Point Kafka config at the same broker already running in the root
   `docker-compose.yml` (`localhost:9092`, while this service runs on the
   host — see the container-networking note at the bottom of this file for
   what changes once it doesn't).

---

## Step 2 — Data model

**What problem this solves:** this service's only job is "remember that a
payment happened for a given booking." That's a much smaller responsibility
than `booking-service` has, and the data model should reflect exactly that
— nothing more.

- `Payment` entity: `id (UUID)`, `bookingId (UUID)`, `amount (BigDecimal)`,
  `status` (enum: `COMPLETED`/`FAILED`, since "payment" itself is simulated
  at this stage, not a real gateway call), `processedAt (LocalDateTime)`.
- **`bookingId` is a plain column, not a JPA `@ManyToOne` foreign key.**
  This isn't an oversight — it's a direct consequence of the database
  decision in Step 1. A foreign key is a constraint the *database* enforces
  by looking up the referenced row; if `payment-service`'s database can't
  see `booking-service`'s `bookings` table at all (separate database), there
  is nothing for a foreign key to point at. The relationship exists only in
  your application's understanding of the data, not in the schema.
- One repository, no service-layer ceremony — there's genuinely not enough
  logic here yet to justify a separate service class.

---

## Step 3 — Kafka consumer

**What problem this solves:** actually reacting to a confirmed booking —
turning "an event arrived" into "a `Payment` row exists."

**How a `@KafkaListener` method actually runs, mechanically.** This is
worth understanding rather than treating as magic:

1. Spring Kafka runs a background thread (a "listener container") for your
   `@KafkaListener` method. That thread does one thing in a loop: call
   `poll()` against the broker, asking "give me any new records on this
   topic I haven't seen yet."
2. The broker replies with a batch of records — each one containing the
   raw bytes that were published, plus metadata like the partition and
   offset (offset = the record's position in that partition's log).
3. Spring deserializes those bytes into your DTO — the JSON deserializer
   you configure has to agree with however `booking-service` serialized the
   event, or this step fails outright (a real gotcha: if `booking-service`
   ever renames a field on `BookingConfirmedEvent`, this consumer breaks
   until it's updated to match — the two services are decoupled at the
   *transport* level, but not at the *shape* of the data they exchange).
4. Your listener method runs with the deserialized object.
5. If your method returns normally (no exception thrown), Spring commits
   the offset — telling the broker "I've successfully processed up through
   here." If it throws, the offset is *not* committed, and — this is the
   important part — **the next poll will return that same record again**,
   either to this consumer after it restarts, or to another instance in the
   same group. This is the literal mechanism behind "at-least-once
   delivery": redelivery isn't a bug or an edge case, it's what happens by
   default whenever processing fails between reading a record and
   committing past it.

**What to build:**

```
@KafkaListener(topics = "booking.confirmed", groupId = "payment-service")
```

- **Why this exact `groupId`, and why it matters relative to
  `notification-service`**: each service uses its *own* `groupId`. Recall
  from the Kafka primer that Kafka guarantees only one consumer *within a
  group* gets each message — but two *different* groups each get their own
  full, independent copy. If `payment-service` and `notification-service`
  accidentally shared a `groupId`, they'd be *competing* for the same
  messages instead of each reliably getting every one.
- Deserialize into the same shape as `booking-service`'s
  `BookingConfirmedEvent`. Either duplicate the DTO class here, or share it
  via a small common module — duplicating is the simpler, more decoupled
  choice for two services this size; a shared module becomes worth the
  coupling only once there are enough services that copy-pasting the same
  class repeatedly starts actively hurting.
- On receipt: simulate payment processing (a stub — always succeeds, or
  fails for a specific test amount so you can exercise the failure path on
  purpose), then save a `Payment` row.

**Idempotency — the part that actually matters here.** Walk through the
concrete scenario from the mechanics above: this listener processes a
message, saves the `Payment` row, but crashes (or the JVM gets killed,
or the network blips) *before* the offset commit goes through. On restart,
Kafka redelivers that exact same message — and without a guard, you'd save
a **second** `Payment` row for the same booking, silently double-charging
in your data even though nothing actually went wrong except bad timing.

The fix is the same shape as a check you've already written in
`booking-service` (the duplicate-seat-generation guard): reject the
duplicate *before* doing the real work.

```
if (paymentRepository.existsByBookingId(event.bookingId())) return;
```

This pattern — checking "have I already done this?" before acting, so that
processing the same input twice is harmless — is called **idempotency**,
and it's the standard answer to Kafka's at-least-once guarantee. Kafka
itself doesn't (by default) prevent redelivery; your consumer has to be
written so redelivery doesn't matter.

---

## Step 4 — (Optional stretch) `payment.completed` event

**What problem this would solve:** right now, `notification-service`
listens to `booking.confirmed` directly. If you wanted the notification to
only fire *after payment is actually recorded* (a more realistic ordering),
`payment-service` could publish its own `payment.completed` (or
`payment.failed`) event once it saves a `Payment` row, and
`notification-service` could listen to *that* instead.

**Why this is a legitimate pattern, not just extra complexity for its own
sake:** this is a small taste of an **event chain** (related to what's
called a Saga in distributed-systems terminology) — instead of one service
being called directly by everyone downstream of it, each service reacts to
the event before it and produces its own event for whatever comes next.
It keeps every service coupled only to the *event shape* immediately before
it, never to who's listening after it.

Skip this initially — it's not required for the core learning goal, which
is consuming and reacting to one event correctly. Add it later if you want
to see a multi-hop chain in action.

---

## Step 5 — Running it

1. Kafka is already up via the root `docker compose up -d` — no separate
   infra needed for this service.
2. `cd payment-service && ./mvnw spring-boot:run` — a separate terminal,
   alongside `booking-service`.
3. Confirm a booking through `booking-service`'s confirm endpoint. Watch
   this service's logs for the consumer picking up the event, then query
   its `Payment` table to see the row land.
4. **A useful sanity check while you're learning this**: stop
   `payment-service`, confirm another booking in `booking-service` (the
   event still publishes to Kafka, which durably holds it), then start
   `payment-service` back up — watch it consume the message it missed
   while it was down. This is the concrete difference between Kafka and a
   direct HTTP call: a call would have simply failed while this service was
   down; the message just waited.

---

## Note for later — container networking

Once this service itself runs as a container (rather than on the host via
`mvnw`), the Kafka `KAFKA_ADVERTISED_LISTENERS` setting in the root
`docker-compose.yml` needs a second, container-facing listener — `localhost`
only resolves correctly for host processes, not for one container trying to
reach another. Flagged already in `docker-compose.yml`'s review; revisit it
at that point, not before.
