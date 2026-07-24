---
phase: 4
slug: offer-link-between-notes
status: draft
nyquist_compliant: true
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
- **After every plan wave:** Full frontend unit suite + `e2e_test/features/recall/accidental_match_reveal.feature` (from Wave 3 onward; Waves 1–2 unit-only is enough until E2E lands)
- **Before `/gsd-verify-work`:** Both green
- **Max feedback latency:** ~90s for unit; ~5 min for targeted E2E

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-T1 | 04-01 | 1 | AM-04 | T-04-01 | CTA gate + server auth on updateTextField | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/MatchedNoteLinkOffer.spec.ts tests/components/recall/AnsweredSpellingQuestion.spec.ts` | ❌ W0 | ⬜ pending |
| 04-01-T2 | 04-01 | 1 | AM-04 | T-04-01 | Readonly CTA omit (D-06) | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestion.spec.ts` | ✅ extend | ⬜ pending |
| 04-02-T1 | 04-02 | 2 | AM-04 | T-04-01 | skipNavigation does not weaken create auth | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/links/AddRelationship.spec.ts` | ✅ extend | ⬜ pending |
| 04-02-T2 | 04-02 | 2 | AM-04 | T-04-01 | Relationship create after confirm only; D-07 no nav | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/MatchedNoteLinkOffer.spec.ts` | ❌ W0 | ⬜ pending |
| 04-03-T1 | 04-03 | 3 | AM-04 | T-04-01 | E2E writable owner + existing write APIs | E2E | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` | ✅ extend | ⬜ pending |
| 04-03-T2 | 04-03 | 3 | AM-04 | — | Human visual CTA / dialog | human | checkpoint:human-verify | — | ⬜ pending |

*Source: `04-RESEARCH.md` § Validation Architecture + PLAN.md tasks.*

---

## Wave 0 Requirements

- [ ] `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` — created in 04-01 (choice / property-write); relationship / D-07 in 04-02
- [ ] Extend `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` — CTA visibility, per-match count, readonly gate (04-01)
- [ ] E2E scenario(s) on `e2e_test/features/recall/accidental_match_reveal.feature` (`@wip` until green) + page object helpers (04-03)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Visual CTA placement under matched NoteShow feels intentional | AM-04 | Layout polish | After accidental match, confirm each matched note has a clear link control; dialog opens with target pre-selected |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (human checkpoint has how-to-verify)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (scheduled in plans)
- [x] No watch-mode flags
- [x] Feedback latency within budgets above
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending execution
