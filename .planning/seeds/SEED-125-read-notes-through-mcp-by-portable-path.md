---
id: SEED-125
status: active
planted: 2026-11-12
scope: story
---

# Read notes through MCP by Portable path

<a id="read-notes-through-mcp-by-portable-path"></a>

### Read notes through MCP by Portable path

**Identity:** SEED-125#read-notes-through-mcp-by-portable-path
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/092-read-notes-through-mcp-by-portable-path/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e915ddae4c595fbf8e74a3531a54ccfaa2aa343aa81068b9a02fac92cd5cd68f","plan":"be8b9a1be20f8d957398c03e744f63fa994a8abf88bdd3a7c24f014d14676f3d"}}
```

**Goal.** A Donut user can ask their connected assistant to follow a wiki link
and get the intended note or its graph context using the same address they use
in their notes.

**Scope.** Use the existing wiki-link resolution rules for `read_note` and
`get_note_graph`. Every unqualified destination (title, alias, folder path, or
exact-root path) needs an explicit notebook scope because MCP has no source
note. A notebook-qualified destination supplies its notebook-name scope.
Readable display-name matches take precedence over aliases; path-shaped
references select the full folder trail, including the exact-root spelling
`/Title`. A property selector keeps its existing resolution semantics: settle
the note first and require the exact decoded property key. These tools still
return the containing full note or note graph.

If several notes match within a notebook, return the actual readable resolution
candidates with enough notebook and full folder information to choose. Discovery
continues to use the existing ranked title/alias search. `list_note_candidates`
supplies qualified Portable paths that the read and graph tools accept directly.
MCP-generated references to the focus note, related notes, outgoing destinations,
inbound referrers, sampled peers, and retrieval paths use that same addressing
contract. Full reads supply the qualified path with the whole notebook's
`acceptedRevision` when available (omitted when unavailable), and return authored Markdown verbatim.
Graph reads retain their combined token budget, traversal, and explicit body
truncation behavior.

Delete numeric note ID arguments and MCP-generated note ID fields, together with
their dedicated instructions and fixtures. Sweep tool schemas, responses,
documentation, and callers for remnants of the removed contract. This is deletion
plus a one-time acceptance reading, separate from proof of the replacement
behavior. Shared REST/browser note identities and authored Markdown URLs have
retained consumers and stay under their existing contracts.

**Assumptions and scope limits.** The developer selected unique notebook names
as this increment's working assumption on 2026-11-12. Circle-owned notebooks and
handling duplicate notebook names are deferred. Adding or changing MCP access
restrictions is explicitly outside this story. Preserve current authorization;
neither implement an ownership filter nor add a guard enforcing the naming
assumption. Other deferred promises are historical reads by revision, addresses
stable across rename or move, property-only content retrieval, broader discovery
search, and new web routes. These are delivery limits, not rejection rules.

**Required restrictions.** Existing notebook read authorization applies to
resolution candidates, full reads, and graph retrieval. Existing wiki semantics
exclude trashed notes and leave zero-match or ambiguous targets unresolved;
ambiguity must not choose arbitrarily. An unqualified destination needs explicit
notebook scope because there is no source notebook to infer. These requirements
come from authorization and resolution correctness, not fixture arrangements.

**Key examples.**

- A user gives the assistant `[[Engineering:Practices/Code Review]]`. The
  assistant supplies that destination to either tool without searching first.
  A full read returns the authored content, qualified path, and accepted revision
  for citation; a graph read returns the existing bounded context.
- In `Engineering`, two readable notes are titled `Code Review`. A lookup with
  that title and explicit notebook scope returns both full-folder candidates.
  The selected candidate path leads to the intended note. A root candidate can
  use the exact-root spelling `Engineering:/Code Review`.
- A readable display-name match and an alias match compete for
  `Engineering:Review`. The display-name match wins. If only the alias matches,
  it resolves under the existing alias rule.
- Discovery finds a note by alias in a nested folder. Its returned qualified
  Portable path can be passed unchanged to either read tool.
- A generated graph reference targets one of two equal-titled notes in different
  folders. Its qualified path selects that destination when used in a later read.
- A full read contains authored wiki links and a Markdown URL such as
  `[Reference](/n1234)`. Content is returned unchanged; MCP-generated citation and
  navigation metadata supplies Portable paths.

**Alternatives and recommendation.**

| Candidate | Fit to the goal and settled scope | Consequence |
| --- | --- | --- |
| Qualified Portable path with optional explicit notebook-name scope for an unqualified input | Reuses authored addresses, shared resolution, and the selected unique-name assumption | Recommended; direct reads and reusable candidate/reference paths |
| Discovery before every read | Reuses the current navigation sequence | Adds an unnecessary step to following an already-known link |
| Additional notebook selector or owner-qualified address grammar | Can distinguish multiple ownership namespaces | Extra contract and language changes for the deferred duplicate-name case |

The useful borrowed mechanism is namespace-based lookup in a database: a name
resolves inside a selected schema, and a catalog lets a caller compare competing
names before reusing its selected address. Here the note is the named object,
the notebook is the scope, and candidates are the catalog entries. Share the
resolution rule and return a reusable address instead of choosing arbitrarily.
The analogy's limit is that notebook display names are not globally unique;
this story uses the developer's unique-name assumption rather than extending
addressing or access policy for that case.

**Architecture.** Keep parsing, title/alias precedence, candidate matching, and
readability with shared wiki-resolution owners. MCP supplies source-less
notebook scope and its wire projection. Reuse the current content, revision
attribution, and bounded graph retrieval responsibilities; the shared read DTO
also has REST consumers. Generated graph references currently omit folder
ancestry, so adapting input arguments alone would miss the navigation promise.
The strongest challenge was a visually selectable candidate whose address still
matched two notebooks. Under the developer's explicit unique-name assumption,
that case is deferred; same-title ambiguity within one notebook remains in scope.

Relevant Accepted decisions are
[ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure)
(Portable paths differ from server identities),
[ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
(accepted revision belongs to the whole tree),
[ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)
(shared resolution and property semantics),
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md#notebook-links-as-web-destinations)
(authored URLs retain their meaning), and
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md#usage)
(handle deliberate business outcomes). The selected direction conflicts with none.

**Investigation evidence.** Local source readings on 2026-11-12 at
`b481551f17d71f637213f7c7d706b282889eefcc`. These are source readings;
runtime preparation observations belong to the associated plan:

- `PortablePath` parses a notebook-name qualifier. `WikiLinkNoteCandidates`
  matches readable titles before aliases and matches path-shaped destinations
  by the full folder trail. `PortablePathAuthoring` lengthens a note's address
  when its shorthand is ambiguous.
- The schema's `uk_notebook_ownership_name` constraint is on
  `(ownership_id, name)`. `NoteControllerShowWikiLinkAmbiguityTests` constructs
  same-named notebooks under different owners and observes ambiguity once both
  become readable. Setup and assertions were inspected, not freshly executed.
- `ListNoteCandidatesTool` currently searches owned and subscribed notebooks;
  `ListNoteCandidatesMcpEndpointTest.includesSubscribedNotebookNotes` proves a
  foreign subscribed note appears. MCP token/OAuth context adds no notebook
  restriction. Read authorization permits ownership, subscription, or bazaar
  publication; `User.owns` also includes circle membership. These observations
  establish current behavior, not a new access commitment or restriction.
- `ReadNoteTool` serializes shared `AttributedNoteContent`; its service returns
  `note.getContent()` and the notebook's accepted revision.
  `FocusContextWikiUri` emits notebook/title or title-only wiki strings;
  `FocusContextRetrievalService` uses those throughout graph metadata and keeps
  explicit body truncation.

Portable paths describe location and change when a note moves or is renamed.
No human decision remains open for this bounded story.
