---
phase: 5
slug: alias-as-wiki-link-overlap-declaration
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-24
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (backend) + Vitest (frontend) |
| **Config file** | Spring Boot test / frontend Vitest config |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and `CURSOR_DEV=true nix develop -c pnpm frontend:test` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (when needed); targeted frontend specs |
| **Estimated runtime** | ~60–180 seconds (backend:test_only) |

---

## Sampling Rate

- **After every task commit:** Run targeted class/spec under Nix prefix
- **After every plan wave:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` + affected frontend authored-alias specs
- **Before `/gsd-verify-work`:** Backend unit green; frontend authored-alias specs green
- **Max feedback latency:** ~180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | OVL-02 | — | N/A (parser) | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 05-01-02 | 01 | 1 | OVL-02 | — | N/A (frontend validation parity) | unit | `CURSOR_DEV=true nix develop -c pnpm frontend:test` | ❌ W0 | ⬜ pending |
| 05-02-01 | 02 | 2 | OVL-03 | — | Wiki-link aliases not searchable/indexed | service/controller | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 05-02-02 | 02 | 2 | OVL-03 | — | Cloze / matchAnswer / resolve / AM alias leg ignore wiki-link items | controller/integration | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/.../FrontmatterAliasesTest.java` — stubs for wiki-link accept + plain segregation + overlap accessor
- [ ] `frontend` `authoredAliasesValidation.spec.ts` — parity for well-formed `[[…]]`
- [ ] `NoteAliasIndexServiceTest` — mixed-list indexes plain only
- [ ] Extend search / wiki-resolve / cloze / matchAnswer / accidental-match alias regressions
- [ ] Framework install: none

---

## Manual-Only Verifications

All phase behaviors have automated verification (Structure declaration; no recall UI in this phase).

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
