---
phase: 3
slug: reveal-both-notes-after-accidental-match
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-24
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (backend); Vitest (frontend); Cypress 15 + Cucumber (E2E) |
| **Config file** | backend Gradle test; frontend Vitest; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` / `CURSOR_DEV=true nix develop -c pnpm -C frontend test <spec>` |
| **Full suite command** | Backend all tests + targeted frontend specs + `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` |
| **Estimated runtime** | ~60–180 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Targeted AccidentalMatch controller tests and/or touched frontend specs
- **After every plan wave:** Backend AccidentalMatch suite + frontend specs for AnsweredSpellingQuestion / RecallPage
- **Before `/gsd-verify-work`:** Backend green + frontend specs green + E2E feature untagged and passing
- **Max feedback latency:** ~180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | AM-03 | T-03-01 | matchedNotes only readable notes | controller | `pnpm backend:test_only` (AccidentalMatch) | ✅ extend | ⬜ pending |
| 03-01-02 | 01 | 1 | AM-03 | — | all matches in matchedNotes; matchedNoteId = first | controller | same | ✅ extend | ⬜ pending |
| 03-02-01 | 02 | 2 | AM-03 | — | ACCIDENTAL_MATCH alert + NoteShow(s) | frontend unit | `pnpm -C frontend test` AnsweredSpellingQuestion | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 2 | AM-03 | — | plain wrong copy unchanged | frontend unit | same | ❌ W0 | ⬜ pending |
| 03-03-01 | 03 | 3 | AM-03 | — | user sees reviewed + matched notes | E2E | cypress accidental_match_reveal.feature | ❌ W0 @wip | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Flip `assertNull(getMatchedNotes())` on ACCIDENTAL_MATCH paths in `RecallPromptControllerTests`
- [ ] Rewrite title-over-alias preference test for union semantics
- [ ] Multi-match fixture asserting full `matchedNotes` order
- [ ] Frontend `AnsweredSpellingQuestion` specs (ACCIDENTAL_MATCH vs plain wrong)
- [ ] E2E capability-named feature (`accidental_match_reveal`) tagged `@wip` until green
- [ ] Framework install: none — existing infrastructure covers phase requirements

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | All phase behaviors have automated verification |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
