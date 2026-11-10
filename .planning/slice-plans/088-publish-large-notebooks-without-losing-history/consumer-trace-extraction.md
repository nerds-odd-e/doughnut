<<running within nix env>>
# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java` | changed path |
| `NotebookGitWebContentExtractedNoteDatingControllerTest.java` | basename of `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java` |
| `extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit` | supplied |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest` | import path of `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java` |
| `NotebookGitWebContentExtractedNoteDatingControllerTest` | import path of `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java` |
| `---\ntopic: physics\n---\nbody` | removed from `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java`:39 |
| `---\norigin: lab\n---\nextracted body` | removed from `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java`:48 |
| `---\ntopic: physics\nexample of: \"[[Matter]]\"\n---\nbody` | removed from `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java`:49 |

## Scope

Whole checkout at `e7f811f43833`, including uncommitted and untracked files: 4181 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

none found

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentPropertyDatingControllerTest.java` | `---\ntopic: physics\n---\nbody` | 42: `saveAt(note, "---\ntopic: physics\n---\nbody", T1);` | excluded: independent property-dating scenario shares a fixture literal only; its setup, assertions and production caller are unchanged |
| `backend/src/test/java/com/odde/donut/services/notebookGit/NotePropertyValueDateRebuildServiceTest.java` | `---\ntopic: physics\n---\nbody` | 50: `.content("---\ntopic: physics\n---\nbody")` | excluded: independent property-dating scenario shares a fixture literal only; its setup, assertions and production caller are unchanged |

### Test support or stand-in

none found

### Journey or feature

none found

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` | `extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit`, `NotebookGitWebContentExtractedNoteDatingControllerTest` | 92: `` Proof: strengthen `NotebookGitWebContentExtractedNoteDatingControllerTest.extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit` through the controller… `` | reused: existing test name remains the plan observation owner; new exact-byte and native-tree assertions directly implement the planned proof |

## Changed surface and completed proof

- Owned change: `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java` only; no production/API changes, no generation trigger.
- Setup: lines 35–50 create a Git-backed notebook and real persisted note; publish typed content at T1 through TextContentController; invoke AiController.createExtractedNote at T2 with literal typed original/new-note content. Expected bytes come from those explicit UTF-8 strings, not the product encoder.
- Exact accepted-byte observations: lines 55–60 compare the raw accepted Git bundle blobs for Atom.md and Electron.md with the full literal expected bytes.
- Complete-tree observation: lines 61–63 use the native notebook diagnosis boundary to compare both accepted and live tree IDs with the accepted bundle's tip tree. Diagnosis does not snapshot/publish the live tree during verification.
- Retained observations: lines 53–54 preserve accepted parent chain and both paths; lines 64–67 preserve T2 accepted commit time, T2 dates for newly added properties, and the prior T1 date for the existing property.
- Literal required proof command, run from the checkout: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
- Owned terminal session 62726 returned exit 0: `BUILD SUCCESSFUL in 1m 39s`; 616 result suites, 3055 tests, 0 failures, 0 errors, 2 skipped. The changed target test passed (1 test, 0 failures/errors/skipped, testcase 0.036s).
- The ordinary command selected isolated database `doughnut_wt_91a387d2611349a3850318e6f5d14dd1_test`. No filtered run, duplicate command or restart occurred. No command remains running.
- Expensive-proof basis: local Mac 48GiB; before launch load 7.15/8.84/8.18 with 91238 free pages and 20924 purgeable pages. One owned full backend command; no other process terminated. The coordinator withheld another heavy proof while this command ran.
- Cheap whitespace proof: `./scripts/run.sh scripts/check_diff_whitespace.sh` returned exit 0 after the edit and again after the full test command.
- Existing correction 9cdb6d998d is reused for C21. This leaf strengthens observation of its accepted extraction result and does not claim to establish the exact historical production caller.
- No changed-surface consumer is left for CI observation. All located consumers have a disposition above. No implementation/proof gap or pending process remains for this leaf. Active work was approximately three minutes, excluding the full backend runtime.
