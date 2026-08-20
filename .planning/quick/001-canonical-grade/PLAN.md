# Canonical Grade (replace Tutor score & Just-review Yes/No)

**Status:** in progress  
**Type:** Ad-hoc quick plan (not on roadmap)  
**Objective:** One scheduling evaluation concept — `Grade` (FSRS G: 1 AGAIN, 2 HARD, 3 GOOD, 4 EASY) — flowing through protocol, services, scheduling, persistence, APIs, and UI. Remove Tutor “score” and Just-review Yes/No as domain concepts.

## Design decisions

1. **Single `Grade` type** — Java `Grade` enum with `AGAIN(1)`, `HARD(2)`, `GOOD(3)`, `EASY(4)`. Numeric value **is** FSRS `G`, not a Tutor-specific score. Same type everywhere; no Tutor- or Just-review-only wrappers that only translate identical values.
2. **RecallLog outcomes** — Graded paths persist/use `Grade` directly. `CONFUSION` (and Overlap as answer outcome) stay **non-grade**. Model that distinction without a second equivalent grade representation (no parallel int score, no Yes/No enum). Existing `product_outcome` column may keep string values `AGAIN|HARD|GOOD|EASY|CONFUSION`; Java must not keep bidirectional score↔grade maps.
3. **Delete `CommissionedLearningSessionFeedbackScheduling`** — Its remaining job is translation + a thin `recordFeedback` switch. Move scheduling onto the Grade/scheduling owner (`MemoryTracker` / `MemoryTrackerService`).
4. **Just review** — Submits `Grade.GOOD` or `Grade.AGAIN` only. UI labels and wire params are grade names (e.g. Good / Again), **not** Yes/No and not a hidden `successful` boolean.
5. **Protocol** — New tagged block `<session_item_grades>`. Line values remain `1`–`4` (= G). Accept `<session_item_scores>` **only** as legacy parser spelling; normalize to `Grade` immediately; never expose “score” past that boundary. Request markdown and new reports use grades language.
6. **ADRs (human-owned vocabulary)** — Amend in place, **do not** change status, **do not** create a new ADR:
   - Accepted `docs/adrs/0001-ubiquitous-language.md` — Feedback carries a Grade; Grade is the single scheduling evaluation concept.
   - Proposed `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — Drop Tutor score / `score = G` / Just-review Yes/No map; Grade is first-class.
   - Proposed `docs/adrs/0005-commissioned-learning-session-protocol.md` — Requests/Reports exchange Grades; `<session_item_grades>`.
7. **Preserve scheduling behavior** — Stability, Difficulty, first-rating, elapsed-time, due, RecallLog DSR snapshot unchanged; only naming/type consolidation.
8. **Unrelated WIP** — Do not touch or mix commits with existing dirty `e2e_test/` export-zip work.

## Current hotspots (for executors)

| Area | Today | Target |
|------|--------|--------|
| `CommissionedLearningSessionFeedbackScheduling` | `score` ↔ `ProductOutcome` | Delete; schedule with `Grade` |
| `LearningSessionReportParser` / Request builder | `session_item_scores`, `score` | `session_item_grades`, `Grade`; legacy tag only in parser |
| DTOs / API | `RecordedLearningSessionItem.score`, `latestTutorFeedbackScore`, `successful` | `grade`, `latestTutorFeedbackGrade`, `grade` query |
| Just review UI / CLI | Yes/No, `successful` | Good/Again, `Grade` |
| `ProductOutcome` | GOOD/EASY/HARD/AGAIN/CONFUSION | Graded → `Grade`; CONFUSION non-grade |
| ADRs 0001 / 0003 / 0005 | score / Yes-No / compatibility map | Grade vocabulary |

Regenerate client after OpenAPI/DTO changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

---

## Slices

### 1. ADR vocabulary: Grade is canonical — **Structure** — done

Amended Accepted 0001 and Proposed 0003 / 0005 in place (status unchanged). Grade is first-class; legacy `<session_item_scores>` noted in 0005 only.

**Learning:** Keep intentional “score”/Yes-No only as negations or parser-legacy notes so later name-sweep (slice 6) does not strip those.

### 2. `Grade` owns scheduling and graded RecallLog — **Structure** — done

Introduced `Grade`; deleted `CommissionedLearningSessionFeedbackScheduling` and `ProductOutcome`. Graded RecallLog uses `Grade`; CONFUSION via null grade + converter. Public `score`/`successful` wire names deferred to Behavior slices 3–4.

**Learning:** Call sites should use `applyGrade` directly; Yes/No wrappers on MemoryTracker were dead after the structure cut.

### 3. Just review submits Good / Again — **Behavior** — done

API query `grade` replaces `successful`; web/CLI/E2E use Good/Again. Schedule still GOOD/AGAIN. Binary→Grade via `Grade.fromCorrect`.

**Learning:** CLI session-level `successful` for shared MCQ/spelling flow is separate from just-review wire params — leave until a dedicated CLI session cleanup if needed.

### 4. Learning Session Feedback is Grade — **Behavior** — done

Request/report/DTO/API/UI use grades + `<session_item_grades>` / `grade` / `latestTutorFeedbackGrade`. Parser emits `Grade` at parse time; legacy scores tag still accepted for slice 5.

**Learning:** Keep `ParsedReportEntry.grade` as `Grade` (not int) so the service does not re-wrap.

### 5. Legacy `<session_item_scores>` still grades — **Behavior** — planned

**Pre:** Pasted Report still uses `<session_item_scores>`.  
**Trigger:** Record report.  
**Post:** Entries normalize to `Grade` immediately; scheduling identical to `<session_item_grades>`; “score” does not appear outside the parser compatibility path.

**Verify:** Parser unit tests (legacy tag + new tag); no new public score API.

### 6. Leftover name sweep — **Structure** — planned

Remove dead score/Yes-No identifiers in tests, fixtures, generated artifacts (via regenerate), and **current** planning/docs pointers (e.g. STATE / FSRS gap / SEED-004 language that still says `score = G`). Do **not** rewrite historical milestone diaries unnecessarily. Lint + `scripts/check_diff_whitespace.sh`.

**Done when:** Repo search shows no live Tutor-score / Yes-No domain API; definition of done checklist green.

---

## Definition of done (plan-level)

- [ ] One `Grade` concept; no live Tutor score concept
- [ ] Just review uses GOOD/AGAIN directly; no Yes/No domain translation
- [ ] No bidirectional score↔grade or Yes/No↔grade mapping (legacy scores **tag** only at parser)
- [ ] Backend, frontend, CLI, API, protocol examples, tests, ADR terminology agree
- [ ] Focused tests, `generateTypeScript`, lint, whitespace checks pass
- [ ] Unrelated `e2e_test/` WIP preserved

## Out of scope

- Accepting ADR 0003 / 0005 (human)
- Changing Confusion / Overlap semantics
- FSRS weight fitting (E4) or other scheduling math changes
- Mixing with unrelated export-zip E2E WIP
