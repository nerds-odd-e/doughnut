# Phase 1: Shared Cancellation Contract - Context

**Gathered:** 2026-07-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Introduce the shared frontend contract for opt-in request abortion, per-operation loading cleanup, an explicit normal cancelled outcome, and conditional global-modal control. This is a stop-safe Structure phase: existing callers retain their current behavior and expose no Cancel control, and the new structure prepares only Phase 2's initial note-refinement layout-generation behavior. It does not adopt cancellation at a product call site or claim that browser abort stops server work.

</domain>

<decisions>
## Implementation Decisions

### Cancellation outcome
- **D-01:** An opted-in call returns an explicit two-way discriminated result: `{ status: "completed", result }` or `{ status: "cancelled" }`. Ordinary API failures continue through the existing SDK-shaped error handling instead of becoming a third outcome.
- **D-02:** The cancelled variant carries status only. It does not expose a reason, browser `AbortError`, signal details, or partial result.
- **D-03:** The TypeScript contract must require callers to branch on the status before completed data is accessible, preventing accidental success handling after cancellation.

### Cancellation timing and cleanup
- **D-04:** Activating Cancel immediately aborts and removes exactly that operation's loading state from every shared indicator. Other loading states remain untouched.
- **D-05:** Once the shared layer accepts a cancel action, cancellation wins over a nearly simultaneous completion. Late browser results are consumed safely and cannot revive success handling.
- **D-06:** The outer call resolves promptly with `{ status: "cancelled" }` without waiting for server work or the underlying request to settle.

### Overlapping blockers
- **D-07:** The visible Cancel control targets only the currently displayed blocking state, which follows the existing most-recent-blocker selection.
- **D-08:** After the displayed blocker is cancelled, any older active blocker is revealed immediately with its own message and its own cancelability. There is no close-and-reopen transition.
- **D-09:** A noncancelable displayed blocker never exposes Cancel for a hidden cancelable blocker.
- **D-10:** Cancellation is idempotent and bound to the displayed loading state's identity. A repeated or stale action must not retarget a replacement blocker.

### Cancellation feedback
- **D-11:** Shared cancellation is silent: no generic toast, banner, or `Cancelling`/`Cancelled` interstitial. Immediate disappearance or revelation of the next blocker is the acknowledgement.
- **D-12:** Callers own only required domain-local post-cancel state, such as preserving inputs or exposing retry. The shared layer does not accept or emit caller-specific cancellation messages.
- **D-13:** Routine user cancellation does not produce console logging or telemetry in this phase. The typed outcome and focused tests are the diagnostic contract.

### the agent's Discretion
- Exact type, function, and property names, provided they enforce D-01 through D-03 without changing existing callers.
- Internal abort-race coordination and promise plumbing, provided D-04 through D-06 are deterministic and do not leak unhandled rejections.
- Test decomposition and helper placement, provided nested and concurrent behavior is covered through existing high-level frontend testing patterns.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope and delivery grammar
- `.planning/PROJECT.md` — Core value, progressive client-side scope, safety constraints, and deferred server cooperation.
- `.planning/REQUIREMENTS.md` — `COHE-01` ownership boundary and the later behavior requirements this structure must support without implementing early.
- `.planning/ROADMAP.md` — Phase 1 Structure boundary, success criteria, and immediate dependency on Phase 2.
- `.cursor/rules/planning.mdc` — Stop-safe Behavior-versus-Structure grammar and one-observable-behavior constraint.
- `.cursor/rules/gsd-coexistence.mdc` — Required local planning and delivery overlays for GSD phases.

No external specifications, ADRs, or design documents are referenced for this phase.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/managedApi/clientSetup.ts`: `apiCallWithLoading` already owns shared loading lifecycle and SDK error handling; `runWithBlockingApiLoading` is the existing convenience path for composite blocking work.
- `frontend/src/managedApi/ApiStatusHandler.ts`: every loading state already has a unique identity, and `currentBlockingApiState` already selects the most recently started blocking state.
- `frontend/src/components/commons/LoadingModal.vue`: the global blocker currently renders only the spinner and operation message, so conditional control can remain additive.

### Established Patterns
- `apiCallWithLoading` starts state synchronously and removes that exact state in `finally`, preserving nested and concurrent calls.
- `DoughnutApp.vue` derives one visible blocking state from the shared state list and mounts a single global `LoadingModal`.
- Frontend API calls return SDK field results and use centralized toast behavior; generated SDK files are never edited by hand.
- Existing loading tests in `frontend/tests/managedApi/clientSetup.loading.spec.ts` cover synchronous visibility, nesting, concurrency, and cleanup and are the primary regression seam.

### Integration Points
- Extend the shared loading options/state so an opted-in operation can carry state-bound cancellation without altering default callers.
- Connect the currently selected blocking state from `frontend/src/DoughnutApp.vue` to conditional control in `LoadingModal.vue` while leaving noncancelable states visually unchanged.
- Keep all abort ownership and outcome classification in `frontend/src/managedApi/`; later caller phases should receive only the discriminated result and define their own local post-cancel state.

</code_context>

<specifics>
## Specific Ideas

- Treat disappearance of the cancelled blocker—or immediate replacement by an older blocker—as sufficient user acknowledgement.
- Bind the action to the displayed state's unique identity so rapid repeated input cannot accidentally cancel the next operation.
- Preserve the existing newest-blocker-first display model rather than inventing a cancellation queue or multi-operation picker.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within the Phase 1 boundary. Server-side cooperative cancellation, mutation-safe cancellation, and product call-site adoption remain in their already assigned later requirements/phases.

</deferred>

---

*Phase: 1-Shared Cancellation Contract*
*Context gathered: 2026-07-21*
