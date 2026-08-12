# Feature Research

**Domain:** Spaced-repetition scheduling policy (ADR 0003)  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## Landscape

ADR 0003 defines **safety properties** for how graded evidence moves the schedule — not a new SRS engine. Findings fall into: **policy gaps**, **already-built outcome rules**, and **Doughnut differentiators**.

### Table stakes

| Feature | Finding |
|---------|---------|
| **Evidence vs due-time separation** | **Primary gap.** Success path keys off `nextRecallAt` deviation; should use elapsed since `lastRecalledAt`. No schema change needed. |
| **Correct schedules forward** | Partially met. Commissioned has strictly-future guard; recall paths should share the same invariant. |
| **No late-success penalty** | **Gap.** Overdue correct must be ≥ on-time for strength/schedule progress. Legacy early/late index tests encode old semantics. |
| **Early = weaker evidence, not failure** | Mostly built via early discount; re-verify after switching to retention-based time. |
| **Incorrect shortens without permanent trap** | Built (`recallFailed` + recovery via later correct). |
| **Distinct outcomes drive schedule** | Routing built (`SpellingRecallGrading`, `AnswerOutcome`); schedule effects must stay distinct. |
| **User spacing table** | Built (`SpacedRepetitionAlgorithm`). |
| **Post-grade projection consistency** | Built via JPA update on grade; rebuild-from-history deferred. |
| **Policy tests on observable schedule** | **Gap.** Prefer `nextRecallAt` / due eligibility / interval ordering over index alone. |
| **Recall effort within bounds** | Built with clamp + sqrt curve; audit slow correct cannot zero-interval or invert. |
| **Frequent-failure warning** | Built informational; overlap excluded; accidental counting needs product decision. |

### Differentiators (product-specific)

| Feature | Finding |
|---------|---------|
| Accidental match | Built: weaker path than incorrect; verify ordering. |
| Declared overlap | Built: no schedule mutation; same-session retry. |
| Commissioned Tutor 0–5 | Built v1.3: ADR table; effort/lateness neutral by design — audit. |
| Property-keyed trackers | Built; policy applies per tracker. |
| Thinking-time effort | Built for recall; commissioned ignores effort. |
| Materialized due with incomplete history | Architectural stance; rebuildable projection deferred. |

### Anti-features (problematic if pursued)

| Feature | Why problematic |
|---------|-----------------|
| Late-success / schedule-compliance penalty | Causes SEED-004 trap |
| FSRS migration now | ADR deferred; unnecessary to establish policy |
| Rebuild `nextRecallAt` from incomplete history | Unsafe |
| Frequent-failure-driven reschedule/delete | ADR informational only |
| Overlap → forced resolve / SRS reclaim | Locked anti-feature |
| Commissioned → 12h forced retry | Meaningless for commissioned cadence |
| Zero interval after graded answer | Forbidden after grade (new assimilation may start at 0) |
| Collapse outcomes to boolean | Breaks accidental / overlap / commissioned |
| Effort inverts outcome | Violates ADR |
| Index-only policy tests | Miss schedule contract |
| Frontend-side scheduling | Must stay server-authoritative |

## Dependencies (factual)

- Evidence separation underpins correct-forward scheduling and late-success removal.
- Overlap and accidental match are independent outcome branches.
- Commissioned feedback is largely independent of the recall-time evidence bug but shares post-grade interval safety.
- Frequent-failure must stay read-only on schedule; accidental-match counting is unresolved vs ADR “incorrect only.”

## Competitor norms

| Topic | Anki / FSRS | Doughnut |
|-------|-------------|----------|
| Overdue success | Not failure solely for lateness; elapsed time matters | ADR aligns; code still uses due deviation |
| Grading inputs | Ease buttons / FSRS rating | Spelling grade + optional thinking time; Tutor 0–5 |
| Ambiguous match | N/A | Accidental + overlap differentiators |
| Failure streak | Leech / suspend (varies) | Warning only by design |

## Outcome status snapshot

| Policy area | Doughnut touchpoint | Status |
|-------------|---------------------|--------|
| Memory evidence | `lastRecalledAt`, answers / Tutor score | Exists; success path wrong time base |
| Due metadata | `next_recall_at` | Exists; must stop feeding strength penalty |
| Correct | `recalledSuccessfully` → `succeeded` | **Fix primary** |
| Incorrect | `recallFailed` | Aligned |
| Accidental | `partialFail` + normal interval | Aligned — verify |
| Overlap | Skip `markAsRecalled` | Aligned — verify |
| Commissioned 0–5 | Feedback policy + scheduling | Aligned — audit |
| Effort | `thinkingTimeMs` | Aligned — audit bounds |
| Frequent failure | Threshold API + UI | Aligned — confirm accidental count |

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md`
- `ForgettingCurve.java`, `MemoryTracker.java`, `SpellingRecallGrading.java`
- `CommissionedLearningSessionFeedbackPolicy.java`
- `RecallServiceWithSpacedRepetitionAlgorithmTest.java`
- [Anki Studying](https://docs.ankiweb.net/studying.html), [FSRS wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)

---
*Researched: 2026-08-12*
