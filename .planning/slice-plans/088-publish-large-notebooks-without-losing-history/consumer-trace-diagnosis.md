<<running within nix env>>
# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `diagnoseNotebookGit` | supplied |
| `NotebookGitDiagnosis` | supplied |
| `requireMatchingAcceptedTree` | supplied |
| `NotebookGitProjection` | supplied |

## Scope

Whole checkout at `b1713646ac1c`, including uncommitted and untracked files: 4176 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: none named.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` | `NotebookGitDiagnosis` | 26: `import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java` | `diagnoseNotebookGit`, `NotebookGitDiagnosis` | 8: `import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitDiagnosis.java` | `NotebookGitDiagnosis`, `NotebookGitProjection` | 20: `public class NotebookGitDiagnosis {` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProjection.java` | `requireMatchingAcceptedTree`, `NotebookGitProjection` | 16: `public class NotebookGitProjection {` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalAcceptance.java` | `requireMatchingAcceptedTree`, `NotebookGitProjection` | 28: `private final NotebookGitProjection projection;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalFolderRelocation.java` | `NotebookGitProjection` | 15: `private final NotebookGitProjection projection;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalNoteAddition.java` | `NotebookGitProjection` | 32: `private final NotebookGitProjection projection;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalOrdinaryNoteApplication.java` | `NotebookGitProjection` | 24: `private final NotebookGitProjection projection;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java` | `requireMatchingAcceptedTree`, `NotebookGitProjection` | 28: `private final NotebookGitProjection projection;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `open_api_docs.yaml` | `diagnoseNotebookGit` | 3056: `operationId: diagnoseNotebookGit` |run: generateTypeScript passed; frontend consumer proof remains coordinator-owned delivery obligation |
| `packages/generated/donut-backend-api/index.ts` | `diagnoseNotebookGit` | 4: `export type { AcceptCorrectionData, AcceptCorrectionResponse, AcceptCorrectionResponses, AcceptLfsAttachmentTipForTestabilityData, AcceptLfsAttachmentTipForTes…` |run: generateTypeScript passed; frontend consumer proof remains coordinator-owned delivery obligation |
| `packages/generated/donut-backend-api/sdk.gen.ts` | `diagnoseNotebookGit` | 5: `import type { AcceptCorrectionData, AcceptCorrectionResponses, AcceptLfsAttachmentTipForTestabilityData, AcceptLfsAttachmentTipForTestabilityResponses, AddData…` |run: generateTypeScript passed; frontend consumer proof remains coordinator-owned delivery obligation |
| `packages/generated/donut-backend-api/types.gen.ts` | `diagnoseNotebookGit` | 4100: `export type DiagnoseNotebookGitData = {` |run: generateTypeScript passed; frontend consumer proof remains coordinator-owned delivery obligation |
| `scripts/profiling/AnalyzePublication.java` | `NotebookGitProjection` | 88: `"NotebookGitProjection", "DefaultFlush", "AbstractFlushingEventListener",` |excluded: existing names/guard semantics unchanged; additive diagnostic does not change this documentation or profiling boundary |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitDiagnosisControllerTest.java` | `diagnoseNotebookGit`, `NotebookGitDiagnosis` | 14: `import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProjectionDriftControllerTest.java` | `NotebookGitProjection` | 27: `class NotebookGitProjectionDriftControllerTest extends NotebookGitControllerTestBase {` |run: full backend:test_only; new diagnosis controller assertions plus retained projection/publication controller proof |

### Test support or stand-in

none found

### Journey or feature

none found

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/seeds/SEED-121-publish-large-notebooks.md` | `requireMatchingAcceptedTree`, `NotebookGitProjection` | 135: `` - `NotebookGitProjection.requireMatchingAcceptedTree` compares the complete `` |excluded: existing names/guard semantics unchanged; additive diagnostic does not change this documentation or profiling boundary |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` | `NotebookGitProjection` | 20: `` - Reuse `NotebookGitProjection`, encoder, accepted-tree readers, state-loader locking and domain/publication `` |excluded: existing names/guard semantics unchanged; additive diagnostic does not change this documentation or profiling boundary |
| `docs/notebook-publication-profiling.md` | `NotebookGitProjection` | 35: `` Updates call `NotebookGitProjection.requireOneNoteAtPath` and then `` |excluded: existing names/guard semantics unchanged; additive diagnostic does not change this documentation or profiling boundary |
| `packages/generated/donut-backend-api/api-summary.md` | `diagnoseNotebookGit` | 141: `` - `diagnoseNotebookGit`: GET `/api/notebooks/{notebook}/git-diagnosis` -> `DiagnoseNotebookGitResponse` (request: `DiagnoseNotebookGitData`; path: notebook; re… `` |run: generateTypeScript passed; frontend consumer proof remains coordinator-owned delivery obligation |
