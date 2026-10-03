# Discovery execution context

**Identity:** SEED-066#discover-voice-input-problems
**Plan:** [Ordered slices and gates](PLAN.md).

- Execution authorized 2026-10-03 through the established start: story-branch
  mode, publisher `dashboard-territory.local-doughnut`, agent Maki-chan, owned
  workspace `/Users/terryyin/git/doughnut/.worktrees/discover-voice-input-problems-through-manual-tes`,
  branch `codex/discover-voice-input-problems-through-manual-tes`. The workspace
  creation ref names this story. Starting revision is
  `e088c83507835924b796ac628be7ae5dd414f15d`; accepted claim and first delivery
  base are `f2e4076dd720706fec888074b4919ec2149dc7a9`, confirmed on both
  `origin/main` and the remote story branch. Integration checkout is
  `/Users/terryyin/git/doughnut`; it hosts the reused app and is not this
  execution's write location. Increments target the remote story branch.
- Exploration budget starts at 2026-10-03 01:33:26 UTC (09:33:26 Singapore).
  Checkout setup `./scripts/run.sh bash scripts/worktree_setup.sh` completed
  with the locked workspace dependencies already up to date; applicable command
  `./scripts/run.sh bash scripts/check_diff_whitespace.sh` passed. The shared
  pre-commit hook is check-only `./scripts/run.sh pnpm lint:changed`.
  Existing planning authority permits bounded refinement without widening scope.
  The established claim's trunk CI is unobserved; managed increment delivery
  owns observation of the story branch. A bounded slice-1 retry uses the known
  `http://localhost:5175/` origin: `127.0.0.1` sign-in succeeds but Note and Add
  New Notebook activation stays unchanged in two tabs, whereas the existing
  localhost session navigates via Note. This does not establish the cause;
  the retry must verify the documented account and recording route there.

## Current observation

Slice 1 published as `2eec224d133f9bfa081e5918a55c46f4de3827a1` on the remote
story branch. Managed delivery reported unobserved CI: it did not bind the
Codex yielded-cell bridge and started no observer. The verified GitHub selector
is `ci.yml` (pushes on all branches; `.planning/**` is ignored).
Slice 2 published as `be8f93f99b8337e82bc4baa5cecf4a202b1f8337`; its
[sustained evidence](../../seeds/SEED-066-voice-input-sustained-evidence.md)
records completed orchard-content loss while the prior paragraph survived.
Slice 3 published as `51331973c78a7040a05d664dba3e527092f6e61d`: two finished
paragraphs preserved with a distinct addition after reload. Slice 4 records
real pending-result loss of visible typing; an already-saved edit race remains
unobserved. Its later paste was after settlement. Chosen title changed twice.
Slice 4 published as `c66d7874a6a02c7b0f19325f48387943705f3953`.
Slice 5 published as `f968a52233f4682ae97e1d405817824e421f6ebc` using matching
consecutive-session proof below. Slice 6 observed live pending navigation:
destination `13727` survived, both results persisted to source `13726`, whose
first paragraph became `...uesday.` after reload. Cause remains unproved.
Slice 6 published as `e4b434ed41069e5a0774e506152ce49debc5fd2c`.
Slice 7 published as `a2f401883067fd244837ed12ea1a1603e5a623d5` using existing
title sequences below. Slice 8 inspected title editing, Audio tools/Advanced
Options, and New note controls; no explicit voice-title route was discovered.
Slice 8 published as `0aac20658726185004897491063cbb9bfe142af8`.
OS dictation, extensions and processing-instruction routes remain unobserved.
Slice 9's allowed skip and remaining reserve are recorded below.
Independent refactor found the records clean, whitespace and formatting passed.
Publication receipts stay in the execution conversation before each next slice.
Hardware capture, permission and frequency remain uncovered.

## Repeated-session proof mapping

Slice 5 reuses two completed, distinguishable sessions on the same disposable
note `13726` in notebook `23`, Chrome/macOS, `manual`, localhost Development:

- [First session and saved comparison](../../seeds/SEED-066-voice-input-manual-evidence.md#existing-content-preservation):
  6.2826667 s lighthouse/oranges input, content PATCH at 01:56:33.252 UTC,
  Stop after settlement at 01:56:40.251, then reload around 01:56:50 confirmed
  both original paragraphs unchanged and the distinct addition exactly once.
- [Next session and saved comparison](../../seeds/SEED-066-voice-input-manual-evidence.md#manual-edit-preservation-during-processing):
  its recorded starting body is exactly that saved body, with no intervening
  body reset. Record Audio started the distinct 29.168 s orchard/book/garden/
  meeting passage at 02:02:23.592. The linked
  [sustained input](../../seeds/SEED-066-voice-input-sustained-evidence.md#sustained-speech-and-revision-evidence)
  retains its literal audio-generation command and capture setup. Stop was
  reached after audio settlement; reload around 02:03:44 confirmed both
  originals and the prior lighthouse addition, plus the later book fragment
  and meeting sentences. New orchard content was absent. A manual sentence
  pasted after settlement also persisted, prefixed to the lighthouse paragraph;
  that edit is distinguished from the lost typing during the pending result.

This establishes accumulation and preservation of the prior distinct addition
across the observed start/stop sessions, alongside loss within the later
recording. The evidence identifies no duplication or stale result across these
two sessions; it establishes neither defect frequency nor a general preservation
guarantee. Capture was paced synthetic MediaStream input through the actual
worklet and real services; hardware/permission behavior remains uncovered.
The two reports retain network/DOM timing, reload proof, revision limitations,
and cleanup. No new probe, fixture, tab, or product change was needed for this
mapping; the disposable note remains for later slices. Review took about one
minute, charged to the existing exploration budget.

## Automatic-title proof mapping

Slice 7 reuses accepted Chrome/macOS `manual` observations on disposable note
`13726`, notebook `23`, real-service localhost Development. Both required title
starting states and a repeat session are covered:

- [Initially Untitled and repeat recording](../../seeds/SEED-066-voice-input-manual-evidence.md#recording-route-and-short-speech-baseline):
  capture began at 01:40:26.708 UTC; `Untitled` became “Sensory Notes on Food
  and Drink” at 01:40:56.033. A second recording began at 01:41:05.341 and
  changed it to “Sensory Impressions of Foods and Drinks” at 01:41:33.405,
  although the final body was identical. Reload confirmed the second title.
- [User-chosen title during a later recording](../../seeds/SEED-066-voice-input-manual-evidence.md#manual-edit-preservation-during-processing):
  the author set “Author chosen preservation title” through the title UI before
  capture at 02:02:23.592. It became “Example Paragraphs for Preservation” at
  02:02:41.563, then “Sample Paragraphs for Preservation” at 02:03:02.520.
  Reload around 02:03:44 confirmed the saved result. These changes reproduce
  unwanted ongoing title updates and loss of the chosen title's control, as
  described in the [owner's title expectations](../../seeds/SEED-066-voice-input.md#title-behavior).
- [Navigation observation](../../seeds/SEED-066-voice-input-navigation-evidence.md#late-result-after-supported-navigation)
  retains the variability: two source suggest-title requests returned HTTP 200,
  but no title PATCH or visible change was observed; both titles survived reload.
  This does not establish a general title-preservation guarantee or its cause.

The linked reports retain input durations, body/title timing separation, capture
labels, revision limits and cleanup. No new recording or UI probe was needed;
review and mapping took about one minute. Hardware capture remains uncovered,
and disabling automatic titles versus delayed one-time generation remains the
owner's later decision. No product source or disposable-note state changed.

## Failure-probe budget disposition

Slice 9 uses the plan's explicit permission to skip this secondary probe to
confirm a higher-value finding. At 02:26:46 UTC, about 6 minutes 40 seconds
remained before the 02:33:26 mission deadline. Preserve that reserve for
independent confirmation of the saved source-paragraph loss and final evidence
review and cleanup in slice 10. No new browser or service probe was performed.

Denied permission, device failure, recording interruption, and service-failure
feedback and recovery remain unobserved. The paced synthetic MediaStream route
used in earlier observations does not establish genuine hardware or browser
permission behavior. No failure-feedback or recovery claim, or absence of a
defect, follows from this disposition. Discovery scope is unchanged; recording
this permitted gap took less than one minute and changed no app state.
