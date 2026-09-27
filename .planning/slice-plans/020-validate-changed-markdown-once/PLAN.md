# Local publish checks the Markdown it changes once, before applying anything

## Source

- Story: [SEED-050#story-1](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-1)
- **Identity:** SEED-050#story-1
- Found by the closing review of the local AI notebook effort; re-evaluated
  against `d9abdcc2eb` and cut on 2026-09-27 (see the seed).
- Direction: [North Star — One format boundary](../../NORTH-STAR.md#one-format-boundary)
  and [One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary).

## Goal and scope

A notebook owner publishing from a local checkout gets the same Markdown
refusal at the same step whether or not the proposal moves a folder, and the
check costs in proportion to what changed. Validation and the drift guard live
once each in `NotebookGitProposalPublisher`.

Included: one strict typed-Markdown check of added and changed `.md` files
right after the path refusals; one drift check before any change; the apply
steps stop validating; the relocation's second mode goes away.

Excluded: a final-state rewrite of note/folder publication (dropped); moving
authored-property or Readme-type checks; the double YAML parse inside the
format check; refusal wording.

Starts after SEED-009#story-48 (plan 019) lands: its slice 1 rewrites the
`.md` test in `NotebookGitProposalMarkdownFormat` and its classifier decides
what "a Markdown path" is here. It has landed; the starting facts below are re-checked.

## Starting facts (checked 2026-09-27 at `d9abdcc2eb`; re-checked after story-48 at `d907a9e6dd`)

Paths under `backend/src/main/java/com/odde/donut/services/notebookGit/`.

- Format check (`NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(Repository, ObjectId)`)
  walks every `.md` in the proposed tree; called from `Publisher:173`,
  `DocumentApplication:43` and `FolderRelocation:89`. An ordinary publish with
  documents walks twice, a relocation publish with before and after documents
  three times, an attachment-only publish never.
- Drift check (`projection.requireMatchingAcceptedTree`) is called at
  `Publisher:145`, `:169`, `:175` and `FolderRelocation:91-93`, the last behind
  a flag (`applyAfterMatchedAcceptedTree`, `FolderRelocation:40-65`); each
  path runs exactly one before changing anything.
- `inspectRegularFiles` (`Publisher:106-108`) gives every regular file on
  either side with `acceptedBlobId` and `proposedBlobId`; unchanged means equal
  ids.
- `NotebookGitProposalMarkdownFormatControllerTest` has 7 cases, all on the
  ordinary path; no test covers invalid Markdown on the relocation path.
- `NotebookGitProjectionDriftControllerTest` covers drift refusals.
- Many refusal tests seed accepted Git with `seedAcceptedBinding`, whose
  entries have no matching rows: their projection has already drifted, and
  the refusal they assert fires today only because it comes before the drift
  check.
- Plan 019 landed: `inspectRegularFiles` (`Publisher:96-98`) runs before size
  admission, followed by its path refusals (`refuseMiscasedMarkdown`,
  `refuseLeftoverFolderMarkers`); `PortablePathKind.of(path) == MARKDOWN` is the
  Markdown classifier.

## Outside-in proof

Controller-level tests through the publish endpoint.

| Seed example | Slice |
| --- | --- |
| 1. relocation + untyped new note under the destination → "Invalid Markdown" naming it, nothing changes | 1 (new guard case) |
| 2. only `Notes.md` changed → only that file is judged; an untouched accepted note without frontmatter no longer blocks | 1 (new fail-first case + existing duplicate-key case) |
| 4. file-only proposal → still publishes | 1 (existing attachment publish cases) |
| 3. drifted projection + relocation or note add → "refresh the checkout", nothing changes | 3 (existing drift cases) |

## Slices

### 1. Markdown the proposal adds or changes is validated once, before any branching

Type: Behavior
Status: done
Proof: first add the guard case for example 1 to
`NotebookGitProposalMarkdownFormatControllerTest` (relocate a folder, add an
untyped note under the destination → `BAD_REQUEST` "Invalid Markdown" naming
it, binding unchanged); it passes before and after. Then a fail-first case:
a `makeMe` note `Legacy` whose content has no frontmatter (accepted Git admits
it through `PortableTreeEntry.ofNote`) plus one typed note; the proposal edits
only the typed note → today refused naming `Legacy.md`, afterwards it
publishes. Then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitProposalMarkdownFormatControllerTest'`
and `--tests 'com.odde.donut.controllers.NotebookGit*'` are green.

Behavior: a publish judges only Markdown whose proposed blob is not already
in accepted history; an untouched accepted note is not judged again, a moved
unchanged note is not re-parsed.

Change: `assertValidTypedMarkdown(Repository, List<InspectedRegularFile>)`
checks each Markdown path (story-48's classifier) whose proposed blob id is
present and not among the accepted blob ids, by opening that blob; its
TreeWalk goes. Call it once in `publish` right after plan 019's path
refusals; delete the calls in `DocumentApplication:42`,
`FolderRelocation:89` and `Publisher:173`.

Accepted proof: `NotebookGitProposalMarkdownFormatControllerTest` 9 pass
(new `rejectsAnUntypedNoteAddedUnderARelocatedFolderWithoutMutatingTheAcceptedBinding`
and fail-first `publishesAnEditBesideAnUntouchedAcceptedNoteWithoutFrontmatter`,
which was refused naming `Legacy.md` before the change); `NotebookGit*` 442 pass,
2 skipped (pre-existing).

### 2. Refusal tests start from a projection that matches accepted main

Type: Structure
Status: done
Proof: the re-seeded tests stay green with the same refusals —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitProposalMarkdownFormatControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposalFolderRelocationShapeControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposalFolderRelocationPlacementControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposalRelocationDestinationControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposalFolderRelocationEmptyDescendantControllerTest'`.

Change: tests that seed accepted Git with `seedAcceptedBinding`
(`NotebookGitAcceptedObjectStoreTestSupport:30`) and no matching rows start
instead from `makeMe` rows plus a snapshot of the current Portable tree, so
their projection matches accepted main. Enables slice 3, which checks drift
before these refusals.

Accepted proof: the five named classes plus
`NotebookGitProposalFolderRelocationDestinationControllerTest` (the class that
actually used `seedAcceptedBinding`) green, 25 tests before and after; the
refactor moved shared `README_BODY`/`README`/`NOTE` into
`NotebookGitControllerTestBase` (12 relocation classes, 38 tests green).
Not re-seeded because their refusal needs drift (a matching snapshot writes
`.keep` for every empty folder): Placement
`rejectsAnExactFolderRelocationOntoAnInvisibleEmptySameNameContainer`,
FolderRelocationDestination
`rejectsAnExactFolderRelocationIntoAnExistingUnrepresentedParent`,
EmptyDescendant `rejectsAnExactFolderRelocationWhenASourceDescendantHasNoTrackedContent`
— slice 3 input. `seedAcceptedBinding` also stays in
`NotebookGitProposalTreeShapeControllerTest` and
`NotebookGitProposalRenameRejectionControllerTest` (outside this slice).

### 3. The live projection is checked against accepted main once, before any change

Type: Behavior (drift is now reported before shape and placement refusals)
Status: planned
Proof: existing drift and relocation tests are green —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitProjectionDriftControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposal*Relocation*' --tests 'com.odde.donut.controllers.NotebookGitProposalFolderPublicationSafetyControllerTest'`,
then `--tests 'com.odde.donut.controllers.NotebookGit*'`.

Behavior: a drifted projection → any publish that would change content →
"refresh the checkout" before any other refusal; nothing changes.

Change: one `requireMatchingAcceptedTree` call in `publish` before any
change; delete the calls at `Publisher:145-146`, `:169-170`, `:175-176`;
flatten the before-relocation `if/else`; delete
`applyAfterMatchedAcceptedTree`, its flag and the drift call in
`FolderRelocation`. The no-change publish's drift check in
`NotebookGitProposalAcceptance:130` stays. Then check which relocation
refusals can fire only under drift
(`NotebookGitProjection.requireNoUnrepresentedEmptySourceDescendants` `:139-156`,
`requireRepresentedRelocationSource` and the unrepresented-parent refusal);
delete each that became unreachable together with its test (for example
`NotebookGitProposalFolderRelocationEmptyDescendantControllerTest`).

## Current decisions

- Changed Markdown only: a proposed blob already in accepted history is not
  judged again (SEED-009#story-48); pure moves are not re-parsed.
- Authored properties and the Readme type stay checked where the blob is read.
- Refusal messages unchanged; no new classes.

## Learnings

- Slice 1: the format check now runs before shape and placement refusals, so a
  fixture whose changed `.md` has no frontmatter now gets "Invalid Markdown"
  first (`NotebookGitProposalFolderRelocationShapeControllerTest` "edited" case
  was made typed). Slices 2-3 should expect the same for changed untyped
  fixture notes; unchanged untyped fixture notes are no longer judged.
