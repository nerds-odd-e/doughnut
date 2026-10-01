# Consolidate numbered properties while retaining their learning

**Identity:** SEED-063#story-2
**Source:** [Existing numbered property keys become one list, and their trackers follow](../../seeds/SEED-063-track-property-values-separately.md#story-2)

## Goal and authority

Authors see one meaningful property containing their associations. Learners
retain the identity, history and next recall of each distinct tracked
association. The owner permits dropping redundant trackers at a duplicate
destination; the retained tracker keeps its own history without merging.

This plan is preparation for the owner's request to make the story ready.
It authorizes neither implementation nor deployment. The story remains queued.
All slices below are planned; no migration has been run by this preparation.

## Scope and current decisions

- Use the accepted numeric-family rule: suffix >=2, list-capable base,
  including `url`; preserve scalar-only structural and word-suffix keys.
  Group authored base names exactly; structural recognition may ignore case,
  but that does not authorize merging case-distinct authored families.
- Produce a list even for a missing base with one remaining value. Existing
  base values come first, then numeric suffix order and each source list's
  authored order. Keep the first occurrence of an equal value. Preserve
  unrelated YAML, body text and authored value meanings using the shared codec.
- Map scalar trackers with value `''` to their original scalar value; list
  trackers follow their existing item. Include all learners and persisted
  tracker states, not just available notes or active trackers. Do not create
  new learning records or distribute one scalar tracker across several items.
  An unrepresentable source, orphan focus or ambiguous mapping is reported
  before that operation changes anything; leave that operation unchanged.
- For a duplicate final database destination, retain an existing destination
  tracker; otherwise retain the lowest-ID member. Delete other members through
  the existing deletion owner before conflicting updates are flushed. Compare
  with the database's actual uniqueness/collation semantics. Preserve every
  distinct learner/type destination and all survivor history and schedules.
- Retarget authored wiki selectors that uniquely resolve to a removed key to
  the retained base. Preserve visible text, source scope and unresolved or
  ambiguous references. Include changed referrers and any trackers of their
  rewritten list values in the complete operation. Derive and recheck the
  complete affected notebook set before mutating; lock in ascending order.
- Reuse `AcceptedWebChangeService` and `AuthoredNoteDocumentPersistence` for
  the complete operation. Append one Donut System descendant per changed
  bound notebook, atomically with content, tracker changes and derived state.
  Preserve existing unbound behavior without adding another publication path.
- Choose a temporary Spring startup migration **after** the existing Flyway
  listener, using injected services and separately committed notebook
  operations. No toggle, placeholder gate, schema change, new public API or
  migration UI is needed. Install the live startup caller only after its
  operation is proved. Retry based on remaining legacy content; append nothing
  when the accepted tree is unchanged. Do not wrap the complete run in one
  outer transaction or rely on self-invoked transactional methods.
- The owner expects no production duplicates but has no established census.
  Correctness must not depend on that expectation. Production counts are not
  an execution-readiness gate. Do not connect to or mutate production during
  preparation or the isolated probe. Release remains separately authorized.
- When Java migration code is introduced, select and queue the owner-required
  cleanup story for removal after confirmed migration completion. Do not
  remove it merely because the unit fixtures passed, and do not count reported
  unmigrated operations as completed.

Deferred: history merging, word-suffix migration, recurring author actions,
UI, removal of structural numbered-key helpers, and speculative schema
relaxation. These are deferred delivery promises, not new rejection rules.

## Existing solutions and architecture

PFE inspected both property editors and the backend domain owners. Reuse
`PropertyKeyNaming` for suffix recognition and structural meaning, and extend
the existing frontmatter editing owner for consolidation. Do not use
`isReservedStructuralKey` unchanged as the eligibility rule: it also excludes
`url` from learning indexes. Existing append is case-insensitive and can leave
duplicates as a scalar; it therefore needs an exact-family list operation,
not blind repeated calls that lose the selected semantics.

`PropertyMemoryTrackerService.followPropertyValue` demonstrates scalar-to-item
preservation but does not rename keys, and the interactive rename excludes
Trash and checks active trackers only. Extend the property-tracking
responsibility for the complete migration mapping rather than chaining these
interactive actions. Reuse `MemoryTrackerService.delete` for allowed redundant
tracker removal. Avoid a second tracker-history or suffix representation.

Accepted [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and [domain operation ownership](../../../docs/notebook-git-synchronization.md#domain-operation-ownership)
require one accepted-change boundary, forward history and a precomputed,
reverified notebook set. [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
owns the codec and exact authored property selector. [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
allows unexpected infrastructure failures to surface loudly; add no generic
catch-and-continue machinery. [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires disposable isolated test data. Follow the existing North Star topics
[One accepted-change boundary and Moving and retiring](../../NORTH-STAR.md).
No new architectural direction or ADR exception is needed.

## Decisive premises and observations

Static observations were made at preparation base `5aba10754238274f5a77dbad04de7607d6bacc2c`.
They settle the stated code contracts, not runtime integration or production
data. Keep the initial probe early because registration and storage observations
require a state-changing disposable run.

| Premise and consumer | Literal observation and result | Runtime proof owner |
| --- | --- | --- |
| Family transformation can share existing syntax/codec (slices 2–4) | Read `PropertyKeyNaming`, `FrontmatterPropertyValues`, `FrontmatterInPlaceEdit`, and `NoteContentMarkdown`: numeric suffix >=2 exists; scalar/one-level list parsing and in-place edits exist; append alone does not force exact-family lists. | Slices 2–4 prove the new contract; no claim it already exists. |
| Destination uniqueness includes inactive rows (slice 7) | Read `V300000352__add_property_value_to_trackers_and_property_index.sql`; then queried the migrated isolated MySQL 8.4.11 schema: exact five-column unique key, both focus columns `varchar(255)` with `utf8mb4_0900_ai_ci`, no active filter. | Slice 7 includes collation-equivalent values; reject unrepresentable focus mappings before mutation. |
| Spring schema migration finishes before the proposed listener consumes services (slice 12) | Read `FlyWayFreeVersionRealMigration`: synchronous `ApplicationReadyEvent`, highest precedence, `repair()` then `migrate()`. Search other listeners: no existing property migration/startup owner. | Slice 1 observes the real event, consumer and database rather than assuming injection/order. |
| Complete accepted operations can retain identities and atomically append (slices 3, 9–12) | Read `AcceptedWebChangeService.apply` through `commitIfChanged`, `AuthoredNoteDocumentPersistence.persist`, and `JdbcNotebookGitRepository`: locked operation, flush, derive, append and store share the transaction; unchanged root returns without append. | Slice 1 establishes service wiring; slices 3, 9–12 consume it and prove this migration. |
| Stored-content enumeration includes Trash (slices 5–6) | Read `NoteRepository.findAllByNotebookIdOrderByIdAsc` and `MemoryTrackerRepository.findByNote_IdIn`: complete collections exist; interactive property rename refuses unavailable notes. | Slices 5–6 prove migration enumeration and restore. |
| Deleting a duplicate has dependent-data effects (slice 7) | Read `MemoryTrackerService.delete`, `MemoryTrackerDeleteControllerTest`, and ERD: tracker cascades to recall logs/prompts and batch requests; prompt-to-conversation uses SET NULL. | Slice 7 seeds the full FK closure and observes survivor/deletion effects in MySQL. |
| Renamed property selectors require authored rewrites (slices 8–9) | Read `WikiLinkPropertyMatch`, `PortablePath`, `WikiLinkMarkdownRewrite`, `AuthoredNoteDocument`, and `NoteReferenceService.notebooksToLock`: exact-key live matching and multi-notebook coordination exist; index refresh cannot repair old authored keys. | Slices 8–9 consume the current resolver and prove post-rewrite resolution and publication. |

Relevant existing proof patterns are
`MemoryTrackerFollowPropertyValueControllerTest`,
`MemoryTrackerUpdatePropertyKeyControllerTest`,
`MemoryTrackerDeleteControllerTest`,
`NotebookGitExistingNoteBatchPublicationControllerTest`,
`NotebookGitWebContentSaveAtomicControllerTest`, and
`NotebookGitWebTrashControllerTest`. Their triggers are existing web/controller
operations, not the migration. The new proof must invoke the migration's public
run/notebook operation boundary with real collaborators and then observe
controller reads, committed database state and downloaded Git content.

## Ordered slices and outside-in proof

Each slice targets about 5 minutes including local edits and focused cleanup.
Slices 3, 7 and 9 are scrutinized cohesive outcomes targeting 5–8 active minutes:
their content/history or transaction promises must be proved together. The
mandatory complete backend suite and isolated process boot are explicit
verification-wait exceptions when they exceed that target; do not use them to
hide more implementation. At >10 active minutes, stop and finer-decompose the
remaining work with recorded learning before continuing.

### 1. Startup services are usable after schema migration
Type: Behavior
Status: planned
Proof: An isolated bootstrap probe delivers the actual startup event; observes
Flyway completion before a consumer invokes the real accepted-change service
and reads the migrated tracker schema. Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.

Behavior: Disposable schema and a representative bound notebook → real event
delivery → injected services can open and close a no-change notebook operation
after schema migration, without an outer Flyway transaction or extra Git commit.
Read current column limits, collation and unique keys in the same test engine.
Keep the probe in the test harness; introduce no live transformation caller.
If ordering, injection, schema or independent transactions differ from the
observed contracts, stop dependent slices and revise this plan. This is early
evidence, not a substitute for the later migration journey.

### 2. One exact-family transformation owns consolidation
Type: Structure
Status: planned
Proof: Pure contract tests on the existing authored-frontmatter boundary prove
the selected numeric grouping/list/ordering rule and preserve unrelated bytes;
existing codec tests stay green under the full backend suite.

Change: Extend the shared frontmatter owner to return transformed content and
the source-to-final-focus mapping consumed immediately by slice 3. Reuse the
suffix domain owner. Unsupported or unmappable shapes yield a diagnostic
without transformed content. No separate preview API or general migration
framework. Retain scalar-only keys and word suffixes as authored.

### 3. A learned scalar family becomes one accepted list
Type: Behavior
Status: planned
Proof: Migration notebook operation → `NoteController`/tracker history reads
and downloaded bundle → list content, retained note/tracker IDs, histories and
next recall, refreshed property/reference indexes, one descendant commit and
no private learning data in Portable content. Run the full backend suite.

Behavior: Learned `example of` and `example of 2` with distinct scalar values →
migrate → both original trackers focus on their values under the base. Apply
content and learning changes through the existing accepted-change transaction.
Keep the startup caller absent until slice 12. Queue the authorized cleanup
story when this slice first introduces temporary Java migration code.

### 4. Sparse and already-listed families use the same rule
Type: Behavior
Status: planned
Proof: Invoke the migration on missing-base/out-of-order suffixes and an
existing base list; observe the selected order and tracker destinations through
the same boundary. Include an untracked `url 2` and retained structural/word
keys in the canonical content contract; run the full backend suite.

Behavior: Absent base or existing list with repeated values → migrate → one
ordered list with stable deduplication; existing list-item trackers remain at
their item, and scalar trackers move only when their original value is known.
Do not treat a tracked list as a scalar or clone its tracker onto all items.

### 5. Every learner's persisted tracker follows
Type: Behavior
Status: planned
Proof: Two learners with distinct tracking states → one migration operation →
both query their original tracker IDs at the new focuses; an unrelated tracker
retains its focus. Run the full backend suite.

Behavior: Other learners and inactive persisted trackers of a migrated note →
migrate → all mapped trackers follow; active-only UI filtering is not reused.
Do not repeatedly assert canonical history fields already owned by slice 3.

### 6. Restoring Trash retains the migrated association
Type: Behavior
Status: planned
Proof: Trash a learned note through `NoteController`, migrate, download its
`_trash` content, undo Trash through the controller → restored list and retained
tracker/history. Use the existing real Trash fixture pattern and full suite.

Behavior: A numbered-family note is already in Trash → migration then restore
→ the legacy convention does not return. Enumerate complete stored content,
not only available notes; do not recreate Trash ancestry rules.

### 7. Duplicate destinations retain one tracker safely
Type: Behavior
Status: planned
Proof: Duplicate mapping → migration → the deterministic survivor keeps its
history/schedule; only redundant trackers and their normal dependent data are
removed. Seed every tracker FK child and the prompt/conversation edge. Include
inactive and collation-equivalent destinations in the real MySQL fixture.
Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.

Behavior: Final destinations collide by the schema's rule → retain the existing
destination tracker, or otherwise the lowest-ID candidate → drop redundant
trackers before updating the survivors. Do not drop different learners/types,
merge histories, or relax uniqueness. Recheck the current FK closure against
`information_schema` before implementing hard deletion. Failure leaves the
complete accepted operation unchanged, including the deletion fan-out.

### 8. In-notebook selectors still resolve after consolidation
Type: Behavior
Status: planned
Proof: A uniquely resolving `#prop:` selector in body/frontmatter → migration →
controller wiki resolution selects the base, visible text is retained, derived
source rows match authored content and downloaded Markdown has the new selector.
For a tracked list item containing the selector, observe retained tracker ID
at the rewritten value. Run the full backend suite.

Behavior: A note refers to a removed suffixed key → migrate the target family
and retarget only resolved selectors in the complete operation. Preserve the
existing resolver's source-scope and ambiguity rules. Use shared reference
rewrites and tracker mapping; add no alternate link syntax or saved destination
authority.

### 9. Cross-notebook referrers publish in the same operation
Type: Behavior
Status: planned
Proof: Target and referrer in separate bound notebooks → migration → both
downloaded descendant trees agree with controller reads and retained learning.
The changed notebook set is computed before locking and reverified under lock.
Run the full backend suite.

Behavior: A resolved selector is authored in another stored notebook → complete
migration operation → one descendant per changed notebook in the same
transaction. Include affected source-value trackers, using the same duplicate
rule. This is system migration authority, not a user web action's ownership
filter. Do not publish a target change before its required reference rewrites.

### 10. Late publication failure rolls back the whole migration
Type: Behavior
Status: planned
Proof: Reuse the existing failing-binding-save/committed-reader pattern at the
migration boundary. After late failure, inspect original notes, tracker
identities/focuses and all deletion fan-out, derived references, accepted heads
and native object rows from a fresh committed reader and reopened bundle.
Run the full backend suite.

Behavior: Failure after content projection and redundant deletion but before
final accepted binding save → all affected notebooks and private learning
remain at their pre-operation state. The injected failure verifies the
business atomicity promise; it adds no generic failure-recovery framework.

### 11. A resumed run changes only remaining legacy content
Type: Behavior
Status: planned
Proof: Run migration twice → stable content, tracker IDs/history and accepted
heads on the second run. Interrupt a two-notebook run after the first committed
operation → invoke again → first remains unchanged, second finishes. Run the
full backend suite with separately committed readers.

Behavior: Already-consolidated content or a partially completed run → retry →
no new duplicate commit/deletion/history reset. Use separately proxied
transactions and derive eligibility from current authored content. A late
infrastructure failure may surface loudly; the next run resumes safely.

### 12. Startup invokes the proved complete migration
Type: Behavior
Status: planned
Proof: Extend slice 1's real event probe to seed a learned numbered family and
consume the actual production-configured runner. Startup after Flyway →
controller/database/downloaded-tree observations satisfy slice 3. Deliver
startup again and concurrently invoke the runner from a separate transaction
→ the unchanged head and survivor prove no double application. Run
`CURSOR_DEV=true nix develop -c pnpm backend:verify`.

Behavior: Application starts with legacy content → after schema migration,
iterate notebooks through the proved operation, with per-operation locks and
fresh reads → eligible content is consolidated. Keep startup synchronous under
the existing failure lifecycle; add no fire-and-forget task. Report remaining
unmappable operations rather than claiming full completion. Document the
startup entry, deterministic duplicate exception, retry and completion check
in the maintained migration/operations guidance. This slice activates the
caller only after preceding behavior is safe.

## Verification and delivery gates

The [backend testing skill](../../../.agents/skills/backend-testing/SKILL.md)
requires all backend unit tests, not selected classes. Use
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` at non-migration-code
boundaries; use `pnpm backend:verify` under the same Nix prefix when migration
code is involved. Existing test patterns are starting points; each new test
invokes the migration operation, uses real collaborators and observes its
public results. Do not claim that a green existing controller test proves the
new startup caller. No frontend/API-signature changes are planned.

Preparation baseline on 2026-10-01:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` completed successfully
against the wrapper-selected disposable database
`doughnut_wt_d9db2098469f4feda1a05f2e1216828a_test`. It migrated that database,
then ran 2,733 tests with zero failures/errors and two skipped tests. The six
named existing proof-pattern classes all ran with zero failures and no skips,
as observed in their `backend/build/test-results/test/TEST-*.xml` files.
This proves the baseline operations consumed by the plan; the migration and
its startup path do not exist yet and remain owned by the planned slices.

The following read-only metadata observation consumed the same migrated test
schema, not Development or Production:

```sh
CURSOR_DEV=true nix develop -c mysql -h 127.0.0.1 -P 3309 -u root doughnut_wt_d9db2098469f4feda1a05f2e1216828a_test -e "SELECT VERSION(); SHOW FULL COLUMNS FROM memory_tracker WHERE Field IN ('property_key', 'property_value'); SHOW INDEX FROM memory_tracker WHERE Key_name = 'uq_memory_tracker_user_note_type_property'; SELECT TABLE_NAME, COLUMN_NAME, DELETE_RULE FROM information_schema.KEY_COLUMN_USAGE k JOIN information_schema.REFERENTIAL_CONSTRAINTS r USING (CONSTRAINT_SCHEMA, CONSTRAINT_NAME, TABLE_NAME) WHERE k.CONSTRAINT_SCHEMA = DATABASE() AND k.REFERENCED_TABLE_NAME IN ('memory_tracker', 'recall_prompt');"
```

Result: MySQL 8.4.11; destination collation/limits and unique key agree with the
table above. The current tracker FK closure cascades to batch requests, recall
logs and recall prompts; deleting a prompt sets the conversation's prompt FK
to NULL. Re-query at execution if the schema has changed. This establishes the
fan-out to fixture, not approval to delete any live data during preparation.

There is no schema change planned, so no new Flyway version or ERD regeneration
is required merely for DML. If execution discovers a necessary schema change,
stop that path, replan and apply the migration/ERD rules. Do not edit committed
migrations or add a placeholder gate against the owner's explicit direction.

Execution follows AGENTS.md's required Jidoka → fresh
`dough-post-change-refactor` agent → API generation if needed → coordinator
`./scripts/run.sh pnpm format:changed` once → update plan → commit with the
check-only lint hook → push. Implementers and refactorers run neither that
formatter nor standalone `lint:changed`. Execute-plan owns asynchronous CI
repair. This preparation landing starts no execution claim or CI observer.

## Plan review and learnings

The sequence uses one exact-family content/mapping owner, one tracker
responsibility and one accepted-change owner. Scalar, sparse/list, learner,
Trash and duplicate fixtures extend the same rule; they do not create separate
migration handlers. Reference preservation uses the existing multi-notebook
contract. The early probe bounds schema/service/startup uncertainty before
broad implementation; activation stays last for stop-safe delivery.

No remaining unbounded premise or product decision was identified in this
review. No slice is done and no live-data census is claimed. If a probe or
outside-in observation contradicts the basis, preserve its evidence, stop the
dependent path and revise this same plan before continuing.
