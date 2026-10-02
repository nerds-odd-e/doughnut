# Retire numbered-property migration

## Source and ownership

- **Identity:** SEED-063#story-3
- **Source:** [Remove spent numbered-property migration after confirmed completion](../../seeds/SEED-063-track-property-values-separately.md#story-3).
- **Authorization:** planning only. On 2026-10-02 the owner confirmed “the migration
  is done” and requested a slice plan. Treat that confirmation as satisfying the
  existing operational prerequisite; no production observation is part of this work.
- **Preparation:** Shunka-chan; reuse the owned worktree
  `/Users/terryyin/git/doughnut/.worktrees/remove-spent-numbered-property-migration-after-c`,
  branch `codex/remove-spent-numbered-property-migration-after-c`, starting revision
  `e9ca70c06289ed6e57bc105b455db72af9de79ee`.
- **Publication target:** `origin/main`. Integration checkout:
  `/Users/terryyin/git/doughnut`. Retain the existing Preparing assignment and
  uncommitted preparation draft until an explicit disposition instruction.

## Goal and scope

Ordinary application startup no longer runs the spent numbered-property
conversion. Remove its caller, orchestration, helpers and migration-only proof,
including obsolete responsibilities embedded in shared classes. Preserve ordinary
frontmatter editing, list-property tracking, legacy-key recognition, reference
resolution, accepted Git history and private learning identities and state.
Update documentation that currently describes the runner as active.

No further data conversion, schema or Flyway-history changes, history rewriting,
deployment, production observation, refusal repair, or ongoing converter is included.
No frontend, CLI, MCP or generated API change is expected.

## Existing solution and constraints

PFE decision: remove the retired responsibility from its current owners; add no
replacement conversion service or abstraction. `PropertyMemoryTrackerService`
already owns ordinary key rename, scalar-to-list following and relationship
tracking. `NoteContentMarkdown` and `FrontmatterInPlaceEdit` own ordinary authored
editing; `PropertyKeyNaming` still recognizes authored numbered keys. The wiki
resolver and candidate owners serve ordinary links and aliases. Preserve these
owners while deleting conversion-only entry points and proposed-content plumbing.
The frontend's `isListCapablePropertyKey` serves rich editing and remains in use;
its matching name is not evidence that it belongs to the migration.

Follow the existing [North Star](../../NORTH-STAR.md), particularly One notebook
tree and One accepted-change boundary. No new architectural direction is needed.
Relevant Accepted constraints are [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
(accepted history and private identities), [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#recall-history-and-current-state)
(learning state/history), [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(authored content and exact property selectors), and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(disposable isolated test data). Removal follows these decisions; no exception is proposed.

## Observed premises and proof ownership

Inspected at the preparation's starting revision; these are source observations,
not claims that tests have passed. All final proof belongs to slice 1.

| Premise and consuming operation | Observation and result | Final proof |
| --- | --- | --- |
| The migration is spent; removal may proceed | Owner's 2026-10-02 confirmation above settles the story's operational prerequisite. No independent production evidence is claimed. | Keep this decision with the source; if contrary evidence arrives, stop removal and revisit the prerequisite. |
| Removing the listener ends startup conversion while Flyway remains | Read `configs/NumberedPropertyStartupMigration.java` and `configs/FlyWayFreeVersionRealMigration.java`: separate non-test ready-event listeners; the former calls the runner, the latter independently repairs and migrates. | Source/caller scan establishes that the conversion listener and runner are gone. A permanent startup boundary test exercises the surviving Flyway listener and observes unchanged consolidated content, accepted head/history, tracker IDs, schedules and recall history across ready events. |
| Shared classes contain removable conversion-only methods | Product-wide `rg -n 'NumberedProperty|ConsolidatedProperties|consolidateNumberedProperties|composeConsolidatedFocuses|consolidationDiagnostic|followConsolidatedProperties|readWithoutAutoFlush|findAllIdsInOrder|retargetProperty|equalPersistedFocus|findProjectedAliasTarget' backend frontend cli mcp-server scripts e2e_test docs .planning` found the conversion callers and their supporting methods. Read the runner, preparation, tracker service, and frontmatter entry points: ordinary tracker methods are separate. | Compile and the full backend suite; repeat the caller search after edits and inspect any surviving hit before removal. Preserve ordinary consumers. |
| Proposed-content resolution belongs to conversion; current-content resolution remains needed | Read `WikiLinkResolver.tokenCandidates`, `WikiLinkCandidateClassifier` and `WikiLinkNoteCandidates`; `rg -n 'projectedContent|tokenCandidates|findProjectedAliasTarget|nameMatches' backend/src/main backend/src/test` traces non-empty proposed content to the migration, while ordinary resolution uses current content. | Existing `NoteControllerShowWikiLinkTests` and `NoteControllerShowWikiLinkAmbiguityTests` observe resolved aliases, title/alias collisions and viewer-specific candidate sets through note-controller responses. Retain those assertions while simplifying unused projection support. |
| Ordinary editing and learning have reusable proof, with one missing combined example | Read `NotebookGitWebContentSaveControllerTest.savesARootNoteAsOneAcceptedRevisionWithoutChangingItsLearningIdentity`: saves through the real controller and inspects read-back, tracker identity and accepted Git ancestry. Read `MemoryTrackerFollowPropertyValueControllerTest`: real controller following retains schedule and recall history. Read `MemoryTrackerUpdatePropertyKeyControllerTest` and `UnassimilatedPropertyServiceTest`: list-value rename and value-based matching survive reorder/removal. | Reuse these tests and add one controller regression for a body edit of already-consolidated list content with learned value trackers; observe unchanged tracker identities, schedules/history and a normal forward accepted content edit. Do not claim the existing note-level fixture already proves this combined case. |
| Migration proof can retire without deleting general startup or schema responsibilities | Read `NotebookGitStartupServicesProbeTest` and `NumberedPropertyStartupTestSupport`: these currently require the converter and test conversion, but also exercise the real Flyway listener using the suite's database. | Replace their migration scenario with the permanent startup preservation example above, based on ordinary shared test support; delete migration-only support and assertions. Keep Flyway and schema files unchanged. |

The scans included scripts and E2E sources; the migration entry points have no
ordinary CLI/MCP/frontend/script journey to relocate. Inspection of existing
fixtures and assertions settles the proof-selection premises. Planning does not
run the full backend suite; execution must run it after edits.

## Ordered slices

### 1. Ordinary startup after migration retirement

Type: Behavior
Status: planned

Behavior: after the confirmed conversion, application startup runs its ordinary
Flyway work without scanning or consolidating numbered properties. Existing
consolidated notes, accepted history and learned list-value trackers remain intact;
ordinary property editing and learning continue through their existing owners.

Implementation and cleanup in this same slice:

- Delete `NumberedPropertyStartupMigration`, `NumberedPropertyMigration`,
  `NumberedPropertyMigrationPreparation`, `NumberedPropertyReferencePreservation`
  and `FrontmatterNumberedProperties`.
- Remove conversion records/wrappers from `NoteContentMarkdown` and
  `FrontmatterInPlaceEdit`, conversion mapping/diagnostic/deduplication methods
  from `PropertyMemoryTrackerService`, and now-unused constructor dependencies
  and imports. Retain ordinary edit, following and rehoming methods.
- Remove conversion-only repository queries, the unused read-without-auto-flush
  helper and property-retarget helper, and the Java list-capable eligibility
  predicate if their caller closure remains migration-only. Simplify wiki
  candidate lookup back to current stored content, removing unused projection
  overloads and alias queries without changing viewer or ambiguity semantics.
  Recheck every affected caller rather than deleting an entire shared owner.
- Remove the `NumberedPropertyMigration*` test classes/support and
  `NoteContentMarkdownNumberedPropertiesTest`. Replace the migration-specific
  startup probe/support with the permanent startup-preservation proof above.
  Keep shared key-naming, editing, reference and learning tests.
- Add the single missing controller-level list-edit preservation example above.
  Use `makeMe`, real services and the existing isolated database; no new Spring
  Boot context or additional connection pool.
- Replace the active-runner instructions in `docs/numbered-property-migration.md`
  with concise current behavior: conversion retired, no startup normalization,
  ordinary list properties/trackers continue, accepted history stays intact.
  Keep the still-linked path useful for `SEED-064`; remove obsolete operational
  instructions instead of preserving a migration implementation archive.

Proof: the startup and body-edit examples above, retained shared behavior tests,
and `CURSOR_DEV=true nix develop -c pnpm backend:test_only` all pass. The full
backend suite is a local requirement of the backend and backend-testing skills,
not an inferred CI gate. No schema migration is changed, so `backend:verify`,
ERD export and API regeneration are not required unless execution discovers a
new trigger. Repeat the caller/documentation scan and inspect the final diff
for data-mutating startup code, shared behavior deletion or stale active-runner
instructions. Tests prove disposable fixture preservation; no production query
or release is part of this proof.

Sizing: approximately 5–10 minutes of coherent deletion, proof adaptation and
documentation edits, plus the required full backend-suite wait and delivery
gates. This is one caller closure and one proof loop; splitting classes, tests
or documentation into separate slices creates incomplete intermediate cleanup.
The full-suite wait is the stated test-time exception to the project's five-minute
target and ten-minute hard limit. If active editing exceeds ten minutes, stop
and refine the remaining plan from the concrete cause; the exception does not
cover implementation thrash or scope growth.

Safe stopping point: this slice delivers complete retirement with ordinary
behavior verified. Before proof succeeds, keep attempt-owned changes isolated;
do not publish partial cleanup that breaks compilation or loses preservation proof.

## Execution gates and current decisions

- Follow AGENTS.md and dough-execute-plan delivery: Jidoka, a fresh independent
  dough-post-change-refactor agent, API generation only if triggered, coordinator
  `./scripts/run.sh pnpm format:changed` once, plan update, commit with the
  check-only lint hook, and authorized publication with asynchronous CI repair.
  Implementers/refactorers run neither formatting nor standalone `lint:changed`.
- Preserve production and Development data; use the linked worktree's disposable
  test environment. Do not turn cleanup into deployment or production observation.
- Retain plan and source for retrospective/wrap-up after execution; do not write
  execution-completion records or delete spent planning history during planning.
- No additional Structure slice is justified: all removals belong to the single
  startup-retirement outcome. Shared owners remain; no new model accumulates.
- Preparation review found no remaining slice-specific concern. Readiness must
  be recorded against this source and plan after writing; it grants no execution
  or publication authority.
