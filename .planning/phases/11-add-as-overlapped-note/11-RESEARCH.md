# Phase 11: Add as overlapped note - Research

**Researched:** 2026-08-05
**Domain:** Vue 3 recall resolve dialog — overlap wiki-link declare (frontend-only content mutation)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Per-row CTA and persist path
- **D-01:** Add a per-row **Add as overlapped note** CTA on `AccidentalMatchResolveRow` beside **Build a link** (locked capability copy). Clicking it declares overlap for that row’s match — do **not** introduce a nested Modal/PopButton. — **Reversibility:** reversible
- **D-02:** Persist by composing Phase 10’s `appendOverlapWikiLinkToNoteContent` on the **reviewed** note’s content, then `storedApi.updateTextField(reviewedNoteId, "edit content", …)` when the helper returns a non-null string. Never call `appendAliasToNoteContent` with a plain title for this action (Pitfall 5). — **Reversibility:** reversible
- **D-03:** Host the mutate handler in the resolve dialog host (`AccidentalMatchResolveDialog` or thin sibling under `recall/`), not by branching `AnsweredSpellingQuestion` outcome chrome. Keep ACCIDENTAL_MATCH vs OVERLAP templates outcome-discriminated (Pitfall 8). — **Reversibility:** reversible

#### Post-success dialog / result behavior
- **D-04:** After a successful declare, remain on the accidental-match result (alert + Resolve CTA). Prefer **return to the match list inside the same resolve Modal** (multi-match friendly; mirrors Phase 9 Build-a-link stay-in-dialog). Do **not** force-dismiss unless a later polish phase requires it. — **Reversibility:** reversible
- **D-05:** If `appendOverlapWikiLinkToNoteContent` returns `null` (duplicate / unparseable / bad aliases shape), do **not** call `updateTextField`; stay on the list with no outcome/UI flip. Quiet “already overlapped” chrome is AMR-10 (deferred). — **Reversibility:** reversible

#### AMR-09 — no try-again / no SRS reclaim (Pitfall 4)
- **D-06:** Content mutation only. Do **not** emit `retry`, do **not** re-submit the spelling answer, do **not** change `answer.outcome`, and do **not** offer credit reclaim / undo of the ACCIDENTAL_MATCH schedule for this answer. OVERLAP try-again remains exclusive to graded `outcome === "OVERLAP"`. — **Reversibility:** costly — coupling declare → retry would violate ADR 0003 and corrupt the session narrative
- **D-07:** Assert after declare: no `overlap-try-again` / `overlap-try-again-alert`; accidental-match chrome still present; memory-tracker schedule for this answer unchanged from the accidental-match grade. — **Reversibility:** reversible

#### AMR-07 gates (reuse Phase 9)
- **D-08:** Hide **Add as overlapped note** under the same conditions as **Build a link**: reviewed notebook `readonly` **or** reviewed/matched realms required for the write not yet loaded. Prefer **hide** (not disabled). Titles + path remain visible. — **Reversibility:** reversible
- **D-09:** Prefer one shared gate helper (e.g. rename/generalize `canOfferBuildLink` → mutating-action gate) so both CTAs stay in lockstep; hydrate reviewed realm once at dialog host level. — **Reversibility:** reversible

#### Test coverage for this phase
- **D-10:** Wave 1 — Vitest at the answered-spelling / resolve-dialog boundary: CTA visible when writable+loaded; omitted when readonly/unloaded; click runs append+`updateTextField` with a wiki-link token (not plain alias); after success still ACCIDENTAL_MATCH chrome and **no** overlap try-again. Prefer capability-named specs; reuse `makeMe` accidental-match fixtures. — **Reversibility:** reversible
- **D-11:** Wave 2 — Targeted E2E for Add as overlapped (capability-named; no phase numbers): open Resolve → Add as overlapped → still on accidental-match result; no try-again. Keep `overlap_try_again` green and uncoupled. Prefer page-object updates over rewriting Gherkin where possible. Full reopen-after-navigate polish stays Phase 12. — **Reversibility:** reversible

### Claude's Discretion
- Exact button classes / layout density next to Build a link (DaisyUI `btn-sm` consistent with resolve chrome).
- Whether null-append is silent or uses an existing toast pattern — silent is fine for this phase.
- Whether Vitest stubs `updateTextField` or drives StoredApi with clean storage — follow Phase 9 patterns.
- Exact E2E scenario placement (extend `accidental_match_reveal.feature` vs sibling capability feature).

### Deferred Ideas (OUT OF SCOPE)
- Title navigate, reopen resolve, E2E polish — Phase 12 (AMR-05)
- AMR-10 quiet state when already linked/overlapped; AMR-11..13 polish — v2
- SEED-001 MCQ / fuzzy / `Notebook:Title` spelling — parked seed

None — discussion stayed within phase scope (auto mode)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AMR-08 | From a resolve-dialog row, user can **Add as overlapped note**, which declares an overlap wiki-link alias on the reviewed note toward that match | Wire per-row CTA → host handler → `appendOverlapWikiLinkToNoteContent` → `updateTextField(..., "edit content", …)` on reviewed note; wiki-link token already locked by Phase 10 util |
| AMR-09 | After **Add as overlapped note**, the current result does not show try-again and does not reclaim SRS credit (outcome stays accidental-match; schedule unchanged for this answer) | Content mutation only; do not emit `retry` / re-grade; leave `AnsweredSpellingQuestion` outcome chrome on `ACCIDENTAL_MATCH`; assert no `overlap-try-again*` |
</phase_requirements>

## Summary

Phase 11 is a **Behavior** frontend slice: expose **Add as overlapped note** on each resolve-dialog match row, persist an overlap **wiki-link** alias onto the **reviewed** note’s frontmatter via the Phase 10 util + existing `updateTextField` seam, and leave the current graded answer as `ACCIDENTAL_MATCH` with **no** try-again and **no** SRS credit reclaim (Pitfall 4 / ADR 0003). Phase 10 already solved Pitfall 5 at the util layer; this phase must **call** `appendOverlapWikiLinkToNoteContent`, never plain `appendAliasToNoteContent` with a title.

The host, gates, and stay-in-dialog patterns already exist from Phase 9 (`AccidentalMatchResolveDialog` / `AccidentalMatchResolveRow`, `canOfferBuildLink`, list stays open). Implementation is wiring + tests — **zero new libraries**, **no backend/OpenAPI/AnswerOutcome/SRS changes**.

**Primary recommendation:** Generalize the Phase 9 mutating gate; add a gated per-row CTA that emits to the dialog host; host loads reviewed + matched realms, runs `appendOverlapWikiLinkToNoteContent` → conditional `updateTextField`; stay on list; Vitest Wave 1 then targeted E2E Wave 2 with page-object helpers; keep `overlap_try_again` uncoupled.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Per-row **Add as overlapped note** CTA + hide gates | Browser / Client | — | Same AMR-07 client visibility as Build a link (D-08/D-09) |
| Compose wiki-link alias into reviewed content | Browser / Client (pure util) | — | Phase 10 util already owns token + merge |
| Persist content | API / Backend | Browser / Client | Existing `updateTextField` → note content PATCH; no new endpoint |
| Result chrome / try-again gating | Browser / Client | — | Outcome-discriminated in `AnsweredSpellingQuestion`; declare must not touch it (D-06) |
| SRS schedule for this answer | API / Backend (unchanged) | — | Already graded ACCIDENTAL_MATCH; UI must not reclaim/re-grade (ADR 0003) |
| Vitest / E2E evidence | Browser / Client | — | Answered-spelling boundary + Cypress page object |

## Project Constraints (from .cursor/rules/)

| Rule | Directive for this phase |
|------|--------------------------|
| `planning.mdc` | Behavior phase; one observable behavior (declare overlap from dialog + no try-again/reclaim); stop-safe; ~5 min slice budget; after phase: Jidoka, post-change-refactor, update plan, commit+push |
| `unit-testing.mdc` | Drive `AnsweredSpellingQuestion` / resolve-dialog boundary; data over mocks; focused assertions; concise `makeMe` |
| `frontend-testing.mdc` | `CURSOR_DEV=true nix develop -c pnpm frontend:test …`; Vitest browser; `data-testid`; `mockSdkService` for HTTP; avoid role queries in Vitest |
| `gsd-coexistence.mdc` | Local wrap-up required; Nix prefix for tooling; phase numbers only under `.planning/` |
| `general.mdc` | No phase numbers in product test/feature names; capability-named testids |
| `architecture-decisions.mdc` | Honor ADR 0003: ACCIDENTAL_MATCH vs OVERLAP scheduling; UI content mutation only |
| E2E (planning) | Targeted Cypress for touched feature; keep `overlap_try_again` green; full suite not required locally |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue | 3.5.40 | Dialog host handler + row CTA | Pinned [VERIFIED: frontend/package.json:79] — `"vue": "3.5.40"` |
| Vitest | 4.1.10 | Wave 1 unit tests | Pinned [VERIFIED: frontend/package.json:75] — `"vitest": "4.1.10"` |
| `appendOverlapWikiLinkToNoteContent` | in-repo | Wiki-link overlap append | Phase 10 shipped [VERIFIED: frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts:4-15] |
| `StoredApi.updateTextField` | in-repo | Persist `"edit content"` | Field union `"edit title" \| "edit content"` [VERIFIED: frontend/src/store/StoredApiCollection.ts:98-105] |
| DaisyUI `daisy-btn` classes | in-repo | CTA visual density | Match Build a link: `daisy-btn daisy-btn-secondary daisy-btn-sm` [VERIFIED: frontend/src/components/recall/AccidentalMatchResolveRow.vue:16-26] |
| `@generated/doughnut-backend-api` | — | Realms / content update types | No OpenAPI change |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `MatchedNoteLinkOffer` pattern | in-repo | Reference for compose → `updateTextField` | Copy async write shape; do **not** nest offer for overlap |
| `makeMe` / `answeredSpellingQuestionTestSupport` | in-repo | Accidental-match fixtures + `seedRealms` | Wave 1 gates + click |
| Cypress + `AnsweredQuestionPage` | in-repo | Wave 2 Resolve → Add as overlapped | D-11 |
| vue-router | 5.2.0 | Unchanged title links | Pinned [VERIFIED: frontend/package.json:80] — `"vue-router": "5.2.0"` |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `appendOverlapWikiLinkToNoteContent` | Plain `appendAliasToNoteContent(title)` | **Forbidden** — Pitfall 5 / D-02 |
| Content write only | Emit `retry` / flip to OVERLAP chrome | **Forbidden** — Pitfall 4 / D-06 / ADR 0003 |
| Host handler in dialog | Branch `AnsweredSpellingQuestion` | **Forbidden** — D-03 / Pitfall 8 |
| Shared mutating gate | Separate gates per CTA | Drift risk; D-09 prefers one helper |
| New `POST …/declare-overlap` | Existing content edit | Architecture anti-pattern; out of scope |

**Installation:**

```bash
# No new packages. Zero npm installs for this phase.
```

**Version verification:** Vue / Vitest / vue-router read from `frontend/package.json` this session. No registry packages added → package-legitimacy check N/A.

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | None — reuse in-repo stack only |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
AnsweredSpellingQuestion
  outcome === "ACCIDENTAL_MATCH" ──► alert + Resolve PopButton (unchanged)
  outcome === "OVERLAP" ───────────► overlap-try-again (unchanged; NOT used by declare)
        │
        └─ AccidentalMatchResolveDialog  [step stays list for overlap]
              ├─ canOfferMutatingAction(matchedId)  ← generalize canOfferBuildLink
              ├─ AccidentalMatchResolveRow
              │     [Build a link] ──► step=link (Phase 9)
              │     [Add as overlapped note] ──► emit addAsOverlapped
              │
              └─ onAddAsOverlapped(matchedId):
                    reviewedRealm.note.content
                         + matched realm (notebookId/name + topology)
                         │
                         ▼
                    appendOverlapWikiLinkToNoteContent(...)
                         │
                    null ──► stop (no updateTextField; stay on list)
                    string ─► updateTextField(reviewedId, "edit content", composed)
                         │
                         ▼
                    stay on list + ACCIDENTAL_MATCH chrome
                    (no retry emit, no outcome change)
```

### Recommended Project Structure

```
frontend/src/components/recall/
├── AccidentalMatchResolveDialog.vue   # MODIFY: shared gate + add-as-overlapped handler
├── AccidentalMatchResolveRow.vue      # MODIFY: gated Add as overlapped CTA + emit
├── AnsweredSpellingQuestion.vue       # LEAVE outcome chrome alone (no retry wiring)
└── MatchedNoteLinkOffer.vue           # REFERENCE only (compose + updateTextField)

frontend/src/utils/
└── appendOverlapWikiLinkToNoteContent.ts  # REUSE as-is

frontend/tests/components/recall/
└── AnsweredSpellingQuestionAccidentalMatch.spec.ts  # EXTEND (or capability-named sibling)

e2e_test/start/pageObjects/AnsweredQuestionPage.ts   # ADD: open Resolve → Add as overlapped
e2e_test/features/recall/accidental_match_reveal.feature  # EXTEND scenario (discretion)
e2e_test/features/recall/overlap_try_again.feature   # MUST stay green / uncoupled
```

### Pattern 1: Host-owned declare (list stays open)

**What:** Row emits; dialog host composes + saves; no step swap (unlike Build a link).  
**When to use:** Always for Add as overlapped (D-01..D-05).  
**Example (sketch from locked seams):**

```typescript
// Source: AccidentalMatchResolveDialog + MatchedNoteLinkOffer + appendOverlapWikiLinkToNoteContent
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"

async function addAsOverlappedNote(matchedNoteId: number) {
  const reviewed = reviewedRealm.value
  const matched = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  if (!reviewed?.note || !matched) return

  const composed = appendOverlapWikiLinkToNoteContent(
    reviewed.note.content ?? "",
    {
      noteTopology: matched.note.noteTopology,
      notebookId: matched.notebookRealm.notebook.id,
      notebookName: matched.notebookRealm.notebook.name,
    },
    { notebookId: reviewed.notebookRealm.notebook.id }
  )
  if (composed === null) return // D-05 — silent OK

  await storageAccessor.value
    .storedApi()
    .updateTextField(props.reviewedNoteId, "edit content", composed)
  // stay on list — do not change step; do not emit retry
}
```

### Pattern 2: Shared mutating-action gate (AMR-07)

**What:** One helper gates both **Build a link** and **Add as overlapped note**.  
**When to use:** Always (D-08, D-09).  
**Current gate (rename/generalize):**

```typescript
// Source: AccidentalMatchResolveDialog.vue:55-62 [VERIFIED]
function canOfferBuildLink(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}
```

Recommend rename to e.g. `canOfferMutatingAction` and pass the same boolean to both CTAs.

### Pattern 3: Outcome-discriminated chrome (do not couple)

**What:** Try-again is exclusive to graded OVERLAP.  
**When to use:** Always — never show after dialog declare.  
**Verified chrome gates:**

```typescript
// Source: AnsweredSpellingQuestion.vue:65-73, 31-41 [VERIFIED]
const isOverlap = computed(
  () => props.answeredQuestion.answer.outcome === "OVERLAP"
)
const showResolveAccidentalMatchCta = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)
// try-again button: v-if="isOverlap" data-testid="overlap-try-again"
```

### Anti-Patterns to Avoid

- **Declare → retry / OVERLAP chrome:** Violates Pitfall 4, D-06, ADR 0003.  
- **Plain alias append for overlap:** Violates Pitfall 5 / D-02 — util already exists.  
- **Nested Modal for declare:** Forbidden (D-01); declare is a list-row click, not a step.  
- **Force-dismiss resolve Modal on success:** Prefer stay on list (D-04).  
- **Quiet “already overlapped” UI:** AMR-10 — deferred; null-append is silent (D-05).  
- **Backend declare-overlap RPC / AnswerOutcome change:** Out of scope.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Wiki-link overlap token | Custom string concat / plain title | `appendOverlapWikiLinkToNoteContent` | Pitfall 5; Phase 10 locks shape |
| Frontmatter merge | New YAML parser | Util → `appendAliasToNoteContent` internals | Already shipped |
| Content persist | New API | `updateTextField(..., "edit content", …)` | Same seam as link property write |
| Readonly / unload gates | New permission API | Generalize `canOfferBuildLink` | AMR-07 already decided in Phase 9 |
| Try-again after declare | Shared handler with OVERLAP | Leave `AnsweredSpellingQuestion` alone | Pitfall 4 |

**Key insight:** This phase is **wiring**, not invention — util + persist + gates + negative try-again asserts.

## Common Pitfalls

### Pitfall 1 (milestone #4): Declare → try-again / reclaim

**What goes wrong:** After declare, UI shows `overlap-try-again` or offers credit reclaim.  
**Why it happens:** Product word “overlap” collides with `AnswerOutcome.OVERLAP`.  
**How to avoid:** Content mutation only; never emit `retry`; never mutate `answer.outcome`.  
**Warning signs:** Shared handler with try-again; tests expecting OVERLAP after PATCH.  
**Verification:** Vitest + E2E assert no `overlap-try-again` / `overlap-try-again-alert`; accidental-match alert remains.

### Pitfall 2 (milestone #5): Plain alias instead of wiki-link

**What goes wrong:** Frontmatter gets `- sedation` instead of `- "[[sedation]]"`.  
**Why it happens:** Calling `appendAliasToNoteContent` with a bare title.  
**How to avoid:** Always call `appendOverlapWikiLinkToNoteContent`.  
**Warning signs:** Update payload lacks `[[`.  
**Verification:** Vitest assert `updateNoteContent` / composed content contains `[[…]]` wiki-link token.

### Pitfall 3: Gate drift between CTAs

**What goes wrong:** Build a link hidden but Add as overlapped visible (or vice versa) on readonly.  
**How to avoid:** One shared gate helper (D-09).  
**Warning signs:** Duplicate boolean expressions in template.

### Pitfall 4: Null-append still calls updateTextField

**What goes wrong:** No-op duplicate triggers unnecessary PATCH / undo noise.  
**How to avoid:** `if (composed === null) return` before update (D-05).

### Pitfall 5: Wiring retry from dialog host into AnsweredSpellingQuestion

**What goes wrong:** Outcome chrome coupling (Pitfall 8).  
**How to avoid:** Handler stays in resolve dialog host (D-03); answered spelling only hosts PopButton.

## Code Examples

### Util contract (already shipped — call site must match)

```typescript
// Source: frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts:4-15 [VERIFIED]
export function appendOverlapWikiLinkToNoteContent(
  contentMarkdown: string,
  target: {
    noteTopology: { title: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number }
): string | null {
  const token = buildWikiLinkText(target, { notebookId: source.notebookId })
  return appendAliasToNoteContent(contentMarkdown, token)
}
```

### Persist seam (MatchedNoteLinkOffer reference)

```typescript
// Source: MatchedNoteLinkOffer.vue:83-96 [VERIFIED] — property path; mirror for overlap
const composed = appendWikiLinkPropertyRow(source.content ?? "", linkText)
if (composed === undefined) return
await closeDialogThen(() =>
  storageAccessor.value
    .storedApi()
    .updateTextField(source.id, "edit content", composed)
)
// Overlap differencing: no closeDialogThen / step change; null-check is `=== null`
```

### Row CTA shape (discretion: classes; locked: copy + hide gate)

```vue
<!-- Beside Build a link; same daisy-btn-sm density -->
<button
  v-if="canMutate"
  type="button"
  class="daisy-btn daisy-btn-secondary daisy-btn-sm"
  :data-testid="`add-as-overlapped-note-${matched.id}`"
  title="Add as overlapped note"
  aria-label="Add as overlapped note"
  @click="$emit('addAsOverlapped')"
>
  Add as overlapped note
</button>
```

### Wave 1 Vitest focus (canonical + deltas)

```typescript
// Boundary: mountAnsweredSpellingQuestion + seedRealms + mockSdkService(TextContentController, "updateNoteContent", …)
// Canonical success: click add-as-overlapped → update called with wiki-link → accidental-match-alert still present
//   → no overlap-try-again / overlap-try-again-alert; wrapper.emitted("retry") undefined
// Deltas only: omit CTA when readonly; omit when realms unloaded; null-append → no update call
```

### Wave 2 E2E (page-object preferred)

```typescript
// AnsweredQuestionPage — mirror openLinkToMatchedNote:
// resolve-accidental-match → click add-as-overlapped-note-* → expectStillOnAccidentalMatchResult
// → assert overlap-try-again does not exist
// Keep overlap_try_again.feature untouched / green
```

## State of the Art

| Old Approach (v1.1) | Current Approach (v1.2 Phase 11) | When Changed | Impact |
|---------------------|----------------------------------|--------------|--------|
| Overlap declare under stacked match NoteShow | Declare from resolve-dialog row | v1.2 | Reviewed note stays primary |
| Risk of coupling declare to OVERLAP try-again | Explicit content-only path | Locked | ADR 0003 preserved |
| Plain alias temptation | Named util forces wiki-link | Phase 10 | Pitfall 5 closed at util |

**Deprecated/outdated:**
- Stacked per-match overlap CTAs on result — replaced by resolve dialog actions
- Teaching call sites to pass plain titles into `appendAliasToNoteContent` for overlap — forbidden

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Silent null-append (no toast) is acceptable for this phase | Claude's Discretion / D-05 | Low — AMR-10 owns quiet “already overlapped” chrome |
| A2 | Recommended testid `add-as-overlapped-note-{id}` is fine for E2E | Code Examples | Low — planner/discretion can rename if page-object prefers another capability id |
| A3 | Schedule-unchanged is adequately proven by no retry/re-grade + staying on ACCIDENTAL_MATCH chrome in Vitest; E2E need not open Memory Tracker unless easy | Validation / D-07 | Medium if product insists on schedule field assert — then add E2E memory-tracker check |

**If this table is empty:** All claims in this research were verified or cited — no user confirmation needed.  
*(A1–A3 are discretion / verification-depth only; locked decisions do not need reconfirmation.)*

## Open Questions

1. **Exact E2E feature file placement**
   - What we know: D-11 allows extend `accidental_match_reveal.feature` or sibling; page-object preferred.
   - What's unclear: Product preference for one more scenario vs new feature file.
   - Recommendation: Extend `accidental_match_reveal.feature` with one scenario + page-object helper (mirrors Build a link).

2. **Schedule field assertion depth**
   - What we know: D-07 asks schedule unchanged; Pitfall 4 lists memory tracker.
   - What's unclear: Whether Vitest chrome + no retry is enough for Wave 1.
   - Recommendation: Wave 1 = chrome + no retry + wiki-link write; Wave 2 = stay on accidental-match + no try-again; skip Memory Tracker unless a cheap page-object path already exists.

## Environment Availability

> Step 2.6: SKIPPED (no external dependencies — code/config-only; uses existing Nix/`pnpm` frontend + Cypress stack).

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| In-repo Vue/Vitest/Cypress | Implementation + tests | ✓ | pinned in frontend | — |
| New npm packages | — | N/A | — | — |

**Missing dependencies with no fallback:** none  
**Missing dependencies with fallback:** none

## Validation Architecture

> `workflow.nyquist_validation` is enabled in `.planning/config.json`.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.10 (browser) + Cypress E2E |
| Config file | frontend Vitest config (existing) |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm frontend:test` (unit); targeted E2E via `cypress run --spec` for accidental_match / overlap_try_again |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AMR-08 | CTA + append wiki-link + `updateTextField` | unit | `pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | ✅ extend existing |
| AMR-08 | Util wiki-link shape | unit | util spec (already green) | ✅ `appendOverlapWikiLinkToNoteContent.spec.ts` |
| AMR-08 | Resolve → Add as overlapped E2E | e2e | targeted Cypress on `accidental_match_reveal.feature` | ❌ Wave 0 — add scenario + page-object |
| AMR-09 | No try-again / still ACCIDENTAL_MATCH after declare | unit | same Vitest file | ❌ Wave 0 — add cases |
| AMR-09 | No try-again E2E; overlap uncoupled | e2e | accidental_match + keep `overlap_try_again.feature` green | ❌ / ✅ (overlap exists) |
| AMR-07 reuse | Hide CTA when readonly/unloaded | unit | same Vitest file (mirror Build a link cases) | ❌ Wave 0 — add parallel asserts |

### Sampling Rate

- **Per task commit:** targeted Vitest file above
- **Per wave merge:** Vitest file green; Wave 2 add targeted Cypress spec
- **Phase gate:** Vitest green + targeted E2E green; `overlap_try_again` still green; no full E2E suite unless CI requires

### Wave 0 Gaps

- [ ] Extend `AnsweredSpellingQuestionAccidentalMatch.spec.ts` (or capability-named sibling) for CTA visibility, wiki-link write, no try-again, readonly/unloaded gates
- [ ] Page-object helper for Add as overlapped + Gherkin scenario (extend reveal feature preferred)
- [ ] None for frameworks — Vitest/Cypress/`makeMe` already present
- [ ] Util tests already cover token shape — do not re-test util exhaustively at dialog boundary; assert wiring + negative chrome

## Security Domain

> `security_enforcement` enabled (ASVS level 1).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (reuse session) | Existing app auth |
| V3 Session Management | no | — |
| V4 Access Control | yes | Client hide on `notebookRealm.readonly`; backend enforces write authz on content update |
| V5 Input Validation | yes | `appendOverlapWikiLinkToNoteContent` / `authoredAliasesValidation` / backend frontmatter rules |
| V6 Cryptography | no | — |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Writing overlap into wrong note | Tampering | Always mutate **reviewed** note id from dialog props; match only supplies wiki-link target |
| Readonly notebook bypass via UI | Elevation | Hide CTA when `readonly === true`; server still rejects unauthorized edits |
| XSS via crafted title in wiki-link | Tampering | Existing content pipeline / markdown rendering; no new HTML sink |
| Accidental schedule undo / reclaim | Elevation of privilege over grade | Do not call re-grade/contest APIs; content-only (D-06) |

## Sources

### Primary (HIGH confidence)

- `.planning/phases/11-add-as-overlapped-note/11-CONTEXT.md` — locked D-01..D-11
- `.planning/research/{ARCHITECTURE,STACK,PITFALLS,FEATURES,SUMMARY}.md` — v1.2 declare path
- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` — gate + list/link host
- `frontend/src/components/recall/AccidentalMatchResolveRow.vue` — Build a link CTA pattern
- `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` — Phase 10 util
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — `updateTextField` compose pattern
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — outcome chrome
- `frontend/src/store/StoredApiCollection.ts` — `updateTextField` signature
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — ACCIDENTAL_MATCH vs OVERLAP
- Phases 09–10 CONTEXT/RESEARCH — Build a link + util precedents

### Secondary (MEDIUM confidence)

- Context7 `/vuejs/docs` — script-setup event handlers / async methods (no new pattern required; in-repo offer is authoritative)

### Tertiary (LOW confidence)

- None material

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — zero new packages; pins verified in `frontend/package.json`
- Architecture: HIGH — seams Read this session; CONTEXT locks path
- Pitfalls: HIGH — milestone Pitfalls 4–5 + ADR 0003 verified against code

**Research date:** 2026-08-05  
**Valid until:** 2026-09-04 (30 days — stable in-repo UI wiring)
