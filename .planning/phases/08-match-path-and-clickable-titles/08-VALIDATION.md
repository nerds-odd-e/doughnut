---
phase: 8
slug: match-path-and-clickable-titles
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-05
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.x (browser mode) + Cypress/Cucumber E2E |
| **Config file** | frontend Vitest config (existing); `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| **Full suite command** | Targeted Vitest file + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` |
| **Estimated runtime** | ~30–90 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run quick Vitest command above
- **After every plan wave:** Run Vitest + targeted accidental_match_reveal E2E
- **Before `/gsd-verify-work`:** Targeted unit + E2E green
- **Max feedback latency:** ~90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | AMR-04 | T-08-01 | Text interpolation via NoteTitleWithLink / BreadcrumbWithCircle | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | ✅ extend | ⬜ pending |
| 08-01-02 | 01 | 1 | AMR-04 | T-08-01 | Deterministic seedRealms path names | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | ✅ extend | ⬜ pending |
| 08-02-01 | 02 | 2 | AMR-04 | T-08-01 | E2E asserts visible path + title link only | e2e | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` | ✅ extend | ⬜ pending |
| 08-02-02 | 02 | 2 | AMR-04 | — | N/A (wording / scope guard) | e2e | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` | ✅ extend | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements.

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | All phase behaviors have automated verification. |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
