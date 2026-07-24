---
phase: 4
slug: offer-link-between-notes
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-24
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (frontend unit) + Cypress + `@badeball/cypress-cucumber-preprocessor` (E2E) |
| **Config file** | `frontend/vitest.config.ts`; `e2e_test/cypress.config.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test` + targeted E2E `accidental_match_reveal.feature` |
| **Estimated runtime** | ~30–90s unit file; ~2–5 min targeted E2E |

---

## Sampling Rate

- **After every task commit:** Run targeted Vitest file(s) touched by that task
- **After every plan wave:** Full frontend unit suite + `e2e_test/features/recall/accidental_match_reveal.feature`
- **Before `/gsd-verify-work`:** Both green
- **Max feedback latency:** ~90s for unit; ~5 min for targeted E2E

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-*-* | TBD | TBD | AM-04 | T-04-01 IDOR/write | CTA gated + server auth on write | unit + E2E | see RESEARCH Validation Architecture | ❌ W0 | ⬜ pending |

*Filled by planner when PLAN.md tasks are written. Source: `04-RESEARCH.md` § Validation Architecture.*

---

## Wave 0 Requirements

- [ ] `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` (or equivalent) — choice / property-write / relationship / D-07 close
- [ ] Extend `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` — CTA visibility, per-match count, preselection wiring
- [ ] E2E scenario(s) on `e2e_test/features/recall/accidental_match_reveal.feature` (`@wip` until green) + page object helpers

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Visual CTA placement under matched NoteShow feels intentional | AM-04 | Layout polish | After accidental match, confirm each matched note has a clear link control; dialog opens with target pre-selected |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency within budgets above
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
