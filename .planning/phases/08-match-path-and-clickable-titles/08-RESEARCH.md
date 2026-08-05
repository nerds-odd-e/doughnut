# Phase 8: Match path and clickable titles - Research

**Researched:** 2026-08-05
**Domain:** Vue recall resolve-dialog match identity (path breadcrumb + clickable title)
**Confidence:** HIGH

## Summary

Phase 8 is a **frontend Behavior** slice on top of the Phase 7 resolve-dialog shell. `AccidentalMatchResolveDialog` today lists plain `matched.title` text from `NoteTopology[]`. Locked decisions require keeping that topology shape (D-01), hydrating each match via `getNoteRealmRefAndLoadWhenNeeded` (D-02), rendering clickable title first + `BreadcrumbWithCircle` under it (D-03/D-04), extracting a thin per-match row under `recall/` (D-05), and deferring AMR-05 reopen polish to Phase 12 (D-07).

All required seams already exist in-repo: `NoteTitleWithLink` → `noteShowLocation`, `BreadcrumbWithCircle` ← `notebookRealm` + `ancestorFolders`, and the same store hydrate used by `MatchedNoteLinkOffer`. Modal already closes on `route.fullPath` change, so title navigation dismisses the dialog by design. Zero new npm packages. Vitest must seed or per-id-mock `NoteRealm` fixtures so path asserts are deterministic; E2E can assert notebook path identity within the existing same-notebook accidental-match fixture without expanding into navigate-and-reopen.

**Primary recommendation:** Extract `AccidentalMatchResolveRow` under `frontend/src/components/recall/`; each row calls `getNoteRealmRefAndLoadWhenNeeded(matched.id)` on mount, always renders `NoteTitleWithLink`, and mounts `BreadcrumbWithCircle` only when the realm is present — leave list host + CTA ownership in `AccidentalMatchResolveDialog` / `AnsweredSpellingQuestion`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
### Path data source
- **D-01:** Keep `answeredQuestion.matchedNotes` as `NoteTopology[]` (id + title). Do **not** widen `NoteTopology` or enrich the grade response for path. — **Reversibility:** reversible
- **D-02:** Load each match’s `NoteRealm` with existing `getNoteRealmRefAndLoadWhenNeeded(matched.id)` (same seam as `MatchedNoteLinkOffer`). Render path from `notebookRealm` + `ancestorFolders`. — **Reversibility:** reversible

### Path chrome and row layout
- **D-03:** Per row: clickable **title first**, notebook **breadcrumb under** it (title = primary identity; path = disambiguator). Never mount `NoteShow` / note body in the dialog. — **Reversibility:** reversible
- **D-04:** Use existing `BreadcrumbWithCircle` once the match realm is available (`notebookRealm` + `ancestorFolders`). Do not add a new path/breadcrumb package or a plain-string-only notebook label. — **Reversibility:** reversible
- **D-05:** Extract a thin per-match row component under `frontend/src/components/recall/` that owns realm hydrate + title + breadcrumb (dialog stays the list host). Sets up Phase 9 per-row actions without nested `PopButton`. — **Reversibility:** reversible

### Title navigation boundary
- **D-06:** Make each match title a `NoteTitleWithLink` (`noteShowLocation`) so click navigates to that note. Allow leaving recall; existing Modal route-change close applies. — **Reversibility:** reversible
- **D-07:** Do **not** implement AMR-05 reopen-after-return guarantees in this phase. Minimum bar: Resolve CTA remains available if the answered result is still mounted when the user returns. Full reopen / remount polish is Phase 12. — **Reversibility:** reversible

### Hydrate timing and loading UX
- **D-08:** Start realm hydrate when the dialog body mounts (on open), not before the CTA click and not blocked on all paths before show. Title is visible/clickable immediately from `NoteTopology`; breadcrumb appears when that row’s realm arrives (brief empty/missing path until then is OK). — **Reversibility:** reversible
- **D-09:** No skeleton/spinner requirement beyond existing store-load behavior; do not block the whole list on the slowest match. — **Reversibility:** reversible

### Test coverage for this phase
- **D-10:** Extend Vitest around the resolve dialog / answered spelling accidental-match boundary: each row shows title + path once realm fixtures are present; titles are links toward the match note; still no note body / peek. Prefer capability-named tests; use `makeMe` realm fixtures. — **Reversibility:** reversible
- **D-11:** Update accidental-match E2E (or page object) so dialog rows assert path identity + clickable title where fixtures already provide distinct notebooks/paths. Do not expand this phase into full AMR-05 navigate-away-and-reopen E2E (Phase 12). Keep overlap E2E uncoupled. — **Reversibility:** reversible

### Claude's Discretion
- Exact row component name and `data-testid`s (prefer capability names like `resolve-match-row-*`, path/breadcrumb testids).
- Visual density / DaisyUI breadcrumb classes inside the dialog list.
- Whether breadcrumb folder segments are clickable (follow `BreadcrumbWithCircle` defaults) vs display-only — prefer existing component behavior.

### Deferred Ideas (OUT OF SCOPE)
- Build a link / readonly unload gates — Phase 9 (AMR-06, AMR-07)
- Overlap alias append util — Phase 10 (Structure)
- Add as overlapped note — Phase 11 (AMR-08, AMR-09)
- Title navigate, reopen resolve, full E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed
- API enrichment of `matchedNotes` with path — only if later product needs history path without N× `showNote`

None — discussion stayed within phase scope (auto mode)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AMR-04 | Resolve dialog lists each matched note with a clickable title and notebook path/breadcrumb only (no note body / peek) | Pattern 2 client realm hydrate + `NoteTitleWithLink` + `BreadcrumbWithCircle` in thin row; Vitest/E2E map under Validation Architecture |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Match list host (titles + paths together) | Browser / Client | — | Presentation only; grade payload already on answered result |
| Path / breadcrumb identity | Browser / Client | API / Backend (`showNote`) | Path lives on `NoteRealm`; client loads via existing store; no OpenAPI enrichment (D-01) |
| Clickable title → note page | Browser / Client | — | `NoteTitleWithLink` + vue-router; Modal route-close is local |
| Realm cache hydrate | Browser / Client | API / Backend | `StoredApiCollection.getNoteRealmRefAndLoadWhenNeeded` → `NoteController.showNote` |
| Grading / `matchedNotes` shape | API / Backend | — | Unchanged; do not touch SRS or topology DTO |
| Reopen after title navigate (AMR-05) | Browser / Client | — | Deferred Phase 12 |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue | 3.5.40 `[VERIFIED: frontend/package.json]` | Row SFC composition | Existing frontend runtime |
| vue-router | 5.2.0 `[VERIFIED: frontend/package.json]` | Title `router-link` via `noteShowLocation` | `NoteTitleWithLink` already uses it; Modal watches `route.fullPath` |
| DaisyUI | 5.7.15 `[VERIFIED: frontend/package.json]` | `daisy-breadcrumbs` via `BasicBreadcrumb` | In-repo breadcrumb chrome |
| Vitest | 4.1.10 `[VERIFIED: frontend/package.json]` | Unit tests for dialog/row | Existing browser-mode suite |

### Supporting (reuse — do not install)

| Library / seam | Version | Purpose | When to Use |
|----------------|---------|---------|-------------|
| `NoteTitleWithLink` | in-repo | Clickable match title | Every resolve row (D-06) |
| `BreadcrumbWithCircle` | in-repo | Notebook + folder path | When `NoteRealm` present (D-04) |
| `getNoteRealmRefAndLoadWhenNeeded` | `StoredApiCollection` | Path source | Per-row setup (D-02/D-08) |
| `useStorageAccessor` | in-repo | Access `storedApi()` | Same as `MatchedNoteLinkOffer` |
| `noteShowLocation` | in-repo | `{ name: "noteShow", params: { noteId } }` | Already used by `NoteTitleWithLink` |
| `makeMe.aNoteRealm` | doughnut-test-fixtures | Realm fixtures + optional `inFolder` / `ancestorFolders` | Vitest path asserts (D-10) |
| Cypress + Cucumber | existing | E2E path + link asserts | Extend page object (D-11) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Client `NoteRealm` hydrate | Enrich `matchedNotes` OpenAPI | Locked out by D-01; Structure cost for no Phase 8 need |
| `BreadcrumbWithCircle` | Plain notebook string label | Locked out by D-04 |
| Per-row progressive breadcrumb | Vue `<Suspense>` wrapping whole list | Blocks list on slowest match — contradicts D-08/D-09; Suspense is for unified fallback, not progressive rows `[CITED: https://vuejs.org/guide/built-ins/suspense.html]` |
| Keep dialog open across title nav | `prevent` on title click / history state | Blocks inspect-matched-note goal; AMR-05 is Phase 12 |

**Installation:**

```bash
# No new packages for Phase 8.
```

**Version verification:** Versions read from `frontend/package.json` this session. No registry installs required.

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A — reuse-only |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
┌─ AnsweredSpellingQuestion ─────────────────────────────────────────┐
│  ACCIDENTAL_MATCH alert                                            │
│  PopButton[data-testid=resolve-accidental-match]                   │
│       │ open (v-if Modal)                                          │
│       ▼                                                            │
│  AccidentalMatchResolveDialog[data-testid=accidental-match-resolve-dialog]
│       │ v-for matchedNotes: NoteTopology { id, title }             │
│       ▼                                                            │
│  AccidentalMatchResolveRow (NEW)                                   │
│       │ setup: getNoteRealmRefAndLoadWhenNeeded(matched.id)        │
│       ├─ immediate: NoteTitleWithLink(noteTopology)                │
│       │              → router-link noteShowLocation(id)            │
│       └─ when realm: BreadcrumbWithCircle(                         │
│                        notebookRealm, ancestorFolders)             │
└────────────────────────────────────────────────────────────────────┘
         │ title click → route.fullPath change
         ▼
   Modal watch → close_request → PopButton show=false
         │
         ▼
   noteShow page (user left recall; CTA reopen = Phase 12)
```

### Recommended Project Structure

```
frontend/src/components/recall/
├── AccidentalMatchResolveDialog.vue   # MODIFY: list host → use row component
├── AccidentalMatchResolveRow.vue      # NEW: hydrate + title + breadcrumb
├── AnsweredSpellingQuestion.vue       # unchanged for Phase 8 (still hosts PopButton)
└── MatchedNoteLinkOffer.vue           # reference hydrate pattern only (Phase 9)

frontend/tests/components/recall/
├── AnsweredSpellingQuestionAccidentalMatch.spec.ts  # EXTEND: path + link asserts
└── answeredSpellingQuestionTestSupport.ts           # EXTEND: seedRealms / distinct notebook names

e2e_test/start/pageObjects/AnsweredQuestionPage.ts   # EXTEND: path + clickable title in dialog
e2e_test/features/recall/accidental_match_reveal.feature  # optional wording; prefer page-object change
```

### Pattern 1: Thin resolve row owns hydrate (recommended name)

**What:** `AccidentalMatchResolveRow` takes one `NoteTopology`, hydrates realm in `setup`, renders title always and breadcrumb when ready.  
**When to use:** Always for Phase 8 (D-05). Name is discretion; `AccidentalMatchResolveRow` matches Architecture research and Phase 9 action slot readiness.  
**Example:**

```vue
<!-- Recommended shape — compose verified seams -->
<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"

const props = defineProps<{ matched: NoteTopology }>()
const storageAccessor = useStorageAccessor()
const matchRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matched.id)
</script>

<template>
  <li :data-testid="`resolve-match-row-${matched.id}`">
    <NoteTitleWithLink :note-topology="matched" />
    <div
      v-if="matchRealmRef"
      :data-testid="`resolve-match-path-${matched.id}`"
    >
      <BreadcrumbWithCircle
        :ancestor-folders="matchRealmRef.ancestorFolders ?? []"
        :notebook-realm="matchRealmRef.notebookRealm"
      />
    </div>
  </li>
</template>
```

Hydrate call signature `[VERIFIED: frontend/src/store/StoredApiCollection.ts:230-234]`:

```typescript
getNoteRealmRefAndLoadWhenNeeded(noteId: Doughnut.ID) {
  const result = this.storage.refOfNoteRealm(noteId)
  if (!result.value) this.loadNote(noteId)
  return result
}
```

### Pattern 2: Dialog mounts body only when open (hydrate timing)

**What:** `PopButton` uses `v-if="show"` on `Modal`, so row `setup` runs on open — satisfying D-08 without an explicit `onMounted` prefetch before CTA.  
**When to use:** Always; do not preload match realms on accidental-match result mount.  
**Verified:** `[VERIFIED: frontend/src/components/commons/Popups/PopButton.vue:16-24]` — `Modal` is `v-if="show"`.

### Pattern 3: Title navigation closes dialog

**What:** `NoteTitleWithLink` is a `router-link` to `noteShowLocation(id)`. `Modal` watches `route.fullPath` and emits `close_request`.  
**When to use:** Always (D-06). Do not prevent navigation to keep the dialog open.  
**Verified quotes:**

`NoteTitleWithLink` `[VERIFIED: frontend/src/components/notes/NoteTitleWithLink.vue:1-8]`:

```vue
<router-link
  :to="noteShowLocation(noteTopology.id)"
  class="no-underline"
>
  <NoteTitleComponent v-bind="{ noteTopology }" />
</router-link>
```

`noteShowLocation` `[VERIFIED: frontend/src/routes/noteShowLocation.ts:3-9]`:

```typescript
export function noteShowLocation(noteId: number): RouteLocationNamedRaw {
  return {
    name: "noteShow",
    params: {
      noteId: String(noteId),
    },
  }
}
```

`Modal` route close `[VERIFIED: frontend/src/components/commons/Modal.vue:74-78]`:

```typescript
watch(
  () => route.fullPath,
  () => {
    emit("close_request")
  }
)
```

### Anti-Patterns to Avoid

- **Mounting `NoteShow` / peek in the dialog:** Locked anti-feature; AMR-04 is identity only.
- **Widening `NoteTopology` or grade DTO for path:** Violates D-01; path fields are on `NoteRealm` `[VERIFIED: packages/generated/doughnut-backend-api/types.gen.ts:214-238]` — `NoteRealm` has `notebookRealm` + `ancestorFolders?`; `NoteTopology` is `id` + `title` (+ timestamps).
- **Blocking the list until all realms load / Suspense around the list:** Violates D-08/D-09.
- **Nested `PopButton` for future Build a link:** Phase 9 concern; row extract must leave room for in-dialog steps, not nested modals.
- **Implementing AMR-05 navigate-and-reopen E2E now:** Phase 12 (D-07/D-11).
- **Plain-string notebook label instead of `BreadcrumbWithCircle`:** Violates D-04.
- **Changing ACCIDENTAL_MATCH / OVERLAP grading or SRS:** UI-only; ADR 0003 is still Proposed (not Accepted) but CONTEXT + milestone research forbid schedule changes — trail as product constraint.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Notebook/folder path chrome | Custom path string / new breadcrumb npm | `BreadcrumbWithCircle` | Already encodes bazaar/circle/notebook + folder links |
| Clickable note title | Custom `<a @click>` / `router.push` wrapper | `NoteTitleWithLink` | Correct `noteShow` route shape |
| Path data for matches | OpenAPI enrichment of `matchedNotes` | `getNoteRealmRefAndLoadWhenNeeded` | Same seam as link offer; zero backend Structure |
| Dialog close on navigate | Custom beforeRouteLeave in resolve dialog | Existing `Modal` `route.fullPath` watch | Already closes PopButton-hosted modal |
| Progressive load UX | Vue Suspense list gate | Per-row `v-if="matchRealmRef"` | Independent row reveal (D-09) |

**Key insight:** Phase 8 is composition of three existing widgets behind a list host already shipped in Phase 7 — the risk is fixture/mock determinism and scope creep into AMR-05, not greenfield UI.

## Common Pitfalls

### Pitfall 1: Generic `showNote` mock erases path identity

**What goes wrong:** `beforeEach` uses `mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())` so every match hydrates to a random title/notebook; breadcrumb asserts flake or show wrong names.  
**Why it happens:** Current accidental-match specs only needed titles from `NoteTopology`, not realms.  
**How to avoid:** Prefer `seedRealms: [matchedA, matchedB, …]` via `mountAnsweredSpellingQuestion` (already supported) **or** `mockSdkServiceWithImplementation` keyed by `options.path.note` (pattern in `sidebarTestSupport.ts`). Set distinct `notebookRealm.notebook.name` after `please()` — `NoteRealmBuilder.do()` defaults notebook name to note title `[VERIFIED: packages/doughnut-test-fixtures/src/NoteRealmBuilder.ts:96-99]`.  
**Warning signs:** Path assert fails intermittently; all rows show the same notebook name as the generic mock.

### Pitfall 2: Treating empty breadcrumb before hydrate as a bug

**What goes wrong:** Tests or implementers add a list-level spinner / wait-for-all before rendering titles.  
**Why it happens:** Instinct to avoid “incomplete” UI.  
**How to avoid:** Title must render from topology immediately (D-08); path optional until realm arrives (D-09). Assert path **after** `flushPromises` + seeded realm or resolved mock.

### Pitfall 3: Accidental NoteShow / body peek regression

**What goes wrong:** Reusing NoteShow toolbar breadcrumb patterns pulls note body into the dialog.  
**Why it happens:** `NoteShow` already composes `BreadcrumbWithCircle`.  
**How to avoid:** Only import `BreadcrumbWithCircle` + `NoteTitleWithLink`; keep asserting no matched `note-show-stub` / no body content in dialog; never mount `NoteShow` for matches.

### Pitfall 4: Scope creep into AMR-05 reopen E2E

**What goes wrong:** Phase spends time on navigate-away → return → reopen.  
**Why it happens:** Pitfall 6 in milestone research + AMR-05 wording sit next to AMR-04.  
**How to avoid:** Phase 8 E2E: assert path text + title is a link (href/`router-link` toward match) inside dialog; stop before leaving recall. Phase 12 owns full round-trip.

### Pitfall 5: E2E fixture has one notebook only

**What goes wrong:** Expecting distinct notebook labels when background creates all notes under `"English practice"`.  
**Why it happens:** D-11 says “where fixtures already provide distinct notebooks/paths.” Current feature uses one notebook `[VERIFIED: e2e_test/features/recall/accidental_match_reveal.feature:10-14]`.  
**How to avoid:** Assert path identity as notebook name `"English practice"` (and optional folder trail if present) inside the resolve dialog; do **not** require a second notebook for Phase 8 unless product asks. Distinct notebooks remain a Vitest strength via mutated realm fixtures.

### Pitfall 6: Breadcrumb folder clicks leave recall mid-resolve

**What goes wrong:** User clicks a folder segment in breadcrumb and leaves recall (same as title nav).  
**Why it happens:** `BasicBreadcrumb` folder segments are `router-link`s when `breadcrumbNotebookId` is set `[VERIFIED: frontend/src/components/commons/BasicBreadcrumb.vue:9-16]`.  
**How to avoid:** Accept existing `BreadcrumbWithCircle` behavior (CONTEXT discretion). Do not disable links unless product later requires display-only path.

## Code Examples

### Current dialog (titles-only baseline)

`[VERIFIED: frontend/src/components/recall/AccidentalMatchResolveDialog.vue:1-13]`:

```vue
<ul
  class="flex flex-col gap-2"
  data-testid="accidental-match-resolve-dialog"
>
  <li
    v-for="matched in matchedNotes"
    :key="matched.id"
    :data-testid="`resolve-match-row-${matched.id}`"
  >
    {{ matched.title }}
  </li>
</ul>
```

### MatchedNoteLinkOffer hydrate reference

`[VERIFIED: frontend/src/components/recall/MatchedNoteLinkOffer.vue:42-47]`:

```typescript
const storageAccessor = useStorageAccessor()

const reviewedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.reviewedNoteId)
const matchedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matchedNoteId)
```

### BreadcrumbWithCircle props

`[VERIFIED: frontend/src/components/toolbars/BreadcrumbWithCircle.vue:45-53]`:

```typescript
defineProps({
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  notebookRealm: {
    type: Object as PropType<NotebookRealm>,
    required: true,
  },
})
```

### Vitest seed pattern (already in test support)

`[VERIFIED: frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts:47-50]`:

```typescript
if (options.seedRealms) {
  for (const realm of options.seedRealms) {
    useStorageAccessor().value.refreshNoteRealm(realm)
  }
}
```

Recommended Phase 8 test delta (capability-named; focused asserts):

```typescript
// After opening dialog with seedRealms that have distinct notebook names:
const row = document.body.querySelector('[data-testid="resolve-match-row-10"]')
expect(row?.querySelector("a")?.getAttribute("href")).toMatch(/10/) // or noteShow path
expect(
  document.body.querySelector('[data-testid="resolve-match-path-10"]')?.textContent
).toContain("Notebook Alpha")
expect(document.body.querySelector('[data-testid="note-show-stub"]')).toBeNull() // in dialog — or assert dialog has no note body
```

Prefer one canonical test for “title + path + link shape”; siblings only assert deltas (empty matchedNotes / dismiss already covered).

### E2E page-object extension point

`[VERIFIED: e2e_test/start/pageObjects/AnsweredQuestionPage.ts:84-88]`:

```typescript
cy.findByTestId('resolve-accidental-match').click()
cy.findByTestId('accidental-match-resolve-dialog')
  .should('be.visible')
  .and('contain.text', matchedNoteTitle)
```

Extend here: within dialog, assert notebook path text (e.g. `"English practice"`) and that the matched title is inside a clickable link — without clicking through to noteShow / return (Phase 12).

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Stacked matched `NoteShow` bodies | Compact Resolve dialog (titles only) | Phase 7 (v1.2) | Reviewed note stays primary |
| Titles-only interim rows | Title link + `BreadcrumbWithCircle` via realm hydrate | Phase 8 (this) | AMR-04 identity |
| Client hydrate | (optional later) API-enriched matched notes | Deferred | Only if history/offline path fidelity required |

**Deprecated/outdated:**

- Stacked match bodies / `matched-notes-section` as reveal surface — replaced by dialog
- Vue Suspense as default for this list — wrong tool for progressive per-row path

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Recommended row component name `AccidentalMatchResolveRow` is acceptable (CONTEXT left naming to discretion) | Architecture Patterns | Rename-only churn; no behavior risk |
| A2 | Recommended path testid `resolve-match-path-${id}` is acceptable | Code Examples | E2E/unit selector tweak only |
| A3 | Same-notebook E2E path assert (`English practice`) satisfies AMR-04 without a second notebook fixture | Pitfall 5 / Validation | May need a second-notebook E2E later if product insists on cross-notebook disambiguation in Cypress |

**If this table is empty:** — not empty; A1–A3 are discretion/E2E scope assumptions only.

## Open Questions (RESOLVED)

1. **Should Vitest extend `NoteRealmBuilder` with `.notebookName(...)`?** — RESOLVED: Prefer concise helper (or mutate-before-seed) in `answeredSpellingQuestionTestSupport`; only extend `NoteRealmBuilder` if mutation is awkward. Locked by Plan 01 Task 2.
2. **How strong should E2E “clickable” be?** — RESOLVED: Assert visible `a` / href toward match inside the dialog without clicking through to noteShow or reopen round-trip. Locked by Plan 02 Task 1 / D-11; Phase 12 owns leave/return.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | Vitest / frontend tooling | ✓ | v24.5.0 | Nix shell |
| pnpm | Test commands | ✓ | 11.20.0 | `CURSOR_DEV=true nix develop -c …` |
| `pnpm sut` services | E2E | assume running | — | Suggest start if healthcheck fails; do not restart nag |

**Missing dependencies with no fallback:** none for code/unit path  
**Missing dependencies with fallback:** none

Step 2.6: External deps are existing repo toolchain only; no new services.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.10 (browser mode) + Cypress/Cucumber E2E |
| Config file | frontend Vitest config (existing); `e2e_test/config/ci.ts` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm frontend:test` (unit gate); targeted E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AMR-04 | Dialog rows show title + path once realm present; title is link; no body | unit | `pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | ✅ extend existing |
| AMR-04 | Dialog shows path identity + clickable title in E2E | e2e | `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` | ✅ extend page object / feature |
| AMR-05 | Navigate away and reopen | e2e | — | ❌ deferred Phase 12 — do not Wave 0 |

### Sampling Rate

- **Per task commit:** targeted Vitest file above
- **Per wave merge:** same Vitest file + targeted accidental_match_reveal E2E (not full Cypress suite)
- **Phase gate:** Vitest green + targeted E2E green; overlap_try_again remains uncoupled (smoke only if regression fear)

### Wave 0 Gaps

None — existing test infrastructure covers AMR-04; extend specs/page objects rather than create new frameworks. Optional: thin dedicated `AccidentalMatchResolveRow.spec.ts` only if mounting the row alone is clearer than the answered-question boundary — prefer driving through `AnsweredSpellingQuestion` / dialog (small-test style).

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Existing session; no new auth |
| V3 Session Management | no | Unchanged |
| V4 Access Control | yes (indirect) | `showNote` / notebook readonly already enforced server-side; dialog only displays what hydrate returns |
| V5 Input Validation | no new inputs | Match ids come from server-graded `matchedNotes`; no free-text path input |
| V6 Cryptography | no | — |

### Known Threat Patterns for this UI slice

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Client displays note id/title from grade response | Information Disclosure | Only show topologies already returned for this answer; hydrate uses authenticated `showNote` |
| Title link navigates to unauthorized note | Elevation of Privilege | Backend `showNote` / note page authz unchanged; do not invent client-only deep links past API |
| XSS via notebook/folder names in breadcrumb | Tampering | Vue text interpolation in existing breadcrumb components |

`security_enforcement: true` in `.planning/config.json` — no new attack surface beyond composing existing navigable title + breadcrumb.

## Project Constraints (from .cursor/rules/)

| Rule | Directive for this phase |
|------|--------------------------|
| `planning.mdc` | Behavior phase; one observable behavior (AMR-04); stop-safe; ~5 min slices; after phase: Jidoka, post-change-refactor, plan update, commit+push |
| `unit-testing.mdc` | Small tests: drive answered-question / dialog boundary; `makeMe` fixtures; focused assertions; mock only SDK |
| `frontend-testing.mdc` | Vitest browser; `data-testid` selectors; avoid role queries; `mockSdkService`; `withRouter` when links needed |
| `frontend-component.mdc` | DaisyUI `daisy-*` + Tailwind; use existing Modal/PopButton; no new modal libraries |
| `e2e-authoring.mdc` | Targeted `cypress run --spec`; prefer page-object changes; assume `pnpm sut` running |
| `gsd-coexistence.mdc` / `general.mdc` | Nix prefix for tooling; phase numbers only under `.planning/`; capability-named tests |
| `architecture-decisions.mdc` | Do not change SRS/grading; ADR 0003 cited by CONTEXT is Proposed — still treat UI-only constraint as locked for this milestone |

## Sources

### Primary (HIGH confidence)

- In-repo components/tests read this session: `AccidentalMatchResolveDialog.vue`, `MatchedNoteLinkOffer.vue`, `NoteTitleWithLink.vue`, `BreadcrumbWithCircle.vue`, `BasicBreadcrumb.vue`, `PopButton.vue`, `Modal.vue`, `StoredApiCollection.ts`, `NoteRealmBuilder.ts`, Phase 7/8 CONTEXT, `types.gen.ts` NoteRealm/NoteTopology
- `.planning/research/ARCHITECTURE.md` Pattern 2; `STACK.md` zero new libs; `PITFALLS.md` Pitfall 6 (navigate/reopen → Phase 12)
- `frontend/package.json` version pins

### Secondary (MEDIUM confidence)

- Vue Suspense official guide — prefer not blocking progressive sections `[CITED: https://vuejs.org/guide/built-ins/suspense.html]`
- Vue Router dialog community patterns — local modal state does not survive navigation; reopen via CTA `[CITED: web search synthesis]`

### Tertiary (LOW confidence)

- None material to planning; A1–A3 are discretion assumptions, not external claims

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — reuse-only; versions verified in package.json
- Architecture: HIGH — locked CONTEXT + live code seams verified
- Pitfalls: HIGH — fixture/mock and scope-creep risks verified against current tests/E2E

**Research date:** 2026-08-05  
**Valid until:** 2026-09-04 (stable in-repo UI; re-check if Phase 7 dialog/testids change)
