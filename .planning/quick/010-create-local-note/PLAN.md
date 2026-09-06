# Create a new note locally

## Source and status

- Source: [SEED-009 Story 4](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-4).
- Status: planned; implementation has not started.
- Developer-approved boundary: one added note per commit, at the notebook root
  or in an existing folder represented in accepted history.
- Follow-up: [Story 11](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-11)
  owns multiple additions and mixed additions/edits in one commit.

## Goal and scope

An authenticated notebook owner commits one new Portable Markdown note locally
and runs `donut notebook publish <directory>`. Donut accepts that exact commit
and displays one fresh note at the authored path. The note then participates
in the existing content-edit and receive workflow.

Reuse the existing binding, owner authorization, clean `main`, direct-child
ancestry, typed-Markdown, projection-drift, and atomic-publication contracts.
The filename supplies the display name; preserve authored content exactly.
Copies receive fresh identities and never inherit another note's private data.
Reject invalid destinations and unsupported combinations without partial writes.

Exclude new folders, README authoring, attachments, multiple changed files,
several unpublished commits, deletion, rename/move, web structural
synchronization, drift repair, and rebase/conflict handling. An empty notebook
whose projection matches its accepted empty tree is an eligible root location.

## Execution context and decisions

- Active CI observer: coordinator `root`, checkout
  `/Users/terryyin/git/doughnut`, repository `nerds-odd-e/doughnut`, branch
  `main`, receipt directory `/tmp/donut-ci-501/watch-wLj96p`, PID `61936`,
  session `1200`, yielded cell `12`.

- `NotebookController.publishNotebookGitProposal` is the stable backend entry
  point. Extend the existing bundle fixtures and controller test base with real
  Git objects and committed database observations.
- `NotebookGitProposalTreeShape` currently rejects every added path. Change only
  the single-note contract; retain unsupported-tree rejection. Existing
  tree-shape fixtures include untyped Markdown, so positive addition fixtures
  must use a conformant live projection, not just an accepted synthetic tree.
- `NotebookGitProposalPublisher` owns one serializable transaction and accepted
  binding update. Its final projection comparison must include the new note:
  the `liveNotes` list loaded before creation is not automatically refreshed.
- Reuse note initialization/creator attribution from `NoteConstructionService`
  and derived-content persistence from `AuthoredNoteDocumentPersistence` where
  appropriate. Do not route Git content through a repair/enrichment operation.
  Validate title DTO constraints explicitly at this non-DTO entry point; Java
  method calls do not automatically apply HTTP `@Valid` validation.
- Reuse `NoteTitlePlacementRules` for a soft-deleted title collision. A local
  addition must not restore or reuse that entity. Names normalized differently
  by Donut must reject if accepting them would rewrite the proposed tree.
- Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs paths and lossless Markdown. Advisory `index`/`log` names succeed
  and remain visible through the existing notebook-health warning; no new
  publish response or warning transport is required.
- Accepted [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  permits propagation of unexpected failures. Atomic rollback is an explicit
  business promise and uses the existing late-binding-save failure harness.
  Proposed ADR 0002 remains Proposed; the seed records the approved workflow.
- Keep the existing Git-bundle endpoint and response shape. No schema migration
  or generated API change is expected. Generate clients if an actual signature
  change becomes necessary, rather than editing generated files.
- One bounded Structure leaf extracts the existing fresh-note initialization
  used by web creation, immediately before Git root-note acceptance. It must
  have a live existing caller and preserve web behavior. No batch-publication
  abstraction, new transaction boundary, or unused preparation is included.

## Refinement assessment

All original slices were planned; there is no completed execution evidence or
attempt-owned product WIP to preserve. The selected story and sibling ordering
are unchanged.

| Original slice | Assessment | Replacement |
|---|---|---|
| 1. Atomic root creation | Refine: validation, construction extraction, and acceptance integration were bundled | 1–3 |
| 2. Existing folder | Ready: one placement rule and its rejection boundary | 4 |
| 3. Copy identity | Ready: one identity outcome, fixture-driven proof | 5 |
| 4. Unavailable destination | Refine: deleted-title, stale-head, and projection-drift cases exercise different rules | 6–8 |
| 5. Advisory names | Ready: one advisory-name policy | 9 |
| 6. Retry | Ready: one idempotency outcome | 10 |
| 7. Installed CLI | Ready: existing harness needs only explicit addition staging | 11 |
| 8. Continued editing | Refine: local publication and web save are separate triggers | 12–13 |
| 9. Receive | Ready: one real-Git fast-forward fixture variation | 14 |

## Ordered slices

Each leaf below targets about five minutes for its change, focused proof, and
local cleanup, subject to the verification-runtime exception below. "One proof
loop" means one owning behavior boundary; several data variations or success/
rollback cases of the same atomic operation do not create a second feature.
Keep rejection guards effective from the first accepting commit.

### 1. Explain why a local root-note proposal is invalid
Type: Behavior
Status: done
Proof: Publish-controller rejection cases identify the offending path and
reason, preserve the accepted binding, and create no note.

Behavior: An owner proposes one added root Markdown file with invalid content
or title, or an unsupported combination of changes → publication gives
actionable validation or split-commit guidance without any remote mutation.

Teach the existing tree walker to describe one modified or added regular note
path while continuing to reject deletes/moves, nonregular/unsafe paths,
README changes, and multiple changes. Keep the descriptor local to publication;
do not design a general change-set API. For a single root addition, reuse
`NotebookGitProposalMarkdownFormat`, `AuthoredNoteContent.assertValidForSave`,
and the existing title constraints. Report filename normalization mismatches
as rejection rather than silently rewriting a proposed path.

Drive a small data table through `publishNotebookGitProposal`: missing/invalid
type or YAML, invalid/reserved title, and add-plus-edit. Retain the existing
shape-validator cases for other unsupported input. Do not reproduce their full
matrix solely for new test-class coverage. Align obsolete "every addition
rejects before content inspection" fixture expectations with these diagnostics.

**Interim stopping point:** A valid addition still reports that creation is not
yet supported, and accepted history is unchanged. This is a usable diagnostic
improvement for authors correcting local files. Slice 3 removes that temporary
root-addition rejection; permanent invalid-input checks remain. Existing
single-note editing continues through its current path.

Sizing basis: one rejecting controller path using existing validators; no
database creation or E2E wiring. About five minutes, medium confidence.

### 2. Share existing fresh-note initialization
Type: Structure
Status: done
Proof: Existing notebook note-creation and extraction controller tests remain
green, including creator attribution, default content, and deleted-title rules.

Structure: Extract the current `NoteConstructionService.createNote` operation
into a small shared collaborator, preserving initialization, default authored
content, creator attribution, reserved-title and soft-deleted-title checks.
The existing web construction service must use it in this same slice.

Enables the immediately following Behavior, slice 3, to obtain a fresh Note
without duplicating those rules. Keep web content preparation, Wikidata
enrichment, realm building, and later reference refresh in their existing
callers. The Git publisher will replace initial default content with an exact,
already-validated authored document inside its transaction; Git bytes must
never pass through `prepareDocumentForSave` normalization.

Use existing `NotebookNoteCreateControllerTest`,
`NotebookRootNoteCreationWithWikidataTests`, and
`AiControllerCreateExtractedNoteTest` as regression evidence. Do not introduce
factory-level tests, publish additions, or refactor the rest of note creation.

Sizing basis: move one existing construction routine and route its live caller,
with no product change. About five minutes, medium confidence.

### 3. Accept a root note and its commit atomically
Type: Behavior
Status: done
Proof: Publish/show/download controller round trip observes one fresh note and
the exact proposed commit/tree; the existing forced late-binding-save failure
case observes no new note, creator/reference residue, or accepted revision.

Behavior: The live notebook matches accepted main and one valid root addition
passes slice 1's checks → the authored commit and its new note become accepted
together, or both roll back if acceptance fails.

Remove the interim root-addition rejection. Verify the accepted parent
projection before creating anything; obtain a fresh note using slice 2's
collaborator and persist `AuthoredNoteDocument.fromContent` through the existing
authored-document persistence. Include the created note in the final projection
comparison instead of reusing an unchanged pre-creation `liveNotes` snapshot.
Keep the existing binding-write transaction and ancestry/authentication guards.

Use one canonical payload with an unknown valid type, author YAML title and
unknown keys, an H1, and an authored wiki reference. The filename is the display
name and downloaded bytes remain exact; observe the reference through the note
view. The empty notebook is the smallest valid starting fixture; a populated
notebook variation preserves unchanged entries. No history reset or resnapshot
is part of acceptance.

The failure variation belongs here, using
`NotebookGitPublicationAtomicControllerTest` and its existing failure injection.
It tests the same atomic publication outcome, not a later hardening task.
Check committed state outside the failed transaction. Reserved/deleted title
checks must already run through the shared factory; slice 6 supplies the
specific user-facing deleted-destination example.

**Stopping point:** Root additions and existing edits work. Non-root additions
still reject until slice 4. Update any existing positive addition fixture
expectations; keep permanent invalid/mixed-change cases from slice 1 green.

Sizing basis: validation and initialization are already complete; remaining
work is one publisher branch, post-create projection input, and two existing
controller-harness variations. About five minutes, medium confidence. If the
branch demands another creation policy or transaction redesign, stop and refine
instead of extending this leaf.

### 4. Place an addition inside an existing Portable folder
Type: Behavior
Status: planned
Proof: Publish/show controller observations place `Physics/Inertia.md` in the
existing folder; a missing or unrepresented parent rejects without writes.

Behavior: The accepted tree represents a folder → one valid note addition under
its full path creates the note there without creating any parent.

Reuse the projection's existing full folder-path mapping. Readme-only and
nested folders are data variations of this placement rule; include repeated
folder basenames where needed to distinguish full-path selection. Restrict
resolution to this notebook and folders represented in accepted content.
Remove slice 3's blanket non-root rejection, retaining missing-parent guidance.

Sizing basis: one destination resolver feeding the existing creation branch
and focused placement variations. About five minutes, medium confidence.

### 5. Give copied content a fresh identity
Type: Behavior
Status: planned
Proof: Publication exposes distinct original/copy note IDs; the original keeps
its tracker, question, and conversation associations and the copy inherits none.

Behavior: A learned note remains unchanged while its Markdown is added at a
new path → both notes exist and all existing private data stays on the original.

Use concise `makeMe` associations and existing controller views/repository
observations. No similarity or rename inference. Reuse slice 3's creation path;
if it already meets this case, add only this new identity scenario and any
necessary behavior correction, not another canonical Git round-trip assertion.

Sizing basis: one fixture-driven identity case at the publication boundary.
About five minutes, medium confidence.

### 6. Reject a destination reserved by a deleted note
Type: Behavior
Status: planned
Proof: A proposal at a deleted note's title returns the existing restore/
choose-another-title guidance and leaves the deleted identity and binding intact.

Behavior: The proposed path is absent from the Portable tree but reserved by a
soft-deleted note in that location → reject the addition without restoring,
reusing, or replacing that note.

Exercise the `NoteTitlePlacementRules` already preserved by slices 2–3.
Use the real deleted entity and committed state; do not invent new conflict
messages or a recovery path. Assert this outcome through publication, adapting
the existing rejection helper if its exception type is too narrow.

Sizing basis: one destination-conflict fixture and existing rule. About five
minutes, medium confidence.

### 7. Reject an addition based on an obsolete accepted head
Type: Behavior
Status: planned
Proof: After an accepted remote save advances main, the old addition proposal
rejects and the winning remote note content/head remain unchanged.

Behavior: The owner commits a local addition, but accepted remote history
advances before publication → preserve the remote winner and reject the
obsolete proposal.

Adapt the existing old-parent example in
`NotebookGitProjectionDriftControllerTest` to an addition. Keep the current
expected-head guard; no new concurrent-test harness or rebase. Existing
`NotebookGitPublicationConcurrencyControllerTest` continues to cover its
unchanged locking mechanism.

Sizing basis: one deterministic stale-head example using existing save and
publish boundaries. About five minutes, medium confidence.

### 8. Reject an addition when current web state has drifted
Type: Behavior
Status: planned
Proof: A web structural change without an accepted Git commit causes addition
rejection; the occupied destination/current notes and accepted bundle survive.

Behavior: Accepted main is unchanged but the live tree no longer matches it,
including a destination occupied by an unsynchronized web creation → reject
publication without absorbing or overwriting that drift.

Reuse the deterministic structural-drift setup in
`NotebookGitProjectionDriftControllerTest` and slice 3's pre-create projection
guard. Do not alter locking ownership, repair history, or expand web structural
synchronization. The existing CLI rejection test already observes preservation
of branches, index, files, and local commit; keep that evidence valid rather
than adding a separate client recovery loop.

Sizing basis: one drift fixture and the unchanged projection guard. About five
minutes, medium confidence.

### 9. Accept advisory names with existing health warnings
Type: Behavior
Status: planned
Proof: `index.md`/`log.md` additions succeed with the authored filename and the
existing notebook-health endpoint reports the advisory issue.

Behavior: The owner publishes a profile-valid note with an advisory filename →
the note is accepted and its existing health warning remains discoverable.

Use one parameterized publication-to-health observation and the existing
`OkfIncompatibleTitleHealthRule`. No new warning transport or API; reserved
README names remain rejected by slice 1.

Sizing basis: one advisory policy through existing publication/health surfaces.
About five minutes, medium confidence.

### 10. Retry an accepted addition without duplication
Type: Behavior
Status: planned
Proof: Repeating the proposal with its original expected head returns the same
accepted head and preserves note count, identity, and timestamps.

Behavior: The first addition was accepted but its result was uncertain →
repeating that proposal succeeds without creating another note.

Reuse the already-accepted-head path and existing committed publication-state
snapshot helpers. Do not add retry storage or local identity metadata.

Sizing basis: one idempotency example on an existing branch. About five minutes,
medium confidence.

### 11. Publish a new note through the installed CLI
Type: Behavior
Status: planned
Proof: One installed-CLI E2E commits an addition, reports that commit accepted,
and opens the new note in Donut at its authored path.

Behavior: The owner adds and commits a file in an acquired checkout and runs
`donut notebook publish <directory>` → the note becomes usable in Donut.

Extend `cli_notebook_clone.feature` using its existing installation and owner
setup. Its checkout-edit task uses `git commit -am`; add an explicit addition
task that writes the file, stages it with `git add`, and commits. Keep step/
page-object plumbing thin. The testability resnapshot hook may establish only
the Given baseline, never the result of publication.

Update clone guidance in `nonInteractiveCli.ts` to describe one added or edited
note and supported locations. Assert the relevant guidance in this existing
E2E flow; do not create a separate copy-only test leaf.

Sizing basis: one existing installed-command proof loop, one small task with
ordinary Git staging, and guidance copy. About five minutes, medium confidence;
E2E runtime may dominate the verification exception below.

### 12. Publish a later local edit to the created note
Type: Behavior
Status: planned
Proof: After a real accepted addition, publishing its next content-edit commit
preserves the created note ID and advances accepted history with the exact body.

Behavior: A locally created note is accepted → a later supported local content
edit updates that same note.

Use `NotebookGitMixedEditingControllerTest` with creation through publication
as Given setup and one later local publication as the trigger. No web-save beat,
resnapshot, or repeated creation assertions in this example.

Sizing basis: one changed starting state for the existing content-edit flow.
About five minutes, medium confidence.

### 13. Save a web edit to the created note
Type: Behavior
Status: planned
Proof: A content save through `TextContentController` after accepted local
creation retains the note ID and produces a downloadable child commit with
the saved content.

Behavior: A locally created note has been accepted → a supported web body or
frontmatter save edits that same note and records its accepted history.

Use the existing web-content controller harness and publication as Given setup.
Do not combine another local edit/pull loop into this example. Preserve the
existing mixed-editing sequence as regression evidence; no new structural web
synchronization or snapshot hook.

Sizing basis: one created-note fixture exercising the existing web-save path.
About five minutes, medium confidence.

### 14. Receive the added file into a clean checkout
Type: Behavior
Status: planned
Proof: A `run`-boundary CLI pull test with real Git fast-forwards from the
accepted parent, produces exact added-file bytes, and leaves a clean canonical
working tree without metadata additions.

Behavior: Another eligible clean bound checkout predates the accepted addition
→ `donut notebook pull <directory>` receives its file.

Add an added-path variation in `notebookPull.fastForward.suite.ts`, run through
`notebookPull.test.ts`, using the existing real-Git bundle helpers. Backend
download of the actual accepted addition is owned by slice 3. Do not duplicate
transport calls, readiness, or ancestry assertions already covered by the
unchanged canonical pull case. No auto-commit, stash, or rebase.

Sizing basis: one fixture variation and focused file/tree delta assertions.
About five minutes, medium confidence.

## Contract-to-proof ownership

| Selected promise | Owning slice and observation |
|---|---|
| Fresh root note, empty/populated notebook, exact commit/tree and basename title | 3: publish/show/download round trip |
| Atomic create, creator and derived-state rollback | 3: same publication operation under the existing late-save failure harness; committed reads |
| Authored YAML title/unknown keys, unknown valid type, headings, body and links | 3: canonical exact-content/reference observation |
| Invalid content/title/path/mode, README, multiple/mixed changes reject | 1: controller rejection variations; preserved by 3 |
| Owner-only, clean main, direct-child ancestry, existing edits still work | 1 and 3: existing backend/CLI auth, readiness, ancestry and edit evidence retained |
| Existing web construction behavior survives extraction | 2: existing creation/extraction controller regression suite |
| Existing full-path folder only; nested/readme-only parents; missing-parent rejection | 4: placement/rejection variations |
| Copies have fresh identity and no inherited private data | 5: original/copy association observation |
| Soft-deleted destination preserves deleted identity and gives guidance | 6: publication conflict observation |
| Stale accepted head and competing accepted changes | 7: deterministic rejected proposal preserves winner; existing concurrency evidence |
| Drift/occupied destination without head advancement | 8: rejected proposal preserves current projection and binding |
| Rejections retain the local commit, branches, index and files | 8: reuse existing CLI publication-rejection state observation |
| Advisory index/log filenames accepted with warning | 9: accepted filename and existing health warning |
| Already accepted head returns success without duplicate/timestamp mutation | 10: committed-state comparison |
| Ordinary bound/authenticated CLI publication and accurate guidance | 11: installed CLI add/publish/open flow |
| Subsequent local content edit retains created identity | 12: publication after creation |
| Subsequent web save retains created identity and accepted history | 13: web save after creation |
| Added file downloadable and receivable identically; no Portable metadata | 3: exact Git tree; 14: canonical real-Git pull |
| Interim root rejection removed; temporary non-root rejection replaced | 3 removes slice 1's valid-root rejection; 4 resolves represented parents |

## Verification and wrap-up

- Backend leaves: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend rules require all backend unit tests, not individual classes.
  The named tests above identify owning proof, not a narrower runner exception.
- CLI leaf: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`.
  Reuse existing clone/publication regression evidence; rerun their test files
  only when edits invalidate it.
- Installed CLI: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
- No manual or mutation testing is included. No product tests run during this
  refinement turn. Each execution leaf ships green with its own proof and
  cleanup; never park required new behavior tests in a later leaf.
- Existing proof is reusable when its boundary is unchanged. If later
  inspection shows a scenario is already directly proven, cite it and remove
  redundant work while retaining ownership; do not manufacture code changes.
- Execute via `execute-plan`: Jidoka, fresh post-change-refactor agent, API
  generation if needed, one coordinator-run
  `./scripts/run.sh pnpm format:changed`, plan update, commit and push.
  Refinement alone authorizes none of those implementation/commit actions.
- **Sizing exception:** the required backend-suite or focused installed-CLI
  run may itself exceed five/ten minutes. Record measured test time separately;
  it does not permit extra implementation. Local cleanup and proof authoring
  belong inside the target; external test/hook/CI waits are recorded, not
  disguised as additional feature work.
- Scrutinize a leaf after five minutes. After ten minutes of implementation
  without convergence, preserve attempt-owned work safely and refine this same
  plan before continuing. No execution-time guarantee is implied.

## Readiness

**Ready for execution.** Every remaining leaf is classified Ready after the
replacements above: one rejection, placement, identity, publication, or receive
outcome, or the immediately enabling construction extraction. The first
accepting leaf retains creation rollback proof; validation and construction
preparation no longer compete with its integration loop.

There are no unresolved product decisions, no completed slices to invalidate,
and no execution overrun to recover. Execute from slice 1 when requested.
The broader follow-up commit scope remains outside this plan.

## Refinement learnings

- Root additions now validate the accepted projection before calling
  `NoteFactory`, replace the default document with exact Git-authored content,
  and compare the proposed tree against a live-note list containing the new
  identity; the existing publication transaction rolls all derived rows back.
- Fresh-note identity initialization now lives in `NoteFactory`; web creation
  and extraction retain content preparation and downstream enrichment while
  sharing title placement, default document, timestamp, and creator setup.
- The proposal tree descriptor now distinguishes one added or modified regular
  note, while mixed changes name the paths and ask for separate commits.
- Added-note validation reuses typed-Markdown/content validation, validates a
  `NoteUpdateTitleDTO` explicitly, and rejects `DisplayName` normalization
  mismatches before the interim creation-not-supported boundary.
- `AuthoredNoteContent.prepareDocumentForSave` normalizes stored type; Git
  acceptance already uses exact `AuthoredNoteDocument.fromContent`. Share
  fresh identity creation, not web content preparation.
- Existing note initialization also creates `NoteCreator` and rejects deleted
  titles. Extract that complete concept before publication rather than
  duplicating or omitting its private identity rules.
- The existing transaction-failure harness and deterministic stale/drift
  examples remove the need for new failure or concurrency infrastructure.
