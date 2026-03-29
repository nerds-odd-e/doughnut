# CLI: Esc cancels in-flight API work (plan only)

**Status:** Planning — do not treat as an implementation spec.  
**Goal:** Restore the user-visible behavior: **while the interactive CLI is waiting on a slow backend/network call (spinner / blocking stage), the user can press Esc to cancel**; the session shows a clear outcome and stays usable. **No new E2E scenarios** — cover with **Vitest** using the preferred **observable** style (`runInteractive` / `ink-testing-library` + mock TTY, or existing render patterns per `.cursor/rules/cli.mdc`).

**Guidance:** `.cursor/rules/planning.mdc`, `.cursor/rules/cli.mdc`, `ongoing/cli-architecture-roadmap.md` — especially **§3.2 async + cancellation**, **§4 stage isolation**, **§11.3 keyboard ownership**. **Do not** resurrect the pre-removal wiring; use it only to remember **behavior and copy**.

---

## Git history (inspiration only)

Relevant removals on **2026-03-28** (and the stack they sat on):

| Commit     | What to remember (behavior / intent) |
|-----------|--------------------------------------|
| `66bf4d049` | Removed `fetchAbort.ts`, `interactiveFetchWait.ts`, and large `terminalLayout` / `listDisplay` chunks. Prior **`runInteractiveFetchWait`**: module-level “active wait” + **`AbortController`**, Esc called **`cancelInteractiveFetchWaitFor(output)`** tied to an **`OutputAdapter`**. User-visible abort mapping lived next to **`AbortError`**. |
| `2ab49528d` | Earlier step removing interactive fetch-wait from **`interactive.ts`** / **`ShellSessionRoot`** / **`interactiveApp`** (non–Ink path). |
| Older chain (`babdc38e3`, `5274df4c4`, `099de3448`, …) | Iteration on fetch-wait + Esc; confirms the **product expectation**: cancellable waits during token / recall / similar flows. |

**Prior shape to avoid:** **Global mutable state** (`activeWaitLine`, `escBinding`) keyed by **`OutputAdapter`**, plus adapter callbacks (**`onInteractiveFetchWaitChanged`**) to force redraws. That couples cancellation to **transport/layout** layers and makes **ownership of Esc** implicit and hard to test. **Replace with** stage-local lifecycle: **React mount/unmount + one obvious input owner** for the spinner sub-stage.

**What already exists today (reuse):**

- **`userVisibleSlashCommandError`** — maps **`AbortError`** to **`Cancelled.`** (same family as picker **`PICKER_ABORTED_MESSAGE`** in `AccessTokenLabelPickerStage`).
- **Backend helpers** — e.g. **`removeAccessTokenCompletely(label, signal?)`**, **`addAccessToken(..., signal?)`**, **`doughnutSdkOptions(signal)`**; Gmail stack already accepts **`AbortSignal`** in **`addGmailAccount`**, **`getLastEmailSubject`**, **`waitForCallback`**, etc.
- **Stage key forwarding** — **`InteractiveCliApp`** root **`useInput`** + **`SetStageKeyHandlerContext`**; **`AccessTokenLabelPickerStage`** registers **`useLayoutEffect`** / fallback **`useInput({ isActive })`** — **same pattern should own Esc during fetch-wait**, not a new global registry.

**Gap:** **`AsyncAssistantFetchStage`** starts work in **`useEffect`** with a **`cancelled`** flag on unmount only; it does **not** **`abort()`** the request, and **Esc does nothing**. Interactive wrappers **`runAddGmailInteractiveAssistantMessage`** / **`runLastEmailInteractiveAssistantMessage`** pass **`undefined`** signal into functions that already support cancellation.

---

## Cross-cutting constraints

1. **Keyboard:** While a fetch stage is mounted, **Esc = cancel in-flight work** (not slash-suggestion dismiss — **`MainInteractivePrompt`** is not mounted then). Use a **stable `useCallback`** for the handler registered on the stage key forwarder (see **cli.mdc** Ink notes).
2. **Cancellation semantics:** Prefer **real** propagation (**`AbortSignal`** into **`fetch` / generated client`**) where the stack already supports it; for work that cannot abort (pure CPU), document **best-effort** (still clear **`Cancelled.`** after Esc if the task respects signal, or avoid claiming cancel where impossible).
3. **Tests:** **No new Cypress features.** Extend **`cli/tests/`** only: drive **`runInteractive`** (or the project’s **`renderApp`** pattern), assert **stdout / last frame** for spinner → Esc → **`Cancelled.`** (or settled without duplicate assistant lines). **No `setTimeout(…, N)`** waits — **`waitForFrames`** / turn-capped loops per **cli.mdc**.
4. **Cohesion:** Prefer **one** cancellable async stage primitive (likely **`AsyncAssistantFetchStage`**) over duplicating **`AbortController`** in every parent stage.
5. **Deploy gate:** Per **planning.mdc**, commit/deploy between phases if the team expects it.

---

## TDD workflow (every phase)

Follow **Test-driven workflow** in **`.cursor/rules/planning.mdc`**:

1. **Add or extend the test(s)** for the phase’s **observable** outcome (Vitest: **`runInteractive`** / **`renderApp`** + **`waitForFrames`**, no wall-clock sleeps).
2. **Run** the relevant test file or **`pnpm cli:test`** (with **Nix** prefix from **general.mdc**); confirm the test **fails**.
3. Confirm failure is **for the right reason** (missing Esc handling, signal not passed, etc.) — not env or typo.
4. **Implement** the **smallest** change that makes the test(s) pass.
5. **Refactor** with tests green.

**Discipline:** While driving a phase, keep **at most one intentionally failing test** (the one you are implementing toward), per **planning.mdc**.

---

## Phase 1 — Cancellable fetch stage (Esc + `AbortSignal` contract)

**User outcome:** During any flow that uses the shared async spinner stage, **Esc** aborts the controller, the in-flight **`Promise`** rejects with **`AbortError`**, and the user sees **`Cancelled.`** (via existing **`userVisibleSlashCommandError`**), then returns to the normal prompt.

### 1.1 — Red: tests first

- Add a **minimal interactive test** (preferred): tiny **harness** or **test-only stage** under **`SetStageKeyHandlerContext.Provider`** (same wiring as **`InteractiveCliApp`**), **`runAssistantMessage(signal)`** that **does not resolve** until aborted (or rejects on abort), send **Esc**, assert **`onSettled('Cancelled.')`** and/or visible frame text.
- Add a second case **only if needed** in the same file: **unmount** while pending → same single outcome, no duplicate assistant lines.
- Run tests → **fail** (Esc ignored, no **`AbortController`**, or wrong callback shape).

### 1.2 — Green: implementation

- Change **`runAssistantMessage`** to **`(signal: AbortSignal) => Promise<string>`** so call sites must thread **`signal`**.
- In **`AsyncAssistantFetchStage`**: **`AbortController`** per effect, **`abort()` on unmount**, **Esc** via stage key registration (**`useLayoutEffect`** + **`SetStageKeyHandlerContext`**, fallback **`useInput(..., { isActive })`** like **`AccessTokenLabelPickerStage`**), **stable `useCallback`**.
- One **`onSettled`** per attempt: guard **abort vs unmount** races (extend existing **`cancelled`** pattern).

### 1.3 — Refactor

- Adjust call sites to the new **`(signal) => …`** signature (stub implementations that ignore **`signal`** temporarily only if split across commits — prefer **one** commit that compiles).

**Phase-complete when:** tests green; **no** new E2E.

---

## Phase 2 — Doughnut API call sites behind the spinner

**User outcome:** Cancelling during **revoke/remove** (and any other **Doughnut** HTTP work using this stage) **actually cancels the HTTP request** where the client honors **`signal`** (generated SDK / **`fetch`**).

### 2.1 — Red: tests first

- Add an **observable** test: e.g. drive **remove-completely** (or a minimal stage + mocked **`UserController.revokeToken`**) with a **deliberately slow** or **pending** request, **Esc**, assert **request cancellation** *or* a **user-visible** outcome that **only** occurs when **`signal`** is honored (prefer **stub `fetch` / mock client** that records **`signal.aborted`** after Esc if stable).
- If proving HTTP abort in Vitest is too flaky, add **one** black-box test that still fails until **`signal`** is passed from the stage into **`removeAccessTokenCompletely(..., signal)`** — only if that remains **observable** (e.g. assistant **`Cancelled.`** only when underlying call respects abort); avoid tests that only assert “callback received signal” without user-visible effect unless that API is the **deliberate** contract (**planning.mdc**).

### 2.2 — Green: implementation

- Update **`RemoveAccessTokenCompletelyPickerStage`** (and any other **`AsyncAssistantFetchStage`** + Doughnut callers) to pass **`signal`** into **`removeAccessTokenCompletely`** / **`doughnutSdkOptions(signal)`**.

### 2.3 — Refactor

- Keep **one** pattern across **`AsyncAssistantFetchStage`** usages.

---

## Phase 3 — Gmail interactive entry points

**User outcome:** **`/add gmail`** and **`/last email`** (spinner stages) respect **Esc**: OAuth wait / **`fetch`** paths receive **`AbortSignal`** (Gmail code already supports **`signal`** in **`addGmailAccount`**, **`getLastEmailSubject`**, etc.).

### 3.1 — Red: tests first

- **`/last email`**: add **`runInteractive`** (or existing app harness) test — slow **`fetch`** (or mock) + **Esc** → assistant shows **`Cancelled.`**; run → **fail** until wrappers pass **`signal`**.
- **`/add gmail`**: add a test that matches **CI stability** (e.g. **`DOUGHNUT_NO_BROWSER`**, mock **`GOOGLE_BASE_URL`** if the suite already does); if a full OAuth flow is untestable, add a **public**-boundary test (e.g. **`addGmailAccount(..., signal)`** rejects **`AbortError`** when aborted during **`waitForCallback`**) **only** if that function is the stable contract — otherwise one interactive case + manual checklist note.

### 3.2 — Green: implementation

- Change **`runAddGmailInteractiveAssistantMessage`** and **`runLastEmailInteractiveAssistantMessage`** to accept **`AbortSignal`** from the stage and forward to **`addGmailAccount`**, **`getLastEmailSubject`**.
- Verify **`waitForCallback`** / **`server.close()`** on abort so ports do not leak.

### 3.3 — Refactor

- Align naming/types with Phase 1 **`runAssistantMessage(signal)`** usage.

---

## Phase 4 — Optional: current stage band parity with old “fetch wait line”

**User outcome (only if product still wants it):** The **first line** of the **current prompt** uses the **stage band** styling (see **cli.mdc** vocabulary) for long waits, not only the **`Spinner`** line — closer to the old **`INTERACTIVE_FETCH_WAIT_LINES`** UX.

**Trigger:** If designers / recall revival / access-token flows need **consistent** “Recalling”-style bands for **network wait**, add a **small** presentational wrapper around **`AsyncAssistantFetchStage`** (props: **`stageIndicator`**, **`spinnerLabel`**) using existing **`renderer`** / **`Text`** patterns — **without** reintroducing **`OutputAdapter`**-driven layout state.

### TDD if executed

1. **Red:** Extend an existing interactive test to expect **band / indicator** copy in output; run → fail.
2. **Green:** Add wrapper / props and render band.
3. **Refactor** layout only.

**If spinner-only is enough:** **Skip this phase** and delete this section from the plan when closing the work.

---

## Phase 5 — Audit remaining long async interactive commands

**User outcome:** No silent “long wait without Esc” regressions for **interactive** Doughnut operations that **should** match the roadmap.

### TDD if gaps found

1. **Red:** For each gap, add **one** failing **`runInteractive`** (or equivalent) test describing the missing cancel behavior.
2. **Green:** Wire cancellation or document **non-cancellable** and adjust test to assert documented behavior.

- Review **`command.run`** paths from **`InteractiveCliApp`** (non-stage **`Promise.resolve(run)`**) and staged flows.
- **No new E2E** unless product changes that rule.

---

## Explicit non-goals (this revival)

- Reintroducing **`interactiveFetchWait.ts`**, **`fetchAbort.ts`**, or **global** Esc binding keyed by **`OutputAdapter`**.
- Rewiring **`processInput`** / non-Ink **`interactive.ts`** paths (removed in **`2ab49528d`**) unless the product explicitly revives that entry point.
- New **Cucumber** coverage (per request).

---

## Summary

| Phase | User-visible slice                          | TDD order (each phase)                                                                 | Primary mechanism                                      |
|-------|---------------------------------------------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| 1     | Esc cancels spinner → **`Cancelled.`**      | **Test** (Esc + harness) → fail → **impl** **`AbortController`** + key forward → green | Stage-local cancellation, **`(signal) => Promise`**    |
| 2     | Doughnut HTTP honors cancel                 | **Test** (abort observable) → fail → **impl** pass **`signal`** → green                 | **`removeAccessTokenCompletely`**, SDK **`signal`**     |
| 3     | Gmail spinners honor cancel                 | **Test** (last email / add gmail scope) → fail → **impl** wrappers forward **`signal`** | **`run*InteractiveAssistantMessage`**                  |
| 4     | (Optional) stage band + spinner             | **Test** expects band copy → fail → **impl** → green                                   | Presentational wrapper only                            |
| 5     | No orphan long waits                        | **Test** per gap → fail → fix or document → green                                       | Audit **`run` / stages**                               |

When implementation finishes, **update or remove** this document per **planning.mdc** phase discipline.
