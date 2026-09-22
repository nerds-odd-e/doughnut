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
cache are allowed; local attachment workflows require Git LFS. The
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

## Cutover and creation

One fleet migration creates a dedicated repository and accepted `main` for
every existing notebook. Each receives exactly one root commit containing its
canonical Portable tree at cutover. Do not fabricate commits for earlier MySQL
history, require owner opt-in, or defer repository creation until local
acquisition. New notebooks are Git-backed from creation.

After cutover, accepted Git content is authoritative, including pointers that
select immutable attachment versions in object storage. MySQL holds the current
projection without attachment payloads and is authoritative for private
identity-bound data. Existing database payloads transition under the
[attachment storage contract](./notebook-git-lfs.md).

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
backup includes them. Legacy binary Git blobs remain in historical bundles,
even after their server-side bytes move to object storage.

Only the publication tip must satisfy
[ADR 0004 validation](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
and current business invariants. Intermediate trees may contain malformed
Markdown, temporary files, or structures Donut cannot represent. Git object
integrity, connectivity, permitted ancestry, authorization, and safe inspection
still apply to the entire received range. Attachment admission and referenced
object availability also apply across newly admitted history, including files
absent from the tip. The legacy transition does not rewrite existing commits.
Retention does not authorize rendering
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

Every accepted web editing batch that changes Portable content appends exactly
one commit; never amend or coalesce accepted or advertised commits. V1 authors
directly on `main`. For a future project binding, repository policy determines
whether Donut commits directly or opens a pull request. In either case, the
projection changes only when the commit reaches the accepted ref. Autosave
draft buffering is outside this decision.

## Domain operation ownership

One accepted-change owner coordinates complete web domain operations. Lock and
load current state, apply the complete operation, capture the projection rows
it inserted, updated or deleted, derive the new tree from the accepted head's
tree and those rows, then append one accepted commit in the publication
transaction when the root tree differs. Derivation opens the accepted root and
the ancestor directories of the paths that captured change affects, rewrites
those directories from the changed leaf toward the root, and reuses untouched
child-tree object IDs. It does not load unrelated subtrees, note bodies, or
attachment bytes in order to publish history. Cutover, history reset, drift
checks, and bundle download may still read a complete tree. An unchanged root
tree ID does not append a commit or insert new Git objects. Destination construction, collision
resolution, placement, and authored reference handling finish before that
capture closes; newly created folders and all affected in-notebook content are
captured with it. Do not publish individual low-level placement steps.

Placement, folder construction, reference choices, content persistence, deletion,
and Portable encoding remain with their domain owners. Controllers do not
duplicate Git coordination. Preserve existing non-Git behavior. A derived commit
neither adopts nor detects pre-existing projection drift at paths the change
did not touch; drift remains detectable at local publication, which does not
silently adopt it.

An operation touching several notebooks uses the same owner over their set.
Determine that set before locking, lock bindings in ascending notebook-id order,
and re-verify the set under lock. Refuse a mismatch. Apply the domain operation
once and append one accepted commit per changed locked notebook in the same
transaction, each derived from that notebook's own captured rows.

Existing coverage includes ordinary note content/title edits, creation and
movement, same-notebook folder creation, rename, move and dissolve, and
relationship reduction over its touched-notebook set. Cross-notebook move and
cross-notebook referrer rewrites remain outside this owner until selected for
delivery; this contract does not claim that all callers have been integrated.

Git publication accumulates one final correspondence between tree content and
private identities and applies that result once through the existing domain
owners. One-commit and multi-commit publication share this path. History
inspection carries identity evidence rather than replaying live mutations.
Do not introduce an event log, persisted shadow notebook, second identity
service, or per-story dispatch mode to coordinate these operations.

## Identity across a publication range

Start with the private identity mapping at A and derive one final mapping at T
using relevant paths, blobs, and parent/child relationships across the range.
Git rename labels and similarity scores are candidate evidence, not proof of
identity. Folder correspondence requires its own domain conclusion.

Supported transitions must compose: an unambiguous exact-content move followed
by a same-path edit retains the original identity even if A and T have very
different contents. Same-path and unique exact-content correspondence can
supply evidence, but do not resolve every copy, rewrite, or recreation. Broader
admission rules remain open.

Unresolved identity conflicts block publication and preserve both sides.
Do not silently transfer learning history or treat a possible move as permanent
delete/add. Owner-assisted resolution remains a future protocol decision; do
not introduce IDs, manifests, or commit trailers as an implicit solution.

A resolved move updates the original entity and retains its dependent data.
Trash moves follow [ADR 0004 — Trash](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash).
A confirmed committed deletion removes the entity and its dependent data.
Recreation starts a new identity, even at the same path with identical bytes
within one publication; later moves preserve that new identity. Resolve
possible moves before declaring deletion. An invalid intermediate notebook
alone does not establish a deletion.

Consequently, deletion followed by recreation must produce replacement and
dependent-data removal even when A and T have identical trees. `git revert`
restores Portable content, not deleted identities or learning history, including
when deletion and revert are published together.

### Ordinary-note correspondence

Use the existing Eclipse JGit rename detector with the owner's explicit 50%
similarity threshold for ordinary notes. Keep the policy in one correspondence
owner shared by pairwise detection and linear-history composition; do not add a
parallel handwritten detector or semantic identity inference. Exact equivalence
to native Git on ambiguous inputs is not promised.

Retain ambiguous exact-pair and unresolved removal/addition refusal safeguards.
A detector limit or inability to match is not evidence of intentional deletion.
Carry original identities through parent/child evidence even when accepted-to-tip
similarity is low. Endpoint matching must not override a known deletion gap or
carried lineage. Folder identity retains its existing exact-subtree semantics.

Feed correspondence into the existing final application and reference owners,
preserving authored bytes and learning state regardless of semantic content
changes. Known web operations already have explicit note identities and do not
require heuristic inference. No persisted identity journal or client-side
rename protocol is introduced.

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
