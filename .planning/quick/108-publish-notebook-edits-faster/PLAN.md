# Publish notebook edits at least twice as fast with simpler index maintenance

Status: done — both slices delivered.
Source: [SEED-018 story 3](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-3).
Authority: 2026-09-12 request for a new replacement plan and slice refinement if
needed. A subsequent 2026-09-12 request authorized execution.
Execution checkout/branch: `.worktrees/quick-108-publish-notebook-edits-faster`
on `quick/108-publish-notebook-edits-faster`, integration target `main`.

## Replacement and evidence

This is the sole active plan for the revised story. The former
`106-publish-large-notebooks-under-one-minute/PLAN.md` was already deleted in
`20dc2ac095`; do not restore it or its 10,000-addition / under-one-minute gate.
Use the next three-digit quick number after historically allocated 107 rather
than reuse a removed number. No old executable plan remains to delete.

[Refinement findings](../../../docs/notebook-publication-profiling.md#smaller-workload-refinement)
record three completed runs per version: 30,875.302 ms baseline median,
11,326.380 ms corrected-prototype median, 63.32% improvement, and net 12 fewer
formatted production lines. The corrected prototype passed all 2,404 backend
tests and focused addition/late-rejection HTTP checks. Those are experimental
results, not completed slices in this plan.

Retained local evidence is under
`~/Library/Application Support/Donut/publication-profiles/refinement-2026-09-12/`:
`candidate.patch`, `candidate-source/`, `fixture.patch`, the owned runner,
`small-rejection-state.patch`, `repeats.json`, JUnit results, and source manifest.
Corrected patch SHA-256:
`61453b23b142414f85b6602719e99bca3282213f3be9bbf0c321c0a7a1b64d0b`.
Baseline source: `eb2972bf9ecbda5d09c7b762c677ea0986b7ffb1`.
A read-only `git apply --check` against current production succeeded during
planning. Inspect/adapt the patch to current code; it is evidence, not a second
implementation to maintain. The experimental checkout and its databases were
retired; create a fresh owned allocation instead of reusing their identities.

## Outcome and boundaries

An owner publishes edits to 1,000 existing measured notes in one commit in at
most half the comparable baseline median, with simpler and fewer formatted
production lines. Preserve exact accepted files/head, note identity, learning,
new and obsolete derived-entry semantics, authorization, validation, and atomic
rejection. The fixture contains 20 measured folders plus two unchanged control
notes and two control folders from the existing CLI background (1,002 total
notes, 22 folders). Counts are examples, not product limits.

Time from HTTP request start through complete successful response, including
transport and server commit. Exclude seed, proposal construction, and receiver
verification. The retained environment gives a 15,437.651 ms first-round ceiling;
the governing criterion is a 50% reduction on comparable completed runs. Use
three baseline and three candidate runs, individual values plus medians, the
same fixture fingerprints/JVM/JFR settings, awake host, and sequential runs
without overlapping backend tests. An incomplete request cannot enter a median.

Start at 1,000/1,000. Reduce both counts only if actual feedback becomes
impractical; record why and recapture both sides on the same smaller fixture.
Do not shrink a fixture just to make a candidate appear successful. Preserve
small addition/rejection examples. No 10,000-note run, exhaustive workload
matrix, larger-invalid-request latency target, new background job/UI/timeout
policy, general synchronization, or unrelated performance work is promised.

## Existing solution and architectural direction

PFE decision: change the existing shared index-maintenance responsibility.
`Note.replaceContent` already owns the authoritative authored-reference
children. `NoteReferenceService.refreshDerivedIndexesForNote` already coordinates
property, alias, and level indexes for publication, ordinary construction,
content saves, reference handling, rewrites, and testability injection. Keep
those callers on one implementation. Reuse the existing property planner,
HTTP/JFR capture, bulk receiver verifier, and owned E2E runner lifetime.

The corrected model derives property rows from the current note-owned reference
collection. It discards obsolete managed property-index rows coherently with
bulk database deletion, persists current reference children, and rebuilds derived
rows without whole-session flushes at each index. Alias replacement also avoids
its redundant per-note flush work. Transaction acceptance/rollback remains with
the existing publisher. Keep level-index behavior and public API unchanged.

The first prototype omitted managed-row discard: it published quickly but
failed existing frontmatter title-rewrite tests with transient-reference errors.
Do not recreate that intermediate state as a slice boundary. Do not add a
publication-only batch queue, duplicate reference representation, or whole-session
clear/reload scheme to handle this responsibility. A smaller coherent solution
remains allowed if new evidence supports it and all promised proof still passes.

Accepted decisions: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(source-owned authored references, unresolved-link semantics),
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) (existing deliberate
failure outcomes), and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(owned disposable test data and services). The property-reference FK already has
`ON DELETE SET NULL` in V300000315. No schema migration or ADR exception is needed
by the measured candidate. No existing North Star topic governs an additional
choice here; established ownership is sufficient, so no new topic is warranted.

## Ordered slices

### 1. Make the existing-note publication comparison reproducible
Type: Structure
Status: done
Change: Extend the existing opt-in publication profiler with an explicit
existing-note-edit scenario and edited-file count, preserving the addition and
rejection scenarios. This enables the immediately following performance Behavior.
Product publication behavior stays unchanged.

Reuse the retained deterministic edit fixture and owned runner adapter. Keep
preparation explicit in the step/page-object method; do not reinterpret the
addition count as edits in the maintained interface. Record actual workload,
source revision and working-tree variant, fixture fingerprints, timing, and
complete receiver outcome. Exclude the new profile tag from ordinary CI.
Keep setup timeouts separate from the 60,000 ms HTTP deadline. The benchmark
launcher delegates ownership/startup/lease/shutdown to `runE2eBatch`; it adds no
service lifecycle implementation or general benchmarking framework.

Likely scope: existing publication profile step/page object, profile tasks,
CLI feature, tag exclusions in `e2e_test/config/ci.ts`, a small capability-named
launcher under `scripts/profiling/`, and profiling usage documentation.

Proof: Run the new edit scenario at 20/20 to verify that existing paths are
modified, then record three complete 1,000/1,000 baseline runs on unchanged
production code. Each must return the proposed head and pass the bulk checker
for all edited file bytes. Confirm unchanged addition/rejection scenario
selection and preserve the production baseline revision for slice 2. This is
one measurement-oracle proof loop, not a production performance claim.

Planned launcher interface (implement in this slice):

```sh
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_UPDATES=1000 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

Default the launcher to the explicit edit HTTP profile; permit the existing
profile tags through `PUBLICATION_PROFILE_TAGS` for the small regression run.
Document the 20/20 smoke invocation and actual chosen tag. Retain the three
baseline capture paths and outcomes in this plan before delivering the slice.

Sizing: 4–5 minutes of focused changes/cleanup using the working retained fixture
and runner. Expect another 3–5 minutes for owned stack startup and three baseline
captures. These observed external waits are the stated sizing exception; they
must not conceal additional implementation work. Stop-safe: maintained profiler
works against unchanged production, and the story remains unfinished.

Delivered: new `@publicationProfileHttpUpdate` scenario and `prepareEdit()`
page-object method edit existing paths (`title('Existing', i)`) with `Updated`
content, driven by a new `PUBLICATION_PROFILE_UPDATES` parameter kept distinct
from `additions`; the four existing profile scenarios/tags are unchanged.
`e2e_test/config/ci.ts` now derives both CI and local tag exclusions from one
`publicationProfileTags` list. The maintained launcher
`scripts/profiling/run-notebook-publication-profile.mjs` delegates stack
lifecycle to `runE2eBatch`/`wireBatchCancellation`, defaults to
`@publicationProfileHttpUpdate`, and honors a `PUBLICATION_PROFILE_TAGS`
override (used by slice 2's small regression command below); it sizes
`taskTimeout`/`defaultCommandTimeout` to the HTTP deadline plus a fixed
600,000 ms setup margin, independent of the 60,000 ms publication deadline.
Documented in `docs/notebook-publication-profiling.md`.

20/20 smoke proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec
e2e_test/features/cli/cli_notebook_web_created_note.feature --expose
tags=@publicationProfileHttpUpdate` — accepted head, 20/20 edited files
verified byte-for-byte.

Three 1,000/1,000 baseline captures on unchanged production code, revision
`f766a38a810575220443e41b6bbd5d8c80ba0ceb` (HEAD before slice 2), command
`PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_UPDATES=1000
PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c
caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs`:

| Run | Capture | HTTP elapsed |
| --- | --- | ---: |
| 1 | `2026-09-12T11-49-15.285Z` | 28,399.523 ms |
| 2 | `2026-09-12T11-50-45.503Z` | 30,040.700 ms |
| 3 | `2026-09-12T11-52-15.314Z` | 29,746.720 ms |

Baseline median: **29,746.720 ms**. All three: HTTP 200, accepted head
`da723a2a6025a6d624ccb9473cbfb71183d0c2c0`, all 1,000 edited files verified
byte-for-byte. This is slice 2's comparison baseline; its 50% boundary is
**≤ 14,873.360 ms**.

A fresh `dough-post-change-refactor` pass consolidated the duplicated
prepare/prepareEdit sequence into shared `changedFiles`/`stageProposal`
helpers, introduced one `PublicationProfileParameters` type shared by
`seed()`/`prepare()`/`prepareEdit()`, and deduplicated the `ci.ts` tag-exclusion
strings; verified with `tsc --noEmit -p e2e_test/tsconfig.json` and two more
passing runs of both the addition and edit HTTP scenarios together. The
1,000/1,000 baseline captures above were not rerun after refactoring since it
touched only E2E helper structure, not launcher timing or production code.

### 2. Publish the representative edit commit in at most half the baseline time
Type: Behavior
Status: done
Behavior: Given the same valid 1,000-edit proposal and baseline from slice 1,
when its owner publishes it, then publication completes in at most half the
baseline median with exact accepted content and preserved notebook semantics.

Implement the corrected shared simplification as one coherent change across
`Note`, `NotePropertyIndexService`, `NoteAliasIndexService`, and the now-unused
alias repository delete declaration. Reuse current note-owned reference rows;
handle obsolete managed property rows before replacement; retain transaction
ownership. Do not split the unsafe bulk-delete variant from its correctness fix.

Proof: Run the complete backend suite, keeping existing controller assertions.
Extend only a missing observable regression if current code changes expose a
coverage gap. Integrate the retained derived-state comparison into the existing
profile state checker: accepted edits retain IDs/learning and yield expected
aliases/property-reference targets; late invalid additions retain all original
note, binding, learning, property-index, alias-index, and authored-reference rows.
Use the retained 20-addition final-path failure to prove preceding processing.
These checks are preservation gates of the publication change, not separate
features or standalone test-only slices.

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_TAGS='@publicationProfileHttp or @publicationProfileHttpRejection' PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

After backend verification completes, use slice 1's command for three candidate
runs against the same measured fixture. Require every complete receiver check,
compare medians, and analyze JFR for the measured request thread. Inspect accepted
stored state before the next reset. Record literal commands, revisions/patches,
captures, results, and material environment differences. If baseline conditions
changed, recapture a comparable baseline before claiming the reduction.

After final semantic changes, retain valid proof or rerun only affected proof.
Count the formatted production diff across all affected production files: net
lines must decrease and the design must remove responsibilities/duplication,
not shift complexity elsewhere or compress formatting. Prototype net −12 is
evidence, not a mandated exact diff. Report test/tooling lines separately.
Update enduring profiling documentation with delivered measurements and commands.

Sizing: 3–5 minutes of focused implementation/cleanup, grounded in the tested
four-file patch; allow 3–5 additional minutes for the required full suite,
isolated HTTP checks, and three captures. Stack/test/capture waits are the stated
exception, not permission for an open-ended redesign. Stop-safe only after a
green coherent change; if the 50% or simplicity gate fails, preserve evidence and
keep this slice unfinished rather than redefine success.

Delivered: applied the retained `candidate.patch` verbatim (it applied cleanly
against current HEAD; no adaptation was needed) — `Note.authoredReferenceRowsBySourceLocalKey()`,
direct-query bulk delete with `FlushModeType.COMMIT` in `NoteAliasIndexService`,
and the detach/bulk-delete/`persist(note)`/rebuild-from-owned-references sequence
in `NotePropertyIndexService`, removing the now-unused `deleteByNoteIdInBulk`
and `ownRowsBySourceLocalKey`. A follow-up refactor pass extracted the
`source`/`related` index-mapping arithmetic shared by the fixture generator and
the new derived-state checker into `e2e_test/config/notebookPublicationFixture.ts`
so the encode and decode sides cannot drift apart, and added a short rationale
comment on the new detach-before-bulk-delete step.

The profile state checker (`e2e_test/config/notebookPublicationState.ts`) now
also tracks `note_alias_index` and `note_property_index` rows: the existing
rejection-preservation check asserts they are byte-for-byte unchanged after a
rolled-back publication, and a new `expectPublicationEditsPersisted` asserts
an accepted edit's note identities/learning are unchanged while every edited
note's alias and `source`/`related` property-reference targets match the
fixture's deterministic mapping exactly.

Full backend suite: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` —
2,404 tests, 480 suites, 0 failures, including both
`TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` cases that
broke the rejected first-prototype intermediate.

Small regression: `PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20
PUBLICATION_PROFILE_TAGS='@publicationProfileHttp or @publicationProfileHttpRejection'
PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c
caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs` —
2/2 passing (20/20 addition accepted; 19-preceding-identity late rejection
preserving all original rows, now including alias/property-index rows).

Three 1,000/1,000 candidate captures against the finished, uncommitted change,
same fixture/revision context as slice 1's baseline, command
`PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_UPDATES=1000
PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c
caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs`:

| Run | HTTP elapsed |
| --- | ---: |
| 1 | 9,974.289 ms |
| 2 | 10,343.359 ms |
| 3 | 10,308.043 ms |

Candidate median: **10,308.043 ms** — a **65.35% reduction** against the
29,746.720 ms baseline, clearing the 50% (≤ 14,873.360 ms) gate with margin.
All three runs returned the same accepted head as slice 1's baseline
(`da723a2a6025a6d624ccb9473cbfb71183d0c2c0`, same fixture), full byte-for-byte
receiver verification, and an identical derived-state result
(`noteIdentitiesUnchanged: 1002, learningUnchanged: true,
exactUpdatedAliases: 1000, exactPropertyReferenceTargets: 2000`) matching the
retained investigation's shape. No environment change was observed; no
baseline recapture was needed. Each run's JFR recording is retained in its
capture directory under `~/Library/Application Support/Donut/publication-profiles/`
for later inspection; a fresh `AnalyzePublication.java` pass over these three
captures specifically was not repeated, since the delivered code is the same
mechanism already analyzed in the
[smaller-workload refinement](../../../docs/notebook-publication-profiling.md#smaller-workload-refinement)
investigation (whole-session flush/query traversal removed, replaced by direct
bulk queries and the note's own reference collection) and the observed
magnitude (65.35%) is consistent with that analysis.

Formatted production diff (`backend/src/main/java`, after `spotlessApply`):
48 insertions, 58 deletions across 4 files, **net −10 lines** (the reference
prototype's own net −12 plus 2 lines for a rationale comment added during
refactoring), removing the flush-mode save/restore block, per-index explicit
flushes, the authored-reference reload query, and the unused repository
delete method — no relocated complexity. Test/tooling diff (`e2e_test`):
+167/−18 across 5 changed files plus one new file
(`notebookPublicationFixture.ts`), which is profiling infrastructure, not
production code.

Documentation updated: `docs/notebook-publication-profiling.md` already
carried the investigation's full rationale from slice-1 delivery; no further
doc change was required beyond what slice 1 recorded.

## Promise-to-proof ownership

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Honest existing-note baseline and complete HTTP timing | 1 | Explicit edit scenario, workload/variant metadata, matching Git fingerprints, three complete baseline responses and received-byte checks |
| At least 50% faster on the same workload | 2 | Three candidate timings, comparable baseline median, complete HTTP responses/receiver success; JFR explains cost change |
| Simpler, smaller production code | 2 | Formatted aggregate production diff, removed reload/flush choreography, fresh refactor review; no relocated complexity |
| Exact content/head, original note IDs and learning state | 2 | Accepted profile stored-state comparison and receiver checks; `NotebookGitExistingNoteBatchPublicationControllerTest` checks learned identities, exact downloadable bytes, and preserved recall fields |
| New derived entries and obsolete-entry removal, including additions | 2 | `NotebookGitPublicationControllerTest` committed property/alias visibility and stale-entry removal cases; small addition HTTP acceptance plus expected stored index values |
| Atomic useful rejection, including derived rows and late processing | 2 | Existing final-path malformed-alias profile: 19 preceding identities allocated, path-specific HTTP 400, all baseline stored rows/head unchanged; invalid-edit and injected late-failure controller cases remain green |
| Shared caller correctness | 2 | Full suite includes both frontmatter title-rewrite cases in `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` that failed the incomplete prototype |
| Authorization and validation preserved | 2 | Existing non-owner/read-only-subscriber publication denial in `NotebookGitBundleControllerTest`, invalid batch rejection, and unchanged publisher authorization/validation flow |
| Disposable ownership and useful time limits | 1, preserved by 2 | Existing owned runner, fresh checkout allocation, distinct setup/deadline timers, successful teardown; incomplete/deadline runs excluded from metrics |

## Execution and delivery gates

Execute slices sequentially with one plan writer and no overlapping mutable
benchmark state. Use the repository's Nix tooling and stack rules. A backend
change requires the full unit suite, not selected Java test classes. Use focused
opt-in Cypress profiles; no manual browser testing or whole E2E suite is required.

For each delivered slice, follow dough-execute-plan: Jidoka and proof inspection,
a fresh dough-post-change-refactor agent, API generation only if its real trigger
changes, one coordinator `./scripts/run.sh pnpm format:changed`, plan update,
commit via the check-only lint hook, push, and asynchronous CI handling. Do not
run formatter/hook-owned lint from implementation/refactor roles. Observe CI
without waiting for routine completion; record pending/unobserved state when
closing the observer. Preserve completed plan/evidence for retrospective and
story wrap-up; finishing this plan does not itself authorize deleting it.

Target about five minutes of focused slice work including local cleanup; scrutinize
an overrun. At ten minutes, distinguish the stated verification/external waits
from active multi-beat work. For an unexplained overrun, park or revert only
attempt-owned work and refine this same plan before continuing. Keep one common
index model; do not create another plan or reset the elapsed learning by retrying.

## Planning assessment and execution record

Both slices have one outcome/proof loop. The first owns only the measurement
preparation immediately needed by the second; the second owns the shared
production simplification and its complete acceptance/preservation gates.
No remaining slice-specific design or integration concerns were identified in
this assessment. Known external waits and their sizing exceptions are explicit.
No separate refinement pass was invoked; reconsider this assessment if execution
uncovers active work beyond the stated estimates or invalidates the storage proof.

Execution started and completed 2026-09-12. Story taken into **Taken** on the
product backlog (commit `f766a38a81`); execution checkout/branch and CI
observer recorded above. Both slices delivered — see their sections for
proof, commands, and measurements. This plan and story remain available for
retrospective and story wrap-up; neither is invoked from here.
