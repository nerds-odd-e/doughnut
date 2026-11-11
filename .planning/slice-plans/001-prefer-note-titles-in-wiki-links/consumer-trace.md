<<running within nix env>>
# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md` | changed path |
| `PLAN.md` | basename of `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md` |
| `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/source-review.md` | changed path |
| `source-review.md` | basename of `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/source-review.md` |
| `backend/src/main/java/com/odde/donut/services/WikiLinkCandidateClassifier.java` | changed path |
| `WikiLinkCandidateClassifier.java` | basename of `backend/src/main/java/com/odde/donut/services/WikiLinkCandidateClassifier.java` |
| `backend/src/main/java/com/odde/donut/services/WikiLinkNoteCandidates.java` | changed path |
| `WikiLinkNoteCandidates.java` | basename of `backend/src/main/java/com/odde/donut/services/WikiLinkNoteCandidates.java` |
| `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests.java` | changed path |
| `NoteControllerAuthoredPortablePathTests.java` | basename of `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests.java` |
| `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkAmbiguityTests.java` | changed path |
| `NoteControllerShowWikiLinkAmbiguityTests.java` | basename of `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkAmbiguityTests.java` |
| `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java` | changed path |
| `NoteControllerShowWikiLinkTitlePrecedenceTests.java` | basename of `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java` |
| `backend/src/test/java/com/odde/donut/controllers/TextContentControllerUpdateNoteContentAliasCardinalityTests.java` | changed path |
| `TextContentControllerUpdateNoteContentAliasCardinalityTests.java` | basename of `backend/src/test/java/com/odde/donut/controllers/TextContentControllerUpdateNoteContentAliasCardinalityTests.java` |
| `WikiLinkResolver` | supplied |
| `authoredPortablePath` | supplied |
| `authoredWikiLinkTokenForInsert` | supplied |
| `authoredWikiLinkTokenFromOriginalPath` | supplied |
| `forNotebookAndTitle` | supplied |
| `readableNotebookMatches` | supplied |
| `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/PLAN` | import path of `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md` |
| `PLAN` | import path of `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md`; in 303 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `backend/src/main/java/com/odde/donut/services/WikiLinkCandidateClassifier` | import path of `backend/src/main/java/com/odde/donut/services/WikiLinkCandidateClassifier.java` |
| `WikiLinkCandidateClassifier` | import path of `backend/src/main/java/com/odde/donut/services/WikiLinkCandidateClassifier.java` |
| `backend/src/main/java/com/odde/donut/services/WikiLinkNoteCandidates` | import path of `backend/src/main/java/com/odde/donut/services/WikiLinkNoteCandidates.java` |
| `WikiLinkNoteCandidates` | import path of `backend/src/main/java/com/odde/donut/services/WikiLinkNoteCandidates.java` |
| `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests` | import path of `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests.java` |
| `NoteControllerAuthoredPortablePathTests` | import path of `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests.java` |
| `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkAmbiguityTests` | import path of `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkAmbiguityTests.java` |
| `NoteControllerShowWikiLinkAmbiguityTests` | import path of `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkAmbiguityTests.java` |
| `backend/src/test/java/com/odde/donut/controllers/TextContentControllerUpdateNoteContentAliasCardinalityTests` | import path of `backend/src/test/java/com/odde/donut/controllers/TextContentControllerUpdateNoteContentAliasCardinalityTests.java` |
| `TextContentControllerUpdateNoteContentAliasCardinalityTests` | import path of `backend/src/test/java/com/odde/donut/controllers/TextContentControllerUpdateNoteContentAliasCardinalityTests.java` |
| `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/source-review` | import path of `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/source-review.md` |
| `source-review` | import path of `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/source-review.md` |
| `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests` | import path of `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java` |
| `NoteControllerShowWikiLinkTitlePrecedenceTests` | import path of `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java` |
| `WikiDup Pantry/color` | removed from `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests.java`:79 |
| `scalar` | declared in `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java`; in 90 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `list` | declared in `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java`; in 1473 files, too broad to list consumers by itself; supply it as a `--term` to list them |

## Scope

Whole checkout at `f6d89e3470a6`, including uncommitted and untracked files: 4223 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md`, `.planning/slice-plans/001-prefer-note-titles-in-wiki-links/source-review.md`, `backend/src/main/java/com/odde/donut/services/WikiLinkCandidateClassifier.java`, `backend/src/main/java/com/odde/donut/services/WikiLinkNoteCandidates.java`, `backend/src/test/java/com/odde/donut/controllers/NoteControllerAuthoredPortablePathTests.java`, `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkAmbiguityTests.java`, `backend/src/test/java/com/odde/donut/controllers/NoteControllerShowWikiLinkTitlePrecedenceTests.java`, `backend/src/test/java/com/odde/donut/controllers/TextContentControllerUpdateNoteContentAliasCardinalityTests.java`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-reader.mjs` | `source-review` | 84: `// The optional field names the source-review commitments this entry disposes.` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-source-review-record.mjs` | `source-review.md`, `source-review` | 11: `} from "./story-obligation-source-review.mjs";` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-source-review-write.mjs` | `source-review` | 3: `import { tableCells } from "./story-obligation-source-review.mjs";` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligation-source-review.mjs` | `source-review.md`, `source-review` | 39: `problems: [{ reason: "unreadable-source-review", message }],` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligations-cli.mjs` | `PLAN.md` | 5: `'usage: story-obligations.mjs check --plan <PLAN.md> [--slice <N>] [--completion] \| --planned-completion; or list --plan <PLAN.md> --slice <N>; or record --pla…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/scripts/story-obligations.mjs` | `source-review` | 16: `} from "./story-obligation-source-review.mjs";` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.planning/done/SEED-113_assimilate-notes-with-a-clear-default-and-result.json` | `PLAN.md` | 11: `"report": "## Dough Intent Coverage: SEED-113#assimilate-notes-with-a-clear-default-and-result\n\n**Outcome: Caught.**\n\n**Story and revision.** SEED-113 \"Ch…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.planning/done/SEED-116_run-repository-commands-without-interactive-shell-setup.json` | `PLAN.md` | 11: `"report": "**Outcome: Caught.** One Dough Intent Coverage check found that the existing test catches a cached-launch regression in the selected story promise.\…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.planning/done/SEED-117_find-the-note-and-property-after-recall.json` | `PLAN.md` | 11: `` "report": "**Caught.** One existing assertion detects the selected first-match navigation fault.\n\nStory: `SEED-117#find-the-note-and-property-after-recall`, … `` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `backend/src/main/java/com/odde/donut/algorithms/WikiLinkMarkdownRewrite.java` | `authoredPortablePath` | 14: `public static String newInnerForAuthoredPortablePath(` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/controllers/dto/AuthoredPortablePath.java` | `authoredPortablePath` | 5: `public record AuthoredPortablePath(` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/controllers/NoteController.java` | `authoredPortablePath` | 218: `public AuthoredPortablePath authoredPortablePath(` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/entities/repositories/AuthoredNoteReferenceInboundFacade.java` | `WikiLinkResolver` | 9: `import com.odde.donut.services.WikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/entities/repositories/AuthoredNoteReferenceRowRepository.java` | `WikiLinkResolver` | 25: `* com.odde.donut.services.WikiLinkResolver#resolveReference}. Ordered by referrer note id then` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/AccidentalWikiLinkMatches.java` | `WikiLinkNoteCandidates` | 58: `return WikiLinkNoteCandidates.distinctByNoteId(notes);` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/health/DeadWikiLinkHealthRule.java` | `WikiLinkResolver` | 9: `import com.odde.donut.services.WikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/MemoryTrackerService.java` | `WikiLinkResolver` | 49: `WikiLinkResolver wikiLinkResolver,` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/NoteConstructionService.java` | `WikiLinkResolver` | 44: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/NoteReferenceHandling.java` | `WikiLinkResolver` | 32: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/NoteReferenceService.java` | `WikiLinkResolver` | 37: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/NoteService.java` | `WikiLinkResolver` | 27: `WikiLinkResolver wikiLinkResolver,` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/PortablePathAuthoring.java` | `WikiLinkResolver`, `authoredPortablePath` | 12: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/SpellingRecallGrading.java` | `WikiLinkResolver` | 25: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/UnassimilatedPropertyService.java` | `WikiLinkResolver` | 20: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/WikiLinkReferenceCapture.java` | `WikiLinkResolver` | 24: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/WikiLinkRelocationRewrite.java` | `WikiLinkResolver` | 34: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/WikiLinkResolver.java` | `WikiLinkResolver`, `WikiLinkCandidateClassifier`, `WikiLinkNoteCandidates` | 19: `public class WikiLinkResolver {` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/WikiLinkRewriteService.java` | `WikiLinkResolver` | 29: `private final WikiLinkResolver wikiLinkResolver;` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/main/java/com/odde/donut/services/WikiLinkRewriteSupport.java` | `WikiLinkResolver`, `authoredPortablePath` | 62: `String authoredPortablePath =` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `frontend/src/components/recall/MatchedNoteWikiLinkOrRelationshipOffer.vue` | `authoredWikiLinkTokenForInsert` | 29: `import { authoredWikiLinkTokenForInsert } from "@/utils/wikiLinkAuthoring"` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/src/components/wiki-link-or-relationship/SearchForm.vue` | `authoredWikiLinkTokenForInsert`, `authoredWikiLinkTokenFromOriginalPath` | 47: `authoredWikiLinkTokenForInsert,` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` | `authoredWikiLinkTokenForInsert` | 2: `import { authoredWikiLinkTokenForInsert } from "./wikiLinkAuthoring"` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/src/utils/wikiLinkAuthoring.ts` | `authoredPortablePath`, `authoredWikiLinkTokenForInsert`, `authoredWikiLinkTokenFromOriginalPath` | 6: `async function authoredPortablePathFor(` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `open_api_docs.yaml` | `authoredPortablePath` | 2982: `operationId: authoredPortablePath` |  excluded: unchanged API schema/signatures; selection changes only existing response values |
| `packages/generated/donut-backend-api/index.ts` | `authoredPortablePath` | 4: `export type { AcceptCorrectionData, AcceptCorrectionResponse, AcceptCorrectionResponses, AcceptLfsAttachmentTipForTestabilityData, AcceptLfsAttachmentTipForTes…` |  excluded: unchanged API schema/signatures; selection changes only existing response values |
| `packages/generated/donut-backend-api/sdk.gen.ts` | `authoredPortablePath` | 5: `import type { AcceptCorrectionData, AcceptCorrectionResponses, AcceptLfsAttachmentTipForTestabilityData, AcceptLfsAttachmentTipForTestabilityResponses, AddData…` |  excluded: unchanged API schema/signatures; selection changes only existing response values |
| `packages/generated/donut-backend-api/types.gen.ts` | `authoredPortablePath` | 1256: `export type AuthoredPortablePath = {` |  excluded: unchanged API schema/signatures; selection changes only existing response values |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitDerivedTreeOracleControllerTest.java` | `PLAN.md` | 73: `assertThat(acceptedHistory(notebook).tipPaths(), contains("Ideas/Plan.md"));` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/test/java/com/odde/donut/services/WikiLinkResolverFrontmatterAndBodyResolutionTest.java` | `WikiLinkResolver` | 15: `class WikiLinkResolverFrontmatterAndBodyResolutionTest extends SpringTestBase {` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `backend/src/test/java/com/odde/donut/services/WikiLinkResolverReferenceResolutionTest.java` | `WikiLinkResolver` | 17: `* The domain-stable {@link WikiLinkResolver#resolveReference} entry point: one resolution result` |  run: full backend unit suite; controller and resolver observations in implementation handoff |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | `authoredPortablePath` | 145: `mockSdkService(NoteController, "authoredPortablePath", {` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts` | `authoredPortablePath` | 31: `mockSdkService(NoteController, "authoredPortablePath", {` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/tests/components/recall/MatchedNoteWikiLinkOrRelationshipOffer.spec.ts` | `authoredPortablePath` | 120: `const authoredPortablePathSpy = mockSdkService(` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` | `authoredPortablePath` | 27: `mockSdkService(NoteController, "authoredPortablePath", {` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` | `authoredPortablePath` | 22: `mockSdkService(NoteController, "authoredPortablePath", { portablePath })` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |
| `frontend/tests/wiki-link-or-relationship/SearchDialog.spec.ts` | `authoredPortablePath` | 214: `mockSdkService(NoteController, "authoredPortablePath", {` |  reused: full frontend suite and both typechecks at 090302e; unchanged frontend/test/generated content, inspected response consumption |

### Test support or stand-in

none found

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/done/SEED-115_trust-learning-session-and-assimilation-e2e-feedback.json` | `PLAN.md` | 11: `"report": "# Dough Intent Coverage\n\n## Outcome\n\n**Caught.**\n\n## Story and revision\n\n- Story: SEED-115#trust-learning-session-and-assimilation-e2e-feedb…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/references/delegation.md` | `PLAN.md`, `source-review.md`, `source-review` | 12: `[Obtain the source-only review](obtain-source-review.md#before-planned-delegation).` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/references/deliver-the-change.md` | `PLAN.md` | 82: `` `node '<installed-execute-plan>/scripts/story-obligations.mjs' check --plan '<PLAN.md>' --slice N` `` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/references/finish-or-stop.md` | `PLAN.md` | 177: `` `node '<installed-execute-plan>/scripts/story-obligations.mjs' check --plan '<PLAN.md>' --completion` `` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/references/obtain-source-review.md` | `source-review.md`, `source-review` | 23: `` When you retain the return beside the plan as `source-review.md`, use its `` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/references/story-obligations.md` | `PLAN.md`, `source-review.md`, `source-review` | 55: `[the source review](#record-the-source-review):` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/references/wrap-up.md` | `source-review.md`, `source-review` | 9: `Complete [source-only review at acceptance](obtain-source-review.md#at-acceptance)` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-execute-plan/SKILL.md` | `source-review.md`, `source-review` | 101: `\| Before first implementation delegation \| [delegation](references/delegation.md), [source review](references/obtain-source-review.md#before-planned-delegation…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-land/SKILL.md` | `PLAN.md`, `source-review` | 91: `node <installed>/dough-execute-plan/scripts/story-obligations.mjs check --plan <PLAN.md> --planned-completion` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-product-backlog/references/record-preparation.md` | `PLAN.md` | 241: `` `slice-plans/075-correction/PLAN.md`). For `--approach planned`, `--plan` is `` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-story-refinement/references/planning.md` | `source-review.md`, `source-review` | 148: `[source review](../../dough-execute-plan/references/story-obligations.md#record-the-source-review)` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.agents/skills/dough-story-wrap-up/references/lifecycle-commitments.md` | `PLAN.md`, `source-review` | 4: `[lifecycle owner](../../dough-execute-plan/references/story-obligations.md#record-the-source-review)` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.planning/PRODUCT-BACKLOG.md` | `PLAN.md` | 11: `- [Prefer note titles in wiki links](seeds/SEED-122-prefer-note-titles-in-wiki-links.md#prefer-note-titles-in-wiki-links) — SEED-122#prefer-note-titles-in-wiki…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.planning/seeds/SEED-122-prefer-note-titles-in-wiki-links.md` | `PLAN.md` | 16: `{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md","assessment":"ready","reaso…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `.planning/TEST-OPTIMIZATION-CANDIDATES.md` | `PLAN.md` | 10: `- **Evidence:** [execution plan results](https://github.com/nerds-odd-e/doughnut/blob/e48370f021a6c393481b4a8e7bcafd7c56c65100/.planning/slice-plans/080-remain…` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `DearDough.md` | `source-review.md`, `source-review` | 399: `## ODF-436 — The source-review link form did not fit a plan whose Source section is a list` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `DonutRetrospectiveFindings.md` | `PLAN.md`, `source-review.md`, `source-review` | 80: `` - SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / `681768a71b`, 2026-09-29T07:25:42+08:00, Claude Code/claude-opus-5-5, Op… `` |  excluded: planning/guidance reference to PLAN or source-review; wiki candidate selection does not affect its boundary |
| `packages/generated/donut-backend-api/api-summary.md` | `authoredPortablePath` | 118: `` - `authoredPortablePath`: GET `/api/notes/{note}/authored-portable-path` -> `AuthoredPortablePathResponse` (request: `AuthoredPortablePathData`; path: note; qu… `` |  excluded: unchanged API schema/signatures; selection changes only existing response values |
