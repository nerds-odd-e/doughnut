---
phase: 4
slug: dead-link-findings
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-22
---

# Phase 4 — Validation Strategy

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
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (prefer; use `backend:verify` only if format/migration gates needed)
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | DLNK-01 | T-04-01 | Viewer-readable resolve only; no any-target leak | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 04-01-02 | 01 | 1 | DLNK-02 | T-04-01 | Frontmatter dead links via shared extract | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 04-02-01 | 02 | 2 | DLNK-03 | T-04-02 | autoFixable=false; no mutation | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Planner may refine Task IDs / wave split; keep requirement coverage for DLNK-01/02/03.*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRuleTest.java` — body + FM dead links; alias/qualified live; soft-deleted source excluded; soft-deleted/missing targets dead; distinct-token dedupe; nested children; always-emit; autoFixable=false; no mutation; coexistence with folder groups
- [ ] Update `HealthRunContext` call sites once viewer `User` is required
- [ ] Keep existing `NotebookHealthControllerTest` auth green (foreign/anon rejected)

*Existing JUnit/Spring test infrastructure covers the framework — only new test class + context wiring needed.*

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
