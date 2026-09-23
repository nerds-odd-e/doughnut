# Publish supporting files and acquire a fresh usable checkout

## Source and authority

- Identity: SEED-035#story-13.
- Source: [first replacement story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-13).
- Owner authorized resplitting/refinement, then keeping and publishing the
  planning result to main. Implementation and application release remain separate.
- Reused session-created workspace:
  `/Users/terryyin/.codex/worktrees/attachment-lfs-refinement/doughnut`,
  branch `codex/attachment-lfs-refinement`, base
  `64ca49163a20154f973f1ad99d16e222966f3c73`.
- Originating/integration checkout: `/Users/terryyin/git/doughnut`.
  Eventual preparation publication target: `origin/main`.
- This plan replaces the original 18-leaf plan in place. Nothing was implemented;
  there is no completed proof or execution history to transfer.

## Execution

- Mode: Story Branch. Replanning is allowed; there was no `--no-replan`.
- Originating and integration checkout: `/Users/terryyin/git/doughnut` on `main`.
- Owned workspace, created this session:
  `/Users/terryyin/git/worktrees/notebook-lfs-continuity`,
  branch `story/notebook-lfs-continuity`, starting revision
  `e0fcf33229fc7226031d54ddf399c3d7385478f4`.
- Increment target: `refs/heads/story/notebook-lfs-continuity`.
- Published claim: `e84967c03a86375eccb66a173f50db032fffc819` on
  `refs/heads/main`. Claim CI is unobserved: the story-branch observer does not
  cover trunk, and `ci.yml` ignores `.planning/**`.
- Published increment: `fce754e518c12751539f33e51d5e92a5a266353e` on
  `refs/heads/story/notebook-lfs-continuity`. Registered with the story-branch
  observer. Parent on that branch: `c51f7cbcd0840a286bb6c12fae02e71b47934804`.
- Default-checkout refresh is deferred (`unclear-ownership`). That checkout
  stayed at `e0fcf33229`, clean, one commit behind `origin/main`.
- Preparation: `./scripts/run.sh bash scripts/worktree_setup.sh` succeeded, then
  `CURSOR_DEV=true nix develop -c node -e "console.log('worktree-command-ready')"`
  printed `worktree-command-ready`.
- Automated tests do not call Google Cloud Storage. Slice 2 continues with the
  Book storage test seam.
- CI observer: `/tmp/dough-ci-501/watch-p2I1y8`, GitHub Actions, workflow
  `ci.yml` / `donut CI`, target `story/notebook-lfs-continuity` on
  `nerds-odd-e/doughnut`. Armed after the claim; the claim itself stays
  unobserved.
- [Second replacement plan](../020-notebook-lfs-receive/PLAN.md) owns receiving
  into an existing checkout, equal-head hydration retry, and supported rebase.

## Outcome and stopping point

An owner uses Donut CLI to clone a new notebook, commit and publish supporting
files, and clone a fresh usable checkout elsewhere. Files survive ordinary web
edits and later publication, current acquisition does not fetch old payloads,
and all published versions remain explicitly retrievable. This is an automated
user journey, not a manual API/LFS workflow or an infrastructure-only story.

The first delivery supports publication from a checkout still based on the
accepted head, as today's publish command already requires. After remote/web
changes, a fresh clone is the supported receive path until story 15. Preserve
the old checkout and any unpublished work. LFS pull has an explicit temporary
pre-mutation refusal, removed by story 15; legacy pull is unchanged. No custom
manual merge procedure or new conflict-resolution capability is promised.

Include the 10 MiB inclusive policy, exact bytes, root/nested/empty files,
ordinary Markdown, metadata preservation, authorization, actionable failures,
current-only hydration, corrective oversized intermediate commits, and history
retention. Exclude existing-notebook conversion, new file UI, Books, quotas,
garbage collection, direct Git hosting, and historical shrinking.

## Critical review: complexity removed

The original plan mixed two user journeys and presented several observations as
independent capabilities. Retry belongs with clone; byte/traffic measurements
belong with the publish-to-clone loop; format regression belongs with the format
owner. Object selection and upload sequencing belong to one publication step.
No standalone benchmarking, compatibility-audit, or object-selection slice remains.

Inspecting all production `NotebookAttachment` consumers found tree projection,
encoding, path tracking and folder occupancy, not a generic file-download API.
Its existing binary column can project the accepted Git blob representation:
legacy payload bytes or LFS pointer bytes. Give the Java/row accessor an explicit
Git-content meaning while retaining the existing physical column. Do not store
hydrated payloads in it for LFS notebooks. Avoid separate digest/size columns
that duplicate the pointer; future browsing resolves content through the shared
attachment-content owner, never blindly returns this column.

An explicit binding representation flag remains justified: submitted filenames,
pointer-looking legacy bytes, first download, or object-store presence must not
silently choose conversion. This requires one compatible schema addition, not an
attachment data migration. The existing two-release schema rule still applies.

## Existing owners and architectural constraints

- ADRs 0002, 0004, 0006 and 0007; [North Star](../../NORTH-STAR.md).
  No architecture reversal, new sync protocol, cache, or exemption registry.
- `NotebookGitProposalPublisher` retains locked-head authorization, ancestry,
  atomic acceptance and final projection. Extend story 12's
  `NotebookGitAttachmentSizeAdmission`; raw blob size and verified LFS payload
  size are representations of the same policy.
- Taken work was rechecked: its plan records all three slices done and published
  through `4e04fa3eb54ed8a39b8935c7c8bcb805b6018804` on
  `story/attachment-size-admission`. Its completed work and cleanup are now
  integrated through trunk commit `d1dc83a698`; CI completion is not asserted here.
- `NotebookGitProposalAcceptance` projects Git content.
  `NotebookGitTreeEncoder` owns derived/full trees. Preserve accepted metadata
  explicitly in full assembly/drift checks and reset, as well as derived web
  trees; do not invent a metadata attachment row or regenerate author metadata
  from defaults on each save. New creation generates initial attributes.
- `NotebookService.createNotebookForOwnership` already initializes the Git
  binding. Select LFS at creation after activation, never at first acquisition.
  Older notebooks remain raw, even when never acquired before rollout.
- `notebookAcquisition` owns staged clone and retry cleanup;
  `notebookPublishSubmission` owns bundle submission; publish ancestry checking
  is independent of `receiveAcceptedNotebookHead`. That verified separation
  makes the story cut viable.
- Book PDF storage and notebook attachment content are different policies.
  Books put, get, and delete format-specific files, and non-production uses
  `DbBookStorage`. Notebook content is immutable, digest-addressed, and
  notebook-scoped; removing a current file does not delete retained bytes, and
  those bytes never go through `BookStorage` or MySQL.
- They share the Google `Storage` client. Production keeps the single
  `StorageOptions.getDefaultInstance().getService()` construction in
  `BookStorageConfiguration`. Adapter tests inject a mocked `Storage`, the same
  seam as `GcsBookStorageTest`. No second client factory, mock style, or remote
  bucket. Test and end-to-end runs use an in-memory notebook content store so
  Git LFS talks to Donut on localhost.
- Use notebook-scoped digest keys, standard Batch/Basic API, and existing
  authorization at every transfer route.
- Retain all published and in-limit intermediate bytes. Only new oversized
  intermediate-only LFS payloads may be absent after a corrective commit.
  Missing required objects reject; omitted history reports unavailable.
  Raw-Git admission remains strict. Align the durable LFS contract with the
  owner's recorded exception when implementing it.

## Commands, proof boundaries, and sizing

All proof below is planned, not run:

- B: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (whole backend).
- DB: `CURSOR_DEV=true nix develop -c pnpm backend:verify`.
- C: `CURSOR_DEV=true nix develop -c pnpm cli:test`.
- E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
  (new focused feature, not an existing passing command).
- R: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature`.

Unit proof drives controllers or CLI `run` and mocks `Storage` the way
`GcsBookStorageTest` does. The protocol journey uses the installed CLI, standard
Git LFS, and the local backend over its in-memory content store. No automated
test contacts Google Cloud Storage.
Existing acquisition tests that mock Git cannot prove hydration. Reuse
publication rollback support and tree/save-cost oracles; do not duplicate all
shared rollback assertions for every rejection.

Effort hypothesis: L (2–4 hours), low-to-medium confidence after removing the
existing-checkout journey and redundant schema/standalone proofs. This is not a
guarantee that ten slices fit that band. Each target is about five minutes;
8–10-minute cohesive loops are scrutinized below. Required full-suite runtime
and schema release waiting are explicit exceptions, never edit-time exceptions.
If an edit exceeds ten minutes or repeated overruns invalidate the story band,
stop and refine/resplit with evidence instead of hiding work in another title.

## Refined slices

### 1. Preserve legacy bindings while adding representation selection
Type: Structure
Status: done

`notebook_git_binding.attachment_representation` is `NOT NULL DEFAULT 'RAW'`,
mapped as `NotebookGitAttachmentRepresentation` (`RAW` / `LFS`). Existing and
new rows stay `RAW`. Nothing writes `LFS` yet. No payload migration and no
digest or size columns. This enables the following content and admission work.
The separate schema release is still required before deploying code that
depends on the column. Commit and push are not that confirmation.

Accepted proof: populated raw notebooks remain readable and publishable, and
the binding stays `RAW`.
- Promise: existing populated raw notebooks remain readable/publishable and no
  binding converts.
- Boundary: Flyway on `notebook_git_binding`; Notebook Git proposal publish and
  web note save.
- Setup: `publishedRootFilesAreTheExactAcceptedTipAndSurviveTheNextWebNoteSave`
  starts from `createGitBackedNotebook()` and does not set the representation.
- Observations: tip entries after publish and after the web save stay the exact
  attachment bytes; `reloadCommittedBinding(...).getAttachmentRepresentation()`
  is `RAW`. First-parent ancestry is `AcceptedTip.ancestry().getFirst()`.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- Result: pass
- ERD: regenerated from
  `doughnut_wt_8a981b514c284189bb0b2d030193d322_test`. `docs/database-erd.md`
  was unchanged because the exporter lists key columns only.

### 2. Stage verified immutable content for standard transfers
Type: Structure
Status: done

`NotebookAttachmentContent` streams, counts, and SHA-256-checks bytes, then
stores them under `notebook/{id}/lfs/{sha256}`. Production uses
`GcsNotebookAttachmentContent` with the existing prod `Storage` bean. Test and
other non-production profiles use `InMemoryNotebookAttachmentContent`. A
mismatch or failed upload leaves nothing verified. A second store of the same
digest does not overwrite verified bytes. Book storage is unchanged. The prod
bucket name `doughnut-notebook-lfs-carbon-syntax-298809` is configuration only;
no bucket was created and no test contacts Google.

Accepted proof:
- Promise: exact bytes round-trip; wrong size or digest stores nothing; a
  failed upload stores nothing; same-digest retry keeps the original bytes.
- Boundary: `NotebookAttachmentContent` GCS adapter and in-memory store.
- Setup: mocked `Storage` or an empty in-memory store. The product verifies
  the stream.
- Observations: `NotebookAttachmentContentTest.GcsStore` and `.InMemoryStore`.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass (2615 tests). After the test class move, the focused command
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.donut.services.notebookAttachment.NotebookAttachmentContentTest`
  passed. Production store and get were unchanged.

### 3. Transfer exact content with the standard authenticated client
Type: Behavior
Status: done

Permitted owners upload and download verified bytes through standard Git LFS
Batch and Basic transfer at `/api/notebooks/{id}/lfs`. Readers can download.
Missing access is forbidden. A missing object and the same digest on another
notebook are standard object-not-found failures. Upload does not change the
accepted Git head. Non-production uses the in-memory content store. Git LFS
`3.7.1` authenticates with the existing bearer token via `http.extraHeader`.
New notebooks stay raw.

Accepted proof:
- Promise: exact upload/download, reader download, forbidden access, missing
  object, cross-notebook hash isolation, no head change, and a real Git LFS
  client round trip plus unauthorized refusal.
- Boundary: MockMvc LFS controller and installed `git lfs` against the local
  backend.
- Setup: owned notebook and payload digest for the controller tests. E2E logs
  in, creates the notebook and token; the denial scenario re-logs as
  `another_old_learner`. The payload is not pre-stored for the owner upload.
- Observations: `NotebookLfsTransferControllerTest` nested upload, reader,
  forbidden, missing-object, and cross-notebook cases.
  `cli_notebook_lfs.feature` compares downloaded bytes and SHA-256 in
  `notebookLfsStandardClientRoundTrip`, and the denial Then checks non-zero
  status plus denial-shaped output.
- Commands: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (pass,
  2620 tests, 2 skipped). After the denial assertion moved into the Then step,
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
  passed (2 scenarios). Backend proof was unchanged.

### 4. Represent attachments and metadata through the existing tree owner
Type: Structure
Status: done

`NotebookAttachment.content` is accepted Git content (`getAcceptedGitContent`):
legacy payload bytes or LFS pointer bytes, never a hydrated payload. No digest
or size columns. `NotebookGitLfsPointer` classifies a canonical v1 pointer and
treats an empty file as not a pointer. `.gitattributes` is reserved metadata.
Full assembly and history reset keep accepted attribute bytes. New notebooks
stay `RAW` with no attributes. `NotebookGitAttributes.initialMetadata()` exists
for later creation-time LFS and is not applied yet.

Accepted proof:
- Promise: populated raw trees stay readable; pointer-looking legacy bytes stay
  legacy bytes; authored `.gitattributes` survive web save and history reset;
  creation does not activate LFS.
- Boundary: Notebook Git proposal publish, web note save, and
  `NotebookGitCutoverService.resetHistory`.
- Setup: `createGitBackedNotebook()` does not set representation. The LFS
  fixture sets the binding mode and pointer bytes and does not publish.
- Observations: `NotebookGitAttachmentMetadataControllerTest` (attachment row
  is only `diagram.png`; authored attributes remain after save and
  `snapshotCurrentPortableTree`; new notebooks are `RAW` with an empty tip;
  legacy pointer bytes round-trip). `NotebookGitLfsPointerTest` classifies a
  canonical pointer and an empty file. `NotebookGitTreeEncoderTest` keeps
  supplied initial attributes and exact attachment bytes.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass (2606 tests, 2 skipped). After metadata selection moved to
  `NotebookGitAttributes.selectFrom`, the four slice tests were rerun with
  `backend/gradlew -p backend test --tests` for `NotebookGitTreeEncoderTest`,
  `NotebookGitAttachmentClassificationTest`, `NotebookGitLfsPointerTest`, and
  `NotebookGitAttachmentMetadataControllerTest`; result pass. Note
  correspondence still admits only `.md` paths, matching the previous skip of
  attachments, markers, and metadata.

### 5. Publish a valid LFS tree and preserve it through web edits
Type: Behavior
Status: done

An LFS tip is accepted through the existing publisher only when each pointer's
stored bytes match its size and digest and are within 10,485,760 bytes. Git
and the attachment projection hold pointer bytes. Root, nested, and empty
files are accepted. One byte over, a missing object, or a corrupt pointer
refuses without changing head, projection, learning, or native object rows.
Web note save and folder rename keep attributes, pointer identity, and
learning. A note-only save does not read or rewrite attachment content.
Creation still does not select LFS. History-range policy remains slice 6.

Accepted proof:
- Promise: valid pointers publish; oversize, missing, and corrupt tips refuse
  atomically; web edits preserve identity; note-only save does not touch
  payloads.
- Boundary: proposal publish, web note save, and folder rename.
- Setup: fixture sets the binding to `LFS` and stores payloads before publish.
  Save-cost counts are reset immediately before the note save.
- Observations: `NotebookGitAttachmentLfsPublicationControllerTest` (accept,
  exact limit, mixed refusal) and
  `NotebookGitAttachmentLfsWebContinuityControllerTest`.
  `NotebookGitWebContentSaveCostControllerTest.noteOnlySaveOnLfsNotebookDoesNotReadPayloadsOrRewriteObjects`
  sees zero content-store get/store calls and no `NotebookAttachment` query.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass (2625 tests, 2 skipped). After claim matching moved to
  `VerifiedNotebookAttachmentBytes.matchesClaim` and the LFS tests were split,
  the focused Gradle tests for the LFS publication, web continuity, raw
  publication, save-cost, content, size-admission, folder rename, and folder
  relocation classes passed.

### 6. Retain required history while allowing oversized corrections
Type: Behavior
Status: done

`NotebookGitAttachmentSizeAdmission` walks the unpublished first-parent range
for LFS notebooks. In-limit intermediate payloads must be stored. A new
oversized payload that appears only in unpublished intermediate commits may be
absent when the tip is valid. Previously accepted same-notebook digests stay
usable, including after removal and restore. Raw Git history stays strict.
Original commit IDs are kept. There is no client-claimed exemption.

Accepted proof:
- Promise: multi-version in-limit history, required temporary history, 20 MiB
  then 3 MiB omission, missing in-limit history refusal, and grandfathered
  oversized restore. Raw oversized intermediates still refuse.
- Boundary: proposal publish through `NotebookGitAttachmentSizeAdmission`.
- Setup: `enableLfs` and `pointerFor` store in-limit payloads. The 20 MiB
  pointer is not stored before publish. Grandfathering plants an already
  accepted oversized digest, then removes and restores it.
- Observations: `NotebookGitAttachmentSizeAdmissionLfsHistoryControllerTest`
  (omitted digest empty, tip payload present, first-parent chain, missing
  intermediate 400, restored oversized bytes). Raw cases stay in
  `NotebookGitAttachmentSizeAdmissionHistoryControllerTest`.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Result: pass. After the LFS cases moved to their own class, `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*AttachmentSizeAdmission*History*'` passed.

### 7. Clone current usable files with a recoverable failure path
Type: Behavior
Status: done

CLI clone stages with `GIT_LFS_SKIP_SMUDGE=1`, records the local notebook
binding, configures standard Git LFS against the notebook endpoint, hydrates
only the current tip (`lfs fetch origin` then `lfs checkout`), and installs the
destination only after success. Missing Git LFS or a failed download is
actionable and does not install pointer text. An existing destination is left
untouched. Rerunning clone is the retry. The bearer token stays in local Git
config. An unreferenced obsolete payload is not downloaded.

Accepted proof:
- Promise: skip-smudge staging, authenticated current-tip hydration, install
  only after success, actionable missing-client and failed-download paths,
  existing destination untouched, no tracked credentials, no obsolete object
  in the checkout cache.
- Boundary: `acquireNotebookGitCheckout` and
  `configureAndHydrateCurrentLfsCheckoutIfNeeded`.
- Setup: unit tests mock `spawnSync` and write a `.gitattributes` LFS filter
  plus a pointer. E2E plants an accepted LFS tip and a stored obsolete payload
  that is not a Git pointer, then clones with the installed CLI.
- Observations: `notebookAcquisition.lfs.test.ts` (fetch/checkout order,
  hydrated bytes, missing Git LFS, failed fetch, two origin removals).
  `cli_notebook_lfs.feature` clean main, exact `payload.bin` bytes, cache
  equals only the tip oid, token not tracked, `lfs.url`, sentinel file
  unchanged.
- Commands: `CURSOR_DEV=true nix develop -c pnpm cli:test` and
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
- Result: pass. CLI proof was rerun after LFS acquisition moved to its own
  module (64 files, 453 tests). E2E stayed valid (4 scenarios) and was not
  rerun.
Safe stop: a fresh checkout can receive LFS content without later story 15.

### 8. Publish local files and acquire them in another checkout
Type: Behavior
Status: planned

Existing CLI publish selects required objects from its unpublished range,
explicitly uploads using standard LFS, then submits the original Git bundle.
No blind upload of omitted oversized intermediate content; selection cannot
override server admission. Failed upload/submission preserves local refs/files.
A fresh CLI clone receives exact current content after publication and web save.

Proof: E actual installed CLI commits/filters, two valid versions and the
corrective-commit case. Observe original commit IDs and upload-before-acceptance.
The same loop owns size proof: three incompressible 3 MiB versions yield pointer
blobs, no payload copy in MySQL and current-only download; report bundle and
object traffic separately. Structural absence of raw binary blobs is required;
no flaky timing threshold or separate benchmark project. C and E.
8–10 minutes after established transfer and admission; if object enumeration
exceeds this, decompose on observed evidence instead of duplicating policy.
Safe stop: automated publish-to-fresh-clone journey complete in test mode.

### 9. Recover published versions explicitly after replacement or deletion
Type: Behavior
Status: planned

From a fresh object cache, explicit standard LFS fetch of a previously published
ref retrieves its exact bytes; omitted oversized intermediate content reports
unavailable. Removal of a current row never deletes historical objects.

Proof: E performs historical fetch against the configured endpoint after
replacement/deletion, observes requested digest/bytes and omitted-object failure.
Document the verified standard commands; no new Donut history UI/command.
5–8 minutes. Safe stop: preserved history has independently observed access.

### 10. Enable new notebooks with an honest interim receive boundary
Type: Behavior
Status: planned

After schema confirmation and complete-loop proof, creation selects LFS and
generates attributes. Existing notebooks remain raw, including never-cloned ones.
CLI success/help describes supported publish + fresh clone. Until story 15,
LFS pull refuses before changing refs/files and advises a separate fresh clone,
preserving unpublished work; do not affect legacy pull or broaden publish ancestry.
Older clients must fail actionably rather than publish raw payloads into LFS mode.

Proof: creation/cutover controller tests use actual creation, not injected mode;
history reset preserves the existing representation and attributes, without
deleting retained content. C tests the temporary refusal and raw pull regression.
E creates/publishes/web-edits/fresh-clones; R preserves legacy paths explicitly.
B/C/E/R; 8–10 minutes. Safe stop: first story is independently useful even if
existing-checkout receive is never delivered.

## Proof ownership and mapping from the original plan

| Original leaf | New owner |
| --- | --- |
| 1 schema | First 1; reduced to representation selection |
| 2 encoding | First 4; no duplicated payload metadata |
| 3a content | First 2 |
| 3b upload, 4 download | First 3, one authenticated transfer contract |
| 5 acceptance | First 5 |
| 6 history | First 6 |
| 7 web preservation | First 5, same accepted-tree guarantee |
| 8 acquisition, 10 retry | First 7, one successful/failed acquisition lifecycle |
| 9a selection, 9b publication | First 8, one publication operation |
| 11 pull/equal-head retry | Story 15, mapped receive slice |
| 12 rebase | Story 15, mapped reconciliation slice |
| 13 historical fetch | First 9 |
| 14 activation | First 10; receive boundary completed by story 15 |
| 15 size proof | First 8's end-to-end observation |
| 16 compatibility | First 4/5/10's format, projection and reset proofs |

No source promise was deleted. The two-story boundary postpones in-place receive,
not safe publication, fresh acquisition, history, or web preservation. Automated
clone/publish remain first; the user explicitly said a manual whole workflow
would not be used, so no infrastructure-only story was created.

## Refinement assessment and delivery

Ten slices after consolidation and outcome split. First-story scope, proof,
safe stopping points and dependency on deferred receive are explicit. No further
story-boundary decision is needed for this proposed automated fresh-checkout
increment under the authorized resplit. No slice inherits readiness from the
old plan. No product tests or infrastructure experiments ran during planning.

Owner decision, 2026-09-23: keep automated tests off Google Cloud Storage and
use the Book storage test seam. The remote contract test is removed. Standard
Git LFS still runs against the local backend. Low-to-medium effort confidence
remains a hypothesis; stop on actual overruns under the project rules.

Authorized implementation follows dough-execute-plan: Jidoka, fresh independent
post-change refactor, API regeneration for changed contracts, coordinator format
once, plan update, check-only commit hook, push and CI observer. Schema release
confirmation is separate from CI/push success. Use isolated disposable test/E2E
state; never Book production objects. No agents or implementation were started.
