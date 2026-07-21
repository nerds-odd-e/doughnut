---
phase: 1
slug: shared-cancellation-contract
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-21
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.1.10 in Playwright Chromium browser mode |
| **Config file** | `frontend/vitest.config.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/managedApi/clientSetup.loading.spec.ts tests/managedApi/clientSetup.spec.ts tests/components/commons/LoadingModal.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:verify` |
| **Estimated runtime** | ~5 seconds focused; full frontend verification must remain under the 120-second feedback budget |

---

## Sampling Rate

- **After every task commit:** Run the focused quick command above.
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm frontend:verify`.
- **Before `$gsd-verify-work`:** Focused cancellation/modal specs and full frontend verification must be green.
- **Max feedback latency:** 120 seconds.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | COHE-01 | T-01-01 / T-01-02 | Cancellation removes only its identity-bound state and never retargets a replacement | browser unit/contract | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/managedApi/clientSetup.loading.spec.ts` | ✅ extend existing | ⬜ pending |
| 01-01-02 | 01 | 1 | COHE-01 | T-01-03 | Accepted cancellation wins deterministic races and consumes late resolve/reject without success handling | browser unit/contract | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/managedApi/clientSetup.loading.spec.ts tests/managedApi/clientSetup.spec.ts` | ✅ extend existing | ⬜ pending |
| 01-02-01 | 02 | 2 | COHE-01 | T-01-04 | Only the selected cancelable blocker exposes its state-bound action; noncancelable blockers remain unchanged | component + integration | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/commons/LoadingModal.spec.ts tests/managedApi/clientSetup.loading.spec.ts` | ✅ extend existing | ⬜ pending |
| 01-02-02 | 02 | 2 | COHE-01 | Opted-in callers must narrow completed versus cancelled while default callers retain their current SDK result type | static typecheck | `CURSOR_DEV=true nix develop -c pnpm frontend:build` | ✅ add assertion to existing spec | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `frontend/tests/managedApi/clientSetup.loading.spec.ts` with pending-cancel, resolve-then-cancel, late resolve/reject, concurrency, stale-action, and type-narrowing cases before implementation.
- [ ] Extend `frontend/tests/managedApi/clientSetup.spec.ts` with accepted-cancel no-toast coverage while retaining ordinary error-toast coverage.
- [ ] Extend `frontend/tests/components/commons/LoadingModal.spec.ts` with conditional Cancel rendering/activation and unchanged-default assertions.
- [ ] Update `frontend/tests/helpers/GlobalApiLoadingModal.ts` when production plumbing changes so later tests exercise the selected-state contract.
- Framework install/config gaps: none.

---

## Manual-Only Verifications

All Phase 1 behaviors have automated verification. This Structure phase does not adopt cancellation at a product call site, so no manual UI or full E2E gate is required.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verification or Wave 0 dependencies.
- [ ] Sampling continuity: no 3 consecutive tasks without automated verification.
- [ ] Wave 0 covers all missing references.
- [ ] No watch-mode flags.
- [ ] Feedback latency < 120 seconds.
- [ ] `nyquist_compliant: true` set in frontmatter after validation.

**Approval:** pending
