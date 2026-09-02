# Authoritative authored note references

**Status:** complete  
**Architecture:** ADR 0004, “Links and attachments”  
**Goal:** Make authored semantic note references a domain-owned representation derived from note Markdown, and make every consumer resolve those references against current state without notebook-wide cache refreshes.

## Outcome (shipped)

- `AuthoredNoteReference` is the one domain type for wiki Portable-path targets and semantic Donut note-ID URLs.
- Stored note Markdown remains authoritative. A source-owned `authored_note_reference` index mirrors every distinct authored reference, including missing and ambiguous wiki targets.
- Resolution has one sealed result (`Resolved`, `Missing`, or `Ambiguous`) computed from the authored reference, source scope, current notebook state, and current viewer.
- Outgoing links, inbound references, rename/delete/move rewriting, focus context, and property assimilation use `NoteReferenceService` / `AuthoredNoteReferenceInboundFacade`. No persisted resolved-link cache.
- Every production content mutation updates Markdown and its authored-reference children through `Note.replaceContent(AuthoredNoteDocument)`.
- Same-note body autosave completes before delete (`noteContentMutationBarrier`). Delete/restore do not walk the notebook.
- `resolved_wiki_link`, `ResolvedWikiLink*`, `NotePropertyIndex.targetNote`, and notebook-scope resolution refreshes are removed (`V300000315`, `V300000316`).

## Maintainer notes

- **Index writes:** production create/save/rewrite go through `Note.replaceContent`. Rewrites use `WikiLinkRewriteSupport.documentFromRewrittenContent`. Tests that need inbound discovery use `MakeMe.authorReferencingContent`.
- **Derived indexes:** `NoteReferenceService.refreshDerivedIndexesForNote` rebuilds property/alias/level indexes only; resolution is always live.
- **Property index:** links to `authored_note_reference_id` via `sourceLocalKey`; assimilation gates via live resolution at read time.
