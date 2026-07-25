---
phase: 02-empty-folder-findings
verified: 2026-07-22T10:23:57Z
status: passed
score: 9/9 must-haves verified
behavior_unverified: 0
overrides_applied: 0
---

# Phase 2: Empty-folder findings Verification Report

**Phase Goal:** As a notebook owner, I want to run authorized Health lint and receive fully empty folders under an empty-folder findings group, so that I can audit structural decay without mutating my notebook.
**Verified:** 2026-07-22T10:23:57Z
**Status:** passed
**Re-verification:** No — initial verification
**Mode:** mvp

## User Flow Coverage

User story: As a notebook owner, I want to run authorized Health lint and receive fully empty folders under an empty-folder findings group, so that I can audit structural decay without mutating my notebook.

| Step | Expected | Evidence | Status |
|------|----------|----------|--------|
| Own a notebook with empty folder shells | Owner-scoped notebook and folders exist for audit | `NotebookHealthControllerTest.ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook`; `EmptyFolderHealthRuleTest` MakeMe fixtures | ✓ |
| Run authorized Health lint | Owner can invoke report-only lint without fix options | `NotebookHealthController.lint` → `POST /api/notebooks/{notebook}/health/lint`; `@Transactional(readOnly = true)`; method takes only `Notebook` path variable; `new HealthRunContext()` (empty) | ✓ |
| Receive empty-folder findings group | Report includes `empty_folders` items for fully empty folders | `EmptyFolderHealthRule.evaluate` emits group `ruleId=empty_folders` with `folderId` + `label`; controller returns `NotebookHealthLintReport` | ✓ |
| Audit without mutation | Notebook folder set unchanged after lint | Folder count before/after assertion in controller test; rule performs only repository reads; `@Transactional(readOnly = true)` | ✓ |

**Outcome clause:** Owner can audit structural decay (empty folders listed under `empty_folders`) without mutating the notebook — **observably true** via authorized lint API (no Health UI in this phase by design; UI is Phase 5).

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | Calling notebook Health lint reports folders whose entire subtree has no notes (recursive; soft-deleted notes do not count as content) | ✓ VERIFIED | `EmptyFolderHealthRule` memoized `subtreeHasLiveNotes` over parent→children map; occupied ids from `findLiveNoteFolderIdsByNotebookId` (`deletedAt IS NULL`). Tests: `listsEveryNestedFullyEmptyFolder`, `liveNoteInDescendantClearsAncestorOccupancy`, `softDeletedNoteDoesNotOccupyFolder` — **PASS** |
| 2 | The empty-folder findings group lists those folders even when auto-fix is not selected (report path does not require fix options) | ✓ VERIFIED | Controller accepts only notebook path var (no body/fix DTO); `HealthRunContext` is empty; generated `LintData.body?: never`. Test: `ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook` — **PASS** |
| 3 | Lint does not delete folders or otherwise mutate notebook data | ✓ VERIFIED | Rule has no delete/save/dissolve; controller `@Transactional(readOnly = true)`; folder count unchanged after `controller.lint`. Test: `ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook` — **PASS** |
| 4 | Only an authorized notebook actor can run lint (foreign/anon callers are rejected) | ✓ VERIFIED | `authorizationService.assertAuthorization(notebook)` (owner write / `hasFullAuthority`, not `assertReadAuthorization`). Tests: `rejectsForeignUser`, `rejectsAnonymousUser` — **PASS** |
| 5 | Folders with non-blank own readmeContent are omitted from the empty_folders group items | ✓ VERIFIED | `isBlankReadme` requires null or `String.isBlank()`; non-blank excluded. Test: `nonBlankReadmeExcludesFolderFromEmptyFolders` — **PASS** |
| 6 | Every matching fully empty folder is listed as its own HealthFindingItem with folderId and label=folder name | ✓ VERIFIED | Loop emits one item per match with `folder.getId()` and `folder.getName()`. Test: `listsEveryNestedFullyEmptyFolder` — **PASS** |
| 7 | Empty-folder group metadata is title Empty folders, severity warning, autoFixable true | ✓ VERIFIED | `title()` / `severity()` / `autoFixable()` on rule; group always emitted. Test: `alwaysEmitsEmptyFoldersGroupWithMetadata` — **PASS** |
| 8 | Rule is a Spring HealthRule bean discovered by HealthRuleRunner with no mutation of notebook data | ✓ VERIFIED | `@Service EmptyFolderHealthRule implements HealthRule`; `HealthRuleRunner(List<HealthRule>)`; service tests exercise `NotebookHealthService.lint` and receive `empty_folders` group — **PASS** |
| 9 | TypeScript OpenAPI client is regenerated from the new endpoint | ✓ VERIFIED | `packages/generated/doughnut-backend-api/sdk.gen.ts` `NotebookHealthController.lint` → `POST /api/notebooks/{notebook}/health/lint`; `LintData.body?: never`; `api-summary.md` lists operation |

**Score:** 9/9 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ---------- | ------ | ------- |
| `backend/.../NoteRepository.java` | `findLiveNoteFolderIdsByNotebookId` live notes only | ✓ VERIFIED | JPQL: `DISTINCT n.folder.id`, `deletedAt IS NULL`, `folder IS NOT NULL` |
| `backend/.../EmptyFolderHealthRule.java` | Fully-empty `HealthRule` bean | ✓ VERIFIED | Substantive recursive evaluate; `@Service`; wired via Spring `List<HealthRule>` |
| `backend/.../EmptyFolderHealthRuleTest.java` | Predicate coverage | ✓ VERIFIED | Recursive, soft-delete, readme exclusion, metadata; all green |
| `backend/.../NotebookHealthController.java` | Authorized `POST .../health/lint` | ✓ VERIFIED | Package-private controller; write auth then service.lint |
| `backend/.../NotebookHealthControllerTest.java` | Owner / foreign / anon / no-mutate | ✓ VERIFIED | All four behaviors covered; green |
| `packages/generated/doughnut-backend-api` | Regenerated health lint operation | ✓ VERIFIED | `sdk.gen.ts`, `types.gen.ts`, `api-summary.md` |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `EmptyFolderHealthRule.evaluate` | `FolderRepository.findByNotebookIdOrderByIdAsc` | one notebook folder load | ✓ WIRED | Line 50 of rule |
| `EmptyFolderHealthRule.evaluate` | `NoteRepository.findLiveNoteFolderIdsByNotebookId` | occupied folder ids from live notes | ✓ WIRED | Lines 51–52 of rule |
| `EmptyFolderHealthRule` | `HealthRuleRunner` | Spring `List<HealthRule>` discovery | ✓ WIRED | `@Service` + runner constructor; proven by service-level tests receiving group |
| `NotebookHealthController.lint` | `AuthorizationService.assertAuthorization(notebook)` | owner write gate | ✓ WIRED | Line 36; no `assertReadAuthorization` |
| `NotebookHealthController.lint` | `NotebookHealthService.lint` | `new HealthRunContext()` | ✓ WIRED | Line 37 |
| `POST /api/notebooks/{notebook}/health/lint` | `NotebookHealthLintReport.groups` | OpenAPI return type | ✓ WIRED | Controller return type + generated client |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| `EmptyFolderHealthRule` | `folders` | `folderRepository.findByNotebookIdOrderByIdAsc` | Live DB folders for notebook | ✓ FLOWING |
| `EmptyFolderHealthRule` | `occupiedFolderIds` | `noteRepository.findLiveNoteFolderIdsByNotebookId` | Live note folder ids (`deletedAt IS NULL`) | ✓ FLOWING |
| `EmptyFolderHealthRule` | `items` | Built from recursive emptiness + blank readme | Dynamic per notebook state | ✓ FLOWING |
| `NotebookHealthController` | lint report | `notebookHealthService.lint` → runner → rules | Real rule evaluation, not static empty | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Empty-folder predicate + authorized lint API | `CURSOR_DEV=true nix develop -c backend/gradlew -p backend test --tests 'com.odde.doughnut.services.health.EmptyFolderHealthRuleTest' --tests 'com.odde.doughnut.controllers.NotebookHealthControllerTest' -Dspring.profiles.active=test` | `BUILD SUCCESSFUL` (exit 0) | ✓ PASS |

### Probe Execution

| Probe | Command | Result | Status |
| ----- | ------- | ------ | ------ |
| — | — | No phase-declared or migration probes | SKIPPED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| EFOL-01 | 02-01, 02-02 | Lint reports folders whose entire subtree has no notes (recursive; soft-deleted ignored) | ✓ SATISFIED | Rule + `EmptyFolderHealthRuleTest` (recursive, soft-delete, live-note occupancy) |
| EFOL-02 | 02-02 | When auto-fix is not selected, list empty folders under empty-folder findings group | ✓ SATISFIED | Report-only endpoint (no fix options); controller returns `empty_folders` items |

No orphaned Phase 2 requirements (EFOL-03 is Phase 3).

### Prohibitions (judgment-tier, evidence-backed)

| Prohibition | Verdict | Evidence |
| ----------- | ------- | -------- |
| must NOT include note-empty folders with non-blank own readmeContent under empty_folders items | satisfied | `nonBlankReadmeExcludesFolderFromEmptyFolders` PASS; `isBlankReadme` gate |
| must NOT mutate notebook data on lint path | satisfied | readOnly transaction; no writes in rule; folder-count test PASS |
| must NOT authorize lint with read-only/bazaar/subscriber entrypoint | satisfied | only `assertAuthorization`; zero `assertReadAuthorization` in controller |
| must NOT add Health tab UI / frontend Health consumers | satisfied | no Health UI components; only backend + generated SDK |
| must NOT implement fix/purge endpoint or empty-folder applicator | satisfied | only `POST .../health/lint`; no purge/fix routes or dissolve on path |
| must NOT implement readme_only_folders rule | satisfied | `README_ONLY_FOLDERS` id reserved only; no `ReadmeOnly*` rule class |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | No TBD/FIXME/XXX, placeholders, or hollow stubs in phase artifacts | — | — |

### Human Verification Required

None. Phase is API-only by design (Health UI deferred to Phase 5); all success criteria are exercised by green backend tests.

### Gaps Summary

No gaps. Phase 2 goal is achieved: authorized report-only lint lists fully empty folders under `empty_folders` without mutating the notebook.

---

_Verified: 2026-07-22T10:23:57Z_
_Verifier: Claude (gsd-verifier)_
