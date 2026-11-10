# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `.planning/seeds/SEED-121-publish-large-notebooks.md` | changed path |
| `SEED-121-publish-large-notebooks.md` | basename of `.planning/seeds/SEED-121-publish-large-notebooks.md` |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` | changed path |
| `PLAN.md` | basename of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` | changed path |
| `execution-evidence.md` | basename of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review.md` | changed path |
| `source-review.md` | basename of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review.md` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitCandidateAdmission.java` | changed path |
| `NotebookGitCandidateAdmission.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitCandidateAdmission.java` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java` | changed path |
| `NotebookGitProposalPublisher.java` | basename of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java` |
| `NotebookGitCandidateAdmission` | supplied |
| `NotebookGitProposalPublisher` | supplied |
| `.planning/seeds/SEED-121-publish-large-notebooks` | import path of `.planning/seeds/SEED-121-publish-large-notebooks.md` |
| `SEED-121-publish-large-notebooks` | import path of `.planning/seeds/SEED-121-publish-large-notebooks.md` |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` |
| `PLAN` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md`; in 300 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` |
| `execution-evidence` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review.md` |
| `source-review` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review.md` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher` | import path of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java` |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitCandidateAdmission` | import path of `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitCandidateAdmission.java` |
| `bf84b409edbf400b3bd0e7264daf87570f04f00cda5689ddec1ec924bad14b5f` | removed from `.planning/seeds/SEED-121-publish-large-notebooks.md`:16 |

## Scope

Whole checkout at `4d135fd1c877`, including uncommitted and untracked files: 4178 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `.planning/seeds/SEED-121-publish-large-notebooks.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review.md`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitCandidateAdmission.java`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-reader.mjs` | `source-review` | 84: `// The optional field names the source-review commitments this entry disposes.` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-source-review-record.mjs` | `source-review.md`, `source-review` | 11: `} from "./story-obligation-source-review.mjs";` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-source-review-write.mjs` | `source-review` | 3: `import { tableCells } from "./story-obligation-source-review.mjs";` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-source-review.mjs` | `source-review.md`, `source-review` | 39: `problems: [{ reason: "unreadable-source-review", message }],` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligations-cli.mjs` | `PLAN.md` | 5: `'usage: story-obligations.mjs check --plan <PLAN.md> [--slice <N>] [--completion] \| --planned-completion; or list --plan <PLAN.md> --slice <N>; or record --pla…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligations.mjs` | `source-review` | 16: `} from "./story-obligation-source-review.mjs";` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.planning/done/SEED-113_assimilate-notes-with-a-clear-default-and-result.json` | `PLAN.md` | 11: `"report": "## Dough Intent Coverage: SEED-113#assimilate-notes-with-a-clear-default-and-result\n\n**Outcome: Caught.**\n\n**Story and revision.** SEED-113 \"Ch…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` | `NotebookGitProposalPublisher` | 27: `import com.odde.donut.services.notebookGit.NotebookGitProposalPublisher;` |run: full backend controller suite through unchanged proposal HTTP boundary |
| `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java` | `NotebookGitProposalPublisher` | 10: `import com.odde.donut.services.notebookGit.NotebookGitProposalPublisher;` |run: full backend controller suite through unchanged proposal HTTP boundary |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitDerivedTreeOracleControllerTest.java` | `PLAN.md` | 73: `assertThat(acceptedHistory(notebook).tipPaths(), contains("Ideas/Plan.md"));` |run: full backend suite retains proposal publication/rollback and tree results |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalRenameRollbackControllerTest.java` | `NotebookGitProposalPublisher` | 26: `* Proves {@code NotebookGitProposalPublisher#applyRename} rolls back folder and title with the rest` |run: full backend suite retains proposal publication/rollback and tree results |

### Test support or stand-in

none found

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/done/SEED-115_trust-learning-session-and-assimilation-e2e-feedback.json` | `PLAN.md` | 11: `"report": "# Dough Intent Coverage\n\n## Outcome\n\n**Caught.**\n\n## Story and revision\n\n- Story: SEED-115#trust-learning-session-and-assimilation-e2e-feedb…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/references/delegation.md` | `PLAN.md`, `source-review.md`, `source-review` | 12: `[Obtain the source-only review](obtain-source-review.md#before-planned-delegation).` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/references/deliver-the-change.md` | `PLAN.md` | 82: `` `node '<installed-execute-plan>/scripts/story-obligations.mjs' check --plan '<PLAN.md>' --slice N` `` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/references/finish-or-stop.md` | `PLAN.md` | 194: `` `node '<installed-execute-plan>/scripts/story-obligations.mjs' check --plan '<PLAN.md>' --completion` `` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/references/obtain-source-review.md` | `source-review.md`, `source-review` | 23: `` When you retain the return beside the plan as `source-review.md`, use its `` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/references/story-obligations.md` | `PLAN.md`, `source-review.md`, `source-review` | 55: `[the source review](#record-the-source-review):` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/references/wrap-up.md` | `source-review.md`, `source-review` | 9: `Complete [source-only review at acceptance](obtain-source-review.md#at-acceptance)` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-execute-plan/SKILL.md` | `source-review.md`, `source-review` | 101: `\| Before first implementation delegation \| [delegation](references/delegation.md), [source review](references/obtain-source-review.md#before-planned-delegation…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-land/SKILL.md` | `PLAN.md`, `source-review` | 91: `node <installed>/dough-execute-plan/scripts/story-obligations.mjs check --plan <PLAN.md> --planned-completion` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-product-backlog/references/record-preparation.md` | `PLAN.md` | 241: `` `slice-plans/075-correction/PLAN.md`). For `--approach planned`, `--plan` is `` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-story-refinement/references/planning.md` | `source-review.md`, `source-review` | 148: `[source review](../../dough-execute-plan/references/story-obligations.md#record-the-source-review)` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.agents/skills/dough-story-wrap-up/references/lifecycle-commitments.md` | `PLAN.md`, `source-review` | 4: `[lifecycle owner](../../dough-execute-plan/references/story-obligations.md#record-the-source-review)` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.planning/PRODUCT-BACKLOG.md` | `SEED-121-publish-large-notebooks.md`, `PLAN.md`, `SEED-121-publish-large-notebooks` | 11: `- [Highlight and link to a specific property item](seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item) — SEED-11…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.planning/seeds/SEED-119-highlight-specific-property-item.md` | `PLAN.md` | 16: `{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/087-highlight-specific-property-item/PLAN.md","assessment":"ready","reaso…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-cli.md` | `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md`, `PLAN.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN` | 63: `` \| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` \| `notebook-git-projection` \| 78: `` Documentation structure: before editi… `` |excluded: retained earlier execution observation; no runtime consumer of admission orchestration |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-diagnosis.md` | `.planning/seeds/SEED-121-publish-large-notebooks.md`, `SEED-121-publish-large-notebooks.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md`, `PLAN.md`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java`, `NotebookGitProposalPublisher.java`, `NotebookGitProposalPublisher`, `.planning/seeds/SEED-121-publish-large-notebooks`, `SEED-121-publish-large-notebooks`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN`, `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher` | 35: `` \| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java` \| `requireMatchingAcceptedTree`, `NotebookGitProjection` \| 28: … `` |excluded: retained earlier execution observation; no runtime consumer of admission orchestration |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-extraction.md` | `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md`, `PLAN.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN` | 50: `` \| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` \| `extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit`, `Notebo… `` |excluded: retained earlier execution observation; no runtime consumer of admission orchestration |
| `.planning/TEST-OPTIMIZATION-CANDIDATES.md` | `PLAN.md` | 10: `- **Evidence:** [execution plan results](https://github.com/nerds-odd-e/doughnut/blob/e48370f021a6c393481b4a8e7bcafd7c56c65100/.planning/slice-plans/080-remain…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `DearDough.md` | `execution-evidence`, `source-review` | 292: `Former local code: DD-274. Consumer discovery treated execution-evidence Identity lines and non-story anchors as canonical work, producing duplicate-home and u…` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |
| `docs/notebook-publication-profiling.md` | `NotebookGitProposalPublisher` | 566: `` The owned backend was still in `NotebookGitProposalPublisher.publish` when the `` |reused: dated profiling evidence still names the retained Publisher.publish entry point; no historical observation rewritten |
| `DonutRetrospectiveFindings.md` | `PLAN.md`, `source-review.md`, `source-review` | 78: `` - SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / `681768a71b`, 2026-09-29T07:25:42+08:00, Claude Code/claude-opus-5-5, Op… `` |excluded: generic planning/source-review filename match; immutable candidate admission does not reach this boundary |

## Selected sufficient proof

The existing NotebookGitHttpSupport → NotebookGitProposalPublisher ordinary Publish path is the real caller of the extracted admission owner. Full backend:test_only covers controller acceptance/refusal and safe file, Markdown, folder marker, attachment durability/size and book-source cases without new internal-surface tests. No endpoint signature or generated contract changes. Existing ancestry, drift refusal, identity projection and transaction boundaries stay with Publisher.

## Observation

`CURSOR_DEV=true nix develop -c pnpm backend:test_only` completed exit0; actual tests BUILD SUCCESSFUL in1m49s. XML aggregate616suites/3055tests,0failures,0errors,2skipped. Current XML begins2026-11-10T08:53:35.974Z. Relevant controller results: MarkdownFormat9, AttachmentSizeAdmission3, BookSourceFileProtection2, ProposalRenameRollback3 all pass. No separate focused command, formatting or hook-owned lint ran.
