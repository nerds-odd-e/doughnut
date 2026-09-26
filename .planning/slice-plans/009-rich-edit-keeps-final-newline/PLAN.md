# A rich body edit keeps the file's final newline

## Source

- Story: [SEED-046#story-14](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-14)
- **Identity:** SEED-046#story-14
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
Status: done
Proof: the three examples above, whole emitted Markdown with `toBe`; run the
new example 1 case red before the change.
Accepted proof: red first (2 of 11 failed in the spec, final `\n` lost); then the
command above passed, 30 files, 317 tests. Example 1 is "keeps the body's final
newline" (no-frontmatter path through `composeNoteContentFromPropertyRows`);
example 2 is "keeps the authored frontmatter and blank line on a body edit";
example 3 is "keeps comments, quoting and flow lists in unsorted frontmatter
byte for byte" and the property panel edit cases (bodies end `Body`).

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

- Clearing every rich body line of a note whose body ended in a newline now
  saves just that newline (after any frontmatter); no example covers it.
- `\r\n` final newlines are kept by the code but not tested.

## Execution complete

Product advice: no backlog change. Story wrap-up should add the kept final
newline to "Rich body editing" in `docs/note-content-saving.md`.
