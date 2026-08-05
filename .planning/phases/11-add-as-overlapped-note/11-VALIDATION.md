---
phase: 11
slug: add-as-overlapped-note
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-05
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.x (browser mode) + Cypress/Cucumber E2E |
| **Config file** | frontend Vitest config (existing); `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| **Full suite command** | Same Vitest file + targeted Cypress on `accidental_match_reveal.feature` (+ keep `overlap_try_again.feature` green) |
| **Estimated runtime** | ~30–120 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run quick Vitest command above
- **After every plan wave:** Vitest green; Wave 2 add targeted Cypress
- **Before `/gsd-verify-work`:** Targeted unit + Add-as-overlapped E2E green; `overlap_try_again` still green
- **Max feedback latency:** ~120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | AMR-08 | T-11-01 | CTA + wiki-link `updateTextField` on reviewed note | unit | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | ❌ Wave 0 extend | ⬜ pending |
| 11-01-02 | 01 | 1 | AMR-09 | T-11-02 | No try-again / still ACCIDENTAL_MATCH after declare | unit | same | ❌ Wave 0 add | ⬜ pending |
| 11-01-03 | 01 | 1 | AMR-07 | T-11-03 | Hide Add CTA when readonly / unloaded | unit | same | ❌ Wave 0 add | ⬜ pending |
| 11-02-01 | 02 | 2 | AMR-08 | T-11-01 | Resolve → Add as overlapped → stay on result | e2e | Cypress `accidental_match_reveal.feature` | ❌ Wave 0 scenario | ⬜ pending |
| 11-02-02 | 02 | 2 | AMR-09 | T-11-02 | No try-again after declare; overlap_try_again uncoupled | e2e | accidental_match + `overlap_try_again.feature` | ❌ / ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `AnsweredSpellingQuestionAccidentalMatch.spec.ts` (or capability-named sibling): CTA visible when writable+loaded; omitted when readonly/unloaded; click appends wiki-link via `updateTextField`; after success still ACCIDENTAL_MATCH chrome and no `overlap-try-again`
- [ ] Page-object helper for Add as overlapped + Gherkin scenario (prefer extend `accidental_match_reveal.feature`)
- [ ] Util wiki-link shape already covered by `appendOverlapWikiLinkToNoteContent.spec.ts` — do not re-test util exhaustively at dialog boundary
- [ ] Existing Vitest/Cypress/`makeMe` infrastructure — no framework install

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
