# Note-content saving

Ordinary editor typing uses a one-second autosave debounce. New wiki-link text
and blur can flush it earlier. Saves are serialized, and the editor retains
newer unsaved input while applying an earlier response. Save completion includes
durable acceptance and application of the refreshed note and live link state.

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
assembles a notebook's complete tree for repository cutover, history reset and
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
from publication proposals.

The [Git synchronization contract](notebook-git-synchronization.md) governs
history, identity and publication guarantees.

## Rich property editing

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

The rich editor rebuilds the Markdown body from its content on each edit, so it
keeps body image embeds (`![alt](src)`) through an edit to other text. Showing
those pictures in the editor is not promised.
