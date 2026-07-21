# Phase 3: Cancel Extraction Preview Generation - Context

**Gathered:** 2026-07-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Adopt the shared cancelable blocking contract for AI extraction-preview generation in note refinement (`REFN-03`, `REFN-04`). While preview generation is pending, the same global blocker shows Cancel. Cancelling aborts only that browser request, keeps all selected refinement-layout items, leaves the user before the preview (for Extract-from-layout), shows no API error, and lets the user retry from the preserved selection. This phase does not cancel final extracted-note creation, does not redesign Cancel visuals, and does not claim server-side AI stop.

</domain>

<decisions>
## Implementation Decisions

### Post-cancel landing surface
- **D-01:** Cancelling preview generation started from the layout **Extract** action never opens or shows the extraction-preview panel. The user remains on the layout selection view.
- **D-02:** All selected refinement-layout checkboxes / `selectedItemIds` stay unchanged. Do not clear selection, reload layout, emit `contentUpdated`, or mutate note content on cancel.
- **D-03:** Cancellation remains silent (Phase 1 D-11): no error toast, no `createError` banner, no success handling, no navigation, no Cancelling/Cancelled interstitial.

### Retry affordance from preserved selection
- **D-04:** After cancel from Extract-from-layout, retry is the existing **Extract** button (still enabled because selection is preserved). Do not add a layout-panel empty-state or dedicated "Ask AI to retry" control for this path (unlike Phase 2 layout cancel, where items were empty).
- **D-05:** A cancelled outcome must not apply preview payload fields. Late/partial responses must not revive success handling (Phase 1 D-05).

### Cancel during in-preview regenerate
- **D-06:** If the user cancels while regenerating via **Ask AI to retry** on an already-visible preview, remain on the extraction-preview panel with the **prior** preview content unchanged. Do not wipe fields, set `createError`, force Back to layout, or apply the cancelled response.
- **D-07:** Existing unsaved-edit confirm before retry stays as today; cancel does not add a new confirmation.

### Blocker message and Cancel adoption
- **D-08:** Keep the existing blocker message `AI is generating preview...`. Do not rename it for symmetry with layout unless a later UI pass requires it.
- **D-09:** Adopt Phase 1 Cancel visuals and interaction unchanged (same adoption stance as Phase 2): accessible Cancel on the global blocker only; no Escape/backdrop cancel; no Cancel redesign.
- **D-10:** Opt in with the shared literal `{ blockUi: true, cancelable: true }` overload on the preview request path. Do not invent a cancelable `runWithBlockingApiLoading`. Leave create-note and other non-preview refinement blockers noncancelable in this phase.

### Carried forward (do not re-open)
- Phase 1 D-01–D-13 remain binding for outcome shape, cleanup, overlapping blockers, and silent feedback.
- Phase 2 proved caller adoption for layout; Phase 3 reuses that pattern for preview only.
- Client-only abort; no server-cooperative stop claim.

### Claude's Discretion
- Exact refactor of `fetchExtractionPreview` / `runExtractionPreview` / `runWithBlockingApiLoading` into a single cancelable `apiCallWithLoading` call, provided D-01–D-10 and `.cursor/rules/frontend-api.mdc` hold.
- Test file placement and helpers under `frontend/tests/components/recall/`, mirroring Phase 2 cancel specs.
- Whether preview API-error paths that today force-show the preview panel stay unchanged (out of Phase 3 cancel scope) as long as cancel never uses that path.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope and requirements
- `.planning/PROJECT.md` — Core value, progressive client-side cancellation, safety constraints.
- `.planning/REQUIREMENTS.md` — `REFN-03`, `REFN-04` (this phase); `REFN-05` / `COHE-02` remain Phase 4.
- `.planning/ROADMAP.md` — Phase 3 goal, success criteria, dependency on Phase 2.
- `.cursor/rules/planning.mdc` — Behavior phase: one observable behavior, stop-safe.
- `.cursor/rules/gsd-coexistence.mdc` — Local delivery overlays for GSD execute.

### Prior phase locks
- `.planning/phases/01-shared-cancellation-contract/01-CONTEXT.md` — D-01–D-13 shared contract.
- `.planning/phases/01-shared-cancellation-contract/01-UI-SPEC.md` — Cancel visual/interaction contract (adopt only).
- `.planning/phases/02-cancel-refinement-layout-generation/02-UI-SPEC.md` — Prior adoption stance; preview cancel explicitly out of Phase 2.
- `.planning/phases/02-cancel-refinement-layout-generation/02-RESEARCH.md` — Layout adoption research; extract-preview left for Phase 3.

### Frontend API contract
- `.cursor/rules/frontend-api.mdc` — Cancelable overload, forbid cancelable mutations and cancelable `runWithBlockingApiLoading`.

No external ADRs or design specs beyond the above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/managedApi/clientSetup.ts` — Phase 1 cancelable `apiCallWithLoading` overload returning `{ status: "completed" | "cancelled" }`.
- `frontend/src/components/commons/LoadingModal.vue` — Conditional Cancel already projects from selected blocker identity.
- `frontend/src/components/recall/NoteRefinement.vue` — `fetchExtractionPreview` / `runExtractionPreview` / `openExtractionPreview` / `retryExtractionPreview`; layout selection via `useRefinementLayoutSelection`.
- Phase 2 layout cancel Vitest patterns in `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts` and `noteRefinement*TestSupport` helpers (`createDeferredGate`, `GlobalApiLoadingModal`).

### Established Patterns
- Layout generation already uses `{ blockUi: true, cancelable: true, message: "AI is generating layout..." }` with status narrowing.
- Preview today uses noncancelable `runWithBlockingApiLoading(..., "AI is generating preview...")` wrapping a non-`blockUi` `apiCallWithLoading` — must migrate to the cancelable single-call overload (not a cancelable composite helper).
- Preview panel already has **Ask AI to retry** and **Back**; layout panel has **Extract** gated on selection — reuse those for post-cancel retry rather than inventing a third surface.
- On API failure, current code can still show the preview panel with `createError`; cancel must not share that path.

### Integration Points
- Change only the extraction-preview generation path in `NoteRefinement.vue` (and focused tests). Leave `createExtractedNote`, remove/export paths, and Phase 2 layout cancel behavior untouched.
- Pass wrapper-owned `AbortSignal` into `AiController.extractNotePreview` the same way layout passes it into `generateRefinementSuggestions`.
- Refine-note dialog shell stays open (parent `AssimilationSettings`); cancel must not close it.

</code_context>

<specifics>
## Specific Ideas

- Mirror Phase 2's vertical slice: cancelable blocking call + status branch + focused Vitest cancel/retry coverage — without Phase 2's empty-layout panel, because selection preservation keeps Extract available.
- Treat "before the preview" as: Extract-from-layout cancel never enters preview; in-preview regenerate cancel keeps the prior preview rather than inventing a wipe.
- Message string stays `AI is generating preview...` for continuity with today's blocker copy.

</specifics>

<deferred>
## Deferred Ideas

- Cancel final extracted-note creation / keep it noncancelable — Phase 4 (`REFN-05`).
- Full blocker classification audit — Phase 4 (`COHE-02`).
- Server-cooperative AI cancellation — `SERV-01` / v2.
- Redesigning Cancel visuals, Escape/backdrop cancel, or spinner chrome.

</deferred>

---

*Phase: 3-Cancel Extraction Preview Generation*
*Context gathered: 2026-07-21*
