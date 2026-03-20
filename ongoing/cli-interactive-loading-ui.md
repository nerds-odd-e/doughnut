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
| Command wiring | `interactive.ts` (param commands with `usesInteractiveFetchWait` + `waitLine`, recall paths, gmail/email); token list remove-completely in `ttyAdapter.ts` |

## Phase 3.1 (done) — recall load cancel

| Piece | Location |
|------|-----------|
| Recall load + `AbortSignal` | Same as other waits: `runInteractiveFetchWait` + `INTERACTIVE_FETCH_WAIT_LINES.recallNext` (`interactive.ts`) |
| TTY Esc (historical) | `cancelInteractiveRecallLoadFor` → **`cancelInteractiveFetchWaitFor`** (`ttyAdapter.ts`) |
| Recall API | `recall.ts`: `recallNext(due, abortSignal?)` on recalling / askAQuestion / showMemoryTracker |
| Abort vs generic error | `fetchAbort.ts`: `userAbortError`, `isFetchAbortedByCaller`, `CLI_USER_ABORTED_WAIT_MESSAGE`; `withBackendClient` rethrows abort |
| User copy | `interactive.ts`: `logCancelledOrError`; failed recall **load** also calls `endRecallSessionAfterFailedRecallLoad` |

## Phase 3.2 (done) — slow recall load knob

| Piece | Location |
|------|-----------|
| Env + abortable delay | `recall.ts`: `DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS`, `awaitCliTestRecallLoadDelayIfConfigured` (only when `recallNext` gets an `AbortSignal`) |
| Tests | `recall.test.ts`: `DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS` describe + `processInput /recall` (real `recallNext`, mocked API) |

## Phase 3.3 (done) — Esc cancels all interactive fetch waits

| Piece | Location |
|------|-----------|
| Shared abort + wait chrome | `interactiveFetchWait.ts`: `runInteractiveFetchWait` + `InteractiveFetchWaitTask<T>`; `escBoundAbort` ties Esc to the active `OutputAdapter` |
| TTY wiring | `ttyAdapter.ts` imports `runInteractiveFetchWait` / `getInteractiveFetchWaitLine` directly (not via `TTYDeps`) |
| TTY Esc | `cancelInteractiveFetchWaitFor(ttyOutput)` |
| User copy | `CLI_USER_ABORTED_WAIT_MESSAGE` (`fetchAbort.ts`); `logCancelledOrError` + `endRecallSessionAfterFailedRecallLoad` for recall **load** failures |
| Backend / network | `accessToken.ts`, `recall.ts` (`recallStatus`, `contestAndRegenerate`), `gmail.ts` — optional `AbortSignal` on I/O |
| Token list remove-completely | `ttyAdapter.ts`: `signal` into `removeAccessTokenCompletely`; abort → `CLI_USER_ABORTED_WAIT_MESSAGE` in history |

**Tests:** `interactiveFetchWait.test.ts` — `/recall-status` cancel via `cancelInteractiveFetchWaitFor`; `interactive.test.ts` — **“TTY recall-status wait — Esc cancels”**; existing recall-load / contest assertions updated for `AbortSignal`.

## Future phases

| Phase | Scope |
|-------|--------|
| 3.x | **3.1–3.3** done. **3.4** optional: copy / classification (`planning.mdc`). |

**Planning note:** Sub-phases 3.1–3.3 below are **done**. Optional **3.4** only adjusts copy when “cancel” could imply rollback (see GET vs write table). E2E-shaped proof for waits uses Vitest + `runTTY` / stdin simulation (not Cypress for transient lines); see `.cursor/rules/cli.mdc`.

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

#### Research: CLI wiring (current)

- **`runInteractiveFetchWait`** — wait chrome + `AbortController` registered as `escBoundAbort`. Recall load uses the same helper with `INTERACTIVE_FETCH_WAIT_LINES.recallNext`.
- **`withBackendClient`** — maps failures to “service unavailable” except **`isFetchAbortedByCaller`** (`fetchAbort.ts`).
- **TTY** — Esc calls **`cancelInteractiveFetchWaitFor`**; overlapping async keypress handlers during `await processInput` remain a product choice for later.

---

#### Proposed sub-phases (scenario-first, ordered by value)

Split by **who can cancel what**, not by “plumbing layer then TTY layer.” The **first** phase must already show **Esc → cancelled wait + clear history outcome** for at least one real wait (minimal internal surface area is fine; **avoid** a whole generic framework before the first scenario works).

1. **Phase 3.1 — Cancel one high-value read wait (end-to-end)** ✅  
   - **User scenario:** During **recall load** (`INTERACTIVE_FETCH_WAIT_LINES.recallNext`), user presses **Esc** → wait chrome clears, recall session ends, **“Cancelled by user.”** is logged (same line as token-list cancel).  
   - **Implementation (first slice):** dedicated recall-load registration, then unified in **3.3** with `runInteractiveFetchWait` + `recallNext(due, abortSignal?)` + **`isFetchAbortedByCaller`**.  
   - **Tests:** `interactiveFetchWait.test.ts`; `interactive.test.ts` **“TTY recall load wait — Esc cancels”**; `recall.test.ts` / `accessToken.test.ts` edges.

2. **Phase 3.2 — CLI testability: simulated slow recall load** ✅  
   - **Implemented:** `DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS` (cap 60000) in `recall.ts` when `recallNext` receives an `AbortSignal`.  
   - **Tests:** `recall.test.ts` (delay + abort / delay completion / no signal; `processInput /recall` with real `recallNext`).  
   - **Docs:** `CLAUDE.md`, `.cursor/rules/cli.mdc`.

3. **Phase 3.3 — Cancel applies to remaining interactive waits** ✅  
   - **User scenario:** Same Esc cancel works for other `runInteractiveFetchWait` entry points (contest, recall-status, token flows, Gmail, etc.).  
   - **Implementation:** Single `AbortController` in `runInteractiveFetchWait`; `signal` threaded through access-token, recall (`recallStatus`, `contestAndRegenerate`), Gmail, token-list remove-completely.  
   - **Tests:** `interactiveFetchWait.test.ts` `/recall-status` abort; `interactive.test.ts` **TTY recall-status wait — Esc cancels**.

4. **Phase 3.4 (optional) — Copy / classification**  
   - **User scenario:** Where writes or multi-step flows make “cancel” misleading, history or prompt copy reflects **stopped waiting** vs implied rollback (see GET vs write table above).  
   - **Tests:** Unit or a small E2E assertion on the **final message string** if product adds distinct copy.

**Big refactor:** If threading `signal` through call sites **requires** a large structural change before **any** user-visible cancel, planning allows **one** phase dedicated to that structure — but that phase should still exit with **one** thin user-visible cancel (smallest scenario) or be agreed as an explicit exception with the team.

**Interim behavior:** Allowed (e.g. Esc only on one command first); remove when a later phase replaces with full coverage.

---

#### Open questions (before implementation)

- **Key binding:** Esc only vs Esc + Ctrl-C behaviour during wait (today Ctrl-C exits process).  
- **Concurrent keypress:** Whether to ignore non-Esc keys during wait to reduce duplicate submits.  
- **Non-interactive `-c`:** Still out of scope unless product asks (see table below).

## Out of scope (unless product asks)

- Non-interactive `-c` wait UX  
- Cypress E2E for transient wait lines  
- Progress percentage  
- Copy refinement for misleading “cancel” on writes (phase **3.4**)  

---

**Status:** Phases 1–3.3 done. **3.4** (optional copy / classification) pending.
