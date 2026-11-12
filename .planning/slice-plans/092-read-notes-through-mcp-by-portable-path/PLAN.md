# Read notes through MCP by Portable path

**Identity:** SEED-125#read-notes-through-mcp-by-portable-path
**Source:** [refined story](../../seeds/SEED-125-read-notes-through-mcp-by-portable-path.md#read-notes-through-mcp-by-portable-path)

## Goal and boundaries

Let an assistant follow a known wiki destination, read the intended note or
bounded graph, and reuse returned candidate/reference addresses without numeric
note identifiers. Full reads preserve authored Markdown and notebook revision
attribution. This is preparation only; all slices remain unexecuted.

Developer decision, 2026-11-12: assume notebook names are unique. Circle-owned
notebooks and duplicate notebook-name handling are deferred. Adding or changing
access restrictions is outside scope. Keep current authorization and discovery
coverage; add neither an ownership filter nor a naming guard. Historical reads,
stable addresses across moves/renames, property-only reads, and new routes are
also deferred. Shared REST/browser identifiers and authored URLs retain their
existing contracts.

## Current decisions and existing solutions

- MCP input is a required `portablePath` destination string, with optional
  explicit `notebookName` scope for an unqualified destination. A qualified
  destination supplies its own scope. Missing scope receives actionable feedback;
  it is not inferred from an earlier call or another user's request.
- Reuse `PortablePath`/`WikiLinkMarkdown` syntax and the shared
  `WikiLinkResolver` → `WikiLinkCandidateClassifier` → `WikiLinkNoteCandidates`
  rules. Expose their candidate-bearing source-less operation; do not implement
  another title/alias/path matcher in MCP. Preserve note cardinality before
  property-key validation and the current readable/available-note rule.
- Reuse `PortablePathAuthoring` for qualified addresses, with the authenticated
  viewer explicit. Preserve exact folder trails, root ambiguity fallback, and
  wiki escaping. `NotebookGitPortablePath` owns exported filenames, so it is not
  the wiki address owner. No separate notebook identifier is needed here.
- Reuse `AttributedNoteContentService` for full content and accepted revision;
  MCP owns its wire projection while shared REST identifiers remain useful.
  Preserve current omission of `acceptedRevision` when unavailable. New MCP
  schema assertions describe the positive Portable-path shape.
- Keep bounded graph retrieval with its existing owners. Route graph-generated
  references through the same address authoring; add reusable addresses to focus
  and related-note projections. Preserve authored body/YAML destinations.
- These PFE choices follow [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
  [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md), and
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
  [North Star](../../NORTH-STAR.md), One notebook tree/One format boundary,
  supports these existing owners; no new architectural direction is needed.

## Outside-in proof ownership

All added proof drives MCP HTTP or an existing stable product boundary with real
matching, database fixtures, and current authorization. Use MakeMe; no internal
service mocks. Parameterize data variations rather than repeat canonical shapes.
New tests below are planned proof, not claims that existing tests cover new paths.

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| Discovery supplies qualified paths and complete folder locations | 1 | Real `list_note_candidates` HTTP results for nested and root notes, including alias discovery |
| Shared title/alias/path/property and readability behavior remains coherent | 2 | Existing wiki controller tests remain green after exposing source-less matching |
| Known qualified destination works without discovery; shorthand requires explicit scope | 3 | MCP full reads with qualified path and notebook-scoped shorthand; missing scope gives actionable feedback |
| Title wins over alias; alias fallback, exact-root/path spelling, property semantics | 3 | New MCP resolution cases through `read_note`, including the existing exact-key rule |
| Candidate address selects the intended note; full content and attribution are retained | 3 | Discovery → read round trip; verbatim Markdown, text/structured projection, accepted revision changes after another note edit, unbound read |
| Ambiguous title returns every readable choice and selected path resolves | 4 | Two folders plus exact-root candidate; ambiguous response → chosen candidate → full read |
| Graph takes the same destination and retains budgets/traversal/truncation | 5 | `GetNoteGraphMcpEndpointTest` migrated to path input plus scoped/ambiguous lookup using the shared outcome |
| Personal and hosted credentials still read as the correct user | 3, 5 | MCP/OAuth endpoint tests and the two existing MCP E2E journeys, migrated with their callers |
| Every graph navigation reference is reusable despite equal titles | 6 | Graph → read/graph round trips for outgoing, inbound, sampled-peer and retrieval-path targets |
| Focus and related notes expose reusable qualified addresses | 7 | Returned graph-node address → full read; generated API and frontend remain valid |
| Existing unauthorized/private-content refusals remain effective | 2–7 | Retained MCP and wiki authorization proof; no new ownership policy |

## Consumers and proof routes

- `McpToolArguments` feeds both read tools. Their changed arguments reach
  `ReadNoteMcpEndpointTest`, `GetNoteGraphMcpEndpointTest`,
  `AssistantMcpAccessTest`, and `AssistantTokenRenewalTest`: migrate/run in 3/5.
- `ListNoteCandidatesTool` feeds `mcpAgentActions.callToolOnFirstCandidate`,
  `hostedAssistant`, and MCP step definitions. Inspect their actual request and
  observation chains, migrate read consumers in 3 and graph consumers in 5.
  Run `mcp_services.feature` and `assistant_account_linking.feature` for each
  changed contract; retain isolation, budgets, revision, renewal, and withdrawal.
- `FocusContextWikiUri` feeds retrieval/BFS/folder peers, then graph JSON and
  `FocusContextMarkdownRenderer`. Export and AI question/conversation/session builders
  consume the renderer: run full backend proof in 6; preserve content/traversal
  purposes while updating intentional address spelling assertions.
- Shared focus/related graph models reach OpenAPI and frontend generated types:
  regenerate and run frontend proof in 7. Generator-owned files are regenerated,
  not manually edited or split.
- During execution, run the installed `consumer-trace.mjs` against each current
  diff (`--base HEAD`), adding terms `read_note`, `get_note_graph`,
  `callToolOnFirstCandidate`, and the changed MCP step phrases. Record every
  located consumer as run, reused with matching evidence, or excluded with an
  effect-based reason; extend this trace when the actual implementation differs.

Commands, from the execution checkout, with Nix:

- Every backend slice: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend skill requires the full suite. Do not substitute filtered tests.
- Changed MCP caller journeys in 3/5/6/7:
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature,e2e_test/features/mcp/assistant_account_linking.feature`.
- Shared graph DTO change in 7: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`,
  then full backend proof against regenerated OpenAPI, plus
  `CURSOR_DEV=true nix develop -c pnpm frontend:test`,
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`, and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.
  These broader checks are required by the API-generation and frontend skills.

## Decisive premises and preparation observations

Observed on 2026-11-12 in the named preparation worktree at
`b481551f17d71f637213f7c7d706b282889eefcc`, using its isolated test database
`doughnut_wt_0f536327332d443d8e6724b873e6c22a_test`. Each scratch edit was restored
byte-for-byte before planning writes. Production Git diff is empty.

| Premise/consumer | Consuming operation and observed form/profile | Result and limit |
| --- | --- | --- |
| Backend proof route can execute current behavior | `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, original source, isolated `test` profile | Green: 3,118 tests, zero failures/errors, two skipped; Gradle 1m38s. This is the retained unchanged baseline, not future path proof. |
| Address changes reach real MCP and hosted callers; shared matching has discriminating proof | Same full-suite command with `McpToolArguments` lookup temporarily changed from `noteId` to `portablePath`, title matching temporarily forced to alias fallback, and formatter title temporarily set to `preparation-probe` | 154 expected failures among 3,118 tests. Read/graph endpoint, hosted access/renewal and title-precedence/property tests failed at their real results. Combined mutations do not isolate formatter coverage. |
| Shared graph strings have maintained consumers | Same full-suite command with only both `FocusContextWikiUri` title expressions changed to `preparation-probe` | Three failures: outgoing depth/path, depth-two traversal, and seeded sibling-selection tests. Restore matched the earlier green source exactly; reuse that baseline. This does not prove all generated addresses are navigable: 6 adds round trips. |
| Both named E2E specs are admitted in this checkout | Nix Node module executed `selectedCypressSpecs` with the exact two-spec argument above, then `assertSupportedIsolatedCypressSpecs` | Both selected; neither requires private OpenAI/Wikidata mocks. This observes runner selection only, stopping before starting/resetting an E2E stack. Actual journeys are owned by execution, not claimed run here. |

Local host decision: macOS load averages 3.97/6.86/10.97, reclaimable inactive
memory available; unrelated processes were left alone. Run one owned backend
invocation at a time and never alongside Cypress in the same checkout. Before
another expensive launch, refresh machine/load evidence if contention changes.
The measured 1m38s–1m57s full-suite wait is an explicit focused-test timing
exception; it does not excuse oversized active edits. No decisive current-state
premise remains unobserved; new capability assertions remain implementation work.

## Ordered slices

### 1. Discover notes with reusable qualified addresses
Type: Behavior
Status: planned
Proof: Discovery HTTP results expose canonical qualified addresses/full folders;
run full backend proof. Candidate → read round-trip proof belongs to 3.

Behavior: A title/alias search finds notes → `list_note_candidates` returns
qualified Portable paths and complete folder context usable for selection.
Extend the shared address authoring owner with an explicit viewer. Keep search
ranking and coverage. Interim: retain the candidate's numeric bridge only for
current callers; 3 migrates full reads and 5 deletes the remaining bridge.
Sizing: about five active minutes, plus required full-suite wait.

### 2. Share source-less candidate resolution
Type: Structure
Status: planned
Proof: Full backend suite preserves wiki behavior; the scratch observation
specifically discriminated title-precedence/property effects, not every rule.

Expose the existing matching owner's candidate-bearing operation with explicit
notebook-name scope and viewer. Preserve its existing callers/cardinality rules.
This immediately enables 3; add no MCP policy or alternate matching model.
Sizing: about five active minutes, plus required full-suite wait.

### 3. Read uniquely resolved notes by Portable path
Type: Behavior
Status: planned
Proof: New MCP resolution/full-read cases and discovery → read round trips;
retained attribution/authorization/OAuth proof; full backend and both MCP E2Es.

Behavior: A known qualified destination, or shorthand with explicit notebook
scope, uniquely resolves → `read_note` returns its full authored text, qualified
address, location, timestamp, and available notebook revision. Accept the existing
path and property rules. Give actionable missing-scope/unresolved feedback.
Map the shared read result into MCP's positive path-based response/schema;
remove the read-specific numeric contract. Migrate read callers and guidance.
Interim: ambiguity is unresolved without arbitrary selection; 4 adds choices.
Sizing: five to eight active minutes, plus required proof waits.

### 4. Recover from ambiguous shorthand by selecting a path
Type: Behavior
Status: planned
Proof: MCP ambiguous response contains all readable folder/root candidates;
using the chosen returned path reads the selected note. Run full backend proof.

Behavior: Several readable notes match within the notebook → return the shared
candidate result with reusable paths/full folders → caller selects and reads one.
Use actual resolver candidates, not a second discovery search with a different
coverage. Preserve current authorization and title-before-alias semantics.
Sizing: about five active minutes, plus required full-suite wait.

### 5. Retrieve bounded graphs by the same address
Type: Behavior
Status: planned
Proof: Graph HTTP path/scoped/ambiguous input plus existing budget and private
content proof; full backend and both MCP E2Es against migrated real callers.

Behavior: A Portable path resolves → `get_note_graph` returns the intended bounded
focus context, using the same resolution/ambiguity contract and token limit.
Migrate graph callers, hosted assistant, MCP steps and instructions. Delete the
candidate numeric bridge and remaining dedicated MCP numeric-note machinery.
Interim: existing generated reference spellings remain until 6 and direct
focus/related addresses until 7; content, traversal and budgets remain usable.
Sizing: five to eight active minutes, plus required proof waits.

### 6. Follow graph navigation references without title ambiguity
Type: Behavior
Status: planned
Proof: Graph → read/graph round trips distinguish equal-titled notes for outgoing,
inbound, sampled-peer and retrieval-path strings; full backend and MCP E2Es.

Behavior: Graph metadata names an actual note → its generated wiki destination
uses shared qualified address authoring → a subsequent read selects that note.
Change the single formatter and its retrieval/BFS/peer callers coherently;
preserve wiki escaping and authored content. Update affected renderer consumers'
positive address observations without weakening their content/traversal purpose.
Sizing: five to eight active minutes, plus required proof waits.

### 7. Address returned graph notes for subsequent reads
Type: Behavior
Status: planned
Proof: Focus/related node `portablePath` → full-read round trips; full backend,
MCP E2Es, regenerated API, frontend tests and both frontend typechecks.

Behavior: A graph returns focus/related notes → each carries its reusable qualified
Portable path alongside existing metadata. Extend their existing shared projection,
reuse authoring from 1/6, and regenerate controller-derived API artifacts in the
same slice. Retain graph budgets, truncation signals and internal AI consumers.
Sizing: about five active minutes; full-suite/generation waits are the stated
focused-test exception, not extra active implementation allowance.

## Acceptance reading and delivery

At final acceptance, separately from tests, sweep MCP schemas/handlers/responses,
callers/fixtures, E2E phrases, docs and agent guidance for the removed numeric-note
contract. Search `McpToolArguments.noteId`, numeric-note argument instructions,
old MCP step phrases and `noteId` in MCP wire producers/consumers. Classify retained
shared HTTP/browser identifiers and authored Markdown URLs by their live purpose.
Delete dedicated remnants transitively. Record this as a one-time reading;
create no absence assertions, migration guard, compatibility alias or tombstone.

Expected handwritten edited files currently stay below 250 lines. Highest growth
risk is `AssistantMcpAccessTest` (247): keep new resolution proof in cohesive new
MCP test classes; if its migration needs growth, separate successful scoped access
from credential-format/rejection tests in 3. Other expected services/tests/helpers
are at most 243 lines. Respect the 250-line refactoring check as actual edits grow.

Each execution slice uses Jidoka → fresh post-change-refactor agent → API generation
when applicable → coordinator `./scripts/run.sh pnpm format:changed` once → plan
update → agent commit/check-only hook → push with asynchronous CI ownership.
Follow the execution skill; preparation grants no Take or implementation authority.

Plan refinement was not needed: the graph's two distinct address outcomes are
separate bounded slices, Structure is adjacent to its first Behavior, and all
final promises have proof owners. No remaining slice-specific concern was found.
