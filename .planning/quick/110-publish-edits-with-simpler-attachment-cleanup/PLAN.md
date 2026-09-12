# Publish notebook edits faster with simpler attachment cleanup

Status: completed
Source: [SEED-018 story 5](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-5).
Authority: 2026-09-12 request to execute plan 110.
Execution completed on the retained execution branch; the story remains in
**Taken** through retrospective and story wrap-up.

## Outcome and scope

An owner publishes updates to 1,000 existing measured notes in at most half
the comparable baseline median, with simpler production code and a negative
formatted production-line delta. These are updates, not additions. Preserve
exact accepted content/head, note identity, learning, derived state,
authorization, validation, and atomic rejection. Preserve shared image/blob
ownership and cleanup behavior.

The maintained fixture has 1,000 measured notes in 20 folders plus two unchanged
control notes and two control folders (1,002 notes, 22 folders total). Its
content includes aliases, properties and authored references, but no attachments.
Image-containing controller examples own attachment preservation proof. Counts
are examples, not product limits.

Keep staged/background indexing and larger-scale validation in
[story 4](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-4).
Title reservation and soft deletion belong to
[SEED-009 story 26](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-26).
Do not tune that title check in this slice. No 10,000-note capture, 50% addition
speedup, image-heavy throughput target, new benchmark framework, background
job, or changed timeout policy is promised.

## Existing solution and evidence

PFE decision: change the existing shared image-cleanup responsibility in
`NoteService`, using the existing `EntityPersister` and entity deletion lifecycle.
Both publication updates and additions already use it through
`AuthoredNoteDocumentPersistence`; ordinary content saves, construction and
reference handling also share it. Select this note's orphan images directly
instead of fetching all its images and filtering them in a Java loop. Remove
the service's now-unneeded `ImageRepository` dependency and its unused finder.
Keep the repository's other inherited operations.

The tested query expresses the current predicate:

```sql
FROM Image i WHERE i.note = :note AND (:keepId IS NULL OR i.id <> :keepId)
```

Run that typed selection with query-local `FlushModeType.COMMIT`, then delete
each returned entity through the existing persister. Retain the current missing
identity and noncanonical-image-path behavior. Keep the domain rule in one
place and retain cascade deletion of `AttachmentBlob`; bulk image-row deletion
alone is not equivalent. No new collection association, publication mode or
session-wide flush policy is needed by the measured solution.

Storage assumption: selecting orphan images does not require pending note or
reference changes to be flushed. Existing uploads set image ownership on
creation, use identity inserts, and explicitly flush before returning a path;
publication does not reparent images. This is a query-specific justification,
not a general permission to ignore pending entity changes. If execution finds
a conflicting caller, resolve that assumption before retaining the optimization.

[Research findings](../../../docs/notebook-publication-profiling.md#attachment-cleanup-refinement)
record isolated JDK 25.0.3 / Hibernate 7.4.5.Final / MySQL 8.4.11 proof against
baseline `85816929018967c3f995459be119f820bed89f7f`. Three completed runs per
version measured **11,907.715 → 3,038.709 ms median**, **74.48% less waiting**.
The prototype is **11 production lines added / 15 removed**, with all **2,404
backend tests** and accepted-update/late-rejection state checks passing. It
removes the dominant repeated image-query pre-flush traversal. This is retained
experimental evidence, not a completed slice.

Artifacts: `~/Library/Application Support/Donut/publication-profiles/second-refinement-2026-09-12/`
contains `candidate.patch`, `preservation-tests.patch`, exact source copies,
`source-manifest.json`, `comparison-summary.json`, JUnit results, driver logs
and caller analysis. Production patch SHA-256:
`6fab86ea944d47fbf9b2693650a3cb7ac8e306a1c37d5f55c039135ae995c7fa`.
Both patches passed read-only `git apply --check` during planning. Inspect and
adapt to execution's current code; the outcome and proof govern delivery, not
verbatim patch application. The experimental checkout, branch and databases were
retired. Allocate fresh owned resources instead of recovering their identities.

Accepted decisions: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(authored content/reference semantics),
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) (deliberate failures),
and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(owned disposable environments). The existing image/blob lifecycle and
publication transaction remain sufficient; no schema or ADR change is needed.
No established North Star topic adds a governing decision here, and the local
shared simplification does not warrant a new one.

## Ordered slices

### 1. Halve completed edit-publication waiting time through simpler image cleanup
Type: Behavior
Status: done

Behavior: Given the representative existing-note proposal, when its owner
publishes it, then the accepted publication completes in at most half the
comparable baseline median with preserved content, learning and derived state,
and simpler, smaller production code.

Before changing production, record three fresh baseline captures in the owned
execution checkout using the maintained command below. No new fixture or
Structure slice is required. Record source revision and the literal command.

```sh
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_UPDATES=1000 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

Implement the shared selection/dependency simplification above. Carry the
retained preservation assertions into the existing controller examples rather
than creating another test surface. The normal formatter's net production
delta must remain negative; explain removed responsibilities, not just counts.
Prototype net −4 is evidence, not a mandated exact diff. Count test/tooling
changes separately and do not move complexity elsewhere to satisfy the count.

Run the whole backend suite, then the small HTTP addition/rejection pair:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_TAGS='@publicationProfileHttp or @publicationProfileHttpRejection' PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

After the suite completes, repeat the update command for three candidate
captures. Keep identical deterministic tree fingerprints, JVM/JFR settings,
logging and measurement boundary; use fresh owned JVMs, an awake host, and
sequential captures without an overlapping test suite. The launcher already
owns startup, fixture reset and shutdown. Measure HTTP start through complete
response including transaction commit, excluding setup and receiver checks.
Every counted run must return the proposed head and verify all edited bytes
plus the maintained accepted-state assertions. A timeout or failed verification
is incomplete evidence; inspect actual owned-server completion before retrying.

Record all six timings and capture paths, medians, source/patch metadata and
material environment differences. The comparison must show at least 50% less
waiting; the refinement's 5,953.858 ms halfway reference does not replace the
fresh ratio. Analyze a representative completed request JFR to confirm the
image-cleanup traversal has disappeared, using the retained caller analyzer or
the maintained `scripts/profiling/AnalyzePublication.java` with the request's
start/end and sampled publication thread. Preserve the analysis with its capture.
Sample proportions explain cost; completed timings establish the gain.

Update `docs/notebook-publication-profiling.md` with delivered source, actual
results, code-size count and commands, clearly distinguishing them from the
prototype evidence. Retain the update/addition distinction and later-story leads.

Sizing: about 4–5 minutes of focused implementation, proof inspection and
cleanup, grounded in the tested two-file change and existing assertions.
Allow 6–10 additional minutes for six fresh stack/capture invocations, the
full backend suite and small HTTP pair; the research observed a 62-second
suite and roughly 30–60 seconds per capture invocation after dependency setup.
First-use dependency provisioning is separately visible external wait.
These are explicit verification-wait exceptions, not allowance for an
unbounded redesign. Stop-safe only after the coherent change passes all gates;
if speed or simplicity fails, keep the slice unfinished and retain evidence.

Delivered evidence (2026-09-12): three fresh baseline captures measured
11,847.590 / 13,303.793 / 12,089.157 ms; three fresh candidate captures measured
3,028.152 / 3,095.452 / 3,125.534 ms. The median fell from 12,089.157 to
3,095.452 ms, a 74.39% reduction. All six runs completed with HTTP 200, the
proposed head, 1,000 verified documents, 1,002 unchanged identities, unchanged
learning, 1,000 aliases and 2,000 property-reference targets. Capture paths and
environment metadata are retained in
`docs/notebook-publication-profiling.md#attachment-cleanup-execution-comparison`.

`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed all 2,404 tests.
The literal small addition/rejection command above passed both scenarios. The
representative candidate JFR recorded 67 request-thread samples, seven
transaction-commit flush samples and zero image-cleanup flush samples; its
analysis is retained with capture `2026-09-12T15-23-44.883Z`. Formatted
production code added 11 and removed 15 lines (net −4), removing the repository
dependency, unused finder and Java filtering without relocating the
responsibility. `scripts/check_diff_whitespace.sh` passed. The fresh post-change
refactor review found `none — already clean` and ended `## REFACTOR COMPLETE`
without edits or invalidated proof. Active implementation/proof cleanup was
approximately four minutes; profiler and suite waits used the stated exception.

## Promise-to-proof ownership

All proof belongs to slice 1; correctness examples preserve the one changed
publication behavior rather than forming independent delivery slices.

| Promise | Existing proof to use or extend |
| --- | --- |
| At least 50% faster on 1,000 existing / 1,000 updates | Three baseline and three candidate completed HTTP captures through `@publicationProfileHttpUpdate`, matching fingerprints and median comparison |
| Exact files/head, IDs, learning, new/obsolete aliases and property references | Maintained receiver and `expectPublicationEditsPersisted`: 1,000 verified files, 1,002 unchanged IDs, unchanged learning, 1,000 exact aliases, 2,000 exact related-property targets; existing batch-publication controller cases |
| Referenced image/blob retained; orphan image/blob deleted; other notes untouched | Extend `TextContentControllerUpdateNoteContentTests.deletesOrphanImagesWhenContentReferencesSingleAttachmentPath` with retained blob and other-note assertions; keep no-scalar and noncanonical-scalar cases |
| Late failure restores images/blobs with content, references and binding | Extend `NotebookGitPublicationAtomicControllerTest.lateBindingSaveFailureRollsBackWebContentTimestampReferencesAndAcceptedBinding` with the retained blob assertion |
| Shared additions and atomic late invalid rejection | Existing 20-addition acceptance and final-path invalid-alias HTTP scenarios, 19 preceding identities allocated and unchanged original head/rows/learning/indexes; existing invalid-edit controller cases |
| Authorization, validation and shared caller correctness | Full backend suite including `NotebookGitBundleControllerTest`, publication controllers, content and title-rewrite examples; no weakened assertions |
| Simpler and fewer production lines | Formatted aggregate production diff plus fresh refactor review: removed dependency/finder/filtering, preserved domain ownership and cascade, no relocated complexity |

## Execution and delivery

Use the current `dough-execute-plan` workflow when execution is authorized.
Planning leaves the backlog entry queued. Execution defaults to a fresh linked
worktree: inspect originating changes, make the authorized Taken transition,
then record originating/execution checkout and branch plus integration target
(`main` unless directed otherwise) in this plan. Existing uncommitted seed and
backlog work must be preserved and available to the execution checkout; resolve
its ownership through the normal preflight rather than stashing or discarding it.

Follow Jidoka → fresh `dough-post-change-refactor` agent → API generation only
if an actual API change triggers it → coordinator runs
`./scripts/run.sh pnpm format:changed` once → update plan → commit through the
independent check-only lint hook → push the execution branch and observe CI
asynchronously. Implementers/refactorers do not run the formatter or standalone
`lint:changed`. Follow the execution workflow's integration and ownership rules;
retain this plan and review evidence through retrospective and story wrap-up.

Execution identity (started 2026-09-12): originating checkout
`/Users/terryyin/git/doughnut` on `main`; execution checkout
`/Users/terryyin/git/doughnut/.worktrees/quick-110-publish-edits-with-simpler-attachment-cleanup`
on `quick/110-publish-edits-with-simpler-attachment-cleanup`; integration target
`main`. The execution branch starts from Taken-only commit `9468b251b7`.
GitHub Actions workflow `.github/workflows/ci.yml` (`donut CI`) is push-triggered
only for `main`, so it cannot observe pushes to the retained execution branch;
CI notification coverage is unavailable for this execution branch.

Use the repository's five-minute slice target and ten-minute hard scrutiny:
distinguish the stated external waits from active work. If active implementation
develops multiple outcomes or an unexplained overrun, safely park attempt-owned
work and refine this same plan. New evidence does not lower the story's criteria
or authorize the deferred indexing or soft-delete work.

## Planning assessment

One Behavior slice owns one measured outcome and its preservation gates. The
profiler and storage proof already exist; a separate setup or test-only slice
would not add an independently useful outcome. The cumulative model remains
the existing shared cleanup, with one orphan-selection rule and unchanged entity
lifecycle. No remaining slice-specific design, integration or sizing concern
was identified in this assessment beyond the explicit external-wait exception.
No separate slice-plan refinement pass was invoked. This finding supplies no
execution authority; reconsider it if code or evidence invalidates the stated
assumptions.
