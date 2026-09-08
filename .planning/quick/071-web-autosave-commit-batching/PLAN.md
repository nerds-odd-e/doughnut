# Web autosave commit batching

Source: [SEED-009 Story 10](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10)

## Goal and scope

Give notebook owners one Git commit for consecutive same-note web content
saves separated by less than ten minutes, without delaying durable saves or
rewriting history delivered to clients. The first changed save appends;
subsequent eligible saves replace only its unexposed tip, keeping its parent.
A ten-minute gap, client exposure, or intervening accepted notebook change
ends the batch. Later editing starts a new batch.

Ordinary note content at unchanged root or nested paths only. No-op saves do
not extend the interval. Creation commits stay separate. Retain identity,
learning data, authorization, validation and existing drift behavior.
No UI, background timer, configurable interval, native Git transport, expanded
structural synchronization, drift repair, or broader rebase. No product work
is authorized by this planning task.

## Current decisions

- User approved ten-minute rolling amendment and freezing on pull. Exactly
  ten minutes starts a new batch, as agreed in the preceding recommendation.
- The durable accepted tip may be unexposed and amendable. Previously exposed
  heads and their ancestors are immutable. Existing bindings default frozen.
- Every successful bundle download freezes under the binding write lock before
  returning the selected bytes. This covers clone and all pull clients. Freeze
  is conservative if delivery or a later client operation fails.
- Idempotent publish returns a head too: freeze that head in its existing
  transaction. Newly accepted proposals are never amendment candidates.
- Track one candidate head, note identity and last changed-save timestamp on
  the binding. Require candidate head == current head as well as same note,
  unchanged path and interval eligibility. Intervening writers therefore
  cannot accidentally amend an earlier candidate or a client-authored tip.
- Preserve first commit author metadata when replacing a tip; use the latest
  save for committer time. Keep the normal edit message. No extra tree metadata.
- No-op saves preserve Git bytes/head and do not reset the changed-save clock.
  A net reversion can remain a commit with the parent's tree; don't invent
  commit elision or another history-rewriting policy.
- Server persisted state owns eligibility; neither process-local state nor a
  timer is required. Freeze and save participate in database transactions.

## Execution context and ADRs

Production paths inspected (relative to repository root):
- `backend/src/main/java/com/odde/donut/services/notebookGit/WebNoteContentSaveService.java`:
  serializable transaction, binding lock, projection gate and canonical no-op.
- `AcceptedSnapshotPersistence.java` in that directory appends a snapshot;
  `WebNoteCreationService.java` also calls it. Keep creation append-only.
- `NotebookGitBundleBuilder.java` currently moves main only by ordinary ref
  update; replacement needs an explicit in-memory non-fast-forward operation.
- `NotebookController.downloadNotebookGitBundle` currently returns binding
  bytes in a read-only transaction. Move selection/freezing to a write
  transaction; it must finish before response bytes can leave the controller.
- `NotebookGitProposalPublisher.publish` already locks and has an idempotent
  early return. `NotebookGitProposalFolderAcceptance` also writes heads.
- `NotebookGitBindingRepository.findByNotebookIdForUpdate` provides the shared
  lock. No new transaction ownership model is necessary.
- CLI acquisition and accepted-history fetch both use the bundle endpoint;
  no other production history-read endpoint or serialized binding was found.
  Testability resnapshot returns only OK and must leave its replacement frozen.

Accepted constraints: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
keeps Portable content/identity metadata unchanged;
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) keeps the existing API route;
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits loud
failures, with no speculative recovery framework. Index and all record statuses
agree. ADR 0002 is Proposed, not binding; owner-approved amendment of unexposed
accepted tips differs from its current prose. Do not change its status or text.

Storage: additive nullable fields on the existing binding, null meaning frozen;
no new FK, DDL sequencing trick, REQUIRES_NEW visibility assumption, or process
cache. Existing committed-transaction and queued-writer tests provide matching
locking/visibility evidence. No separate database experiment is warranted.

## Outside-in proof

Use real controller calls, committed transactions, real MySQL and JGit bundle
inspection; `TestabilitySettings.timeTravelTo` avoids real ten-minute waits.
Extend `NotebookGitWebContentControllerTestBase` and use
`NotebookGitConcurrentWriterTestSupport` for independent requests and bounded
queued transactions. Existing atomic publication fixtures supply rollback and
fresh-persistence-context patterns. Do not inspect a batch through a download
mid-example unless the example intends to freeze it.

Commands during execution:
- `CURSOR_DEV=true nix develop -c pnpm backend:verify` for the migration slice.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only` for backend leaves;
  backend.mdc requires the full backend unit suite, not a single class.
- Regenerate the database ERD after migration using the database-erd skill.
- Preserve API wire signatures; regenerate the API only if a signature/DTO
  actually changes. No frontend/CLI product changes are expected.

Every leaf includes test-first proof, local cleanup, and execute-plan wrap-up:
Jidoka, fresh post-change-refactor agent, API generation if needed, coordinator
format once, update plan, commit and push. Current task only writes the plan.

## Ordered slices

### 1. Represent durable eligibility for exposure freezing
Type: Structure
Status: planned
Proof: Existing bundle/save controller tests remain green; persist and reload
an eligible binding fixture using the repository's real committed-transaction
fixture convention. Existing bindings load with absent eligibility.

Structure: Add nullable amendment-head, note-id (scalar, no FK) and last-changed
save timestamp fields to NotebookGitBinding in one new additive Flyway migration.
Put candidate clearing/eligibility state with the binding; don't create a second
history store. Add a concise fixture only where required by the immediately
following download-freezing proof. Production writers still append and do not
register candidates. Regenerate the ERD with the migration.
Enables immediately: slice 2 can durably freeze a candidate on download.
Sizing: about 5 minutes implementation/checks plus required backend:verify and
ERD runtime. Routine additive nullable columns only; use the next available
migration version at execution, not a version reserved in this plan.
Stop-safe: old histories stay frozen and all saves still append.

### 2. Freeze the exact history returned by a download
Type: Behavior
Status: planned
Proof: Through NotebookController, download a seeded eligible tip; the returned
bundle has that head, and eligibility is absent in a fresh committed transaction.
Use the existing queued-writer harness to prove selection waits on the shared
binding lock, then returns the committed current head. Denied access leaves
eligibility untouched; retain existing missing-binding behavior.

Behavior: An owner downloads a notebook's Git bundle → lock the binding, freeze
its current tip, select its bytes and commit → return that exact frozen bundle.
Keep the HTTP signature and authorization. Replace the read-only transaction
with a write transaction that completes before response delivery; avoid an outer
read-only scope suppressing the update. No browser acknowledgement or unfreeze
on download failure. All cloning/pulling clients already use this endpoint.
Implementation focus: NotebookController and a small transactional operation
in services/notebookGit, using the existing repository lock directly; no need
to load the full Portable projection just to download it.
Sizing: about 5–8 minutes plus full backend unit runtime. Scrutinized: one
response path and one reused locking proof loop; no new concurrency harness.
Stop-safe: no production amendment yet; downloads continue returning history.

### 3. Freeze a head returned by idempotent publication
Type: Behavior
Status: planned
Proof: Submit the current bundle through publishNotebookGitProposal using a
persisted eligible fixture; returned head is unchanged and freshly loaded
eligibility is cleared. Rejected publication retains its prior state.

Behavior: An authorized proposal is already the accepted tip → existing
projection validation succeeds → freeze that tip before returning its ID.
Use the existing locked publish transaction and early return. New proposal
heads stay frozen: candidate-head equality prevents stale eligibility from
matching them, including the separate folder-acceptance path. Do not introduce
an independent commit solely to freeze a rejected request.
Sizing: about 5 minutes plus backend unit runtime; one existing return branch.
Stop-safe: publication remains append-only and no candidate is yet registered
by production saves.

### 4. Make snapshot parent choice explicit without changing saves
Type: Structure
Status: planned
Proof: Existing snapshot builder and controller append tests remain green;
the domain-stable JGit builder proof constructs a replacement with the old
parent, bundles it, and reads back the selected tip and unchanged ancestor.

Structure: Extend the concrete snapshot-building operation to support replacing
one current tip with the same parent, retaining original author metadata and
using current committer time. Explicitly permit non-fast-forward movement only
inside that in-memory replacement operation; retain ordinary append behavior
for existing callers. Share tree/bundle construction rather than duplicating it.
Do not activate replacement in WebNoteContentSaveService yet.
Enables immediately: slice 5 can choose append versus replace under its lock.
Sizing: about 5 minutes plus backend unit runtime; existing tree construction,
bundle writer and builder tests cover the required path. No filesystem Git
repository, new transport or generic history-editing abstraction.
Stop-safe: existing production behavior is unchanged.

### 5. Keep a continuous same-note edit as one durable commit
Type: Behavior
Status: planned
Proof: Drive changed saves through TextContentController with controlled time,
then inspect the final downloaded Git chain and persisted note. Saves at 10:00,
10:08 and 10:16 produce one edit commit containing the last content and retaining
the pre-edit parent. This is the canonical identity/learning-data assertion.
Use concise scenario variations for the eligibility boundary listed below.

Behavior: A changed eligible ordinary-note save occurs → under the existing
binding lock, amend only the matching current unexposed content tip when the
same note/path was changed less than ten minutes ago; otherwise append →
persist note, bundle/head and refreshed candidate together in the same transaction.

Production focus: WebNoteContentSaveService and AcceptedSnapshotPersistence.
Register eligibility only after a changed ordinary-note content snapshot;
creation, cutover, local publication and unrelated snapshot writers never opt in.
Require matching candidate/current heads, same note/path, and a nonnegative
elapsed interval below ten minutes. Missing metadata means append. Existing
projection/no-op gates stay ahead of the append-or-amend decision. Candidate
state is database-backed, with no process cache. Don't reset it on no-op saves.

One grouping-contract proof loop, with these data variations:
- Just below ten minutes amends; exactly ten minutes and longer append.
- A no-op between changed saves does not extend eligibility or change Git bytes.
- Download, then two quick saves: downloaded head remains the parent of one
  new batch; repeat download freezes that new batch too.
- Same A/B/A save sequence yields three commits. Web creation or an accepted
  local structural/content commit between saves remains an ancestor; the next
  web save appends. Existing/newly created notes both use content-only batches.
- Root and nested ordinary paths obey the same rule; note/path mismatch cannot
  replace a candidate. Retain existing refusal/drift behavior rather than
  extending synchronization to unsupported changes.
- Fresh persistence contexts between calls retain both candidate and frozen
  state. A rejected save leaves note/head/candidate as before. Reuse the existing
  atomic-save transaction pattern rather than mocking the persistence layer.
- Queue download then save, and save then download with the existing harness;
  perform one further save and assert each returned head is still reachable.
  Update the existing two-queued-web-saves test's obsolete append-per-save
  expectation to one final-content batch. Existing publish/save races retain
  their stale-head and ordering behavior.

Adjust overlapping tests that assumed every unexposed save appended; preserve
all tests proving exposed history immutability. Keep the canonical Git shape
assertion in one example; siblings assert only the differing grouping outcome.
Update permanent capability documentation/comments that claim all durable
accepted tips are immediately immutable, limited to this changed concept.
Do not edit Proposed ADR 0002 without a separate drafting request.

Sizing: about 5–10 minutes of implementation and local proof authoring, plus
required backend suite runtime; medium confidence after extracting exposure
and JGit preparation. This is the largest leaf, scrutinized because all cases
exercise the same eligibility decision with existing fixtures. At five minutes
inspect progress; a non-test-runtime overrun beyond ten minutes requires finer
refinement of the remaining work before continuing. Do not postpone download
safety, writer exclusion, or atomic persistence to a later unsafe commit.
Stop-safe: complete agreed batching is active with every exposure boundary
already guarded. No transitional flags or partial history protocol remain.

## Promise ownership after refinement

| Promise | Owning slice and observation |
|---|---|
| Durable candidate, old histories frozen by default | 1 reloads nullable metadata; 5 proves behavior across fresh transactions |
| Clone/pull freeze exact returned bytes before exposure | 2 transactional download; 5 proves subsequent saves preserve returned heads |
| Idempotent publish cannot expose an amendable ID | 3 clears eligibility before the unchanged-head response |
| Original ancestor/author retained; latest content/time used | 4 replacement bundle round trip; 5 final controller chain |
| First save appends, rolling ten-minute threshold | 5 timed save chain including exact boundary |
| Canonical no-op does not extend interval | 5 no-op variation and existing no-op regression |
| New batches after exposure and intervening writers | 5 download, A/B/A, creation and accepted-proposal variations |
| Creation/cutover/client-authored history never amended | 1 defaults frozen; 3 candidate-head exclusion; 5 ancestry examples |
| Save durable with identity, learning data and validation preserved | 5 controller readback, existing atomic-save/authorization cases |
| Save/download race preserves every exposed head | 2 lock-selection evidence; 5 both queue orders plus a later save |
| No API/Portable-format change or broader sync | Existing regression suite in each leaf; 5 scoped code/doc review |

## Refinement assessment and execution gate

Initial slice 1 split into 1–3: metadata, download exposure, and idempotent
publication each have a separate bounded proof. Initial slice 2 split into 4–5:
JGit preparation immediately precedes the single append-or-amend behavior.
Initial slice 3's durability/concurrency promises moved to their actual owners
(1–2 and 5); it is no longer a detached verification stage after unsafe activation.
No completed work or evidence was discarded; all leaves remain planned.

Ready for execution, with no blocking product question. Estimates are hypotheses,
not guarantees. The required full backend suite and migration/ERD runtime are
explicit sizing exceptions; record actual elapsed time during execution rather
than treating implementation overruns as test time. Slice 5 retains medium
sizing confidence and the hard refinement trigger above.

## Learnings

The additional exposure path is idempotent publish. Download is currently
read-only, so freezing must move it into a write transaction. Shared binding
locking and controller test infrastructure already exist. Amendment must only
be activated after all exposure paths are guarded. No new storage experiment
is required for the routine additive-column and existing transaction pattern.
