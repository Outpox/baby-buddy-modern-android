# Interface Control Document (ICD)

High-level definition of the interfaces between this Android app and the external systems it talks to. The app is a pure client: every interface below is owned by the [Baby Buddy](https://github.com/babybuddy/babybuddy) server unless stated otherwise. See [ARCHITECTURE.md](ARCHITECTURE.md) for internal design and `app/src/main/java/com/example/data/api/BabyBuddyApi.kt` for the exact Retrofit declarations.

## 1. Scope and system context

```
┌───────────────────────────┐          ┌──────────────────────────────┐
│  Baby Buddy Android app   │  HTTPS   │  Baby Buddy server (self-    │
│  (this repo, client only) │◄────────►│  hosted, external system)    │
│                           │ REST/JSON│  REST API under /api/        │
└───────────────────────────┘          └──────────────────────────────┘
```

- **Controlled interface:** the Baby Buddy REST API. The server defines schemas, validation and persistence; the app adapts to it.
- **No other external interfaces.** No push service, no analytics, no third-party backend or SDK.
- Internal boundaries (repository ⇄ UI  widgets/notifications) are in-process and out of scope for this ICD.

## 2. Transport, authentication, formats

| Property | Value |
|---|---|
| Protocol | HTTPS, REST (Django REST Framework on the server) |
| Base path | `<server-url>/api/` |
| Auth | `Authorization: Token <api-token>` per request; token issued by the server, stored on-device encrypted (Android Keystore) |
| Optional gateway auth | `CF-Access-Client-Id` / `CF-Access-Client-Secret` when the server sits behind Cloudflare Access |
| Payload | JSON (Moshi); timestamps ISO 8601; dates `YYYY-MM-DD` |
| TLS | standard validation, or opt-in permissive trust for self-signed servers |
| Errors | HTTP status codes; 4xx treated as permanent write failures, 5xx/network retried with backoff |

## 3. Interface inventory (app ⇄ Baby Buddy API)

| Endpoint | Methods used by the app | Purpose / key filters |
|---|---|---|
| `api/children/` | GET | Child list; selects the tracked child |
| `api/feedings/` | GET, POST, PATCH, DELETE | Breast/bottle feeds; `child`, `start_min`/`start_max`, `limit` |
| `api/sleep/` | GET, POST, PATCH, DELETE | Sleep sessions; `child`, `start_min`, `end_min`, `limit` |
| `api/changes/` | GET, POST, PATCH, DELETE | Diaper changes; `child`, `date_min`/`date_max` |
| `api/tummy-times/` | GET, POST, PATCH, DELETE | Tummy time; `child`, `date_min`/`date_max` |
| `api/pumping/` | GET, POST, PATCH, DELETE | Pumping sessions; `child`, `date_min`/`date_max` |
| `api/temperature/` | GET, POST, DELETE | Temperature entries; `child`, `date_min`/`date_max` |
| `api/weight/` | GET, POST, DELETE (generic) | Weight (kg); `child`, `date`, `limit` |
| `api/height/` | GET, POST, DELETE (generic) | Length (cm; lying-down length under 2); `child`, `date`, `limit` |
| `api/head-circumference/` | GET, POST, DELETE (generic) | Head circumference (cm); `child`, `date`, `limit` |
| `api/notes/` | GET, POST, PATCH, DELETE (generic) | Notes; `child`, `date_min`/`date_max`; also carries baths, milk stash, caregiver/side metadata |
| `api/tags/` | GET | Server tags for quick notes |
| `api/timers/` | GET, POST, PATCH, DELETE (generic) | Shared live timers (see §5) |
| `api/{endpoint}/` (generic) | POST / PATCH / DELETE | Uniform write path used by the offline write queue for any resource |

Writes go only through the generic create/update/delete path so the queued-write engine can treat every resource uniformly (`createRecord` / `updateRecord` / `deleteRecord` in `BabyBuddyRepository`).

## 4. Data ownership and sync semantics

- **Server is the source of truth.** The device keeps only: an encrypted login, an ordered queue of unsent writes, and a cache of today's entries.
- **Reads** are pull-based: "today" sync on launch/resume/3-min interval; history and trends queries on demand.
- **Writes** are queued locally, flushed in order, retried on network/5xx, dropped on 4xx (surfaced as a sync error); a retried create first searches for an existing record to survive lost responses.
- **Conflicts:** last-writer-wins per record, except shared timers (§6) which compare an explicit `changedAtMillis` carried in the timer name.

## 5. Implicit interfaces (encoding inside server fields)

The server has no fields for some app features, so they are encoded in stock fields — part of this ICD because other clients must parse them identically:

- **Note text:** `Caregiver: <name>` and, on breastfeeds, `Left: mm | Right: mm | Sides: …`; diaper extras as `Rash | Cream: … | Consistency: …`.
- **Tags:** a note tagged `bath` is a bath; a note tagged `milk-stash` ("Milk stash: N ml") is the milk stash.
- **Shared timers:** a Baby Buddy timer whose name carries marker `#bb1` plus serialized live state. The full protocol is defined in §6.

## 6. Shared (parent-to-parent) timer sync

Baby Buddy timers only have a name and a start time, so the app uses them as a message bus: the entire live state of a running breastfeed or sleep is serialized into the timer **name**, behind a human-readable prefix. The server applies no logic; all merge rules are client-side.

**Wire format** (`SharedTimers.kt`): `<readable> #bb1 k=<kind>;<field>=<value>;…`, times in epoch seconds except `t` (epoch millis, to order changes made within the same second). Timers without the `#bb1 ` marker (e.g. created in the web UI) are ignored.

Feed snapshot:

| Field | Meaning |
|---|---|
| `k=feed` | kind discriminator (`k=sleep` for sleeps) |
| `f` | server id of the feeding record this session belongs to (links an adopted timer to its record) |
| `a` | active side, `L`/`R` |
| `p` | paused flag, `0`/`1` |
| `l` / `r` | accumulated left/right seconds, valid as of `t` |
| `fs` | first side of the session |
| `ss` / `pa` / `la` | session start / paused-at / last-active (epoch seconds) |
| `t` | `changedAtMillis`, the ordering key |

Sleep snapshot: `k=sleep`, `s` = start (epoch seconds), `t` = `changedAtMillis`.

**Lifecycle and merge rules:**

1. Any local timer action (start, side switch, pause, correct, end) bumps `changedAtMillis` and signals a conflated channel; `pushTimer` then creates, renames or deletes the server timer.
2. `pollSharedTimers` runs on every refresh and every 15 s while the app is resumed.
3. **Newest `changedAtMillis` wins.** A polled snapshot newer than local state is adopted: the other parent's phone shows the same clock, side and pause state and can continue the session.
4. A timer deleted elsewhere ends the local session; `feedEndedAt`/`sleepEndedAt` stamps stop an ended session from being re-adopted from a stale snapshot.
5. An adopted feed is linked to its feeding record via `f`, so side taps on the adopting phone update the same record.
6. Only the phone that stops a running sleep writes the sleep entry to `api/sleep/`; the timer is then deleted for everyone.

Failure behavior: push/poll are best-effort; a lost push is healed by the next local change or by the peer's own push, and a lost poll simply retries on the next interval. Two parents acting in the same instant resolve by `t`, so at most one state survives.

## 7. Non-interfaces

- No websocket/push channel; multi-device liveness comes from timer polling (every 15 s while resumed) plus refresh intervals.
- Home-screen widgets, notifications and reminders are device-local outputs; they never touch the network directly.

## 8. References

- Baby Buddy server & API: https://github.com/babybuddy/babybuddy (API docs: https://docs.baby-buddy.net/api/)
- Client-side contract: `app/src/main/java/com/example/data/api/BabyBuddyApi.kt` (Retrofit service + DTOs)
- Internal design: [ARCHITECTURE.md](ARCHITECTURE.md)
