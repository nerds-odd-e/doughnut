# Publish web saves from hash-only notebook assembly

Work item: **SEED-037#story-3**. Status: **planned**.
Source: [refined story](../../seeds/SEED-037-note-save-cost-independent-of-folder-count.md#story-3).
Preparation: owner-requested default checkout `/Users/terryyin/git/doughnut`,
`main`; leave these changes uncommitted for review. Planning does not Take the
story, change queue order, authorize implementation or publish anything.
Inspected baseline: `4f0b551959c8a5b9bbc4625fe0f13f3502a00a07`.

## Outcome and boundaries

One complete Portable-tree encoder replaces change capture and partial-tree
derivation. A web save assembles paths and Git blob IDs without fetching all
note/attachment bodies, publishes only needed objects and remains synchronous
and atomic. Initial cutover and reset still fetch the content needed to create
a complete repository. Local publication still checks projection drift and
preserves identity evidence.

Deliver today's legacy attachment representation. LFS conversion, GCS APIs,
the 10 MiB limit, attachment migration, stored note hashes and further
round-trip optimization are later stories. No new move/upload capabilities,
asynchronous workers, caches, indexes, generic storage framework or ADR.
Approximately 500 fewer production lines is a simplification hypothesis, not
permission to delete behavioral coverage or a numeric acceptance threshold.

## Architecture and existing solutions

- Follow [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  [the LFS contract](../../../docs/notebook-git-lfs.md), and North Star topics
  [One format boundary](../../NORTH-STAR.md#one-format-boundary),
  [One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
  and [Attachment storage transition](../../NORTH-STAR.md#attachment-storage-transition).
  The SQL query is a current-storage adapter returning the Git ID of represented
  bytes. A later LFS pointer's Git blob ID is not its payload SHA-256; neither
  bucket URLs nor unchanged bucket reads belong in assembly. Do not implement
  hypothetical LFS branches now.
- PFE: reuse `NotebookGitTreeEncoder`, `NotebookGitPortablePath`,
  `PortableTreeEntry`/`PortableTreeReadmeMarkdown` and the native directory
  serializer. They already own folder prefixes, UTF-8, null-as-empty notes,
  README production, `.keep` and Git ordering. Change row input/content loading,
  not the format rules; no independent SQL codec.
- Shared callers inspected: `NotebookGitCutoverService.buildRepository` serves
  initial binding and reset; `NotebookGitProjection.requireMatchingAcceptedTree`
  serves publication comparison over locked identity-bearing entities;
  `AcceptedWebChangeService.commitIfChanged` serves all integrated web domain
  operations. Cutover needs all missing bytes, comparison needs IDs only, web
  append needs only missing bytes. Keep one assembly with these distinct uses,
  rather than retaining full/derived encoders or a per-operation selector.
- Reuse `NotebookGitTreeContent`, `NotebookGitCommitBuilder` and the existing
  JDBC object store's transaction and batched inserts. Its existing-ID query
  is currently private to insertion. Expose/reuse a narrowly scoped ID-only
  presence operation if necessary to avoid fetching reused blobs before
  deduplication. `JdbcNotebookObjectReader.open` currently loads object bytes;
  an inherited `has` implementation that calls it would not prove payload-free
  presence. No new persistence/index layer is justified.
- Reuse `SqlStatementCallLog` and the existing controller cost fixture.
  Currently `fetchedObjectBytes()` counts only accepted-object bytes, not all
  SQL results; the representative fixture has tiny notes/one tiny attachment.
  Neither currently proves the story's <1 MB result-value budget. Extend that
  observation rather than introducing another profiler.
- The story's existing owner decision replaces the *web* untouched-drift rule
  in the synchronization document. No current ADR has a numbered "drift
  decision 4". Update that document at delivery, retain Git authority and the
  separate local-publication drift refusal, and add no architecture approval
  ceremony. No North Star change is needed.

## Proof commands and prerequisites

All new tests below are planned, not already-passing evidence. Test locations
are under `backend/src/test/java/com/odde/donut/` unless stated otherwise.

**B — required backend gate at every slice:**

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

**R — representative local experiment, before production changes and after
the final switch, forcing execution rather than accepting cached tests:**

```sh
git rev-parse HEAD
DONUT_MEASURE_REPRESENTATIVE_SAVE_COST=true CURSOR_DEV=true nix develop -c pnpm backend:test_only --rerun-tasks
```

Record the revision plus any uncommitted harness changes, `SELECT VERSION()`
and connection charset, fixture sizes, five samples/median, JDBC execution
count and result-value byte count in this plan. Use the same harness, machine,
MySQL 8.4 test database, transaction boundary and cold entity/repository state
for both runs. Exclude setup and assertion queries. Count each returned column
value once, with explicit byte encoding, including projection rows and Git
objects; do not label this application-value accounting as network-wire bytes.
Use a temporary disposable test fixture, never production/shared data.

Historical evidence: the source records the SQL-hashed prototype at commit
`458496764f931f05b0d46d955e1bbbebf04beefc` (56 ms / 29 JDBC / 885 KB versus
85 ms / 62 JDBC / 173 KB, median of three). It supports choosing this design,
but is not current proof: no matching retained harness establishes today's
complete byte accounting. Slice 1 must obtain the baseline before slice 2
changes production behavior. A failed/unavailable baseline stops the dependent
performance claim and changes this plan before proceeding.

Concrete storage assumption: MySQL 8.4 can hash the exact blob bytes with the
native Git header and byte length, including null/empty note content,
multibyte UTF-8 and arbitrary binary bytes. Slice 1's real-database parity test
runs under **B** and **R**, comparing SQL IDs with JGit `OBJ_BLOB` IDs from
`PortableTreeEntry`; record its result. A mismatch stops the SQL-backed path,
not an invitation to silently fall back to transferring every payload.

**E — Git-focused end-to-end compatibility after the web switch:**

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/cli/cli_notebook_*.feature'
```

This selects the existing notebook Git clone, reset, edit, create, rename,
move, trash, relocation, publication and reconciliation features, not the
whole E2E suite. Extend a feature only for a missing user-level promise;
controller tests own SQL cost, exact byte contracts and failure injection.

## Ordered slices

Target about 5 minutes including focused proof and cleanup; estimates below
are hypotheses. Each >5-minute slice was scrutinized for hidden outcomes.
Full **B**, representative **R** and **E** may exceed the 10-minute limit in
test execution alone: that is the explicit test-wait exception, not an
exception for coding/debugging. At 10 minutes of non-wait work, stop, preserve
attempt-owned work and evidence, and refine the affected boundary before
retrying. Never deliver a red suite or silently drop a mapped promise.

### 1. Establish exact hash inputs and a comparable save-cost baseline

Type: **Structure**. Status: **planned**. Estimate: **7–9 minutes plus test wait**.

Internal change: add narrow note/legacy-attachment row reads containing row
identity, placement, filename and SQL Git blob ID; no content in those result
rows. Prove null → empty and UTF-8 byte-length semantics against the existing
codec on real MySQL. Add only the row types required by the next slice.
Extend the existing JDBC observer to count returned value bytes and its
representative fixture to depth 12, 4,000 folders, 11,000 1 KB notes and
20 × 500 KB binary attachments. Do not switch a production caller yet.

Enables immediately: slice 2 can assemble real repositories from proven IDs
without inventing encoding rules or losing the pre-change cost observation.
The parity probe and baseline are the single prerequisite gate for that
storage-input substitution; failure leaves current saves unchanged.

Proof: a database-backed parity case covers ASCII, multibyte/emoji, empty/null
notes, binary NUL/invalid UTF-8 and empty attachments. Existing controller
download/reset/save assertions remain green under **B**. Run **R**, retain
its baseline and inspect the observer's setup/observation boundary before
changing production readers. Do not assert the future byte budget yet.

### 2. Build and reset complete history from one ID-based assembly

Type: **Behavior**. Status: **planned**. Estimate: **7–9 minutes plus test wait**.

Behavior: a notebook with notes, binary files, nested empty folders and
Readmes → initial binding or history reset → a downloadable repository with
the same expected paths, exact bytes and existing reset/identity semantics.

Change the full-tree path to consume the descriptors from slice 1, with
README/marker bytes supplied by their existing codec and note/attachment
content fetched by row identity only when materialization requires it.
Keep the already-loaded-note publication-comparison caller on this same
assembly, retaining the identity-bearing entity loading it needs. The old web
derive caller may remain only until slice 4; do not add a second serializer.

Proof: extend `NotebookGitTreeEncoderTest` with independent expected entries
and use `NotebookGitHistoryResetControllerTest` plus existing initial-binding
controller coverage to decode downloaded bundles, not to compare the encoder
to itself. `NotebookGitProjectionDriftControllerTest` and
`NotebookGitConcurrentProjectionDriftControllerTest` retain local-publication
refusal after the shared encoder change. Run **B**. Byte materialization is
expected for reset/cutover and does not fail the web-save budget.

Safe stop: cutover, reset and comparison use the ID-based assembly; existing
web saves still work. No LFS behavior is claimed.

### 3. Select only missing objects from an assembled tree

Type: **Structure**. Status: **planned**. Estimate: **7–9 minutes plus test wait**.

Internal change: beside the existing assembly/writer, compare the candidate
root top-down with accepted trees, stop at equal subtree IDs and collect only
needed trees/blobs. Read each differing accepted directory at most once.
Use ID-only presence in the target repository before hydrating candidate
blobs or descending into a relocated, already-stored subtree. Reuse the
store's batched existence logic; do not perform one content fetch per file.
An object in another notebook's repository is not necessarily present in this
one. Deleted paths need no content, and identical blobs need one materialization.

Enables immediately: slice 4 publishes the whole projected state without
turning a one-note edit or a folder relocation into payload transfer. Existing
web behavior remains on the old path until that switch.

Proof: extend `NotebookGitCommitBuilderTest`/encoder tests using real in-memory
Git repositories: edit, deletion, unchanged root and 1,000-note subtree
relocation yield independently decoded expected trees; record content loads
at the materialization boundary and assert none for reused blobs. Verify the
ID-only JDBC presence path under the existing object-store SQL observation
tests; a payload-loading `has` is not sufficient. **B** must remain green.
For mode-only tree differences, establish Portable path/blob equality without
declaring a content change or needing all blob bytes; preserve that existing
no-op contract for slice 4.

### 4. Publish every integrated web operation through full assembly

Type: **Behavior**. Status: **planned**. Estimate: **8–10 minutes plus test wait**.

Behavior: a complete web operation under existing notebook locks → flush and
assemble each affected notebook → one append per changed Portable tree, with
projection, native objects, head and private identities committed or rolled
back together. Adopt existing projected drift in that new web commit, as the
source decision requires; equivalent content/file-mode-only differences append
nothing. No operation-type switch or partial integration is introduced.

Use slices 2–3 in `AcceptedWebChangeService`. Remove change capture, the
derive caller, changed-file/folder and accepted-directory helper classes,
relocation/unresolved-subtree machinery and their dead wiring in the same
increment. This deletion belongs with the switch, not a later cleanup layer.
Replace obsolete tests that assert no whole-projection queries or old web
drift policy. Preserve their path/content/identity promises at controllers;
remove self-comparison oracles only once independently expected outcomes are
covered. Update `docs/note-content-saving.md`, the synchronization contract's
domain-operation section and attachment documentation's encoder description
to match delivered behavior, without claiming LFS is already implemented.

Proof: run **B**, **R**, **E** and the mappings below. Compare **R** with the
slice-1 baseline: <1,000,000 returned value bytes, no more JDBC executions,
no median latency regression. Record latency as observed, never a CI timing
assertion. Changed-depth paths read at most 13 accepted trees; unchanged
note/attachment payloads are absent from reads, including path-only relocation.
The new commit and final tree must remain readable from a freshly reopened
repository; rollback assertions use separately committed readers.

Sizing review: this is one inseparable replacement of the publication path
and its obsolete machinery, not separate implementations for notes, folders
and attachments. Necessary helpers/probes were placed earlier. If converting
callers/tests reveals extra domain policy or >10 minutes of non-wait work,
stop and refine; do not ship half the owner on each strategy.

Safe stop: the selected story is implemented only once these results and the
single-encoder cleanup are verified. Keep story 2 and LFS rollout out of scope.

## Promise-to-proof ownership

| Promise | Owner and observation |
| --- | --- |
| SQL IDs equal represented bytes, not character counts | 1: real-MySQL parity against JGit, including null/empty/multibyte and arbitrary binary |
| One encoder, correct initial/reset history and publication comparison | 2: downloaded bundle entries; reset/identity and local-drift controller tests |
| Missing-only materialization and relocation reuse | 3, then 4 at controllers: observe ID-only existence and zero unchanged payload loads; verify moved subtree IDs |
| Canonical no-op, including accepted executable files | 4: `NotebookGitWebContentSaveControllerTest` and `NotebookGitWebContentSaveFileModeControllerTest`; head and native-row count unchanged |
| Note/folder changes, Readmes, `.keep`, supported cross-notebook operations | 4: existing web note/folder/trash/README and `NotebookGitWebFolderCrossNotebookMoveControllerTest` assertions of expected paths/content/identities; adapt query-shape assertions, not business outcomes; E |
| Binary attachments preserved and guards unchanged | 2/4: attachment publication/independence/relocation controller tests and decoded bundle bytes, no new file workflow |
| Web adopts drift; local publication still refuses it | 4: replace old web drift example with both projected contents in one child commit; 2/4 retain local projection-drift tests |
| Atomic projection, native objects, accepted heads and identities | 4: `NotebookGitWebContentSaveAtomicControllerTest`, multi-notebook operation/rollback tests; real late failure and committed-reader observation |
| Quantified cost, no latency regression | 1 baseline → 4 comparison: extended `NotebookGitWebContentSaveCostRepresentativeExperimentTest`, **R**; stable count assertions in `NotebookGitWebContentSaveCostControllerTest` |
| Capture/derive removal without a second format model | 4: reference search/code review plus **B** and **E**; record production diff size without treating it as a quota |

## Delivery and review gates

This turn writes preparation only; no product tests have been run and no slice
is done. Once execution is separately authorized, use the current
`dough-execute-plan` workflow: Jidoka/proof acceptance, fresh independent
`dough-post-change-refactor` agent, API regeneration only if signatures change,
coordinator formatting once with `./scripts/run.sh pnpm format:changed`, plan
evidence update, then authorized commit/publication and CI observation. Do not
infer publication authority from this plan or its preparation assessment.

Construction review: four slices, two temporary Structure increments each
immediately enabling a named Behavior. One common tree model handles all web
operations; no LFS scaffolding or per-operation policies accumulate. No
remaining slice-specific concern identified in this review. The bounded SQL
and baseline gates are required execution evidence, not claims already proved.
No separate slice-plan refinement pass is needed after these construction
adjustments. Readiness is recorded by the backlog preparation tool against
the reviewed source and plan, not by this prose.

## Current learnings and accepted proof

- Planning found the current byte observer and large-fixture payload sizes
  insufficient for the stated budget; slice 1 owns that gap before replacement.
- Existing mode-only no-op coverage and payload-loading object-reader behavior
  constrain the diff/materialization design; neither may be lost in cleanup.
- No implementation evidence yet. Record actual commands, observations,
  results and consequential deviations here during authorized execution.
