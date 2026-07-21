# Phase 4: Enforce Safe Blocking Boundaries - Context

**Gathered:** 2026-07-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Lock the final extracted-note creation path as an intentionally noncancelable shared blocker (`REFN-05`), and complete the cohesion audit (`COHE-02`): every existing whole-UI blocker and every long-running note-refinement AI request receives an explicit cancellation classification, every safe in-scope request already uses the shared cancelable contract, and no in-scope caller duplicates generic abort / loading-cleanup / cancellation-error-suppression logic. This phase does not offer Cancel on create-note, does not adopt cancellation outside note refinement, and does not claim server-side stop or mutation-safe cancel.

</domain>

<decisions>
## Implementation Decisions

### Create-note noncancelable boundary (REFN-05)
- **D-01:** While final extracted-note creation is pending, the global blocker shows `AI is creating note...` and **must not** expose Cancel. This is intentional safety, not a missing opt-in.
- **D-02:** Keep the existing create success path unchanged: one successful completion still focuses/navigates via `focusNoteRealm` as today. Cancellation is not introduced; do not add retry-after-abort affordances for create.
- **D-03:** Preserve the Phase 3 regression stance: a pending create-note blocker must fail any assertion that Cancel is present. Strengthen or promote that coverage as the REFN-05 proof, not a product behavior change.
- **D-04:** Do not opt `createExtractedNote` into `{ cancelable: true }`. Do not invent a cancelable `runWithBlockingApiLoading`. Client-only abort remains unsafe for this transactional mutation (duplicate note / double original-note update risk).

### Cohesion audit and adoption scope (COHE-02)
- **D-05:** Produce an explicit inventory of every current whole-UI blocker and every long-running note-refinement AI request, each labeled **cancelable**, **intentionally noncancelable**, or **nonblocking**.
- **D-06:** In-scope cancelable adopters for this milestone remain exactly the two note-refinement AI reads already shipped: layout generation (`AI is generating layout...`) and extraction-preview generation (`AI is generating preview...`). Both must continue using the shared `{ blockUi: true, cancelable: true }` overload.
- **D-07:** Classify all other whole-UI blockers as **intentionally noncancelable** for v1 (including create-note, remove-content, assimilation, book-layout mutations/apply, note delete, relationship finalize, and book-layout AI suggest). Do **not** migrate them to Cancel in this phase — broader adoption is `ADPT-01` / v2 after per-domain post-cancel outcomes are defined.
- **D-08:** Classify thin-bar / non-`blockUi` note-refinement helpers (e.g. export request fetches without whole-UI blocking) as **nonblocking** — they are out of Cancel UX scope.
- **D-09:** Audit must confirm no in-scope caller reimplements AbortController ownership, AbortError-name matching, loading-state pop-by-id, or cancellation toast suppression outside `frontend/src/managedApi/`. Remove any such duplication found; do not add parallel helpers.

### Classification artifact and verification
- **D-10:** Persist the classification inventory in `.cursor/rules/frontend-api.mdc` (the living cancelable-contract home), not a planning-only diary. Include the cancelable allowlist and the intentional noncancelable examples that matter for agents.
- **D-11:** Verification is frontend Vitest / existing high-level entry points: create-note Cancel-absent; layout + preview remain cancelable; optionally a lightweight guard that production `cancelable: true` appears only at the allowed note-refinement call sites. No full E2E suite required unless a plan task explicitly needs a targeted Cypress spec.

### Carried forward (do not re-open)
- Phase 1 D-01–D-13 remain binding for outcome shape, cleanup, overlapping blockers, and silent feedback.
- Phase 2 / Phase 3 caller adoption patterns for layout and preview remain binding.
- Project Key Decision: extracted-note creation stays noncancelable in v1; browser abort does not promise server stop.
- `.cursor/rules/frontend-api.mdc` already forbids cancelable mutations and cancelable `runWithBlockingApiLoading`.

### Claude's Discretion
- Whether to leave `createExtractedNote` on `runWithBlockingApiLoading(..., "AI is creating note...")` or flatten to a single noncancelable `apiCallWithLoading(..., { blockUi: true, message })` — either is fine if Cancel stays absent and success navigation is unchanged.
- Exact wording/layout of the frontend-api.mdc inventory table, provided D-05–D-10 are satisfied.
- Test file placement (extend Phase 3 create-note edge vs dedicated cohesion/create-boundary specs), provided REFN-05 and COHE-02 are observable.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope and requirements
- `.planning/PROJECT.md` — Core value; keep extracted-note creation noncancelable; opt-in cancellation only.
- `.planning/REQUIREMENTS.md` — `REFN-05`, `COHE-02` (this phase); `ADPT-01` / `SERV-*` remain deferred.
- `.planning/ROADMAP.md` — Phase 4 goal, success criteria, dependency on Phase 3.
- `.cursor/rules/planning.mdc` — Behavior phase: one observable behavior, stop-safe.
- `.cursor/rules/gsd-coexistence.mdc` — Local delivery overlays for GSD execute.

### Prior phase locks
- `.planning/phases/01-shared-cancellation-contract/01-CONTEXT.md` — Shared contract D-01–D-13.
- `.planning/phases/03-cancel-extraction-preview-generation/03-CONTEXT.md` — Preview adoption; create-note deferred to Phase 4; D-10 noncancelable create stance.
- `.planning/phases/03-cancel-extraction-preview-generation/03-03-SUMMARY.md` — Existing create-note Cancel-absent edge and frontend-api forbid wording.

### Frontend API contract
- `.cursor/rules/frontend-api.mdc` — Cancelable overload, mutation forbid, noncancelable `runWithBlockingApiLoading`; extend with Phase 4 classification inventory.

No external ADRs or design specs beyond the above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/managedApi/clientSetup.ts` — Shared cancelable / noncancelable `apiCallWithLoading` and noncancelable `runWithBlockingApiLoading`.
- `frontend/src/components/commons/LoadingModal.vue` — Conditional Cancel projected from selected blocker identity.
- `frontend/src/components/recall/NoteRefinement.vue` — Layout + preview already `cancelable: true`; `createExtractedNote` and `removeSelectedLayoutItems` use noncancelable blocking; export helpers are nonblocking SDK calls.
- Phase 3 edge coverage in `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` — includes create-note Cancel-absent while `AI is creating note...`.

### Established Patterns
- Only two production `cancelable: true` call sites exist today (layout + preview in `NoteRefinement.vue`).
- Other whole-UI blockers use `{ blockUi: true }` or `runWithBlockingApiLoading` without cancelable: assimilation, next-note load, book-layout mutations/AI reorganize, note delete, relationship finalize, notebook attached-book section.
- frontend-api.mdc already documents create-note as a forbidden cancelable example.

### Integration Points
- Primary product touch: keep `createExtractedNote` noncancelable and document/audit all blockers; update `.cursor/rules/frontend-api.mdc` inventory.
- Regression/tests under `frontend/tests/components/recall/` and optionally a small managedApi / static allowlist check — no backend or generated-SDK changes expected.
- Do not change Phase 2/3 cancel UX for layout/preview.

</code_context>

<specifics>
## Specific Ideas

- Treat REFN-05 as an enforcement + proof gate on behavior that is largely already correct after Phase 3, paired with the COHE-02 inventory so the milestone closes with an explicit map rather than implied safety.
- Prefer documenting classifications where implementers already look (`frontend-api.mdc`) over a disposable planning note.
- Book-layout AI suggest may look "safe to cancel" later — leave it intentionally noncancelable now and route to `ADPT-01`.

</specifics>

<deferred>
## Deferred Ideas

- Cancel additional safe long-running operations outside note refinement — `ADPT-01` / v2.
- Server-cooperative AI cancellation — `SERV-01`.
- Mutation-safe / idempotent cancellation for transactional writes — `SERV-02`.
- Redesigning Cancel visuals, Escape/backdrop cancel, or spinner chrome.

</deferred>

---

*Phase: 4-Enforce Safe Blocking Boundaries*
*Context gathered: 2026-07-21*
