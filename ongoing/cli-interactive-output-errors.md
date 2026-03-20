# CLI interactive: history placement, API errors, and user cancel

Informal plan — **Phases 1–2 done** (Phases 3–5 pending).

## Problems (from product + code)

1. **`/recall-status` result looks like it lives in the “input box” area** instead of the chat history strip the E2E parser calls “history output”.
2. **API / network failures** (and similar) can show **“0 notes to recall today”** instead of an error — same symptom as “server down”.
3. **Esc during interactive fetch-wait** can show **“0 notes to recall today”** instead of a clear “cancelled by user” message — behaviour matches (2) because the root cause is the same.
4. **Desired UX**: real errors → **history** with **dedicated style** (e.g. red). User-initiated cancel → **system message** in **history** (e.g. grey + italic), **same treatment for all** such cancellations.
5. **Opaque failures**: after errors surface (Phase 1), the user still sees **one generic backend message** for distinct situations — **service unreachable**, **no token configured**, **token invalid / expired**, **403**, etc. — and cannot tell what to do next without reading logs.

Note: the real command is **`/recall-status`** (hyphen), not `/recall_status`.

---

## Root causes (verified in code)

### A. Generated API client defaults to `throwOnError: false`

`packages/generated/doughnut-backend-api/client/client.gen.ts`: on non-OK HTTP or on fetch failure (including **AbortError** when `throwOnError` is false), the client returns a **result object** with `error` set and **no `data`**, instead of throwing.

`cli/src/recall.ts` → `recallStatus()` does:

- `const count = result.data?.toRepeat?.length ?? 0`

So **missing `data` becomes 0** → **“0 notes to recall today”**. That covers:

- real HTTP errors,
- aborted requests (Esc) when the abort is expressed as a **returned** `error` rather than an exception.

`withBackendClient` in `cli/src/accessToken.ts` only wraps **thrown** errors; it does not fix **silent `{ error }` returns**.

### B. TTY `OutputAdapter.log` / `logError` double-write

`cli/src/adapters/ttyAdapter.ts`: `log` and `logError` both:

1. **`process.stdout.write`** the text immediately, and  
2. append to **`collectedOutputLines`** for the next **`chatHistory` push** + **`drawBox()`** / **`renderFullDisplay`**.

The immediate writes land in the PTY transcript **outside** the same layout pass as the bordered live region. `e2e_test/step_definitions/cliSectionParser.ts` partitions output using the **last** green separator and **last** `┌`/`└` box — orphan lines often fall into **`currentPromptLines`** (between separator and box) or the tail, so they look like **Current prompt / chrome** rather than **history**, and E2E “history output” can disagree with what a human sees.

This is **systematic**: any command that uses `output.log` during `processInput` after `clearLiveRegionForRepaint` is affected (not only `/recall-status`).

### C. Cancel vs error messaging today

`cli/src/interactive.ts` → `logCancelledOrError` already branches **abort** vs **other** (`cli/src/fetchAbort.ts`), but both paths use **`output.log` / `output.logError`** with **no history “kind”**, and (until A is fixed) abort may never surface as an exception. Token list completion uses **`endTokenListSelection`** which **does** push structured history — a second, inconsistent path.

---

## Cohesive direction

Treat three layers together:

| Layer | What |
|--------|------|
| **Data plane** | Every CLI call to `doughnut-api` controllers must **surface failure as throw** (or assert unwrap), so `recallStatus`, `recallNext`, token flows, etc. never interpret `error` as empty data. Prefer **`throwOnError: true`** on each controller options object (spread with `signal` where used) in **`cli/src/recall.ts`** and **`cli/src/accessToken.ts`** — only places that import `*Controller` from `doughnut-api`. |
| **TTY paint plane** | **`log` / `logError` must not print message body to stdout** in `ttyAdapter`; only update **`collectedOutputLines`** / **`chatHistory`** and let **`drawBox` → `renderFullDisplay`** paint scrollback. Eliminates duplicate/misplaced lines in the PTY stream (fixes (1) and stabilizes E2E sections). |
| **Semantics plane** | Extend **`ChatHistory`** / **`ChatHistoryOutputEntry`** (`cli/src/types.ts`) with a **line kind**: at least **`normal` | `error` | `system`**. **`renderer.renderFullDisplay`** applies SGR (reuse **`GREY`** / **`ansi.ts`** conventions; error e.g. red, system e.g. grey+italic). **`OutputAdapter`**: add **`logSystem`** (or equivalent) so **`logCancelledOrError`** and token-list cancel use **system** styling; **`logError`** maps to **error** styling in TTY. **`interactive.ts` `defaultOutput`** (non-TTY) can stay plain or use minimal prefixes — keep simple. |

Gmail / raw `http` in `cli/src/gmail.ts` is out of scope for `throwOnError`; only ensure **interactive fetch-wait** paths that already use **`logCancelledOrError`** still classify abort vs error correctly once throws work.

---

## Suggested phases (value-first, each test-complete)

### Phase 1 — Stop swallowing API/abort outcomes ✅

- **`throwOnError: true`** on all **`UserController`**, **`RecallsController`**, **`MemoryTrackerController`**, **`RecallPromptController`** calls under **`runWithDefaultBackendClient`** / **`withBackendClient`** (`cli/src/recall.ts` **`backendSdkOpts`**, `cli/src/accessToken.ts`).
- **`recall.test.ts`**: expectations include **`throwOnError: true`**; **`contestAndRegenerate`** regenerate failure is **`mockRejectedValue`** + wrapped service error; **`recallStatus`** adds rejection + **`AbortError`** tests.
- **`accessToken.test.ts`**: invalid-token / **`generateToken`** failure cases use **`mockRejectedValue`** and expect **`withBackendClient`** wrap (no silent `{ error }` return shapes).
- **`interactive.test.ts`**: unchanged (mocks recall at module level, not SDK `{ error }` returns).
- **Regression**: Esc during **`/recall-status`** remains covered by **`interactiveFetchWait.test.ts`** (mock **`recallStatus`** rejects with **`userAbortError()`**); real path now uses SDK **`throwOnError`** + abort so **`recallStatus`** rejects instead of reporting **0 notes**.

### Phase 2 — TTY: history-only emission for command output ✅

- Change **`ttyOutput.log` / `logError`** (and any **`writeError`** paths that duplicate content) so **message lines are not written** in the adapter; **`drawBox`** after **`processInput`** is the source of truth for visible history.
- Re-run **`pnpm cli:test`**; run **one** relevant Cypress spec (e.g. `cli_recall.feature`) to confirm **`history output`** assertions still pass.
- Update **`.cursor/rules/cli.mdc`** briefly: command results are painted only via history + live region redraw.

### Phase 3 — Error vs system styling in history

- Implement **`kind`** on output history entries + renderer styles.
- Wire **`logCancelledOrError`**: abort → **`logSystem`**, else **`logError`** (error kind).
- **`endTokenListSelection`** (token list Esc / cancel): use the same **system** helper text as fetch-wait cancel.
- **E2E / Vitest**: prefer **Vitest** on renderer or adapter for ANSI presence; extend **`cliSectionParser`** or add a small **raw history** assertion only if E2E must see grey italic / red (avoid flaky loading lines per existing rule).

### Phase 4 — Cancel audit (same semantics + style)

Inventory and align:

| Location | Today | Target |
|----------|--------|--------|
| Esc during **`runInteractiveFetchWait`** | abort controller | system message (after Phases 1–3) |
| Token list: non-Enter exit | **`endTokenListSelection(CLI_USER_ABORTED_WAIT_MESSAGE)`** | same copy, **system** kind |
| **`/contest`** / other **`runInteractiveFetchWait`** callers | **`logCancelledOrError`** | unchanged logic, **system** vs **error** styling |
| Esc in recall substate / MCQ stop flow | mostly **no** history line | **optional**: one system line (“Recall cancelled” / “Cancelled”) for consistency — **confirm with product**; if out of scope, document as follow-up |

### Phase 5 — Distinct user-facing messages (connectivity vs auth)

**Goal:** When a command fails for a **known class** of reason, the **message in history** (or stderr for non-TTY if we extend there) should **name the situation** so the user knows whether to fix network, run `doughnut login`, refresh token, or contact support — not a single undifferentiated “backend error”.

**Classify at least:**

| Situation | Example signals | User-facing intent |
|-----------|-----------------|-------------------|
| **Service not available** | `ECONNREFUSED`, `ENOTFOUND`, timeouts, fetch failures without HTTP response | Backend unreachable or DNS wrong; check URL / VPN / server. |
| **No access token** | Missing stored token when one is required for the operation | Configure or run login / token flow. |
| **Access token invalid** | HTTP **401**, refresh failure, token rejected by API | Re-authenticate or regenerate token. |
| **No permission** (optional same phase if small) | HTTP **403** | Distinct from “bad token” where the API distinguishes it. |

**Implementation sketch:** Centralize mapping in one place (e.g. extend **`withBackendClient`** / a small **`cli/src/backendErrors.ts`** helper) that inspects **`cause`**, **`response.status`**, and existing token helpers in **`accessToken.ts`**. Reuse **`throwOnError`** thrown shapes from the generated client where they expose status.

**Tests:** Unit tests in **`accessToken.test.ts`** / **`recall.test.ts`** (or a focused **`backendErrors.test.ts`**) with **mocked** network errors, 401, 403, and “no token” preconditions — assert **exact or stable substring** messages per class. E2E only if an existing CLI feature already asserts error text; avoid flaky network E2E.

**Dependency:** Best after Phase 1 (failures are thrown, not silent). Can ship **after** Phase 2–3 so messages land in **styled** history, or **with** Phase 3 if error **kind** and **copy** are done together — prefer **one** phase that owns **copy + classification**; styling stays Phase 3 unless merged deliberately.

---

## Files likely touched (when implementing)

- `cli/src/recall.ts` — `throwOnError`, no silent `data` fallback on failure  
- `cli/src/accessToken.ts` — `throwOnError` on `UserController.*`; token presence / **`withBackendClient`** error mapping (Phase 5)  
- `cli/src/backendErrors.ts` (or equivalent) — Phase 5: classify fetch vs HTTP status vs missing token → message  
- `cli/src/adapters/ttyAdapter.ts` — log routing, possibly token-list **`writeError`** alignment  
- `cli/src/types.ts` — `OutputAdapter`, `ChatHistoryOutputEntry`  
- `cli/src/renderer.ts` — `renderFullDisplay` output line styling  
- `cli/src/interactive.ts` — `defaultOutput`, `logCancelledOrError`  
- `cli/tests/*.test.ts` — mocks + regression  
- `e2e_test/step_definitions/cliSectionParser.ts` — only if new sections/steps needed  
- `.cursor/rules/cli.mdc` — terminology  

---

## Explicit non-goals (this pass)

- Changing **OpenAPI** / regenerating client defaults globally (CLI-local `throwOnError` is enough).  
- Redesigning **non-interactive** (`-c`) output (unless **`defaultOutput`** must mirror kinds for tests).  
- Full **E2E** coverage of transient loading lines (already discouraged in **`cli.mdc`**).

---

## Open questions before implementation

1. **Exact copy** for system cancel: keep **`Cancelled by user.`** or standardize to **“Command cancelled by user.”** everywhere (token list + fetch-wait)?  
2. **Esc exiting recall** (without fetch-wait): add a system line or leave silent?  
3. **Server error body**: Phase 5 covers **classification + short user copy**; optional follow-up: append **safe** snippet from JSON **`error`** body when present (still no secrets).  
4. **Exact strings** for Phase 5: agree copy for “service not available” vs “cannot reach server” etc. once and reuse in tests.

When these are decided, update this doc and implement phase-by-phase per **`planning.mdc`** (tests first where practical).
