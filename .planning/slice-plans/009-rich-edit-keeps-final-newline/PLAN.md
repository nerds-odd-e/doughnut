# A rich body edit keeps the file's final newline

## Source

- Story: [SEED-046#story-13](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-13)
- **Identity:** SEED-046#story-13
- Correction of SEED-046#story-5 (plan 006); provenance: plan 006 slices 1–2
  (5f71d4d237, 61d400007d) on `exec/006-web-edit-changes-only-edit`.

## Goal and scope

A rich body edit keeps the authored body's trailing newline run; a body
without one stays without one. Everything plan 006 delivered is preserved.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 `First\n\nLast\n`, edit first line → final `\n` kept | slice 1, `RichMarkdownEditor.changesOnlyTheEdit.spec.ts` |
| 2 story 4b example 1 keeps `\n` after the edited last line | slice 1, same spec: change the existing example 1 expectation to `note.replace("# Demo2", "# My Demo2")` |
| 3 no final newline stays without one | slice 1, same spec (existing example 4/8 bodies have none) |

Command:
`CURSOR_DEV=true nix develop -c pnpm frontend:test RichMarkdownEditor markdownizer quillHtmlToMarkdown noteContent NoteShowPage`

## Slices

### 1. A rich body edit keeps the file's final newline

Type: Behavior
Status: planned
Proof: the three examples above, whole emitted Markdown with `toBe`; run the
new example 1 case red before the change.

Behavior: the examples.

Change: in `composeNoteContentInPlace` (`frontend/src/utils/noteContentInPlaceEdit.ts`),
append the authored body's trailing newline run to the composed body, mirroring
how it already keeps the leading blank lines. It covers notes with and
without frontmatter only if both paths go through it; check the no-frontmatter
path (`composeNoteContentFromPropertyRows`) and cover it with example 1.

## Current decisions

- Retrospective, 2026-09-27: the loss comes from Turndown, which never ends
  its output with a newline; the fix belongs to the one frontend composition
  owner, not to Turndown rules.

## Learnings

None yet.
