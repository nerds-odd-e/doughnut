# Phase 3: Reveal both notes after accidental match - Research

**Researched:** 2026-07-24
**Domain:** Spring Boot + Vue — populate `matchedNotes` (all readable accidental matches) and reveal reviewed + matched notes in spelling answer UI
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Extend the accidental-match lookup so grading can obtain **all** readable matches — title **∪** alias candidates, exact case-insensitive, across all notebooks the viewer can read, excluding the reviewed note, **deduped by note id**, ordered by **id ascending**. Prefer an additive `findAllAccidentalMatches` (or equivalent) on `WikiLinkResolver`; keep existing `findAccidentalMatch` as first-of-list / lowest-id for backward compatibility. — **Reversibility:** reversible — additive method + list population; singular `matchedNoteId` path stays.
  - Rationale: Phase 3 success criterion #2 requires surfacing **all** matched notes. Phase 2's title-then-alias **first** match would miss alias-only siblings when a title match exists first, and misses additional title matches. Union + dedupe is the correct "all matches" semantics.
- **D-02:** On accidental match, populate `AnsweredQuestion.matchedNotes` with `NoteTopology` for every match from D-01. Keep writing `Answer.matchedNoteId` to the **first** list entry (lowest id) and `outcome = ACCIDENTAL_MATCH`. Do **not** leave `matchedNotes` null/empty when an accidental match occurred. — **Reversibility:** reversible — fills the Phase 1 contract field Phase 2 intentionally left empty.
  - Rationale: Phase 2 D-05 deferred list population to Phase 3; Phase 1 already shipped `matchedNotes: List<NoteTopology>` on the OpenAPI client.
- **D-03:** Reveal each matched note with a full **`NoteShow`** (same reveal depth as the reviewed note already shown in `AnsweredSpellingQuestion.vue`), not title-only links. — **Reversibility:** reversible — presentation-only; can thin later without contract change.
  - Rationale: PROJECT/ROADMAP goal is that "confusion becomes visible" — titles alone do not reveal what the user confused. Reuse existing `NoteShow` (`:expand-children="false"`) for consistency.
- **D-04:** Layout is a **vertical stack**: keep the existing reviewed-note block first (`NoteUnderQuestion` + `NoteShow` for the recalled note), then a labeled **"Matched note(s)"** section that renders one `NoteShow` per entry in `matchedNotes` (order = D-01 id ascending). No side-by-side columns. — **Reversibility:** reversible — layout-only.
  - Rationale: Matches current recall result flow (single column); scales when multiple notes match; reviewed note stays primary context.
- **D-05:** When `answer.outcome === 'ACCIDENTAL_MATCH'`, show a **distinct** result message that the typed answer named **another note** (still not correct for the reviewed note). Keep non-success alert styling (error/warning — it remains a miss for the reviewed note). Plain wrong answers (`correct === false` without accidental match) keep today's "`Your answer … is incorrect.`" copy unchanged. — **Reversibility:** reversible — copy/branch in the Vue component.
  - Rationale: Visibility of *why* this outcome differs from a plain wrong answer is core user value for AM-03; Phase 4 will sit on the same result surface.
- **D-06:** No add-link / `LinkInsertionChoice` / preselection UI in this phase. Structure the matched-notes section so Phase 4 can attach actions later, but do not ship link-building controls now. — **Reversibility:** reversible.
  - Rationale: stop-safe / one observable behavior per phase; AM-04 is Phase 4.

### Claude's Discretion
- Exact English wording of the accidental-match alert (as long as it clearly distinguishes from plain incorrect).
- Whether `AnsweredQuestion.from(RecallPrompt)` gains a helper to attach `matchedNotes`, or the service/controller sets the list after `from()` — prefer the smallest seam that keeps authorization filtering with the lookup.
- Exact label text for the matched-notes section ("Matched note(s)" vs similar).
- Frontend unit vs E2E coverage split (phase has UI hint: yes — include observable UI coverage; prefer capability-named tests, not phase numbers).
- Whether to keep calling `findAccidentalMatch` internally as `findAll…().stream().findFirst()` or dual-path — either is fine if list semantics match D-01/D-02.

### Deferred Ideas (OUT OF SCOPE)
- **Offer add-link UI with matched note pre-selected** — Phase 4 (AM-04); reuses `LinkInsertionChoice` / `AddRelationshipFinalize`.
- **Alias-as-wiki-link overlap declaration** — Phase 5 (OVL-02, OVL-03).
- **Overlap "try again, no credit" response** — Phase 6 (OVL-01).
- **MCQ accidental-match / fuzzy matching / qualified Notebook:Title typing** — v2, out of scope.

None of the above were folded into Phase 3; discussion stayed within AM-03 reveal scope.
</user_constraints>

## Summary

Phase 3 is a **Behavior** phase (AM-03): after Phase 2 already grades `outcome = ACCIDENTAL_MATCH` and sets singular `matchedNoteId`, make the confusion **visible** by (1) returning **all** readable matched notes as `AnsweredQuestion.matchedNotes: List<NoteTopology>` and (2) showing the reviewed note plus each matched note via full `NoteShow` in `AnsweredSpellingQuestion.vue`, with distinct accidental-match alert copy.

Today `WikiLinkResolver.findAccidentalMatch` is **title-then-alias short-circuit** (returns first readable title hit, else first alias hit). That cannot satisfy success criterion #2 or D-01’s title∪alias union. Phase 3 must add `findAllAccidentalMatches` (union + dedupe by id + id ascending + same `userMayReadNotebook` filter), drive `matchedNoteId` from the **first of that list**, and populate `matchedNotes` on the DTO — `AnsweredQuestion.from(RecallPrompt)` currently never sets `matchedNotes`, and Phase 2 controller tests explicitly assert `assertNull(getMatchedNotes())`.

Frontend gap is equally concrete: `AnsweredSpellingQuestion.vue` always shows the plain incorrect alert and a single reviewed `NoteShow`; it never reads `outcome` or `matchedNotes`. No OpenAPI regen is required — `matchedNotes` and `ACCIDENTAL_MATCH` already exist in `packages/generated/doughnut-backend-api/types.gen.ts`. No new npm/Maven packages.

**Primary recommendation:** Add `WikiLinkResolver.findAllAccidentalMatches` (title∪alias, readability filter, id-asc); make `findAccidentalMatch` = first of that list; in `answerSpelling` use the full list once for `matchedNoteId` + assemble `AnsweredQuestion.matchedNotes` at the controller/DTO seam; update Phase 2 tests that asserted null/title-prefer; update `AnsweredSpellingQuestion.vue` for distinct alert + vertical “Matched note(s)” `NoteShow` stack; cover with controller tests + frontend component/page tests + a capability-named E2E (`@wip` until green). Do **not** ship Phase 4 link UI.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AM-03 | After an accidental match, the reviewed note and the matched note(s) are revealed together. | Backend: `findAllAccidentalMatches` → populate `AnsweredQuestion.matchedNotes` (D-01/D-02). Frontend: `AnsweredSpellingQuestion.vue` vertical stack — reviewed `NoteShow` + labeled matched `NoteShow`s (D-03/D-04) + distinct `ACCIDENTAL_MATCH` alert (D-05). Multi-match: all entries in list order id ascending. |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Collect all readable accidental matches (title∪alias) | API / Backend (`WikiLinkResolver`) | Database (`NoteRepository`, `NoteAliasIndexRepository`) | Match logic already lives in the resolver; Phase 2 queries exist — extend to union/list. |
| Set `matchedNoteId` + `outcome` + populate `matchedNotes` | API / Backend (`MemoryTrackerService` + DTO assembly in controller/`AnsweredQuestion`) | — | Grading stays in service; `matchedNotes` is on `AnsweredQuestion`, not `Answer` — assemble at response boundary. |
| IDOR / readability filter for list | API / Backend (`AuthorizationService.userMayReadNotebook`) | — | Same Java-side filter as Phase 2; list must never include unreadable notebooks [CITED: asvs.dev/v4.0.3/V4-Access-Control/ ASVS 4.2.1]. |
| Reveal reviewed + matched note content | Browser / Client (`AnsweredSpellingQuestion.vue` + `NoteShow`) | API (`NoteController.showNote` via `NoteRealmLoader`) | UI renders from `matchedNotes` ids; each `NoteShow` re-auth’s on note load. |
| Accidental-match vs plain-wrong messaging | Browser / Client | — | Branch on `answer.outcome === 'ACCIDENTAL_MATCH'`; plain wrong copy unchanged (D-05). |
| Auth gate for answer endpoint | API / Backend (`RecallPromptController.assertCanMutateRecallPrompt`) | — | Existing gate; Phase 3 does not add a new auth entrypoint. |

## Project Constraints (from .cursor/rules/)

| Rule | Directive for this phase |
|------|--------------------------|
| `planning.mdc` | Phase 3 is **Behavior**, one observable behavior (reveal). Stop-safe. Do not front-run Phase 4 add-link. ~5 min slices; >10 min → finer decompose. Capability-named tests (not phase numbers). Targeted E2E only; `@wip` until green. Jidoka / post-change-refactor / commit+push at phase end (execute wrap-up). |
| `gsd-coexistence.mdc` | Local Behavior/Structure grammar, Nix prefix, commit+push wrap-up override plain GSD defaults. |
| `backend-code.mdc` | Prefer entities/existing DTOs; no new response DTO unless needed — reuse `NoteTopology` / `AnsweredQuestion`. Imports at top. Verify with `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (no migration). |
| `backend-testing.mdc` | Prefer controller-boundary tests (`RecallPromptControllerTests`); `makeMe` builders; all backend unit tests when verifying; one behavior per test. |
| `frontend-component.mdc` | Vue 3 + DaisyUI (`daisy-alert-*`); PascalCase components; `data-testid` for selectors. |
| `frontend-testing.mdc` | Vitest browser mode; `data-testid`; avoid `getByRole`; `mockSdkService` / `makeMe`; run via `CURSOR_DEV=true nix develop -c pnpm frontend:test …`. |
| `frontend-api.mdc` | Use generated `@generated/doughnut-backend-api` types — already have `matchedNotes` / `ACCIDENTAL_MATCH`; no regen unless OpenAPI shape changes (not expected). |
| `e2e-authoring.mdc` | Capability-named `.feature` under `e2e_test/features/`; page objects; `pnpm cypress run --spec …`; assume `pnpm sut` running; tag new scenarios `@wip` until pass. |
| `general.mdc` | Tooling: `CURSOR_DEV=true nix develop -c …`; git without Nix. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot / Spring Data JPA | existing (repo) | `WikiLinkResolver`, controller, `@Transactional` | Already powers Phase 2 grading [VERIFIED: codebase] |
| Vue 3 + TypeScript | existing (frontend) | `AnsweredSpellingQuestion.vue` reveal UI | Existing recall result surface [VERIFIED: codebase] |
| DaisyUI + Tailwind | existing | `daisy-alert` / `daisy-alert-error` for miss styling | Current spelling result alert pattern [VERIFIED: codebase `AnsweredSpellingQuestion.vue`] |
| Generated OpenAPI TS client | existing `packages/generated/doughnut-backend-api` | `AnsweredQuestion.matchedNotes`, `outcome` | Phase 1 already shipped types — no regen [VERIFIED: codebase `types.gen.ts:289-300`] |
| Vitest (browser) + Cypress/Cucumber | existing | Frontend + E2E observable coverage | Project standard [VERIFIED: `.cursor/rules/frontend-testing.mdc`, `e2e-authoring.mdc`] |
| JUnit 5 + Spring Boot Test | existing | Controller tests for `matchedNotes` population | Extend `RecallPromptControllerTests.AccidentalMatch` [VERIFIED: codebase] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `Note.getNoteTopology()` | existing | Map `Note` → `NoteTopology` for list entries | When assembling `matchedNotes` [VERIFIED: codebase `Note.java:143-150`] |
| `NoteShow` / `NoteRealmLoader` | existing | Full note reveal by `noteId` | One instance per matched note (D-03) [VERIFIED: codebase] |
| `doughnut-test-fixtures` `AnsweredQuestionBuilder` | existing | Frontend fixtures | Extend builder optionally for `matchedNotes`/`outcome` in tests [VERIFIED: codebase `AnsweredQuestionBuilder.ts`] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Title∪alias union (D-01 locked) | Keep Phase 2 title-then-alias short-circuit and only list title hits | Rejected by D-01 — misses alias-only siblings when any title match exists [VERIFIED: codebase `WikiLinkResolver.findAccidentalMatch` + test `shouldPreferTitleMatchOverAliasMatchWhenBothExist`] |
| Title-only chips / links for matches | Full `NoteShow` (D-03 locked) | Titles alone don’t reveal confusion content |
| Side-by-side columns | Vertical stack (D-04 locked) | Columns don’t scale to N matches; breaks existing single-column recall result flow |
| New matched-note DTO | Reuse `NoteTopology` | Already on contract; no regen [VERIFIED: Phase 1 / `types.gen.ts`] |

**Installation:**
```bash
# No packages to install — Phase 3 reuses existing backend + frontend stack.
```

**Version verification:** N/A — no new packages. Package Legitimacy Gate not triggered.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — (none) | — | — | — | — | — | No external packages added |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
POST /api/recall-prompts/{id}/answer-spelling
        │
        ▼
RecallPromptController.answerSpelling
        │  assertCanMutateRecallPrompt  (existing)
        ▼
MemoryTrackerService.answerSpelling
        │
        ├─ correct = note.matchAnswer(answer)
        │     └─ if correct → markAsRecalled(true)  [unchanged; search skipped]
        │
        └─ if !correct
             │
             ├─ List<Note> matches = wikiLinkResolver.findAllAccidentalMatches(answer, note, user)   ◄── NEW (D-01)
             │       title candidates: findByNoteTitleOrderByIdAsc(answer)          [LOWER=LOWER]
             │       alias candidates: findByAliasLookupKeyOrderByNoteIdAsc(key)
             │       union → dedupe by note.id → filter userMayReadNotebook && id != reviewed
             │       sort by id ASC
             │
             ├─ if matches non-empty
             │     answer.matchedNoteId = matches.getFirst().id
             │     answer.outcome = ACCIDENTAL_MATCH
             │     markAsAccidentalMatch(...)                         [unchanged Phase 2 penalty]
             │
             └─ else → markAsRecalled(false)                          [plain wrong]
        ▼
AnsweredQuestion.from(answered) + setMatchedNotes(topologies)         ◄── NEW (D-02)
        │  matchedNotes null/absent when not ACCIDENTAL_MATCH
        ▼
JSON: { answer: { correct:false, outcome:ACCIDENTAL_MATCH, matchedNoteId }, matchedNotes:[{id,title},…] }
        ▼
AnsweredSpellingQuestion.vue
        ├─ if outcome === ACCIDENTAL_MATCH → distinct alert (daisy-alert-error)   (D-05)
        ├─ else if !correct → existing "Your answer … is incorrect."
        ├─ reviewed: NoteUnderQuestion + NoteShow(recalledNote.id)
        └─ if matchedNotes?.length → section "Matched note(s)"
              └─ v-for note in matchedNotes :key="note.id" → NoteShow(:note-id="note.id" :expand-children="false")
```

### Recommended Project Structure

```text
backend/.../services/WikiLinkResolver.java          # findAllAccidentalMatches + thin findAccidentalMatch
backend/.../services/MemoryTrackerService.java      # use findAll; set matchedNoteId from first
backend/.../controllers/RecallPromptController.java # assemble matchedNotes on AnsweredQuestion
backend/.../controllers/dto/AnsweredQuestion.java   # optional from(..., List<Note>) helper
frontend/.../recall/AnsweredSpellingQuestion.vue    # alert branch + matched Notes stack
frontend/tests/.../AnsweredSpellingQuestion.spec.ts # (new) or extend RecallPage.spec.ts
e2e_test/features/recall/accidental_match_reveal.feature  # capability-named E2E
e2e_test/start/pageObjects/AnsweredQuestionPage.ts  # assertions for reveal
```

### Pattern 1: findAll then first-of-list
**What:** Implement `findAllAccidentalMatches` as the source of truth; `findAccidentalMatch` returns `findAll….stream().findFirst()` (or `Optional` of first).  
**When to use:** Always for accidental-match grading + DTO population so `matchedNoteId` and `matchedNotes.get(0).id` stay aligned (D-02).  
**Example:**
```java
// Source: codebase pattern — WikiLinkResolver (Phase 3 target)
public List<Note> findAllAccidentalMatches(String answer, Note reviewedNote, User viewer) {
  // title ∪ alias → readable → exclude reviewed → dedupe id → sort id ASC
}
public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
  return findAllAccidentalMatches(answer, reviewedNote, viewer).stream().findFirst();
}
```

### Pattern 2: DTO assembly after `from()`
**What:** `AnsweredQuestion.from(RecallPrompt)` stays as today; attach `matchedNotes` at the response boundary so authorization stays with the same lookup that graded.  
**Recommended seam (discretion):** Prefer **single lookup** inside `MemoryTrackerService.answerSpelling` (compute `List<Note> matches` once), then either:
1. **Preferred:** Return a small package-visible result / have the controller call a dedicated assembler that receives the already-computed list (avoid a second DB round-trip), **or**
2. Acceptable simpler variant: after `from()`, if `outcome == ACCIDENTAL_MATCH`, call `findAllAccidentalMatches` again in the controller (idempotent; inject `WikiLinkResolver` into `RecallPromptController`).

Do **not** put `matchedNotes` on `Answer` — Phase 1 locked the list on `AnsweredQuestion`.

### Pattern 3: Vue vertical reveal with stable keys
**What:** `v-for` over `matchedNotes` with `:key="matchedNote.id"` and one `NoteShow` each.  
**Source:** [CITED: Vue TodoMVC / Context7 `/vuejs/vue` — stable `:key` on list items]  
```vue
<section v-if="isAccidentalMatch" data-testid="matched-notes-section">
  <h3>Matched note(s)</h3>
  <NoteShow
    v-for="matched in answeredQuestion.matchedNotes ?? []"
    :key="matched.id"
    :note-id="matched.id"
    :expand-children="false"
  />
</section>
```

### Anti-Patterns to Avoid
- **Title-then-alias short-circuit for “all matches”:** Phase 2 behavior; fails D-01 / success criterion #2.
- **Leaving `matchedNotes` null when outcome is ACCIDENTAL_MATCH:** Violates D-02; breaks UI reveal.
- **Shipping `LinkInsertionChoice` / add-link:** Phase 4 only (D-06).
- **OpenAPI regen “just in case”:** Types already present; regen only if Java schema annotations change.
- **Phase numbers in product/test names:** Use capability names (`accidental_match_reveal`, etc.).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Matched-note card UI | Custom title/body viewer | Existing `NoteShow` | Same reveal depth as reviewed note; loads via authorized `showNote` |
| Note id+title wire shape | New DTO | `NoteTopology` / `Note.getNoteTopology()` | Phase 1 contract |
| Readability filtering | Ad-hoc ownership checks | `authorizationService.userMayReadNotebook` | Covers owns/subscription/circle/bazaar; Phase 2 IDOR tests already rely on it |
| Accidental-match candidate queries | New search APIs | Existing `findByNoteTitleOrderByIdAsc` + `findByAliasLookupKeyOrderByNoteIdAsc` | Already case-insensitive / ordered [VERIFIED: codebase] |
| Frontend API types | Hand-written interfaces | `@generated/doughnut-backend-api` | Already includes fields |

**Key insight:** Phase 3 is mostly **wiring Phase 1 contract fields the UI already types** plus **changing match collection from first-of-short-circuit to union-all** — not greenfield UI or schema work.

## Common Pitfalls

### Pitfall 1: Title∪alias changes `matchedNoteId` selection vs Phase 2 short-circuit
**What goes wrong:** Phase 2 test `shouldPreferTitleMatchOverAliasMatchWhenBothExist` expects title match wins even if an alias-only note has a lower id. After D-01, **lowest id across the union** wins for `matchedNoteId`.  
**Why it happens:** Short-circuit vs union semantics.  
**How to avoid:** Rewrite that test to assert **both** notes in `matchedNotes` and `matchedNoteId == min(ids)`. Do not preserve title-prefer when implementing findAll.  
**Warning signs:** Test still named “prefer title over alias” after union lands.

### Pitfall 2: `from()` leaves `matchedNotes` unset
**What goes wrong:** Grading sets `outcome`/`matchedNoteId` but response still has `matchedNotes: null` — UI can’t reveal.  
**Why it happens:** `AnsweredQuestion.from` only embeds `Answer`; list is a separate DTO field [VERIFIED: codebase `AnsweredQuestion.java:37-49`]. Phase 2 tests assert null.  
**How to avoid:** Explicit `setMatchedNotes` at assembly; flip Phase 2 null assertions for accidental-match cases.

### Pitfall 3: Double-query drift / forgetting to update service to use findAll
**What goes wrong:** Service still uses old short-circuit `findAccidentalMatch` while DTO uses findAll → `matchedNoteId` not in list or not first.  
**How to avoid:** Single source of truth: findAll drives both (Pattern 1).

### Pitfall 4: IDOR leak via `matchedNotes` topologies
**What goes wrong:** Unreadable notes appear as `{id, title}` in the JSON list (even if `NoteShow` later 403s).  
**How to avoid:** Reuse Phase 2 readability filter on **every** candidate before adding to the list; keep/extend `shouldNotLeakMatchedNoteIdFromUnreadableNotebook` to also assert `matchedNotes` null/empty. [CITED: OWASP ASVS 4.2.1]

### Pitfall 5: Regressing plain-wrong UI / E2E
**What goes wrong:** Alert always shows accidental-match copy; `expectSpellingAnswerToBeIncorrect` breaks.  
**How to avoid:** Branch strictly on `outcome === 'ACCIDENTAL_MATCH'`; leave plain incorrect string exact for non-match wrongs (D-05). Update E2E page object with a **separate** expectation for accidental match.

### Pitfall 6: Front-running Phase 4
**What goes wrong:** Add-link / preselection shipped “while we’re here.”  
**How to avoid:** D-06 — only a labeled section + NoteShows; optional `data-testid` hooks for Phase 4, no controls.

## Code Examples

### Backend: collect all matches (prescriptive shape)
```java
// Source: [VERIFIED: codebase WikiLinkResolver + NoteRepository.findByNoteTitleOrderByIdAsc]
List<Note> titleHits = noteRepository.findByNoteTitleOrderByIdAsc(answer);
List<Note> aliasHits = aliasAccidentalCandidates(answer); // existing private helper
// merge → LinkedHashMap/TreeMap by id → filter readable & != reviewed → List ordered by id ASC
```

### Frontend: alert + stack
```vue
<!-- Source: [VERIFIED: codebase AnsweredSpellingQuestion.vue] + D-03..D-05 -->
<div class="daisy-alert" :class="alertClass">
  <strong>{{ alertText }}</strong>
</div>
<!-- existing NoteUnderQuestion + reviewed NoteShow -->
<section
  v-if="answeredQuestion.answer.outcome === 'ACCIDENTAL_MATCH'"
  data-testid="matched-notes-section"
>
  <h3>Matched note(s)</h3>
  <NoteShow
    v-for="m in answeredQuestion.matchedNotes ?? []"
    :key="m.id"
    :note-id="m.id"
    :expand-children="false"
  />
</section>
```

**Recommended alert copy (discretion):**  
`Your answer \`${spelling}\` names another note — not correct for this review.`  
(Keep `daisy-alert-error` because `correct === false`.)

### Controller test flip
```java
// Replace assertNull(answerResult.getMatchedNotes()) on ACCIDENTAL_MATCH paths:
assertThat(answerResult.getMatchedNotes(), notNullValue());
assertThat(answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
    contains(secondNote.getId())); // or containsInRelativeOrder for multi-match
assertThat(answerResult.getAnswer().getMatchedNoteId(),
    equalTo(answerResult.getMatchedNotes().getFirst().getId().longValue()));
```

## State of the Art

| Old Approach (Phase 2) | Current Approach (Phase 3 target) | When Changed | Impact |
|------------------------|-----------------------------------|--------------|--------|
| `findAccidentalMatch` title-then-alias first only | `findAllAccidentalMatches` title∪alias | Phase 3 | All matches surfaced; `matchedNoteId` = lowest id in union |
| `matchedNotes` always null | Populated on ACCIDENTAL_MATCH | Phase 3 | UI can reveal |
| Spelling UI: plain incorrect + reviewed NoteShow only | Distinct alert + matched NoteShow stack | Phase 3 | Confusion visible |

**Deprecated/outdated:**
- Asserting `assertNull(getMatchedNotes())` on accidental-match success paths — obsolete once Phase 3 ships.
- Interpreting “prefer title over alias” as the lasting selection rule for `matchedNoteId` — replaced by lowest-id-in-union (D-01).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Exact alert English string is discretionary; recommended copy above is fine without user reconfirmation | Discretion / Code Examples | Copy tweak only — low risk |
| A2 | Double `findAll` in controller (if chosen over single-lookup assembler) is acceptable performance for spelling answers | Architecture Patterns | Extra query per answer — unlikely to matter; prefer single-lookup if easy |

*(A1–A2 are labeled ASSUMED only for product-copy/perf preference; core code seams are VERIFIED.)*

## Open Questions (RESOLVED)

1. **Assembler seam** — RESOLVED: Plan 03-01 — single `findAllAccidentalMatches` lookup in `MemoryTrackerService` + thin assembler `AnsweredQuestion.from(prompt, List<Note> matches)` (controller does not re-query). Avoid dual short-circuit + union paths.
2. **E2E vs frontend-unit** — RESOLVED: Plan 03-02 Vitest (alert + NoteShow stack) + Plan 03-03 capability-named E2E `@wip`→green.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JDK / backend tests | Controller tests | ✓ | openjdk 24.0.2 | — |
| Node / frontend tests | Vitest | ✓ | v24.5.0 | — |
| Nix + `CURSOR_DEV=true nix develop -c` | All pnpm scripts | ✓ (project contract) | — | Cloud VM skill if no Nix |
| `pnpm sut` (backend/frontend HMR) | E2E | assumed running | — | Start in separate terminal per e2e-authoring |
| MySQL / Redis (dev) | Backend + E2E | via process-compose | — | Inspect `mysql/mysql.log` / `redis/redis.log` |

**Missing dependencies with no fallback:** none identified for this phase.  
**Missing dependencies with fallback:** none.

Step 2.6: External deps are existing project tooling only — no new services.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (backend); Vitest browser (frontend); Cypress 15 + Cucumber (E2E) |
| Config file | backend Gradle test; `frontend` Vitest; `e2e_test/config/ci.ts` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/…` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend); targeted `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AM-03 | Accidental match response includes all readable matched note topologies; `matchedNoteId` = first | controller | `pnpm backend:test_only` (filter AccidentalMatch) | ✅ extend `RecallPromptControllerTests` — Wave 0: flip null asserts + add multi-match/union test |
| AM-03 | Unreadable notes never appear in `matchedNotes` | controller | same | ✅ extend existing IDOR test |
| AM-03 | UI shows distinct alert + reviewed + matched `NoteShow`(s) | frontend unit | `pnpm -C frontend test tests/.../AnsweredSpellingQuestion.spec.ts` (or RecallPage) | ❌ Wave 0 — add |
| AM-03 | Plain wrong still shows old incorrect copy | frontend unit | same | ❌ Wave 0 — add |
| AM-03 | User types another note’s title and sees both notes | E2E | `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` | ❌ Wave 0 — add `@wip` then untag |

### Sampling Rate
- **Per task commit:** targeted backend AccidentalMatch tests and/or frontend spec for the file touched
- **Per wave merge:** `pnpm backend:test_only` + touched frontend specs
- **Phase gate:** backend green + frontend specs green + E2E `@wip` removed and passing for the capability feature

### Wave 0 Gaps
- [ ] Flip `assertNull(getMatchedNotes())` on ACCIDENTAL_MATCH paths in `RecallPromptControllerTests` to assert populated list + id alignment
- [ ] Rewrite/replace `shouldPreferTitleMatchOverAliasMatchWhenBothExist` for union semantics + both-in-list
- [ ] Add multi-match fixture (two title matches and/or title+alias) asserting full `matchedNotes` order
- [ ] Frontend: `AnsweredSpellingQuestion` (or RecallPage) specs for ACCIDENTAL_MATCH vs plain wrong
- [ ] Optionally extend `AnsweredQuestionBuilder` with `withMatchedNotes` / outcome helpers
- [ ] E2E: capability-named feature + page-object expectations; start `@wip`
- [ ] Framework install: none

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | yes (endpoint already gated) | `assertLoggedIn` via `assertCanMutateRecallPrompt` |
| V3 Session Management | no change | existing session |
| V4 Access Control | **yes** | `userMayReadNotebook` on every list candidate; `NoteController.showNote` re-checks on reveal [CITED: ASVS 4.2.1] |
| V5 Input Validation | yes (existing) | spelling answer already validated/blank-skipped |
| V6 Cryptography | no | — |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR: leak unreadable note id/title in `matchedNotes` | Information Disclosure | Filter with `userMayReadNotebook` before list assembly; controller test |
| Client trusts `matchedNotes` without server auth on NoteShow | Elevation / Disclosure | `showNote` calls `assertReadAuthorization(note)` — defense in depth [VERIFIED: codebase `NoteController.java:57-59`] |
| Accidental-match UI offers link write (Phase 4) early | Tampering / scope creep | D-06 — no link UI this phase |

## Sources

### Primary (HIGH confidence)
- Codebase: `WikiLinkResolver.java`, `MemoryTrackerService.answerSpelling`, `AnsweredQuestion.java`, `AnsweredSpellingQuestion.vue`, `RecallPromptControllerTests.AccidentalMatch`, `types.gen.ts`, `Note.java#getNoteTopology`, `NoteController.showNote`
- `.planning/phases/03-…/03-CONTEXT.md` — locked D-01..D-06
- `.planning/phases/02-…/02-CONTEXT.md` — prior locks (list deferred to Phase 3)
- OWASP ASVS V4.2.1 Access Control / IDOR — [CITED: https://asvs.dev/v4.0.3/V4-Access-Control/]

### Secondary (MEDIUM confidence)
- Context7 `/vuejs/vue` — `v-for` + stable `:key` for list identity [CITED: Vue examples]
- `.cursor/rules/*` — project testing/execution constraints

### Tertiary (LOW confidence)
- Exact marketing copy for alert (discretion)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — reuse only; no new packages; fields already on wire
- Architecture: HIGH — integration points verified in code; D-01 union vs Phase 2 short-circuit called out
- Pitfalls: HIGH — existing tests encode the exact regressions to fix

**Research date:** 2026-07-24  
**Valid until:** 2026-08-24 (stable brownfield wiring; re-check if Phase 2 match code changes)
