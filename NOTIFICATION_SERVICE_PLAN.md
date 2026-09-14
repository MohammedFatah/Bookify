# notification-service — Plan

**Read the "Concept: what Kafka actually is" section in
[ROADMAP.md](ROADMAP.md) first**, and read
[PAYMENT_SERVICE_PLAN.md](PAYMENT_SERVICE_PLAN.md)'s Step 3 for the
mechanics of how `@KafkaListener` actually runs — this file assumes both
and won't repeat them.

**Why this service exists as a separate thing at all.** Same underlying
reason as `payment-service`: telling a user "your booking is confirmed"
is a completely different concern from recording that money moved, with a
different owner, different failure tolerance, and no reason to block on
each other. Building it as its own consumer of the same event is what lets
you see that two independent services can react to one fact
(`booking.confirmed`) without either knowing the other exists.

Depends on [ROADMAP.md](ROADMAP.md) Step 6 (Kafka events) being done in
`booking-service` first. Can be built in parallel with `payment-service` —
the two don't depend on each other, both just consume from
`booking-service`.

---

## Step 1 — Project setup

1. New Spring Boot project at `notification-service/`, same Java 21 / Spring
   Boot 4.1.1 versions as `booking-service`. Package: `com.bookify.notification`.
2. Dependencies: `spring-boot-starter-kafka`, `lombok`. **No database
   dependency by default** — see the "why no persistence" discussion in
   Step 2 before deciding whether you want the optional audit trail.
3. `application.yaml` — own `spring.application.name`, Kafka config pointed
   at the same broker as `payment-service`.

---

## Step 2 — Kafka consumer

**What problem this solves:** letting a user know their booking's outcome,
without `booking-service` (or `payment-service`) having to know anything
about *how* that notification gets delivered.

**What to build:**

```
@KafkaListener(topics = { "booking.confirmed", "booking.cancelled" }, groupId = "notification-service")
```

- **This service listens to *two* topics — `payment-service` only listens
  to one.** That's a deliberate, meaningful difference: a user needs to
  hear about a cancellation just as much as a confirmation, but
  `payment-service` has no reason to react to a cancellation at all in this
  design (no refund logic exists yet — that's a `Booking.CANCELLED` state
  your schema already anticipates, not something wired up here).
- **`groupId = "notification-service"` — a different group from
  `payment-service`'s `"payment-service"`.** This is the second concrete
  example of the consumer-group concept from the primer: both services
  subscribe to `booking.confirmed`, and because they're in different
  groups, **both** get their own full copy of every message. If they
  accidentally used the same `groupId`, Kafka would split the messages
  between them instead — each event would go to only one of the two
  services, unpredictably, which is very much not what you want here.
- Two ways to handle "which topic was this": either one listener method
  covering both, checking which topic the record came from, or two
  separate listener methods (one per topic). Two methods is simpler to
  read — each one already knows what kind of event it's handling, no
  branching required.
- On receipt: log/print a simulated notification, e.g.
  `"Notification: booking {bookingId} confirmed for user {userId}"`. That's
  the entire core loop, deliberately trivial — the learning value here is
  the Kafka wiring itself, not building real email/SMS integration (out of
  scope; a stubbed log line is an honest stand-in for "notification sent"
  at this project's scope).

**Why this service, unlike `payment-service`, doesn't strictly need an
idempotency check.** Recall from `payment-service`'s plan that
at-least-once delivery means this consumer *can* receive the same message
twice. The difference is in the *consequence* of that happening:
`payment-service` processing the same event twice creates a duplicate
financial record — a real correctness bug. This service processing the
same event twice prints one extra log line, or (if a real email/SMS
integration existed) sends one duplicate notification. Both services face
exactly the same Kafka guarantee; whether that guarantee is dangerous
depends entirely on what the consumer *does* with a duplicate, not on
Kafka itself. That's the actual lesson: idempotency isn't something every
consumer needs by rule — it's something you need whenever reprocessing has
a cost worth guarding against. Worth deciding consciously here, not just
skipping the check because it's less code.

---

## Step 3 — (Optional) audit trail

**What problem this would solve:** right now, "did this user get notified"
is only visible in console output, which disappears the moment the process
restarts or you're not looking at the terminal.

If you want something durable to query instead:

- Add `spring-boot-starter-data-jpa` + `postgresql`, a `Notification`
  entity (`id`, `bookingId`, `userId`, `type`
  [`BOOKING_CONFIRMED`/`BOOKING_CANCELLED`], `sentAt`), its own database —
  same "don't reach into another service's database" reasoning as
  `payment-service`'s Step 1.
- `GET /notifications/{userId}` if you want to inspect them via an
  endpoint rather than querying the database directly.

Skip this until you actually want it. The console-log version already
proves the Kafka consumer wiring works, which is this service's actual
purpose — persistence here is a nice-to-have, not a requirement the way it
was for `payment-service` (where losing a payment record silently would be
a real bug, not just a missing feature).

---

## Step 4 — Running it

1. Kafka already up via the root `docker compose up -d`.
2. `cd notification-service && ./mvnw spring-boot:run` — a third terminal,
   alongside `booking-service` and `payment-service`.
3. Confirm, then cancel, a booking through `booking-service` — watch both
   notification log lines appear here, and (if you're running it
   alongside `payment-service`) notice that the *confirmation* event
   produces output in both services independently, from the same single
   message `booking-service` published once.

---

## Note for later — container networking

Same caveat as `payment-service`: once this runs as a container, Kafka's
`KAFKA_ADVERTISED_LISTENERS` needs a container-facing listener alongside
the host-facing one. Revisit when containerizing, not before.
