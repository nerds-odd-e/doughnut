# 0002 — Host Git-native notebooks backed by MySQL

**Status:** Proposed  
**Date:** 2026-08-04  
**Decision makers:** Terry Yin  
**Consulted:** (people / teams asked for advice)

## Context

Doughnut currently stores notes, folders, readmes, permissions, subscriptions,
memory tracking, and other product behavior as mutable relational state in
MySQL. Its local notebook export is a generated ZIP of Markdown files, and CLI
sync uses a private local baseline rather than a Git commit graph.

The desired architecture makes Git revisions stored by Doughnut the
authoritative record of version-controlled notebook content. The relational
note and folder content used by the web application becomes a projection of
the latest accepted Git snapshot.

Only the portable knowledge content belongs to Git: notebook and folder
readmes, folders, and notes. Images may join the Git-managed content later.
Permissions, subscriptions, memory trackers, questions, answer history,
conversations, and other Doughnut-specific behavior remain authoritative
relational data.

We want a local notebook copy to be:

- compatible with the
  [Open Knowledge Format (OKF)](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md);
- usable with normal offline Git workflows, diffs, commits, branches, and
  merges;
- able to synchronize in both directions with a remote Doughnut notebook;
- free of Doughnut note IDs, UUIDs, or other opaque stable per-note identity
  in the local Markdown tree; and
- compatible with Doughnut continuing to use MySQL as its operational
  database.

OKF represents a concept as a Markdown file and defines its public concept ID
as its path without the `.md` suffix. It recommends a Git repository as a
distribution form, but does not define synchronization behavior.

The authoritative Git objects and refs are stored behind Doughnut's database
boundary rather than maintained as a second independently authoritative
filesystem repository. Advancing the accepted Git ref, recording internal
identity lineage, and updating the current relational projection can therefore
share a transactional acceptance boundary. Immutable objects that are uploaded
but never become reachable from an accepted ref are harmless and may be
garbage-collected.

The web application autosaves more frequently than users would normally create
Git commits. Durable notebook content must nevertheless enter the authoritative
Git revision store before, or atomically with, its current projection. Any
state that is persisted without an accepted Git revision is explicitly a draft,
not the current notebook.

## Decision

### 1. Represent each Git-enabled notebook as a Git repository

Each Git-enabled notebook is represented and served as a Git repository. Its
accepted branch is the canonical version history of the notebook's portable
knowledge content, and its working tree is the canonical OKF representation of
that content.

Doughnut exposes standard clone, fetch, and push over Git HTTPS. Other Git
hosting capabilities are outside the content and authority decisions in this
ADR.

ZIP/OKF export may remain available for portability. A local copy that
participates in two-way synchronization must be a Git repository; Doughnut
will not offer filename-and-modified-time synchronization as an equivalent
mode.

### 2. Make Git revisions authoritative for portable notebook content

The Git revision store in Doughnut's database is authoritative for:

- immutable blobs, trees, commits, and their parent graph;
- the accepted `main` ref;
- notebook and folder readmes;
- folder paths; and
- note paths, titles, frontmatter, and bodies.

Authoritative relational state remains responsible for:

- notebook ownership, authorization, and sharing;
- stable internal note and folder identities and their Git path lineage;
- memory trackers, questions, answer history, conversations, and other
  Doughnut-specific behavior; and
- non-content application state that has no portable Git representation.

Relational note and folder fields that reproduce the accepted Git tree are a
current-head projection, not a second source of truth. They carry the commit ID
from which they were projected and must be rebuildable from the accepted Git
snapshot plus the internal identity lineage. Application code must not update
projected content independently of a Git revision.

### 3. Keep internal identity out of the working tree

The canonical Git tree will not contain a Doughnut note ID, database key,
UUID, or opaque stable identity token in Markdown frontmatter, Markdown body,
filename, or a committed sync manifest.

The OKF path is the public concept identity. Doughnut keeps stable internal note
and folder identities and their path lineage privately in its relational
database so DB-only learning and application data can remain attached when a
file moves or its title changes.

For an incoming change:

- an unchanged path addresses the entity previously accepted at that path;
- a new path normally creates a new entity;
- deletion tombstones the internal identity and removes it from the current
  projection without erasing DB-only history;
- an exact or otherwise unambiguous rename preserves the internal identity;
- a whole-folder move preserves uniquely corresponding descendant identities;
  and
- an ambiguous delete/add, rename, or copy is rejected for explicit resolution,
  or deliberately accepted as delete plus create. Doughnut must not guess.

Explicit rename, restore, or identity resolution is recorded as server-side
lineage associated with the accepted commit, not as identity metadata in the
working tree. A path that disappears and later reappears is a new identity by
default unless an explicit restore associates it with the tombstone.

Filename collisions use human-readable path allocation, such as
`Recipe.md` and `Recipe (2).md`. A collision suffix must not expose the
database key, and existing surviving paths must not be renumbered merely
because another colliding note is removed.

The identity row and lineage are authoritative relational data even though the
row's current title, body, and folder fields are projections. Rebuilding a
projection must update or recreate projected values without allocating new
identities or breaking references from memory trackers, questions, and answer
history.

### 4. Accept all durable content changes as Git revisions

Git pushes and web-originated content edits use the same acceptance boundary.
A durable web edit constructs a commit based on the accepted head; a pushed
commit proposes its existing Git objects and parent graph.

Before advancing the accepted branch, Doughnut must:

1. authenticate the Git user and authorize the notebook operation;
2. require the proposed change to be based on the currently accepted commit;
3. validate repository, blob, file-count, path, and canonical OKF rules;
4. compare the proposed tree with its accepted parent;
5. resolve the note and folder identity lineage;
6. atomically advance `main`, record lineage, and update the current relational
   projection; and
7. reject the whole ref update if its content or identity cannot be represented
   safely in the Doughnut domain.

Accepted commits pushed by a user retain their Git commit IDs. Web-originated
changes create server-authored commits. Derived indexes such as embeddings,
search data, aliases, properties, and wiki-link caches may update
asynchronously because they are neither authoritative Git content nor stable
identity.

Non-main branches may exist only in Git until they are merged. They do not
change the current relational projection merely by being pushed.

### 5. Use an OKF- and Obsidian-compatible canonical Markdown profile

Every note is a Markdown concept document with valid YAML frontmatter and a
non-empty `type`, as required by OKF. Doughnut uses `type: Note` for ordinary
notes and preserves author-owned and unknown frontmatter when round-tripping.

The filename is the ordinary note title. This matches Obsidian's inline-title
model and OKF's rule that a title may be derived from the filename. Doughnut
does not generate a repeated H1 heading in the body. An H1 written by the
author is ordinary note content and is preserved.

An explicit `title` frontmatter property is used when the exact Doughnut title
cannot be faithfully derived from the filesystem-safe filename, including
sanitization, truncation, and duplicate-name suffixes. When present, `title` is
the exact display title; the collision suffix belongs to the path, not the
title.

Internal links in the canonical tree use standard Markdown paths compatible
with OKF and Obsidian. The readme-to-`index.md` representation must respect
OKF's reserved index semantics. The canonical format must preserve a stable
round trip without injecting or stripping author content.

Because Git does not store empty directories, a Doughnut folder exists in the
canonical tree only when represented by tracked content. Empty folders require
an OKF-compatible tracked index or marker rather than relying on filesystem
directory state.

Git-managed images and other binary attachments are deferred. Until they are
included, notes may reference attachments outside the Git repository using
portable URLs.

### 6. Serve application reads from a verifiable current-head projection

The Doughnut application may read current note and folder content from
relational projection tables for efficient queries and compatibility with its
existing domain model. Each projected aggregate records the accepted commit it
represents.

Projection updates required for a coherent notebook view are part of commit
acceptance. Expensive derived indexes may lag and recover independently. A
projection can be discarded and rebuilt from the accepted Git snapshot and
the authoritative identity lineage without changing note IDs or DB-only
history.

Repository reads advertise only accepted refs. Application reads must not
silently treat a projection from a different commit as current.

### 7. Keep non-content Git hosting capabilities outside this decision

Standard Git clone, fetch, and push are part of the desired notebook host.
ZIP/OKF export may remain as a non-Git portability mechanism, but it is not an
equivalent two-way synchronization mode.

SSH transport, pull-request workflows, arbitrary server-side refs, Git LFS,
branch-aware web editing, and storage of images in Git are deferred. Deferring
them does not change the authority or representation decisions above.

## Consequences

- Git revisions, rather than mutable note content rows, determine the current
  portable notebook content.
- Doughnut remains a MySQL-backed application, but its database contains both
  authoritative Git revision/identity state and rebuildable current-head
  projections with different responsibilities.
- A Git clone replaces `.doughnut-sync` baseline content for Git-based local
  workspaces; the Git commit graph supplies the common ancestor.
- Stable note identity remains available to memory trackers, questions, answer
  history, and other DB-only data without appearing in the local Markdown.
- Web content writes must create Git commits. Autosave batching can exist only
  as explicit draft behavior until a commit is accepted.
- The canonical OKF codec becomes a cross-cutting compatibility contract. A
  lossy or nondeterministic projection is not acceptable.
- Ordinary title changes are filename changes and can preserve identity as
  exact-content Git renames. Generated title H1 headings disappear; author H1
  headings remain content.
- The system gains operational responsibility for Git object storage,
  authentication, pack validation, quotas, garbage collection, backup,
  recovery, and abuse prevention.
- Deletion and privacy policies must account for historical Git objects and
  already-created clones. Removing current MySQL rows does not erase content
  from prior commits or users' local repositories.
- Direct pushes may be rejected when the remote web notebook has advanced, an
  edit cannot be represented by Doughnut, or identity-free rename inference is
  ambiguous. Users must fetch and merge/rebase, or resolve the operation
  explicitly.
- Rebuilding the current relational projection must preserve internal identity
  and DB-only history; dropping and recreating note rows is not a valid cache
  rebuild strategy.

## Pros

- Gives local users standard Git history and merge ancestry instead of a
  timestamp-based sync protocol.
- Aligns notebook distribution with OKF's recommended Git form.
- Keeps internal database identity and implementation metadata out of the
  user's Markdown.
- Preserves MySQL-backed permissions, learning history, conversations, and
  other Doughnut-specific features while making portable content independent.
- Allows branches to remain cheap Git-only state until a merge affects the
  live notebook.
- Makes the current application representation rebuildable and verifiable
  against an accepted commit.

## Cons

- Requires Git object/ref storage, an identity lineage model, and a canonical
  projector rather than the current mutable-row and one-way ZIP model.
- Requires existing write paths to create commits instead of directly mutating
  projected content.
- Adds repository storage and Git server operational costs.
- Identity-free Markdown makes some rename/copy cases inherently ambiguous.
- Git history complicates retention, erasure, and authorization changes after
  content has been cloned.
- Filename-derived titles require explicit `title` frontmatter for names that
  cannot round-trip through a portable filesystem path.

## Architectural constraints / Assumptions

- Git blobs, trees, commits, and refs are stored with enough fidelity to serve
  and retain the exact accepted Git object IDs.
- The accepted ref, internal identity lineage, and coherent current projection
  share a transactional acceptance boundary.
- A projection round-trip from accepted Git tree to relational state and back
  reproduces the accepted tree without unintended semantic or textual drift.
- The canonical codec defines title, folder/readme, link, attachment, reserved
  filename, frontmatter, duplicate-name, and filename-sanitization behavior.
- Every durable application mutation that affects portable notebook content
  creates an accepted Git revision.
- Existing Doughnut authorization remains the source of Git read/write
  permissions.
- Repository operations constrain refs, blob sizes, file counts, pack sizes,
  and force pushes according to Doughnut's product and security policies.
- Repository history and DB-only learning data have explicit backup, retention,
  privacy, and erasure semantics.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links:
  - ADR-0000 [Use Architectural Decision Records](./0000-use-adrs-accepted.md)
  - [ADR playbook](./README.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Obsidian settings: inline titles](https://obsidian.md/help/settings)
  - [Obsidian note management](https://obsidian.md/help/manage-notes)
  - [Git rename detection](https://git-scm.com/docs/git-status.html)
