# Receive a web note move locally without losing learning history

Status: planned
Source: [SEED-009 story 25](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-25)
Scope confirmed for planning: 2026-09-15

## Outcome and scope

An owner moves an ordinary note between existing represented locations in one
synchronized Git-backed notebook, pulls the clean local checkout to its new
path, edits its body there, and publishes into the same note with learning
history intact. Root, nested destinations, existing empty folders and ordinary
reference rewrites within this notebook are variations of this outcome.

Preserve authored bytes except for existing move reference rewrites; retain
note/folder identities, tracker associations, recall records, scheduling state
and independent removed-from-tracking preferences. Accepted commits append;
no-op moves need no commit. Retain existing authorization and destination
collision rules. Failed moves cannot leave partially advanced accepted history.
Existing unsynchronized web content must not be overwritten or quietly adopted.

Local editing starts after pull. No new divergent reconciliation, local move
inference, destination creation, subtree moves, cross-notebook transfer or
synchronization of other notebooks' referrers. Broader organization belongs to
[story 40](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-40);
trash remains with story 28, and performance with its existing story. Deferrals
do not justify rejecting or breaking naturally supported existing behavior.

## Existing solution and architectural direction

Carry forward story 25's PFE findings:

- `RelationController` already owns the public move paths. `NoteMotionService`
  checks placement and mutates the same row; `WikiLinkRelocationRewrite` applies
  the existing reference policy. `WebNoteEditService` owns accepted-history
  coordination for note content/title edits. Reuse that locked mutation and
  snapshot responsibility for eligible ordinary location edits, keeping
  placement and link mechanics with their existing owners. Do not copy the
  history algorithm into each endpoint or add a persisted path/identity map.
- Authorization, reference capture before placement, placement, reference
  rewrite and accepted snapshot must form one coherent web operation. Reload
  the note and destination by ID in the owning transaction; do not mutate a
  stale request-bound entity after loading locked state. Account for the
  controller's existing outer transaction: annotating a nested service alone
  does not establish a new transaction isolation boundary. Preserve current
  non-Git and cross-notebook behavior on their existing paths.
- `NoteMotionService` is also called by note trash and restoration through
  `NoteController`. Adding automatic Git snapshots to every low-level placement
  would accidentally broaden the story. Keep history coordination at the
  ordinary web-note edit boundary; preserve these other callers.
- Use `AcceptedSnapshotPersistence` and `PortableTreeSnapshot` for one complete
  post-move tree including in-notebook referrers and `.keep` changes. Folder IDs
  and hierarchy do not change when an ordinary note changes parent.
- CLI `notebookPull.ts` already fast-forwards accepted trees. Publication applies
  same-path modifications to the existing live note. No CLI protocol, schema,
  generated API signature or new identity inference is anticipated.

Follow [One final publication result](../../NORTH-STAR.md#one-final-publication-result)
for the subsequent local publication. It does not require replaying web moves
through proposal inference. Existing structure supports the web extension; no
new North Star topic is needed.

Relevant Accepted ADRs: [0001 domain language](../../../docs/adrs/0001-ubiquitous-language.md)
distinguishes portable paths from note identity;
[0003 scheduling](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
owns recall records/current state;
[0004 portable Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
owns bytes, links and `.keep`;
[0006 failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
allows exceptions to propagate with transaction rollback; and
[0007 isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
keeps tests in disposable environments. ADR 0002 remains Proposed. No conflict
or new storage-engine assumption was identified.

## Proof strategy and retained evidence

Research at `3ad1d460d1`, recorded in the source story, established:

- Six CLI fast-forward tests passed, including a constructed accepted move and
  later edit. This proves receipt only; the test supplies the accepted bundle.
- All 2,406 backend tests passed in 1m 14s including a temporary diagnostic.
  The real move endpoint retained the note/tracker but left the downloaded tree
  at the old path; stale publication was rejected. The diagnostic was removed.
  This is evidence of the gap, not completion of any slice.

Controller proofs use real persisted fixtures, `NotebookGitBundleControllerTestBase`
and `GitBundleTestReader`. Seed accepted state only before the web move; never
resnapshot after it to make the test pass. For continuation, derive proposal C
from the actual post-move bundle B and call the real publication controller.
Observe retained note/tracker/recall IDs and scheduling/preferences after C,
not merely before movement. Existing web-rename and private-association tests
provide fixture patterns; do not duplicate assertions for every path variant.

E2E uses the existing web Move interaction and installed CLI clone/pull/commit/
publish helpers. Add `cli_notebook_web_note_moves.feature` and register it with
the existing active CLI spec selection for isolated worktrees. Reuse
`wiki_link.ts` move steps, `noteTargetSearchDialog`, `cli_notebook_clone.ts`,
and checkout observations. The first journey must show changed content on the
original note route after actual CLI publication; capture its ID before moving.
Detailed persisted learning invariants belong to the controller round trip.
Use named router helpers per ADR 0005 for any new E2E navigation.

Execution commands:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_note_moves.feature
```

Run the full backend suite for backend changes. Run the focused E2E feature
when its behavior changes. Reuse existing CLI fast-forward evidence unless CLI
code changes or a new receipt failure invalidates it. No product tests run in
this planning turn; all new acceptance proof is pending.

## Ordered slices

### 1. Share the existing accepted web-note edit boundary
Type: Structure
Status: planned

Structure: Make the existing locked note-edit/mutation/snapshot operation
reusable by the ordinary location edit in the immediately following slice.
Keep current content/title entry points on that same operation. Expose or
relocate only the current internal envelope as needed; do not create a generic
event framework, extra state store, or public callback API. Preserve the
existing transaction owner, authorization, projection-drift fallback, no-op
handling and append policy. This slice introduces no Git move behavior.

Proof: Run the full backend suite, retaining the existing web content/title
history, no-op, rejection, drift and queued-writer observations. Inspect callers
to confirm both existing save entry points still use the one shared operation.
No new E2E observation is required because public behavior is unchanged.

Safe stop: existing content/title editing remains unchanged. The only added
structure directly enables slice 2's ordinary same-notebook movement.

### 2. Move a learned note between folders and continue local editing
Type: Behavior
Status: planned

Behavior: With synchronized A, a learned `Biology/Cells.md` and represented
`Study` folder, use web Move. It appends B; a clean clone at A pulls the exact
file at `Study/Cells.md` with no old path. Edit its body locally, commit C and
publish. The original Donut note shows the new content with its learning state
retained. Give both folders Readmes in this first example to isolate placement.

Use the shared accepted web-edit coordination to encompass ordinary note
movement, including existing reference semantics. Use the source/destination
notebook IDs to select eligibility. Do not add fixture-driven constraints on
number of files, history depth, or folder arrangement. Keep existing fallback
behavior when no binding exists or the starting projection already differs.

Proof: Add the controller move/download/publish round trip above. Observe B's
single parent A, the exact moved content and absence of the old path, accepted
C, original note/tracker/recall IDs, due state and tracking preference after C.
Include destination collision and denied access as focused variants observing
unchanged database placement/content and binding. A same-place request leaves
the accepted head unchanged. Reuse the current transaction/concurrency harness
to queue a web save after a move and observe both results in parent order;
preserve existing projection-drift refusal and non-Git behavior.

The matching E2E journey clones before Move, receives B through installed CLI,
commits/publishes C at the new path, and reads the new content on the original
note route. A bundle fixture is not a substitute for web-produced B.

Safe stop: the common folder move/edit loop works; remaining variations are
unproved promises until their slices finish.

### 3. Receive moves between root and existing nested folders
Type: Behavior
Status: planned

Behavior: A root note moves into an existing nested folder, or a folder note
moves to root; pull receives the correct full path and local publication can
continue on the same note.

Route the existing root endpoint variants through the same ordinary location
edit rule. In particular, the UI uses the endpoint with an explicit target
notebook even for a same-notebook move. Do not select behavior by endpoint name.

Proof: Controller data variants cover root-to-nested placement and both root
endpoint spellings. Assert only changed path/parent and accepted-tree deltas;
reuse slice 2's canonical identity shape. Extend the E2E journey with the actual
UI folder-to-root path and publish at that root path. Preserve cross-notebook
endpoint regression tests; cross-notebook synchronization is not added here.

Safe stop: existing root and nested placement uses the same supported loop.

### 4. Receive a linked note move with its in-notebook reference updates
Type: Behavior
Status: planned

Behavior: A referrer in the same notebook contains an exact path to the moved
note in body and frontmatter. Move and pull receive the relocated file and the
ordinary rewritten references together, preserving the visible label.

Keep capture-before-placement and rewrite-after-placement inside the single
accepted mutation. Snapshot the current referrer content, not pre-move rows.
No new reference policy or per-referrer commit sequence is introduced.

Proof: A controller case plus a focused linked-move E2E variant observe
`[[Biology/Cells|shown]]` becoming `[[Study/Cells|shown]]` in both locations in
the same accepted B. Reuse existing tests for unchanged shorthand and unrelated
links; retain them when moving orchestration. Other notebooks may still receive
existing web rewrites, but this slice makes no Git synchronization claim for
them and must not block or discard those existing rewrites.

Safe stop: owners receive a self-consistent linked move within this notebook.

### 5. Preserve existing folders when a moved note changes which are empty
Type: Behavior
Status: planned

Behavior: Biology contains only Cells, and Study is an existing empty folder
represented by `Study/.keep`. Move and pull receive `Biology/.keep` and
`Study/Cells.md`, without `Study/.keep`. Subsequent same-path publication keeps
both original folder identities and the note.

Use the same snapshot rule; do not synthesize placeholder notes/Readmes or
interpret an emptied folder as deletion. No new directory lifecycle is added.

Proof: Controller round trip observes exact B paths and original folder IDs
after C. The E2E empty-folder variant observes the same tracked files after
pull, publishes the body edit, and sees the result in Donut. If existing
snapshot/receipt code already satisfies this variation, add only its missing
behavioral evidence rather than another handler.

Safe stop: all selected path, reference and empty-folder promises are covered.

## Promise ownership

| Promise | Owning slice / observation |
| --- | --- |
| Existing web content/title edit semantics unchanged | 1: existing history/rejection/drift/queued-writer controller proofs |
| Actual web move becomes append-only history | 2: real move endpoint, downloaded B parent A |
| Clean checkout receives exact new path/content | 2: installed pull and exact file list/bytes |
| Local edit after pull updates original note | 2: actual CLI C publication, original note route |
| Learning IDs, recall records, schedule/preferences retained | 2: controller round trip after C |
| Authorization, collision, no-op, coherent serial history | 2: focused controller variants/queued writers |
| Existing non-Git, cross-notebook, trash and drift behavior preserved | 1–2: affected controller regression suites in full backend run |
| Root and nested destination paths, both root endpoints | 3: controller variants and UI-to-CLI round trip |
| Same-notebook body/frontmatter rewrites accompany move | 4: exact referrer bytes in B and pulled checkout |
| Empty source/destination markers and original folder IDs | 5: exact tree and persisted folder identities after C |

## Sizing, delivery and assessment

Target approximately five minutes per leaf including focused work and proof;
scrutinize active work beyond five and finer-decompose beyond ten. Full backend
runtime (research baseline 74 seconds), E2E startup/runtime and required delivery
gates are explicit elapsed-time exceptions; they do not excuse expanding active
implementation. Refined hypotheses: slice 1 is 3–5 active minutes; slice 2 is
5–10 active minutes; slices 3–5 are 5–8 active minutes each, all medium
confidence. The over-five-minute hypotheses are scrutinized: slice 2 now
starts with the reusable transaction envelope and existing Move/CLI helpers;
each later slice is one placement, reference or marker variation. Their
observations remain attached to the behavior they establish. If integration
or fixture changes push active work beyond ten minutes, refine the remaining
work in this plan at that point rather than accepting an unbounded exception.

### Refinement assessment (2026-09-15)

Replaced the original first Behavior slice with Structure 1 followed immediately
by Behavior 2. Retained its full user outcome and mapped all promises to the
new owners; renumbered the three remaining Behaviors without expanding scope.
No execution has started and there is no implementation to park or revert.

| Slice | Assessment | Bounded result |
| --- | --- | --- |
| 1 | Ready | Reuse one existing edit envelope, unchanged public behavior |
| 2 | Ready | One complete folder move/receive/edit/publication loop |
| 3 | Ready | Root/nested placement variants using that same rule |
| 4 | Ready | One linked move received with its in-notebook rewrites |
| 5 | Ready | One move preserving the two existing folders via markers |

Five slices; no story resplit recommended. Ready for direct execution under a
separate execution instruction. Sizing depends on the existing fixture and
transaction reuse identified above; E2E runtime is not yet measured here.
No remaining independent implementation beat or unsupported special-case
model was identified in this assessment. Cumulatively all four Behaviors use
one post-mutation Portable snapshot and existing note identity. Preserve the
limited structural scope if the extraction proves simpler than anticipated;
do not invent infrastructure to fill the slice.

When execution is authorized, follow dough-execute-plan: take the queued entry
at execution start; default Story Branch Mode; Jidoka; fresh
dough-post-change-refactor agent; regenerate the API only if required;
coordinator runs `./scripts/run.sh pnpm format:changed` once per delivery;
update this plan; check-only commit hook; push/CI handling. Retain plan/proof
through retrospective and story wrap-up. No execution checkout or branch has
been created by planning. Backlog entry remains queued; GSD STATE is unchanged.

## Execution evidence

Pending. No slice has been implemented, verified, committed or delivered.
