# CLI interactive fetch wait (TTY live region)

Informal requirement; delete or shrink once implemented.

## Problem

- On TTY submit, `ttyAdapter` clears the live region then `await processInput(...)`. Until slow work resolves, nothing repainted the live region, so the **input box disappeared** during network waits.
- **Desired:** Current prompt shows a clear wait line; input box stays visible, greyed, placeholder `loading ...`.

## Implemented (phase 1 — TTY chrome)

| Piece | Location |
|------|-----------|
| TTY repaint + ellipsis | `ttyAdapter.ts`: `onInteractiveFetchWaitChanged`, `formatInteractiveFetchWaitPromptLine`, `INTERACTIVE_FETCH_WAIT_PROMPT_FG` |
| Placeholder context | `renderer.ts`: `interactiveFetchWait` in `PlaceholderContext`, `PLACEHOLDER_BY_CONTEXT` |
| Grey box + no cursor | `isGreyDisabledInputChrome`, `grayDisabledInputBoxLines` in `renderer.ts`; `drawBox` / `doFullRedraw` in `ttyAdapter.ts` |
| Output contract | `types.ts`: `OutputAdapter.onInteractiveFetchWaitChanged` |

**Tests:** `cli/tests/interactiveFetchWait.test.ts` — `processInput`, `INTERACTIVE_FETCH_WAIT_LINES`, renderer helpers; **no Cypress** for this (flaky).

## Phase 2 (done)

| Piece | Location |
|------|-----------|
| Wait lines + state + runner | `interactiveFetchWait.ts`: `INTERACTIVE_FETCH_WAIT_LINES`, `InteractiveFetchWaitLine`, `runInteractiveFetchWait`, `getInteractiveFetchWaitLine` |
| Command wiring | `interactive.ts` (`fetchWaitLine` on param commands, recall paths, gmail/email); token list remove-completely in `ttyAdapter.ts` |

## Future phases

| Phase | Scope |
|-------|--------|
| 3 | Cancellable long waits — see **Phase 3 (planned)** below. |

### Phase 3 (planned) — cancellable interactive fetch wait

**Goal:** While the TTY shows the interactive fetch wait chrome (grey box, blue wait line), the user can **cancel the in-flight work** (e.g. Esc) so the CLI stops waiting, clears wait state, and shows a clear **“Cancelled by user.”** (or similar) outcome without killing the whole session.

**Constraints (from project rules):** No Cypress for transient wait UI; cover cancellation with **Vitest** (`cli/tests/…`) and, if needed, thin harness tests that drive `AbortController` + mocked fetch.

---

#### Research: generated TypeScript client (`@hey-api/client-fetch`)

- Stack: `openapi-ts.config.ts` uses `@hey-api/client-fetch` + `@hey-api/sdk`; output in `packages/generated/doughnut-backend-api/`.
- **Per-request cancellation is already supported at the type level:** `client/types.gen.ts` defines `Config` as extending `Omit<RequestInit, 'body' | 'headers' | 'method'>`, and `RequestOptions` / SDK `Options<>` inherit that. **`signal?: AbortSignal`** can be passed on each generated call, e.g. `RecallsController.recalling({ query: { … }, signal })`, and flows into `new Request(url, requestInit)` in `client/client.gen.ts`.
- **Runtime:** Native `fetch` honours `AbortSignal`; abort typically surfaces as **`AbortError`** (handled explicitly in generated `client.gen.ts` catch path).
- **SSE helper:** `core/serverSentEvents.gen.ts` already uses `options.signal` for abort-aware streaming.
- **No OpenAPI/codegen change required** for basic cancellation—only call sites need to **thread `signal`** from an `AbortController` into the SDK options.

(Context7 `/hey-api/openapi-ts`: docs emphasise Fetch client config and interceptors; cancellation follows standard **Fetch + AbortSignal** semantics.)

---

#### Research: GET vs requests that change state

| Kind | Abort on the client | Server / product semantics |
|------|---------------------|----------------------------|
| **Safe reads (GET)** | Stops waiting; connection may close. | Usually no persistent effect; server may still finish computing—acceptable. |
| **Writes (POST/PATCH/DELETE, etc.)** | Stops waiting; **does not guarantee** the server did not apply the change. | HTTP has no universal “undo”; a request may be **fully processed after** the client aborts. User copy should be honest: cancellation means **“stopped waiting”**, not **“operation was rolled back”**. |
| **Multi-step flows** (e.g. contest → regenerate) | Abort may fire **between** two calls; first step might succeed while the second never runs. | Higher **cohesion risk**; either document, avoid exposing cancel mid-sequence, or treat the flow as one logical unit (single backend operation or compensating behaviour)—product decision. |

**Recommendation:** One UX for “cancel wait” is fine; optionally **vary messaging** by tagging wait lines (read vs write vs multi-step) so history text can warn when a partial write was possible.

---

#### Research: CLI wiring gaps today

- **`runInteractiveFetchWait`** (`interactiveFetchWait.ts`) only toggles visibility; the work is an opaque `() => Promise<T>` with **no `signal`**.
- **`withBackendClient`** (`accessToken.ts`) uses `try/catch` that **rethrows a generic “service not available” for any failure**, which would **mask `AbortError`** and break cancel UX until abort is detected and rethrown (or a dedicated error type is used).
- **TTY** (`ttyAdapter.ts`): `keypress` handler is `async` and **awaits** `processInput` / nested `runInteractiveFetchWait`. While waiting, **further keypresses can still be delivered** (overlapping async handlers are possible). Phase 3 should **define** behaviour: e.g. **Esc** during `getInteractiveFetchWaitLine() != null` → `abort()`, and optionally **ignore or queue** other keys during wait to avoid double-submit (existing risk worth noting).

---

#### Proposed sub-phases (user value first)

1. **Phase 3a — Abort plumbing + API threading**  
   - Introduce an `AbortSignal` (or `AbortController`) for each interactive wait, stored in a small module alongside `activeWaitLine` (same cohesion as current global wait state).  
   - Extend `runInteractiveFetchWait` to accept optional `signal` / controller, or create an inner controller and expose `abort()` to the TTY.  
   - Thread `signal` into **all** SDK calls used inside wait paths (`recall.ts`, `interactive.ts`, `accessToken.ts`, `gmail.ts`, etc.).  
   - Fix **`withBackendClient`** (and any similar wrappers) to **preserve abort**: if `error` is abort (`name === 'AbortError'` or `DOMException` with `ABORT_ERR`), rethrow without replacing the message.  
   - **Tests:** Vitest with mocked client or stub `fetch` that respects `signal`; assert wait teardown and error type/message.

2. **Phase 3b — TTY: Esc cancels wait**  
   - When `getInteractiveFetchWaitLine()` is set, **Esc** calls `abort()` on the active controller (and does not trigger recall exit / other Esc handlers).  
   - Repaint after abort; append **“Cancelled by user.”** (or aligned copy) to history like other cancellations.  
   - **Tests:** Focused tty/keypress tests if a harness exists; otherwise unit-test the abort registration + `processInput` outcome with a fake `OutputAdapter`.

3. **Phase 3c (optional) — Copy and classification**  
   - Tag wait lines or call sites as `read` | `write` | `multiStep` for tailored history messages where misleading “cancel = safe” would be wrong.  
   - Only if product wants the extra clarity; otherwise keep a single neutral line.

**Order:** 3a before 3b (signal must reach `fetch` before keybinding matters). 3c is optional polish.

---

#### Open questions (before implementation)

- **Key binding:** Esc only vs Esc + Ctrl-C behaviour during wait (today Ctrl-C exits process).  
- **Concurrent keypress:** Whether to ignore non-Esc keys during wait to reduce duplicate submits.  
- **Non-interactive `-c`:** Still out of scope unless product asks (see table below).

## Out of scope (unless product asks)

- Non-interactive `-c` wait UX  
- Cypress E2E for transient wait lines  
- Progress percentage; cancellation deferred to phase 3  

---

**Status:** Phases 1–2 done. Phase 3 researched and split into 3a–3c in this doc; not implemented yet.
