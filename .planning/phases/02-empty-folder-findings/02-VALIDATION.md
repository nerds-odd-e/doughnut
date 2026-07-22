---
phase: 2
slug: empty-folder-findings
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-22
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`) |
| **Config file** | `backend/` Gradle + Spring `test` profile |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` |
| **Estimated runtime** | ~60–180 seconds |

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
| 02-01-01 | 01 | 1 | EFOL-01 | — | N/A (predicate) | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | EFOL-01 | — | Soft-deleted notes ignored | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | EFOL-02 | T-02-01 | Owner lint succeeds; foreign/anon rejected | controller | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 02-02-02 | 02 | 2 | EFOL-02 / SC-3 | T-02-03 | Lint does not mutate folders | controller | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java` — recursive emptiness, soft-delete ignored, blank vs non-blank readme, every nested empty folder listed
- [ ] `backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java` — owner success; foreign/anon fail; report `ruleId=empty_folders`; no mutation

*Existing infrastructure (`ControllerTestBase`, `MakeMe`, `FolderBuilder`, `NoteBuilder.softDeleted`) covers fixtures — no new test framework.*

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
