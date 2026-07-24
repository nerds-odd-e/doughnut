---
phase: 6
slug: overlap-try-again-no-credit
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-24
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution (OVL-01).

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (backend) + Vitest browser (frontend) + Cypress/Cucumber (E2E) |
| **Config file** | Spring Boot test / frontend Vitest / `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and targeted Vitest files |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` + targeted Vitest (only when needed) |
| **Targeted E2E** | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/overlap_try_again.feature` |
| **Estimated runtime** | ~60–180s backend; ~30–90s Vitest; E2E longer |

---

## Sampling Rate

- **After every task commit:** Targeted controller nested class or single Vitest file under Nix prefix
- **After every plan wave:** `backend:test_only` + touched Vitest files
- **Phase gate:** Targeted E2E green; remove `@wip` when pass; no full E2E suite unless asked
- **Max feedback latency:** ~180 seconds for unit/controller/Vitest

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | OVL-01 | T-06-01 | Dual-match OVERLAP; zero SRS; overlap flag; warning UI; stay+retry | controller + Vitest | `pnpm backend:test_only` + AnsweredSpellingQuestion/RecallPage specs | ❌ W0 | ⬜ pending |
| 06-01-02 | 01 | 1 | OVL-01 | T-06-01 | Distinguishing plain alias → CORRECT with credit | controller | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 06-02-01 | 02 | 2 | OVL-01 | T-06-02 | Decision: Flyway persist outcome (D-04) | checkpoint | human / yolo recommended | — | ⬜ pending |
| 06-02-02 | 02 | 2 | OVL-01 | T-06-02 | Persist outcome; exclude OVERLAP from wrong-count | controller | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 06-03-01 | 03 | 3 | OVL-01 | T-06-01 | Dead/unreadable → CORRECT; self-token skip | controller | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 06-03-02 | 03 | 3 | OVL-01 | T-06-03 | AM path unchanged when matchAnswer false; matchedNotes empty on OVERLAP | controller + Vitest | backend:test_only + AnsweredSpellingQuestion.spec | ❌ W0 | ⬜ pending |
| 06-04-01 | 04 | 4 | OVL-01 | T-06-01 | E2E try-again + distinguishing credit | E2E | `pnpm cypress run --spec …/overlap_try_again.feature` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `RecallPromptControllerTests` with capability-named `OverlapTryAgain` (or sibling) nested cases
- [ ] Extend `AnsweredSpellingQuestion.spec.ts` for warning alert + Try again + absent matched-notes
- [ ] Extend `RecallPage.spec.ts` for no queue advance + remount nonce retry
- [ ] If Flyway chosen: `V300000236__add_quiz_answer_outcome.sql` + count-query exclusion proof
- [ ] Capability-named `e2e_test/features/recall/overlap_try_again.feature` (+ steps / page object) — `@wip` until green
- [ ] Framework install: none

---

## Manual-Only Verifications

None required for phase gate — OVL-01 covered by controller + Vitest + targeted E2E. Human visual optional after E2E green.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 / checkpoint dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s for non-E2E
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
