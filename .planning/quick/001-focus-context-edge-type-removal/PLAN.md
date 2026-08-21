# Remove `edgeType` from focus context retrieval

## Goal

Delete the `edgeType` concept end to end. Focus-context results (markdown prompt
and `GET /api/notes/{note}/graph` JSON, therefore MCP `get_note_graph`) stop
labelling *how* a related note was reached. No renamed survivor, no negation
left behind.

**Rationale:** the consumer is an AI. OKF is a format with untyped links and no
retrieval contract; Obsidian Copilot feeds the model title/path/content;
Smart-Context-style packs use link direction only to *choose* notes. Direction
and folder adjacency belong to retrieval internals, not to the model-facing
payload.

## Removal rules (apply to every slice)

- Assertions on removed behavior are **deleted**, not inverted. Where the whole
  test only asserted `edgeType`, delete the test; where a separate claim
  survives, keep that claim and rename the test to it.
- Docs lose the removed text. No "we no longer emit…" sentences.
- Test and helper names must not mention edge types after their slice.

## Design decisions

**Wiki-reached vs folder-peer is still observable without `edgeType`.**
`retrievalPath` for a wiki-reached note ends at that note (`size == depth + 1`);
for a folder peer it ends at the *anchor* (`size == depth`). Tests use that
structural fact instead of an edge label. Where a test is about wiki mechanics
only, a content budget below
`FocusContextConstants.MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT` already
suppresses folder peers through the production path.

**Internal edge priority goes too.** Inside one depth iteration every `Proposal`
carries the same `depth`, so `beats()` is decided solely by `edgePriority`, and
inbound candidates already exclude that parent's outgoing targets. Dropping the
priority leaves first-proposal-wins, which differs only for a note proposed by
two *different* frontier parents at the same depth — and only in which
`retrievalPath` is reported. No test covers that; emission order is unchanged.
Dedupe becomes a plain first-wins map.

**Doc has a stale claim to clear at the same time:** the design doc lists an
optional `reason` field on related notes that no longer exists in code.

## Slices

### 1. Focus context prompt drops the reached-by label — Behavior

Related-note blocks in the rendered `<focus_context>` markdown no longer carry a
`Reached by:` line.

- `FocusContextMarkdownRenderer.appendRetrievedNote` — delete the `Reached by:`
  branch.
- `FocusContextMarkdownRendererTest` — drop the `Reached by:` assertions; rename
  `folderSiblingShowsPathToAnchorAndEdgeType` to its surviving claim
  (`folderSiblingShowsPathToAnchor`), `depth1OutgoingLinkFormatsPathEdgeAndBody`
  to `depth1OutgoingLinkFormatsPathAndBody`.
- `e2e_test/start/questionGenerationService.ts` — the three Mountebank
  discriminators stop matching `Reached by:`. Replacements keep them mutually
  exclusive within the scenario notebook: depth-two = `Title: FarDepthTwo` plus
  a `Path:` with two `->`; folder peers = `Title: FocusFolder` with
  `Title: SibOne` / `Title: SibTwo`; wiki-linked = `Title: WikiRecall` with
  `Title: Bahamas`.
- `e2e_test/start/mock_services/openAiFocusContextRecallAssertions.ts` — same
  substitution for the three prompt-shape expectations and the per-body hint
  lines.
- `docs/focus-context/focus_context_retrieval_design.md` — remove
  `Reached by: OutgoingWikiLink` from the rendered sample.

`edgeType` is still on the JSON payload after this slice, so it is stop-safe.

**Tests:** backend unit suite; E2E `--spec
e2e_test/features/recall/recall_quiz_ai_question.feature`.

Status: done

**Learnings:** Title/path discriminators replace `Reached by:` in E2E (shared
`focusContextRecallPromptShapes.ts`). After Java edits, Spring DevTools can
leave the backend unhealthy — `pnpm sut:restart` if E2E fails with missing
beans.

### 2. Retrieval tests identify related notes without the edge label — Structure

No production change. Test-side only, so the field can be deleted in slice 3
without rewriting assertions at the same time.

- `FocusContextRetrievalTestBase` — replace `folderSiblingTitles` with helpers
  built on the path-shape fact above (folder peer: `retrievalPath.size() ==
  depth`; wiki-reached: `== depth + 1`).
- `FocusContextRetrievalFolderSiblingTest`, `…DepthTraversalTest`,
  `…InboundSamplingTest` — switch every `getEdgeType()` filter to those helpers.
  For the wiki-only claims in `Depth1CapAndSeed`, `Depth2InboundCap` and
  `maxDepthOneSkipsSecondHop`, prefer a content budget under
  `MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT` so folder peers cannot enter at
  all, rather than filtering them out after the fact.

All existing tests must still pass unchanged in intent.

**Tests:** backend unit suite.

Status: planned

### 3. Focus context payload drops `edgeType` — Behavior

`relatedNotes` entries in `GET /api/notes/{note}/graph` (and therefore MCP
`get_note_graph`) no longer carry `edgeType`; the enum is gone from the codebase.

- Delete `FocusContextEdgeType`.
- `FocusContextNote` — drop the field, getter and constructor parameter.
- `FocusContextRetrievalService` — drop `edgeType` from `Proposal`, delete
  `beats` / `edgePriority`, dedupe first-wins.
- `FocusContextConstants` — reword the
  `MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT` javadoc so it no longer links the
  enum.
- Regenerate `open_api_docs.yaml` and
  `packages/generated/doughnut-backend-api/**` (`pnpm generateTypeScript`).
- `mcp-server/src/tools/get-note-graph.ts` — remove `edgeType` from the tool
  description's `relatedNotes` sentence.
- Delete the remaining `edgeType` assertions and rename their tests:
  `NoteControllerGraphTests.relatedNotesExposeEdgeTypeDepthAndPath`,
  `FocusContextRetrievalServiceTest`
  (`outgoingWikiLinkEmitsTargetWithEdgeTypeDepthAndPath`,
  `inboundReferrerEmittedWithCorrectEdgeType`,
  `noteReachedAsBothOutgoingAndInboundKeepsOutgoingEdgeType` — the surviving
  claim there is "appears once"), `FocusContextRetrievalFolderSiblingTest`
  (`folderSiblingsIncludeStructuralPeersInSameFolder` keeps the
  no-duplication claim via the exact related set;
  `folderSiblingIsNotWikiExpansionFrontier` becomes "DeepOnly is not retrieved").
- `docs/focus-context/focus_context_retrieval_design.md` — delete the Edges
  table and the `edgeType` entry in the API shape; describe the three inclusion
  sources as retrieval mechanics in prose (outgoing wiki links, inbound wiki
  references, sampled folder peers), keep the depth/priority statement only as
  "shortest path wins", and drop the stale `reason` field.

**Tests:** backend unit suite; MCP tests; frontend unit tests (generated types);
`pnpm lint:all` for the regenerated OpenAPI.

Status: planned

## Out of scope

- `depth` and `retrievalPath` stay on the payload.
- Focus-note `outgoingLinks` / `inboundReferences` / `sampleSiblings` stay — they
  are link lists, not per-note edge labels.
- Any change to which notes retrieval selects, beyond the cross-parent tie noted
  above.
