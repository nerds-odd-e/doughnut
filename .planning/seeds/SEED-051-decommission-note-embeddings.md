---
id: SEED-051
status: dormant
planted: 2026-09-28
planted_during: owner request to retire semantic search after a brief alternatives review
trigger_when: prioritizing non-urgent feature cleanup and reducing ongoing operating cost
scope: medium
---

# SEED-051: Retire costly, underused note semantic search

## Why This Matters

The owner reports that hosted note semantic search performs poorly and provides
little practical value for its recurring embedding work and maintenance. Users
can now check out notebooks locally and work in modern IDEs. After the brief
alternatives review, the owner confirmed complete retirement without a
replacement.

## Decision

The owner's acceptance rule is deletion: remove the feature's code, tests,
documentation, production data, and data structures completely. Do not replace
them with disabled implementations, tests asserting the feature's absence, or
documentation saying it was removed. Git supplies the history.

The alternatives decision is closed. No further rescue investigation or
replacement implementation is included. This seed is temporary preparation
input: remove it and other spent execution records at wrap-up instead of
turning them into permanent historical or research documentation.

## Story Decomposition

<a id="story-1"></a>

### Decommission note embeddings and semantic search

**Identity:** SEED-051#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/009-retire-note-embeddings/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"c2a8037a896eeaf30b73472eb48cf5e166c1c7c33e345fdc2256d0e6a62b90bc","plan":"a92f6f0d272013a6f6f10bc9512fe06131871ec85551e4fb8ab34bcbc60e17eb"}}
```

**Slice plan:** [Retire note embeddings and semantic search](../slice-plans/009-retire-note-embeddings/PLAN.md)

**Goal**

Donut users continue ordinary note discovery and local notebook work while the
operator completely retires hosted note/query embeddings and semantic search,
including production storage and operating dependencies. Current dependent
features work through the remaining capabilities and describe them accurately.

**Scope and acceptance criteria**

- Delete semantic switches and result requests from new-note suggestions and
  relationship/wiki-link target search. Keep the current literal title/alias
  matching, ranking, folder/notebook hits, search scopes, and target selection.
  Existing access and trash boundaries still apply.
- Delete the Notebook Indexing section and reset/update controls. Delete both
  note semantic-search operations and both notebook index operations from the
  backend and generated API contracts, and update their callers.
- Delete embedding generation, scheduled/manual refresh, query generation,
  persistence, production/non-production vector search, and feature-only
  implementation, dependencies, fixtures, mocks, tests, and runner references.
  Remove feature-only branches from shared code and tests.
- Delete obsolete documentation, glossary entries, comments, examples,
  configuration, and operational instructions. Remove feature-only documents;
  edit shared documents to describe the remaining current behavior positively.
- Delete every persisted note embedding in production, including accumulated
  previous versions. Remove the embedding tables, columns, vector indexes,
  constraints, and feature-only Cloud SQL settings. Apply equivalent schema
  cleanup to existing non-production installations and fresh installations.
  Production cleanup is part of completion, not a deferred follow-up.
- Remove embedding schema definitions from the maintained baseline and remove
  obsolete feature migrations through the safe Flyway cleanup lifecycle.
  Transition migrations, operational scripts, and temporary verification
  artifacts needed for rollout are temporary: remove them after their required
  application is verified. The current tree must not retain obsolete DDL as
  historical documentation or keep creating then dropping the former schema.
- Update every current consumer affected by removal, including MCP services,
  generated clients, shared callers, tests, and documentation. In particular,
  `find_most_relevant_note` already calls literal search: retain its working
  behavior, remove semantic/embedding claims, and describe its title/alias
  matching positively. Preserve other MCP tools and the existing notebook
  checkout/publication, content, relationship, and learning workflows.
- Preserve shared dependencies with independent current uses: the OpenAI
  client/SDK, token-budget utility used by question generation and focus
  context, and literal search's result-distance ranking. Authored-reference,
  property, and alias indexes, image embedding, and semantic recall terminology
  belong to other features; remove this feature's responsibilities from shared
  artifacts without damaging those independent uses.
- Leave no disabled controls, feature-only flags, tombstone endpoints, dead
  wrappers, compatibility shims, negated feature tests, retirement notices, or
  historical feature/design/research records. Git is the historical record.

**Verification approach**

Delete tests whose subject is the retired feature. Preserve or adapt tests
that prove current dependent capabilities, such as title/alias target selection
and MCP results, using positive examples. Do not convert semantic-search tests
into permanent assertions such as "semantic search is unavailable", "the
control is absent", or "no embeddings are generated".

Verify deletion through change review, a focused repository inventory, and
inspection of production data/schema/configuration after cleanup. These are
completion evidence during execution, not a new permanent absence-test suite
or retirement document. The examples below describe surviving product
contracts; removal is governed by the acceptance criteria above.

**Key examples**

- A user types an existing title or alias while creating a note or choosing a
  relationship/wiki-link target → matching accessible notes appear and can be
  opened/selected using the existing literal workflow.
- A user broadens search to subscribed notebooks or circles → existing literal
  note and container matches retain their scope, access, and trash behavior.
- A notebook owner opens settings → remaining settings display their current
  purpose and save successfully.
- A note is created or edited, including by Git publication → its content is
  available and ordinary title/alias discovery reflects the change.
- An MCP client calls `find_most_relevant_note` with a matching title/alias →
  the existing literal result is returned and the tool describes it accurately.
- An existing installation completes retirement, or a fresh installation runs
  its migrations → note content, relationships, and learning data continue
  supporting their existing workflows.

**Delivery constraints**

- Production replaces application instances one at a time, while Flyway runs
  at `ApplicationReadyEvent`. Retire every deployed embedding reader/writer
  before removing schema/configuration. Expect separate release boundaries for
  stopping use and dropping storage; this remains one retirement story.
- The baseline defines `embedding_raw VARBINARY(6144)`; production code expects
  an `embedding` VECTOR column. The production vector setup is documented
  operationally rather than reproduced by a current migration. Inspect actual
  deployed schema/indexes and feature-flag ownership before destructive
  cleanup. The earlier read-only Cloud SQL request failed because GCP
  credentials need reauthentication; production cleanup remains unverified.
- The owner's explicit removal requirement supersedes the earlier draft's
  instruction to retain obsolete historical migrations. Coordinate baseline
  DDL and migration removal with Flyway repair/squashing and verified rollout
  state so existing installations upgrade safely and fresh installations use
  only the current schema. Temporary upgrade machinery is not a permanent
  exception to complete removal. Preserve Flyway metadata needed to operate
  the current database correctly.
- Current Accepted ADRs 0002 (Git-native notebook synchronization), 0004
  (OKF-compatible notebook Markdown), 0005 (web/API routes), and 0007
  (environments and isolation) govern preserved workflows, authored indexes,
  contracts, and environment ownership. No retirement conflict was found.
  Delete ADR 0001's obsolete semantic-search glossary entry; its description
  does not require retaining the feature.

**Excluded work**

A replacement search service, another database engine, a new local search tool,
full-text body search, performance rescue experiments, and broader search
redesign are outside this story. Update existing dependencies for their current
behavior without adding replacement semantic capabilities.

**Dependencies and stopping points**

No other queued story is a prerequisite. After all embedding use is retired,
the product can operate while storage cleanup awaits its safe release boundary.
That is an intermediate stopping point only. Completion requires verified
production data/schema removal, dependent-feature updates, deletion of obsolete
code/tests/docs/schema definitions and spent transition artifacts, and removal
of this seed and other spent planning records through story wrap-up.

**Effort hypothesis:** several hours of engineering, low confidence until
execution planning resolves verification and production cleanup; elapsed
delivery includes the separate release boundaries.

**Open decisions**

None. The owner confirmed complete deletion, production cleanup, dependent
feature updates, no replacement or negation artifacts, and Git-only historical
retention. Remaining production facts and rollout sequencing belong to
execution preparation.

## Ordering and Scope Reduction

Keep the story queued as non-urgent maintenance, preserving unrelated priority
and Taken work. The owner requested the linked slice plan for this retirement
outcome. Implementation has not started. Keep the preparation draft available
for review until its disposition is decided.

## Preparation pointers

These pointers support execution preparation and leave with this seed at wrap-up:

- `backend/src/main/java/com/odde/donut/services/EmbeddingMaintenanceJob.java`
  and `NotebookIndexingService.java`: scheduled and manual refresh.
- `backend/src/main/java/com/odde/donut/entities/repositories/NoteEmbeddingJdbcRepository.java`:
  storage, production query, and schema variant assumptions.
- `backend/src/main/java/com/odde/donut/controllers/SearchController.java` and
  `NotebookController.java`: semantic search and index operations.
- `mcp-server/src/tools/find-most-relevant-note.ts`: literal search consumer
  whose description currently claims semantic matching.
- `backend/src/main/resources/db/migration/V100000000__baseline.sql` and
  `docs/gcp/prod_env.md`: local schema and production vector setup.
- `backend/src/main/java/com/odde/donut/configs/FlyWayFreeVersionRealMigration.java`
  and `infra/gcp/scripts/perform-rolling-replace-app-mig.sh`: migration timing
  and rolling replacement.
- Relevant current decisions: `docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md`,
  `docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md`,
  `docs/adrs/0005-web-routes-accepted.md`, and
  `docs/adrs/0007-environments-and-isolation-accepted.md`.
