# Source-only review

Reviewer launch: collaboration.spawn_agent, task `/root/source_review`, fork_turns `none`, 2026-11-12. Input: only selected story Goal, Scope, Required restrictions, Assumptions and scope limits, and Key examples, rendered in the launch message. Reviewer instructed to read no project files and change nothing; no plan, implementation, proof, or slice supplied. Returned numbered commitments, qualifications, journeys and exclusions are retained below (formatting normalized). Actual launch/input and return remain in the session tool history.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Shared address: A connected assistant can follow a wiki-link address to the intended note. | slice 7 |  |
| C02 | Graph address: That same addressing contract can retrieve the intended note’s graph context. | slice 5 |  |
| C03 | Read resolution: read_note uses existing wiki-link resolution rules. | slice 3 |  |
| C04 | Graph resolution: get_note_graph uses existing wiki-link resolution rules. | slice 5 |  |
| C05 | Unqualified title: A title destination requires explicit notebook scope. | slice 3 |  |
| C06 | Unqualified alias: An alias destination requires explicit notebook scope. | slice 3 |  |
| C07 | Unqualified folder path: A folder-path destination requires explicit notebook scope. | slice 3 |  |
| C08 | Unqualified exact-root path: An exact-root destination requires explicit notebook scope. | slice 3 |  |
| C09 | Qualified destination: A notebook-qualified destination supplies notebook-name scope. | slice 3 |  |
| C10 | No inferred source: Resolution does not infer a source note. | slice 3 |  |
| C11 | Name precedence: Readable display-name matches take precedence over alias matches. | slice 3 |  |
| C12 | Alias resolution: Aliases retain their existing resolution semantics. | slice 3 |  |
| C13 | Folder selection: A path-shaped reference selects using the full folder trail. | slice 3 |  |
| C14 | Exact-root selection: /Title retains its exact-root meaning. | slice 3 |  |
| C15 | Property resolution order: A property selector settles the note before checking the property. | slice 3 |  |
| C16 | Property key: Property selection requires the exact decoded property key. | slice 3 |  |
| C17 | Property-selected read: read_note still returns the containing full note. | slice 3 |  |
| C18 | Property-selected graph: get_note_graph still returns the containing note’s graph. | slice 5 |  |
| C19 | Ambiguity candidates: Multiple matches within a notebook produce the actual readable resolution candidates. | slice 4 |  |
| C20 | Candidate notebook context: Candidates provide enough notebook information for the user to choose. | slice 4 |  |
| C21 | Candidate folder context: Candidates provide the full folder information needed to choose. | slice 4 |  |
| C22 | Discovery ranking: Discovery retains existing ranked title/alias search. | slice 1 | observed: SearchControllerAliasTests.shouldRankTitleMatchesBeforeAliasMatches and NoteSearchServiceExactMatchTest full backend pass; ListNoteCandidatesTool preserves search result stream order |
| C23 | Candidate paths: list_note_candidates supplies qualified Portable paths. | slice 1 | observed: ListNoteCandidatesMcpEndpointTest returnsAliasMatchAsCandidate, equalTitlesAreDisambiguatedByNotebookAndFolder, rootCandidateUsesExactRootPathWhenTitleIsAmbiguous and candidatePathEscapesWikiDestinationCharacters HTTP qualified-address assertions |
| C24 | Direct candidate read: Those paths are accepted directly by read_note. | slice 3 |  |
| C25 | Direct candidate graph: Those paths are accepted directly by get_note_graph. | slice 5 |  |
| C26 | Focus references: MCP-generated focus-note references follow the same addressing contract. | slice 7 |  |
| C27 | Related references: MCP-generated related-note references follow that contract. | slice 7 |  |
| C28 | Outgoing references: MCP-generated outgoing-destination references follow that contract. | slice 6 |  |
| C29 | Inbound references: MCP-generated inbound-referrer references follow that contract. | slice 6 |  |
| C30 | Peer references: MCP-generated sampled-peer references follow that contract. | slice 6 |  |
| C31 | Retrieval references: MCP-generated retrieval paths follow that contract. | slice 6 |  |
| C32 | Full-read path: Full reads supply a qualified path. | slice 3 |  |
| C33 | Available revision: Full reads supply the whole notebook’s acceptedRevision when available. | slice 3 |  |
| C34 | Unavailable revision: Full reads omit acceptedRevision when unavailable. | slice 3 |  |
| C35 | Authored content: Full reads return authored Markdown verbatim. | slice 3 |  |
| C36 | Graph budget: Graph reads retain the existing combined token budget. | slice 5 |  |
| C37 | Graph traversal: Graph reads retain existing traversal behavior. | slice 5 |  |
| C38 | Graph truncation: Graph reads retain explicit body truncation. | slice 5 |  |
| C39 | Argument removal: Delete numeric note ID arguments from MCP. | slice 5 |  |
| C40 | Field removal: Delete MCP-generated note ID fields. | slice 7 |  |
| C41 | Instruction removal: Delete instructions dedicated to the removed MCP numeric-ID contract. | slice 5 |  |
| C42 | Fixture removal: Delete fixtures dedicated to that removed contract. | slice 5 |  |
| C43 | REST identities: Shared REST identities retain their existing contracts. | slice 7 |  |
| C44 | Browser identities: Shared browser identities retain their existing contracts. | slice 7 |  |
| C45 | Authored URLs: Authored Markdown URLs retain their existing contracts. | slice 7 |  |
| C46 | Candidate authorization: Existing notebook read authorization applies to resolution candidates. | slice 4 |  |
| C47 | Read authorization: Existing notebook read authorization applies to full reads. | slice 3 |  |
| C48 | Graph authorization: Existing notebook read authorization applies to graph reads. | slice 5 |  |
| C49 | Trashed notes: Existing wiki semantics exclude trashed notes. | slice 3 |  |
| C50 | Missing targets: Zero-match targets remain unresolved. | slice 3 |  |
| C51 | Ambiguous targets: Ambiguous targets remain unresolved. | slice 4 |  |
| C52 | No arbitrary choice: Resolution does not choose an ambiguous target arbitrarily. | slice 4 |  |
| Q01 | Naming assumption: The developer explicitly selected unique notebook names on 2026-11-12. | slice 7 |  |
| Q02 | Scope rationale: MCP has no source note, which explains the explicit notebook-scope requirement. | slice 3 |  |
| Q03 | Existing semantics: Wiki-link resolution and property-selection behavior are inherited contracts, subject to the stated addressing changes. | slice 3 |  |
| Q04 | Existing authorization: This work preserves current authorization. | slice 7 |  |
| Q05 | Revision meaning: The supplied revision is the whole notebook’s acceptedRevision. | slice 3 |  |
| Q06 | Revision availability: Revision metadata is conditional on availability. | slice 3 |  |
| Q07 | Limits: Deferred capabilities are scope limits; they do not independently establish rejection rules. | slice 7 |  |
| Q08 | Schema sweep: The source requires a one-time reading of tool schemas for remnants of the removed MCP numeric-ID contract. | slice 7 |  |
| Q09 | Response sweep: That reading includes responses. | slice 7 |  |
| Q10 | Documentation sweep: That reading includes documentation. | slice 7 |  |
| Q11 | Caller sweep: That reading includes callers. | slice 7 |  |
| Q12 | Sweep separation: The one-time sweep remains separate from replacement-behavior proof. | slice 7 |  |
| J01 | Qualified read: Pass Engineering:Practices/Code Review directly to read_note, without discovery first. | slice 3 |  |
| J02 | Qualified graph: Pass that same destination directly to get_note_graph, without discovery first. | slice 5 |  |
| J03 | Read content: The qualified full-read journey returns authored content. | slice 3 |  |
| J04 | Read path: That journey returns the qualified path. | slice 3 |  |
| J05 | Read revision: That journey returns the notebook revision when available. | slice 3 |  |
| J06 | Graph context: The qualified graph journey returns the existing bounded graph context. | slice 5 |  |
| J07 | Ambiguity: Two Code Review notes in Engineering yield both candidates with their full folder information. | slice 4 |  |
| J08 | Selection: A candidate’s selection path resolves the intended note. | slice 4 |  |
| J09 | Root selection: Engineering:/Code Review selects the root destination. | slice 3 |  |
| J10 | Display-name precedence: A readable display name Review wins over an alias Review. | slice 3 |  |
| J11 | Alias match: An alias resolves when only the alias matches. | slice 3 |  |
| J12 | Nested discovery: Alias discovery for a note in a nested folder yields a qualified path. | slice 1 | observed: ListNoteCandidatesMcpEndpointTest.returnsAliasMatchAsCandidate: alias query with nested Practices/Design folder and same-title root returns Engineering:Practices/Design/Colour Theory |
| J13 | Discovered read: Pass that discovered path unchanged to read_note. | slice 3 |  |
| J14 | Discovered graph: Pass that discovered path unchanged to get_note_graph. | slice 5 |  |
| J15 | Generated reference reuse: A graph-generated reference distinguishes one of two equal-titled notes in different folders and selects it on a later request. | slice 6 |  |
| J16 | Authored wiki link: An authored wiki link remains unchanged. | slice 3 |  |
| J17 | Authored Markdown URL: [Reference](/n1234) remains unchanged. | slice 3 |  |
| J18 | Address metadata: Metadata supplies paths alongside unchanged authored content. | slice 3 |  |
| X01 | Circle ownership: Circle-owned notebooks are deferred. | slice 7 |  |
| X02 | Duplicate notebook names: Handling duplicate notebook names is deferred. | slice 7 |  |
| X03 | Access changes: Adding MCP access restrictions is outside this story. | slice 7 |  |
| X04 | Access modifications: Changing existing MCP access restrictions is outside this story. | slice 7 |  |
| X05 | Ownership filter: This story does not introduce an ownership filter. | slice 7 |  |
| X06 | Naming guard: This story does not introduce a naming guard. | slice 7 |  |
| X07 | Historical reads: Reads by historical revision are deferred. | slice 7 |  |
| X08 | Rename stability: Stable addresses across renames are deferred. | slice 7 |  |
| X09 | Move stability: Stable addresses across moves are deferred. | slice 7 |  |
| X10 | Property-only retrieval: Property-only retrieval is deferred. | slice 7 |  |
| X11 | Discovery expansion: Broader discovery is deferred. | slice 7 |  |
| X12 | Web routes: New web routes are deferred. | slice 7 |  |
