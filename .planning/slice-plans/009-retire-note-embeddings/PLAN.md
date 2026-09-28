# Retire note embeddings and semantic search

**Identity:** SEED-051#story-1  
**Source:** [refined story](../../seeds/SEED-051-decommission-note-embeddings.md#story-1) and the owner's complete-deletion acceptance criteria.  
**Preparation base:** `982e31b5a111be0d0cb1c2e01270be225c46261a`; preparation assignment `92d4ed76db515aec351d45b2dc544ec7fc544304`.  
**Execution:** Story Branch Mode; workspace `/Users/terryyin/git/doughnut/.worktrees/story-retire-note-embeddings`, branch `story/retire-note-embeddings` (created at Take); originating/integration checkout `/Users/terryyin/git/doughnut`; claim `28d0154b3d` on `origin/main` (starting revision `f35fa810f1`); increments publish to `origin/story/retire-note-embeddings`.

## Outcome and boundaries

Completely delete hosted note/query embeddings and semantic search: product
code, tests, documents, generated contracts, feature configuration, production
data and schema, and spent transition artifacts. Update dependent features,
especially MCP, to use and positively describe their remaining behavior.
Delete feature tests rather than replacing them with absence assertions.
Keep history in Git; remove this plan and the seed at completed story wrap-up.

Ordinary note-title/alias search, ranking, scopes, relationships/wiki links,
notebook settings, local Git workflows, learning, and independent AI features
continue. This does not promise full-text body search or equivalent semantic
results. No replacement, rescue experiment, general search redesign, or broad
unrelated schema-history cleanup is included.

The current request authorizes planning only. Every slice below is planned;
production has not been inspected successfully or changed by this preparation.

## Existing solutions and design

PFE decision: reuse the existing literal search owner (`NoteSearchService` and
`RelationshipLiteralHitService`). The web already calls it and MCP's
`find_most_relevant_note` already uses `searchForRelationshipTarget`. Delete
the parallel semantic branch without adding a search abstraction or fallback.
Keep `NoteSearchResult.distance`, which ranks literal matches, the shared
OpenAI client, and `ApproximateUtf8TokenBudget`, which serves question generation
and focus context. Remove semantic-specific state from shared UI models;
preserve the ordinary new-note layout currently coupled to `embedSemanticToggle`.

The [North Star](../../NORTH-STAR.md) topics One notebook tree, One format
boundary, and One accepted-change boundary support retaining existing content
and publication owners. They need no change. Relevant Accepted decisions are
[ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
[0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
[0005](../../../docs/adrs/0005-web-routes-accepted.md), and
[0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
Delete only the obsolete semantic-search entry in ADR 0001; do not confuse it
with authored-reference indexes, semantic recall, or image embedding.

Storage decision: retain the existing schema until every production instance
runs the application retirement. Then deploy a temporary drop migration `D`
and a newer generic tip placeholder `P`. After their verified application to
all retained installations, remove the embedding block from the existing
baseline (keep version `V100000000`), delete `D`, and retain `P` and unrelated
migrations. This narrow cleanup follows the baseline repair / newer-tip safety
invariants and the owner's explicit permission to remove historical feature
definitions; it does not squash unrelated migrations or reapply their DDL.
Slice 2 must prove this exact targeted lifecycle before it is relied on.

`D` and `P` are planning symbols, not allocated migration numbers. Allocate
strictly increasing versions above every version ever used, including deleted
files and live Flyway history, immediately before writing them. Recheck at
integration. Preserve operational Flyway metadata; the generic current tip is
not a historical embedding artifact.

## Decisive premises and observations

These are read-only observations, not passing product-test claims.

| Premise | Literal observation / inspected boundary | Result and remaining proof |
| --- | --- | --- |
| Both web modes share literal discovery | `rg -n 'semanticSearch\|semanticEnabled\|embedSemanticToggle' frontend/src frontend/tests`; inspected `useSearchExecution.ts`, `executeDebouncedSearch.ts`, `searchResultsModel.ts`, mounted search/new-note tests | Semantic branch is separable; ordinary layout/cache/ranking must survive slice 4 |
| MCP already uses literal search | `rg -n 'semantic\|embed\|searchForRelationshipTarget' mcp-server/src/tools/find-most-relevant-note.ts`; tool test and `mcp_services.feature` | Change claims, preserve current call/result in slice 5 |
| Refresh, query, storage and fixtures are bounded | `rg -n 'Embedding\|NotebookIndexing\|semanticSearch' backend/src`; inspected `MakeMe`, builder, controller and service tests | Remove producers in slice 6, remaining semantic responsibility in slice 7 |
| Ordinary search has positive proof | Inspected `SearchControllerTests`, `SearchControllerAliasTests`, `SearchControllerWithinTests`, `UserModelSearchTest`, `NoteNewForm.spec.ts`, `SearchResults.spec.ts`, `InsertWikiLink.spec.ts` | Real DB backend proof; mounted frontend proof mocks HTTP; real E2E owns cross-layer journeys |
| Runner tests depend on the deleted E2E feature | `rg -n 'semantic\|12' scripts/isolated-cypress-openai-mock.test.mjs`; inspected scenario-tag example and surviving `recall/property_memory_tracker.feature` | Adapt runner proof to surviving tagged feature, not a negative semantic test |
| Local and production schema differ | `sed -n '480,520p' backend/src/main/resources/db/migration/V100000000__baseline.sql`; `NoteEmbeddingJdbcRepository` constructor | Local `embedding_raw` VARBINARY; production expects VECTOR `embedding`. Actual catalog/FKs/flags remain slice 1's probe |
| Repair precedes migration, after readiness; rollout overlaps versions | `cat backend/src/main/java/com/odde/donut/configs/FlyWayFreeVersionRealMigration.java`; inspected `perform-rolling-replace-app-mig.sh` and release runbook | Distinct R1/R2/R3 barriers required; a green CI run is not deployment evidence |
| Production inventory requires renewed credentials | Earlier literal `gcloud sql instances describe doughnut-db --project=carbon-syntax-298809` with selected metadata format | Failed token reauthentication. Slice 1 must settle it before production-dependent work |
| Narrow baseline cleanup preserves upgrade/fresh paths | Repository db-migration guidance documents repair and newer-tip behavior; current SQL/Java migration inventory was listed | Exact populated upgrade, repair, and fresh-install replay is unproved; bounded by slice 2 before schema changes |
| Proof commands preserve isolated ownership | Inspected package scripts and `docs/worktree-backend-tests.md` | `backend:test_only` in this linked checkout migrates its owned DB and runs the full suite. `backend:verify` also formats, so use the equivalent isolated migration+test path to respect the single coordinator formatting pass |

## Verification and delivery rules

Before execution, prepare this owned checkout with
`./scripts/run.sh bash scripts/worktree_setup.sh`. Follow its isolated DB/SUT
ownership; never borrow the shared development database for a rehearsal.
Read applicable stack/component/testing skills when editing their files.

Use these proof commands from the owned checkout; the slice names its subset:

- **F:** `CURSOR_DEV=true nix develop -c pnpm frontend:test`, then
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`.
- **B:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (full backend
  suite; linked-worktree routing includes migration). Do not substitute filtered
  JUnit runs for required acceptance.
- **A:** `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit
  generated API files. Regenerate before checking affected client types.
- **M:** `CURSOR_DEV=true nix develop -c pnpm mcp-server:test`, then
  `CURSOR_DEV=true nix develop -c pnpm mcp-server:bundle`.
- **W:** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_view/search_note.feature,e2e_test/features/relationships/add_relationship.feature,e2e_test/features/note_topology/wiki_link_insert.feature'`.
- **E:** after bundling MCP,
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`.
- **G:** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`
  with ordinary tag selection; excludes opt-in publication profiling.
- **H:** `CURSOR_DEV=true nix develop -c node --test scripts/isolated-cypress-openai-mock.test.mjs`.
- **ERD:** `CURSOR_DEV=true nix develop -c env DONUT_ERD_SCHEMA=<verified-owned-schema> pnpm export:database-erd`;
  select the actual owned schema, never let the exporter choose development.

Existing setups/assertions were inspected, but these commands were not run
during planning. Retain sufficient passing proof for unchanged boundaries;
repeat only after a change reaches that boundary or invalidates its evidence.
Tests of current authorization/trash behavior remain legitimate; the owner's
prohibition concerns new tests commemorating the retired feature's absence.

For each code increment, follow execution's Jidoka and fresh independent
`dough-post-change-refactor` agent, API generation where needed, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update, commit with the
independent check-only hook, push, and asynchronous CI repair. Implementers and
refactorers run neither standalone `format:changed` nor `lint:changed`.
Use [release-application](../../../.agents/skills/release-application/SKILL.md)
and its [runbook](../../../docs/gcp/conditional-backend-deploy.md) at each
release boundary. Resolve actual authorized tag/SHA then; do not invent tags
or interpret this planning request as authority to deploy.

Target about five minutes per leaf including proof and cleanup. Estimates below
identify work needing scrutiny above that target. The ten-minute hard limit
applies to active work: stop and finer-decompose on overrun. Named full-suite,
API-generation, E2E-startup, CI/deployment and credential waits are explicit
proof/external-wait exceptions, not permission for unbounded implementation.

## Ordered slices

### 1. Establish the production retirement boundary
Type: Behavior
Status: done — metadata only; owner decided 2026-09-28 to skip SQL catalog inspection (not needed for earlier migrations).
Size: 3–5 minutes active; credential/host access wait excepted.
Observed 2026-09-28 after owner `gcloud auth login` (metadata-only `gcloud sql instances describe/list`,
`gcloud sql databases list`, `gcloud compute instance-groups managed list`, `gcloud compute instances list`,
project `carbon-syntax-298809`):
- Only Cloud SQL instance is `doughnut-db` (MYSQL_8_4, RUNNABLE, us-east1); its only non-system database is
  `doughnut`, so `cloudsql_vector=on` (the instance's only database flag) has no other schema consumer.
- One MIG `doughnut-app-group` (us-east1-b, target size 1), one serving VM `doughnut-app-group-jc41`; no other
  retained long-lived installation was found in the project.
- SQL catalog left unobserved by owner decision. Covering reasoning: a plain `DROP TABLE note_embeddings`
  removes the table and any VECTOR index whatever its column type; the deployed app reads/writes the table, so
  it exists; no foreign keys exist in the baseline or code (an unexpected one fails the migration loudly);
  production only ran this repository's migrations, so versions allocate above every file ever used.
Proof: operator receives a verified target and bounded cleanup inventory.

Probe the documented project `carbon-syntax-298809`, Cloud SQL `doughnut-db`,
and application database `doughnut`. Re-run the metadata-only `gcloud sql
instances describe` after authentication is available. Through the established
authorized DB connection, inspect `SELECT VERSION()`, `SHOW CREATE TABLE
note_embeddings`, its row count, inbound/outbound keys in
`information_schema.KEY_COLUMN_USAGE`, vector indexes, flags, and all Flyway
versions (not only `MAX(version)` string ordering). Identify other retained
long-lived installations that must receive the drop before migration removal.
Inspect actual deployed app revisions and record the R1 all-instances check.
Because vector flags are instance-wide, establish vector usage across every
schema on `doughnut-db` (or authoritative exclusive ownership), plus the
flag-disable preconditions and restart effects. Shared settings remain for
other consumers; only Donut's retired dependency is removed.

Record concrete connection/observation commands and metadata results here
without credentials or note content. No DDL or configuration mutation in this
slice. Unknown shared dependencies, schema drift, or unavailable access holds
dependent storage/release work and triggers refinement; application slices
4–7 can proceed independently. Do not silently treat failed access as evidence.

### 2. Prove fresh and populated installations converge after cleanup
Type: Behavior
Status: done — PASS (2026-09-28, MySQL 8.4 local 127.0.0.1:3309, Flyway core/mysql 12.4.0, OpenJDK 25.0.3).
Harness (deleted after use): throwaway Gradle init script printing backend runtime classpath, then
`java -cp <cp> Harness.java <schema> <sqlDir> repair|migrate|info` configuring
`Flyway.configure().dataSource(<worktree-owned rh_old/rh_fresh schema>).locations("filesystem:<staged copies>","classpath:db/migration").cleanDisabled(true)`
and calling repair then migrate as `FlyWayFreeVersionRealMigration` does; compiled Java migrations from `backend/build`.
Provisional (re-allocate in slice 9): D = `DROP TABLE note_embeddings;` (the only embedding structure: table +
`idx_note_embeddings_note_id`, no FKs), P = `SELECT 1;` placeholder; highest version ever used was `300000349`.
Results: populated old install (39 migrations, 7 embedding rows) → D+P migrate 2, table gone, other tables'
row counts/CHECKSUM unchanged → final resources (baseline block removed, D deleted) repair "Marked missing
migrations as deleted, Aligned applied migration checksums", migrate 0, P stays tip, restart repeat is a no-op →
columns/indexes/FKs/views/data identical to before. Fresh install from final resources (40 migrations, tip P) is
schema-identical to the upgraded install. An install that never applied D would silently keep the table under the
final resources, so slice 11's "D verified on every retained installation" gate is essential. Native VECTOR
behavior is covered by dropping the table (slice 3 dropped).
Size: 5–8 minutes active, one local lifecycle probe; engine/test runtime excepted.
Proof: one disposable rehearsal demonstrates the exact drop → newer tip →
baseline edit/drop-migration removal → repair/migrate sequence and a fresh path.

Using the repository's MySQL 8.4/Flyway runtime and isolated fixture schemas,
stage temporary copies of the migration resources. Populate representative
ordinary notebook/note/learning rows and multiple embedding versions. Exercise
the old-install upgrade through provisional `D` and `P`, then the final targeted
baseline and `D` removal. Compare ordinary schema/data before and after; verify
repair accepts the baseline and removed migration, and preserves the newer tip.
Provision a fresh schema from the final resources and compare the resulting
current schema. This probe owns the local populated/fresh Flyway lifecycle;
dropping the table also removes its native Cloud SQL VECTOR index.

Temporary SQL/Flyway harnesses are observation tools outside tracked product
tests. Record their literal commands, runtime versions, setup, result and
ownership in this plan, then remove them when no longer needed. Do not create a
permanent absence-test suite. Credentialed/state-changing engine observation is
deliberately bounded here; if unavailable or the lifecycle fails, hold slices
9–12 and refine the schema strategy before implementation. Application slices
4–7 remain independent. Stop at the active-work limit rather than growing a
general migration harness.

### 3. Establish native Cloud SQL vector cleanup behavior
Type: Behavior
Status: dropped — owner decision 2026-09-28 together with slice 1's SQL catalog: dropping the table removes its
VECTOR index; the instance-wide flag has no other consumer (slice 1), so slice 10 removes it after storage is gone.

### 4. Web discovery uses its existing title and alias workflow
Type: Behavior
Status: done
Accepted proof: F (`pnpm frontend:test` 1941 pass; `vue-tsc --noEmit` exit 0), H 8/8,
W 16/16 (`search_note`, `add_relationship`, `wiki_link_insert`). `embedSemanticToggle`
became `listModeToggle` (new-note form passes `false`); literal-with-literal cache merge kept
(pinned by the within-note merge test). Generated client still exposes semantic and index
operations for slice 6/7 removal.
Size: 5–8 minutes active; F/W/H runtime excepted.
Proof: F, W, H; mounted title suggestions and real target-selection journeys pass.

Remove semantic controls, query branching, result merging, cache/state inputs
and related mocks across `NoteNewForm`, `SearchForNoteAndFolder`, `SearchResults`,
its header, search composables and model. Preserve new-note layout, literal
ranking, caching, pending results, folder/notebook links and scope changes.
Delete semantic tests, the semantic E2E feature and its unique steps/mocks/
index-update helpers. In runner tests, replace the retired scenario-tag exemplar
with the existing property-memory-tracker feature and update the actual fixture
inventory; keep positive runner behavior proof.

Retain/adapt `NoteNewForm.spec.ts` duplicate-title test, `SearchResults.spec.ts`
ranking/container tests and `InsertWikiLink.spec.ts` real selection/Portable
path example. Remove the existing semantic-spy negation assertion too. Remove
stale mocks in toolbar/global-bar/new-button and shared test support. The server
still accepts old operations at this stopping point; slice 7 removes them.

### 5. MCP describes and returns literal search results
Type: Behavior
Status: done
Accepted proof: M (`mcp-server:test` 11 pass; bundle built), E `mcp_services.feature` 5/5.
Only `find-most-relevant-note.ts` descriptions changed; no MCP doc made semantic claims.
Size: 3–5 minutes active; M/E runtime excepted.
Proof: M and E; actual MCP title query returns the existing note result.

Update `find_most_relevant_note` input/tool descriptions and affected current
docs to describe title/alias discovery. Preserve its existing literal SDK call,
scope, ranking and JSON shape; preserve graph retrieval. Retain the positive
tool test `searches all notebooks and returns the top note as JSON` and the
real MCP feature. Remove embedding/semantic promises, not the working tool.

### 6. Notebook administration stops producing embedding indexes
Type: Behavior
Status: done
Accepted proof: B `backend:test_only` full suite exit 0; A regenerated (index operations gone);
F 1941 pass, `vue-tsc` 0. Manual (controller/service/UI) and scheduled (`EmbeddingMaintenanceJob`)
producers deleted; `SchedulingConfig` stays for `QuestionGenerationBatchMaintenanceJob`. Deleting a
Vue component needs `vite build` to refresh tracked `frontend/components.d.ts`.
Size: 5–8 minutes active; B/A/F runtime excepted.
Proof: B, A, F; current notebook settings save successfully; reviewed removal
inventory covers manual and scheduled producers.

Delete `NotebookIndexingSection` and its page wiring, both notebook index API
operations, `NotebookIndexingService`, and `EmbeddingMaintenanceJob`. Delete
index-only tests and assertions, including the embedded index authorization
cases in `NotebookFolderListingControllerTest`; preserve ordinary authorization.
Regenerate contracts and declarations, and delete newly unused producer-only
support. Keep surviving `NotebookPageView.settings.spec.ts` saves/title updates.
Generation helpers still required by the remaining semantic endpoint or its
tests may remain until slice 7; do not commit broken fixtures between slices.

### 7. The server contains only the surviving search responsibility
Type: Behavior
Status: done
Accepted proof: A regenerated (semantic operations gone); B `backend:test_only` 2697 tests 0 failures;
`vue-tsc` 0; M 11 pass + bundle; W 16/16; G 3/3. Inventory `git grep -niE 'embedding|semantic' -- . ':!.planning'`
leaves only independent wording plus retained storage/ops references for slices 9–11: baseline
`note_embeddings` block, `docs/database-erd.md`, `docs/gcp/prod_env.md` §4 vector flag/index, and the
excalidraw "vector enabled" label. Nothing reads/writes `note_embeddings`; it has no foreign keys locally.
Size: 5–10 minutes active; B/A/F/M/W/G runtime excepted where changed boundaries require them.
Proof: B, A, frontend typecheck, M; W and G against the final application tree
cover target selection and web/local note continuity. Reviewed source inventory
proves deletion without introducing negative feature tests.

Delete both semantic note operations and their callers, semantic service,
embedding generation/storage classes and entity, JDBC/non-production searchers,
repositories, `NoteEmbeddingBuilder`/MakeMe support, and feature tests/mocks.
Remove feature-only branches in shared code. Preserve literal distance/ranking,
OpenAI usage and token budgets. Regenerate API contracts and align surviving
consumers; delete unused generated/component references through their generators.
Delete obsolete glossary/tech-stack/focus-context commentary; retain only the
temporary operational schema/flag information still needed for slices 9–10.

Backend proof owns real title/alias, exact/partial rank, scope/container and
trash contracts in `SearchControllerTests`, alias/within tests and
`UserModelSearchTest`. No semantic 404, missing-control or no-generation tests.
Storage still exists inertly; production has not yet been changed.

### 8. Production runs the application retirement everywhere — R1
Type: Behavior
Status: done — R1 = `v1.3.30` (owner-authorized 2026-09-28) on main merge `ab08d6d046` (first parent
`22dfc1db90`, story tip `d111968b62`). CI run `36385416563` attempt 1 success with jar/frontend/cli artifacts.
Application Release run `36386169192`: attempt 1 admission `waiting` (GitHub run search had not yet indexed
the just-finished CI), deploy skipped; runbook rerun (attempt 2) admitted and deployed successfully.
`/api/healthcheck` reports `Commit: ab08d6d046`; MIG `doughnut-app-group` `isStable: true`,
`versionTarget.isReached: true`, one instance `doughnut-app-group-jc41`, so no older reader/writer serves.
Smoke: SPA 200, unauthenticated `POST /api/notes/search` 401, no application ERROR logs after rollout
(only startup-script curl progress on stderr).
Size: 3–5 minutes active; CI/release/rollout waits excepted.
Proof: release runbook receipt plus every serving instance on the retirement
build, stable MIG target and positive current-product smoke observations.

Requires slice 1's target inventory and slices 4–7. Release the verified exact
main commit through an authorized immutable tag. Confirm actual publication,
build/release SHA, every backend instance and drained old requests/jobs; one
load-balanced health response is insufficient. Verify ordinary discovery and
existing MCP access. Leave storage intact until all writers/readers are gone.
Record the release receipt here. Failure holds schema deployment.

### 9. Installed schemas migrate to the current data model
Type: Behavior
Status: done — D = `V300000350__drop_note_embeddings.sql` (``DROP TABLE `note_embeddings`;``), P =
`V300000351__db_migration_placeholder.sql` (comment-only, like former V300000339). Highest ever used was
`300000349` (branch and `origin/main` files; deleted versions max 339). B `backend:test_only` exit 0 with
`migrateTestDB` applying 350/351 on owned `doughnut_wt_77e8daf1364047cbad7ae6ef537b98a1_test` (table absent);
ERD regenerated from that schema (only `note_embeddings` removed); db-migration skill newest-version facts
updated. SQL matches the slice 2 rehearsal except P being comment-only, as production already accepted for 339.
Size: 3–5 minutes active after slice 2; migration/B/ERD runtime excepted.
Proof: populated isolated upgrade preserves ordinary data and schema; B and ERD.

Requires the successful local lifecycle probe (slice 2) and R1. Allocate `D` and `P` after inspecting
current files, Git-deleted versions, and the now-current live histories. Add the
minimal drop DDL for the verified embedding-only structures and a newer generic
tip placeholder. Use the rehearsed sequence, not speculative conditional DDL
that hides a mismatched schema. Retain the original baseline until the drop is
applied to all identified retained installations. Regenerate the current ERD
from the owned migrated schema. Temporary probe observations prove the drop;
current positive backend contracts prove ordinary behavior.

### 10. Production embedding storage and settings are removed — R2
Type: Behavior
Status: done — R2 = `v1.3.31` (owner-authorized) on main merge `aa6ae01a05` (first parent `ab08d6d046`, story
tip `5034e1543d`); main CI `36389613406` success; Application Release `36390200576` admitted and deployed first
attempt. Instance restarted 07:15:47Z with the new jar, `/api/healthcheck` 503 until 07:17:10Z then 200 at
`aa6ae01a05` through 07:25Z, MIG stable. The app's own log does not reach Cloud Logging, so Flyway lines are not
observable; D runs on `ApplicationReadyEvent` and a failure exits the JVM, so sustained health is the D/P evidence.
Owner ran `gcloud sql instances patch doughnut-db --clear-database-flags` (`cloudsql_vector=on` was the only flag;
the auto-mode classifier blocked Claude from running it); `describe` shows state RUNNABLE with no flags; app
healthy afterwards (SPA 200, unauthenticated search 401). Vector setup notes removed from `docs/gcp/prod_env.md`
and the deployment diagram label.
Size: 3–5 minutes active; release/DB/configuration restart waits excepted.
Proof: authorized R2 publication, Flyway D/P success in the serving instance's application log
(`gcloud logging`), `gcloud sql instances describe` showing the removed flag, healthy instances and
positive current-product smoke.

Release slice 9 only after rechecking the R1 all-instances barrier. Verify
actual migration completion, not readiness alone. Remove the identified
feature-only Cloud SQL vector settings after storage cleanup, using slice 1's
instance-wide ownership evidence; observe any restart the flag change requires.
Preserve settings shared with another consumer and the complete unrelated flag
set; use the actual observed API/configuration form.
If settings require restart, finish and verify that operation too. Delete
obsolete vector setup instructions in current docs when their operational
purpose ends. Record scoped catalog queries and results here; do not retain a
retirement runbook. Incomplete credentials, migration or rollout means the story
remains unfinished.

### 11. Fresh installations use only current schema definitions
Type: Behavior
Status: done — baseline `note_embeddings` block (18 lines) removed, `V300000350` deleted, generic tip `V300000351`
kept, db-migration skill lists 350 as retired. B `backend:test_only` 2697 tests 0 failures (repair realigned the
owned test DB: baseline checksum 183701009, 350 DELETED, tip 351); ERD unchanged. Rehearsal with the slice 2
harness: retained schema built from pre-cleanup resources through D/P, then final resources → repair "Marked
missing migrations as deleted, Aligned applied migration checksums", migrate 0, tip 351, repeat no-op, data
checksums unchanged; fresh schema from final resources identical. Inventory `git grep -niE
'embedding|note_embeddings|vector|semantic search' -- . ':!.planning' ':!pnpm-lock.yaml'` leaves only ADR 0004's
independent "image embedding". No migrations exist above 351 on main, so no allocation freeze conflict arose.
Size: 3–5 minutes active after rehearsal; fresh/upgrade/B runtime excepted.
Proof: fresh schema and populated post-R2 upgrade converge through repair/migrate;
B passes; ERD remains the current schema; final tracked-source inventory confirms
the feature, its tests/docs, schema definitions and spent machinery are deleted.

Requires verified D/P application on every retained installation. Coordinate a
short migration-allocation freeze through the final cleanup release. Delete the
complete embedding table block from `V100000000__baseline.sql`, keeping its
version and every unrelated definition unchanged. Delete only temporary `D` and
spent feature cleanup artifacts; retain generic `P` and unrelated migrations.
Update current migration guidance's newest-version facts. Run the exact slice 2
rehearsal with the final files on both fresh and retained fixture schemas.
Do not perform a full unrelated baseline squash. If intervening migrations
invalidate the rehearsed assumptions, recheck/rehearse before proceeding.

### 12. Production accepts the final current schema history — R3
Type: Behavior
Status: done — R3 = `v1.3.32` (owner-authorized) on main merge `a0ab5999db` (first parent `aa6ae01a05`, story
tip `428ed03bb3`); main CI `36393101614` success; Application Release `36393690732` admitted and deployed first
attempt. Health 503 during restart until 07:55:09Z, then `OK … Commit: a0ab5999db` through 07:57:33Z (repair +
migrate run at ready and would exit the JVM on failure); MIG stable, single instance `doughnut-app-group-jc41`;
SPA 200, unauthenticated search 401. No migrations above 351 were added on main during the cleanup window.
Size: 3–5 minutes active; release/verification waits excepted.
Proof: R3 publication and actual Flyway repair/migrate success on retained
installations; positive product smoke and final catalog observations.

Release slice 11 through the same immutable-tag workflow. Verify repaired
baseline history, successful current tip, healthy serving instances and usable
ordinary search/notebook data. Complete the migration freeze. Do not report
completion after R1 or R2 alone.

## Completion and ordinary closure

Final repository inventory is owned by slice 11 before R3: review tracked source,
including ignored-by-default docs and generated artifacts, for feature remnants,
obsolete tests, notices and transitional machinery. Use domain judgment for
unrelated words such as semantic recall; do not delete independent features.

After all twelve slices pass, record execution completion and run the required
retrospective and ordinary story wrap-up. Closure is a mandatory workflow step,
not part of slice 12's timing estimate or a discretionary deferred feature.
Remove the seed, plan, backlog item, assignment and spent artifacts through
that workflow. Evidence lives in this plan only until closure consumes it;
do not copy it to a historical retirement document. Story completion requires
both verified production cleanup and this final disposition.

## Promise ownership

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Web title/alias, scopes, ranking, container navigation and target insertion continue | 4, final integration 7 | F/W; inspected mounted and real E2E boundaries |
| MCP consumers accurately expose current matching and usable note context | 5, generated-client compatibility 7 | M/E; existing real MCP process/backend scenario |
| Notebook settings work; manual/background indexing implementation is deleted | 6, all-instance retirement 8 | Positive settings/controller proof; source inventory; R1 build/rollout evidence |
| All remaining feature code/tests/contracts and obsolete documentation are deleted | 7, operational docs 10, final audit 11 | Reviewed tracked-source inventory and generated contracts; no new absence tests |
| Independent AI utilities, literal distance ranking, learning and Git workflows continue | 7, schema transitions 9/11 | B, retained ranking proofs, G, preserved ordinary fixture data/schema |
| All production embedding records/structures and feature-only settings are removed | 1 establishes premises, 9/10 deliver | Metadata inventory and flag ownership; D/P success in application logs and removed flag |
| Fresh installs and existing installs use the final current schema safely | 2 establishes recipe, 11/12 deliver | Populated upgrade + fresh rehearsal, R3 repair/migration and current-product observations |
| No historical/negated replacement artifacts; temporary work is deleted | 4–7 delete feature tests/docs; 11 final audit; ordinary closure after 12 | Source review, transition cleanup and normal wrap-up; Git retains history |

## Current decisions and limitations

R1 (retire users/producers/consumers) precedes R2 (drop storage + newer tip),
which precedes R3 (final source schema). Future releases must not skip these
barriers just because later commits are on main. Production auth, schema facts,
local lifecycle/native-vector behavior and exact release identities are deliberately
owned by separate early probes and release slices, not asserted as already verified.
Application deletion can progress independently while those dependencies wait.

The cumulative design removes one responsibility and reuses one existing
literal model. Separate slices isolate web discovery, MCP contract, indexing
production, semantic backend retirement, and the migration/release lifecycle;
they introduce no new product model. Required full-suite and deployment waits
are sizing exceptions only for those checks; overlong active edits still stop.

Considered and excluded: a hosted/local semantic replacement, ANN tuning,
replacement negative tests, historical retirement docs, broad Flyway squash,
and immediate production DDL during application rollout.
