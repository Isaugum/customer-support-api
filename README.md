# Customer Support API

A demo customer-support/helpdesk project. Users open support tickets - operators can claim and resolve them via REST and WebSocket chat.

**Stack:** Java 21 · Quarkus 3.x · PostgreSQL · Flyway · SmallRye JWT · WebSockets Next · Hibernate ORM Panache · Docker

---

## Contents

- [Quick Start](#quick-start)
- [Make targets](#make-targets)
- [Ticket Lifecycle](#ticket-lifecycle)
- [API Reference](#api-reference)
- [Testing with Postman](#testing-with-postman)
- [Seed Data](#seed-data)

---

## Quick Start

```bash
make init        # creates .env, generates JWT keys, builds, and starts the stack
```

> **IMPORTANT:** `make init` copies configuration from .env.example, which is currently not empty. Either use the defaults for demo purposes or update .env.example before running `make init`.

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui`

---

## Make targets

| Target | Description |
| :--- | :--- |
| `make init` | Full first-time setup — creates `.env`, generates keys, builds, starts stack |
| `make dev` | Starts Postgres in Docker, runs Quarkus in dev mode (hot reload) |
| `make docker-up` | Builds and starts the full Docker stack |
| `make docker-down` | Stops the stack |
| `make test` | Runs all tests (requires Docker for Dev Services) |
| `make clean` | Cleans Maven output |
| `make reset` | Cleans Maven output and destroys DB volume |

---

## Ticket Lifecycle

```
WAITING  →  ACTIVE  →  CLOSED  →  ARCHIVED
```

- **WAITING** — Created by a user, pending operator pickup
- **ACTIVE** — Operator has claimed it; WebSocket chat is live
- **CLOSED** — Resolved by the operator
- **ARCHIVED** — Soft-deleted; hidden from the active queue

---

## API Reference

| Method | Path | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Public | Returns a JWT on valid credentials |
| `GET` | `/ticket/all` | Operator | Lists all non-archived tickets. Supports `?status=`, `?roomId=`, `?sort=`, `?order=` |
| `GET` | `/ticket/{id}` | Operator | Get a single ticket by ID |
| `POST` | `/ticket/new` | User | Open a new support ticket |
| `POST` | `/ticket/{id}/take` | Operator | Claim a WAITING ticket |
| `POST` | `/ticket/{id}/close` | Operator | Close an ACTIVE ticket (assigned operator only) |
| `POST` | `/ticket/{id}/archive` | Operator | Archive a CLOSED ticket (assigned operator only) |
| `WS` | `/chat/{ticketId}` | User / Operator | Real-time chat (see below) |

Full schema and request/response models are available at `/swagger-ui`.

---

## Testing with Postman

Import both files from the `postman/` folder:
- `customer-support.postman_collection.json`
- `customer-support.postman_environment.json`

Select the **Customer Support API** environment.

Auth is cookie-based — Postman stores the `jwt` cookie automatically after login and sends it on every subsequent request, including WebSocket handshakes. No tokens need to be copied or pasted manually.

### REST flow (sections 1–3)

Run requests top to bottom in this order:

| Step | Request | Notes |
| :--- | :--- | :--- |
| 1 | **1 · Auth → Login as User (miha)** | Sets the `jwt` cookie, captures `user_id` |
| 2 | **2 · Tickets (User) → Open Ticket** | Creates a WAITING ticket, captures `ticket_id` |
| 3 | **3 · Tickets (Operator) → Login as Operator (tone)** | Overwrites the `jwt` cookie, captures `operator_id` |
| 4 | **3 · Tickets (Operator) → View All Tickets** | Lists all tickets as operator |
| 5 | **3 · Tickets (Operator) → Take Ticket** | Claims the ticket, transitions to ACTIVE |
| 6 | **3 · Tickets (Operator) → Close Ticket** | Transitions to CLOSED |
| 7 | **3 · Tickets (Operator) → Archive Ticket** | Transitions to ARCHIVED (alternative to step 6) |

Steps 6 and 7 are mutually exclusive — run only one per flow.

### WebSocket flow (section 4)

WebSocket requests cannot be included in the collection runner — they must be opened manually as tabs. The requests in section 4 have the correct URL pre-filled; Postman sends the `jwt` cookie automatically at connect time.

Follow this order to have both participants connected simultaneously:

1. Run **Login as User** (step 1 above) if not already logged in as user
2. Open **4 · WebSocket Chat → User connects to chat** → click the saved request → **Open in new WebSocket tab** → click **Connect**
3. Run **Login as Operator** (step 3 above) to switch the active session
4. Open **4 · WebSocket Chat → Operator connects to chat** → **Open in new WebSocket tab** → click **Connect**
5. Both tabs are now live — send plain text messages in either tab to chat

The server pushes full message history immediately on connect. All sessions for a ticket are closed automatically when the ticket is CLOSED or ARCHIVED.

> **Note:** Because only one user can be logged in at a time (one cookie per domain), the order above matters. The cookie is checked once at connect time — switching sessions afterwards does not affect already-open WebSocket connections.

---

## Seed Data

Applied automatically on startup via `V2__seed_data.sql`. All passwords are `password`.

**Users**

| ID | Username | Role | Email |
| :--- | :--- | :--- | :--- |
| 1 | Miha | User | `miha@example.com` |
| 2 | Brina | User | `brina@example.com` |
| 3 | Tone | Operator | `tone@example.com` |
| 4 | Peter | Operator | `peter@example.com` |

**Rooms**

| ID | Name |
| :--- | :--- |
| 1 | Tehnika |
| 2 | Storitve |
| 3 | Pogovor |

IDs are stable as long as the database hasn't been reset. If you wipe the DB (`make reset`) and restart, IDs will be reassigned in the same order.
