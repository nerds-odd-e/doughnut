---
phase: 9
slug: build-a-link-from-resolve-dialog
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-05
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.x (browser mode) + Cypress/Cucumber E2E |
| **Config file** | frontend Vitest config (existing); `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| **Full suite command** | Same Vitest file (+ `MatchedNoteLinkOffer.spec.ts` if offer touched) + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` |
| **Estimated runtime** | ~30–120 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run quick Vitest command above
- **After every plan wave:** Run Vitest + targeted accidental_match_reveal E2E when untagging `@wip`
- **Before `/gsd-verify-work`:** Targeted unit + both link E2E scenarios green without `@wip`
- **Max feedback latency:** ~120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | AMR-06 | T-09-01 | Single Modal step; no nested PopButton | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | ✅ extend | ⬜ pending |
| 09-01-02 | 01 | 1 | AMR-07 | T-09-02 | Hide Build a link when readonly / unloaded | unit | same | ❌ Wave 0 restore | ⬜ pending |
| 09-02-01 | 02 | 2 | AMR-06 | T-09-01 | Property + relationship stay on result | e2e | Cypress `accidental_match_reveal.feature` | ✅ `@wip` | ⬜ pending |
| 09-02-02 | 02 | 2 | AMR-06 | — | Page-object opens Resolve then link helpers | e2e | same | ✅ update PO | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Restore/adapt Vitest cases in `AnsweredSpellingQuestionAccidentalMatch.spec.ts`: writable CTAs, readonly omit, unloaded realms omit, step-in-same-Modal, stay-on-result after link
- [ ] Update `AnsweredQuestionPage` Resolve → Build a link path before untagging `@wip`
- [ ] Untag `@wip` on wiki-property and relationship scenarios only after green

Existing Vitest/Cypress infrastructure covers the framework; Wave 0 is test-case restore only.

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
