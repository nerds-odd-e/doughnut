# Receive one added note beside a local edit

## Source

[SEED-009 story 16](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-16),
item six in the product backlog. Status: planned. Planning only is authorized.

## Goal and scope

Receive one web-created ordinary note while retaining one committed local
refinement, then explicitly publish the refinement on its original identity.

- Bound, clean `main`; exactly one unpublished single-parent commit changing
  one existing ordinary note's body/frontmatter at its unchanged path.
- The accepted interval from that commit's parent is exactly one single-parent
  commit adding one different ordinary note, with no other tree changes.
  Addition destination is root or an already represented folder; the remote
  projection matches accepted history.
- Pull keeps the added note and local edit, leaves the edit unpublished, and
  preserves accepted commit IDs. Explicit publish retains both note identities
  and their private learning data.
- Exclude dirty worktrees/automatic stash, multiple local commits or edited
  notes, multiple remote commits/additions, addition plus content edits,
  collisions, moves, renames, deletes, README changes, new folders, and drift
  repair. Creation followed by another web save is deliberately excluded.
  Existing content-only rebase and clean fast-forward behavior remain supported.

## Execution context and decisions

- `cli/src/commands/notebook/notebookLocalCandidate.ts` already checks one local
  ordinary-note edit. `notebookAcceptedInterval.ts` currently rejects every
  structural edge. Add a narrow alternative eligibility case there; do not
  globally permit additions inside arbitrary accepted intervals.
- Reuse `notebookPull.ts` staging/readiness/native rebase and
  `notebookPublishAncestry.ts` direct-child publication. No new identity mapping,
  Git metadata, command, API, schema, or storage mechanism is needed.
- CLI tests drive `run` with real temporary Git repositories and mocked network
  bundles. Extend the existing rebase/structural-history suites through
  `cli/tests/notebookPull.test.ts`. Replace the plain-addition rejection case;
  retain rejection cases for other structures.
- Publication evidence starts from
  `NotebookGitLocalContentOverWebEditPublicationControllerTest` and
  `NotebookGitWebCreatedNotePublicationControllerTest`. Reuse their committed
  fixture transactions and proposal-bundle helpers. Their current assertions
  do not directly prove both learned identities across this exact addition case.
- Reuse existing installed-CLI steps in
  `e2e_test/features/cli/cli_notebook_web_created_note.feature` and the rebase
  assertions used by `cli_notebook_clone.feature`. The current web-created-note
  feature is not tagged `@ignore`; inspect actual tags when running it.
- Follow [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for Portable paths/files and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  for actionable refusal without speculative recovery. ADR 0002 remains Proposed.
- No new transaction/storage assumption: reuse existing committed fixtures and
  ordinary publication. No storage experiment is warranted. No open questions.

## Outside-in proof and ownership

| Promise / example | Owner | Observation |
|---|---|---|
| Local Alpha edit, accepted Beta addition → pull retains both | 1 | Real CLI `run`: file contents; one unpublished child whose parent is the unchanged accepted SHA; original local commit recoverable |
| Root and already represented nested destination; body/frontmatter local edit | 1 | Root canonical case and nested/frontmatter variant assert only their distinct result |
| Pull never publishes | 1, confirmed by 3 | Only bundle download at CLI boundary; Donut Alpha unchanged before explicit publish |
| Only the stated accepted interval is newly eligible | 1 | Addition with another change, two additions, creation then save, and new-folder cases refuse; checkout state retained |
| Dirty/local-multiple/structural work stays rejected; content-only rebase and fast-forward stay supported | 1 | Existing readiness, local-history, structural-history, rebase, and accepted-history tests remain green |
| Explicit publication updates original Alpha and preserves Beta and both learning histories | 2 | Controller publication of a one-child content proposal on the creation head; original IDs, contents, tracker ownership and learning values retained |
| Actual web creation → installed CLI pull → explicit publish works | 3 | Web-created Beta received, Alpha still unchanged remotely until publish, then Alpha updated and Beta retained; accepted parent SHA preserved |

## Ordered slices

### 1. Pull one addition while retaining one unpublished edit

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookClone.test.ts` (74 tests)

Behavior: One clean bound checkout has one existing-note content commit and
accepted main has one different-note addition → run notebook pull → receive the
addition beneath the retained unpublished edit, with all other structural
intervals still refused.

Narrow eligibility in `notebookAcceptedInterval.ts` for one single-parent,
one-ordinary-file addition at root or an already represented folder. Root and
nested/frontmatter success cases live in `notebookPull.addition.suite.ts`;
near-boundary refusals remain in the structural-history suite. Shared pull
guidance updated.

### 2. Publish the retained edit without changing either learned identity

Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Behavior: Accepted creation of Beta follows the shared base; both Alpha and
Beta have private learning state; a direct-child proposal changes only Alpha →
publish through the controller → original Alpha receives the edit and both
notes retain their identities and learning state.

Add one focused characterization scenario beside the existing local-content
publication tests. Create Beta through the ordinary creation controller, attach
trackers through the existing committed-fixture helper without another Portable
save, and publish using the creation head. Assert Alpha's content and both
original note/tracker associations and learning values; Beta's content remains
unchanged. This proves the existing publication behavior for the newly reachable
case; no new backend production behavior is expected. A failure calls for
diagnosis within the story, not a speculative identity redesign.

Sizing hypothesis: about five minutes of fixture adaptation and cleanup, medium
confidence. Backend rules require all backend unit tests; their runtime is an
explicit sizing exception, not a reason to split this single proof loop.

### 3. Complete the web-capture and local-refinement journey

Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`

Behavior: Clone and commit an edit to Recipes/Pasta, then create a title-only
Shopping list on the web → installed CLI pull followed by explicit publish →
the web capture is retained and Donut's original Pasta receives the local text.

Add one scenario using existing creation, commit, pull, rebase, publish and note
content steps. Before publication observe unchanged remote Pasta, the received
Shopping list, and the retained local patch over the creation head. After
publication observe updated Pasta and unchanged Shopping list. Do not add a web
body save after creation: that would test excluded multi-commit receipt. Reuse
the identity proof from slice 2 rather than building another inspection harness.

Sizing hypothesis: about five minutes authoring and cleanup plus the focused
Cypress run, medium confidence. Its runtime is an explicit exception if it alone
exceeds the target. No new PTY/browser harness or manual testing is planned.

## Delivery and current evidence

Slice 1 delivered: pull rebases one unpublished ordinary-note edit over one
eligible accepted addition; other structural intervals still refuse. Next:
slice 2 publication characterization. Do not change the unrelated
browser-worktree plan or backlog order. After all slices, reduce story
refinement to goal/scope and remove spent execution history.

## In-place refinement review

| Leaf | Classification | Sizing and stopping judgment |
|---|---|---|
| 1 | Ready | One eligibility decision, one CLI proof loop. Root/nested cases and refusal rows exercise the same decision. Reuse existing fixture/state helpers; no preliminary framework. The 5–8 minute estimate warrants the five-minute check but reveals no separate preparatory beat. |
| 2 | Ready | One controller publication scenario using established creation/proposal/committed-transaction helpers. Full backend test runtime is the stated exception. |
| 3 | Ready | One existing-step E2E scenario proving explicit publication after receipt. Identity coverage belongs to 2; no duplicate identity harness. Focused Cypress runtime is the stated exception. |

No leaves replaced: splitting these by helper, test, or layer would introduce
preparation-only boundaries. All promise ownership remains in the table above;
there is no completed execution evidence to migrate. Confirmed pull suites are
registered through one test entrypoint because their temporary-directory leak
checks share a namespace: keep added cases within that registration instead of
introducing another worker. This keeps slice 1's proof loop and sizing bounded.

Ready for direct execution when requested. Scope and sibling order are unchanged;
there are no unresolved decisions or unaccounted runtime exceptions. Estimates
include local cleanup and remain hypotheses, not execution-time guarantees.
