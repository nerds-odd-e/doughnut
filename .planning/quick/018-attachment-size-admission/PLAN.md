# Reject oversized notebook attachments

## Source and authority

- Identity: SEED-035#story-12
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-12).
- Owner accepted the preventive rationale, content-based grandfathering,
  accepted-state atomicity, and strict raw-Git limit before LFS recovery in the
  2026-09-23 refinement conversation. This request authorizes planning only.
- Direction: [attachment storage transition](../../NORTH-STAR.md#attachment-storage-transition).
- Preparation workspace: `/Users/terryyin/.codex/worktrees/attachment-size-refinement/doughnut`,
  branch `codex/attachment-size-refinement`, created by this session from
  `0dc458c359fff2ef15e6d42c4f545c2fd699e857`.
- Originating/integration checkout: `/Users/terryyin/git/doughnut`.
  Eventual publication target: `origin/main`; no publication requested yet.

## Goal and scope

Reject newly introduced attachment payloads larger than 10,485,760 bytes across
the entire submitted raw-Git commit range. Accept the boundary inclusively.
Grandfather payloads already accepted as attachments in this notebook's retained
history, independent of current placement or presence. Rejection leaves accepted
head, durable Git objects, projection, notes, files, and private learning data
unchanged. Show the offending path, actual size, limit, and usable recovery advice.

Temporary proposal import is allowed. This is not an aggregate quota or a bound
on request memory. Do not reduce the cap to a bundle-size check. Preserve current
Markdown/structural classification, existing web-image validation, ordinary web
preservation, and old published bytes. No LFS/GCS, migration, automatic local
history rewrite, new upload UI, Book changes, or garbage collection.

Story 13 replaces strict intermediate rejection for LFS with the owner-approved
omission of new oversized unpublished intermediate payloads. Until then, removing
one at the tip is insufficient; rewrite only unpublished commits. No accepted
commit or previously published payload is removed by this work.

## Existing owners and architectural assessment

Inspected at the preparation base above (source inspection, not executed proof):

- `NotebookGitProposalPublisher.publish` owns authorization, locked current-head
  checks, ancestry, and publication under a serializable transaction. Put the
  admission rule before projection mutations and before all non-idempotent
  acceptance branches, including attachment-only and unchanged-tip-tree ranges.
- `NotebookGitProposalAncestry.firstParentRange` already enumerates the linear
  range; reuse it. `NotebookGitProposalTreeShape.isAttachment` owns current
  classification. Reuse that classification rather than recognizing extensions
  separately. LFS `.gitattributes` support remains in story 13.
- `NotebookGitProposalAcceptance` materializes the final attachment projection;
  `NotebookGitAcceptedRepositoryStore` and `NotebookGitReachableObjectCopier`
  persist all reachable raw Git objects. Do not discard blobs or validate only
  projected rows. Discover grandfathered attachment object IDs from trusted
  notebook history rooted at the locked accepted head, not arbitrary objects
  supplied in the bundle or another notebook. Inspect object lengths without
  loading new payload byte arrays solely for size checks; deduplicate object
  inspection within the operation. No new persistent exemption registry.
- Web saves derive from accepted content and introduce no new generic attachment
  payloads. Do not add whole-history scans to ordinary web note saves.
- `NoteImageUploadDTO` and `MultipartFileValidator` already implement inclusive
  10 MiB multipart image validation. Preserve the legacy image model and its
  validation entry point; no blanket file-size policy for Books or audio.
- `cli/src/commands/notebook/notebookPublishSubmission.ts` forwards the server's
  `ApiError.message` and leaves local refs/files untouched. Reuse that path;
  do not add a second CLI admission algorithm or automatic rewrite.

PFE conclusion: extend current Git admission; reuse ancestry, classification,
transaction/storage owners, image validation, and CLI error handling. The gap is
attachment-size admission, not synchronization or a new storage abstraction.

ADRs 0002 (Git-native synchronization), 0004 (Portable format), and 0006 (failure
handling) apply. Preserve submitted commit IDs and loud, actionable refusals.
The owner-approved LFS exception is recorded in story 13 and is not applied to
raw blobs here. No novel infrastructure/storage-engine assumption needs an
experiment for this plan.

## Outside-in proof and slices

All slices are planned. Tests drive the real controller/installed CLI, not a
mocked admission service. Shared rollback assertions belong in one mixed-change
scenario; other cases assert only their unique delta. Grandfather fixtures must
create pre-policy accepted content deliberately: they prove preservation, not
that the new rule accepts an oversized addition.

### 1. Reject oversized proposed files while preserving existing content

Type: Behavior
Status: done

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
before refactor. Refactor kept `publish` → ancestry → `admitTip` before
projection mutations and reran
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGitAttachmentSizeAdmission*'`
(pass). Image Bean Validation tests were not edited. Grandfather fixtures use
`storeFolderAttachmentAndSnapshot`, then prove reuse through publish. Slice 2
should extend `NotebookGitAttachmentSizeAdmission`, not grow
`NotebookGitProposalPublisher`.

Behavior: An owner publishes root or nested files → exact-limit payloads are
accepted; a new over-limit tip payload is refused with path, size, and limit.
Previously accepted same-notebook bytes remain reusable, including a version no
longer at the accepted tip. Different oversized bytes are refused.

Extend admission at the existing publisher boundary. Keep the accepted-content
identity lookup and size rule together. Cover initial attachment-only publication
and mixed note/file changes without requiring a note to trigger validation.

Proof: Extend `NotebookGitAttachmentPublicationControllerTest` or a capability-
named sibling using `NotebookGitWebContentControllerTestBase` and existing bundle
fixtures. Parameterize root/nested boundary cases. One mixed refusal observes
unchanged committed head/history, attachment projection, note content and learning
tracker, plus absence of newly accepted object bytes. Reuse committed-state
readers from `NotebookGitPublicationAtomicControllerTest`; do not add its injected
late-failure application context merely to test an ordinary admission refusal.
Prove grandfathered keep/rename/historical restoration, a web note save preserving
those bytes, and no exemption from another notebook. In
`NoteControllerUploadNoteImageTests`, use the existing Bean Validation entry to
verify exact-limit and one-byte-over image DTOs; a direct controller invocation
alone does not exercise `@Valid`. Existing successful image persistence proof
remains the owner of the upload behavior. No image-production refactor is needed.

Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Sizing: ~8 minutes of edits and local review, plus the required whole-backend
suite. The >5 minute estimate is deliberate: grandfathering and refusal must ship
together to avoid denying previously accepted content. Full-suite runtime is the
only >10 minute exception; if edits exceed 10 minutes, stop and finer-decompose.
Safe stop: tip protection and compatibility work; intermediate admission is
explicitly unfinished until slice 2, with no content loss or history changes.

### 2. Refuse oversized payloads hidden in unpublished history

Type: Behavior
Status: done

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
`admit` walks `firstParentRange` from index 1. History scenarios live in
`NotebookGitAttachmentSizeAdmissionHistoryControllerTest`. Refactor consolidated
fixtures and reran
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGitAttachmentSizeAdmission*'`
(pass). Docs in `docs/notebook-git-attachments.md` state the raw-Git rule and
that the LFS exception is not delivered.

Behavior: A new oversized payload occurs in an intermediate commit, followed by
deletion or smaller replacement → the whole publication is refused even if the
tip is valid or matches the accepted tree. A wholly within-limit range publishes
with its original commit IDs and historical attachment bytes intact.

Extend the same rule over the existing first-parent range, excluding the accepted
base from new-content admission. Check attachment identity at each historical
path; later rename to Markdown must not hide an earlier attachment. Every
non-idempotent path through the publisher must pass the same check. Reuse the
grandfathering decision, without a second tip/history policy.

Proof: Reuse range construction and downloaded-history inspection from
`NotebookGitProposalCommitRangeControllerTest`. Add attachment scenarios for
add/delete (including an unchanged final tree), oversize/smaller replacement,
and valid multi-version history. Verify rejection before durable storage and
acceptance preserving earlier bytes after replacement/deletion. A pair of valid
files whose sum exceeds 10 MiB proves this is a per-file limit. Boundary and full
rollback details remain owned by slice 1. Update attachment publication docs with
the raw-Git rule and its later LFS replacement, without claiming LFS is delivered.

Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Sizing: ~5 minutes of edits plus the required suite; test-runtime exception only.
Safe stop: complete server admission and historical preservation; CLI recovery
journey still requires its outside-in proof.

### 3. Recover from a size rejection through the installed CLI

Type: Behavior
Status: done

Accepted proof: installed CLI journey in
`e2e_test/features/cli/cli_notebook_attachment_size_admission.feature`.
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature`
passed before the scenario was split out; replacement
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_attachment_size_admission.feature`
passed (1/1). Refusal wording change covered by
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`, then focused
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGitAttachmentSizeAdmission*'`
after the assertion was tightened. CLI production code was unchanged. New CLI
features must be listed in `scripts/isolated-cypress-active-specs.mjs`.

Behavior: Publish an oversized attachment → CLI reports the offending path,
actual bytes, 10 MiB limit, and explains removing it from unpublished history;
accepted state and the local committed proposal remain intact. The owner amends
that unpublished commit to contain a permitted payload → publish succeeds and a
fresh clone receives the corrected exact bytes.

Finish actionable server rejection wording, reusing the CLI's existing error
transport. The advice must distinguish amend/rebase of unpublished history from
adding a later deletion; never recommend rewriting accepted commits. Product code
in the CLI changes only if the real journey exposes a forwarding gap.

Proof: Add one scenario to
`e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature`. Existing
`cli_notebook_clone.ts` steps and `cli.notebookCloneCheckout()` already own
`publishExpectingRejection`, retained-proposal assertions, publish and fresh clone.
Extend their Git fixture operations with deterministic sized bytes and an explicit
unpublished amend. Drive the installed CLI against the actual backend; do not mock
its admission response. Observe the rejection text, unchanged accepted/local
heads, then successful corrected publication and exact fresh-clone content.
`cli/tests/notebookPublish.submission.suite.ts` already tests generic 400 message
forwarding and local-state preservation through `run`; it is not proof of backend
admission and needs no duplicate size-specific mock scenario.

Command: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature`.
Also run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if backend wording
changed; `CURSOR_DEV=true nix develop -c pnpm cli:test` if CLI production changed.

Sizing: ~8 minutes of edits plus isolated E2E startup/run. The journey is kept
together because rejection alone would not prove recoverability. Test-runtime
exception only; >10 minutes of edits requires finer decomposition.
Safe stop: story's publication, refusal, preservation, and recovery promises are
proved without LFS or new UI.

## Delivery and review

During authorized execution, follow dough-execute-plan: Jidoka, fresh independent
dough-post-change-refactor agent, API generation only if signatures changed,
coordinator `./scripts/run.sh pnpm format:changed` once, update plan without a
second routine formatting pass, commit with check-only hook, push and CI repair.
No API/schema change is expected. Use isolated worktree test tooling; inspect
MySQL/Redis logs if services fail. No manual testing is required by this plan.

Plan review: three cohesive Behavior slices, one admission rule with progressively
broader observations, no preparatory abstraction or split by implementation layer.
Every current promise maps above; future LFS recovery and object omission remain
in story 13. The >5 minute slices were scrutinized and retain a single proof loop;
required full-suite/stack startup is explicitly separated from edit estimates.
No remaining slice-specific blocking concern was identified in this review.
Structured readiness is recorded in the story after reviewing this final content.

## Execution evidence

Story Branch Mode. Created this session.

- Owned workspace: `/Users/terryyin/.cursor/worktrees/attachment-size-admission/doughnut`, branch `story/attachment-size-admission`, starting revision `d8c411dd724e209bfbb813ba477bad33667b272f`.
- Originating checkout: `/Users/terryyin/git/doughnut`.
- Integration checkout: `/Users/terryyin/git/doughnut` (local `main`). Default-checkout refresh after the claim was deferred (`unclear-ownership`); it remains at `d8c411dd724e209bfbb813ba477bad33667b272f`.
- Claim publication: `4ad31c66bca8d73110e8a37de10a5aded3711fa3` accepted on `origin` `refs/heads/main`.
- Increment target: `origin` `refs/heads/story/attachment-size-admission`.
- Published increments on `origin` `refs/heads/story/attachment-size-admission`: `b3ddc1fa3e48bdf95cb0b5b264636a720d0cd7cc` (slice 1), `71c3716963f300145be29dde4da4ad02ccf5f922` (slice 2), `4e04fa3eb54ed8a39b8935c7c8bcb805b6018804` (slice 3). Registered with observer `/tmp/dough-ci-501/watch-mO4rGQ`.
- Replanning: preserved (allowed on overrun).
- CI source: GitHub Actions workflow `ci.yml`, display name `donut CI`. Observer directory `/tmp/dough-ci-501/watch-mO4rGQ`, bound to `story/attachment-size-admission`. Claim on trunk is `pendingCi: unobserved`.

All three slices are published. The latest increment is `4e04fa3eb54ed8a39b8935c7c8bcb805b6018804` on `origin/story/attachment-size-admission`.
