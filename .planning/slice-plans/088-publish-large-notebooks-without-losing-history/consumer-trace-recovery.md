# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` | changed path |
| `NotebookController.java` | basename of `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` |
| `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java` | changed path |
| `NotebookGitHttpSupport.java` | basename of `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitLiveRecovery.java` | changed path |
| `NotebookGitLiveRecovery.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitLiveRecovery.java` |
| `recoverNotebookGit` | supplied |
| `git-recovery` | supplied |
| `RecoverNotebookGitData` | supplied |
| `RecoverNotebookGitResponse` | supplied |

## Scope

Whole checkout at `8f013cefd3b9`, including uncommitted and untracked files: 4186 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `backend/src/main/java/com/odde/donut/controllers/NotebookController.java`, `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitLiveRecovery.java`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `open_api_docs.yaml` | `recoverNotebookGit`, `git-recovery` | 1167: `/api/notebooks/{notebook}/git-recovery:` |reused: successful generateTypeScript; backend RobotsTests compares committed YAML against the current controller contract |
| `packages/generated/donut-backend-api/index.ts` | `recoverNotebookGit`, `RecoverNotebookGitData`, `RecoverNotebookGitResponse` | 4: `export type { AcceptCorrectionData, AcceptCorrectionResponse, AcceptCorrectionResponses, AcceptLfsAttachmentTipForTestabilityData, AcceptLfsAttachmentTipForTes…` |run: generated-client full frontend tests/two typechecks and CLI tests; source-generation output retained unchanged |
| `packages/generated/donut-backend-api/sdk.gen.ts` | `recoverNotebookGit`, `git-recovery`, `RecoverNotebookGitData`, `RecoverNotebookGitResponse` | 5: `import type { AcceptCorrectionData, AcceptCorrectionResponses, AcceptLfsAttachmentTipForTestabilityData, AcceptLfsAttachmentTipForTestabilityResponses, AddData…` |run: generated-client full frontend tests/two typechecks and CLI tests; source-generation output retained unchanged |
| `packages/generated/donut-backend-api/types.gen.ts` | `recoverNotebookGit`, `git-recovery`, `RecoverNotebookGitData`, `RecoverNotebookGitResponse` | 2540: `export type RecoverNotebookGitData = {` |run: generated-client full frontend tests/two typechecks and CLI tests; source-generation output retained unchanged |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitLiveRecoveryAdmissionControllerTest.java` | `recoverNotebookGit` | 24: `assertThrows(RuntimeException.class, () -> controller.recoverNotebookGit(notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));` |run: full backend:test_only; exact recovery setup/assertions in this controller test |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitLiveRecoveryAtomicControllerTest.java` | `recoverNotebookGit` | 39: `var failure = assertThrows(RuntimeException.class, () -> controller.recoverNotebookGit(notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));` |run: full backend:test_only; exact recovery setup/assertions in this controller test |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitLiveRecoveryControllerTest.java` | `recoverNotebookGit` | 52: `String accepted = controller.recoverNotebookGit(notebook.getId(), new Review(report.acceptedHead(), report.liveTree()));` |run: full backend:test_only; exact recovery setup/assertions in this controller test |

### Test support or stand-in

none found

### Journey or feature

none found

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-story-wrap-up/references/follow-up-disposition.md` | `git-recovery` | 35: `[before-cleanup commit](../SKILL.md#commit-closure-inputs-and-preserve-git-recovery),` |excluded: generic git-recovery prose anchor refers to planning-history retention, not the notebook endpoint |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-admission.md` | `backend/src/main/java/com/odde/donut/controllers/NotebookController.java`, `NotebookController.java`, `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java`, `NotebookGitHttpSupport.java` | 54: `` \| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` \| `NotebookGitProposalPublisher` \| 27: `import com.odde.donut.services.notebookGit… `` |excluded: retained earlier trace names controller paths; no runtime or behavioral dependency |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-diagnosis.md` | `backend/src/main/java/com/odde/donut/controllers/NotebookController.java`, `NotebookController.java`, `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java`, `NotebookGitHttpSupport.java` | 27: `` \| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` \| `NotebookGitDiagnosis` \| 26: `import com.odde.donut.services.notebookGit.Noteboo… `` |excluded: retained earlier trace names controller paths; no runtime or behavioral dependency |
| `packages/generated/donut-backend-api/api-summary.md` | `recoverNotebookGit`, `git-recovery`, `RecoverNotebookGitData`, `RecoverNotebookGitResponse` | 133: `` - `recoverNotebookGit`: POST `/api/notebooks/{notebook}/git-recovery` -> `RecoverNotebookGitResponse` (request: `RecoverNotebookGitData`; path: notebook; body:… `` |run: generated-client full frontend tests/two typechecks and CLI tests; source-generation output retained unchanged |

## Changed contract and sufficient consumer proof

Controller wiring adds NotebookGitLiveRecovery; Spring context/controller coverage owns that integration. POST /api/notebooks/{notebook}/git-recovery takes Review{acceptedHead,liveTree} and returns the accepted head. Generated sdk/types/index/summary add this operation without changing existing methods or body definitions. Full raw trace and per-file dispositions are retained at /Users/terryyin/.codex/tmp/seed121-recovery-attempt/consumer-trace-disposed.md; ordinary CLI command integration is the next planned capability, installed continuation belongs to8. No API caller yet outside controller tests/generated SDK.

## Fixture correction

RecallLogBuilder sets the owning memoryTracker relation, while MemoryTracker.getRecallCount reads its inverse collection. Capturing the same managed tracker before commit observes the cached three-log list. The corrected fixture captures the baseline by a separately committed find after all four log rows are durable, then compares the recovered tracker at the same boundary. No product code changed to repair this failure. Prior failed XML is retained at /tmp/seed121-continuation-first-TEST-*NotebookGitLiveRecovery*.xml.

## Terminal proof observations

On Territory.local, CURSOR_DEV=true nix develop -c pnpm backend:test_only exited0 (session52438),3065tests/0failures/0errors/2skipped,1m49s. Recovery controller5cases passed at2026-11-10T09:17:16.940Z; admission4 and atomic1 passed at09:18:02Z. Success now executes recall-log identity/count retention and the topic changedAt/related authored-reference index assertions after native parent/tree/history checks. The earlier failed count observation is retained separately and attributed to the inverse-collection fixture lifecycle, not waived.

CURSOR_DEV=true nix develop -c pnpm frontend:test exited0(session79361),399files/2470tests,90.00s. CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit exited0(session96261); plain tsc --noEmit exited0(session38130). CURSOR_DEV=true nix develop -c pnpm cli:test exited0(session62952),68files/442Vitest tests,51.54s; the package Python runner selected0tests (no Python coverage claimed). No frontend/CLI call-site edits were needed for the additive operation. Prior successful generateTypeScript is reused byte-for-byte; source contract unchanged after generation. Coordinator owns selective formatting/check-only hook and OpenAPI lint; no standalone lint or formatting ran here.

# Independent refactor trace

# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitLiveRecovery.java` | changed path |
| `NotebookGitLiveRecovery.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitLiveRecovery.java` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitNoteDocuments.java` | changed path |
| `NotebookGitNoteDocuments.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitNoteDocuments.java` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalNoteAddition.java` | changed path |
| `NotebookGitProposalNoteAddition.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalNoteAddition.java` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalOrdinaryNoteApplication.java` | changed path |
| `NotebookGitProposalOrdinaryNoteApplication.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalOrdinaryNoteApplication.java` |
| `readValidatedDocument` | supplied |
| `NotebookGitNoteDocuments` | supplied |
| `NotebookGitProposalNoteAddition` | supplied |
| `NotebookGitProposalOrdinaryNoteApplication` | supplied |
| `NotebookGitLiveRecovery` | supplied |

## Scope

Whole checkout at `8f013cefd3b9`, including uncommitted and untracked files: 4186 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitLiveRecovery.java`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitNoteDocuments.java`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalNoteAddition.java`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalOrdinaryNoteApplication.java`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` | `NotebookGitLiveRecovery` | 26: `import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |
| `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java` | `NotebookGitLiveRecovery` | 8: `import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalDocumentApplication.java` | `NotebookGitProposalNoteAddition` | 24: `private final NotebookGitProposalNoteAddition noteAddition;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java` | `NotebookGitProposalNoteAddition`, `NotebookGitProposalOrdinaryNoteApplication` | 33: `private final NotebookGitProposalNoteAddition noteAddition;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalRelocatedDocuments.java` | `NotebookGitProposalNoteAddition` | 45: `NotebookGitProposalNoteAddition noteAddition,` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitLiveRecoveryAdmissionControllerTest.java` | `NotebookGitLiveRecovery` | 11: `import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery.Review;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitLiveRecoveryAtomicControllerTest.java` | `NotebookGitLiveRecovery` | 10: `import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery.Review;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitLiveRecoveryControllerTest.java` | `NotebookGitLiveRecovery` | 15: `import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery.Review;` | run: full backend:test_only preserves normal addition/edit/rename/relocation and recovery validation, derived state and rollback through controller entry points |

### Test support or stand-in

none found

### Journey or feature

none found

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-diagnosis.md` | `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalNoteAddition.java`, `NotebookGitProposalNoteAddition.java`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalOrdinaryNoteApplication.java`, `NotebookGitProposalOrdinaryNoteApplication.java`, `NotebookGitProposalNoteAddition`, `NotebookGitProposalOrdinaryNoteApplication` | 33: `` \| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalNoteAddition.java` \| `NotebookGitProjection` \| 32: `private final NotebookGitPr… `` | excluded: retained earlier trace is evidence naming the same classes, not a runtime consumer of document reading |
| `docs/notebook-publication-profiling.md` | `NotebookGitProposalNoteAddition` | 37: `` `NotebookGitProposalNoteAddition.apply`, create a note with `NoteFactory`, then `` | reused: dated profiling names NoteAddition.apply, whose responsibility and behavior remain; no historical observation rewritten |

## Refactor decision and proof

NotebookGitProposalNoteAddition held authored-document reading used by ordinary edits and now recovery, which do not add notes. NotebookGitNoteDocuments now owns that exact UTF-8 reading, authored-property validation/error context and document construction. Addition, modification/rename and recovery retain their real callers and use it; no new policy, dependency subsystem or API contract. Existing addition and ordinary-application placement/persistence stay in their owners. Changed caller files were inspected directly because the trace excludes its supplied changed files.

Backend controller proof is renewed after the internal dependency move. Accepted generation, frontend399files/2470tests, both frontend typechecks and CLI68files/442tests remain applicable: controller signatures, generated artifacts and their consumer setup/assertions were untouched. No new test surface is needed for the internal reader. No uncovered consumers remain. Coordinator owns formatting and hook validation.

## Terminal verification

CURSOR_DEV=true nix develop -c pnpm backend:test_only exited0 (session42272), BUILD SUCCESSFUL in1m42s; XML3065tests,0failures/errors,2skipped on Territory.local. Recovery admission4cases at2026-11-10T09:29:15.985Z, atomic1 at09:29:16.054Z, success/stale/authorization/serialization5 at09:29:16.086Z; retained MarkdownFormat9 at09:29:22.504Z and composed-move/edit4 at09:29:11.830Z passed. No controller/test setup or observing assertions changed. Required full backend supersedes the pre-refactor backend result; generated/front-end/CLI evidence remains unchanged. ./scripts/run.sh bash scripts/check_diff_whitespace.sh exited0. No formatter or standalone lint ran.
