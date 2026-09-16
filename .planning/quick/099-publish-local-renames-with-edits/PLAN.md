# Publish local renames and edits with note continuity

Status: planned
Source: [SEED-009 story 36](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-36)
Authority: refinement and planning requested 2026-09-16; no execution requested.

## Outcome and boundaries

Owners publish ordinary local note rename/move plus edits, including accumulated
linear commits, retaining original note/learning identity and final authored
content without rewriting commits. Use JGit RenameDetector configured at 50,
compose correspondence through history, and apply one final projection.

The source seed owns full scope and examples. Existing destination folders are
the new relocation commitment; retain existing creation and companion behavior.
Preserve exact-pair ambiguity guards, unresolved mixed removal/addition refusal,
copy and deletion-gap semantics, authorization, validation, atomicity, references,
and learning state. No new folder/subtree, cross-notebook, trash, divergence,
performance, metadata, confirmation UI, semantic reset, or link-repair capability.
Do not reject supported cases merely because they exceed example counts.

## Architecture and PFE findings

Follow [one final publication result](../../NORTH-STAR.md#one-final-publication-result)
and [Git rename correspondence](../../NORTH-STAR.md#git-rename-correspondence).
ADR 0004 preserves filenames/Portable authored content; ADR 0005 preserves
identity-based web URLs. ADR 0002 is Proposed, not an additional binding policy.

Product-wide search found ordinary-note inference in
`NotebookGitProposalNoteCorrespondence`: exact detector, adjacent-step origin
carry, and endpoint reconciliation. Change this owner rather than add another
identity service. Its callers include main publication through TreeShape and
residual note changes alongside an already identified exact folder relocation
through `requireAdmittedResidualShape`; both need the same ordinary-note
pairwise detection responsibility. FolderShape's whole-folder correspondence has
a different domain purpose and retains its supported semantics.

Reuse the existing JGit dependency (backend/build.gradle) for scoring/matching.
Keep Donut admission safeguards outside scoring, in the same correspondence
owner. Do not write another similarity matcher or treat native Git comparison as
a runtime dependency. Preserve existing path/mode/Portable validation.

Reuse OrdinaryNoteApplication.applyRename for final placement and changed
content; it already persists the same note and uses authored content persistence.
Acceptance compares the resulting Portable tree before changing the head.
CLI clone/publish transmits Git bundles and need not implement rename inference.
Existing web changes have explicit identity and use their accepted-change owner.
Reference resolution remains in the existing authored-reference domain.

Evidence is source inspection only, not executed proof. Key existing tests:
ComposedMoveEdit, DeletionRejection, DeletionThenRecreation,
ProposalRename, ProposalRelocationReferrer, ProposalFolderRelocationPrivateAssociation,
and ComposedRangePublicationAtomic controller tests. Prefix each with NotebookGit
and suffix ControllerTest where applicable; inspect actual files before reuse.
Existing installed-CLI journey: cli_notebook_web_local_reconciliation.feature.

## Verification and delivery contract

Backend command (all backend tests, per stack rule):
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Outside-in CLI command (wrapper owns its stack):
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_local_reconciliation.feature`

Controller boundary is publishNotebookGitProposal, with actual bundles/history,
real persistence, and makeMe data. Never mock rename detection. Characterize
required preserved behavior before replacing it; inspect assertions and retain
only sufficient existing proof. A test asserting the old exact-only rejection
must change with the new policy; replace it with a genuinely below-threshold
case rather than delete preservation coverage. Use varied substantial bodies
so shared YAML alone cannot accidentally dominate the intended score.

Each slice owns a single proof loop. Target approximately 5 minutes including
local cleanup; scrutinize >5, stop and refine >10 unless the only excess is the
required all-backend suite or E2E external wait. Record actual test/runtime
exceptions, not a blanket allowance for longer implementation. Failing library
assumptions stop the affected path and change this same plan before broadening.

Execution uses dough-execute-plan: take the backlog item only then; Jidoka,
fresh post-change-refactor agent, API generation only if needed, coordinator
runs ./scripts/run.sh pnpm format:changed once, updates this plan, commits through
the check-only hook, pushes, and observes CI asynchronously. Implementers do not
run format:changed or lint:changed. Retain plan/evidence for retrospective and
story wrap-up. No API change or migration is expected.

## Ordered slices

### 1. One library-backed exact correspondence path
Type: Structure
Status: done

Replace hand-written exact matching mechanics with JGit-backed detection at 100
initially, preserving existing ambiguity and unresolved-mixture safeguards.
Adapt both ordinary and folder-residual callers; centralize detector policy.
The residual TreeShape entry currently receives documents only; pass the existing
proposal repository/context from Publisher so it can use the same detector.
Keep residual candidates limited to the already computed outside-subtree set,
so consumed folder descendants cannot become competing note candidates.
Keep history composition and final application unchanged. This immediately
enables slice 2's 50% policy without a parallel exact/fuzzy recognizer.

Proof: existing exact rename, composed move/edit, ambiguous exact-pair,
folder-residual/private-association and deletion-gap controller assertions stay
green with the backend command. If library tie behavior differs, preserve the
existing guard explicitly; do not infer broader authority from its pairing.
Done 2026-09-16: JGit `RenameDetector` at 100 is behaviorally equivalent to the
prior blob-`ObjectId` pairing (content-addressed storage ⇒ identical content ⇒
score 100). Detector extracted to `NotebookGitProposalRenameDetector` owning
`RENAME_SCORE_THRESHOLD`, `detectRenames`, the ambiguous-blob safeguard, and the
`NoteChangeDiffEntry` JGit adapter (protected-constructor subclass; `DiffEntry`
factories are package-private). Package-level API unchanged; Publisher passes
`proposal.repository()` to `requireAdmittedResidualShape`. Full backend suite
green (2434 tests). Slice 2 lowers only the threshold constant; the exact-blob
ambiguity safeguard may need revisiting when content-similarity pairing is enabled.
Sizing: ~5 minutes active work plus required suite runtime; medium confidence.

### 2. Publish a rename or move with edits in the same commit
Type: Behavior
Status: done

Given a learned ordinary note and existing destination folder, when a single
commit changes its filename/location and authored text with JGit score >=50,
publication retains that note and its learning associations at the final path.
Set the shared detector to 50. Carry its result through existing ordinary note
application; update the obsolete changed-content rejection fixture in the same
slice. Use same-parent and existing-folder data variants of this one behavior,
not new handlers. Include a score in [50,60) to distinguish the configured policy
from JGit's default. Library fixtures must be established against the actual
project dependency; no hand-written scoring oracle.

Proof: extend ComposedMoveEdit/ProposalRename controller coverage at publication,
asserting original note ID and canonical learning associations, final authored
text/path and accepted submitted commit. Reuse unaffected association assertions;
backend command. Below-threshold mixed changes remain refused from this slice.
Sizing: ~5 minutes active work plus suite; medium confidence.
Done 2026-09-16: lowered `RENAME_SCORE_THRESHOLD` 100 → 50 (only production change).
`applyRename` already persisted the same note with authored content — unchanged.
Added same-parent (score ~59, in [50,60)) and existing-folder move-with-edit
(score ~85) acceptance tests asserting note ID, final content/path, and learning
associations. Updated the obsolete changed-content rejection fixture to a
genuinely below-threshold case (substantial unrelated bodies, score ~4). The
exact-blob ambiguity safeguard is threshold-independent (keys on `ObjectId`
before scoring) and needed NO adjustment. Shared the fox/quantum below-threshold
fixture bodies into `NotebookGitBundleControllerTestBase` during refactor. Full
backend suite green (2436 tests). Learning: short frontmatter-dominated bodies
score deceptively high (e.g. "original content" vs "new" scored 54); substantial
multi-line bodies where the body dominates the score are required for below-
threshold fixtures. Pre-existing `NotebookGitProposalRenameControllerTest` file
size (466 lines) flagged for retrospective, not split here.

### 3. Carry detected identity through accumulated edits
Type: Behavior
Status: done

Given accepted A, B renames/moves and edits a note, and C further edits its new
path so A-to-C similarity is below 50, one publication preserves the original
identity at C. Compose adjacent results with simultaneous source/destination
mapping; do not let endpoint matches override the carried origin. Retain known
deletion gaps and final-only projection. Generalize the existing composition,
not per-history pattern recognizers.

Proof: controller range fixture observes original identity, final C content,
original commit chain/bundle, and unchanged learning schedule. Reuse inspected
range atomicity/deletion-recreation regressions through the backend command.
Intermediate history is evidence, not mandatory current Portable validation.
Sizing: ~5 minutes active work plus suite; medium confidence. If endpoint and
range precedence demands another model, stop and reassess rather than patch cases.
Done 2026-09-16: NO production change required — the existing composition from
slices 1–2 already carries identity through accumulated edits when the endpoint
is below 50. `carryExactMoveOrigins` walks the first-parent range, re-keying the
original origin (A) to each adjacent detected destination (B, then C); the tip
detector leaves C as ADDED when A→C is below 50, and `resolveMoveCorrespondence`
pairs C's addition with the carried A origin. Added two tests in a new
`NotebookGitComposedAccumulatedRenameControllerTest` (split from
`NotebookGitComposedMoveEditControllerTest` during refactor to respect the
250-line gate): a range fixture proving original identity, final C content,
commit chain, and learning schedule, plus a single-shot A→C refusal proving the
endpoint is genuinely below 50. Full backend suite green (2438 tests). No
endpoint/range precedence model conflict arose.

### 4. Explain unresolved correspondence without partial publication
Type: Behavior
Status: done

Given a genuinely below-threshold unmatched removal/addition or existing
ambiguous identical-file pairing, publication fails with affected paths and
leaves accepted state intact. Improve existing refusal diagnostics only as
needed; retain a single failure outcome. Detector limits/incomplete detection
must not be taken as permission for destructive replacement. Do not add a
confirmation UI or speculative similarity confidence policy.

Proof: extend DeletionRejection controller scenarios with affected-path output
and preserved head/note/learning state through committed rollback observations.
Reuse existing copy and deletion-gap tests to distinguish intentional supported
operations. Backend command; avoid repeated unrelated preservation assertions.
Sizing: ~5 minutes active work plus suite; medium confidence.
Done 2026-09-16: `refuseUncertainIdentityCorrespondence` now takes affected paths
and appends `; affected paths: <sorted, joined>` to the unchanged message prefix;
the single `unsupportedTreeShape` failure outcome and atomic rollback are retained.
`refuseResidualRemovalAndAdditionMixture` and `refuseAmbiguousBlobCorrespondence`
thread their affected paths; the both-present guard is preserved (deletion-only
and addition-only stay admitted). Extended `NotebookGitDeletionRejectionControllerTest`
with per-case expected affected paths and a new committed-rollback learning-state
test. Refactor extracted the shared refusal into `NotebookGitProposalIdentityRefusal`
(named for its responsibility) and split reserved-file rejection tests into
`NotebookGitReservedFileRejectionControllerTest` to respect the 250-line gate.
Full backend suite green (2439 tests). No confirmation UI or confidence policy added.

### 5. Preserve references through inferred rename and relocation
Type: Behavior
Status: planned

Given references to a note, a changed-content inferred move uses existing
publication reference semantics. Unchanged old-path text is preserved and
resolves as before; locally updated references resolve to the moved note.
Reuse authored persistence/resolution and the existing application path, with
no implicit Git-tree rewrite and no new web behavior.

Proof: extend ProposalRelocationReferrer/ProposalRename controller fixtures with
inferred move-and-edit data, observe authored content and existing resolution
signals. Observe the same note ID via the current note controller boundary.
Backend command. Existing web reference behavior remains regression coverage;
no new web implementation is prescribed.
Sizing: ~5 minutes active work plus suite; medium confidence.

### 6. Publish the accumulated session through the installed CLI
Type: Behavior
Status: planned

Given two clean clones, the owner commits a note rename/move and same-note edit,
then a further edit, publishes once, and pulls the second clone. The same Donut
note shows the final content; the receiving clone has the submitted paths,
bytes, and original commit chain. Extend the existing CLI feature and its
purpose-named checkout helpers, keeping intermediate actions explicit.
Inspection confirms commitCliNotebookCheckoutNoteRenameAndEdit stages a rename
then writes the supplied path before one commit; reuse it with relativePath equal
to toRelativePath. Add accurate same-note Gherkin wording instead of calling the
existing "unrelated edit" step. Reuse the subsequent edit and original-note-route
steps; no new checkout task or generic scenario dispatcher is needed.

Proof: installed-CLI feature via the wrapper command above, actual local commits,
publication and pull; reopen the captured original note route to show identity
continuity. Backend canonical proof owns detailed learning associations. Do not
substitute a mocked SDK response or testability-supplied final state.
Sizing: ~5 minutes active work plus E2E runtime; medium confidence after
inspection of the existing staging/commit and original-route helpers. Keep the
end-to-end outcome together; no separate helper-only delivery slice.

## Promise ownership

| Promise | Owning slice / observation |
| --- | --- |
| One cohesive library detector; existing exact/residual behavior | 1, controller regression |
| 50% changed-content rename/move; identity and learning continuity | 2, real publication and persistence |
| History evidence, low endpoint similarity, original commits, final-only result | 3, range fixture plus existing atomicity proof |
| Non-destructive unresolved outcome and affected paths | 4, committed rollback and error |
| Existing copy/deletion-gap semantics | 1 and 4, inspected existing controller tests |
| References and stable note URLs | 5, authored content/resolution and note controller |
| Actual owner publish/pull workflow | 6, installed CLI and original note route |
| Authorization, validation, companions, existing folder support | 1–5 backend regression suite; inspect relevant assertions before credit |

## Current assessment

No blocking product decision remains for this cut. The main risks are JGit
candidate behavior versus preserved safeguards, threshold-sensitive test data,
and the residual-folder caller. Exact native-Git parity is not promised.
No production code changed and no behavioral checks ran during planning.


## Slice refinement assessment — 2026-09-16

Applied dough-slice-plan-refinement after construction. Reassessment preserves
the selected story and PFE choice. No slices replaced or split; six remain.
Slices 1 and 6 were clarified in place: residual detection needs repository
context without reintroducing consumed subtree candidates, and the installed
CLI already has the required same-commit staging operation and identity-route
observation. These remove the identified hidden preparation assumptions.

Each slice is Ready under the planning assessment: one structure gate enabling
the next behavior, or one observable behavior with one proof loop. Later examples
exercise the same detector → range correspondence → final application model;
none requires an additional recognizer. Backend suite and E2E runtime are the
only stated sizing exceptions; actual overruns must still be recorded. No story
resplit recommended. Ready for direct execution when separately authorized;
this assessment supplies no execution authority and claims no passing tests.
