# Verify publication receiver content without per-file browser overhead

Status: planned — retrospective correction only; implementation not authorized.
Source: Git revision `e6402aae754201d791fdf18dd2d6e2b6bfc1f65f`, paths
`.planning/quick/106-profile-large-notebook-publication/PLAN.md` and
`.planning/seeds/SEED-018-publish-large-authored-notebooks.md` (story 2).
Review boundary: execution checkout `doughnut-106-profile-large-notebook-publication`,
branch `codex/106-profile-large-notebook-publication`, HEAD `3651153736`.
The original nine slices are complete under the user-approved reduced discovery
scope. This is a separate correction, not reopening large profiling.

## Bounded correction contract

Beneficiary: maintainers running the opt-in publication profiler.
Outcome: the existing public publish/pull scenario verifies every expected received
file and records acceptance without one browser-command chain per file. Retire
the verbose profiling evidence record while keeping a concise, usable baseline
for the pending optimization.

Finding: `e2e_test/start/pageObjects/cli/notebookPublicationProfile.ts:expectReceived`
still iterates through `expectCheckoutFileAt` for every proposal file. In the second
large capture, HTTP completed at 13:40:01 UTC and receiver verification was still
running when the user requested a safe stop. Independent read-only verification
of the already pulled receiver checked all 10,000 bytes/head/tree/clean status in
2.5 seconds. The workaround is retained outside Git; documented advice does not
correct the reusable runner. No product-publication regression is attributed.

Evidence: `docs/notebook-publication-profiling.md#baseline-captures` and persistent
`2026-09-11T12-37-08.116Z/{receiver-driver-observation.json,independent-verification.json,verify-received-checkout.py}`
under the documented publication-profiles directory.

Preserve all expected authored bytes, clean accepted head/tree observations,
public HTTP or installed-CLI submission and receiver pull, isolated ownership,
recording stop before verification, and incomplete outcome on verification failure.
Preserve small late rejection and note/learning/binding state observations.
Exclude product optimization, new large publication runs, production timeout
changes, blanket E2E deletion, and a general benchmark framework.

## Current architecture and selected reuse

The benchmark reuses the real public publication operation; Node tasks own Git,
filesystem observation and capture-result writing. The browser owns the scenario.
Keep that division: move bulk byte comparison to the existing Node task boundary,
using the proposal-file data already held by the scenario. Reuse existing checkout
state observation (`cliE2eNotebookCloneTasks.ts:readCliNotebookCheckoutState`) and
receiver pull/head assertions; do not implement another acceptance protocol.
`notebookCloneCheckoutReceiver.ts:expectCheckoutFileAt` remains useful for ordinary
small user journeys. No evidence supports rewriting its unrelated callers.
The capture-local Python comparator supplies behavioral reference, not a new
runtime dependency; check all files, not a random sample or count alone.

This follows the near-future direction in PRODUCT-BACKLOG.md: owned worktree
verification without interfering with other tasks. Relevant Accepted ADRs are
0007 (isolation), 0006 (visible failures), and 0004 (lossless authored content).
No domain or cross-production-subsystem change is needed. Proposed ADR 0002 is
not treated as a binding implementation contract.

## Ordered slices

### 1. Verify the publicly received proposal in one bulk observation
Type: Behavior
Status: done
Proof: Given a publicly pulled receiver, the existing opt-in accepted scenario
checks every expected file and records accepted outcome using one bulk filesystem
observation. A missing or changed file reports its path and prevents accepted
outcome. Clean/head verification and the rejection scenario retain their meaning.

First establish file-observation proof with real temporary files at the proposed
Node verifier boundary: matching multiple folders and exact bytes; missing file;
changed content (including a newline difference). Establish this proof before
removing the per-file Cypress assertions. Use a filesystem-only 10,000-file fixture
if needed to demonstrate bulk verification cost; do not publish that workload.
Then integrate into the existing scenario and preserve final acceptance ordering.

Planned focused unit command (new test, not yet present):
`CURSOR_DEV=true nix develop -c node --test scripts/profiling/verify-publication-receiver.test.mjs`.
The helper may live with the profiling tasks; keep one owner for the comparison.
Adapt this command to the selected actual module without inventing a parallel runner.

Existing integration proof, with the default small fixture only:
`PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'`.
Preserve incomplete results and do not reset a server request still running after
a deadline. No broad backend/frontend/CLI/MCP suite run is required for this local
verification seam unless implementation reveals a changed consumer boundary.

Estimate: 5–10 minutes active implementation and focused proof; scrutinize at 5,
stop and finer-decompose above 10 active minutes. Required test runtime is exempt.
Safe stop: cheap complete receiver proof, unchanged publication and rollback behavior.
Concern: ensure the result is marked accepted only after the bulk check succeeds;
a fast comparison that bypasses that ordering would weaken proof.

Done: `scripts/profiling/verify-publication-receiver.mjs` exports
`findPublicationReceiverMismatch`, proven with real temporary files at
`scripts/profiling/verify-publication-receiver.test.mjs`
(`CURSOR_DEV=true nix develop -c node --test scripts/profiling/verify-publication-receiver.test.mjs`,
4/4 passing) covering multi-folder exact-byte matches, a missing file by path,
changed content by path, and a newline-only difference by path. Integrated as
one new Cypress task `verifyPublicationReceiverFiles`
(`e2e_test/config/notebookPublicationProfile.ts`), called once from
`expectReceived()` in
`e2e_test/start/pageObjects/cli/notebookPublicationProfile.ts` in place of the
per-file `expectCheckoutFileAt` loop; a thrown mismatch stops the chain before
`confirmNotebookPublicationProfile`, preserving accept-only-after-verification
ordering. `notebookCloneCheckoutReceiver.ts:expectCheckoutFileAt` and its other
callers are unchanged and still have live callers elsewhere.
Learning: the existing-integration Cypress command above could not be run from
this execution's Git worktree — `pnpm sut`'s isolated-SUT bring-up builds a
Unix-domain socket path (`<checkoutRoot>/.sut.local.lock/owner.sock`) that
exceeds macOS's ~104-byte `sun_path` limit once the checkout sits this deep
under `.claude/worktrees/...`; it fails during generic SUT ownership bring-up
before any publication-profile-specific code runs, so it is unrelated to this
slice's change. Recommend running that command from a shallower checkout to
obtain the live end-to-end confirmation before relying on this in a large
publication run.

### 2. Retire the verbose profiling record and retain the optimization baseline
Type: Structure
Status: done
Proof: Maintainers can locate the measured first priority, compare a later change
with the valid baseline, and run the small opt-in profiler from concise maintained
documentation; detailed execution history no longer occupies the current snapshot.

After slice 1, replace the 525-line `docs/notebook-publication-profiling.md` record
in place with a short reference (aim for roughly 100 lines, not a new rigid limit).
Delete historical static-candidate lists, individual smoke-run accounts, recovery
commands tied to expired PIDs/checkouts, continuation probes and duplicate run
narration. Keep the same document path for its useful current instructions; do not
move the deleted narrative into another tracked archive or into the story seed.
The full original record is recoverable with
`git show 13a2e2edc6:docs/notebook-publication-profiling.md`.

Retain only what the optimization still needs:
- The ORM-flush priority (about 98.2% of request-thread execution samples), index
  refresh callers, allocation estimate and the warning that sample shares do not
  predict wall-time savings or establish which flushes can safely be removed.
- A compact baseline table: both capture IDs and source revisions, fixture counts
  and fingerprints, elapsed HTTP times, accepted-content proof, and environment
  qualifiers. Identify the awake run as the usable elapsed-time reference;
  distinguish the sleep-affected run and interrupted Cypress verification.
- Small late-rejection correctness evidence; explicitly state that large rejection
  latency remains unmeasured. Preserve the small-first stopping rule.
- The raw-artifact location, minimum analysis invocation and current small-fixture
  capture/reset/ownership instructions, updated for slice 1's bulk verification.

Raw JFRs and recovery artifacts already live outside Git under
`~/Library/Application Support/Donut/publication-profiles/`; retain them through
optimization and its before/after review. This slice removes repository narration,
not those external measurements or the reusable profiler/analyzer code. Deleting
tracked text reduces the working-tree/documentation footprint, not Git history size;
no history rewrite is needed or authorized.

Repair links in SEED-018 story 3 and this plan when removing document anchors;
point to the concise reference instead of recreating historical sections. Review
other incoming links to the document and preserve their useful navigation.
Verification: check the retained facts against the existing record before replacing
it, inspect the documentation diff and affected links, and confirm the profiling
code is unchanged by this slice. No publication run or new benchmark is required.
Estimate: about 5 minutes active work. Safe stop: concise runnable reference and
baseline remain available, with detailed evidence recoverable from Git/external
captures. No need to wait for optimization to complete this repository cleanup.

Done: `docs/notebook-publication-profiling.md` trimmed from 525 to 119 lines,
retaining the ORM-flush finding, a compact "Baseline captures" table (merging
the former "First large valid capture" and "Awake valid repeat" sections),
the small late-rejection evidence, and current run instructions updated for
slice 1's bulk verification task. Retained facts were checked against
`git show 13a2e2edc6:docs/notebook-publication-profiling.md`. Links repaired:
`.planning/seeds/SEED-018-publish-large-authored-notebooks.md` and this plan's
own Evidence line now point `#awake-valid-repeat`/`#first-large-valid-capture`
references to `#baseline-captures`; `#findings-and-next-experiment` and
`#small-late-rejection-capture` anchors were unchanged. No dangling links
remain (repo-wide grep confirmed); profiling code is unchanged.

All slices complete.

## Suite assessment and retained coverage

E2E scenarios drove the profiler's implementation. Its four tags are excluded
from ordinary CI; retain CLI-versus-HTTP and acceptance-versus-rejection distinctions,
which exercise different transport/result boundaries. Ordinary web-created-note
pull/rebase/publish journeys remain useful integration documentation.
Backend controller atomicity coverage (e.g. NotebookGitNoteCreationAtomicControllerTest)
and CLI run-boundary submission/rejection suites provide distinct lower-cost
contract coverage; neither replaces the real server/receiver journey. Repository
suite configuration also separates frontend, MCP and infrastructure tests. No
observed overlap/cost finding warrants pruning those unrelated suites. This review
inspected suite structure and relevant examples, not every test or a full run.

## Reviewed commit manifest

All commits below are attributable through the execution conversation and changed
paths; the range has no intervening unrelated commit. Planning-only entries are
provenance, not product defects. No backend/frontend/CLI/MCP product code changed.

| Commit | Membership evidence |
| --- | --- |
| 3548fb8bc3 | Taken-only claim; provenance |
| 00c9735cd1 | Static candidate document, first delivered investigation slice |
| cda93cdccc | Proof-step refinement; provenance |
| 453603a428 | Opt-in capture and receiver verification harness |
| 0ceff10238 | Representative fixture and fingerprints |
| e4a324d82e | Rejection and preserved-state proof |
| b650a294f6 | Benchmark HTTP transport |
| 361627c3ff | First profile, recovery evidence and analysis script |
| 4543afeed1 | Continuation probe/repeat instructions |
| eabef0a7cb | Second verified profile and safe pause |
| 3651153736 | User-approved reduced scope and completed findings handoff |

## Delivery when separately authorized

Use dough-execute-plan from the selected execution location: independent
post-change refactor, API generation only if a trigger changes, one coordinator
selective format, check-only commit hook and authorized push. No correction code,
benchmark, commit or push is performed by this retrospective. The completed predecessor is recoverable from the source revision above.
