# Read notes through MCP by Portable path

**Identity:** SEED-125#read-notes-through-mcp-by-portable-path
**Source:** [refined story](../../seeds/SEED-125-read-notes-through-mcp-by-portable-path.md#read-notes-through-mcp-by-portable-path)
**Source review:** [independent commitments](source-review.md)
**Execution evidence:** [identity, resume context and accepted proof](execution-evidence.md)

## Goal and boundaries

Let an assistant follow a known wiki destination, read the intended note or
bounded graph, and reuse returned candidate/reference addresses without numeric
note identifiers. Full reads preserve authored Markdown and notebook revision
attribution. Execution began on 2026-11-12; slice status below owns progress.

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
| Discovery paths/full folders and ranked alias search | 1 | Real list_note_candidates HTTP nested/root/alias results; retained search ranking proof |
| Shared wiki matching/readability remains coherent | 2 | Existing controller tests after source-less candidate exposure |
| Full content, qualified citation path and whole-notebook revision | 3 | MCP canonical text/structured projection, verbatim Markdown, another-note revision change and unbound omission |
| Known qualified destination and discovered path work directly | 4 | MCP direct and discovery → read; exact-root selection; migrated personal/hosted callers |
| Explicit shorthand scope and inherited title/alias/path/property rules | 5 | MCP scope and semantic cases, exact decoded property key, no inferred history |
| Ambiguity returns every actual readable choice, selection resolves | 6 | Two folders plus exact-root candidate → chosen path → full read |
| Graph same address, budgets/traversal/truncation and credentials | 7 | Migrated graph/MCP/OAuth proof and both MCP E2E journeys |
| Generated navigation references distinguish equal titles | 8 | Outgoing/inbound/peer/retrieval strings → read/graph round trips |
| Focus/related reusable paths and API consumers | 9 | Returned node → full read; regenerated API and frontend proof |
| Current authorization/refusals stay effective | 2–9 | Retained MCP/wiki authorization proof, no added ownership policy |

## Consumers and proof routes

[Consumer chains and literal required commands](proof-routes.md) own the
backend, MCP E2E, generation and frontend proof routes. Each implementation
brief carries its selected routes and the consumer-trace requirement.

## Preparation evidence

[Decisive premises and dated observations](preparation-evidence.md) retain the
full baseline, mutation checks, environment, commands and limitations. Reuse the
matching evidence; new capabilities still need their owning slice proof.

## Ordered slices

### 1. Discover notes with reusable qualified addresses
Type: Behavior
Status: done
Proof: Discovery HTTP results expose canonical qualified addresses/full folders;
run full backend proof. Candidate → read round-trip proof belongs to 4.

Behavior: A title/alias search finds notes → `list_note_candidates` returns
qualified Portable paths and complete folder context usable for selection.
Extend the shared address authoring owner with an explicit viewer. Keep search
ranking and coverage. Interim: retain the candidate's numeric bridge only for
current callers; 4 migrates full reads and 7 deletes the remaining bridge.
Sizing: about five active minutes, plus required full-suite wait.

### 2. Share source-less candidate resolution
Type: Structure
Status: done
Proof: Full backend suite preserves wiki behavior; the scratch observation
specifically discriminated title-precedence/property effects, not every rule.

Expose the existing matching owner's candidate-bearing operation with explicit
notebook-name scope and viewer. Preserve its existing callers/cardinality rules.
This enables the next full-read outcome, now delivered incrementally in 3–5;
add no MCP policy or alternate matching model.
Sizing: about five active minutes, plus required full-suite wait.

### 3. Cite full reads with qualified addresses
Type: Behavior
Status: done
Proof: Canonical MCP full-read result supplies qualified path/full location,
verbatim content and whole-notebook revision, including another-note edit and
unbound omission; full backend suite. Existing E2E inputs/results stay compatible.

Behavior: An existing full read returns authored content with citation metadata
→ enrich MCP's owned projection/schema with qualified portablePath and location,
using AttributedNoteContentService and PortablePathAuthoring(viewer). Retain the
current numeric input/result only until 4 migrates their callers. Preserve shared
REST DTOs. This is useful additive citation output, with no unused resolver.
Sizing: five to seven active minutes, plus required full-suite wait.

### 4. Follow a qualified destination directly for a full read
Type: Behavior
Status: planned
Proof: Known qualified and discovered paths → full reads, exact-root selection,
retained attribution/authorization/OAuth proof; full backend and both MCP E2Es.

Behavior: A qualified portablePath uniquely resolves through the shared resolver
→ return the full read projection from 3. Switch the read argument/schema and all
OAuth/E2E read callers atomically; delete read-specific numeric fields/instructions.
Keep hosted graph identity from discovery until 7. Unqualified input receives
qualification advice; 5 supplies explicit notebookName shorthand scope. Ambiguity
stays unresolved without arbitrary selection until 6 supplies choices.
Sizing: eight to ten active minutes; proof waits excepted. Return before extending.

### 5. Read shorthand within an explicit notebook scope
Type: Behavior
Status: planned
Proof: Parameterized MCP scope/title/alias/path/property and missing-scope cases,
including title precedence, exact decoded key and cardinality before property;
full backend and both MCP E2Es for the completed read input contract.

Behavior: Supply unqualified portablePath with optional explicit notebookName
→ the same shared resolver selects the containing full note. Qualified input
keeps its own scope. Preserve readable/available matching and actionable missing
scope/unresolved feedback. No separate matcher or inferred request history.
Sizing: eight to ten active minutes, plus required proof waits.

### 6. Recover from ambiguous shorthand by selecting a path
Type: Behavior
Status: planned
Proof: Actual readable folder/root candidates → selected returned path → intended
full read; full backend suite.

Behavior: Several notes match → return the shared candidates with qualified paths
and full locations. Keep title precedence and authorization; use no second search.
Sizing: about five active minutes, plus required full-suite wait.

### 7. Retrieve bounded graphs by the same address
Type: Behavior
Status: planned
Proof: Graph HTTP path/scoped/ambiguous input, budgets/private-content refusal;
full backend and both MCP E2Es against migrated callers.

Behavior: Portable path resolves → intended bounded focus context with the shared
resolution/ambiguity outcome and token limit. Migrate graph/hosted/step callers and
guidance; delete candidate numeric bridge and dedicated MCP numeric-note machinery.
Generated navigation spelling follows in 8, direct focus/related paths in 9.
Sizing: five to eight active minutes, plus required proof waits.

### 8. Follow graph navigation references without title ambiguity
Type: Behavior
Status: planned
Proof: Graph → read/graph round trips distinguish equal titles for outgoing,
inbound, sampled-peer and retrieval-path strings; full backend and MCP E2Es.

Behavior: Actual graph targets → shared qualified address authoring → subsequent
read selects the target. Change formatter and retrieval/BFS/peer callers coherently;
preserve escaping/authored content. Retain renderer content/traversal proof.
Sizing: five to eight active minutes, plus required proof waits.

### 9. Address returned graph notes for subsequent reads
Type: Behavior
Status: planned
Proof: Focus/related portablePath → full read; full backend, MCP E2Es, regenerated
API, frontend tests and both typechecks from proof-routes.md.

Behavior: Returned focus/related notes carry reusable qualified paths. Extend the
shared projection, reuse authoring from 1/8 and regenerate API artifacts together.
Preserve budgets, truncation and internal AI consumers.
Sizing: about five active minutes; required proof/generation waits excepted.

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
from credential-format/rejection tests in 4. Other expected services/tests/helpers
are at most 243 lines. Respect the 250-line refactoring check as actual edits grow.

Each execution slice uses Jidoka → fresh post-change-refactor agent → API generation
when applicable → coordinator `./scripts/run.sh pnpm format:changed` once → plan
update → agent commit/check-only hook → push with asynchronous CI ownership.
Follow the execution skill; preparation grants no Take or implementation authority.

Refinement on 2026-11-12 retains completed 1/2 and splits the oversized read
contract into citation output (3), atomic qualified-input/caller transition (4),
and explicit notebook scope (5). Their shared resolution/authoring model stays
unchanged; all later slices shift by two. See execution evidence for sizing,
stop-safe alternatives and retained proof. Nine slices; execution may resume.

## Story obligations

### G1. Discovery retains a numeric bridge during caller migration
Reported: slice 1 — "Discovery adds Portable paths while retaining noteId for current read/graph callers until their migration."
Story clause: "Delete numeric note ID arguments and MCP-generated note ID fields"
Disposition: interim until slices 4, 7

### G2. Discovery paths await read and graph consumers
Reported: slice 1 — "Discovery produces reusable addresses; candidate-to-read and candidate-to-graph observations belong to slices 3 and 5."
Story clause: "`list_note_candidates` supplies qualified Portable paths that the read and graph tools accept directly."
Disposition: interim until slices 4, 7

### G3. Read projection retains its current numeric bridge during citation enrichment
Reported: slice 3 — "Sizing inspection found hosted graph calls consume the current read result identity; retain it during additive citation output and migrate graph identity to discovery in the qualified-input transition."
Story clause: "Delete numeric note ID arguments and MCP-generated note ID fields"
Disposition: interim until slices 4
