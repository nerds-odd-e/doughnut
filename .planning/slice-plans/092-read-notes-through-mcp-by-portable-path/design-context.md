# Selected design and existing owners

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
