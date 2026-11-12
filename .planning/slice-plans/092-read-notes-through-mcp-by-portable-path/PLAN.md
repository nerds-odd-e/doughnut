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

[Selected design and existing owners](design-context.md) retain the exact input,
resolution, authoring, attribution, graph and ADR decisions. Reuse these owners.

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
| Graph same address, budgets/traversal/truncation and credentials | 8 | Migrated graph/MCP/OAuth proof and both MCP E2E journeys |
| Generated navigation references distinguish equal titles | 9 | Outgoing/inbound/peer/retrieval strings → read/graph round trips |
| Focus/related reusable paths and API consumers | 10 | Returned node → full read; regenerated API and frontend proof |
| Current authorization/refusals stay effective | 2–10 | Retained MCP/wiki authorization proof, no added ownership policy |

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
current callers; 4 migrates full reads and 8 deletes the remaining bridge.
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
Status: done
Proof: Known qualified and discovered paths → full reads, exact-root selection,
retained attribution/authorization/OAuth proof; full backend and both MCP E2Es.

Behavior: A qualified portablePath uniquely resolves through the shared resolver
→ return the full read projection from 3. Switch the read argument/schema and all
OAuth/E2E read callers atomically; delete read-specific numeric fields/instructions.
Keep hosted graph identity from discovery until 8. Unqualified input receives
qualification advice; 5 supplies explicit notebookName shorthand scope. Ambiguity
stays unresolved without arbitrary selection until 6 supplies choices.
Sizing: eight to ten active minutes; proof waits excepted. Return before extending.

### 5. Read shorthand within an explicit notebook scope
Type: Behavior
Status: done
Proof: Parameterized MCP scope/title/alias/path/property and missing-scope cases,
including title precedence, exact decoded key and cardinality before property;
full backend and both MCP E2Es for the completed read input contract.

Behavior: Supply unqualified portablePath with optional explicit notebookName
→ the same shared resolver selects the containing full note. Qualified input
keeps its own scope. Preserve readable/available matching and actionable missing
scope/unresolved feedback. No separate matcher or inferred request history.
Sizing: eight to ten active minutes, plus required proof waits.
Timing exception: bounded fixture/index-precondition and compile repairs, about
3.5 extra active minutes; same scope/property proof, detailed in execution evidence.

### 6. Recover from ambiguous shorthand by selecting a path
Type: Behavior
Status: done
Proof: Actual readable folder/root candidates → selected returned path → intended
full read; full backend suite plus actual SDK ambiguity→selection E2E.

Behavior: Several notes match → return the shared candidates with qualified paths
and full locations. Keep title precedence and authorization; use no second search.
Sizing: about five active minutes, plus required full-suite wait.

### 7. Share MCP note destination resolution
Type: Structure
Status: done
Proof: Preserved read HTTP scope/property/candidate/authorization proof, actual
SDK recovery and hosted qualified/renewal journey; full backend and both MCP E2Es.

Extract the existing MCP destination parsing, explicit scope, shared resolver
result and actionable text-only refusals into one current read owner usable by
immediate graph Behavior8. ReadNoteTool delegates without observable change;
graph still uses its existing numeric contract until8. Reuse WikiLinkResolver
and candidate projection, add no matcher, schema framework or speculative policy.
Sizing: five to eight active minutes, plus required proof waits.

### 8. Retrieve bounded graphs by the same address
Type: Behavior
Status: done
Proof: Graph HTTP path/scoped/ambiguous input, budgets/private-content refusal;
full backend and both MCP E2Es against migrated callers.

Behavior: Portable path resolves → intended bounded focus context with the shared
resolution/ambiguity outcome and token limit. Migrate graph/hosted/step callers and
guidance; delete candidate numeric bridge and dedicated MCP numeric-note machinery.
Generated navigation spelling follows in 9, direct focus/related paths in 10.
Sizing: five to eight active minutes, plus required proof waits.

### 9. Follow graph navigation references without title ambiguity
Type: Behavior
Status: planned
Proof: Graph → read/graph round trips distinguish equal titles for outgoing,
inbound, sampled-peer and retrieval-path strings; full backend and MCP E2Es.

Behavior: Actual graph targets → shared qualified address authoring → subsequent
read selects the target. Change formatter and retrieval/BFS/peer callers coherently;
preserve escaping/authored content. Retain renderer content/traversal proof.
Sizing: five to eight active minutes, plus required proof waits.

### 10. Address returned graph notes for subsequent reads
Type: Behavior
Status: planned
Proof: Focus/related portablePath → full read; full backend, MCP E2Es, regenerated
API, frontend tests and both typechecks from proof-routes.md.

Behavior: Returned focus/related notes carry reusable qualified paths. Extend the
shared projection, reuse authoring from 1/9 and regenerate API artifacts together.
Preserve budgets, truncation and internal AI consumers.
Sizing: about five active minutes; required proof/generation waits excepted.

## Acceptance reading and delivery

[Final sweep, file-size seams and delivery contract](acceptance.md) govern final
acceptance and per-slice delivery. Execution evidence retains refinement decisions.

## Story obligations

### G1. Discovery retains a numeric bridge during caller migration
Reported: slice 1 — "Discovery adds Portable paths while retaining noteId for current read/graph callers until their migration."
Story clause: "Delete numeric note ID arguments and MCP-generated note ID fields"
Disposition: proved by slice 8: discovery bridge deletion reading and positive Portable-path schemas; discovered nested alias path → intended graph HTTP, preserved discovery → read and both actual SDK graph journeys; execution-evidence.md slice8 and /tmp/seed125-slice8-handoff.md

### G2. Discovery paths await read and graph consumers
Reported: slice 1 — "Discovery produces reusable addresses; candidate-to-read and candidate-to-graph observations belong to slices 3 and 5."
Story clause: "`list_note_candidates` supplies qualified Portable paths that the read and graph tools accept directly."
Disposition: proved by slice 8: discovered nested alias path passed unchanged → intended graph; qualified/discovered reads and both real SDK journeys retained and rerun, full backend3161 and MCP E2Es9 passing; execution-evidence.md slice8

### G3. Read projection retains its current numeric bridge during citation enrichment
Reported: slice 3 — "Sizing inspection found hosted graph calls consume the current read result identity; retain it during additive citation output and migrate graph identity to discovery in the qualified-input transition."
Story clause: "Delete numeric note ID arguments and MCP-generated note ID fields"
Disposition: proved by slice 4: qualified read HTTP projection/callers plus hosted E2E discovery identity → graph, detailed in execution-evidence.md and /tmp/seed125-slice4-consumer-trace.md

### G4. Explicit shorthand scope follows qualified-path migration
Reported: slice 4 — "Unqualified destinations currently receive qualification advice; explicit notebookName shorthand scope is the next slice's input contract."
Story clause: "Every unqualified destination (title, alias, folder path, or exact-root path) needs an explicit notebook scope because MCP has no source note."
Disposition: proved by slice 5: ReadNoteScopedPathMcpEndpointTest explicit scope and no-inferred-history HTTP cases, full backend3147 and MCP E2Es8 passing; execution-evidence.md

### G5. Actual SDK ambiguity recovery still needs an observation
Reported: slice 6 — "Coordinator inspected cached SDK output validation: raw HTTP cases alone do not execute the client error path. Add one actual SDK ambiguity → returned-path selection → full-read journey."
Story clause: "If several notes match within a notebook, return the actual readable resolution candidates with enough notebook and full folder information to choose."
Disposition: proved by slice 6: actual cached-schema SDK ambiguity → candidate full-folder selection → returned Portable path → full read, mcp_services.feature new scenario8/8 passing

### G6. Generated graph navigation addresses still need canonical qualification
Reported: slice 8 — "Graph input now uses paths; generated outgoing, inbound, sampled-peer and retrieval-path wiki spellings remain interim until navigation slice9."
Story clause: "MCP-generated references to the focus note, related notes, outgoing destinations, inbound referrers, sampled peers, and retrieval paths use that same addressing contract."
Disposition: interim until slices 9

### G7. Returned graph nodes still need direct reusable addresses
Reported: slice 8 — "Focus and related projections keep current metadata; direct reusable portablePath fields remain owned by slice10."
Story clause: "MCP-generated references to the focus note, related notes, outgoing destinations, inbound referrers, sampled peers, and retrieval paths use that same addressing contract."
Disposition: interim until slices 10
