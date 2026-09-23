# Simplify Git publication during note saves

Status: abandoned; strict implementation-size gate failed; no product change retained.
Work item: **SEED-037#story-2**.
Source: [refined story](../../seeds/SEED-037-note-save-cost-independent-of-folder-count.md#story-2)
and the owner's 2026-09-23 simplify-or-abandon decision and planning request.

## Goal and acceptance

Try one coherent simplification of existing publication persistence. Keep it
only if it makes the design cleaner, reduces affected production code and
unnecessary save-path SQL, preserves correctness, and leaves representative
latency at least as good. Roughly one-second production saves are acceptable;
this is not an incident response or a fixed-query-budget promise. Failure or
unresolved evidence ends the attempt and removes the story from the backlog.

Scope: a body edit at an unchanged path, root and depth 12, on the existing
ancestor-only save path. Shared callers must retain their behavior. No index,
cache, schema change, new storage/publication mode, whole-notebook assembly,
async acknowledgement, history rewrite, LFS work, or general query campaign.

## Existing owners and selected hypothesis

PFE inspected production callers and the real JGit/JDBC adapter at preparation
base `8c96f96894c4a8efd5923c5ae7bd4a334853af3c`:

- `AcceptedWebChangeService.commitIfChanged` already obtains the accepted
  parent, derives a tree and calls `NotebookGitCommitBuilder.append`. Append
  returns the new commit ID, which this caller discards. It then calls
  `NotebookGitAcceptedRepositoryStore.store`, which reads `main` again.
- Proposal acceptance already owns `ImportedProposal.mainHead`; its call to
  the same store also rediscovers the head. Unlike web saves, this repository
  is in memory and its reachable objects must be copied into native storage.
- Creation/reset calls `apply`: it repeats binding head/timestamp/save logic
  but must persist a new binding before copying objects because of the FK.
  That ordering is essential and must not disappear through consolidation.
- `JdbcNotebookObjectDatabase.insertMissing` already batches existence checks
  and insertion. The directory editor already limits reads to affected paths.
  Neither requires replacement. Ref reads remain live and ref updates retain
  compare-and-swap and JGit fast-forward checks.

Selected hypothesis: give the existing persistence owner the accepted head
already produced/validated by its caller, and consolidate duplicated binding
persistence while preserving new-binding-before-object-copy ordering. Keep
JGit commit construction, native ref update, proposal validation and domain
change capture with their current owners. Adapt all actual callers together;
do not introduce a separate web fast path, a caller-controlled trust flag, or
an overload that merely preserves redundant orchestration.

This is a hypothesis, not a claim that it saves enough lines. If preserving
the differing copy/creation lifecycles makes the combined implementation
larger or harder to understand, abandon it. Repeated commit parsing is recorded
historical evidence but is not a second optimization candidate in this plan.

Follow [North Star: one accepted-change boundary](../../NORTH-STAR.md),
[ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and [domain operation ownership](../../../docs/notebook-git-synchronization.md#domain-operation-ownership).
Preserve the format under [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
and loud failure under [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
No new architectural decision or North Star change is needed.

## One ordered execution slice

### Save through one coherent accepted-head persistence handoff

Type: Behavior. Status: abandoned.

Behavior: an author edits an existing root or deeply nested note → publication
uses the already-known new head without rediscovering it → the same complete,
durable history is accepted with fewer unnecessary SQL calls, using less and
clearer production code and without a measured latency regression.

This is one deliverable with one keep/discard decision. Baseline, regression
proof, implementation, refactoring and final comparison belong to it; none is
an independent preparatory story or independently shipped test-only slice.

1. Before production edits, retain the execution base SHA and capture the
   matched baseline using the measurement procedure below. Inspect the actual
   extra ref read in the controller trace. If absent on the execution base,
   stop and abandon rather than searching for another design.
2. Extend the existing controller cost proof for the eliminated read, using
   real persistence and JGit. Establish its pre-change failure, then perform
   the accepted-head handoff/consolidation above. Include necessary creation,
   reset and proposal caller alignment in this same change. Preserve binding
   timestamps and transaction completion. No product-code commit yet.
3. Run required tests and the fresh `dough-post-change-refactor` agent. Review
   the aggregate affected implementation for clearer ownership, fewer duplicate
   steps and strictly fewer production lines. Reject transferred complexity,
   cosmetic shortening or removed test coverage. Recheck after final formatting.
4. Run the matched candidate measurements. Retain only if every gate passes;
   otherwise discard only attempt-owned code/test changes and close the story
   as abandoned, with a brief result and no replacement backlog item.

Sizing hypothesis: production/cost-test edit and cleanup target 5 minutes;
scrutinize at 5, hard reassessment at 10 minutes of implementation. The explicit
exception is waiting for the mandatory whole backend suite, fixture setup and
bounded matched measurements, which finer code slices cannot shorten. If the
implementation overruns or needs independent concerns, stop this attempt;
do not expand/re-split into a storage redesign. One noise-driven repeat of the
same comparison is permitted; no repeated candidate search. Ordinary correction
of a typo/test harness defect is not a new candidate.

## Proof owned by this slice

All paths below are under `backend/src/test/java/com/odde/donut/`.
These are inspected existing proof locations, not tests run during preparation.

| Promise | Setup, trigger and observable proof |
|---|---|
| Less unnecessary SQL, including root/depth-12 fresh-context saves | Extend `controllers/NotebookGitWebContentSaveCostControllerTest` and its existing `CostTestSupport`; observe actual executions around `TextContentController.updateNoteContent`, excluding setup and assertion reads. Verify one fewer post-append ref SELECT and lower total Git executions against the matched baseline; do not replace all-save counts with a narrower window. |
| No whole-notebook traversal or payload load | Retain cost tests for 40 versus 3,000 unrelated folders, ancestor width and unrelated attachments; tree fetches remain bounded by depth. Do not claim constant bytes when ancestor width grows. |
| Correct content, parent, author, timestamp and learning identity | `NotebookGitWebContentSaveControllerTest.savesARootNoteAsOneAcceptedRevisionWithoutChangingItsLearningIdentity` reads the returned/shown note, tracker, downloaded native commit and stored binding. |
| No-op produces no objects/head change | `NotebookGitWebContentSaveCostControllerTest.unchangedContentSaveDoesNotInsertGitObjectsOrAdvanceTheRef` plus canonical no-op cases in `NotebookGitWebContentSaveControllerTest`. |
| Late failure rolls back projection and native objects | Both tests in `NotebookGitWebContentSaveAtomicControllerTest` inject failure at the binding-save boundary and read committed state/fresh downloaded history. If consolidation relocates that seam, keep injection after native writes and before commit; failure before the changed operation would not prove rollback. |
| Proposal head/bytes/identity preserved by the shared store | `NotebookGitExistingNoteBatchPublicationControllerTest.publishesEditsOnlyRevisionOnOriginalLearnedNotesAndMakesItDownloadable` compares submitted and downloaded head/tree/parent and learning data. |
| Creation/reset still copies objects after binding creation | Existing notebook creation through the controller test fixtures plus `NotebookGitHistoryResetControllerTest.resetRestartsHistoryFromTheCurrentNotebookSoAPlainEditPublishesAgain`; add a focused public notebook-creation observation only if existing fixture/controller coverage leaves initial durable head/objects unobserved. |
| Shared web operations and files preserved | Existing derived-tree/folder oracle, attachment publication and web move controller tests run in the full backend suite; compare native tree with full assembly, exact file bytes and preserved identities. |
| Live refs and native transaction visibility | `services/notebookGit/NotebookGitJdbcObjectStoreTest` close/reopen/append/export and aborted-transaction cases. Keep JGit ref update/CAS code unchanged; existing stale proposal refusal remains required. |
| Cleaner design and fewer production lines | Fresh refactor review of aggregate diff versus execution base; list removed duplication and count all affected production files with `git diff --numstat <base> -- backend/src/main`. Include new/untracked files before counting. Report tests/support separately and inspect the diff to exclude cosmetic/comment-only reductions. |
| No latency regression | Matched baseline/candidate procedure below, evaluated after final code/refactor changes. An unsupported or inconclusive claim fails acceptance. |

## Matched measurement procedure

Reuse `NotebookGitWebContentSaveCostRepresentativeExperimentTest`,
`NotebookGitWebContentSaveCostTestSupport`, `SqlStatementCallLog`, and
`CommittedTransactionTestSupport`; do not build a profiling framework.
Before changing production, make only the small test-harness adjustments
necessary for identical baseline/candidate selection and retain them in both:

- Root and depth 12 on the approximately 4,000-folder/11,000-note shape;
  explicitly associate the root note with the measured notebook (a null
  folder alone is insufficient). Include unchanged attachment content.
- Clear the persistence context before each sample. Keep first-save samples
  visible separately from subsequent saves. This is application-context cold,
  not a cold MySQL buffer pool, process restart, or production measurement.
- Confirm the timed controller invocation owns a real completing transaction;
  if a surrounding test transaction masks commit, use the existing committed
  transaction helper. Keep timer/recorder active through completion; verify
  returned content/history outside the measured window. Keep note reload and
  setup outside it equally for both revisions. This measures server save work,
  not browser/network end-to-end latency.
- Use the same isolated MySQL environment, notebook shape, JVM conditions and
  sample count. Capture two baseline batches before production edits to expose
  ordinary run-to-run variation, then two candidate batches with the same
  setup. Existing five samples per batch are the minimum; retain individual
  timings, median and range, with first saves identified. Do not claim p95
  significance from this small sample.

Literal command for both revisions, from the execution workspace:

```sh
DONUT_MEASURE_REPRESENTATIVE_SAVE_COST=true CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Record SHA/dirty patch identity, engine version/configuration, exact harness
selection, executed command and result alongside the observations. Ensure each
run actually executes the gated experiment rather than reusing cached results.
The inspected linked-worktree route in `scripts/backend-worktree-gradle-route.sh`
adds `--rerun-tasks --no-build-cache --no-daemon`; confirm that route in the
execution output rather than interpreting cached results as fresh observations.
JDBC execution counts are not measurements of network packets/round trips.
No timing assertion enters normal CI, and no production edit/release or manual
browser test is part of this plan.

Accept performance only when both shapes show maintained or improved timings
relative to observed baseline variation, including first-save behavior and
slower samples. A consistent slowdown fails even with fewer queries. If noise
prevents a judgment, allow one matched repeat under the same conditions; if
still inconclusive, abandon. Never treat a broad arbitrary percentage allowance
as proof of no regression. Report the local limits honestly; production speedup
is not established by this comparison.

## Delivery and abandonment

For authorized execution, use `dough-execute-plan` in an owned execution
checkout and prepare its dependencies with `./scripts/run.sh bash scripts/worktree_setup.sh`.
Follow linked-worktree database isolation and the full backend suite, not a
selected-file substitute. Verification time is the explicit sizing exception.
Successful delivery follows Jidoka → fresh refactor agent → API generation if
unexpectedly required → coordinator `./scripts/run.sh pnpm format:changed` once
→ update plan → commit/check-only lint hook → push and asynchronous CI repair.
No API/schema change is expected. Implementers/refactorers do not format or run
standalone changed-file lint. Publish no speculative intermediate implementation.

On failure of any gate, the owner has authorized abandonment: restore only the
attempt-owned implementation and disposable harness edits, preserve unrelated
work, remove SEED-037#story-2 from the active backlog using its maintenance
workflow, and retain a short failed-hypothesis/evidence note in this seed.
Do not mark the optimization delivered, retain partial optimizations, defer the
story or create a successor automatically. Failed attempt history may be cleaned
after its concise lesson is captured; the failed story-3 context remains intact.

## Preparation review

One coherent attempt, no independent Structure slice or further refinement needed; no blocking product decision and no preparation runtime claims. Readiness is in the seed.
Preparation provenance is retained in `2604bfcfee`: session-created `/Users/terryyin/.codex/worktrees/simplify-note-save-planning/doughnut`, branch `codex/simplify-note-save-planning`, base `8c96f96894c4a8efd5923c5ae7bd4a334853af3c`; origin/integration `/Users/terryyin/git/doughnut`, publication target `origin/main`. The preparation has since been published.

## Execution context (2026-09-23)

User invoked dough-execute-plan 017. Story Branch Mode; no replanning or
fallback candidate is authorized by this plan's stopping rule.
Session-created execution checkout:
`/Users/terryyin/.codex/worktrees/simplify-note-save-publication/doughnut`,
branch `codex/simplify-note-save-publication`, starting revision
`2604bfcfee8b34e346be2acecbba2a006e01fcff`.
Originating/integration checkout: `/Users/terryyin/git/doughnut`.
Claim published and confirmed on `origin/main`:
`596ad1f7e4cde58b505404bae9ab15bccc40abcc`.
Implementation destination: `origin/codex/simplify-note-save-publication`.
Default checkout refresh deferred: no exclusive ownership established; clean
main at `33939aa45a` preserved. Claim CI coverage: unobserved (Story Branch).
Worktree setup succeeded using `./scripts/run.sh bash scripts/worktree_setup.sh`;
locked dependencies installed, and the claim's check-only lint hook passed.
CI source: GitHub Actions `ci.yml`, display name `donut CI` (push all branches).
Execution observer: coordinator `root-017`, functions cell `20`, PTY `50444`,
mailbox `/tmp/dough-ci-501/watch-iKcBgO`, PID `21932`; request identity verified
against this execution checkout and `codex/simplify-note-save-publication`.
No implementation revision was published. Branch observer stopped with no unread events; PID exit confirmed. Abandonment closure target: `origin/main`, under the plan's authorized closeout disposition.

### Attempt evidence and disposition

- Baseline production SHA: `596ad1f7e4cde58b505404bae9ab15bccc40abcc` (same production as base). Evidence: `/tmp/donut-017-evidence/`; baseline-harness.patch SHA-256 `882746431b776a0fd2400681bbb402e274b8b1091f0adb3234d351984c76c903`.
- Baseline command, twice: `DONUT_MEASURE_REPRESENTATIVE_SAVE_COST=true CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Both full suites passed, six tasks executed. Route confirmed `--rerun-tasks --no-build-cache --no-daemon` in route-processes.txt.
- MySQL 8.4.11, port 3309, schema `doughnut_wt_ce756949e0f540b58e4167743c40394e_test`, connection isolation 4/autoCommit true outside controller. NOT_SUPPORTED test base; controller's SERIALIZABLE service completes its transaction within timing. Fresh context/note reload before timer; content and downloaded parent checks afterward.
- Both shapes: 4,000 folders, 11,000 notes, unchanged attachment. Root explicitly associated with measured notebook. Initial builder error (notebook plus folder forbidden) corrected before valid baselines, with no production edits.
- Baseline 1 root ms `[18,20,17,20,23]` (median20, range17–23); depth12 `[53,45,44,47,43]` (median45, range43–53). First entry is first-save timing.
- Baseline 2 root ms `[14,13,22,13,12]` (median13, range12–22); depth12 `[76,71,69,69,73]` (median71, range69–76). First entry is first-save timing.
- Both baseline JDBC totals: root `[25,26,27,27,27]`, depth12 `[60,61,62,62,62]`. Raw first-save trace proves redundant accepted-head SELECT after native CAS.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`: regression red in both root/depth12 cases (post-append reads expected0, actual1), then candidate green: 2,581 tests, zero failures/errors, two opt-in skips. Real controller/JDBC proof observed zero post-append reads and one preserved live ref read; canonical content/history/identity, no-op, committed rollback, creation/reset, proposal and native-ref tests remained green.
- Fresh independent refactor: no changes needed, accepted proof unchanged. `./scripts/run.sh pnpm format:changed` passed once. After formatting, four production files total 512→498 physical lines but 397→397 nonblank/noncomment lines (per-file deltas +1,−13,+11,+1).
- Final independent review rejected the size gate: all net physical-line savings were comments/blank lines. Genuine statement consolidation cannot replace the agreed size metric after observing the result. No cosmetic shortening or alternate candidate was attempted.
- The plan's authorized abandonment applies. Restore only attempt-owned production/test/harness edits, remove SEED-037#story-2 from active backlog, retain seed lesson and this existing plan as evidence. Performance remains unaccepted; no replacement story, no product-delivery or successful-optimization claim.
- Candidate batch 1 already running at rejection completed successfully before restoration: root `[19,17,19,19,17]` ms (median19, range17–19), depth12 `[78,89,71,74,73]` (median74, range71–89); first entry is first-save timing. JDBC totals root `[24,25,26,26,26]`, depth12 `[59,60,61,61,61]`: one fewer execution throughout. No second candidate batch was run after the decisive size failure; these timings do not establish performance acceptance.
- All seven attempt-owned production/test files were restored to the published claim revision. Only this plan, seed lesson and backlog removal remain; the discarded patch/logs are local evidence. The slice is abandoned, not delivered.
- Closeout observer: coordinator `root-017-closure`, target `origin/main`, cell `60`, PTY `71442`, mailbox `/tmp/dough-ci-501/watch-g9hLWU`, PID `73104`; verified same execution checkout. No implementation was published to the execution branch.
