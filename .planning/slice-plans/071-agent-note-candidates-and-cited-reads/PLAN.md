# Let an external agent inspect several candidate notes and cite its evidence

**Identity:** SEED-073#story-4

## Source

[Let an external agent inspect several candidate notes and cite its evidence](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-4).
The seed holds the goal, scope, key examples, explored directions, the
borrowed mechanism and its limits, and the Architecture note.

## Goal and scope

An agent connected to the Donut MCP server lists the note candidates for a
search text, tells equal titles apart by notebook and folder, reads a chosen
note in full, and cites it by note id and the notebook's accepted Git revision
at the time of the read. The single-best-note tool is replaced by the candidate
list; the bounded-context tool stays unchanged.

Excluded (deferred, not rejected): body-text candidates
([story 3](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-3)),
reading a note at an older revision, a "still current?" check, a web link in
the response, circle-wide search scope, and remote hosting
([SEED-002](../../seeds/SEED-002-host-mcp-over-https.md)).

Assumptions: the MCP server stays on stdio with the existing access token;
the HTTP API and OpenAPI client remain the only path from the MCP server to
the backend; every notebook created through the product has an accepted Git
revision, and a notebook without one yields no revision.

## Outside-in proof

| Key example | Observable signal | Owner |
| --- | --- | --- |
| 1: equal titles in different places are listed as distinct candidates | MCP candidate tool returns every note hit the search endpoint returns, in order, each with note id, title, notebook name, folder, and distance; a unit case with two hits titled "Functional" in different notebooks and folders shows both | Slice 1: `mcp-server/tests/tools/<candidate tool>.test.ts`; `mcp_services.feature` lists several candidates for one term |
| 5: no match | MCP candidate tool returns the text `No matching notes.` | Slice 1: unit case and the existing "Fiona" row of `mcp_services.feature` |
| 6: bounded context still works with a candidate id | Existing graph scenarios of `mcp_services.feature` pass using the first candidate's id | Slice 1: retained scenario outline, step reads the id from the candidate list |
| 2: a read returns full content with attribution | `GET /api/notes/{note}/<attributed read>` returns note id, title, notebook name, folder path, content, updated time, and the notebook's accepted revision equal to its binding head | Slice 2: new `NoteController` test class |
| 3: a later edit does not relabel old evidence | After a web content edit of a note in the same notebook, the same read returns a different revision; the earlier value is unchanged in the caller's hands | Slice 2: same test class, read → edit → read; Slice 3: e2e scenario reads, edits on the web, reads again and sees a different 40-hex revision |
| 4: access | Reading a note in a notebook the user cannot read throws `UnexpectedNoAccessRightException`; such notes do not appear in candidates (existing search scoping, retained `SearchControllerTests`) | Slice 2: unauthorized case; Slice 3: MCP read surfaces the backend error text, no content |
| unversioned notebook | A note whose notebook has no Git binding reads with no accepted revision | Slice 2: case using `makeMe.aNote()` without product notebook creation |
| 2: the agent reads through MCP | `read_note` returns the backend payload as JSON; a backend error becomes an `ERROR:` text | Slice 3: `mcp-server/tests/tools/read-note.test.ts`; `mcp_services.feature` read scenario |
| descriptions are accurate | Tool list shows the three tools with descriptions that match their returns | Slice 1 and 3: `mcp-server/tests/server.test.ts`; `.agents/skills/mcp-server/SKILL.md` tool list |

Local gates, cited from the stack skills: `CURSOR_DEV=true nix develop -c pnpm mcp-server:test`
for the MCP server; focused backend classes with
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests '<class>'`;
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`
in this worktree's isolated stack. Slice 2 changes a controller signature, so
it runs `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` and
`RobotsTests` (`openApiDocsMatchCommittedYaml`) in the same slice, as the
`generate-api-client` skill requires.

## Decisive premises

Observed on 2026-10-17 in this workspace at `0ed49423da`:

- **The search endpoint already returns several note hits with notebook name
  and folder, scoped to the user's notebooks and subscriptions.** Consuming
  operation: `SearchController.searchForRelationshipTarget`. Observation:
  `backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests 'com.odde.donut.controllers.SearchControllerTests'`
  under the Nix shell. Result: 11 tests, 0 failures, including
  `shouldReturnMatchingNotes` (notebook name on every hit),
  `noteSearchHitShowsContainingFolderNameAndOmitsItAtNotebookRoot`,
  `literalSearchReturnsAllMatchingNotes`, and `shouldRespectSearchScopeSettings`.
- **A web content edit advances the notebook's accepted revision, and a
  product-created notebook has a 40-hex revision before any edit.** Consuming
  operation: `TextContentController.updateNoteContent` then
  `NotebookGitBindingRepository.findByNotebook_Id`. Observation: a scratch
  test class extending `NotebookGitWebContentControllerTestBase` created a
  notebook through `createGitBackedNotebook()`, added a note, snapshotted the
  tree, edited the note's content, and asserted both heads match
  `[0-9a-f]{40}` and differ. Same Gradle command with
  `--tests 'com.odde.donut.controllers.ScratchRevisionAdvanceTest'`. Result:
  1 test, 0 failures. The scratch class was deleted before this plan was written.
- **The MCP unit suite runs here.** `pnpm mcp-server:test`: 6 files, 11 tests
  passed in 220 ms.
- **The MCP e2e proof route runs in this worktree.** Consuming operation:
  `pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature` with the
  worktree's isolated stack. Result: `All specs passed`, 5 of 5 scenarios in 6 s (the
  `@mcpIsolationProof` scenario is excluded by tag without rendezvous
  parameters); the run created this worktree's ignored `.worktree.local.json`
  identity and its own E2E database and ports.
- **Settled by reading only, not decisive for approach:** the accepted
  revision lookup `bindingRepository.findByNotebook_Id(id).map(getAcceptedGitObjectId).orElse(null)`
  already appears in `WebNoteEditService`, `NoteConversationAiReplyService`,
  `ConversationCorrectionReviewService`, and
  `ConversationCorrectionAcceptanceService`; `FocusContextFocusNote` already
  computes a note's `folderPath`. These are reuse candidates, not premises
  the slices depend on.

## Slices

### 1. Agents list the note candidates for a search text

Type: Behavior
Status: planned
Proof: new unit cases in `mcp-server/tests/tools/` for the candidate tool
(several hits pass through in order with id, title, notebook, folder,
distance; two equal titles stay distinct; container hits are left out; empty
result gives `No matching notes.`); `server.test.ts` lists the renamed tool;
`mcp_services.feature` passes with the "Fiona" row expecting
`No matching notes.` and a scenario that lists more than one candidate for
one term (for example "Functional" against notes "Functional" and
"Functional analysis", exact first). The sweep reading at acceptance: a
search for `find_most_relevant_note` over the product returns nothing.

Behavior: notes whose titles or aliases match a text exist in the user's
notebooks and subscriptions → the agent calls the candidate tool with that
text → it receives every note hit from `POST /api/notes/search` as JSON
candidates (`noteId`, `title`, `notebookName`, `folderName`, `distance`),
in the backend's order; with no note hit it receives `No matching notes.`.
The tool replaces `find_most_relevant_note` (file, test, description
references in `get_note_graph`, the tool list in
`.agents/skills/mcp-server/SKILL.md`, and the e2e page object
`mcpAgentActions.searchForNote` / `getNoteGraphFromLastSearch`, which now
takes the first candidate's id). Its description says it matches titles and
aliases, returns candidates to compare, and that `read_note` (slice 3) reads
one in full.

### 2. A note read carries its source attribution over HTTP

Type: Behavior
Status: planned
Proof: a new `NoteController` test class: the read returns note id, title,
notebook name, folder path, content, updated time, and an accepted revision
equal to the binding head; after `TextContentController.updateNoteContent`
on a note in the same notebook the revision differs; an unauthorized note
throws `UnexpectedNoAccessRightException`; a note whose notebook has no
binding reads with a null revision. `generateTypeScript` regenerates
`packages/generated/donut-backend-api`; `RobotsTests` stays green.

Behavior: a readable note exists → `GET /api/notes/{note}/<attributed read>`
→ one read-only transaction returns the note's content with `noteId`,
`title`, `notebookName`, `folderPath`, `updatedAt`, and `acceptedRevision`
(the notebook's `NotebookGitBinding.acceptedGitObjectId`, null when the
notebook has none). Name the endpoint and DTO by capability (for example
`attributed-content` / `AttributedNoteContent`); no planning number in
product code.

### 3. An agent reads a candidate in full and cites its revision

Type: Behavior
Status: planned
Proof: `mcp-server/tests/tools/read-note.test.ts` (payload passthrough as
JSON; backend error becomes `ERROR:` text; the description names the
accepted revision as the whole notebook's commit and says content is the
user's note text); `server.test.ts` lists three tools; `mcp_services.feature`
scenario: list candidates for a term, read the first candidate, the response
contains the note's content and a 40-hex `acceptedRevision`; the user edits
that note's content on the web; a second read returns a different revision.
`.agents/skills/mcp-server/SKILL.md` tool list names all three tools.

Behavior: the agent holds a candidate id → it calls `read_note` with it →
it receives the slice 2 payload as JSON; a refused or missing note yields the
backend's error as tool error text and no content.

## Current decisions

- Replace the single-best tool; do not keep it beside the list. One search
  concept, accurate descriptions, and the removal leaves no trace.
- Candidates are note hits only, in the backend's order with its `distance`;
  folder and notebook hits are not candidates. No new ranking or limit.
- The revision is the notebook's accepted Git commit read in the same
  transaction as the content, following ADR 0002's "existing identities and
  revisions for attribution". Absent binding → null, stated in the tool
  description; nothing is invented.
- `folderPath` reuses the owner that `FocusContextFocusNote` already uses; do
  not add a second path computation.
- The repeated accepted-revision lookup may get one owner during the
  post-change refactor if a coherent home exists; do not add a mechanism for it.
- The MCP read returns the backend payload unchanged; the MCP server adds no
  shaping beyond JSON text, as the graph tool does today.
- Interim behavior: none. After slice 1, `get_note_graph` consumers follow the
  candidate list; nothing is left half-renamed.

## Learnings

None yet.
