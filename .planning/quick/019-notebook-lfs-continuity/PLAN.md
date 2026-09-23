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
- Published increment: `7db86ca0a5f54aba91d6856c76864025eaa9d7b3` on
  `refs/heads/story/notebook-lfs-continuity`. Registered with the story-branch
  observer.
- Default-checkout refresh is deferred (`unclear-ownership`). That checkout
  stayed at `e0fcf33229`, clean, one commit behind `origin/main`.
- Preparation: `./scripts/run.sh bash scripts/worktree_setup.sh` succeeded, then
  `CURSOR_DEV=true nix develop -c node -e "console.log('worktree-command-ready')"`
  printed `worktree-command-ready`.
- Slices 2–3 stay blocked until an isolated GCS target and a representative
  standard-client result exist. Slice 1 does not depend on that evidence.
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
- Reuse GCS client/configuration patterns from `BookStorageConfiguration`,
  not Book keys, deletion policy, byte-array storage, or non-prod DB fallback.
  Use notebook-scoped digest keys, immutable content, standard Batch/Basic API,
  and existing authorization at every transfer route.
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

Unit proof drives controllers or CLI `run`; mock only external GCS/network.
The real protocol journey uses installed CLI + standard Git LFS + actual backend.
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
Status: planned

Add the narrowly scoped attachment content owner consumed immediately by slice 3:
stream, count, hash and durably store immutable notebook/digest content. Reuse
SDK 2.73.0 and private GCS configuration patterns. An interrupted/mismatched
attempt cannot become verified content or overwrite valid content. No custom
cache, generic storage framework or durable pending-upload workflow.

Proof: B with external GCS seam; an isolated representative GCS contract check
must observe exact bytes, wrong size/digest, interrupted upload, and same-digest
retry before broad integration. Planned test `NotebookLfsGcsContractTest` uses
the ordinary B command with explicit isolated test-storage configuration; record
the literal configured invocation, SDK version, prefix and result at execution.
No target/credentials are currently established: missing configuration skips no
required proof and never falls back to production or Development.
8–10 minutes excluding representative test runtime. Safe stop: unused verified
content operation; slice 3 is its immediate consumer.

### 3. Transfer exact content with the standard authenticated client
Type: Behavior
Status: planned

Given a permitted notebook owner/reader, standard LFS Batch/Basic upload/download
uses the same verified content. Wrong notebook/access and missing objects return
standard failures; a hash alone grants no access. Upload does not accept a commit.

Proof: HTTP-boundary `NotebookLfsTransferControllerTest` plus real standard-client
round trip in E. Observe upload, download, authorization denial and exact digest;
slice 2 owns storage failure combinations. Add Git LFS to reproducible runtime
and record actual version (host `git lfs version` previously failed).
B and E; 8–10 minutes if the standard auth exchange fits existing bearer access.
If not, stop this slice and reassess the precise integration issue.
Safe stop: transfers work, user-facing LFS activation still off.

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
Status: planned

A valid root/nested/empty attachment proposal is accepted through the existing
publisher with pointers in Git and projection. Verify referenced actual size,
digest and durability before acceptance. Preserve attributes, exact file identity
and private learning state through existing web edits/folder operations.
A note-only save performs no payload reads or GCS rewriting.

Proof: extend `NotebookGitAttachmentPublicationControllerTest`, committed-state
rollback support, derived-tree oracles, and `NotebookGitWebContentSaveCostControllerTest`.
Exact 10 MiB succeeds; one byte over/missing/corrupt required content refuses.
One mixed refusal owns atomic head/projection/learning assertions. Scope SQL/GCS
observations to the save, excluding setup. B; 8–10 minutes, reusing existing
projection and admission rather than building another store.
Safe stop: complete server current-tree behavior in test mode.

### 6. Retain required history while allowing oversized corrections
Type: Behavior
Status: planned

Extend the same admission owner over the existing first-parent range. Require
valid intermediate payloads; allow only new oversized intermediate-only LFS
payloads to be omitted. Previously accepted same-notebook content remains exempt
and retained. Preserve original commit IDs; no claimed client exemption.

Proof: extend story 12's history controller proof with valid multiple versions,
unchanged-tip history, 20 MiB then 3 MiB correction, missing in-limit history,
and trusted-history grandfathering. Raw admission stays strict. B.
8–10 minutes: one range policy and shared content evidence.
Safe stop: server policy complete, original retention promise unchanged.

### 7. Clone current usable files with a recoverable failure path
Type: Behavior
Status: planned

Existing CLI clone stages without premature smudge, records binding/endpoint,
configures standard LFS, hydrates only the selected current ref, and installs
the destination only after success. Missing client/auth/download failures are
actionable and never report pointers as usable files. Rerunning clone is the
retry; existing destination content is untouched. No new resume subsystem.

Proof: CLI `run` clone/failure tests plus E actual hydration. Observe clean main,
exact bytes, no obsolete payload requests, no tracked credentials and intact
existing destination. C and E; 8–10 minutes.
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

Remaining readiness concern is localized to 2–3: actual isolated GCS and standard
client integration has no representative result or established test target yet.
The plan is refined; mark it not-ready for direct execution until the evidence
and environment prerequisites are resolved, rather than changing story scope or
claiming a passing infrastructure check. Low-to-medium effort confidence remains
a hypothesis; stop on actual overruns under the project rules.

Authorized implementation follows dough-execute-plan: Jidoka, fresh independent
post-change refactor, API regeneration for changed contracts, coordinator format
once, plan update, check-only commit hook, push and CI observer. Schema release
confirmation is separate from CI/push success. Use isolated disposable test/E2E
state; never Book production objects. No agents or implementation were started.
