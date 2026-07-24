---
phase: 2
slug: accidental-match-grading-penalty
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-24
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Phase 2 has **no UI** (no frontend change) — backend integration tests through the HTTP controller are the primary verification surface. Per `.cursor/rules/backend-testing.mdc`: "Always run all backend unit tests instead of a selected file or test case."

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test (`@SpringBootTest @ActiveProfiles("test") @Transactional`) |
| **Config file** | `backend/build.gradle` (Gradle); test profile via `@ActiveProfiles("test")` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (full backend suite — required by backend-testing rule) |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (includes migration test DB) |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- **Before `/gsd-verify-work`:** Full backend suite must be green
- **Max feedback latency:** ~120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | AM-01 | T-2-01 / V4 IDOR | Title-match accidental match returns `outcome=ACCIDENTAL_MATCH`, `matchedNoteId=<otherNote.id>`, `correct=false` | integration (controller) | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | AM-01 | T-2-01 / V4 IDOR | Alias-match accidental match (alias leg of the lookup) returns `ACCIDENTAL_MATCH` + `matchedNoteId` | integration (controller) | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-01-03 | 01 | 1 | AM-01 | — | N/A — plain-wrong branch: answer matching NO note → `outcome=null`, `matchedNoteId=null`, `correct=false` | integration (controller) | `pnpm backend:test_only` | ✅ exists (RecallPromptControllerTests:707-715) | ⬜ pending |
| 02-01-04 | 01 | 1 | AM-01 / Security | T-2-01 | IDOR guard: wrong answer matching a note in an UNREADABLE notebook is NOT returned as `matchedNoteId` | integration (controller) | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-01-05 | 01 | 1 | AM-02 | — | Accidental match → `forgettingCurveIndex` drops by 10 (not 20); `nextRecallAt` is NOT `now+12h` and is `> now` | integration (controller) | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-01-06 | 01 | 1 | AM-02 | — | Accidental match still counts toward the wrong-answer threshold (`correct=false` counted by `countWrongAnswersSinceForMemoryTracker`) | integration (controller) | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-01-07 | 01 | 1 | SC-03 (AM-01) | — | Correct answer that ALSO matches another note's title → `correct=true`, `outcome=null` (search skipped, no auto-overlap) | integration (controller) | `pnpm backend:test_only` | ✅ partial (RecallPromptControllerTests:660-668) — extend with shared-title fixture | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `RecallPromptControllerTests$AnswerSpelling` — new `@Nested AccidentalMatch` class covering: title-match, alias-match, plain-wrong (keep existing), IDOR-unreadable, lighter-penalty, threshold-counts, skip-when-correct-shared-title. Uses existing `makeMe.aNote().rememberSpelling()`, `makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).forgettingCurveAndNextRecallAt(200.0f).spelling().please()`, `makeMe.aRecallPrompt().forMemoryTracker(mt).spelling().please()` builder pattern. [VERIFIED: codebase `RecallPromptControllerTests.java:459-477`]
- [ ] No new framework install needed — JUnit 5 + Spring Boot Test + `makeMe` already in place.
- [ ] No new test fixture/builder needed — existing `makeMe.aNote()` + a second note in another notebook (owned by same user for readable; owned by another user for IDOR) covers all cases.

*Existing infrastructure covers the framework and builder needs; only new test methods are required because Phase 2 introduces new behavior.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | — |

*All phase behaviors have automated verification (backend integration tests through the controller).*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
