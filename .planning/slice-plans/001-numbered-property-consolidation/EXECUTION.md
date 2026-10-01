# Execution context

- Workspace: `/Users/terryyin/git/doughnut/.worktrees/existing-numbered-property-keys-become-one-list`
- Branch: `codex/existing-numbered-property-keys-become-one-list`
- Mode: story-branch; remote: `origin`; integration target: `main`.
- Publisher: `dashboard-mac.lan-doughnut`; assigned agent: Rio-chan.
- Starting revision: `a11dfaee415d7fdbaf4bb916f0d125581da37a03`.
- Published claim and initial candidate: `28442ce9d9ea3e50d1f002081a6b93907f0f57f1`.
- Remote execution branch confirmed at the published claim. Claim CI on trunk
  is unobserved; subsequent increments observe the execution branch.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` passed;
  `CURSOR_DEV=true nix develop -c pnpm exec node --version` passed (`v26.10.0`).
- Replanning remains authorized within this plan's scope and sizing rules.
- CI source: GitHub Actions, `nerds-odd-e/doughnut`, workflow `ci.yml`
  (`donut CI`), verified push selector. Managed delivery owns observation setup.
- Slice 1 accepted revision: `2a87cb0e014abfc7cc007af35fbe8bd8c89ed23c`.
- CI observer: `/tmp/dough-ci-501/watch-Z3lvDT`, PID 8731, this checkout and
  execution branch, with exact revision registration. Live Codex notifications
  are unavailable: its stream entry creates another observer rather than
  attaching. Extra mailbox `watch-bX3jiL` was stopped with no unread events;
  use the retained managed observer for the bounded completion operation.

## Slice 2 sizing reassessment

The first attempt became test-ready at approximately nine active minutes, then
acceptance inspection found two correctness gaps. The target was exceeded; the
hard limit was not crossed. All five attempt-owned algorithm/test edits are
safely parked at `/tmp/numbered-property-family-attempt.patch` and restored to
HEAD. Slice 1 and coordinator planning edits remain intact.

`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed on the prototype
(2744 tests, zero failures/errors, two skipped). Ten authored-frontmatter cases
observe scalar/list mappings, order/deduplication, case distinction, url and
structural eligibility, shared scalar meanings, unsupported outcomes and BOM /
CRLF / unrelated source bytes. This is incomplete acceptance evidence: whitespace
keys were incorrectly grouped and large suffixes threw NumberFormatException.

The sizing assumption missed exact authored naming and source normalization
inside reused owners. The corrected slice reuses the compatible prototype and
has one remaining proof loop for those gaps (target five active minutes; complete
suite wait remains excepted). Twelve slices remain in this cumulative design;
no story boundary or ADR change is required. Record the original nine-minute
attempt when reporting continuation; a retry does not erase it. All later slices
are retained because this evidence concerns the common transformation contract.

## Slice 2 accepted proof

Corrected continuation took four active minutes after the recorded nine-minute
attempt. Independent refactoring separated exact family policy from shared YAML
source-edit mechanics, with the public authored-document contract preserved.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after all edits:
2746 tests, zero failures/errors, two skipped. XML for
`NoteContentMarkdownNumberedPropertiesTest` confirms twelve selected cases.

The authored-Markdown fixtures and exact output/focus assertions observe forced
lists, scalar and list-item mappings, stable deduplication and numeric ordering,
missing bases, case/whitespace distinction, suffixes beyond Long range, url and
structural/word eligibility, shared scalar meanings, diagnostic-only unsupported
shapes, repeated-run stability and BOM/CRLF/unrelated source preservation.
`PropertyKeyNaming` retains one suffix representation using BigInteger;
structural consumers trim explicitly. Maps reuse the existing PropertyFocus.
The refactor's full-suite proof also covers existing source-edit callers.
Independent refactor completed; coordinator formatter passed. Migration tracker
and accepted-publication integration remained the original slice 3's obligation,
now split between slices 3 and 4 as recorded below.

## Slice 1 accepted proof

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm backend:verify` passed;
`NotebookGitStartupServicesProbeTest` observes the actual ready-event listener,
Flyway completion, transaction boundaries, live schema and unchanged downloaded
history/native objects. The probe uses a test-only event context with real
service proxies; it establishes service/order usability, not production boot.

## Migration operation sizing reassessment

The original slice 3 attempt stopped at ten active minutes. Its six owned
production/test paths are parked in `/tmp/numbered-property-migration-attempt.patch`
and `/tmp/numbered-property-migration-attempt/`; all product changes were restored
or removed individually. Coordinator cleanup-story/backlog edits are preserved.

`CURSOR_DEV=true nix develop -c pnpm backend:verify` ran 2751 tests with one
failure and two skips. Four diagnostic cases passed (orphan item, zero-item list,
tracked empty value, oversized tracked destination). Canonical proof stopped at
FSRS stability: an unpersisted Java float 2.1112142 was compared with MySQL's
rounded 2.11121. Reloading the committed baseline is the evidenced fixture fix;
later history/index/download assertions remain unaccepted until observed.

The original leaf hid two independently evaluable risk boundaries: whole-operation
preflight refusal and successful learning-preserving accepted publication. Split
it into slices 3 and 4, retaining one migration/mapping owner and the immediate
next behavior. Both reuse the parked work (target five active minutes each; full
suite waits excepted). Thirteen slices result; no source outcome or architecture
change is required. Later slices retain their promises in the same order. The
prior nine-minute transformation attempt and ten-minute operation attempt remain
recorded; subdivision does not erase elapsed work or claim missing proof.

## Slice 3 accepted proof

`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2750 tests,
zero failures/errors, two skipped. All four parameter cases in
`NumberedPropertyMigrationControllerTest.anUnmappableFocusLeavesTheWholeNotebookUnchanged`
passed. Each fixture places a valid learned family before the invalid note.
The trigger is the real migration notebook operation; committedState captures
fresh controller content, persisted tracker identities/focus/type/state/schedule/
FSRS and serialized controller recall histories. Equality after diagnostic plus
unchanged downloaded acceptedHistory proves complete refusal without mutation.
The cases cover orphan items, empty source lists, tracked empty values and
oversized persisted destinations. Independent refactor found no edits; formatter
passed. Continuation took five active minutes after the recorded ten-minute
attempt. Successful transformation/publication remains slice 4's proof obligation.

## Slice 4 accepted proof

`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2751 tests,
zero failures/errors, two skipped; migration XML records five passing cases.
`learnedScalarsBecomeOneAcceptedListKeepingBothLearningIdentities` restores the
canonical journey with a committed-state baseline, correcting the recorded
float precision mismatch. The real notebook operation is followed by controller
content/identity and tracker focus reads, exact serialized histories, schedules,
FSRS/type/state, wiki/property index reads, exact downloaded Markdown/path set,
and one descendant count/parent with Donut System author/message. The fixture
supplies a bound notebook with two learned scalar associations. Independent
refactor found no edits; formatter passed. No production/API changes. Continuation
stayed within five active minutes; prior attempt remains recorded.

## Slice 5 accepted proof

`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2753 tests,
zero failures/errors, two skipped. Both cases in
`NumberedPropertyMigrationListControllerTest` passed. Real migration followed
by fresh committed controller content and exact ID-to-focus maps proves sparse
numeric order, existing-list precedence, stable deduplication, scalar/item
destinations and no tracker cloning. The sparse fixture also proves untracked
url consolidation and structural/word-key preservation. Independent refactor
found no edits; coordinator formatter passed. No production/API changes;
implementation took approximately five active minutes.

## Slice 6 accepted proof

`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2754 tests,
zero failures/errors, two skipped; LearnersControllerTest XML records one pass.
The real operation moves two learners' persisted scalar trackers, including an
inactive one. Fresh committed showMemoryTracker reads under each actual learner
observe original IDs, ownership, new focuses and retained inactive state; the
unrelated focus stays unchanged. First verification failed during fixture setup
on a detached User before migration; reloading it in the fixture transaction
corrected setup. No product defect inferred. Independent refactor found no
edits; formatter passed. Six active minutes including correction, waits exempt;
no production/API changes.

## Slice 7 accepted proof

`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2755 tests,
zero failures/errors, two skipped; TrashControllerTest XML records one pass.
The bound learned-note fixture uses real controller Trash, migration and undo.
Exact downloaded `_trash/Subject.md` contains the consolidated list; fresh
committed controller reads after undo retain note/tracker IDs, final focus,
active learning and exact serialized recall history. Initial verification failed
only on unquoted expected Markdown; correcting it to the shared transformer's
double-quoted items passed full verification. Independent refactor found no
edits; formatter passed. Four active minutes, waits exempt; no production/API
changes or weakened promise.

## Slice 8 accepted proof

`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2757 tests,
zero failures/errors, two skipped, both DuplicatesControllerTest cases pass.
Native MySQL equality drives existing-destination/lowest-ID survivor selection,
including case/accent equivalents and inactive rows. Committed controller state
equality retains survivor learning; separate learners/types remain distinct.
The full live FK closure was rechecked in the current isolated wt schema:
tracker→batch request/log/prompt CASCADE; prompt→conversation SET NULL. Actual
deletion assertions cover every edge and the retained conversation. Later
rollback/retry/startup survivor outcomes are compatible with authorized deletion.
First verification failed on numeric Boolean adaptation and fixture ownership
cleanup; second on Hibernate's null embedded subject after SET NULL. Numeric
adaptation, exact conversation-ID cleanup and corrected observation passed.
No manual DB deletes. Implementation took 9.5 active minutes; hard limit not
crossed. Fresh refactor simplified existence assertions to keep the test at
249 lines and reran the full suite successfully. Formatter passed; no API change.
