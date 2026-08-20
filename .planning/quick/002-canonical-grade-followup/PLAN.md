# Canonical-grade follow-up cleanup

**Status:** in progress  
**Type:** Ad-hoc quick plan (not on roadmap)  
**Objective:** Fix bugs and cohesion/test debt left by `.planning/quick/001-canonical-grade/` without changing FSRS math or Confusion/Overlap semantics.

## Findings (from post-implementation review)

| # | Severity | Finding |
|---|----------|---------|
| 1 | Bug (edge) | Empty `<session_item_grades>` block wins over a filled legacy `<session_item_scores>` block — report records nothing. |
| 2 | ADR / API footgun | `PATCH …/mark-as-recalled?grade=` accepts HARD/EASY; ADR and product UI/CLI only allow Good/Again. Pre-change API was boolean. |
| 3 | Cohesion | Learning-session record calls `persistRecallLog` + `applyGrade` without `markAsRecalled` (no explicit save; threshold path diverges). |
| 4 | Wire smell | `RecallLog` serializes both Lombok `grade` and `productOutcome` (OpenAPI has both; UI uses `productOutcome` only). |
| 5 | Latent misuse | `markAsRecalled(null)` persists CONFUSION then NPEs in `applyGrade`. |
| 6 | Redundant tests | `LearningSessionRecordTutorFeedbackTests` re-pins the full FSRS float matrix already owned by `MemoryTracker*RecallScheduling*`. |
| 7 | Redundant assert | `LearningSessionRecordTutorFeedbackRecallLogTests` checks `answerId == null` only for GOOD. |
| 8 | Test gap | CLI just-review suites count `markAsRecalled` calls but never assert `query.grade` (GOOD vs AGAIN). |

**Out of scope (intentional leftovers / separate product decisions):**
- Parser legacy `<session_item_scores>` tag itself
- Renaming DB column `product_outcome` or CLI session `successful` / `YesNoStagePrompt`
- Migrating history UI from `productOutcome` string to a Grade-shaped wire (after slice 4 hides the duplicate field, a future plan may rename)
- Accepting ADR 0003 / 0005 (human)
- FSRS E4 fitting

## Design decisions for this plan

1. **Empty grades fallthrough** — If `<session_item_grades>…</session_item_grades>` is present but blank (whitespace only), treat as absent and try legacy scores / whole document. Do not fall through when the grades block has non-blank content that fails to parse (those lines stay rejected).
2. **Just-review endpoint** — Reject HARD/EASY with 400 (or equivalent Spring validation); OpenAPI enum for this param becomes GOOD \| AGAIN only. Learning-session grades stay 1–4.
3. **One graded-apply path** — Session recording uses `MemoryTrackerService.markAsRecalled` (discard threshold boolean). `markAsRecalled` requires non-null `Grade`.
4. **RecallLog JSON** — `@JsonIgnore` on the entity `grade` field/getter so the wire stays a single `productOutcome` (grade name or CONFUSION). Regenerate client. No UI rename in this plan.
5. **Tests** — Entity scheduling tests remain numeric source of truth; learning-session tests assert grade recording + schedule moved (smoke), not the full float matrix. CLI adds one focused grade-query assertion.

---

## Slices

### 1. Empty grades tag falls through to legacy scores — **Behavior** — done

Blank `<session_item_grades>` (whitespace only) is treated as absent so legacy `<session_item_scores>` wins. Non-blank unparseable grades content still rejects. Tag-block cases live in `LearningSessionReportTagBlockParsingTest`.

### 2. Just-review mark-as-recalled accepts only Good / Again — **Behavior** — done

HARD/EASY on `mark-as-recalled` return 400; OpenAPI/TS param is GOOD|AGAIN. Membership lives on `Grade.isJustReviewGrade()`; service still accepts full `Grade` for learning session.

### 3. Learning-session grades go through `markAsRecalled` — **Structure** — done

`LearningSessionService.record` uses `MemoryTrackerService.markAsRecalled(..., grade, tracker, null)`. `markAsRecalled` requires non-null `Grade`; confusion stays on `persistRecallLog` only.

### 4. RecallLog wire exposes only `productOutcome` — **Structure** — planned

`@JsonIgnore` on `RecallLog.grade` persistence field (keep Java API `getGrade()` for domain code via a non-serialized accessor if needed). OpenAPI/client drop duplicate optional `grade` on RecallLog. Frontend unchanged (`productOutcome`).

**Verify:** Recall-log / history related backend tests; regenerate TypeScript; whitespace check on generated files via script.

### 5. Thin tutor-feedback FSRS asserts; fix recall-log assert — **Structure** — planned

In `LearningSessionRecordTutorFeedbackTests`, drop redundant first/second-grade float matrix that duplicates entity scheduling tests; keep controller-level “recorded + schedule advanced” coverage (minimal smoke). In `LearningSessionRecordTutorFeedbackRecallLogTests`, assert `answerId` null once for all grades (or drop if already covered).

**Verify:** Focused learning-session + entity scheduling tests green.

### 6. CLI just-review asserts grade query — **Behavior** — planned

**Pre:** Interactive just-review card shown.  
**Trigger:** Answer `y` or `n`.  
**Post:** `markAsRecalled` called with `query.grade` GOOD or AGAIN respectively.

One focused CLI unit assertion (extend existing mock spy); do not broaden session `successful` cleanup.

---

## Definition of done

- [x] Blank grades tag no longer shadows legacy scores
- [x] Just-review HTTP API cannot schedule HARD/EASY
- [x] One non-null graded apply path for quiz/just-review/session
- [ ] RecallLog JSON has a single outcome field (`productOutcome`)
- [ ] No duplicate FSRS float matrix in learning-session tutor tests; recall-log assert not GOOD-only
- [ ] CLI just-review tests pin GOOD/AGAIN query
- [ ] Focused tests + `generateTypeScript` (when API changes) + format/whitespace green

## Out of scope

- Renaming `productOutcome` → `grade` on the wire / history UI
- CLI `YesNoStagePrompt` / session `successful` refactor
- Accepting ADRs; FSRS math changes
