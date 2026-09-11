# Preserve pipe wiki links through rich editing

Source: execution retrospective of SEED-018 story 1 and completed plan 105,
recoverable from before-cleanup commit
`d2fbd5af6926dfdab8e11927c3784aa9055c089a` at
`.planning/seeds/SEED-018-publish-large-authored-notebooks.md` and
`.planning/quick/105-accept-pipe-note-names/PLAN.md`.
Status: planned.

## Historical provenance

The reviewed implementation boundary is
`215463c04c8d398520409ef34e67c595240df4f3..0390b7081e494e48f280fdab86584b2c97716c49`
on `codex/105-accept-pipe-note-names`. Its implementation commits are:

- `2fdd9f5f1d585260c03023b731c911dddc831c9c` — centralize wiki target transformations
- `5c883183999f7d27e97169d6cd4852352b3a4ded` — read escaped wiki references consistently
- `0ccbdd0dfe8f043fb899a72e991b59d9e1a84162` — generate unambiguous wiki references
- `9a75bf3d5e8672dedd27f88ad3dc76c1158b01cb` — accept pipe-titled notes with warning
- `15150fedd8dc33c4bcb396aca667089fdf2fcc63` — preserve wiki escapes in YAML rewrites
- `6e97de1b3fd3db4a71d3ccd0d524ecc04daad95f` — prove pipe-title rename lifecycle
- `45d3b6dfb68a2708dfcd9a9ded05477b02269091` — save pipe aliases with warning
- `d9b99dde8121ad588bba15dc6faa30627fb7951d` — prove pipe-alias resolution
- `7f58f9c0192ac52c777288b8ae1e33c295c21811` — report pipe compatibility in notebook health
- `308d9606f686e582eea70db741365f971bde91a5` — prove pipe-title publication round trip
- `0390b7081e494e48f280fdab86584b2c97716c49` — publish pipe aliases atomically

Planning-only commits `3fb34d5fff3d83e28692ee7beb7cf35cb587a1b9`,
`4ba891985db8a5ed982cc5bc79a86c96742b7b84`, and
`5e0c48065aec41e30d4b5be1ae7e38f388aacc75` explain execution refinements but
are not product-change attribution.

## Goal and bounded correction

Notebook owners can edit and save rich note bodies and recognized YAML property
values containing links to pipe-bearing titles or aliases without changing the
link destination. At the reviewed boundary, rich-editor anchors store decoded
`data-portable-path` and display values, but `wikiAnchorToMarkdownToken` rebuilds
them by interpolation. Consequently a live or unresolved anchor for `A|B`
serializes as `[[A|B]]`, which targets `A` with label `B`, instead of
`[[A\|B]]`.

Use the existing frontend wiki serializer
`wikiLinkTokenFromDecodedParts` when decoded DOM attributes are converted back
to authored Markdown. This is the PFE result: the serializer already owns the
ADR 0004 escaping rule and is already used by insert and repair flows, so the
correction belongs there rather than in another escape implementation.

Preserve ordinary label syntax, property selectors, display text, unresolved
and pending links, ordinary Markdown anchors, and raw fallback behavior for
anchors without decoded Portable-path metadata. Keep backend parsing,
publication, validation, warning copy, routes, and accepted tree semantics
unchanged. Do not add another wiki grammar or broaden the original story.

This correction follows the current near-future direction by completing the
already-selected Portable notebook editing outcome without interrupting the
separately taken large-publication profiling work. Accepted
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
owns wiki escaping and
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) continues to own
resolved navigation; no exception is proposed.

## Key examples and proof ownership

- Given a rich-editor anchor whose decoded Portable path is `A|B`, saving the
  body or YAML property value emits `[[A\|B]]`, so reopening still targets
  `A|B`.
- Given decoded target `folder/A|B#prop:meaning` and display `Read | this`,
  saving emits `[[folder/A\|B#prop:meaning|Read \| this]]`; the first unescaped
  pipe remains the sole label separator.
- Given an ordinary `[[A|B]]` label link, an ordinary Markdown anchor, or a wiki
  anchor without decoded Portable-path metadata, existing serialization remains
  unchanged.

Slice 1 owns all correction promises through the shared rich-editor HTML-to-
Markdown boundary and both of its current consumers: note body serialization
and property-value serialization.

## Ordered slices

### 1. Preserve decoded wiki destinations when rich content is saved
Type: Behavior
Status: planned
Proof: Frontend rich-content serialization examples cover pipe-bearing target,
property selector, escaped display pipe, body conversion, and property-value
conversion; `CURSOR_DEV=true nix develop -c pnpm frontend:test` passes.

Behavior: Given a rich-editor wiki anchor carrying decoded Portable-path and
optional display metadata, when either the note body or a recognized YAML
property value is serialized for save, then the authored wiki token uses the
canonical escape spelling and retains the same decoded destination and visible
label. Reuse `wikiLinkTokenFromDecodedParts` from `authoredLinkMarkup` inside
the shared `wikiAnchorToMarkdownToken` path. Extend the existing
`quillHtmlToMarkdown` and `propertyValueField` black-box examples rather than
testing a new helper or duplicating the grammar matrix. Preserve the raw-anchor
fallback branches and existing ordinary-link cases.

Estimate: 4–5 active minutes, high confidence; one shared serializer change and
one proof loop cover both existing consumers.

## Verification and delivery

Run the full frontend unit suite required by the frontend rules:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

Execution, if separately authorized, follows `dough-execute-plan`: Jidoka,
fresh post-change refactoring, one coordinator formatting pass, plan update,
commit, push, and asynchronous CI handling. This retrospective does not
authorize implementation.
