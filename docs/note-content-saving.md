# Note-content saving

Ordinary editor typing uses a one-second autosave debounce. New wiki-link text
and blur can flush it earlier. Saves are serialized, and the editor retains
newer unsaved input while applying an earlier response. Save completion includes
durable acceptance and application of the refreshed note and live link state.

## Accepted Git changes

`AcceptedWebChangeService` owns the transaction for a complete web operation.
It locks affected notebook bindings in ascending notebook-ID order, compares
the current persisted Portable tree with the accepted tree, applies the domain
operation, flushes and reads the complete final projection. Pre-existing drift
keeps its existing policy: that notebook's web change is not appended to Git.
A canonical no-op does not append a commit.

Snapshots use the shared flat export rows without hydrating every note and
folder as editable entities. Publication retains entity loading where it needs
identity evidence. Both the drift and no-op decisions, like publication's drift
check, compare path to Git blob id maps: accepted ids come from the accepted
tree objects without reading blob content, live ids are hashed in memory, and
file modes are ignored. One final snapshot supplies both comparison and commit
construction. Every final path is considered, including changes to other
notes, README, trash and empty folders. Native JGit `DirCache` keeps blobs the
accepted tree already holds at a path, inserts changed content and omits
deleted paths.

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
