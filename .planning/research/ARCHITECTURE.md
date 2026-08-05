# Architecture Research

**Domain:** Accidental-match resolve dialog UX (Doughnut recall, v1.2)
**Researched:** 2026-08-05
**Confidence:** HIGH

## Standard Architecture

### System Overview

v1.2 is a **frontend presentation change** on the existing accidental-match result. Grading, SRS, and overlap declaration semantics stay on the v1.1 backend path. The dialog mutates note content (link / aliases); it does **not** re-grade the current `Answer`.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Recall result UI (AnsweredSpellingQuestion)                                 │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────────────────────┐ │
│  │ Alert       │  │ NoteUnderQuestion│  │ NoteShow (reviewed only)        │ │
│  │ + Resolve   │  │ + Breadcrumb     │  │ full-height focus               │ │
│  │   CTA       │  └──────────────────┘  └─────────────────────────────────┘ │
│  └──────┬──────┘                                                            │
│         │ opens single Modal (PopButton)                                    │
│         ▼                                                                   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ AccidentalMatchResolveDialog                                         │   │
│  │  per matchedNotes[] row:                                             │   │
│  │    title link + Breadcrumb (from NoteRealm)                          │   │
│  │    [Build a link] → MatchedNoteLinkOffer (same Modal, step swap)     │   │
│  │    [Add as overlapped note] → append [[wiki]] alias + save content   │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────────┤
│  Client note cache (StoredApi / getNoteRealmRefAndLoadWhenNeeded)           │
├─────────────────────────────────────────────────────────────────────────────┤
│  Backend (unchanged for declare-from-dialog)                                │
│  answerSpelling → ACCIDENTAL_MATCH + matchedNotes[]                         │
│  FrontmatterAliases wiki-link items → future OVERLAP + try-again            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `AnsweredSpellingQuestion` | Accidental-match / overlap / correct / wrong result chrome; owns Resolve CTA | Modify existing Vue SFC |
| Resolve dialog (new) | Compact list of matches + per-row actions; no note bodies | New Vue under `components/recall/` |
| `MatchedNoteLinkOffer` | Property wiki-link or relationship finalize for reviewed→matched | Reuse as-is; host inside resolve Modal |
| `Breadcrumb` / `NoteTitleWithLink` | Path + navigable title without `NoteShow` | Reuse existing |
| `appendAliasToNoteContent` / wiki-link builder | Persist overlap declaration into reviewed frontmatter | Extend/reuse utils |
| `MemoryTrackerService.answerSpelling` | Grade ACCIDENTAL_MATCH vs OVERLAP; return topologies | Unchanged |
| OVERLAP try-again button | Same-session retry after **graded** OVERLAP | Unchanged; not used by dialog declare |

## Recommended Project Structure

```
frontend/src/components/recall/
├── AnsweredSpellingQuestion.vue          # MODIFY: CTA + dialog; drop stacked matches
├── AccidentalMatchResolveDialog.vue      # NEW: match list + action orchestration
├── AccidentalMatchResolveRow.vue         # NEW (optional extract): one match row
├── MatchedNoteLinkOffer.vue              # REUSE: Build a link step
├── NoteUnderQuestion.vue                 # unchanged (reviewed breadcrumb)
└── …

frontend/src/utils/
├── buildWikiLinkText.ts                  # REUSE: [[Title]] / [[Notebook:Title]]
├── wikidataTitleActions.ts               # REUSE pattern: appendAliasToNoteContent
└── frontmatterAliases.ts                 # REUSE: mergeAliasIntoList

frontend/tests/components/recall/
├── AnsweredSpellingQuestionAccidentalMatch.spec.ts   # MODIFY
├── AccidentalMatchResolveDialog.spec.ts              # NEW (or fold into above)
└── MatchedNoteLinkOffer*.spec.ts                     # keep; drive via dialog if needed

e2e_test/features/…                       # MODIFY accidental-match scenarios
```

### Structure Rationale

- **Keep resolve UI under `recall/`:** Same domain boundary as `MatchedNoteLinkOffer` and the answered spelling result.
- **Prefer one new dialog component** over scattering list + actions into `AnsweredSpellingQuestion` (already dense).
- **Do not invent a backend DTO for path** unless reopen-from-history without client cache becomes a requirement; `NoteTopology` lacks notebook/ancestors, but `NoteRealm` already supplies them via the existing store load used for link offers.

## Architectural Patterns

### Pattern 1: Single Modal, stepped content (no nested Modals)

**What:** One `PopButton`/`Modal` from the Resolve CTA. Default body = match list. "Build a link" swaps body to `MatchedNoteLinkOffer` (which already supports `@closeDialog` / go-back). Overlap action saves and closes (or returns to list).
**When to use:** Always for v1.2 — current link offer already opens a Modal via `PopButton`; nesting that inside another Modal breaks focus/close semantics.
**Trade-offs:** Slight state machine in the dialog (`list` | `linkOffer(matchedId)`); clearer than dual Modals.

**Example:**
```typescript
// Resolve dialog local step — not a second PopButton around MatchedNoteLinkOffer
type Step = { kind: "list" } | { kind: "link"; matchedNoteId: number }
```

### Pattern 2: Client-side realm hydrate for path display

**What:** `answeredQuestion.matchedNotes` stays `NoteTopology[]` (id + title). Dialog loads each match with `getNoteRealmRefAndLoadWhenNeeded(id)` and renders `Breadcrumb` + `NoteTitleWithLink` from `ancestorFolders` / `notebookRealm.notebook.id`.
**When to use:** Default for breadcrumb/path — same seam `canOfferLinkToMatched` already uses.
**Trade-offs:** Brief empty/loading path until realm arrives; avoids OpenAPI + backend Structure work. Enriching `matchedNotes` toward `RecalledNote`-shaped payloads is only justified if answer history must show path without N× `showNote`.

### Pattern 3: Overlap-from-dialog = content write, not re-grade

**What:** "Add as overlapped note" appends a wiki-link alias item to the **reviewed** note’s frontmatter (`[[Title]]` or `[[Notebook:Title]]` via `buildWikiLinkText`), then `updateTextField(…, "edit content", …)`. Current answer remains `ACCIDENTAL_MATCH` (penalty already applied). No try-again button, no credit reclaim UI.
**When to use:** Always for dialog declare. Graded `OVERLAP` + try-again remains the path when a **future** spelling answer is non-distinguishing against a declared overlap.
**Trade-offs:** Immediate UX is “declared, done”; learner does not get same-session retry credit reclaim — intentional per milestone. Next reviews of this tracker will hit `isNonDistinguishingOverlap` when applicable.

## Data Flow

### Request Flow (unchanged grade path)

```
User submits spelling answer
    ↓
RecallPromptController.answerSpelling
    ↓
MemoryTrackerService.answerSpelling
    ├─ correct + declared overlap → outcome OVERLAP (no matchedNotes) → try-again UI
    ├─ incorrect + wiki matches → ACCIDENTAL_MATCH + matchedNotes topologies
    └─ else → normal correct/incorrect schedule
    ↓
AnsweredSpellingQuestion renders result
```

### State Management (resolve dialog)

```
answeredQuestion.matchedNotes (ids/titles)
    ↓
Resolve CTA → Modal open (local show / PopButton)
    ↓
per id: StoredApi.getNoteRealmRefAndLoadWhenNeeded
    ↓
UI: Breadcrumb(ancestorFolders, notebookId) + NoteTitleWithLink(topology)
    ↓
Build a link → MatchedNoteLinkOffer(reviewedNoteId, matchedNoteId)
              → updateTextField / AddRelationshipFinalize (existing)
Add as overlapped → buildWikiLinkText + appendAliasToNoteContent
                  → updateTextField(reviewed, content)
                  → close Modal (still ACCIDENTAL_MATCH alert; no try-again)
```

### Key Data Flows

1. **Breadcrumb / path display:** `NoteTopology` from grade response → async `NoteRealm` → `ancestorFolders` + notebook id/name → existing `Breadcrumb` / `BasicBreadcrumb`. Title navigation uses `NoteTitleWithLink` → `noteShowLocation` (leaves recall; CTA remains available if the answered result is still mounted on return).
2. **Overlap-from-dialog:** Reviewed note content only. Token shape must be a whole-item wiki-link alias (`[[…]]`) so `FrontmatterAliases.overlapWikiLinkTokensFromNoteContent` picks it up later. Prefer `buildWikiLinkText` with source/target notebook ids so cross-notebook declarations use `Notebook:Title`. Dedup via `mergeAliasIntoList` / `appendAliasToNoteContent`.
3. **Build a link:** Unchanged offer pipeline (`LinkInsertionChoice` → property append or `AddRelationshipFinalize`); only the **host** moves from per-match `PopButton` under stacked `NoteShow` into the resolve dialog step.
4. **OVERLAP try-again (out of dialog scope):** Still gated solely by `answer.outcome === "OVERLAP"` on the graded response — never by a successful dialog declare on an `ACCIDENTAL_MATCH` answer.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Typical accidental matches (1–few) | Client realm hydrate per match is fine |
| Many matches | Keep list compact (title+path only); avoid remounting full `NoteShow` |
| History reopen without live cache | Optional later: persist richer match DTOs or re-resolve from `Answer.matchedNoteId` (today only first id is stored on `Answer`) |

### Scaling Priorities

1. **First bottleneck:** Nested Modals / stacked full-note shows — already the pain v1.2 removes.
2. **Second bottleneck:** N× `showNote` for path — acceptable for small N; API enrichment only if product requires offline/history path fidelity.

## Anti-Patterns

### Anti-Pattern 1: Nested PopButton/Modal for “Build a link”

**What people do:** Keep `PopButton` around `MatchedNoteLinkOffer` inside the resolve Modal.
**Why it's wrong:** Double Modal focus traps, competing close handlers, flaky E2E.
**Do this instead:** Single Modal; swap step to inline `MatchedNoteLinkOffer` and pass the Modal `closer` as `@closeDialog`.

### Anti-Pattern 2: Re-grade current answer to OVERLAP after declare

**What people do:** After appending the alias, flip outcome or show try-again / reclaim credit on this result.
**Why it's wrong:** Violates milestone decision and ADR 0003 separation: accidental-match penalty already applied; overlap try-again is for **graded** non-distinguishing answers against an existing declaration.
**Do this instead:** Content write only; leave alert as accidental-match; leave try-again exclusive to `outcome === "OVERLAP"`.

### Anti-Pattern 3: Stacked `NoteShow` for matches “until dialog is ready”

**What people do:** Keep v1.1 stacked bodies as interim.
**Why it's wrong:** Directly conflicts with the milestone goal (reviewed note full-height focus).
**Do this instead:** First behavior slice should remove stacked match bodies when introducing the CTA/dialog shell.

### Anti-Pattern 4: New backend endpoint only to declare overlap

**What people do:** Add `POST …/declare-overlap`.
**Why it's wrong:** Declaration is already a frontmatter aliases wiki-link write; backend grading already reads it.
**Do this instead:** Client content update through existing note edit API (same as link-as-property).

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| None new | — | Uses existing note show / content update APIs |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `AnsweredSpellingQuestion` ↔ Resolve dialog | Props: `matchedNotes`, `reviewedNoteId`; local Modal | Gate actions with existing writable/realm-loaded checks |
| Resolve dialog ↔ `MatchedNoteLinkOffer` | Props `reviewedNoteId` / `matchedNoteId`; `@closeDialog` | Step swap, not nested Modal |
| Resolve dialog ↔ StoredApi | `getNoteRealmRefAndLoadWhenNeeded`, `updateTextField` | Path + persist |
| Dialog declare ↔ future grade | Frontmatter aliases wiki-link on reviewed note | Consumed by `MemoryTrackerService.isNonDistinguishingOverlap` |
| ACCIDENTAL_MATCH UI ↔ OVERLAP UI | Separate branches on `answer.outcome` | Do not merge try-again into resolve dialog |

### New vs modified (explicit)

| Artifact | Change |
|----------|--------|
| `AccidentalMatchResolveDialog.vue` (+ optional row) | **NEW** |
| Overlap-append helper (or thin wrapper over `appendAliasToNoteContent` + `buildWikiLinkText`) | **NEW** or small util extension |
| `AnsweredSpellingQuestion.vue` | **MODIFY** — remove matched-notes `NoteShow` stack; add Resolve CTA + host Modal |
| Accidental-match Vitest / E2E | **MODIFY** |
| `MatchedNoteLinkOffer.vue` | **REUSE** (hosting change only; avoid API churn) |
| `Breadcrumb`, `NoteTitleWithLink`, `PopButton`/`Modal` | **REUSE** |
| `AnswerOutcome`, `MemoryTrackerService`, `FrontmatterAliases`, OVERLAP try-again | **UNCHANGED** |

### Suggested build order (dependency-aware)

1. **Util / Structure — overlap alias append**  
   Pure helper: wiki-link token via `buildWikiLinkText` + `appendAliasToNoteContent` (or sibling). Unit-testable; enables declare action without UI.

2. **Behavior — CTA + dialog shell, drop stacked match bodies**  
   Replace matched-notes section with “Resolve accidental match” → Modal listing titles from `matchedNotes`. Reviewed `NoteShow` remains. Stop-safe: result is already healthier (no stacked bodies) even if actions are stubbed.

3. **Behavior — path / breadcrumb in list**  
   Hydrate realms; render `Breadcrumb` + clickable `NoteTitleWithLink` (no body). Depends on (2).

4. **Behavior — Build a link from dialog**  
   Step into existing `MatchedNoteLinkOffer` inside the same Modal. Depends on (2); realm gates already required for (3)/(4).

5. **Behavior — Add as overlapped note**  
   Wire (1) + save; assert no try-again / no credit-reclaim UI on this ACCIDENTAL_MATCH result. Depends on (1)+(2).

6. **E2E / polish**  
   Reopen after title navigation (answered result still available), readonly notebook hides mutating CTAs, multi-match list, regression that OVERLAP try-again path is untouched.

**Ordering rationale:** Remove stacked UI first (milestone value), then display, then mutate via existing link offer, then declare-overlap write. Backend/API enrichment is **not** on the critical path.

## Sources

- `.planning/PROJECT.md` — v1.2 goals and decisions (dialog; overlap skips try-again/credit reclaim)
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — current alert + stacked matches + OVERLAP try-again
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — link offer reuse surface
- `backend/.../MemoryTrackerService.java` — ACCIDENTAL_MATCH / OVERLAP grading
- `backend/.../dto/AnsweredQuestion.java` — `matchedNotes` as `NoteTopology` only
- `backend/.../algorithms/FrontmatterAliases.java` — wiki-link overlap declarations
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — accidental match vs declared overlap
- `.planning/codebase/ARCHITECTURE.md` — system layers / StoredApi pattern
- `frontend/src/utils/buildWikiLinkText.ts`, `wikidataTitleActions.ts` — link token + alias append patterns
- `frontend/src/components/toolbars/Breadcrumb.vue`, `NoteTitleWithLink.vue` — path/title display without `NoteShow`

---
*Architecture research for: accidental-match resolve dialog UX (Doughnut v1.2)*
*Researched: 2026-08-05*
