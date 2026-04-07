# Customer Support API

A real-time customer support platform built with Quarkus. Users open support tickets; operators claim and resolve them via REST and WebSocket chat.

**Stack:** Java 21 · Quarkus 3.x · PostgreSQL · Flyway · SmallRye JWT · WebSockets Next · Hibernate ORM Panache · Docker

---

## Contents

- [Quick Start](#quick-start)
- [Environment](#environment)
- [Make targets](#make-targets)
- [Ticket Lifecycle](#ticket-lifecycle)
- [API Reference](#api-reference)
- [WebSocket Chat](#websocket-chat)
- [Testing with Postman](#testing-with-postman)
- [Seed Data](#seed-data)

---

## Quick Start

```bash
make init        # creates .env, generates JWT keys, builds, and starts the stack
```

The first time, open `.env` and set a real `DB_PASSWORD` before running. After that, everything is automatic.
(Data provided in .env.example is functional and will work for demo and testing purposes)

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui`

---

## Environment

Sensitive configuration lives in `.env` (gitignored). `make init` command copies them from the example to get started:

```bash
cp .env.example .env
```

| Variable | Description |
| :--- | :--- |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |

JWT signing keys are generated into `keys/` (also gitignored) via `make init`, or manually via `make keys`.

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

## WebSocket Chat

Connect to `ws://localhost:8080/chat/{ticketId}`.

Authentication happens during the HTTP upgrade — no messages are needed for auth. Provide the JWT in one of two ways:

**Header** (Postman, server clients, native apps):
```
Authorization: Bearer <token>
```

**Query parameter** (browser clients — the browser WebSocket API cannot set custom headers):
```
ws://localhost:8080/chat/5?token=<token>
```

Once connected, the server pushes full message history, then the connection is open for chat. Messages are plain text and broadcast to all participants on that ticket. The server closes all sessions for a ticket automatically when it is closed or archived.

Only the ticket's creator and its assigned operator may connect. All other tokens are rejected at handshake.

---

## Testing with Postman

Import both files from the `postman/` folder:
- `customer-support.postman_collection.json`
- `customer-support.postman_environment.json`

Select the **Customer Support API** environment. Run the requests top to bottom — each step captures tokens and IDs automatically into environment variables used by subsequent requests.

For WebSocket requests (section 4), Postman requires a manual step: open a **New → WebSocket** tab, add `Authorization: Bearer {{user_token}}` in the **Headers** tab (not the standard auth tab — that only applies to HTTP), and connect. Full instructions are in each request's description inside the collection.

! IMPORTANT - An `ACTIVE` ticket is required for this step !

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
