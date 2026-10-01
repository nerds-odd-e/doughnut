# Numbered-property consolidation execution basis

**Identity:** SEED-063#story-2

Required context for [the executable plan](PLAN.md).

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
| Family transformation can share existing syntax/codec (slices 2–5) | Read `PropertyKeyNaming`, `FrontmatterPropertyValues`, `FrontmatterInPlaceEdit`, and `NoteContentMarkdown`: numeric suffix >=2 exists; scalar/one-level list parsing and in-place edits exist; append alone does not force exact-family lists. | Slices 2–5 prove the new contract; no claim it already exists. |
| Destination uniqueness includes inactive rows (slice 8) | Read `V300000352__add_property_value_to_trackers_and_property_index.sql`; then queried the migrated isolated MySQL 8.4.11 schema: exact five-column unique key, both focus columns `varchar(255)` with `utf8mb4_0900_ai_ci`, no active filter. | Slice 8 includes collation-equivalent values; reject unrepresentable focus mappings before mutation. |
| Spring schema migration finishes before the proposed listener consumes services (slice 13) | Read `FlyWayFreeVersionRealMigration`: synchronous `ApplicationReadyEvent`, highest precedence, `repair()` then `migrate()`. Search other listeners: no existing property migration/startup owner. | Slice 1 observes the real event, consumer and database rather than assuming injection/order. |
| Complete accepted operations can retain identities and atomically append (slices 4, 10–13) | Read `AcceptedWebChangeService.apply` through `commitIfChanged`, `AuthoredNoteDocumentPersistence.persist`, and `JdbcNotebookGitRepository`: locked operation, flush, derive, append and store share the transaction; unchanged root returns without append. | Slice 1 establishes service wiring; slices 4, 10–13 consume it and prove this migration. |
| Stored-content enumeration includes Trash (slices 6–7) | Read `NoteRepository.findAllByNotebookIdOrderByIdAsc` and `MemoryTrackerRepository.findByNote_IdIn`: complete collections exist; interactive property rename refuses unavailable notes. | Slices 6–7 prove migration enumeration and restore. |
| Deleting a duplicate has dependent-data effects (slice 8) | Read `MemoryTrackerService.delete`, `MemoryTrackerDeleteControllerTest`, and ERD: tracker cascades to recall logs/prompts and batch requests; prompt-to-conversation uses SET NULL. | Slice 8 seeds the full FK closure and observes survivor/deletion effects in MySQL. |
| Renamed property selectors require authored rewrites (slices 9–10) | Read `WikiLinkPropertyMatch`, `PortablePath`, `WikiLinkMarkdownRewrite`, `AuthoredNoteDocument`, and `NoteReferenceService.notebooksToLock`: exact-key live matching and multi-notebook coordination exist; index refresh cannot repair old authored keys. | Slices 9–10 consume the current resolver and prove post-rewrite resolution and publication. |

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

## Preparation baseline

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

## Plan review and learnings

The sequence uses one exact-family content/mapping owner, one tracker
responsibility and one accepted-change owner. Scalar, sparse/list, learner,
Trash and duplicate fixtures extend the same rule; they do not create separate
migration handlers. Reference preservation uses the existing multi-notebook
contract. The early probe bounds schema/service/startup uncertainty before
broad implementation; activation stays last for stop-safe delivery.

No remaining unbounded premise or product decision was identified in this
preparation review. No slice was done at preparation and no live-data census
is claimed. Current statuses and accepted proof live in PLAN.md and EXECUTION.md.
If a probe or outside-in observation contradicts the basis, preserve its evidence, stop the
dependent path and revise this same plan before continuing.

## Verification and delivery gates

The [backend testing skill](../../../.agents/skills/backend-testing/SKILL.md)
requires all backend unit tests, not selected classes. Use
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` at non-migration-code
boundaries; use `pnpm backend:verify` under the same Nix prefix when migration
code is involved. Existing test patterns are starting points; each new test
invokes the migration operation, uses real collaborators and observes its
public results. Do not claim that a green existing controller test proves the
new startup caller. No frontend/API-signature changes are planned.

There is no schema change planned, so no new Flyway version or ERD regeneration
is required merely for DML. If execution discovers a necessary schema change,
stop that path, replan and apply the migration/ERD rules. Do not edit committed
migrations or add a placeholder gate against the owner's explicit direction.

Execution follows AGENTS.md's required Jidoka → fresh
`dough-post-change-refactor` agent → API generation if needed → coordinator
`./scripts/run.sh pnpm format:changed` once → update plan → commit with the
check-only lint hook → push. Implementers and refactorers run neither that
formatter nor standalone `lint:changed`. Execute-plan owns asynchronous CI
repair; managed increment delivery owns observer setup and publication.
