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

## Phase 3.1 (done) — recall load cancel

| Piece | Location |
|------|-----------|
| Recall load + `AbortSignal` | `interactiveFetchWait.ts`: `runRecallNextFetchWithWaitUi`, `inFlightRecallNextFetch` keyed by `OutputAdapter`; `RecallNextBackendFetch<T>` |
| Other waits (no cancel yet) | `runInteractiveFetchWait` — `fn: () => Promise` only |
| TTY Esc | `ttyAdapter.ts`: `cancelInFlightRecallNextFetchFor(ttyOutput)` returns whether Esc cancelled an in-flight recall-next fetch |
| Recall API | `recall.ts`: `recallNext(due, signal?)` passes `signal` into recalling / askAQuestion / showMemoryTracker |
| Abort vs generic error | `fetchAbort.ts`: `isFetchAbortedByCaller`; `withBackendClient` rethrows that case |
| User copy | `interactive.ts`: `handleRecallNextFetchError` → `Cancelled by user.` when fetch was aborted |

## Future phases

| Phase | Scope |
|-------|--------|
| 3.x | **3.1** done (recall cancel). **3.2** next: CLI slow / testability for exploratory + stable tests. **3.3–3.4**: widen cancel + copy (`planning.mdc`). |

### Phase 3 (planned) — cancellable interactive fetch wait

**Goal:** While the TTY shows the interactive fetch wait chrome (grey box, blue wait line), the user can **cancel the in-flight work** (e.g. Esc) so the CLI stops waiting, clears wait state, and shows a clear **“Cancelled by user.”** (or similar) outcome without killing the whole session.

**Alignment with `planning.mdc`:** Phases are **scenario-first** (user outcome per phase), not layer-first plumbing-then-UI. Each phase ships a **visible** slice; avoid a phase that is **only** refactor with no user-facing behavior. **Test-driven workflow** and **at most one intentionally failing test** while driving a change apply when implementing.

**Testing (planning vs CLI):** Planning expects **E2E-shaped** tests for the **main user behavior** per phase, and unit tests for edges; **normal paths should not be justified by unit tests alone**. For this feature, the **cli** rule still applies: **do not assert transient loading lines** in Cypress (flaky). Satisfy planning by making E2E **post-cancel / post-wait** assertions stable (e.g. history contains **“Cancelled by user.”**, live region restored), or use an **equivalent** full-path test (e.g. Vitest driving `runTTY` / stdin simulation) if the team treats that as the phase’s end-to-end proof. **Extend** `interactiveFetchWait.test.ts` / related tests where behavior already has coverage; avoid duplicate harness code.

**Phase discipline:** Before the next phase: cleanup, **deploy gate** (commit/push/CD per team), update this doc.

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

#### Research: CLI wiring (post–3.1 snapshot)

- **`runInteractiveFetchWait`** — wait chrome only, no abort. **`runRecallNextFetchWithWaitUi`** — recall-next load only, owns `AbortController` + `inFlightRecallNextFetch` per `OutputAdapter`.
- **`withBackendClient`** — maps failures to “service unavailable” except **`isFetchAbortedByCaller`** (`fetchAbort.ts`).
- **TTY** — Esc calls **`cancelInFlightRecallNextFetchFor`**; overlapping async keypress handlers during `await processInput` remain a product choice for later.

---

#### Proposed sub-phases (scenario-first, ordered by value)

Split by **who can cancel what**, not by “plumbing layer then TTY layer.” The **first** phase must already show **Esc → cancelled wait + clear history outcome** for at least one real wait (minimal internal surface area is fine; **avoid** a whole generic framework before the first scenario works).

1. **Phase 3.1 — Cancel one high-value read wait (end-to-end)** ✅  
   - **User scenario:** During **recall load** (`INTERACTIVE_FETCH_WAIT_LINES.recallNext`), user presses **Esc** → wait chrome clears, recall session ends, **“Cancelled by user.”** is logged (same line as token-list cancel).  
   - **Implementation:** **`runRecallNextFetchWithWaitUi`** + **`cancelInFlightRecallNextFetchFor(output)`**; generic waits stay on **`runInteractiveFetchWait`**. **`recallNext(due, signal?)`**, **`isFetchAbortedByCaller`** / **`handleRecallNextFetchError`**.  
   - **Tests:** `interactiveFetchWait.test.ts` (`cancelInFlightRecallNextFetchFor(out)`); `interactive.test.ts` **“TTY recall load wait — Esc cancels”**; `recall.test.ts` / `accessToken.test.ts` edges.

2. **Phase 3.2 — CLI testability: simulated slow recall load**  
   - **Ordering:** Comes **before** 3.3 so exploratory runs and automation for **later** cancel phases do not depend on network luck.  
   - **Why CLI-side (not backend):** Keeps production API and deploy surface unchanged; avoids Mountebank/backend test-only endpoints; any environment (local, CI, pointed at staging) can enable a **predictable pause** so the TTY shows **“Loading recall questions”** long enough to manually press Esc or to drive **stable post-conditions** (e.g. **“Cancelled by user.”**) without asserting animated ellipsis.  
   - **Why it’s reasonable:** Phase 3.1 cancel is already proven with mocks; this phase closes the gap for **real transport + real TTY** exploratory runs and optional Cypress/Vitest “full path” checks.  
   - **Safety / cohesion:** Single knob (prefer **`DOUGHNUT_CLI_*` env** and/or a **dev-only flag** documented in `CLAUDE.md` / `cli.mdc`), **off by default**, **no effect in bundled release** unless explicitly set (team choice: strip in release build or document “never set in prod”). Implement in **one place** (e.g. delay helper used only from **`recallNext`** or **`runWithDefaultBackendClient`** when the knob is set) so behaviour stays obvious.  
   - **User scenario (when enabled):** Starting **`/recall`** (and optionally **`continueRecallSession`** recall load) waits an extra **configured duration** before the first recalling call returns (or before the SDK call runs — product choice), so cancel-during-load can be exercised reliably.  
   - **Tests:** TDD: with knob on in test env, **Vitest** asserts Esc → **“Cancelled by user.”** without mocking `recallNext` internals (or a thin wrapper); optionally **one** Cypress `@interactiveCLI` scenario **only** if it asserts **stable** text after cancel, not loading dots. **Do not** add Cypress that races on transient wait lines (`cli.mdc`).  
   - **Out of scope for 3.2:** Slowing arbitrary commands (that is **3.3+** if needed); changing backend timeouts; non-interactive `-c`.

3. **Phase 3.3 — Cancel applies to remaining interactive waits**  
   - **User scenario:** Same Esc cancel works for other `runInteractiveFetchWait` entry points (contest, recall-status, token flows, Gmail, etc.).  
   - **Implementation:** Generalize threading `signal` for each path; reuse one pattern (extend existing helpers; **generalize only after** 3.1 shows repetition per planning). Use **3.2** slow mode for exploratory / stable checks where helpful.  
   - **Tests:** Extend the same E2E-shaped approach for **one additional** representative slow path if coverage would otherwise duplicate; remaining paths covered by unit tests **only where** they exercise non–happy-path or distinct branching.

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
- Broader cancellation (phase **3.3+**) beyond recall load wait  

---

**Status:** Phases 1–2 done. **Phase 3.1 implemented** (recall load cancel). **3.2** (CLI slow / testability) next, not implemented. **3.3–3.4** pending after that.
