# Phase 2: assimilate-as-commissioned - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults; audit trail in `02-DISCUSSION-LOG.md`)

<domain>
## Phase Boundary

User can create a **commissioned memory tracker** from a caret dropdown next to the note-level **Assimilate** control during assimilation. Assimilation settings then show that commissioned tracker. Ordinary trackers for the same note may already exist or be created later (coexistence). Properties do not get a commissioned option in this phase.

One observable behavior: assimilate note as commissioned (TRK-01, TRK-02).

</domain>

<decisions>
## Implementation Decisions

### Create semantics
- **D-01:** "Assimilate as commissioned" creates **only** a note-level `MemoryTrackerType.COMMISSIONED` tracker. It does **not** also create UNDERSTANDING/SPELLING in the same action. Ordinary assimilate remains a separate click. — **Reversibility:** reversible — create path flag only; no schema change beyond existing type column
- **D-02:** The menu item is available whenever the note has **no** note-level COMMISSIONED tracker yet — whether or not UNDERSTANDING/SPELLING already exist (coexistence entry from either order)
- **D-03:** After only COMMISSIONED exists, the primary **Assimilate** button must still be enabled for ordinary intake (frontend disable logic must ignore COMMISSIONED the same way Phase 1 backend create already does)

### Caret / menu UX
- **D-04:** Note-level control is a **split affordance**: unchanged primary **Assimilate** (and Skip recall / Revive as today) plus a **caret** that opens a dropdown with a single item — **Assimilate as commissioned** (ADR 0001 wording). Not offered on property rows
- **D-05:** When a note-level COMMISSIONED tracker already exists, hide or disable the commissioned menu item (idempotent — no second COMMISSIONED)

### Post-action navigation
- **D-06:** After successful commissioned assimilate: **stay on the current note**, reload assimilation settings / note recall info, and **do not** advance the assimilation queue. (Note may still need ordinary assimilation; advancing would skip that opportunity)

### Settings visibility
- **D-07:** Assimilation settings (note recall info / tracker presentation) must make the **commissioned memory tracker** visibly distinct so E2E can assert “I should see a commissioned memory tracker for {title}”. Prefer an explicit type label using glossary term **Commissioned** (not Tutor / Learning Session — those belong to later phases)

### E2E scope
- **D-08:** Graduate only the Phase 2 scenario from `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` (“Assimilating a note with a tutor creates a commissioned memory tracker”) into `e2e_test/features/learning_session/`, tag `@wip` until green. Do not graduate later-phase scenarios in this phase

### Claude's Discretion
- Exact DaisyUI / split-button markup and `data-test` ids (keep page-object friendly)
- Whether API is an extended `AssimilationRequestDTO` field vs a dedicated create path — prefer the smallest extension of the existing assimilate endpoint if it keeps OpenAPI/clients coherent
- Exact placement of the type badge in NoteInfoBar vs a dedicated tracker row — whatever matches existing spelling/property tracker presentation with minimal UI churn

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase / requirements
- `.planning/ROADMAP.md` — Phase 2 goal, success criteria, E2E scenario name
- `.planning/REQUIREMENTS.md` — TRK-01, TRK-02; out of scope: properties UI (TRK-04), replacing ordinary trackers
- `.planning/phases/01-commissioned-tracker-model/CONTEXT.md` — milestone opt-in surface (caret next to Assimilate), coexistence
- `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` — behavioral draft; Phase 2 scenario is first

### Glossary / ADRs
- `docs/adrs/0001-ubiquitous-language.md` §3 — **Commissioned memory tracker**, Tutor, Learning Session terms (UI copy: Assimilate as commissioned / Commissioned)
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — protocol context (not implemented in Phase 2)
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — score→schedule (not Phase 2)

### Phase 1 foundation (do not regress)
- `.planning/phases/01-commissioned-tracker-model/01-VERIFICATION.md` — COMMISSIONED excluded from ordinary due-recall; coexistence green
- `.planning/phases/01-commissioned-tracker-model/01-REVIEW-FIX.md` — ordinary assimilate ignores COMMISSIONED when deciding note-level existence; daily assimilate count excludes COMMISSIONED

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/components/recall/AssimilationButtons.vue` — primary Assimilate / Skip / Revive; extend with note-level caret+menu (properties keep current buttons without caret)
- `frontend/src/composables/useAssimilateUnit.ts` + `AssimilationController.assimilate` — existing create path; extend request body for commissioned
- `backend/.../MemoryTrackerAssimilation.java` — create site; already filters COMMISSIONED out of ordinary note-level existence checks
- `backend/.../MemoryTrackerType.COMMISSIONED` + `makeMe.aMemoryTrackerFor(...).commissioned()` — type and fixtures ready
- `frontend/src/components/commons/dropdownMenuClasses.ts` + DaisyUI dropdown patterns (e.g. `NotebookCatalogGroupActions.vue`)

### Established Patterns
- Assimilation settings footer card on assimilation / note show with settings on
- Note recall info loads memory trackers for disable/enable and spelling-only mode
- E2E page objects under `e2e_test/start/pageObjects/assimilationPage/`

### Integration Points
- `AssimilationPanel.vue` `assimilateDisabled` / `hasNoteLevelMemoryTrackers` — must ignore COMMISSIONED (D-03)
- `AssimilationRequestDTO` currently: `noteId`, `skipMemoryTracking`, `propertyKey` — needs commissioned signal
- OpenAPI regenerate after controller/DTO change (`generate-api-client` skill)

</code_context>

<specifics>
## Specific Ideas

- Menu copy locked to glossary: **Assimilate as commissioned**
- Success path matches feature: assimilating → assimilate as commissioned → open assimilation settings → see commissioned tracker for that note
- Coexistence observable: ordinary trackers for the same note still exist when present (success criterion 3) — cover when ordinary already existed before commissioned create

</specifics>

<deferred>
## Deferred Ideas

- Potential learning sessions on recall progress bar — Phase 3 (POT-*, TRK-03)
- Learning Session / Request builder — Phase 4–5
- Commissioned trackers for properties in UI — TRK-04 (v2)
- Commissioned assimilation as first intake via Tutor (not only recall) — TRK-05 (v2)
- Feedback score display on tracker — REC-03 (Phase 6)

None — discussion stayed within phase scope for actionable deferred items beyond roadmap.

</deferred>

---

*Phase: 2-assimilate-as-commissioned*
*Context gathered: 2026-08-08*
