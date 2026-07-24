# Phase 2: Accidental-match grading & penalty - Research

**Researched:** 2026-07-24
**Domain:** Spring Boot backend — spelling-answer grading, cross-notebook title/alias lookup, spaced-repetition penalty
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Extend `WikiLinkResolver` with a new wider-lookup method (e.g. `findAccidentalMatch(String answer, Note reviewedNote, User viewer)`) that searches by **title then alias**, **exact + case-insensitive**, across **all notebooks the viewer can read**, **excluding the reviewed note**. Inject `WikiLinkResolver` into `MemoryTrackerService`. Reuse the existing `noteCandidates` / `aliasTargetCandidates` building blocks and `authorizationService.userMayReadNotebook(viewer, notebook)` readability filter — but with **no notebook-name scoping**. The existing `resolveWikiLinkToken` is notebook-scoped and parses `[[wiki-link]]` token syntax, so it is NOT the right fit as-is.
- **D-02:** An accidental match records `Answer.correct = false` (wrong for the reviewed note; `correct` stays the sole SRS-credit signal) and `Answer.outcome = ACCIDENTAL_MATCH`.
- **D-03:** The accidental-match SRS penalty is `updateForgettingCurve(memoryTracker, -10.0f)` — **half of the plain-wrong penalty** (wrong = `ForgettingCurve.failed()` = `add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2)` = **-20**, plus a 12h `nextRecallAt` override). Accidental applies **-10** and **no 12h override** (let `MemoryTracker.calculateNextRecallAt()` recompute from the new index). -10 = `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` (10) = exactly half of -20.
- **D-04:** The accidental match **still counts toward the wrong-answer re-assimilation threshold** (the existing 5-wrong-in-14-days rule in `hasExceededWrongAnswerThreshold`). The threshold logic itself is unchanged. The lighter penalty must NOT go through `MemoryTracker.recallFailed` (which applies -20 + 12h). Likely a dedicated accidental-match path that increments `recallCount`, applies `updateForgettingCurve(-10)` (no 12h), and still runs `hasExceededWrongAnswerThreshold`. Research/planner to confirm the exact seam.
- **D-05:** Phase 2 captures the **first readable match** (lowest `id`, excluding the reviewed note) and sets `Answer.matchedNoteId` to that single id. It leaves `AnsweredQuestion.matchedNotes` (the `List<NoteTopology>`) **empty/null** — Phase 3 owns populating that list. `AnsweredQuestion.from(recallPrompt)` embeds the full `Answer` entity, so `matchedNoteId` + `outcome` surface in the response automatically — **no DTO change needed in Phase 2**.
- **D-06:** If `Note.matchAnswer` returns `correct=true`, the accidental-match search is **skipped entirely** — even if another readable note shares that same title or alias. That shared-title situation is the **overlap** case, which is **declared** (Phase 5) and **not auto-detected**.

### Claude's Discretion
- Exact method name and signature for the new `WikiLinkResolver` wider-lookup method.
- The precise code seam for applying the lighter penalty while preserving the threshold check (research/planner to confirm against `MemoryTracker.markAsRecalled` / `recallFailed`).
- Whether the wider lookup is one combined query or a title-then-alias fallback across readable notebooks (follow the existing `noteCandidates` title-then-alias ordering).
- Test placement and naming (follow existing `RecallPromptControllerTests` / `RecallsControllerTests` patterns; backend unit/integration tests only — no E2E in Phase 2, since there is no UI).

### Deferred Ideas (OUT OF SCOPE)
- Reveal both notes (reviewed + matched) in the UI — Phase 3 (AM-03); populates `AnsweredQuestion.matchedNotes: List<NoteTopology>`.
- Surfacing ALL matched notes (plural) — Phase 3.
- Offer the add-link UI with the matched note pre-selected — Phase 4 (AM-04).
- Alias-as-wiki-link overlap declaration — Phase 5 (OVL-02, OVL-03).
- Overlap "try again, no credit" response — Phase 6 (OVL-01); `AnsweredQuestion.overlap` flag + `outcome = OVERLAP`.
- MCQ accidental-match / fuzzy matching / cross-notebook qualified `Notebook:Title` typing — v2.
</user_constraints>

## Summary

Phase 2 is the first **Behavior** phase that writes the Phase 1 contract fields (`Answer.matchedNoteId` + `Answer.outcome = ACCIDENTAL_MATCH`). Today `MemoryTrackerService.answerSpelling` grades a spelling answer with a single `Note.matchAnswer` boolean and routes every wrong answer through `MemoryTracker.recallFailed` (index `-20`, clamped at floor 100, plus a `+12h` nextRecallAt override). Phase 2 inserts one branch: when the answer is wrong for the reviewed note **but exactly matches the title or alias of a different readable note**, grade it as an accidental match — set `matchedNoteId` + `outcome`, and apply a lighter `-10` penalty with **no** 12h override. The search must span **all notebooks the viewer can read** (broader than the notebook-scoped `WikiLinkResolver.resolveWikiLinkToken`) and must filter candidates by `AuthorizationService.userMayReadNotebook` so a `matchedNoteId` never leaks a note the user cannot read (OWASP ASVS V4 re-check — matched-note data first crosses the trust boundary here).

The grading seam is a single method (`answerSpelling`, lines 255–281) with a clean insertion point between `Boolean correct = note.matchAnswer(...)` (line 269) and `markAsRecalled(...)` (line 278). The penalty seam requires a **new** path that does NOT go through `recallFailed`; the existing `updateForgettingCurve(mt, -10.0f)` is the partial-adjustment API but has two asymmetries versus `failed()` (no floor clamp; stale `lastRecalledAt`) that the planner must resolve. No new packages, no DTO/controller/OpenAPI/frontend changes, no Flyway migration — the contract already exists from Phase 1.

**Primary recommendation:** Add `WikiLinkResolver.findAccidentalMatch(answer, reviewedNote, viewer)` backed by two new non-notebook-scoped repo queries (`NoteRepository.findByNoteTitleOrderByIdAsc` + `NoteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc`), reuse the `noteCandidates` title-then-alias fallback + Java-side `userMayReadNotebook` filter (covers bazaar), exclude the reviewed note, return the first readable match. Wire it into `answerSpelling` behind the existing `assertReadAuthorization` gate, and add a dedicated `markAsAccidentalMatch` path (increment `recallCount`, `-10` via a new clamped `ForgettingCurve.partialFail()`, bump `lastRecalledAt` to now, recompute `nextRecallAt`, run `hasExceededWrongAnswerThreshold`) — never `recallFailed`.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AM-01 | When a spelling answer is wrong for the reviewed note but matches another note's title or alias (searched across all notebooks the user can read), the system detects an accidental match. | `WikiLinkResolver.findAccidentalMatch` (new, D-01) backed by non-scoped `NoteRepository.findByNoteTitleOrderByIdAsc` + `NoteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc`, title-then-alias fallback, Java-side `userMayReadNotebook` filter (covers owns/subscription/bazaar), exclude reviewed note. Inserted in `MemoryTrackerService.answerSpelling` after `matchAnswer` returns false. Sets `Answer.matchedNoteId` + `outcome=ACCIDENTAL_MATCH` (Phase 1 contract fields, first writer). |
| AM-02 | An accidental match applies a slight spaced-repetition penalty that is lighter than a plain wrong answer (a third SRS outcome, no 12h override). | New `markAsAccidentalMatch` path: increment `recallCount`, apply `-10` (= `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT`, half of `failed()`'s -20) via a new clamped `ForgettingCurve.partialFail()`, recompute `nextRecallAt` via `calculateNextRecallAt()` (no 12h override), run `hasExceededWrongAnswerThreshold`. Must NOT call `MemoryTracker.recallFailed` (which applies -20 + 12h). |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Spelling-answer grading (correct vs wrong vs accidental) | API / Backend (`MemoryTrackerService.answerSpelling`) | — | Grading is server-side business logic; the controller (`RecallPromptController.answerSpelling`) only authorizes + delegates. |
| Cross-notebook title/alias lookup | API / Backend (`WikiLinkResolver`) | Database / Storage (`NoteRepository`, `NoteAliasIndexRepository`) | All existing title/alias match logic is cohesive in `WikiLinkResolver`; the new wider lookup belongs there, not in the service. Repositories provide the non-scoped candidate query. |
| Readability filtering (IDOR guard) | API / Backend (`AuthorizationService.userMayReadNotebook`) | — | Readability is the single gate (CONCERNS.md: HTTP security is permit-all; auth is manual). Java-side filtering (not DB) is the established pattern. |
| SRS penalty magnitude / nextRecallAt | API / Backend (`MemoryTracker`, `ForgettingCurve`) | — | Spaced-repetition math lives in entities/algorithms; the service orchestrates. The accidental path needs a new entity/algorithm seam (not `recallFailed`). |
| Contract surfacing (`matchedNoteId`, `outcome`) | API / Backend (`Answer` @Transient → `AnsweredQuestion.from`) | — | Phase 1 already wired @Transient fields to surface via Jackson/springdoc; Phase 2 only writes them. No DTO/OpenAPI/frontend tier change. |
| Auth gate | API / Backend (`RecallPromptController.assertCanMutateRecallPrompt`) | — | Existing `assertLoggedIn` + `assertReadAuthorization(memoryTracker)` gate; Phase 2 reuses it, does not add a new gate. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | existing | DI, `@Service`, `@Transactional`, Spring Data JPA | Already the backend framework [VERIFIED: codebase `MemoryTrackerService.java`] |
| Spring Data JPA | existing | Repository queries (`@Query` JPQL/native) | Already used by `NoteRepository`, `NoteAliasIndexRepository` [VERIFIED: codebase] |
| JPA / Hibernate (`jakarta.persistence`) | existing | `@Transient` contract fields, entity mapping | `Answer.matchedNoteId`/`outcome` are `@Transient` (Phase 1) [VERIFIED: codebase `Answer.java:37,39`] |
| Lombok | existing | `@Getter @Setter` on the new fields | Already used on `Answer` @Transient fields [VERIFIED: codebase `Answer.java:37,39`] |
| JUnit 5 + Spring Boot Test | existing | `@SpringBootTest @ActiveProfiles("test") @Transactional`, `makeMe` builders | Existing test pattern in `RecallPromptControllerTests` [VERIFIED: codebase] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `FrontmatterAliases` (internal algorithm) | existing | Alias normalization (`normalizedLookupKey` = NFKC + lower) and exact case-insensitive alias match | Reuse for the alias leg of the wider lookup and for the lookup key [VERIFIED: codebase `FrontmatterAliases.java:43,108`] |
| `NoteTitle` (internal algorithm) | existing | Title parsing + `matchesForRecall` (trim + equalsIgnoreCase + fragments) | Reviewed-note judge stays `Note.matchAnswer`; the cross-note title leg uses the DB `LOWER(n.title) = LOWER(:noteTitle)` (full title, not fragments) [VERIFIED: codebase `NoteTitle.java:34-39`] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| New non-scoped repo query + Java `userMayReadNotebook` filter (D-01) | Reuse the existing `searchExactForUserInAllMyNotebooks/Subscriptions/Circle` (title) + 3 alias variants and union them | Rejected: those are scoped to ownership/subscription/circle and **miss bazaar-readable notebooks**; `userMayReadNotebook` includes bazaar. Also 6 queries vs 1–2. The D-01 approach (non-scoped query + Java filter) is the established pattern (`firstReadableNotebookMatch`) and covers bazaar. [VERIFIED: codebase `NoteRepository.java:50-67`, `AuthorizationService.java:128-136`] |
| `updateForgettingCurve(mt, -10.0f)` for the lighter penalty | A new `ForgettingCurve.partialFail()` = `add(-INCREMENT)` (clamped) | See Pitfall 1: `updateForgettingCurve` does NOT apply the floor clamp that `failed()` does. **Recommend `partialFail()`** to preserve the "lighter than wrong" invariant near the floor. [VERIFIED: codebase `ForgettingCurve.java:19-25,69-71`, `MemoryTrackerService.java:138-142`] |

**Installation:**
```bash
# No packages to install. Phase 2 adds no dependencies — it reuses Spring Boot, Spring Data JPA, Lombok, and existing internal algorithms.
```

**Version verification:** Not applicable — no new packages. All reused libraries are already pinned in `backend/build.gradle` [VERIFIED: codebase].

## Package Legitimacy Audit

> Phase 2 installs **no** external packages. The Package Legitimacy Gate protocol is not triggered.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — (none) | — | — | — | — | — | No external packages added in this phase. |

**Packages removed due to [SLOP] verdict:** none.
**Packages flagged as suspicious [SUS]:** none.

*No `[ASSUMED]` packages are recommended; the planner does not need any `checkpoint:human-verify` install gates.*

## Architecture Patterns

### System Architecture Diagram

```text
POST /api/recall-prompts/{id}/answer-spelling  (AnswerSpellingDTO)
        │
        ▼
RecallPromptController.answerSpelling
        │  assertCanMutateRecallPrompt  ──►  AuthorizationService.assertLoggedIn
        │                                       + assertReadAuthorization(memoryTracker)   [existing gate]
        ▼
MemoryTrackerService.answerSpelling(recallPrompt, dto, user, now)
        │
        ├─ Boolean correct = note.matchAnswer(spellingAnswer)        ◄── Note.matchAnswer (title OR alias, reviewed note only)
        │
        ├─ if (correct)  ──────────────────────►  markAsRecalled(now, true, mt, tt)   [existing correct path — search SKIPPED per D-06]
        │
        └─ if (!correct)
             │
             ├─ Optional<Note> match = wikiLinkResolver.findAccidentalMatch(spellingAnswer, note, user)   ◄── NEW (D-01)
             │       │
             │       ├─ title candidates:  noteRepository.findByNoteTitleOrderByIdAsc(answer)            ◄── NEW non-scoped query
             │       │      (if empty) ↓
             │       ├─ alias candidates: noteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc(normalizedLookupKey(answer))
             │       │
             │       └─ iterate by id ASC, filter: userMayReadNotebook(user, candidate.notebook) && candidate.id != reviewedNote.id
             │                       ──► first readable match (or empty)            [IDOR guard: never leak unreadable note]
             │
             ├─ if match present  ──►  answer.setMatchedNoteId(match.id); answer.setOutcome(ACCIDENTAL_MATCH)
             │                         markAsAccidentalMatch(now, mt, tt)            ◄── NEW lighter-penalty path (D-03/D-04)
             │                            ├─ mt.recallCount++
             │                            ├─ forgettingCurveIndex += partialFail()  (-10, clamped at 100)
             │                            ├─ mt.lastRecalledAt = now               (recommended — see Pitfall 2)
             │                            ├─ mt.nextRecallAt = calculateNextRecallAt()  (NO +12h override)
             │                            └─ hasExceededWrongAnswerThreshold(...)   (D-04 — still counts; correct=false)
             │
             └─ if match absent  ──►  markAsRecalled(now, false, mt, tt)            [existing plain-wrong path — recallFailed: -20 + 12h]
        ▼
AnsweredQuestion.from(recallPrompt)  ──►  Answer.matchedNoteId + outcome surface via @Transient (Phase 1 A1)
        ▼
JSON response  (correct=false, outcome="ACCIDENTAL_MATCH", matchedNoteId=<id>; matchedNotes/overlap still null)
```

A reader can trace the primary use case — "user types another note's title as a wrong spelling answer" — from the POST at the top, through the `!correct` branch, the wider lookup, the lighter-penalty path, and out to the JSON response carrying `matchedNoteId` + `outcome`.

### Recommended Project Structure
```
backend/src/main/java/com/odde/doughnut/
├── services/
│   ├── MemoryTrackerService.java      # answerSpelling: add accidental-match branch + markAsAccidentalMatch
│   └── WikiLinkResolver.java          # findAccidentalMatch (new) + non-scoped candidate helpers
├── entities/
│   ├── MemoryTracker.java             # (optional) markAsAccidentalMatch entity seam OR keep logic in service
│   └── algorithms/ForgettingCurve.java # partialFail() (new) — clamped -10, mirrors failed() clamp
├── entities/repositories/
│   ├── NoteRepository.java            # findByNoteTitleOrderByIdAsc (new, non-scoped)
│   └── NoteAliasIndexRepository.java  # findByAliasLookupKeyOrderByNoteIdAsc (new, non-scoped)
└── controllers/dto/AnsweredQuestion.java  # UNCHANGED (from() already embeds Answer)
backend/src/test/java/com/odde/doughnut/controllers/
└── RecallPromptControllerTests.java   # AnswerSpelling @Nested: accidental-match + skip-when-correct + IDOR tests
```

### Pattern 1: Authorization-first, readability filtering in Java (not the DB)
**What:** Controllers call `AuthorizationService` before any work; cross-notebook lookups iterate candidates and call `userMayReadNotebook` in Java rather than embedding a readability predicate in JPQL.
**When to use:** Any time a query returns notes that could span notebooks the viewer cannot read (IDOR / horizontal privilege risk).
**Example:**
```java
// Source: WikiLinkResolver.firstReadableNotebookMatch (lines 113-121) — the pattern to mirror
private Note firstReadableNotebookMatch(String notebookName, String noteTitle, User viewer) {
  for (Note candidate : noteCandidates(notebookName, noteTitle)) {
    Notebook notebook = candidate.getNotebook();
    if (notebook != null && authorizationService.userMayReadNotebook(viewer, notebook)) {
      return candidate;
    }
  }
  return null;
}
```

### Pattern 2: Title-then-alias fallback (cohesive in `WikiLinkResolver`)
**What:** `noteCandidates` tries title match first; only if empty does it fall back to alias candidates. The new wider lookup mirrors this ordering.
**When to use:** The accidental-match lookup should prefer a title match over an alias match (consistent with wiki-link resolution).
**Example:**
```java
// Source: WikiLinkResolver.noteCandidates (lines 123-130)
private List<Note> noteCandidates(String notebookName, String noteTitle) {
  List<Note> byTitle = noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
  if (!byTitle.isEmpty()) return byTitle;
  return aliasTargetCandidates(notebookName, noteTitle);
}
```

### Pattern 3: `@Transient` contract fields surface without persistence
**What:** `Answer.matchedNoteId`/`outcome` are `@Transient` (jakarta.persistence) — not persisted, no Flyway migration — but Jackson serializes them and springdoc surfaces them in OpenAPI (Phase 1 A1 verified). The planner must NOT add a migration or `@Column`.
**When to use:** Phase 2 writes these fields at grading time; they ride on the in-memory `Answer` entity through `AnsweredQuestion.from(recallPrompt)` into the JSON.

### Anti-Patterns to Avoid
- **Routing the accidental match through `recallFailed`:** `recallFailed` applies `-20` + the 12h override — the exact penalty Phase 2 must NOT apply. The accidental path must be a separate seam.
- **DB-level readability predicate via the existing `searchExactForUserInAllMyNotebooks/Subscriptions/Circle`:** those miss bazaar-readable notebooks; `userMayReadNotebook` is the single source of truth for readability.
- **Surfacing ALL matched notes / setting `overlap`:** that is Phase 3 / Phase 6. Phase 2 sets only the singular `matchedNoteId` and leaves `matchedNotes`/`overlap` null (D-05).
- **Auto-detecting overlap when `correct=true`:** if the answer matches the reviewed note, SKIP the search entirely (D-06). Flagging a correct answer that also names another note would front-run Phase 6.
- **Hand-editing generated `types.gen.ts` / `open_api_docs.yaml`:** no contract change in Phase 2, so no regen is needed; if a regen were needed, use `pnpm generateTypeScript`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Cross-notebook title/alias match | A new bespoke search service in `MemoryTrackerService` | `WikiLinkResolver.findAccidentalMatch` reusing `noteCandidates`/`aliasTargetCandidates` + `userMayReadNotebook` | D-01 lock; keeps all title/alias match logic cohesive in the resolver; avoids duplicating normalization/lookup logic. [VERIFIED: codebase `WikiLinkResolver.java`] |
| Readability filter | A new DB-level ownership/subscription/bazaar predicate | `AuthorizationService.userMayReadNotebook(user, notebook)` | Single source of truth (owns/subscription/bazaar); established Java-side-filter pattern avoids IDOR. [VERIFIED: codebase `AuthorizationService.java:128-136`] |
| Alias normalization | A new normalize function | `FrontmatterAliases.normalizedLookupKey(answer)` (NFKC + lower) | Matches the stored `alias_lookup_key` column exactly. [VERIFIED: codebase `FrontmatterAliases.java:108-110`] |
| Lighter SRS penalty math | Raw `memoryTracker.setForgettingCurveIndex(index - 10)` | A new `ForgettingCurve.partialFail()` reusing `add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT)` (clamped) | `add()` already enforces the floor (100) and is the single place that clamps; `failed()` uses the same `add()`. [VERIFIED: codebase `ForgettingCurve.java:19-25,69-71`] |
| Threshold counting | A new "accidental counts as wrong" counter | The existing `recallPromptRepository.countWrongAnswersSinceForMemoryTracker` (counts `quiz_answer.correct=false` rows) | D-04 is satisfied automatically by `answer.setCorrect(false)` — the threshold counts wrong ANSWERS, not the penalty path. No new counter needed. [VERIFIED: codebase `RecallPromptRepository.java:67-73`] |
| Contract surfacing | A new DTO field or `AnsweredQuestion.from` overload | The existing `AnsweredQuestion.from(recallPrompt)` (embeds the full `Answer`) | `matchedNoteId`/`outcome` ride along with zero DTO edits (Phase 1 D-05). [VERIFIED: codebase `AnsweredQuestion.java:37-50`] |

**Key insight:** Every piece of Phase 2 — match, normalization, readability, penalty math, threshold counting, contract surfacing — already has a reusable seam in the codebase. The phase is wiring, not building.

## Common Pitfalls

### Pitfall 1: `updateForgettingCurve(-10)` does NOT apply the floor clamp that `failed()` does
**What goes wrong:** `ForgettingCurve.failed()` applies `add(-20)`, and `add()` clamps the index at `DEFAULT_FORGETTING_CURVE_INDEX` (100) as a floor. But `MemoryTrackerService.updateForgettingCurve(mt, adjustment)` does `setForgettingCurveIndex(index + adjustment)` **directly, with no clamp**. So `updateForgettingCurve(-10)` can push the index **below 100**, while the plain-wrong `failed()` cannot.
**Why it happens:** `updateForgettingCurve` was built for the assimilation init path (`+0.0f`), not for negative recall adjustments; it bypasses `ForgettingCurve.add()`.
**How to avoid:** Add a new `ForgettingCurve.partialFail()` = `add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT)` (i.e. `add(-10)`, clamped at 100) — mirroring how `failed()` uses `add()`. Apply it via a new entity/service seam, NOT via the raw `updateForgettingCurve(-10.0f)`. Near the floor this preserves the "lighter than wrong" invariant (accidental index stays ≥ 100, same floor as wrong, but a smaller drop).
**Warning signs:** A test where a tracker starts at index 100 and an accidental match leaves it at 90 (below the wrong-path floor) — that inverts "lighter." [VERIFIED: codebase `ForgettingCurve.java:19-25,69-71`, `MemoryTrackerService.java:138-142`]

### Pitfall 2: `calculateNextRecallAt()` uses a stale `lastRecalledAt` (nextRecallAt could land in the past)
**What goes wrong:** `MemoryTracker.calculateNextRecallAt()` = `lastRecalledAt + repeatInHours(newIndex)`. On the wrong path, `lastRecalledAt` is **not** updated (only `recalledSuccessfully` updates it; `recallFailed` does not), and `nextRecallAt` is forced to `now + 12h` — so staleness doesn't matter. The accidental path ("no 12h override") recomputes from the index using `lastRecalledAt`, which may be the assimilation/last-success time (in the past). The recomputed `nextRecallAt` could fall **before `now`**, making the note immediately due — a harsher, not lighter, outcome.
**Why it happens:** "No 12h override" + "recompute from index" relies on `lastRecalledAt`, which the wrong path never bumps.
**How to avoid:** In the new `markAsAccidentalMatch` seam, **bump `lastRecalledAt` to `currentUTCTimestamp`** before recomputing, so `nextRecallAt = now + repeatInHours(newIndex)` — always future-facing, "sooner than correct but not immediately due." This diverges from the wrong path (which doesn't bump) but matches the "lighter, recomputed from index" intent of D-03. The planner should confirm this is the desired semantic.
**Warning signs:** A test where `nextRecallAt` after an accidental match is earlier than `currentUTCTimestamp`. [VERIFIED: codebase `MemoryTracker.java:115-127,129-137`]

### Pitfall 3: Title-fragment asymmetry between `Note.matchAnswer` and the cross-note lookup
**What goes wrong:** `Note.matchAnswer` → `NoteTitle.matchesForRecall` accepts title **fragments** (cloze suffixes like `~suffix`) via `getRecallTitleFragments()`, not just the full title. The cross-note `findByNoteTitleOrderByIdAsc` matches the **full title** only (`LOWER(n.title) = LOWER(:noteTitle)`). So a user who types a fragment that matches the reviewed note (correct=true → search skipped) is fine, but a fragment that matches ANOTHER note is NOT detected as an accidental match.
**Why it happens:** Wiki-link resolution (which the wider lookup mirrors) uses full titles, not fragments.
**How to avoid:** This is **intended** (D-01: "exact + case-insensitive title or alias"). Document it in the plan so the planner doesn't expect fragment matching cross-note, and so tests use full titles/aliases for the matched note.
**Warning signs:** A test expecting a fragment of another note's title to trigger an accidental match would fail. [VERIFIED: codebase `NoteTitle.java:34-50`, `NoteRepository.java:35-44`]

### Pitfall 4: Normalization asymmetry between the reviewed-note judge and the cross-note lookup
**What goes wrong:** `NoteTitle.matchesForRecall` uses `rawTitle.trim().equalsIgnoreCase(answer)` (trims the title, NOT the answer; case-insensitive, no NFKC). `FrontmatterAliases.matchesFromNoteContent` trims the answer + `equalsIgnoreCase` (no NFKC). But the DB alias lookup uses `normalizedLookupKey` (NFKC + lower), and the DB title lookup uses `LOWER()` (no NFKC, no trim). Unicode/whitespace edge cases can cause `matchAnswer` to miss a match that the DB hits (or vice versa).
**Why it happens:** Three different normalization strategies across the judge and the two DB lookups.
**How to avoid:** Always **exclude the reviewed note** from the wider-lookup candidates (D-01/D-05) as a safety net, so a normalization mismatch can never self-match. Tests should use plain ASCII titles/aliases to avoid NFKC edge cases unless specifically testing normalization.
**Warning signs:** A test where the reviewed note and another note share a title that differs only by Unicode form. [VERIFIED: codebase `NoteTitle.java:34-39`, `FrontmatterAliases.java:81-87,108-110`, `NoteRepository.java:35-44`, `NoteAliasIndexRepository.java:22-30`]

### Pitfall 5: IDOR — leaking a `matchedNoteId` from an unreadable notebook
**What goes wrong:** If the wider lookup returns a note in a notebook the viewer cannot read, `matchedNoteId` leaks that note's existence/id (horizontal privilege escalation). CONCERNS.md flags "HTTP security is permit-all; auth is manual per controller" as a recurring footgun.
**Why it happens:** A non-scoped title/alias query returns notes across ALL notebooks regardless of readability.
**How to avoid:** Filter every candidate through `authorizationService.userMayReadNotebook(viewer, candidate.getNotebook())` in Java (the established `firstReadableNotebookMatch` pattern), NOT in the DB. Add a controller test: a second note with a matching title in an UNREADABLE notebook is NOT returned as `matchedNoteId`.
**Warning signs:** A test where `matchedNoteId` points to a note in a notebook the viewer does not own/subscribe/bazaar-read. [VERIFIED: codebase `AuthorizationService.java:128-136`, `.planning/codebase/CONCERNS.md`]

### Pitfall 6: Breaking the Phase 1 no-behavior tests
**What goes wrong:** Phase 1 added `shouldNotPopulateAccidentalMatchFieldsOnCorrectSpellingAnswer` and `...OnWrongSpellingAnswer` (lines 660-668, 707-715) asserting the four fields are null. The wrong-path test uses answer `"wrong"` (line 674). If Phase 2 introduces a note titled `"wrong"` in a test fixture, that test would flip to ACCIDENTAL_MATCH and break.
**Why it happens:** The no-behavior invariant was "no writer exists"; Phase 2 is the first writer.
**How to avoid:** The wrong-path test stays valid as long as no readable note is titled/aliased `"wrong"` (keep fixtures clean). The correct-path null assertion stays valid (search is skipped when correct). ADD new tests for the accidental-match case rather than mutating the existing wrong-path test. The planner should keep the existing `answerDTO.setSpellingAnswer("wrong")` plain-wrong test (it now documents the plain-wrong branch, not the no-behavior invariant).
**Warning signs:** The existing wrong-path test failing after Phase 2 wiring. [VERIFIED: codebase `RecallPromptControllerTests.java:660-715`]

## Code Examples

Verified patterns from the actual codebase (file:line evidence).

### The grading seam — where to insert the accidental-match branch
```java
// Source: MemoryTrackerService.answerSpelling (lines 255-281)
public RecallPrompt answerSpelling(
    RecallPrompt recallPrompt,
    AnswerSpellingDTO answerSpellingDTO,
    User user,
    Timestamp currentUTCTimestamp) {
  if (recallPrompt.getQuestionType() != QuestionType.SPELLING) { ... }
  if (recallPrompt.getAnswer() != null) { ... }
  MemoryTracker memoryTracker = recallPrompt.requireMemoryTracker();
  String spellingAnswer = answerSpellingDTO.getSpellingAnswer();
  Note note = memoryTracker.getNote();
  Boolean correct = note.matchAnswer(spellingAnswer);          // line 269 — the judge

  Answer answer = new Answer();
  answer.setSpellingAnswer(spellingAnswer);
  answer.setCorrect(correct);
  answer.setThinkingTimeMs(answerSpellingDTO.getThinkingTimeMs());
  recallPrompt.setAnswer(answer);
  entityPersister.save(recallPrompt);                          // line 276 — cascades to Answer (matchedNoteId/outcome are @Transient, not persisted)

  // INSERTION POINT: when !correct, search for an accidental match.
  //   if found: answer.setMatchedNoteId(match.id); answer.setOutcome(AnswerOutcome.ACCIDENTAL_MATCH);
  //            markAsAccidentalMatch(currentUTCTimestamp, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
  //   else:    (existing) markAsRecalled(currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
  markAsRecalled(currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());  // line 278
  return recallPrompt;
}
```

### The plain-wrong penalty to AVOID (recallFailed)
```java
// Source: MemoryTracker.recallFailed (lines 124-127) — the path Phase 2 must NOT take for accidental match
public void recallFailed(Timestamp currentUTCTimestamp) {
  setForgettingCurveIndex(forgettingCurve().failed());                       // add(-20), clamped at 100
  setNextRecallAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));  // the 12h override
}

// Source: ForgettingCurve.failed (lines 69-71)
public float failed() {
  return add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2);   // add(-20); add() clamps at floor 100
}
```

### The lighter-penalty seam to ADD (recommended)
```java
// NEW in ForgettingCurve (mirrors failed() clamp, half the magnitude)
float partialFail() {
  return add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT);   // add(-10), clamped at floor 100 — "half of wrong"
}
```
```java
// NEW path in MemoryTrackerService (or MemoryTracker entity) — NOT recallFailed
//   - increment recallCount (mirrors markAsRecalled line 141)
//   - apply partialFail() (-10, clamped) — lighter than failed() (-20)
//   - bump lastRecalledAt to now (recommended — see Pitfall 2) so nextRecallAt stays future-facing
//   - recompute nextRecallAt via calculateNextRecallAt() (NO +12h override)
//   - run hasExceededWrongAnswerThreshold (D-04 — still counts; correct=false is what counts it)
```

### The wider lookup to ADD in WikiLinkResolver (D-01)
```java
// NEW — reuses noteCandidates title-then-alias ordering + userMayReadNotebook filter, with NO notebook scoping
public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
  // title leg: noteRepository.findByNoteTitleOrderByIdAsc(answer)        (NEW non-scoped query)
  // alias leg (if title empty): noteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc(
  //                            FrontmatterAliases.normalizedLookupKey(answer))   (NEW non-scoped query)
  // iterate by id ASC, filter: userMayReadNotebook(viewer, candidate.getNotebook())
  //                        && !candidate.getId().equals(reviewedNote.getId())   (exclude reviewed note)
  // return first readable match (or empty)
}
```

### The threshold is satisfied by `correct=false` alone (D-04)
```java
// Source: RecallPromptRepository.countWrongAnswersSinceForMemoryTracker (lines 67-73)
//   counts quiz_answer rows WHERE qa.correct = false AND qa.created_at >= :since
// → an accidental match with answer.setCorrect(false) is counted automatically.
//   No new counter is needed; the threshold CHECK still runs in the new markAsAccidentalMatch path.
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Boolean-only answer grading (`correct: true/false`) | `correct` + optional `outcome` enum + `matchedNoteId` (Phase 1 contract) | Phase 1 (2026-07-23) | Phase 2 is the first writer of the new contract fields. `correct` stays the sole SRS-credit signal; `outcome` is metadata alongside it. [VERIFIED: codebase `Answer.java:24-39`] |
| Two SRS outcomes (correct → `recalledSuccessfully`; wrong → `recallFailed` = -20 + 12h) | Three outcomes: + accidental → `partialFail` = -10, no 12h (Phase 2) | Phase 2 | First new outcome since the SRS algorithm was written. The new path must NOT reuse `recallFailed`. [VERIFIED: codebase `MemoryTracker.java:124-147`, `ForgettingCurve.java:69-71`] |
| Notebook-scoped wiki-link resolution (`resolveWikiLinkToken`) | + cross-notebook accidental-match lookup (all readable notebooks) | Phase 2 | First wider-than-notebook title/alias lookup. Reuses `WikiLinkResolver` building blocks with scoping removed. [VERIFIED: codebase `WikiLinkResolver.java:99-149`] |

**Deprecated/outdated:**
- The Phase 1 no-behavior invariant ("grep `setMatchedNoteId|setOutcome|setOverlap|setMatchedNotes` in `backend/src/main/java` = 0") is **intentionally broken by Phase 2** — Phase 2 is the first writer of `setMatchedNoteId` + `setOutcome`. The other two (`setOverlap`, `setMatchedNotes`) MUST stay 0 (Phase 3 / Phase 6). [VERIFIED: `.planning/phases/01-extend-answer-outcome-api/01-VERIFICATION.md` Truth 7]

## Assumptions Log

> Claims tagged `[ASSUMED]` in this research. The planner and discuss-phase use this section to identify decisions needing user confirmation.

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The `note_alias_index` table is populated for all live notes (by the existing backfill/refresh path) so alias accidental-match lookups return results. | Architecture Patterns / Code Examples | If the index is stale for some notes, an alias accidental match would be missed (title-only matches still work). Mitigation: the index is a pre-existing dependency, not introduced by Phase 2; tests create notes via `makeMe` which triggers the refresh. [ASSUMED — not verified in this session; the refresh path is noted in CONCERNS.md "Derived index coherence"] |
| A2 | Bumping `lastRecalledAt` to `currentUTCTimestamp` for the accidental match (Pitfall 2 recommendation) is the desired semantic. | Common Pitfalls / Code Examples | If the team prefers the wrong-path semantic (don't bump), `nextRecallAt` could land in the past. This is a planning decision (Claude's discretion per CONTEXT). [ASSUMED — recommended, not locked] |
| A3 | A new clamped `ForgettingCurve.partialFail()` is preferred over raw `updateForgettingCurve(-10.0f)` (Pitfall 1 recommendation). | Common Pitfalls / Code Examples | If raw `updateForgettingCurve(-10)` is used instead, the index can drop below the 100 floor near the floor, inverting "lighter." [ASSUMED — recommended, not locked] |

## Open Questions

1. **Exact `lastRecalledAt` semantic for the accidental match (A2)**
   - What we know: `calculateNextRecallAt()` uses `lastRecalledAt`; the wrong path doesn't bump it and forces `+12h` instead.
   - What's unclear: Should the accidental match bump `lastRecalledAt` to `now` (recommended — keeps `nextRecallAt` future-facing) or leave it stale (matches wrong-path mutation but risks a past `nextRecallAt`)?
   - Recommendation: Bump to `now`; add a test asserting `nextRecallAt > currentUTCTimestamp` after an accidental match. This is a planner decision (Claude's discretion).

2. **Exact penalty seam: entity method vs service method (A3)**
   - What we know: `MemoryTracker.markAsRecalled` (entity) increments `recallCount` and branches to `recallFailed`/`recalledSuccessfully`. `updateForgettingCurve` (service) adjusts the index + recomputes + saves but does NOT increment `recallCount` or clamp.
   - What's unclear: Should the new `markAsAccidentalMatch` live on the `MemoryTracker` entity (mirroring `markAsRecalled`/`recallFailed`, calling a new `partialFail()`) or on `MemoryTrackerService` (orchestrating setters + `updateForgettingCurve`)?
   - Recommendation: Entity method `MemoryTracker.markAsAccidentalMatch(now)` calling `ForgettingCurve.partialFail()` — keeps SRS mutation cohesive with `recallFailed`/`recalledSuccessfully`, and the service saves + runs the threshold. This is a planner decision (Claude's discretion).

3. **SRS penalty magnitude — confirmed by D-03**
   - What we know: STATE.md lists "Exact SRS penalty magnitude for the accidental-match outcome (Phase 2)" as an open question (2026-07-23). CONTEXT.md (gathered 2026-07-24, later) **locks D-03: `updateForgettingCurve(-10)`-style, no 12h override**, anchored to `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` (10) = half of `failed()` (-20).
   - What's unclear: Whether the floor-clamp asymmetry (Pitfall 1) changes the effective magnitude near the floor.
   - Recommendation: The magnitude is LOCKED at -10 by D-03; the open question is RESOLVED. The planner should implement -10 via a clamped `partialFail()` (Pitfall 1) so the effective magnitude is exactly "half of wrong" at all index levels. Surface the current wrong-answer penalty value (`failed()` = -20, clamped at 100) and the recommended accidental value (-10, clamped at 100) in the plan.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| MySQL 8.4 (+VECTOR) | Spring Data JPA repo queries (existing) | ✓ (via `pnpm sut`) | 8.4 | — | [VERIFIED: `.planning/codebase/ARCHITECTURE.md`]
| `note_alias_index` table (populated) | Alias leg of the wider lookup | ✓ (pre-existing, refreshed by `WikiTitleCacheService.refreshForNote` backfill) | — | Title-only match still works if alias index is stale for a note | [ASSUMED — see A1]
| Nix dev shell (`CURSOR_DEV=true nix develop -c`) | All repo tooling per workspace rules | ✓ | — | — | [VERIFIED: `.cursor/agent-map.md`]
| `pnpm sut` (backend + frontend + LB) | Assumed already running | ✓ (do NOT restart) | — | — | [VERIFIED: `.cursor/agent-map.md`]

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** a stale `note_alias_index` row would miss an alias accidental match; title-only matches still work. Tests use `makeMe` which triggers the refresh, so this is a prod-only concern (A1).

## Validation Architecture

> `workflow.nyquist_validation` is `true` in `.planning/config.json` — this section is required. Phase 2 has **no UI** (no frontend change), so backend integration tests through the HTTP controller are the primary verification surface. Per `.cursor/rules/backend-testing.mdc`: "Always run all backend unit tests instead of a selected file or test case."

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test (`@SpringBootTest @ActiveProfiles("test") @Transactional`) [VERIFIED: codebase `RecallPromptControllerTests.java`] |
| Config file | `backend/build.gradle` (Gradle); test profile via `@ActiveProfiles("test")` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (full backend suite — required by backend-testing rule) |
| Targeted run (dev only) | `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests "com.odde.doughnut.controllers.RecallPromptControllerTests"` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (includes migration test DB) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AM-01 | Wrong spelling answer that matches another readable note's title → `outcome=ACCIDENTAL_MATCH`, `matchedNoteId=<otherNote.id>`, `correct=false` | integration (controller) | `pnpm backend:test_only` (RecallPromptControllerTests$AnswerSpelling) | ❌ Wave 0 — new test |
| AM-01 | Wrong spelling answer that matches another readable note's alias → accidental match via alias leg | integration (controller) | `pnpm backend:test_only` | ❌ Wave 0 — new test |
| AM-01 | Wrong spelling answer matching NO note → plain wrong (`outcome=null`, `matchedNoteId=null`, `correct=false`) | integration (controller) | `pnpm backend:test_only` | ✅ exists (lines 707-715, answer="wrong") — keep as plain-wrong branch |
| AM-01 / Security | Wrong answer matching a note in an UNREADABLE notebook → NOT returned as `matchedNoteId` (IDOR guard) | integration (controller) | `pnpm backend:test_only` | ❌ Wave 0 — new test |
| AM-02 | Accidental match → `forgettingCurveIndex` drops by 10 (not 20); `nextRecallAt` is NOT `now+12h` (no override) and is `> now` | integration (controller) | `pnpm backend:test_only` | ❌ Wave 0 — new test |
| AM-02 | Accidental match still counts toward the wrong-answer threshold (`correct=false` is counted by `countWrongAnswersSinceForMemoryTracker`) | integration (controller) | `pnpm backend:test_only` | ❌ Wave 0 — new test |
| SC-03 | Correct answer that ALSO matches another note's title → `correct=true`, `outcome=null` (search skipped, no auto-overlap) | integration (controller) | `pnpm backend:test_only` | ✅ partially (lines 660-668 correct-path null assertion) — extend with a shared-title fixture |

### Sampling Rate
- **Per task commit:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (full backend suite per backend-testing rule; fast — no migration).
- **Per wave merge:** `CURSOR_DEV=true nix develop -c pnpm backend:verify` (includes migration test DB).
- **Phase gate:** Full backend suite green before `/gsd-verify-work`. No E2E in Phase 2 (no UI). No frontend test needed (no frontend change) — but a `pnpm frontend:test` smoke is cheap insurance that the regenerated contract (Phase 1) still type-checks; not required.

### Wave 0 Gaps
- [ ] `RecallPromptControllerTests$AnswerSpelling` — new `@Nested AccidentalMatch` class: title-match, alias-match, plain-wrong (keep existing), IDOR-unreadable, lighter-penalty, threshold-counts, skip-when-correct-shared-title. Uses `makeMe.aNote().rememberSpelling()`, `makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).forgettingCurveAndNextRecallAt(200.0f).spelling().please()`, `makeMe.aRecallPrompt().forMemoryTracker(mt).spelling().please()` (existing builder pattern). [VERIFIED: codebase `RecallPromptControllerTests.java:459-477`]
- [ ] No new framework install needed — JUnit 5 + Spring Boot Test + `makeMe` already in place.
- [ ] No new test fixture/builder needed — existing `makeMe.aNote()` + a second note in another notebook (owned by same user for readable; owned by another user for IDOR) covers all cases.

*(If no gaps: "None — existing test infrastructure covers all phase requirements" — but here new test methods are required because Phase 2 introduces new behavior.)*

## Security Domain

> `security_enforcement` is `true`, `security_asvs_level: 1`, `security_block_on: "high"` in `.planning/config.json` — this section is required. Phase 2 is when matched-note data **first crosses the trust boundary** (the wider search returns a note the viewer did not navigate to), so OWASP ASVS V4 (Access Control) is the primary re-check (CONTEXT.md `canonical_refs` + Phase 1 SUMMARY "Next Phase Readiness").

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (reuses existing login) | `AuthorizationService.assertLoggedIn` (existing gate) |
| V3 Session Management | no | unchanged |
| V4 Access Control | **yes** | `AuthorizationService.userMayReadNotebook(viewer, notebook)` Java-side filter on every wider-lookup candidate — IDOR guard so `matchedNoteId` never leaks an unreadable note. Reuses the existing `firstReadableNotebookMatch` pattern. [VERIFIED: codebase `AuthorizationService.java:128-136`, `.planning/codebase/CONCERNS.md`] |
| V5 Input Validation | yes | The spelling answer is user input used as a lookup key. Existing repos use parameterized JPQL/native (`:noteTitle`, `:aliasLookupKey`) — no string concatenation into SQL. No injection risk. No new validation needed beyond the existing `@Valid` on the DTO. [VERIFIED: codebase `NoteRepository.java:35-44`, `NoteAliasIndexRepository.java:22-30`] |
| V6 Cryptography | no | unchanged |
| V7 Error Handling | yes (minor) | No new error paths; the accidental-match branch returns the same `AnsweredQuestion` shape. No new exception types. |

### Known Threat Patterns for Spring Boot + manual-auth (Doughnut)

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR / horizontal privilege escalation via `matchedNoteId` | Information disclosure / Elevation of privilege | Java-side `userMayReadNotebook` filter on every candidate (not a DB predicate); exclude the reviewed note; controller test asserting an unreadable-notebook match is NOT returned. [VERIFIED: CONCERNS.md "Application-level auth only"] |
| Forgotten auth on a new endpoint | Spoofing / Elevation | No new endpoint in Phase 2 — reuses `POST /api/recall-prompts/{id}/answer-spelling` and its existing `assertCanMutateRecallPrompt` → `assertLoggedIn` + `assertReadAuthorization(memoryTracker)` gate. [VERIFIED: codebase `RecallPromptController.java:81-101`] |
| SQL injection via the answer string | Tampering | Parameterized Spring Data queries (`:noteTitle`, `:aliasLookupKey`); the answer is only a bind parameter, never concatenated. [VERIFIED: codebase `NoteRepository.java`, `NoteAliasIndexRepository.java`] |
| Information leakage via `outcome`/`matchedNoteId` on existing paths | Information disclosure | Phase 1 already proved the fields are null on correct/wrong/history paths; Phase 2 only populates them on the accidental-match branch. No leakage on non-accidental paths. [VERIFIED: `.planning/phases/01-extend-answer-outcome-api/01-VERIFICATION.md` Truth 7] |

**Block-on-high:** No high-severity findings anticipated — the phase reuses the existing auth gate and adds a Java-side readability filter. The IDOR risk is the one to cover with a test (V4).

## Project Constraints (from .cursor/rules/)

Extracted actionable directives the planner must honor (treat with the same authority as locked CONTEXT decisions):

- **`.cursor/rules/general.mdc` / `CLAUDE.md` / `agent-map.md`:** Run all repo tooling through Nix (`CURSOR_DEV=true nix develop -c …`); **git commands do NOT need the Nix prefix**. Assume `pnpm sut` is already running; do NOT restart it (no restart nag). When deleting files, prefer `trash` over `rm -rf`. [VERIFIED]
- **`.cursor/rules/backend-code.mdc`:** Prefer returning entities (or existing API body types) from controllers; introduce DTOs only when the wire shape differs. Always use top-of-file imports; no inline FQCNs except for name collisions. `MemoryTrackerService.answerSpelling` already returns `RecallPrompt` (entity) — Phase 2 keeps that; `AnsweredQuestion.from` is the existing DTO wrapper. [VERIFIED]
- **`.cursor/rules/backend-testing.mdc`:** Test behavior, not implementation, through stable boundaries (controller-level). Prefer `@SpringBootTest @ActiveProfiles("test") @Transactional` + `makeMe` builders. **Always run all backend unit tests** (`pnpm backend:test_only`), not a selected file. One behavior per test; descriptive names. Group with `@Nested`. Use `assertThat` + matchers; `assertThrows` for exceptions. [VERIFIED]
- **`.cursor/rules/planning.mdc` (hard plan grammar):** Phase 2 is a **Behavior** phase (one observable behavior: accidental-match grading & lighter penalty). Stop-safe — the user may stop after Phase 2 and the contract + this behavior stand alone. No speculative structure for Phases 3–6 (e.g., do NOT populate `matchedNotes`/`overlap` here). [VERIFIED]
- **`.cursor/rules/gsd-coexistence.mdc` (must-keep local):** After-phase wrap-up: Jidoka → post-change-refactor → update plan → commit → push (deploy gate). ~5 min fuzzy per slice; >10 min hard finer-decompose. [VERIFIED]
- **`.cursor/rules/linting_formating.mdc`:** Java via Spotless/Google Java Format (`pnpm backend:format` / `backend:lint`); do not hand-edit generated OpenAPI/TS. Phase 2 adds no TS/OpenAPI change (no regen needed). [VERIFIED]
- **`.cursor/rules/db-migration.mdc`:** New Flyway scripts only under `backend/src/main/resources/db/migration/`; never edit committed migrations. **Phase 2 adds NO migration** — `matchedNoteId`/`outcome` are `@Transient` (Phase 1 lock). [VERIFIED: codebase `Answer.java:37,39`]

## Sources

### Primary (HIGH confidence — read from the actual codebase with file:line evidence)
- `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` — `answerSpelling` (255-281), `markAsRecalled` (152-165), `updateForgettingCurve` (138-142), `hasExceededWrongAnswerThreshold` (283-290).
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — `resolveWikiLinkToken` (38-40), `firstReadableNotebookMatch` (113-121), `noteCandidates` (123-130), `aliasTargetCandidates` (132-149).
- `backend/src/main/java/com/odde/doughnut/entities/Note.java` — `matchAnswer` (134-140).
- `backend/src/main/java/com/odde/doughnut/entities/Answer.java` — `@Transient matchedNoteId`/`outcome` (37,39), `correct` @NotNull (24-27).
- `backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java` — enum (3-8).
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java` — `failed()` (69-71), `add()` clamp (19-25), `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` (7).
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` — `recallFailed` (124-127, the 12h override), `markAsRecalled` (139-147), `calculateNextRecallAt` (115-118).
- `backend/src/main/java/com/odde/doughnut/controllers/RecallPromptController.java` — `answerSpelling` (81-95), `assertCanMutateRecallPrompt` (97-101).
- `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java` — `from(recallPrompt)` (37-50), `overlap`/`matchedNotes` (33,35).
- `backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java` — `matchesFromNoteContent` (43-45), `normalizedLookupKey` (108-110).
- `backend/src/main/java/com/odde/doughnut/algorithms/NoteTitle.java` — `matchesForRecall` (34-39), `getRecallTitleFragments` (42-50).
- `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java` — `findByNotebookNameAndNoteTitleOrderByIdAsc` (35-44), `searchExactForUserInAllMyNotebooks/Subscriptions/Circle` (50-67).
- `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteAliasIndexRepository.java` — `findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc` (22-30), user-scoped alias search variants (32-60).
- `backend/src/main/java/com/odde/doughnut/entities/repositories/RecallPromptRepository.java` — `countWrongAnswersSinceForMemoryTracker` (67-73, counts `quiz_answer.correct=false`).
- `backend/src/main/java/com/odde/doughnut/services/AuthorizationService.java` — `userMayReadNotebook` (128-136), `assertReadAuthorization` (60-78).
- `backend/src/main/java/com/odde/doughnut/entities/User.java` — `canReferTo` (76-79, owns or subscription).
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — `AnswerSpelling` @Nested (459-717), Phase 1 no-behavior tests (660-668, 707-715).

### Secondary (MEDIUM confidence — project planning artifacts)
- `.planning/phases/02-accidental-match-grading-penalty/02-CONTEXT.md` — locked decisions D-01…D-06, discretion, deferred ideas.
- `.planning/phases/01-extend-answer-outcome-api/01-01-SUMMARY.md` + `01-VERIFICATION.md` — the Phase 1 contract this phase writes to; no-writer invariant; A1 (@Transient surfaces in OpenAPI).
- `.planning/codebase/ARCHITECTURE.md`, `CONCERNS.md`, `CONVENTIONS.md` — auth-first controllers, Java-side readability filtering, permit-all HTTP security footgun.
- `.planning/REQUIREMENTS.md`, `ROADMAP.md`, `STATE.md` — AM-01/AM-02, success criteria, open question (resolved by D-03).
- `.cursor/rules/backend-code.mdc`, `backend-testing.mdc`, `planning.mdc`, `gsd-coexistence.mdc`, `db-migration.mdc`, `linting_formating.mdc`, `agent-map.md` — project constraints.

### Tertiary (LOW confidence)
- None — all findings were verified against the actual codebase or authoritative project artifacts this session. The only `[ASSUMED]` claims are A1 (alias index population), A2 (lastRecalledAt semantic), A3 (partialFail vs raw updateForgettingCurve) — all flagged for planner confirmation.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new packages; all reused libraries verified in `backend/build.gradle` and source.
- Architecture: HIGH — grading seam, penalty seam, and wider-lookup seam all identified with file:line evidence; insertion point and the path-to-avoid (`recallFailed`) confirmed.
- Pitfalls: HIGH — three concrete asymmetries (floor clamp, stale lastRecalledAt, title-fragment/normalization) and the IDOR risk identified against actual source.
- Security: HIGH — V4 IDOR guard and V5 parameterized-query mitigation confirmed against `AuthorizationService` and repo query strings.

**Research date:** 2026-07-24
**Valid until:** 2026-08-23 (30 days — stable brownfield codebase; the only fast-moving risk is the alias index refresh path, which is pre-existing and not changed by Phase 2)
