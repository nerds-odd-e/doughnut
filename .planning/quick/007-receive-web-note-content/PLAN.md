# Receive web note content in a clean local repository

Source: [SEED-009, Story 3](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-3).
Status: planned; refinement recommended for leaves 6–7 before execution.

## Goal and scope

A notebook owner can receive accepted web content edits into the same ordinary
Git checkout used by Obsidian or an AI IDE, then continue the delivered local
edit/commit/publish workflow without copying files or cloning again.

The developer selected the narrow scope on 2026-09-05: body and valid authored
frontmatter changes to existing notes at unchanged Portable paths. The whole
current Portable tree must match accepted `main` before the supported web save.
Both root and folder-contained notes are included. Each changed durable save
appends an immutable commit; receiving fast-forwards a clean local `main` to
the exact downloaded accepted head, including several sequential commits.

Excluded: web creation, deletion, rename, moves, folder and README changes;
local structural publication; drift repair or another cutover; rebase, merge,
stash, conflict resolution, commit batching, standard Git remote transport,
interactive slash-command UI, and historical-checkout UI. No migration, external
Git store, background worker, or new Portable-tree metadata is needed.

The supported starting state is an already matching notebook and its bound
checkout. Newly populated notebooks with an empty accepted tree, or notebooks
with earlier unsynchronized changes, are outside this starting state. Existing
web saves remain available there, but do not advance Git or absorb prior drift.
Publication still rejects drift. `pull` returns accepted history, so its help
and success guidance must not claim all current web content was synchronized.
This compatibility boundary is distinct from failure during an eligible save:
that failure must roll back the save, never silently downgrade it to web-only.

## Execution context

- `TextContentController.updateNoteContent` prepares canonical authored content,
  authorizes the Note, calls `AuthoredNoteDocumentPersistence.persist`, and
  returns `NoteRealm` inside an ordinary transaction. The persistence helper
  also updates timestamps, orphan images, and authored-reference indexes.
- `NotebookGitProposalPublisher` already uses a fresh SERIALIZABLE transaction,
  locks the binding first, then reads notebook, folders and live notes, and
  persists note content and bundle together. Reuse its lock order and projection
  comparison concept. Do not append a second web commit when publishing calls
  the shared authored-document persistence helper.
- `NotebookGitProjection` compares canonical MySQL Portable entries with the
  accepted tree. Its current drift message assumes all web changes are still
  unsupported; replace that wording where the new supported case invalidates it.
- `NotebookGitBundleBuilder` writes root-only commits. The web path must import
  the stored accepted bundle, append a parented commit, and use
  `NotebookGitBundleWriter` to retain reachable history. Reuse tree writing;
  never invoke root-cutover or testability resnapshot as synchronization.
  `NotebookGitProposalImporter` has client-input error semantics; do not label
  corrupt stored history as an invalid client proposal when sharing mechanics.
- The owner-authorized `NotebookController.downloadNotebookGitBundle` returns
  the binding's accepted bundle. Keep that wire contract and its accepted-history
  meaning. `notebookAcquisition.ts` already downloads bundles with the normal
  token and API origin; publish modules already check binding and clean `main`.
- CLI tests drive `run` with real temporary Git repositories and mock only
  HTTP. Reuse `notebookClone.testHelpers.ts` and `notebookPublish.testHelpers.ts`;
  move genuinely shared helpers/names when their second caller is introduced.
- Backend Git tests use committed fixtures through
  `NotebookGitBundleControllerTestBase`, `GitBundleTestReader`, and
  `CommittedTransactionTestSupport`. Ordinary TextContent tests use an enclosing
  rollback transaction. Transaction changes must accommodate both deliberately;
  do not add REQUIRES_NEW and then broadly rewrite unrelated fixtures to fit it.

## Current decisions

1. Use `donut notebook pull <directory>` as a one-way receive command. Keep
   `publish` one-way. Reuse the existing local Git config binding and validate
   the configured API origin before sending credentials.
2. Require attached `main` and the existing clean-index/worktree policy,
   including untracked files. Reject active merge/rebase state too; a sequencer
   can be active while porcelain status is empty. Never discard unpublished
   commits or move another user branch. No forced checkout/reset.
3. Download and inspect the accepted bundle in command-owned temporary storage.
   Determine its `main` and whether local HEAD is equal or an ancestor, using
   real Git ancestry. Reject ahead, divergent, or unrelated histories. Identical
   heads are an unchanged success. Receiving never sends a proposal POST.
4. Import needed objects without installing persistent remote refs or rewriting
   the clone binding. Recheck local HEAD, branch and readiness after asynchronous
   download, immediately before a synchronous Git fast-forward operation. Use
   Git's own refusal to overwrite local work; no force, automatic stash, or
   rollback that could erase a concurrently edited user file. Imported unreachable
   objects are harmless on rejection; branches, index and worktree are protected.
   Clean command-owned temporary files/repos on success and failure. This is no
   guarantee against arbitrary simultaneous external Git writers or disk failure.
5. Capture web synchronization eligibility from the full pre-save projection
   under the same notebook serialization boundary as publication. Missing legacy
   binding or genuine pre-existing projection drift retains current web-save
   behavior without a Git update. Represent drift as an explicit comparison
   result, not a catch-all exception fallback. Stored-bundle corruption and
   commit/persistence errors propagate.
6. For eligible saves, reload the authoritative Note in the transaction, check
   authorization on it, prepare content through the existing save contract, and
   keep Note persistence, derived DB state, appended commit, and binding update
   in one transaction. Preserve identity and retain normal validation behavior.
   Read binding → notebook → folders → live notes in the established order;
   verify the note still belongs to that locked notebook. A concurrent move
   cannot redirect the write into an unlocked notebook. No automatic retry.
7. Keep serialization effective for real HTTP calls and committed concurrency
   proofs. Resolve the exact Spring transaction entry point in leaf 6; do not
   rely on an isolation annotation silently joining a weaker existing transaction
   or on a path-converted Note being freshly loaded. No broad transaction or
   persistence-framework refactor is authorized.
8. Web commits use the existing Donut system identity, a concise content-edit
   message, and the existing testable timestamp. No authored metadata enters
   the Portable tree. Equal canonical pre/post trees leave accepted head and
   bundle unchanged, even if existing non-Portable save effects still run.
9. Concurrent successful web saves must append in accepted order; publishing
   from an obsolete head must reject. If publishing wins first, a later web save
   uses that published head as parent. Lock/deadlock failures are unsuccessful
   requests with rolled-back state; do not promise every competing call succeeds.
10. Retain the E2E snapshot hook solely for initial pre-cutover fixture setup.
    Revise its obsolete “remove at Story 3” explanation to the remaining web
    structural-sync dependency. No snapshot after a tested content edit. Keep
    the existing baseline single-root clone assertion; new history proofs must
    retain the root and subsequent commits. No production resnapshot or new
    fixture-only endpoint is permitted.
11. Follow Accepted [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
    [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
    and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
    Git authority and v1 CLI scope come from the seed's recorded human decisions.
    ADR 0002 remains Proposed; this plan neither approves nor changes it.

## Outside-in proof and contract ownership

The canonical backend observation is ordinary content PATCH → read the same
Note → download accepted bundle → inspect exact tree and parent history using
real JGit and committed database state. CLI `run` tests separately prove that
the accepted bundle becomes the exact local head and working tree using real
system Git. Together these use the existing real high-level boundaries allowed
by planning.mdc. No manual testing or new Cypress harness is required.

| Promise / seed example | Owning leaf and observable proof |
|---|---|
| Binding, origin, authentication, owner-only download; decisions 1, 11 | 1, 3: CLI request/output assertions plus existing backend owner-authorization coverage |
| Dirty/untracked work, wrong branch, active operation, detached HEAD stay intact; example 4 | 2: real checkout before/after and actionable refusal |
| Identical heads; example 3 retry | 3: unchanged local head, index, files and no POST |
| No loss of unpublished commits or divergent history; example 4 | 4: ancestry rejection leaves original local work reachable and unchanged |
| Exact fast-forward, accumulated accepted history, no metadata; examples 1–3 | 5: CLI clean checkout, exact downloaded SHA and tree, retained ancestry/binding |
| Network/permission/bundle errors and late local edits cannot overwrite checkout; decisions 3–4 | 3–5: request failures, malformed bundle and data-driven download callback mutations |
| Ordinary accepted web content, root/folder paths, YAML/heading preservation, identity; examples 1–2 | 7: PATCH/read/bundle round trip on crafted notes with learning data |
| No partial accepted save, normal validation/authorization, eligible failure propagates; example 6 | 7: late binding-save failure rolls back note, derived DB state and binding; denied/invalid PATCH makes no Git commit |
| Pre-existing drift/missing binding keep existing web saves, no repair; example 7 | 7: explicit out-of-scope fixtures keep accepted history unchanged; existing publication drift rejection remains |
| Unchanged canonical save adds no commit; decision 8 | 8: same accepted SHA and bytes after repeated unchanged save |
| Web commit identity/message/time and synchronous resource cleanup; decisions 4, 8 | 7: canonical commit metadata assertion; 3–5: temporary-storage cleanup on success/failure; backend repositories use scoped close |
| Sequential web/publish loop and immutable ancestry; example 5 | 9: web B → ordinary publish C → web D → bundle retains A/B/C/D and same Note |
| Competing writers serialize safely; decisions 6–9 | 10: controlled committed races assert accepted parentage and same-head projection, stale publish rejection or rolled-back failure |
| Fixture boundary and limited CLI claims; decision 10 / scope | 7 updates hook wording; 5 supplies accepted-history guidance; existing installed clone/publish feature remains green |

No standalone tests of unhandled internal failures are required by ADR 0006.
The late-save failure proof tests the business promise of atomic persistence.
Use existing scoped failure injection and fresh-transaction assertions. Race
proofs use barriers/latches and bounded futures, not sleeps. Do not share a
mutable CurrentUser across competing threads.

## Ordered leaves

### 1. Explain an unusable receive binding
Type: Behavior
Status: done
Proof: CLI `run` cases for missing directory/binding and mismatched API origin.

Behavior: the owner runs `pull` outside a correctly bound checkout → explain
the binding prerequisite without sending credentials or changing local state.

Add argv routing and usage; share existing binding checks without duplicating
them. Until leaf 3, valid input reports receive not yet available, never success.
Sizing: 3–5 minutes, medium confidence; one CLI entry-point loop.

Learning: publish and pull now share one bound-checkout resolver and common
real-Git test fixtures; valid pull input deliberately stops at the temporary
unavailable boundary owned by leaf 3.

### 2. Protect an ineligible working checkout
Type: Behavior
Status: done
Proof: CLI `run` readiness cases with real staged/unstaged/untracked state,
detached/non-main HEAD, and an active Git operation with otherwise clean status.

Behavior: a correctly bound checkout is not eligible for fast-forward receive
→ `pull` → actionable refusal preserving local files, index and branches.

Share the current publish readiness concept with operation-appropriate text;
keep any new operation-state check bounded to this receive requirement.
Eligible input still receives the temporary unavailable result.
Sizing: about 5 minutes, medium confidence; one readiness proof family.

Learning: publish and pull share attached-clean-`main` checks, while pull adds
read-only detection of active Git operation markers that porcelain can omit.

### 3. Recognize an already received accepted head
Type: Behavior
Status: done
Proof: CLI `run` with a downloaded real bundle equal to local HEAD, and HTTP
denial/malformed-bundle variations before any local mutation.

Behavior: clean local `main` already equals the downloaded accepted `main` →
`pull` → report the unchanged accepted SHA without modifying local state.

Reuse authenticated download and temporary bundle inspection, with cleanup on
all exits. Only GET is sent. Keep differing valid heads explicitly unavailable
until their eligibility and fast-forward are implemented. Existing backend
download authorization is reused, not replaced by trusting the CLI binding.
Sizing: about 5 minutes, medium confidence; one download/no-op proof loop.

Learning: pull and publish now share process-scoped accepted-history download,
validation, and cleanup; pull compares the resulting accepted SHA with local
`main` without installing refs in the user checkout.

### 4. Preserve unpublished or unrelated local history
Type: Behavior
Status: done
Proof: CLI `run` real-Git cases for local-ahead, divergent and unrelated roots.

Behavior: local `main` is not an ancestor of accepted `main` → `pull` → refuse
without losing any local commit or changing the checkout.

Inspect ancestry in temporary storage using imported objects from both sides;
do not infer ancestry from one parent or from file similarity. A valid ancestor
still reaches the temporary unavailable result. Reuse leaf 3's download loop.
Sizing: 3–5 minutes, medium confidence.

Learning: pull imports local `main` only into the scoped accepted-history
repository and uses Git reachability to distinguish an accepted descendant
from local-ahead, divergent, and unrelated histories.

### 5. Fast-forward to the downloaded accepted revision
Type: Behavior
Status: done
Proof: CLI `run` receives one or several real commits, asserting exact SHA,
clean tree, ancestry, unchanged binding and absence of added Portable metadata.
Vary the download callback to create local work/change HEAD before completion.

Behavior: clean local `main` is an ancestor of downloaded accepted `main` →
`pull` → advance the same checkout to that exact accepted revision safely.

Import objects, recheck local eligibility and captured HEAD, then use synchronous
non-forced fast-forward Git. Remove the temporary unavailable path. Report the
accepted head and the narrow web-content limitation; update command help and
clone guidance where needed. Receiving accepted local publications already has
value at this stopping point, before web commits are implemented.
Sizing: about 5 minutes, medium confidence; one checkout-update proof loop.

Learning: receive imports accepted objects without refs, revalidates the
captured clean attached `main` after download, and lets `git merge --ff-only`
perform the synchronous checkout update. User guidance names accepted history
without implying every current web edit is included.

### 6. Establish the notebook transaction boundary for a web content save
Type: Structure
Status: planned
Proof: existing TextContent save/validation/authorization tests and Git
publication tests remain green with unchanged external behavior.

Internal change: route the ordinary content PATCH through a cohesive web-save
entry point that reloads/authorizes the Note and shares the established notebook
lock order with publication. Keep all existing authored persistence effects.
This immediately enables leaf 7's atomic accepted web-content revision; no
other web mutations or shared persistence callers gain Git side effects.

Resolve transaction propagation and path-converter identity using the concrete
call sites and test fixtures. Share only the projection/lock concepts actually
needed by the next leaf. Do not add unused generic transaction abstractions.
Sizing: target 5 minutes, low confidence because existing rollback-test callers
and committed publisher transactions differ. Refinement recommended before
execution; exact propagation/test adaptation is the remaining technical risk.

### 7. Accept a supported web save as one atomic Git revision
Type: Behavior
Status: planned
Proof: committed content-controller → read same Note → bundle tests prove a
single save, its commit metadata, and its atomic rollback. Data variations cover root/folder paths,
authored YAML/headings, unauthorized/invalid requests, and pre-existing drift.

Behavior: the full pre-save projection matches accepted A → ordinary content
PATCH changes one existing note → accepted B has parent A and exact canonical
content, with the same Note and learning data, or the whole save rolls back.

Import the stored bundle, reuse canonical tree writing to append one commit,
and persist its binding within leaf 6's transaction. Keep the trusted stored
bundle error path distinct from client-proposal validation. Close repositories
and inserters synchronously. Detect pre-existing drift explicitly before save:
preserve old web-only behavior there without changing the accepted bundle.
Do not catch a failed eligible Git update and pretend the notebook was drifted.

Replace existing tests that expect an ordinary eligible content edit to leave
Git stale: publication against old A now rejects stale ancestry. Preserve
structural-drift cases and messages appropriate to the remaining unsupported
scope. Update the retained test hook's cleanup comment, without changing its
setup-only behavior or introducing a production call.
Sizing: target 5 minutes, low confidence. The new append operation and precise
replacement scope may exceed the target; refinement recommended after leaf 6's
transaction choice is made. Atomicity is indivisible and must be proven before
this behavior is considered delivered; do not defer rollback to a later leaf.

### 8. Avoid a new revision for unchanged Portable content
Type: Behavior
Status: planned
Proof: content PATCH with unchanged canonical content keeps accepted SHA and
bundle bytes unchanged, including a repeated save after a changed save.

Behavior: an eligible save produces the same canonical Portable tree → save →
retain accepted history without an empty Git commit.

Compare canonical trees, not raw request bytes, because web validation may
normalize typed Markdown. Retain existing non-Portable save effects.
Sizing: 3–5 minutes, medium confidence.

### 9. Continue accepted history across web and local editing
Type: Behavior
Status: planned
Proof: committed controller round trip A → web B → publish C → web D checks
the downloaded bundle's parent chain and unchanged Note identity.

Behavior: a web-edited head B has been received and one direct-child content
commit C is published → another supported web save → append D on C, retaining
all prior accepted commits and their exact object IDs.

Reuse existing publication fixtures and the real publish controller, not a
manually changed binding. Fix any web-append assumption limited to root-only
history; share no independent revision model. CLI accumulation is proven in 5.
Sizing: about 5 minutes, medium confidence; one alternating-history loop.

### 10. Keep competing accepted writers in one linear history
Type: Behavior
Status: planned
Proof: controlled committed races through real content/publish controller
boundaries; assert the accepted bundle parent chain and matching DB projection.

Behavior: eligible web saves and publication compete on the same notebook →
each successful write takes its place after the current accepted head; a stale
publication or failed transaction cannot overwrite an accepted revision.

Use the existing concurrency harness, adapting its transaction coordination to
the new web-save boundary. Cover web-first and publish-first order, plus two
web content saves as variations of the same serialized-writer contract. Reload
state in fresh transactions and verify returned successes against retained
history. Keep unsupported structural-race rejection coverage. Any discovered
atomicity hole must be fixed here, not documented as a passing safety result.
Sizing: about 5 minutes, medium confidence from the existing committed race
harness; refine if adapting that harness becomes a separable preparation beat.

## Verification and delivery

- CLI leaves: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run notebookPull`;
  when sharing helpers, add `notebookClone notebookPublish` as Vitest filters.
  Use Vitest directly for focused runs: the package's `test` script also chains
  a separate Python test command and is not a simple filter-forwarding wrapper.
- Backend leaves: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend rule requires all backend unit tests. Its runtime is a stated
  exception to the five/ten-minute leaf budget, not permission to enlarge code.
- Regression for the retained fixture:
  `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`
  after the web-save change affects it. No manual or mutation testing.
- Preserve HTTP wire shapes when moving transaction entry points. If controller
  signatures or related DTOs change, invoke generate-api-client and regenerate
  with `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; do not hand-edit
  generated artifacts. No schema migration is planned.
- Every implemented leaf uses execute-plan: Jidoka → fresh post-change-refactor
  agent → API generation when necessary → coordinator runs
  `./scripts/run.sh pnpm format:changed` once → PLAN update → commit → push.
  The commit hook supplies independent check-only lint. Never commit red-only
  work or declare a leaf done before its promised observations pass.
- At five minutes scrutinize scope; after ten minutes park only attempt-owned
  WIP and refine this same PLAN unless focused verification runtime explains
  the overrun. Unexpected changes to product boundaries return to the seed.
- When complete, retain enduring behavior in tests/docs, clean spent plan
  history, and reduce Story 3 to goal/scope while preserving its anchor and
  sibling stories. Full fixture-hook removal remains explicitly deferred.

## Readiness

Refinement recommended: leaves 6–7. Each names one immediate structure or
observable outcome, but the exact transaction entry point and the compact
Git-append integration still have low sizing confidence. Other leaves have
one focused proof path using existing seams; timings are hypotheses. Do not
execute this plan merely because it was written: this request authorizes
refinement and planning only.
