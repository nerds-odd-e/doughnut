# Notebook Git synchronization architecture

This document details [ADR 0002](./adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
It shares that ADR's **Accepted** status; it is a design contract, not a claim
that every described capability is implemented. Portable file representation
and validation belong to [ADR 0004](./adrs/0004-okf-compatible-notebook-markdown-accepted.md).

## Repository and transport boundary

A notebook Git binding is server configuration containing a repository, one
accepted ref, and a root directory. Portable paths are relative to that root;
the binding is not a tracked repository file.

V1 uses a Donut-owned dedicated repository for each notebook, with exactly one
binding, the repository root as the notebook root, and `refs/heads/main` as the
only accepted remote ref. The Donut CLI may mediate acquisition and
synchronization. Direct `git clone`, `git fetch`, and `git push` against Donut
are later capabilities using the same objects, refs, and history.

Synchronization requires no `.donut` directory, manifest, local database,
extended attributes, or filesystem watcher. Binding and authentication data may
live in ordinary Git configuration or the normal credential store. Git tree
object IDs supply tree integrity checks; the commit graph supplies common
ancestors. Do not introduce a custom tree digest, sync envelope, delta format,
or three-way merge protocol. Standard Git LFS metadata and its local object
cache are allowed; local notebook workflows require Git LFS. The
[attachment storage contract](./notebook-git-lfs.md) owns object transfer,
verification, authorization, and retention.

A future binding may select a directory inside a project repository. Git
operations, permissions, commits, and integration policy then apply to the
whole repository; Donut validates and projects only the bound subtree. A commit
outside that subtree advances repository history without changing the notebook.
A commit touching several bound notebooks changes their projections at the
same repository-commit boundary. Crossing a binding boundary is deletion from
one Portable tree and addition to the other. Do not give a bound subtree
filtered or synthetic commit IDs as part of this contract.

## Creation and history reset

Every notebook has a dedicated repository and accepted `main` from the moment
it is created. Creating a notebook commits exactly one root commit, "Create
notebook", by Donut System <system@donut.local>, holding the notebook's
canonical Portable tree with the initial LFS `.gitattributes`.

A history reset replaces accepted `main` with one new root commit, "Reset:
restart Git history from the current notebook", by the same author, holding the
notebook's current canonical Portable tree. It keeps the accepted Git metadata,
such as `.gitattributes`, exactly as the accepted tree holds it.

Accepted Git content is authoritative, including pointers that select immutable
attachment versions in object storage. MySQL holds the current projection
without attachment payloads and is authoritative for private identity-bound
data, under the [attachment storage contract](./notebook-git-lfs.md).

Markdown notes and AI guidance, and non-Markdown attachments, are Portable
content. Recall history, memory-tracker state, and other private learning data
remain server-side and are not synchronized through Git. Preserving learning
history during content publication means preserving its server-side associations,
not copying it into the Portable tree. Markdown uses the existing format rules
under ADR 0004 regardless of whether it contains IDE guidance or other content.

## Attachments in the Portable tree

The [attachment synchronization contract](./notebook-git-attachments.md) details
current implementation, placement and ownership, classification, and references.
It follows the same publication guarantees and domain-operation ownership below.

## Accepted history

V1 accepts only fast-forward updates to `refs/heads/main`. Every commit has
exactly one parent except the initial root commit. Reject merge commits,
non-fast-forward or force pushes, deletion or rewind of `main`, and creation or
update of other remote branches or tags.

Donut serializes accepted content changes per notebook into this single history.
This is a publication contract, not a requirement for a single-threaded server.
The remote never merges or rebases. A divergent or stale submission is refused
without rewriting accepted history or the submitted work.

When web and local content changes independently advance from a common base,
the local side acquires the latest accepted history, rebases its unpublished
commits onto that head, resolves any conflicts locally, and pushes the resulting
fast-forward update. Local rebase may change unpublished commit IDs; Donut
preserves the submitted IDs when accepting the result. Already accepted commits
are never rebased, amended, or replaced. Local branches are possible, but only
the resulting linear fast-forward history can be published to accepted `main`.
`donut notebook pull` performs this in the same checkout: it rebases a linear
run of unpublished commits onto accepted history, whatever that history
changed, and Git pauses only on a real conflict. Pull still refuses unpublished
merge commits and unrelated history. For LFS notebooks it then fills in current
attachment files. Publish points a diverged checkout to pull.

For example, web history `A → W` and local history `A → L` become a local
proposal `A → W → L′` after local rebase. Donut accepts `L′` only if `W` is
still its accepted head and the proposal satisfies publication validation. If
the web advances again, the local side repeats the reconciliation against the
new head; the remote does not resolve that race by rewriting either side.

A full clone must receive the entire original history reachable from accepted
`main`, preserving commit IDs, parents, trees, blobs, and commit metadata.
Unsubmitted local refs, reflogs, and unreachable objects are outside this
promise. Do not compact accepted history into equivalent snapshots that change
commit IDs. Quota, backup, erasure, and garbage-collection policies remain
separate operational decisions within the attachment retention contract.
LFS payloads are external to Git blobs: a full Git bundle preserves the original
Git history and pointers, while a usable checkout separately hydrates its files.
Historical LFS objects remain available for explicit retrieval; a complete
backup includes them. Raw binary Git blobs committed before a notebook's
[LFS conversion](./notebook-git-lfs.md#notebooks-converted-from-raw-storage)
remain in full-history bundles.

Only the publication tip must satisfy
[ADR 0004 validation](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
and current business invariants. Intermediate trees may contain malformed
Markdown, temporary files, or structures Donut cannot represent. Git object
integrity, connectivity, permitted ancestry, authorization, and safe inspection
still apply to the entire received range. Attachment admission and referenced
object availability also apply across newly admitted history, including files
absent from the tip. Retention does not authorize rendering
historical drafts as current application content.

V1 requires no historical-checkout UI, remote-history browser, or Donut revert
endpoint. Any future restore publishes a new forward commit whose tip meets
current validation rules; it never rewinds accepted `main`.

## Publication guarantees

A publication proposes tip T against expected accepted head A. Acceptance
requires A to remain current and the complete range A–T to satisfy the ancestry
policy. Web edits and local publications compete at this same boundary; only
one update can replace a given head.

The final MySQL projection, identity outcomes, required derived state, and
accepted head become visible together or remain unchanged. Intermediate commits
supply evidence, not temporary application entities, learning updates, or index
mutations. Apply the resolved final state once. A new commit with the same tree
as A can still be accepted and can still require identity changes.

Git objects and referenced attachment payloads must be durable before
advertising a head that references them. Attachment upload verification follows
the [storage contract](./notebook-git-lfs.md#decision).
When objects and MySQL cannot share a transaction, stage immutable objects first
and use the MySQL-accepted head as publication authority. Unaccepted objects
may remain unreachable for garbage collection. Every advertised head must have
accessible objects and its committed projection.

Retrying T while it is the current accepted head succeeds without reapplying
changes or allocating identities. If the remote has advanced, do not rewind or
reapply T. Reporting an earlier successful publication requires retained
publication-boundary evidence: being an ancestor alone is insufficient.
Otherwise report the advanced head. The receipt surface remains unresolved.

Do not silently rewrite a proposed tree. Derived indexes may be rebuilt, but
Portable changes, including web-authored link rewrites, must be in the published
commit. Reject a final tree that violates a required invariant with an
actionable error. Folder representation follows
[ADR 0004](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#bundle-and-concepts);
omitting a folder's last represented path dissolves its projected Folder.

Local publication judges only the Markdown the proposal adds or changes;
Markdown whose bytes are already in accepted history, untouched or moved, is
not judged again. It checks that the live projection still matches accepted
`main` before any change and refuses a drifted projection, ahead of shape and placement refusals. Run `donut notebook diagnose <directory>`
to review a consistent accepted/live comparison. Pull receives accepted history; it
cannot repair server projection drift. Diagnosis preserves local work. A repair
requires establishing the cause: genuine live-only edits need explicit owner review
and a validated forward commit; encoding/projection defects are corrected against
accepted content.

After reviewing genuine live edits, run
`donut notebook recover <directory> <accepted-head> <live-tree>` with the exact
accepted head and live tree reported by diagnosis. Recovery rechecks both under
the notebook lock, validates existing file/upload policies and sound identity
correspondence, and appends one commit with that accepted parent and exact live
tree. It retains existing Note/Folder and learning associations. A stale review
requires a fresh diagnosis and review. Recovery leaves local commits, staged,
dirty and untracked work unchanged. When the checkout is ready, Pull/rebase,
resolve any real conflicts, review the result, then Publish pending work.

Every accepted web editing batch that changes Portable content appends exactly
one commit; never amend or coalesce accepted or advertised commits. V1 authors
directly on `main`. For a future project binding, repository policy determines
whether Donut commits directly or opens a pull request. In either case, the
projection changes only when the commit reaches the accepted ref. Autosave
draft buffering is outside this decision.

## Domain operation ownership

The [projection and identity contract](./notebook-git-projection.md) owns domain operations and correspondence across a publication range.

## Property value dates

Each `note_property_index` row may carry `changed_at`: the committer time of the
last accepted commit that changed that parsed value (a scalar by its content, a
list item by its presence), never earlier than its note's creation. It is
derived ordering data only. Git history is its evidence, and dating writes no
Git content.

- The index refresh deletes and reinserts a note's rows, and each reinserted
  row keeps the date its (key, value) had. Body edits, reformatting, list
  reordering and edits to other properties keep dates. A new value starts
  undated.
- A web change compares each inserted or updated note's content as first
  captured with its saved content, and dates the values it added or changed
  at the appended commit's time. It reads no Git objects. AI note extraction
  is one such change: the rewritten original and the new note share one
  commit.
- An accepted publication walks the newly accepted range first-parent, newest
  first, following renamed notes. It dates each changed note's values at the
  newest commit that changed them, so an edit reverted inside one push is
  dated at the revert. Every accepting path reaches this single step before
  the head is stored. Its cost follows the range's commits and changed files.
- A value without a date sorts at its note's creation. In production, a
  scheduled job dates up to ten notebooks per run from their whole retained
  history. It selects only notebooks with a Git binding; an unbound
  notebook's values stay undated. A value already present in the root ("Create notebook" or "Reset")
  or outside the history takes its note's creation time, and dated rows are
  kept. A history reset leaves dates unchanged.
- Within a level, the assimilation sequence orders a note unit by its creation
  time and a property unit by `COALESCE(changed_at, note created_at)`. Ties
  break by note, then the note's own unit before its properties, then key,
  then list position. The query and the in-memory merge compare the stored
  times.

## Unresolved policy

- **Identity admission:** Beyond the ordinary-note correspondence policy above,
  define broader admission and cases requiring owner intent. Preserve composition
  and the existing ambiguity/analysis-limit refusals. Owner intent needs defined
  authority, binding to the proposed range, and transport.
- **Atomic failure and receipts:** Define remaining durable-object and
  multi-head receipt guarantees and how prior publication is evidenced.
- **Historical representability:** Resolve whether tip-only validation satisfies
  [ADR 0004's durable-write rule](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
  when retained ancestors are not representable. This document does not amend
  that Accepted ADR.
