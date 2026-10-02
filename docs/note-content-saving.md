# Note-content saving

Ordinary editor typing uses a one-second autosave debounce. New wiki-link text
and blur can flush it earlier. Saves are serialized, and the editor retains
newer unsaved input while applying an earlier response. Save completion includes
durable acceptance and application of the refreshed note and live link state.

The [frontend note store](frontend-note-store.md) owns cached note state,
commands and undo; ordinary content saves leave sidebar listings unchanged.

## Accepted Git changes

`AcceptedWebChangeService` owns the transaction for a complete web operation.
It locks affected notebook bindings in ascending notebook-ID order, applies the
domain operation while Hibernate flush events capture the note, folder,
attachment and notebook-readme rows it inserted, updated or deleted, and derives
the new tree from the accepted head's tree and those rows. Unchanged notes are
neither rendered nor hashed, and unchanged attachment bytes are never read; a
path-only change (folder rename or move, trash, recovery, note move) re-lists
the affected entries under their new paths with the blob ids the accepted tree
already holds. The `.keep` and README rules are applied to the directories the
change touched. A derived commit never adopts drift at untouched paths; drift
stays detectable at local publication. A canonical no-op does not append a
commit.

The same encoder, applied to an empty base with every row as an insertion,
assembles a notebook's complete tree for notebook creation, history reset and
the publication drift check, so one place decides how projection rows become
Portable entries. Publication retains entity loading where it needs identity
evidence. The no-op decision, like publication's drift check, compares path to
Git blob id maps: accepted ids come from the accepted tree objects without
reading blob content, live ids are hashed in memory, and file modes are
ignored. Native JGit `DirCache` keeps blobs the accepted tree already holds at
a path, inserts changed content and omits deleted paths.

Accepted history lives in the native object store
(`notebook_git_accepted_object`) on the connection of the surrounding
transaction, so a save's new objects and accepted head commit or roll back with
the application projection. A save writes only its new blobs, trees and
commit. Git bundles are produced only for download and clone, and read only
from publication proposals. A download loads the binding's stored objects in
one query for that download only (no cache); its head comes from the locked
binding, so it is always the latest accepted commit.

Note Markdown is stored and committed with LF line endings whatever its source
(web, API, MCP, import): `AuthoredNoteDocument.fromContent`, which every note
content write passes, replaces CRLF with LF. A lone CR is left as it is, and
attachment bytes are never normalized. A published `.md` blob that still holds
CRLF does not match the re-encoded tree and is refused.

The [Git synchronization contract](notebook-git-synchronization.md) governs
history, identity and publication guarantees.

## Rich property editing

The property panel lists properties in the order the file has them. Both the
panel and server-side property writes change only the affected entry's lines,
so comments, key order, quoting and flow lists elsewhere stay as written, but
each side makes its own edit.

Each wiki-link element in a property list can be followed independently from
the row, with its chevron panel closed or open, including in read-only views.
Mixed lists preserve plain-text elements and their order. Wiki links retain
their authored display labels, note or property destinations, and pending/dead
status; URL properties retain external links for non-wiki URL elements.

Add property opens one local draft at the end of the shared property list and
focuses its key. Selecting Add property again focuses the same draft. Add, or
Enter in the value field, accepts a complete draft once; Enter in the key field
moves focus to its value. Leaving a draft field saves nothing. Add with a blank
key or value (after trimming) names the missing fields beside the draft and
preserves the entered text. Cancel drops the draft and its message without
saving; reopening starts empty. Add and Cancel are at least 44 px high on touch
devices.

Drafts reuse the stored row's key presets and value displays, including wiki
links, URL links, image upload and Wikidata association. Drafts have Add and
Cancel controls rather than a property panel or value-dialog opener. A finished
image upload and Wikidata dialog Save remain explicit confirmations that add the
property. Drafts stay out of stored Markdown until accepted and are discarded
when the editor is left or incoming parsed properties change.

The panel edits through its property rows (`composeNoteContentInPlace` in the
frontend): a changed value rewrites that `key: value`, a removal deletes its
lines, a new property is appended before the closing fence, and a rename
changes only the key text. The panel matches keys exactly, including case.
Removing the last property drops the block.

Key presets use canonical base names and never generate numbered alternatives.
Add property keeps an occupied list-capable key, such as `url` or `example of`,
available: accepting another value promotes its scalar to a list or appends to
its list in order. Matching is exact; an authored `example of 2` remains a
separate property when `example of` is selected.

Occupied scalar-only and singleton preset slots are omitted. These include
`image`, `wikidata_id`, `question_generation_instruction`, readme `title_pattern`,
and ordinary-note `aliases`, `overlaps` and `note_level`. Recognized legacy aliases
and numbered forms occupy the same slot. Authors change their values in the
existing row; structural values retain their scalar rules.

A stored row's key suggestions also omit another row's exact base key, including
list-capable keys, while ignoring the current row when checking occupancy. Manually
renaming to an existing exact key reports a duplicate and saves nothing; it does
not merge rows. Note/readme preset scope and typed-text filtering still apply.
Authored numbered keys remain readable and editable, with their trackers unchanged.

Server-side property writes — setting `image:` or `image_mask:`, reducing a
relationship note to a source property, and removing a trashed note's links
from other notes' properties — edit through the backend's
`FrontmatterInPlaceEdit`. Setting a property matches its key ignoring case and
replaces that entry, or appends it after the last entry; a note without
frontmatter gets a new block. A value left empty by link removal loses its
entry, and a block left empty is dropped.

New keys and values are quoted when YAML needs it, by each side's own YAML
library (`yaml` in the frontend, SnakeYAML in the backend), so the two sides
may quote the same value differently.

Property drafts synchronize when incoming parsed properties change, rather
than when only the Markdown body or YAML formatting changes. This preserves
an in-progress rename and newer value across an unrelated body refresh.

Property rename and removal use blocking loading through the complete learning
tracker guard, confirmation, route update and property emission. Editor mode
switching is blocked while that operation is pending, including the `M`
shortcut. Confirmation can still complete or cancel; ordinary value edits do
not use this blocking guard. This prevents the rich editor from being unmounted
before an asynchronous property edit emits its content.

## Rich body editing

A rich body edit keeps the authored frontmatter text and the blank lines
after it exactly as written, and ends the body with the newlines the authored
body ended with (`\n` or `\r\n`), or none when it had none. The rich editor
writes `#` headings, `-` bullets, `*` emphasis (`**` strong) and `---` rules,
so a body already using those forms changes only where it was edited; other
forms (such as `+` bullets, `_` emphasis or setext headings) are rewritten to
them.

The rich editor rebuilds the Markdown body from its content on each edit, so it
keeps body image embeds (`![alt](src)`) through an edit to other text. Showing
those images in the editor is not promised.

When a note's body holds content the rich editor cannot carry through an edit,
the rich editor shows it read-only and asks the owner to switch to Markdown
mode. It renders the authored body and the Markdown it would save from what it
loaded, and compares the two HTML renderings, treating whitespace in text as a
browser displays it and keeping whitespace inside code exact. Style-only
differences such as heading or bullet style, emphasis markers, reference links
and line wrapping stay editable; raw HTML, task lists, hard line breaks, tagged
code blocks and loose lists do not. A body that renders only empty paragraphs,
line breaks, non-breaking spaces and whitespace shows nothing to lose, so it
stays editable; opening it changes nothing, and the first rich edit replaces
that invisible markup. Visible content without text, such as an image or a
rule, still goes through the comparison. Read-only viewers see no warning.
Markdown mode edits the text as typed.
