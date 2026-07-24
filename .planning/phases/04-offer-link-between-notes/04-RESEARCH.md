# Phase 4: Offer link between notes - Research

**Researched:** 2026-07-24
**Domain:** Vue 3 frontend reuse of existing add-link UI (`SearchForm` / `LinkInsertionChoice` / `AddRelationshipFinalize`) on the spelling-recall result page; no backend changes.
**Confidence:** HIGH

## Summary

This phase is pure frontend reuse work. All the primitives AM-04 needs already exist in production: `LinkInsertionChoice.vue` (choice buttons), `AddRelationshipFinalize.vue` (relationship-note creation), `buildWikiLinkText` (wiki markdown), and the note-content-frontmatter utilities that already implement "add a wiki-link property" (`parseNoteContentMarkdown` → rows → `composeNoteContentFromPropertyRows`). The backend contract is already sufficient: `AnsweredQuestion.matchedNotes: List<NoteTopology>` and `RecalledNote.noteTopology` give enough note ids to drive everything from the frontend — **no backend change is needed** [VERIFIED: repo source].

The one real hazard this research uncovered (validating D-05's caution) is `useContentCursorInserter` — the wiki/property insertion helper the toolbar's `SearchForm` uses is a **module-level singleton** registered by whichever `NoteEditableContent` last mounted [VERIFIED: repo source]. On the recall result page, `NoteShow` (which internally mounts `NoteEditableContent`) is rendered once for the reviewed note **and once per matched note** — so the singleton would silently point at the *last-mounted* note, not necessarily the reviewed note the CTA intends to write to. **Do not route the property-wiki-link write through this singleton on the recall page.** Instead, write the property directly via the existing `updateTextField` API call (the same mechanism `SearchForm`'s dead-link "link to existing note" path already uses), computed from the reviewed note's already-loaded `Note.content`.

**Primary recommendation:** Build one small, additive frontend feature: (1) a `bareWikiLinkAvailable` prop on `LinkInsertionChoice.vue` (default `true`, so existing callers are unaffected) to hide the cursor-dependent "Insert as a wiki link" button on the recall page; (2) a new small recall-scoped wrapper component that renders `LinkInsertionChoice` (property + relationship only) then `AddRelationshipFinalize`, using notes already loaded into `NoteStorage` by the on-page `NoteShow`s (via `getNoteRealmRefAndLoadWhenNeeded`); (3) a per-matched-note "Link to this note" CTA in `AnsweredSpellingQuestion.vue`, gated on write permission, that opens this wrapper in a `PopButton`/`Modal` pre-seeded with the matched note as target. Extract the existing inline "add wiki-link property to content" logic from `NoteEditableContent.vue` into a shared pure function so the direct-write path and the cursor-based path share one implementation.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Render per-matched-note "Link to this note" CTA | Browser / Client (Vue component) | — | Presentation-only; branches on already-fetched `AnsweredQuestion` |
| Build `Note` (source) / `NoteSearchResult` (target) for the add-link UI | Browser / Client | — | Both notes are already loaded into `NoteStorage` by the on-page `NoteShow`s; no new backend endpoint needed |
| Offer link-type choice (property wiki vs relationship) | Browser / Client | — | Reuses `LinkInsertionChoice.vue` unchanged in structure, additive prop only |
| Write wiki-link-as-property to reviewed note | Browser / Client → API / Backend | — | Client composes new Markdown content, then calls the existing `TextContentController.updateNoteContent` via `storedApi().updateTextField` (already-existing backend endpoint; no new endpoint) |
| Create relationship note | Browser / Client → API / Backend | — | Unchanged: `AddRelationshipFinalize.vue` already calls `createRootNoteAtNotebook` |
| Write-permission gate for the CTA | Browser / Client | — | Same client-side `notebookRealm.readonly` check `NoteShow`/`NoteToolbar` already use; server-side authorization on the mutating endpoints is the real enforcement (already in place) |
| Matched-note readability / IDOR filtering | API / Backend | — | Already done in Phase 2/3 (`findAllAccidentalMatches`); Phase 4 adds no new read path |

## Standard Stack

No new libraries. This phase reuses existing repo code exclusively (Vue 3 `<script setup>`, existing `@generated/doughnut-backend-api` client, existing `yaml` parsing utilities already in the codebase).

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Reuse `LinkInsertionChoice` + `AddRelationshipFinalize` via a thin recall-scoped wrapper | Add a `initialSelectedSearchResult` prop directly to `SearchForm.vue` and reuse it unmodified | `SearchForm`'s `onInsertWikiLinkAsProperty` hard-codes the `useContentCursorInserter` singleton path, which is unreliable with multiple `NoteShow` mounted at once on the recall page (see Pitfall 1). Reusing `SearchForm` as-is would silently write to the wrong note in some render orders. A thin wrapper that calls `LinkInsertionChoice`/`AddRelationshipFinalize` directly (as `SearchForm` itself does) and does its own direct-content-write avoids this without touching `SearchForm`'s cursor-based logic at all. |
| Direct `updateTextField` write for the property-wiki-link | Force the reviewed note into an editor session (asMarkdown/rich edit) so the existing cursor inserter works | D-05 explicitly rejects "forcing the user into a separate edit session mid-recall"; the recall answer page has no active editor and no natural cursor context. |

## Package Legitimacy Audit

Not applicable — no new external packages are introduced by this phase. All work reuses existing repo dependencies (`vue`, `yaml`, generated OpenAPI client).

## Architecture Patterns

### System Architecture Diagram

```
AnsweredSpellingQuestion.vue (recall result page)
  ├─ NoteShow(reviewedNoteId)   ─┐  each mounts NoteRealmLoader → storedApi().loadNoteRealm(id)
  │                              │  → populates NoteStorage.refOfNoteRealm(id)
  ├─ matched-notes-section
  │    └─ per matched note:
  │         ├─ NoteShow(matched.id) ─┘  (same load mechanism, already exists from Phase 3)
  │         └─ [NEW] "Link to this note" CTA (v-if write-permission gate passes)
  │                └─ PopButton → Modal
  │                     └─ [NEW] MatchedNoteLinkOffer.vue
  │                          ├─ reads storedApi().getNoteRealmRefAndLoadWhenNeeded(reviewedId) → Note (source)
  │                          ├─ reads storedApi().getNoteRealmRefAndLoadWhenNeeded(matchedId) → builds NoteSearchResult (target)
  │                          ├─ renders LinkInsertionChoice (bareWikiLinkAvailable=false)
  │                          │     ├─ "Add wiki link as a new property"  → direct write path (NEW, no cursor):
  │                          │     │      parseNoteContentMarkdown(sourceNote.content)
  │                          │     │      → append property row with buildWikiLinkText(target)
  │                          │     │      → composeNoteContentFromPropertyRows(...)
  │                          │     │      → storedApi().updateTextField(sourceNote.id, "edit content", composed)
  │                          │     │           → TextContentController.updateNoteContent (existing backend endpoint)
  │                          │     └─ "Add a new relationship note" → renders AddRelationshipFinalize (UNCHANGED)
  │                          │            → NotebookController.createNoteAtNotebookRoot (existing backend endpoint)
  │                          └─ on success/closeDialog → PopButton closer() closes the Modal; stays on recall result page (D-07, no navigation)
```

A reader can trace the primary use case end to end: click CTA on a matched-note row → dialog opens already past search (target pre-set) → choose property or relationship → existing backend write endpoint → dialog closes → same recall result page still showing both notes.

### Recommended Project Structure

```
frontend/src/components/
├── links/
│   └── LinkInsertionChoice.vue          # MODIFIED: add optional `bareWikiLinkAvailable` prop (default true)
├── recall/
│   ├── AnsweredSpellingQuestion.vue     # MODIFIED: per-matched-note CTA + write-permission gate
│   └── MatchedNoteLinkOffer.vue         # NEW: recall-scoped choice+finalize wrapper (name is a suggestion; keep capability-named)
└── utils/
    └── noteContentPropertyRows.ts        # MODIFIED: extract shared `appendWikiLinkPropertyRow(content, linkText)` pure helper,
                                           # reused by NoteEditableContent.vue's registerWikiPropertyInserter AND the new direct-write path
```

### Pattern 1: Fetch source/target from already-loaded `NoteStorage` (no new API calls, D-04)

**What:** `AnsweredQuestion.matchedNotes` is `List<NoteTopology>` (id + title only — **no `notebookId`**). `NoteSearchResult` (what `LinkInsertionChoice`/`AddRelationshipFinalize` need) requires `notebookId` (+ optional `notebookName`). Rather than expanding the wire contract, use the fact that both the reviewed note and every matched note are **already being loaded into the shared `NoteStorage` cache** by the `NoteShow` components already rendered on this same page (Phase 3).

**When to use:** Whenever the recall page needs a full `Note` or `NoteSearchResult`-shaped value for a note id it already displays via `NoteShow`.

**Example:**
```typescript
// Source: frontend/src/store/StoredApiCollection.ts (existing), frontend/src/components/notes/NoteRealmLoader.vue (existing pattern)
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"

const storageAccessor = useStorageAccessor()

// Auto-loads if not already cached; NoteShow for this id already triggers the same load.
const reviewedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(reviewedNoteId)

const matchedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId)

const matchedSearchResult = computed<NoteSearchResult | undefined>(() => {
  const realm = matchedRealmRef.value
  if (!realm) return undefined
  return {
    noteTopology: realm.note.noteTopology,
    notebookId: realm.notebookRealm.notebook.id,
    notebookName: realm.notebookRealm.notebook.name,
  }
})
```

### Pattern 2: Write a property wiki-link without a cursor (reuse of the dead-link "link to existing note" pattern)

**What:** `SearchForm.vue`'s `onLinkDeadLinkToNote` already writes note content directly (no editor, no cursor) via `storageAccessor.value.storedApi().updateTextField(note.id, "edit content", newContent)`. Reuse the exact same call, but build `newContent` with the same row-composition logic `NoteEditableContent.vue`'s `registerWikiPropertyInserter.insert` already uses.

**When to use:** Recall page "Add wiki link as a new property" action (no live editor, no cursor available).

**Example:**
```typescript
// Source: frontend/src/components/notes/core/NoteEditableContent.vue (existing logic, to be extracted as a shared pure fn)
import {
  parseNoteContentMarkdown,
  sortedPropertyRowsFromNoteProperties,
  propertyRowWithScalar,
  validatePropertyRowsForRichEdit,
  composeNoteContentFromPropertyRows,
} from "@/utils/noteContentFrontmatter"

function appendWikiLinkPropertyRow(content: string, linkText: string): string | undefined {
  const parsed = parseNoteContentMarkdown(content ?? "")
  if (!parsed.ok) return undefined
  const rows = [
    ...sortedPropertyRowsFromNoteProperties(parsed.properties),
    propertyRowWithScalar("", linkText),
  ]
  if (!validatePropertyRowsForRichEdit(rows).ok) return undefined
  return composeNoteContentFromPropertyRows(rows, parsed.body)
}

// Usage in the new wrapper:
const linkText = buildWikiLinkText(matchedSearchResult, { notebookId: sourceNotebookId })
const composed = appendWikiLinkPropertyRow(sourceNote.content ?? "", linkText)
if (composed !== undefined) {
  await storageAccessor.value.storedApi().updateTextField(sourceNote.id, "edit content", composed)
  emit("closeDialog")
}
```

Recommend extracting this function into `frontend/src/utils/noteContentPropertyRows.ts` and having `NoteEditableContent.vue`'s `registerWikiPropertyInserter.insert` call it too (for the markdown-editor branch it still needs the caret math around the composed string — keep that part local, but the parse→rows→validate→compose part is identical and worth deduplicating).

### Pattern 3: Additive prop to hide the cursor-dependent primary button (D-05)

**What:** `LinkInsertionChoice.vue`'s primary button ("Insert as a wiki link" when no dead-link payload) depends on the same cursor-inserter singleton and is not viable on the recall page. Add an optional prop, defaulting to today's behavior, so every existing call site (`SearchForm.vue`) is unaffected.

**Example:**
```typescript
// frontend/src/components/links/LinkInsertionChoice.vue
const props = defineProps<{
  targetNoteTopology: NoteTopology
  wikiPropertyOptionAvailable?: boolean
  deadLinkDisplayText?: string
  bareWikiLinkAvailable?: boolean // NEW, default true — recall wrapper passes false
}>()
```
```html
<button
  v-if="bareWikiLinkAvailable !== false || deadLinkDisplayText"
  class="daisy-btn daisy-btn-primary"
  @click="onPrimaryClick"
>
  {{ primaryLabel }}
</button>
```

### Pattern 4: Write-permission gate for the CTA (D-06)

**What:** Mirror `NoteShow.vue`'s existing readonly check exactly (same class of gate the toolbar's link button already uses):
```typescript
// Source: frontend/src/components/notes/NoteShow.vue (existing pattern, replicate — do not extract prematurely, only 2 call sites)
const currentUser = inject<Ref<User | undefined>>("currentUser")
const reviewedWritable = computed(() => {
  const realm = reviewedRealmRef.value
  return !!currentUser?.value && !!realm && realm.notebookRealm.readonly !== true
})
```
Show the per-row CTA only when `reviewedWritable.value` is true. Server-side authorization on `updateNoteContent` / `createNoteAtNotebookRoot` remains the actual enforcement boundary; this is a UX gate only.

### Anti-Patterns to Avoid

- **Relying on `useContentCursorInserter` (`canInsertWikiLinkAsProperty` / `insertWikiLinkAsProperty` / `insert`) from the recall page:** it is a module-level singleton bound to whichever `NoteEditableContent` last mounted `onMounted`. With one `NoteShow` per matched note plus one for the reviewed note all mounted simultaneously, the "active" registrant is non-deterministic from the CTA's point of view. Confirmed by reading `frontend/src/composables/useContentCursorInserter.ts` and `frontend/src/components/notes/core/NoteEditableContent.vue` (registers **unconditionally** in `onMounted`, `readonly` prop is not checked before registering). Use the direct `updateTextField` write instead (Pattern 2).
- **Expanding `AnsweredQuestion`/`matchedNotes` to carry `notebookId`:** unnecessary — the notebook id/name for every note shown on this page is already obtainable from `NoteStorage` via the id (Pattern 1). Matches D-04's "no contract change unless proven required" — it is not required.
- **Auto-submitting any link on CTA click:** PROJECT.md and D-03 forbid this. The CTA must always land on the choice step (or, for relationship, the placement/relation-type finalize step) and require an explicit user action to persist.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Wiki-link markdown text (`[[Title]]` / `[[Notebook:Title]]` / `[[Title\|Display]]`) | A new formatter in the recall component | `buildWikiLinkText` (`frontend/src/utils/buildWikiLinkText.ts`) | Already handles cross-notebook qualification and display-text aliasing; used by both wiki-insert and dead-link-resolve paths today |
| Relationship note creation, folder placement, title/markdown composition | New recall-specific relationship logic | `AddRelationshipFinalize.vue` unchanged, given `note` + `targetSearchResult` | Fully self-contained; already handles placement options, relation-type validation, `runWithBlockingApiLoading`, router navigation, and undo-safe soft-deleted-title conflicts |
| Frontmatter property row parse/compose/validate | Ad-hoc string concatenation onto `note.content` | `parseNoteContentMarkdown` / `sortedPropertyRowsFromNoteProperties` / `propertyRowWithScalar` / `validatePropertyRowsForRichEdit` / `composeNoteContentFromPropertyRows` (`frontend/src/utils/noteContentFrontmatter.ts` re-exports) | Handles YAML edge cases (empty-key rows, duplicate keys, alias validation) already exercised by production frontmatter editing |

**Key insight:** every write operation AM-04 needs already has a production code path one level away (dead-link resolve for direct content writes; `AddRelationshipFinalize` for relationships). The phase's actual work is *wiring*, not new persistence logic.

## Common Pitfalls

### Pitfall 1: Module-singleton cursor inserter picks the wrong note when multiple `NoteShow` are mounted
**What goes wrong:** If the recall wrapper (or a future change) calls `insertWikiLinkAsProperty` / `canInsertWikiLinkAsProperty` from `useContentCursorInserter`, it will silently operate on whichever `NoteEditableContent` most recently ran `onMounted` — which, on this page, could be the *last matched note* rendered in the `v-for`, not the reviewed note.
**Why it happens:** `_inserter` / `_wikiPropertyInserter` in `useContentCursorInserter.ts` are plain module-level `ref`s (a global singleton), and `NoteEditableContent.vue` registers unconditionally in `onMounted` regardless of `readonly`.
**How to avoid:** Never call the singleton from the recall page. Always drive the property-wiki-link write directly from the fetched `Note.content` + `updateTextField` (Pattern 2). `LinkInsertionChoice`'s `wikiPropertyOptionAvailable` prop should be computed locally from `parseNoteContentMarkdown(sourceNote.content ?? "").ok`, not from `canInsertWikiLinkAsProperty()`.
**Warning signs:** A Vitest test that mounts more than one `NoteShow`/`NoteEditableContent` and observes a property write landing on the wrong note id; or flaky E2E behavior that depends on matched-note count/order.

### Pitfall 2: Assuming `NoteTopology` carries `notebookId`
**What goes wrong:** Code that tries `matched.notebookId` (from `AnsweredQuestion.matchedNotes: NoteTopology[]`) will not compile / will be `undefined` at runtime — `NoteTopology` is only `{id, title, createdAt?, updatedAt?}` [VERIFIED: repo source, `backend/.../dto/NoteTopology.java` and generated `types.gen.ts`].
**Why it happens:** `NoteTopology` is intentionally a thin id+title shape reused across many contracts; notebook membership was never part of it.
**How to avoid:** Resolve notebook id/name via the already-loaded `NoteRealm` for that id (Pattern 1), not from the topology itself.

### Pitfall 3: CTA rendering before the note realm has loaded
**What goes wrong:** If the write-permission gate or target-builder computed reads `refOfNoteRealm(id).value` before the corresponding `NoteShow`'s `loadNoteRealm` promise resolves, it will be `undefined` on first render.
**Why it happens:** `NoteRealmLoader`'s watcher with `{ immediate: true }` kicks off the load asynchronously; Vue's synchronous render pass for the CTA happens before that promise settles.
**How to avoid:** Guard the CTA (`v-if`) on the computed realm being defined (and use `getNoteRealmRefAndLoadWhenNeeded`, not bare `refOfNoteRealm`, so the CTA's own code path is not dependent on `NoteShow`'s mount order). A brief CTA appearance-after-notes-render is acceptable UX (matches how the matched-notes section itself already appears once notes resolve).

### Pitfall 4: Forgetting the D-03 "never auto-submit" constraint while wiring preselection
**What goes wrong:** A naive "preselect and immediately create" implementation would violate the hard "system never auto-writes a link" success criterion (#3) and PROJECT.md's Out of Scope item.
**Why it happens:** Preselection mechanics (skip search, land on choice) can blur into "just do it" if not careful — e.g., auto-clicking `chooseAddRelationship` and then auto-submitting the relation type.
**How to avoid:** Preselection ends at `LinkInsertionChoice` (or `AddRelationshipFinalize`'s placement form) with the target already filled in; the user must still click a choice button and (for relationship) pick a relation type in the existing `RelationTypeSelect` before anything is persisted. This is exactly how `AddRelationshipFinalize` already behaves — no new auto-submit logic needed.

## Code Examples

### Building the CTA's write-permission gate + target search result (combined)

```typescript
// Source: composition of frontend/src/components/notes/NoteShow.vue (readonly gate)
//         + frontend/src/store/StoredApiCollection.ts (getNoteRealmRefAndLoadWhenNeeded)
import { computed, inject, type Ref } from "vue"
import type { User, NoteSearchResult } from "@generated/doughnut-backend-api"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const currentUser = inject<Ref<User | undefined>>("currentUser")
const storageAccessor = useStorageAccessor()

function useMatchedNoteLinkOffer(reviewedNoteId: number, matchedNoteId: number) {
  const reviewedRealmRef = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(reviewedNoteId)
  const matchedRealmRef = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId)

  const canOfferLink = computed(
    () =>
      !!currentUser?.value &&
      !!reviewedRealmRef.value &&
      reviewedRealmRef.value.notebookRealm.readonly !== true &&
      !!matchedRealmRef.value
  )

  const matchedSearchResult = computed<NoteSearchResult | undefined>(() => {
    const realm = matchedRealmRef.value
    if (!realm) return undefined
    return {
      noteTopology: realm.note.noteTopology,
      notebookId: realm.notebookRealm.notebook.id,
      notebookName: realm.notebookRealm.notebook.name,
    }
  })

  return { canOfferLink, reviewedRealmRef, matchedSearchResult }
}
```

## State of the Art

Not applicable — no framework/library version drift is relevant here; this is same-version reuse of code already in the repository.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The exact wrapper component name/location (`MatchedNoteLinkOffer.vue` under `frontend/src/components/recall/`) is a naming suggestion, not a locked decision. | Recommended Project Structure | Low — cosmetic; planner/implementer can rename freely as long as capability-named (per `.cursor/rules/planning.mdc`) |
| A2 | Extracting `appendWikiLinkPropertyRow` out of `NoteEditableContent.vue` into a shared util is recommended but not strictly required — the direct-write path could instead duplicate ~10 lines locally. | Pattern 2 | Low — if skipped, flag minor duplication for a later `post-change-refactor` pass rather than blocking the phase |

**Risk assessment:** both assumptions are naming/structure preferences with reversible, low-blast-radius impact — neither affects correctness of the AM-04 behavior itself.

## Open Questions (RESOLVED)

1. **Where exactly should the per-row CTA render relative to each matched `NoteShow`?** — **RESOLVED**
   - Locked by UI-SPEC + 04-01-PLAN: inside the existing `matched-note-{id}` wrapper, below `NoteShow`, with `data-testid="link-to-matched-note-{id}"`.

2. **Exact component boundary: one wrapper handling both property-write and relationship-finalize stages, or two separate components?** — **RESOLVED**
   - Locked by 04-01/04-02 PLAN + PATTERNS: one `MatchedNoteLinkOffer.vue` mirroring `SearchForm`'s two-ref choice→finalize pattern (minus search). Wave 1 ships property path only (hide relationship choice until Wave 2 mounts `AddRelationshipFinalize`) so a mid-phase stop does not leave a dead-end button.

## Environment Availability

Skipped — this phase has no new external dependencies (frontend-only reuse of existing repo code; no new services, CLIs, or runtimes).

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest (frontend unit) + Cypress + `@badeball/cypress-cucumber-preprocessor` (E2E) |
| Config file | `frontend/vitest.config.ts` (unit); `e2e_test/cypress.config.ts` (E2E) — existing, no changes needed |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm frontend:test` (unit); `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` (targeted E2E) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AM-04 (criterion 1) | CTA appears under each matched note, offering property/relationship choice | unit (Vitest) | `pnpm frontend:test frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` | ✅ extend existing file |
| AM-04 (criterion 1) | Choice + relationship-finalize flow reuses existing components correctly | unit (Vitest) | `pnpm frontend:test frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` (or chosen wrapper name) | ❌ Wave 0 — new file |
| AM-04 (criterion 2) | Matched note is pre-selected (no search step shown) | unit (Vitest) + E2E | same files above; E2E extends `e2e_test/features/recall/accidental_match_reveal.feature` | ❌ Wave 0 for E2E scenario |
| AM-04 (criterion 3) | No auto-write: content/relationship API only called after explicit user choice + confirm | unit (Vitest) | mock `TextContentController.updateNoteContent` / `NotebookController.createNoteAtNotebookRoot` and assert call count only after the confirming click | ✅ pattern exists in `frontend/tests/links/AddRelationship.spec.ts` |
| D-06 | CTA hidden when reviewed note is readonly for the user | unit (Vitest) | extend `AnsweredSpellingQuestion.spec.ts` with a readonly `notebookRealm` seeded via `useStorageAccessor().value.refreshNoteRealm(...)` | ❌ Wave 0 — new test case |
| D-07 | After success, dialog closes and recall result page is unchanged (no navigation) | unit (Vitest) | assert `closeDialog`/`success` emit closes the modal without router calls | ❌ Wave 0 — new test case |

### Sampling Rate
- **Per task commit:** targeted Vitest file(s) touched by that task.
- **Per wave merge:** `pnpm frontend:test` (full frontend unit suite) + the targeted E2E feature (`accidental_match_reveal.feature`), per `.cursor/rules/planning.mdc` (targeted E2E, not full suite).
- **Phase gate:** both green before `/gsd-verify-work` / human spot-check.

### Wave 0 Gaps
- [ ] `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` (or equivalent wrapper name) — new file, covers choice/property-write/relationship-finalize/D-07 close behavior; reuse mocking patterns from `frontend/tests/links/AddRelationship.spec.ts` (`mockSdkService`, `useStorageAccessor().value.refreshNoteRealm(seedRealm)`, `withCleanStorage()`).
- [ ] New Vitest cases in `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` — CTA visibility (write-permission gate), CTA-per-matched-note count, target pre-selection wiring.
- [ ] E2E scenario(s) appended to `e2e_test/features/recall/accidental_match_reveal.feature` (capability-named, not phase-numbered) — tag `@wip` until green, then untag; add step definitions to `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` (extend `expectAccidentalMatchReveal`'s returned object with e.g. `linkMatchedNoteAsProperty(matchedNoteTitle)` / `linkMatchedNoteAsRelationship(matchedNoteTitle, relationType)`), reusing existing button-label queries (`Add wiki link as a new property`, `Add a new relationship note`) already proven in `e2e_test/start/pageObjects/noteTargetSearchDialog.ts`.
- [ ] No new framework/config install — Vitest and Cypress are already fully configured for these directories.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Unrelated — recall flow already requires an authenticated session |
| V3 Session Management | no | Unchanged |
| V4 Access Control | yes | Client-side CTA visibility gate (D-06, UX-only) backed by **existing server-side authorization** on `TextContentController.updateNoteContent` and `NotebookController.createNoteAtNotebookRoot` — both endpoints already enforce write access; Phase 4 adds no new endpoint and must not weaken those checks |
| V5 Input Validation | yes | Reuse existing `validatePropertyRowsForRichEdit` / `authoredAliasesValidationErrorForPropertyRow` (already used by production frontmatter editing) for the direct-write path; no new unvalidated input surface is introduced (wiki-link text is machine-built via `buildWikiLinkText`, not free user text) |
| V6 Cryptography | no | Not applicable |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR — offering a link CTA that could write to or read from a note the user cannot access | Tampering / Information Disclosure | Already mitigated: matched notes reaching the client are already readability-filtered server-side (Phase 2/3 `findAllAccidentalMatches`); the write CTA additionally checks `notebookRealm.readonly` client-side, and the actual write call is server-authorized regardless of client gating |
| Client trusting a stale/forged `NoteSearchResult.notebookId` for the wiki-link text | Tampering | `notebookId`/`notebookName` are read from the just-loaded `NoteRealm` (server-returned), not from client-editable state, so this is not user-controlled input |

## Sources

### Primary (HIGH confidence — verified by reading the repo directly)
- `frontend/src/components/links/SearchForm.vue`, `LinkInsertionChoice.vue`, `AddRelationshipFinalize.vue` — existing add-link UI and its exact state machine
- `frontend/src/composables/useContentCursorInserter.ts` + `frontend/src/components/notes/core/NoteEditableContent.vue` — singleton cursor-inserter mechanics (source of Pitfall 1)
- `frontend/src/components/notes/NoteShow.vue`, `NoteRealmLoader.vue`, `NoteTextContent.vue` — confirms `NoteEditableContent` mounts unconditionally per `NoteShow`, and the readonly-gate pattern to replicate
- `frontend/src/store/StoredApiCollection.ts`, `NoteStorage.ts`, `useStorageAccessor.ts` — `getNoteRealmRefAndLoadWhenNeeded` / `refOfNoteRealm` / `updateTextField` mechanics
- `frontend/src/utils/noteContentFrontmatter.ts`, `noteContentFrontmatterParse.ts`, `noteContentPropertyRows.ts`, `buildWikiLinkText.ts` — property-row compose/validate utilities and wiki-link text builder
- `packages/generated/doughnut-backend-api/types.gen.ts` (`NoteTopology`, `NoteSearchResult`, `Note`, `AnsweredQuestion`, `RecalledNote`) and `backend/src/main/java/com/odde/doughnut/controllers/dto/{AnsweredQuestion,NoteTopology}.java` — confirms contract shapes and that no backend change is needed
- `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts`, `frontend/tests/links/AddRelationship.spec.ts` — existing test conventions/mocking patterns to extend
- `e2e_test/features/recall/accidental_match_reveal.feature`, `e2e_test/step_definitions/recall.ts`, `e2e_test/start/pageObjects/AnsweredQuestionPage.ts`, `e2e_test/start/pageObjects/noteTargetSearchDialog.ts` — existing E2E conventions to extend

No web/library research was needed or performed (no new external packages, frameworks, or docs lookups — this phase is 100% internal codebase reuse); `.planning/config.json` shows all external research providers (`brave_search`, `exa_search`, `tavily_search`, `firecrawl`, `ref_search`, `perplexity`, `jina`) disabled, consistent with this being a pure codebase-archaeology research task.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new stack; all reused code read directly from the repo
- Architecture: HIGH — every integration point (fetch path, write path, gating pattern) was traced to actual source, not inferred
- Pitfalls: HIGH — the singleton-inserter hazard was confirmed by reading `useContentCursorInserter.ts` and `NoteEditableContent.vue`'s unconditional `onMounted` registration, not assumed

**Research date:** 2026-07-24
**Valid until:** No external time pressure (internal codebase reuse); re-verify only if `SearchForm.vue` / `LinkInsertionChoice.vue` / `useContentCursorInserter.ts` change materially before this phase executes.
