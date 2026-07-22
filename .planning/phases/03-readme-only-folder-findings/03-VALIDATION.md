---
phase: 3
slug: readme-only-folder-findings
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-22
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`) |
| **Config file** | `backend/` Gradle + Spring `test` profile |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` |
| **Estimated runtime** | ~60–120 seconds (full backend unit suite) |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | EFOL-03 | T-03-01 | autoFixable=false; no delete path | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | EFOL-03 | — | Mutual exclusion with empty_folders | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java` — inclusion, blank exclusion, live-note exclusion, soft-delete, own-readme partitioning, always-emit metadata (`autoFixable=false`), both groups in one report
- [ ] Keep `EmptyFolderHealthRuleTest` green after shared-helper extract (regression gate)

*Existing JUnit/Spring test infrastructure covers the framework — only new test class stubs needed.*

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
