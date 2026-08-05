# Phase 7: Compact result + Resolve dialog shell - Research

**Researched:** 2026-08-05
**Domain:** Frontend recall UX — accidental-match compact result + optional resolve dialog shell (Vue 3 / DaisyUI / in-repo Modal)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Open the resolve UI with in-repo `PopButton` → `Modal` (native `<dialog showModal>`), CTA label **Resolve accidental match**, gated to `ACCIDENTAL_MATCH` with non-empty `matchedNotes`. — **Reversibility:** reversible
- **D-02:** Place the CTA immediately under the accidental-match alert (before `NoteUnderQuestion` / reviewed `NoteShow`). — **Reversibility:** reversible
- **D-03:** Extract match-list body into a new `AccidentalMatchResolveDialog` under `frontend/src/components/recall/` (keep chrome/CTA ownership in `AnsweredSpellingQuestion`). — **Reversibility:** reversible
- **D-04:** Reuse existing Modal dismiss behavior (close button, backdrop/`close_request`, modal stack ESC, route-change close). Do not add a new modal library.
- **D-05:** Dialog lists match **titles only** from `answeredQuestion.matchedNotes` (`NoteTopology` title/id). No note body, no notebook path/breadcrumb, no clickable title navigation, no per-row actions in this phase. — **Reversibility:** reversible
- **D-06:** Present multiple matches as a simple vertical list (one title per row). Multi-match progress cues are v2 (AMR-12).
- **D-07:** Remove the stacked matched-notes section **including** per-match `MatchedNoteLinkOffer` / `link-to-matched-note-*` CTAs in this phase. Build-a-link returns in Phase 9 as an in-dialog step (single Modal; never nested `PopButton`). Temporary loss of link-from-result until Phase 9 is an accepted stop-safe trade for compact reviewed-note focus. — **Reversibility:** costly — undoing without Phase 9 would reintroduce stacked bodies or a parallel link surface
- **D-08:** Rewrite `AnsweredSpellingQuestionAccidentalMatch` unit tests for: no matched `NoteShow`s / no `matched-notes-section`; CTA present when matches exist; opening dialog shows titles; omit CTA when `matchedNotes` empty; OVERLAP scenarios unchanged (no resolve CTA). — **Reversibility:** reversible
- **D-09:** Update E2E reveal coverage so accidental-match scenarios assert CTA + dialog titles instead of stacked reveal. Tag current link-from-result E2E scenarios `@wip` until Phase 9 restores Build a link. Keep `overlap_try_again` green and uncoupled. Capability-named specs only (no phase numbers in product tests). — **Reversibility:** reversible

### Claude's Discretion
- Exact `data-testid` names for CTA/dialog/rows (prefer capability names like `resolve-accidental-match`, resolve-dialog container, per-title rows).
- Visual density of the title list (DaisyUI/Tailwind classes) within existing recall chrome.
- Whether Phase 7 dialog is a pure presentational list component or receives `closer` from `PopButton` slot (follow existing `PopButton` `#default="{ closer }"` pattern).

### Deferred Ideas (OUT OF SCOPE)
- Notebook path/breadcrumb + clickable titles — Phase 8 (AMR-04)
- Build a link / readonly unload gates — Phase 9 (AMR-06, AMR-07)
- Overlap alias append util — Phase 10 (Structure)
- Add as overlapped note (no try-again / no reclaim) — Phase 11 (AMR-08, AMR-09)
- Title navigate, reopen resolve, full E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed

None — discussion stayed within phase scope (auto mode)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AMR-01 | On an accidental-match spelling result, the UI does not stack full matched-note bodies under the reviewed note (reviewed note keeps primary focus) | Remove `matched-notes-section` + per-match `NoteShow`; keep single reviewed `NoteShow` in `AnsweredSpellingQuestion.vue` |
| AMR-02 | User sees a **Resolve accidental match** control under the accidental-match alert that opens a resolve dialog when matches exist | `PopButton` under alert gated by same outcome+length check as today's `showMatchedNotesSection`; body = `AccidentalMatchResolveDialog` listing titles |
| AMR-03 | User can dismiss the resolve dialog anytime and continue without resolving any match | Rely on existing `Modal` close button, backdrop `close_request`, ESC via `modalStack`, route-change close — no forced wizard / auto-open |
</phase_requirements>

## Summary

Phase 7 is a **frontend-only Behavior** slice: replace v1.1 stacked matched-note `NoteShow` bodies with an optional **Resolve accidental match** CTA that opens the in-repo `PopButton` → `Modal` shell and lists match **titles only**. Grading, SRS, OpenAPI shapes, and OVERLAP try-again chrome stay untouched (ADR 0003). Milestone research already locked reuse-only stack — this phase implements Architecture “Behavior — CTA + dialog shell, drop stacked match bodies” without path hydrate, link offer, or overlap write.

Stop-safe value after this phase alone: reviewed note is primary again; match identity survives via CTA → title list; learners may dismiss and continue. Link-from-result is intentionally paused (`@wip` E2E) until Phase 9 restores Build a link inside the same Modal (never nested `PopButton`).

**Primary recommendation:** Modify `AnsweredSpellingQuestion.vue` — delete the matched-notes `<section>`; insert a gated `PopButton` titled **Resolve accidental match** immediately under the alert; add presentational `AccidentalMatchResolveDialog.vue` that renders `matchedNotes` titles; rewrite Vitest + update `AnsweredQuestionPage` / reveal E2E; `@wip` the two link scenarios; keep overlap unit + `overlap_try_again.feature` green.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Compact accidental-match result chrome | Browser / Client | — | Vue SFC presentation only; no API/SRS change |
| Resolve CTA open/dismiss dialog | Browser / Client | — | Existing `PopButton`/`Modal` Teleport + native `<dialog>` |
| Match title list from grade payload | Browser / Client | API / Backend (read-only) | Titles already on `AnsweredQuestion.matchedNotes: NoteTopology[]`; no new fetch in Phase 7 |
| OVERLAP try-again chrome | Browser / Client | API / Backend (unchanged grade) | Outcome-discriminated UI; must not share gate with resolve CTA |
| Link-from-result / path / overlap declare | — (deferred) | — | Phases 8–11; do not implement here |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue | 3.5.40 | SFC composition for result + dialog body | Frontend runtime pin `[VERIFIED: frontend/package.json]` |
| vue-router | 5.2.0 | Modal already closes on `route.fullPath` change | Existing Modal behavior; no new routing `[VERIFIED: frontend/package.json]` |
| DaisyUI | 5.7.15 | `daisy-btn` / `daisy-alert` classes for CTA + alerts | Repo convention; DaisyUI 5 prefers native `dialog.showModal()` — already what `Modal.vue` does `[VERIFIED: frontend/package.json]` `[CITED: daisyui.com/components/modal]` |
| Tailwind CSS | (repo) | Vertical list spacing (`flex flex-col gap-*`) | Existing utility classes only |

### Supporting (reuse in-repo — do not npm install)

| Library / seam | Version | Purpose | When to Use |
|----------------|---------|---------|-------------|
| `PopButton` → `Modal` | in-repo | Optional resolve dialog host | CTA under accidental-match alert |
| `AccidentalMatchResolveDialog` | **NEW** under `recall/` | Title-only list body | Slot content of Resolve `PopButton` |
| `matchedNotes` / `NoteTopology` | generated API | id + title for list rows | Phase 7 list source — no realm hydrate yet |
| Vitest 4.1.10 + `@vue/test-utils` 2.4.11 | pinned | Unit tests for chrome + dialog | Rewrite AccidentalMatch spec |
| Cypress + Cucumber | existing | Reveal E2E + page object | Update reveal; `@wip` link scenarios |
| `MatchedNoteLinkOffer` | in-repo | — | **Keep file**; unused in Phase 7 UI (Phase 9) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `PopButton` + `Modal` | DaisyUI stock `modal` / `modal-box` markup | Forks Doughnut modal stack, ESC, Teleport — **forbidden** by CONTEXT D-04 and STACK.md |
| `PopButton` + `Modal` | Headless UI / Radix / vue-final-modal | Duplicate modal manager — **out of scope** |
| Titles from `matchedNotes` | Eager `NoteRealm` hydrate for titles | Unnecessary — `NoteTopology.title` already present; hydrate is Phase 8 for path |
| Presentational dialog body | Keep list inline in `AnsweredSpellingQuestion` | CONTEXT D-03 requires extract; parent already dense |

**Installation:**

```bash
# No new packages. Reuse pinned frontend stack only.
```

**Version verification:** vue@3.5.40, vue-router@5.2.0, daisyui@5.7.15, vitest@4.1.10, @vue/test-utils@2.4.11 — read from `frontend/package.json` this session. `[VERIFIED: frontend/package.json]`

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | N/A | No installs |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
┌─ Spelling answer submit (unchanged) ─────────────────────────────┐
│  RecallPromptController → MemoryTrackerService.answerSpelling    │
│  → AnsweredQuestion { outcome: ACCIDENTAL_MATCH, matchedNotes[] }│
└───────────────────────────────┬──────────────────────────────────┘
                                ▼
┌─ AnsweredSpellingQuestion (MODIFY) ──────────────────────────────┐
│  1. accidental-match-alert (keep)                                 │
│  2. PopButton "Resolve accidental match"  ←── NEW (gated)         │
│        │ opens single Modal                                       │
│        ▼                                                          │
│     AccidentalMatchResolveDialog (NEW)                            │
│        vertical list of matchedNotes[].title (no NoteShow)        │
│  3. NoteUnderQuestion + reviewed NoteShow (keep primary)          │
│  4. OVERLAP try-again branch unchanged (separate v-if)            │
│  ✕ matched-notes-section + MatchedNoteLinkOffer PopButtons REMOVED│
└───────────────────────────────────────────────────────────────────┘
```

### Recommended Project Structure

```
frontend/src/components/recall/
├── AnsweredSpellingQuestion.vue       # MODIFY: CTA under alert; drop stacks
├── AccidentalMatchResolveDialog.vue   # NEW: title-only list
├── MatchedNoteLinkOffer.vue           # KEEP unused in UI until Phase 9
└── …

frontend/tests/components/recall/
├── AnsweredSpellingQuestionAccidentalMatch.spec.ts  # REWRITE
├── AnsweredSpellingQuestionOverlap.spec.ts          # UPDATE leak assert → no resolve CTA
└── answeredSpellingQuestionTestSupport.ts           # REUSE helpers

e2e_test/
├── features/recall/accidental_match_reveal.feature  # UPDATE reveal; @wip link scenarios
├── features/recall/overlap_try_again.feature        # KEEP green (may tighten “no resolve”)
└── start/pageObjects/AnsweredQuestionPage.ts        # UPDATE selectors
```

### Pattern 1: Outcome-gated Resolve CTA (same gate as today’s section)

**What:** Reuse the boolean shape of `showMatchedNotesSection`:

```104:108:frontend/src/components/recall/AnsweredSpellingQuestion.vue
const showMatchedNotesSection = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)
```

Rename conceptually to `showResolveAccidentalMatchCta` (or keep name and bind CTA). **Never** gate on `matchedNotes.length` alone — OVERLAP leak tests depend on outcome discrimination. `[VERIFIED: frontend/src/components/recall/AnsweredSpellingQuestion.vue:104-108]`

**When to use:** Always for Phase 7 CTA visibility.

### Pattern 2: PopButton hosts Modal; dialog body is a child

**What:** Standard host:

```1:25:frontend/src/components/commons/Popups/PopButton.vue
<template>
  <button
    ref="buttonRef"
    v-bind="$attrs"
    …
  >
    …
  </button>
  <Modal
    v-if="show"
    …
    @close_request="closeDialog"
  >
    <template #body>
      <slot name="default" :closer="closeDialog" />
    </template>
  </Modal>
</template>
```

Pass `title="Resolve accidental match"`, `data-testid` via attrs (button gets `$attrs`), default `showCloseButton` true. Slot body = `<AccidentalMatchResolveDialog :matched-notes="…" />`. `[VERIFIED: frontend/src/components/commons/Popups/PopButton.vue:1-25]`

**Discretion recommendation:** Make `AccidentalMatchResolveDialog` **pure presentational** (props: `matchedNotes`). Do **not** require `closer` in Phase 7 — Modal X / backdrop / ESC satisfy AMR-03. Reserve `#default="{ closer }"` wiring for Phase 9 when in-dialog steps need `@close-dialog`.

### Pattern 3: CTA placement under alert (D-02)

**What:** Current template order is alert → `NoteUnderQuestion` → … → matched section. Insert Resolve `PopButton` **immediately after** the alert `<div>`, **before** `NoteUnderQuestion`. `[VERIFIED: frontend/src/components/recall/AnsweredSpellingQuestion.vue:1-18]` + CONTEXT D-02.

### Pattern 4: Titles from NoteTopology only

**What:** Generated type:

```233:238:packages/generated/doughnut-backend-api/types.gen.ts
export type NoteTopology = {
    id: number;
    title: string;
    createdAt?: string;
    updatedAt?: string;
};
```

```292:300:packages/generated/doughnut-backend-api/types.gen.ts
export type AnsweredQuestion = {
    …
    matchedNotes?: Array<NoteTopology>;
};
```

List `matched.title` (and key by `matched.id`). No `NoteShow`, no `getNoteRealmRefAndLoadWhenNeeded` in Phase 7. `[VERIFIED: packages/generated/doughnut-backend-api/types.gen.ts:233-238]` `[VERIFIED: packages/generated/doughnut-backend-api/types.gen.ts:292-300]`

### Anti-Patterns to Avoid

- **Keep stacked NoteShows “until dialog is ready”:** Violates AMR-01 / milestone goal — remove stacks in the same phase as CTA+dialog (Pitfall 1).
- **Auto-open Modal on answer submit:** Violates optional resolve (AMR-03 / Pitfall 2).
- **Gate resolve CTA without checking `outcome === "ACCIDENTAL_MATCH"`:** Bleeds into OVERLAP (Pitfall 8).
- **Nest another `PopButton` for future link offer now:** Phase 9 problem; do not leave nested host stubs in Phase 7.
- **Mount `NoteShow` inside dialog “for convenience”:** Defeats compact chrome; bodies are anti-feature.
- **Delete accidental-match E2E without replacement asserts:** Soft-delete of reveal coverage (Pitfall 1).
- **Encode phase numbers in test/file/feature names:** Capability names only (`planning.mdc`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Modal open/close/ESC/route | Custom overlay / third-party dialog | `PopButton` + `Modal` | Already Teleport, `showModal`, modalStack ESC, route close `[VERIFIED: frontend/src/components/commons/Modal.vue:72-95]` |
| Match identity for Phase 7 | New API / realm hydrate | `matchedNotes[].title` | Topology already has title/id |
| Test fixtures for ACCIDENTAL_MATCH | Hand-built DTOs | `makeMe.anAnsweredQuestion.accidentalMatch(...)` | Existing builder + `accidentalMatchWithTwoMatchedNotes` |
| Modal dismiss tests from scratch | Reinvent wait-for-dialog | Patterns in `frontend/tests/commons/Modal.spec.ts` / `modalTestSupport.ts` | `dialog` in `document.body`, `.close-button` |

**Key insight:** Phase 7 is composition and deletion of UI — zero new libraries and zero backend work.

## Common Pitfalls

### Pitfall 1: Reveal lost when removing stacked NoteShows
**What goes wrong:** Alert + reviewed note only; no way to see which notes matched.  
**Why it happens:** Deleting `matched-notes-section` without CTA→dialog in the same change.  
**How to avoid:** Ship remove-stack + Resolve CTA + title list together; rewrite unit/E2E asserts in the same phase.  
**Warning signs:** Specs still looking for `matched-notes-section` NoteShows; reveal feature deleted or `@wip`’d without CTA replacement.

### Pitfall 2: Weak / buried / auto-open CTA
**What goes wrong:** Optional resolve becomes mandatory or undiscoverable.  
**How to avoid:** Visible CTA under alert with locked copy; open only on click; empty `matchedNotes` → no CTA.

### Pitfall 8: OVERLAP chrome coupling
**What goes wrong:** Resolve CTA or dialog appears on OVERLAP results (or try-again appears on ACCIDENTAL_MATCH).  
**How to avoid:** Outcome-discriminated `v-if`; update Overlap leak unit test to assert **no** resolve CTA / no dialog entry; keep `overlap_try_again.feature` green; extend page object `expectNoMatchedNotesOrAccidentalMatch` to also assert resolve CTA absent.

### Pitfall: Link E2E left red without `@wip`
**What goes wrong:** CI fails on “Link to this note” scenarios after stack removal.  
**How to avoid:** Tag the two link scenarios in `accidental_match_reveal.feature` with `@wip` (cap 5; current repo `@wip` count is 0). Rewrite reveal scenario asserts to CTA + dialog titles. Do **not** `@wip` the whole feature.

### Pitfall: Dead `canOfferLinkToMatched` left half-alive
**What goes wrong:** Unused realm-load / currentUser inject linger after link PopButtons removed.  
**How to avoid:** Remove link-offer-only helpers/imports from `AnsweredSpellingQuestion` in Phase 7 cleanup (gates return in Phase 9 on the dialog). Keep `MatchedNoteLinkOffer.vue` file.

### Pitfall: Page object still asserts stacked section
**What goes wrong:** Steps pass locally with old helpers or fail for wrong reason.  
**How to avoid:** Update `AnsweredQuestionPage.expectAccidentalMatchReveal` / `expectMatchedNoteInSection` / link helpers in the same change as the Vue chrome.

## Code Examples

### Recommended AnsweredSpellingQuestion chrome (illustrative)

```vue
<!-- Source: compose from AnsweredSpellingQuestion.vue + PopButton.vue + CONTEXT D-01..D-05 -->
<div class="daisy-alert" :class="alertClass" :data-testid="alertTestId">
  <strong>{{ alertMessage }}</strong>
</div>

<PopButton
  v-if="showResolveAccidentalMatchCta"
  title="Resolve accidental match"
  aria-label="Resolve accidental match"
  btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
  data-testid="resolve-accidental-match"
>
  <template #default>
    <AccidentalMatchResolveDialog
      :matched-notes="answeredQuestion.matchedNotes ?? []"
    />
  </template>
</PopButton>

<NoteUnderQuestion v-bind="recalledNoteUnderQuestionProps(answeredQuestion.recalledNote)" />
<!-- … ViewMemoryTrackerLink + reviewed NoteShow + OVERLAP try-again unchanged … -->
<!-- matched-notes-section REMOVED -->
```

### AccidentalMatchResolveDialog (illustrative)

```vue
<!-- Source: CONTEXT D-03/D-05/D-06 + NoteTopology shape -->
<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
defineProps<{ matchedNotes: NoteTopology[] }>()
</script>

<template>
  <div data-testid="accidental-match-resolve-dialog">
    <ul class="flex flex-col gap-2">
      <li
        v-for="matched in matchedNotes"
        :key="matched.id"
        :data-testid="`resolve-match-row-${matched.id}`"
      >
        {{ matched.title }}
      </li>
    </ul>
  </div>
</template>
```

**Discretion — recommended testids:**
- CTA: `resolve-accidental-match`
- Dialog container: `accidental-match-resolve-dialog`
- Row: `resolve-match-row-{id}`

### Unit test deltas (focused assertions)

Drive `AnsweredSpellingQuestion` via `mountAnsweredSpellingQuestion` / `accidentalMatchWithTwoMatchedNotes`. Assert:

1. No `[data-testid="matched-notes-section"]`; reviewed path still has NoteShow stub(s) for **reviewed** id only (not matched ids 10/20).
2. CTA exists when matches present; click → `document.body` / `dialog` contains titles `"Matched A"` / `"Matched B"`.
3. Empty `matchedNotes` → no CTA.
4. Overlap + leaked `matchedNotes` → no resolve CTA (update existing leak test).

Drop Phase-7-obsolete link CTA / open-link-offer cases from AccidentalMatch spec (or move `@wip` comments toward Phase 9) — do not leave failing tests.

### E2E page object direction

Replace `expectMatchedNoteInSection` stacked asserts with: alert → click `resolve-accidental-match` → dialog visible with matched title text → close (`.close-button` or ESC) → still on accidental-match result. Keep link helpers for Phase 9 but scenarios tagged `@wip`.

## State of the Art

| Old Approach (v1.1) | Current Approach (v1.2 Phase 7) | When Changed | Impact |
|---------------------|----------------------------------|--------------|--------|
| Stacked matched `NoteShow` + per-match Link PopButton | Optional Resolve CTA → title-only Modal list | This phase | Reviewed note primary; temporary link pause |
| Reveal == full note bodies | Reveal == dialog title list | This phase | Identity without height theft |

**Deprecated/outdated:**
- `matched-notes-section` / `link-to-matched-note-*` on the result surface — remove in Phase 7; link returns inside dialog in Phase 9.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| — | *(none material)* | — | All discrete values cited from Read this session |

Discretion items (testids, presentational vs closer, Tailwind density) are recommendations, not assumptions about product truth.

## Open Questions

1. **Should Phase 7 unit-test Modal dismiss explicitly?**
   - What we know: `Modal.spec.ts` already covers close button + ESC; AMR-03 is product-level optional dismiss.
   - What's unclear: Whether AccidentalMatch mount should click `.close-button` once for regression.
   - Recommendation: One light unit assert (open → close → dialog gone) **or** cover dismiss primarily in E2E reveal scenario; avoid duplicating full Modal suite.

2. **Dialog header title copy?**
   - What we know: CTA copy is locked; dialog body is titles only.
   - What's unclear: Whether Modal `#header` should show “Resolve accidental match” or stay body-only.
   - Recommendation: Body-only list is enough for Phase 7; optional short header is discretionary polish, not a requirement ID.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | Frontend tooling | ✓ | v24.5.0 | — |
| pnpm | Scripts | ✓ | 11.20.0 | — |
| Vue / Vitest (repo pins) | Unit tests | ✓ | vue 3.5.40 / vitest 4.1.10 | — |
| `pnpm sut` services | Targeted E2E | Assume running per e2e-authoring | — | Suggest start if missing; do not restart nag |
| Nix (`CURSOR_DEV=true nix develop -c`) | Canonical commands | Project contract | — | Cloud VM skill if no Nix |

**Missing dependencies with no fallback:** none identified for this frontend-only phase.

**Step 2.6:** External tools beyond repo frontend/E2E stack not required.

## Validation Architecture

> `workflow.nyquist_validation` is enabled (`true` in `.planning/config.json`).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.10 (browser mode) + Cypress/Cucumber E2E |
| Config file | `frontend/vitest.config.ts`; E2E `e2e_test/config/ci.ts` (CI skips `@wip`) |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| Full suite command (targeted, not whole CI) | AccidentalMatch + Overlap unit files; `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature,e2e_test/features/recall/overlap_try_again.feature` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AMR-01 | No stacked matched NoteShows / no `matched-notes-section` | unit | AccidentalMatch spec (rewrite) | ✅ rewrite |
| AMR-02 | Resolve CTA + dialog titles | unit + E2E | AccidentalMatch spec; reveal feature scenario 1 | ✅ update |
| AMR-03 | Dismiss anytime | unit and/or E2E | Close dialog; remain on result | ✅ extend |
| (regression) | No resolve CTA on OVERLAP | unit + E2E | Overlap.spec leak test; `overlap_try_again.feature` | ✅ update assert |
| (interim) | Link-from-result paused | E2E `@wip` | Two link scenarios in reveal feature | ✅ tag `@wip` |

### Sampling Rate

- **Per task commit:** AccidentalMatch (+ Overlap) Vitest file(s)
- **Per wave merge:** Same + targeted Cypress specs above
- **Phase gate:** Targeted E2E green (non-`@wip`); no failing unit tests; `@wip` count ≤ 5

### Wave 0 Gaps

- [ ] Rewrite assertions in `AnsweredSpellingQuestionAccidentalMatch.spec.ts` (exists but asserts old stacked UI)
- [ ] Update `AnsweredQuestionPage.ts` reveal/link helpers for CTA/dialog selectors
- [ ] Update `accidental_match_reveal.feature` scenario 1; `@wip` scenarios 2–3
- [ ] Tighten Overlap leak unit test + optionally page object “no resolve CTA”
- [ ] Optional: thin `AccidentalMatchResolveDialog.spec.ts` only if list logic grows beyond presentational — prefer driving via AnsweredSpellingQuestion boundary (`unit-testing.mdc`)

None — frameworks already installed; gaps are assertion/selector rewrites, not new infra.

## Security Domain

> `security_enforcement` enabled in config.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Existing session; no new auth |
| V3 Session Management | no | Unchanged |
| V4 Access Control | no (Phase 7) | No mutate CTAs this phase; readonly gates return Phase 9 |
| V5 Input Validation | yes (display) | Titles from authenticated grade API; Vue text interpolation escapes HTML |
| V6 Cryptography | no | — |

### Known Threat Patterns for Vue recall dialog

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| XSS via note title in dialog | Tampering | Render as text nodes (`{{ matched.title }}`), not `v-html` |
| Privilege escalation via fake resolve actions | Elevation | No write actions in Phase 7 |
| Nested modal focus confusion (later phases) | Denial of service / UX | Single Modal only; no nested PopButton |

## Project Constraints (from .cursor/rules/)

Actionable directives the planner/executor must honor:

| Source | Directive |
|--------|-----------|
| `planning.mdc` | Behavior phase: one observable behavior; stop-safe; no phase numbers in product/tests |
| `planning.mdc` | Test-first where changing behavior; `@wip` failing E2E (cap 5); remove `@wip` when green |
| `planning.mdc` | Targeted E2E only (not full suite) unless explicitly required |
| `planning.mdc` | After phase: cleanup, post-change-refactor, update plan, commit+push (execute-plan wrap-up) |
| `unit-testing.mdc` | Small tests: stable boundary (`AnsweredSpellingQuestion`), data over mocks, focused assertions, concise `makeMe` |
| `frontend-testing.mdc` | `data-testid` selectors; Vitest browser; `mockSdkService` for HTTP; avoid role queries; Nix/`pnpm` commands |
| `frontend-component.mdc` | Vue SFC + DaisyUI `daisy-*`; modals via `Modal.vue`; query dialog in `document.body` in tests |
| `e2e-authoring.mdc` | Capability-named features; page objects fluent; assume `pnpm sut`; `cypress run --spec` for touched features |
| `gsd-coexistence.mdc` | Local Jidoka / refactor / commit+push even under GSD execute |
| `general.mdc` | Nix prefix for tooling; no speculative defensive layers; capability naming |
| ADR 0003 | Do not change ACCIDENTAL_MATCH / OVERLAP grading or SRS in this UI-only phase |

## Sources

### Primary (HIGH confidence)

- `07-CONTEXT.md` — locked decisions D-01..D-09
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — current stacked UI + gates
- `frontend/src/components/commons/Popups/PopButton.vue`, `Modal.vue` — dialog host
- `packages/generated/doughnut-backend-api/types.gen.ts` — `NoteTopology`, `AnsweredQuestion.matchedNotes`
- `AnsweredSpellingQuestionAccidentalMatch.spec.ts`, Overlap spec, `answeredSpellingQuestionTestSupport.ts`
- `e2e_test/.../accidental_match_reveal.feature`, `overlap_try_again.feature`, `AnsweredQuestionPage.ts`
- `.planning/research/{SUMMARY,ARCHITECTURE,STACK,PITFALLS,FEATURES}.md` — milestone reuse
- `frontend/package.json` — version pins
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — graded outcomes separation

### Secondary (MEDIUM confidence)

- Context7 DaisyUI modal docs — native `dialog.showModal()` recommended `[CITED: daisyui.com/components/modal]`
- Context7 Vue classic modal example — slot/mask pattern corroborates Teleport Modal; in-repo Modal is source of truth

### Tertiary (LOW confidence)

- None material for Phase 7 planning

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — reuse-only; pins verified in `frontend/package.json`
- Architecture: HIGH — verified against current Vue SFCs + CONTEXT locks
- Pitfalls: HIGH — mapped from PITFALLS.md Phase-1 items + live E2E/page-object coupling

**Research date:** 2026-08-05  
**Valid until:** 2026-09-04 (30 days; stable in-repo UI seams)
