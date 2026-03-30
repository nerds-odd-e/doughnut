# CLI: Esc cancels in-flight API work (plan only)

**Status:** Planning — do not treat as an implementation spec.  
**Goal:** Restore the user-visible behavior: **while the interactive CLI is waiting on a slow backend/network call (spinner / blocking stage), the user can press Esc to cancel**; the session shows a clear outcome and stays usable. **No new E2E scenarios** — cover with **Vitest** using the preferred **observable** style (`runInteractive` / `ink-testing-library` + mock TTY, or existing render patterns per `.cursor/rules/cli.mdc`).

**Phasing (`.cursor/rules/planning.mdc`):** **One user-visible behavior per phase.** Each phase ships a slice you can **verify from outside** (transcript / last frame / assistant line text, exit from the happy path), then **commit/deploy** before the next phase if the team uses that gate. **Do not** land large bundles (e.g. `InteractiveCliApp` + three slash commands + recall + test helpers in one PR).

**Guidance:** `.cursor/rules/planning.mdc`, `.cursor/rules/cli.mdc`, `ongoing/cli-architecture-roadmap.md` — especially **§3.2 async + cancellation**, **§4 stage isolation**, **§11.3 keyboard ownership**. **Do not** resurrect the pre-removal wiring; use it only to remember **behavior and copy**.

---

## Git history (inspiration only)

Relevant removals on **2026-03-28** (and the stack they sat on):

| Commit     | What to remember (behavior / intent) |
|-----------|--------------------------------------|
| `66bf4d049` | Removed `fetchAbort.ts`, `interactiveFetchWait.ts`, and large `terminalLayout` / `listDisplay` chunks. Prior **`runInteractiveFetchWait`**: module-level “active wait” + **`AbortController`**, Esc called **`cancelInteractiveFetchWaitFor(output)`** tied to an **`OutputAdapter`**. User-visible abort mapping lived next to **`AbortError`**. |
| `2ab49528d` | Earlier step removing interactive fetch-wait from **`interactive.ts`** / **`ShellSessionRoot`** / **`interactiveApp`** (non–Ink path). |
| Older chain (`babdc38e3`, `5274df4c4`, `099de3448`, …) | Iteration on fetch-wait + Esc; confirms the **product expectation**: cancellable waits during token / recall / similar flows. |

**Prior shape to avoid:** **Global mutable state** (`activeWaitLine`, `escBinding`) keyed by **`OutputAdapter`**, plus adapter callbacks (**`onInteractiveFetchWaitChanged`**) to force redraws. **Replace with** stage-local lifecycle: **React mount/unmount + one obvious input owner** for the spinner sub-stage.

**What already exists today (reuse):**

- **`userVisibleSlashCommandError`** — maps **`AbortError`** to **`Cancelled.`**
- **Backend helpers** — **`removeAccessTokenCompletely(label, signal?)`**, **`addAccessToken(..., signal?)`**, **`doughnutSdkOptions(signal)`**; Gmail stack already accepts **`AbortSignal`** in **`addGmailAccount`**, **`getLastEmailSubject`**, **`waitForCallback`**, etc.
- **Stage key forwarding** — **`InteractiveCliApp`** root **`useInput`** + **`SetStageKeyHandlerContext`**.

**Gap (before this work):** **`AsyncAssistantFetchStage`** must own Esc + **`AbortSignal`**; some call sites still omit **`signal`**; **`Promise.resolve(run)`** paths and custom stages (e.g. recall load) can still block without Esc.

---

## Cross-cutting constraints

1. **Keyboard:** While a fetch stage is mounted, **Esc = cancel in-flight work**. **Stable `useCallback`** on the stage key forwarder (see **cli.mdc**).
2. **Cancellation:** Prefer **`AbortSignal`** into **`fetch` / generated client`** where supported; document **non-cancellable** paths explicitly if any remain.
3. **Tests:** **No new Cypress.** **`waitForFrames`** / turn-capped loops — **no `setTimeout(…, N)`** for Ink readiness (see **cli.mdc**). Hung SDK mocks should **reject on `signal` abort** so Esc produces **`Cancelled.`** in tests (not a forever-pending promise with no abort path).
4. **Cohesion:** Prefer **`AsyncAssistantFetchStage`** for “one assistant string from async work” over ad hoc **`AbortController`** copies — except where the UI is **not** that shape (e.g. recall just-review load vs y/n).
5. **Chained slash input in tests:** After an async stage unmounts, **`MainInteractivePrompt`**’s **`useInput`** may attach a tick later; use an **observable** probe (see **`renderInkWhenCommandLineReady`**) or a shared helper in **`cli/tests/inkTestHelpers.ts`** **when a phase’s test needs it** — keep that helper in the **same phase** that first requires chained input, or the immediately following test-only refactor phase if you must split.

---

## TDD workflow (every phase)

Per **`.cursor/rules/planning.mdc`**: add/extend **one** failing **observable** test for the phase → run → implement **smallest** fix → green → refactor. **At most one intentionally failing test** while driving a phase.

---

## Phase 1 — Cancellable fetch stage (Esc + `AbortSignal` contract)

**User-visible outcome:** On **`AsyncAssistantFetchStage`**, **Esc** aborts work; user sees **`Cancelled.`**; prompt returns.

**External verification:** Vitest (harness + **`SetStageKeyHandlerContext`** or full app): spinner visible → Esc → **`Cancelled.`** (and optional unmount case).

**Implementation slice:** **`runAssistantMessage(signal: AbortSignal) => Promise<string>`**; **`AbortController`** per effect; Esc on stage key forwarder; **`onSettled`** once per attempt.

---

## Phase 2 — Doughnut HTTP behind that spinner (picker revoke path)

**User-visible outcome:** Cancelling during **revoke** (picker → **`AsyncAssistantFetchStage`**) cancels or abandons the in-flight client call as the SDK allows.

**External verification:** **`removeAccessTokenCompletelyAbort`-style** test: hung **`DELETE`** (or equivalent) + Esc → **`Cancelled.`**

**Implementation slice:** Pass stage **`signal`** into **`removeAccessTokenCompletely`** / **`doughnutSdkOptions(signal)`** from **`RemoveAccessTokenCompletelyPickerStage`** only.

---

## Phase 3 — Gmail spinner entry points

**User-visible outcome:** **`/add gmail`** and **`/last email`** honor Esc during their spinner waits.

**External verification:** Vitest already targeted at **`InteractiveCliApp.addGmail.test.tsx`** (or equivalent): Esc → **`Cancelled.`**; mocks honor **`AbortSignal`**.

**Implementation slice:** Forward **`signal`** from **`AsyncAssistantFetchStage`** into **`runAddGmailInteractiveAssistantMessage`** / **`runLastEmailInteractiveAssistantMessage`**; no OAuth/port leaks on abort.

---

## Phase 4 — `/recall-status`: spinner + Esc (no direct `run()` async)

**Done:** `RecallStatusStage` + `AsyncAssistantFetchStage` in [`cli/src/commands/recallStatus.tsx`](../cli/src/commands/recallStatus.tsx); Vitest [`cli/tests/InteractiveCliApp.recallStatus.test.tsx`](../cli/tests/InteractiveCliApp.recallStatus.test.tsx).

**User-visible outcome:** **`/recall-status`** shows a **loading** state; **Esc** during the fetch shows **`Cancelled.`**; success still shows the due-count line as today.

**External verification:** **`InteractiveCliApp`**: **`/recall-status`** + **`RecallsController.recalling`** mock that **hangs until `signal` abort** + Esc → transcript **`Cancelled.`** (separate test case: fast mock still shows **`N notes…`**).

**Implementation slice:** **`RecallStatusStage`** (or equivalent) wrapping **`AsyncAssistantFetchStage`** calling **`recallStatus(signal)`**; slash command uses **`stageComponent`** and **drops** the bare **`async run()`** path for this command only. **No** `InteractiveCliApp` generic flags in this phase.

---

## Phase 5 — `InteractiveCliApp`: mount stage when inline argument + `/add-access-token` only

**User-visible outcome:** **`/add-access-token <token>`** goes through the **cancellable spinner**; **Esc** during verify → **`Cancelled.`**; missing token still shows usage (unchanged product rule).

**External verification:** One **new or extended** interactive test: inline token + mock **`getTokenInfo`** that rejects on **`signal` abort** + Esc → **`Cancelled.`**; existing happy path still passes. If tests chain another slash command after add, introduce **`waitForMainInteractivePromptAfterAsyncStage`** (or equivalent probe) **in this phase** only if needed.

**Implementation slice:** **`InteractiveSlashCommand`**: optional **`useStageWhenArgumentProvided`**; **`InteractiveSlashCommandStageProps`**: optional **`initialSlashArgument`**; **`InteractiveCliApp`**: mount **`stageComponent`** when that flag is set and the user provided a non-empty argument; **`AddAccessTokenStage`** + remove **`run`** from **`addAccessTokenSlashCommand`**. **Do not** change **`/remove-access-token-completely`** yet.

---

## Phase 6 — `/remove-access-token-completely <label>` inline → same spinner as picker

**User-visible outcome:** When the user types **`/remove-access-token-completely <label>`** (no picker), **Esc** during revoke shows **`Cancelled.`** (same as picker path).

**External verification:** **`InteractiveCliApp`** + temp config + hung **`DELETE`** + inline label + Esc → **`Cancelled.`**

**Implementation slice:** Set **`useStageWhenArgumentProvided`** on **`removeAccessTokenCompletelySlashCommand`**; **remove** the **`run()`** branch for inline revoke; **`RemoveAccessTokenCompletelyPickerStage`** reads **`initialSlashArgument`** (trimmed) to skip picker when set. Depends on **Phase 5** mount rule.

---

## Phase 7 — `/recall` just-review: Esc during load and during mark-as-recalled

**User-visible outcome:** **Esc** while **“Loading recall…”** or while **mark-as-recalled** is in flight → **`Cancelled.`**; y/n idle (no request in flight) can keep **Esc** as no-op or document in this phase.

**External verification:** Two tests (can be one file): hung **`recalling`** with **abort-aware** mock + Esc; after **`y`**, hung **`markAsRecalled`** with **`signal`** + Esc → **`Cancelled.`**

**Implementation slice:** **`loadRecallJustReviewPayload(signal?)`**, **`markJustReviewRecalled(..., signal?)`** threading **`doughnutSdkOptions(signal)`**; **`RecallJustReviewStage`**: **`AbortController`** for load + Esc; separate controller (or same pattern) for submit while **`submittingRef`**.

---

## Phase 8 — Audit remainder; document intentional non-cancellable paths

**User-visible outcome:** No silent “long network wait without Esc” for **interactive** flows that should match the roadmap; anything else is **named** (e.g. help, exit, local-only **`/remove-access-token`** **`run`**, pickers with no network).

**External verification:** **At most one** new test **per** newly found gap; if no gap, **update this plan** with the audit list only.

**Implementation slice:** Read-only pass over **`interactiveSlashCommands`** + **`InteractiveCliApp`** **`run`** vs **`stageComponent`**; fix or document only what Phase 8 finds.

---

## Explicit non-goals (this revival)

- Reintroducing **`interactiveFetchWait.ts`**, **`fetchAbort.ts`**, or **global** Esc binding keyed by **`OutputAdapter`**.
- Rewiring **`processInput`** / non-Ink **`interactive.ts`** unless product revives that entry point.
- New **Cucumber** coverage (unless product changes that rule).
- **Stage band / “Recalling”-style strip** for generic fetch spinners — **out of scope** for this document (spinner-only UX is enough).

---

## Summary

| Phase | User-visible slice                         | Primary verification                          |
|-------|---------------------------------------------|-----------------------------------------------|
| 1     | Esc on **`AsyncAssistantFetchStage`**       | Harness / app frame + **`Cancelled.`**        |
| 2     | Revoke picker passes **`signal`**           | Hung HTTP + Esc                               |
| 3     | Gmail spinners                              | **`InteractiveCliApp.addGmail`**-style tests  |
| 4     | **`/recall-status`** spinner + Esc (done)   | `InteractiveCliApp.recallStatus.test.tsx`     |
| 5     | **`/add-access-token`** inline + mount rule | Interactive + **`getTokenInfo`** abort mock   |
| 6     | Inline **`remove-access-token-completely`** | Interactive + hung DELETE + Esc               |
| 7     | **`/recall`** load + mark cancel           | Two interactive cases                         |
| 8     | Audit + documented exceptions               | Tests only if new gaps                        |

When implementation finishes, **update or remove** this document per **planning.mdc** phase discipline.
