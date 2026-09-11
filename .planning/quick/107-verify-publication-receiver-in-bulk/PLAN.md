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
file and records acceptance without one browser-command chain per file.

Finding: `e2e_test/start/pageObjects/cli/notebookPublicationProfile.ts:expectReceived`
still iterates through `expectCheckoutFileAt` for every proposal file. In the second
large capture, HTTP completed at 13:40:01 UTC and receiver verification was still
running when the user requested a safe stop. Independent read-only verification
of the already pulled receiver checked all 10,000 bytes/head/tree/clean status in
2.5 seconds. The workaround is retained outside Git; documented advice does not
correct the reusable runner. No product-publication regression is attributed.

Evidence: `docs/notebook-publication-profiling.md#awake-valid-repeat` and persistent
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

## Ordered slice

### 1. Verify the publicly received proposal in one bulk observation
Type: Behavior
Status: planned
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
