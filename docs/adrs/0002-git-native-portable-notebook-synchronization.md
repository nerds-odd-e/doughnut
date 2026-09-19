# 0002 — Git-native Portable notebook tree synchronization

**Status:** Proposed

**Date:** 2026-09-04

**Decision makers:** Terry Yin

## Context

Donut stores notes, folders, Readmes, and their stable private identities in
MySQL. It also stores non-portable data attached to those identities, including
memory trackers, questions, conversations, indexes, and learning history. A
local copy represents only the **Portable notebook tree** defined by
[ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md). It has paths
and Markdown, but no Donut note or folder IDs.

The synchronization requirements are:

1. A user can keep an ordinary local copy of the Portable notebook tree.
2. Accepted remote changes can be reproduced identically in that local copy.
3. Local tree changes can be synchronized to the remote notebook.
4. Concurrent or otherwise incompatible changes are detected before either
   side is overwritten.
5. Either side may receive an accumulated linear range from the other without
   rewriting commit IDs. Independently advanced histories are detected and
   preserved; automatic reconciliation is outside the selected v1 direction.
6. A local note or folder rename/move updates the corresponding existing remote
   entity when that identity conclusion is sound, so non-portable data remains
   attached to the correct entity.

The local usability constraint is equally important: the working tree must not
require a `.donut` directory, a manifest, a local database, extended attributes,
or any other Donut-specific state beside the Portable notebook tree. Standard
Git manages local commits and history. Donut's supported synchronization flow
preserves their IDs and does not require rebasing or squashing. V1 may require the
Donut CLI to acquire and synchronize the repository; direct `git clone`,
`git fetch`, and `git push` against Donut are a later capability. Ordinary Git
repository metadata and minimal binding/authentication data in Git
configuration or the normal credential store are allowed because none of that
state belongs to the Portable tree.

## Decision

### Make Git the Portable tree synchronization contract

The accepted commit at the notebook binding's Git ref is authoritative for the
accepted Portable notebook tree. Git object IDs are the revision identifiers;
the commit graph is the retained content history; and Git object and ref
exchange is the transport model. V1 integration advances one linear mainline
without rewriting history. V1 may use the Donut CLI to mediate repository acquisition and
synchronization. Direct standard-Git remote access is a later transport surface;
adding it must not introduce a second revision, history, or merge model.

Donut does not create a parallel notebook revision number, tree digest
protocol, sync envelope, delta format, or custom three-way merge protocol.
Use Git tree object IDs for integrity checks and the Git commit graph for
common ancestors.

The working tree contains only ADR-0004 Portable notebook files. In
particular, it contains no Donut note/folder IDs and no synchronization
manifest. The only additional state in a normal local checkout is ordinary Git
state such as `.git`, refs, the index, and Git configuration.

### Bind a notebook to a Git tree, not permanently to a repository

The durable architectural concept is a **notebook Git binding** consisting of:

- a Git repository;
- one accepted ref; and
- a root directory whose tree is the Portable notebook tree.

Portable paths are relative to the binding's root directory. The binding is
Donut server configuration; it is not a file that must be added to the
repository.

V1 supports the deliberately smaller case:

- one Donut-owned dedicated repository per notebook;
- `refs/heads/main` as the only accepted remote ref;
- the repository root as the notebook root; and
- at most one notebook binding in the repository.

This does not encode `Notebook == Repository` as a permanent domain invariant.
A future binding may select `notes/`, `docs/knowledge/`, or another directory
inside a larger project repository. Git operations, permissions, commits,
and any future integration policy then apply to the whole project repository. Donut
validates and projects only the bound subtree. A commit outside it advances the
repository without changing that notebook; a commit touching multiple bound
notebooks changes their projections at the same repository-commit boundary.
Moving a file across a binding boundary is a deletion from one Portable tree
and an addition to the other.

Do not serve a bound subtree with filtered or synthetic commit IDs as part of
this synchronization contract.

### Bootstrap every notebook at cutover

One fleet migration creates the dedicated Git repository and accepted `main`
for every existing notebook. Each repository receives exactly one root commit
whose tree is the notebook's canonical ADR-0004 Portable notebook tree at
cutover. Donut does not fabricate commits for earlier MySQL history, require
owner opt-in, or wait for a first local acquisition before persisting the Git
repository. New notebooks are Git-backed from creation.

After this cutover, the accepted Git `main` is authoritative for Portable
content. MySQL remains its current application projection and the authority for
Donut-only identity-bound data.

### Keep one linear, append-only accepted mainline in v1

The remote accepts only fast-forward updates to `refs/heads/main`. Each
accepted commit has exactly one parent except the repository's initial commit.
V1 rejects:

- merge commits;
- non-fast-forward and force pushes;
- deletion or rewind of `main`; and
- creation or update of any other remote branch or tag.

The supported Donut workflow uses this one mainline, with no branching,
merging, rebasing, squashing, or amendment of local or accepted commits to make
publication possible. Git itself permits local branches; that does not make
publication of a divergent branch supported. If both sides independently
advance from a common ancestor, v1 fails clearly and preserves both histories.
Divergent-history reconciliation is outside v1 scope.

### Retain history independently of application representability

An accepted publication makes its entire reachable original Git history
available to a full clone, preserving commit IDs, parents, trees, blobs, and
commit metadata. This does not include unsubmitted local refs, reflogs, or
unreachable objects. It does not promise that Donut can render or materialize
every historical tree as an application revision.

Only the newly proposed tip must have a valid Donut projection under ADR 0004
and current business rules. Intermediate commits may contain malformed
Markdown, temporary files, or intermediate structures that Donut cannot
represent. They remain ordinary Git history. Git object integrity, connectivity,
permitted ancestry, authorization to publish, and safe storage/inspection still
apply to the received range. Historical notebook validity is distinct from
valid Git objects. A historical draft is not parsed or rendered as current
Donut content merely because its objects are retained.

Previously published tips were valid when accepted; no claim is made that
all retained ancestors were published tips or remain valid under future rules.
A historical restore, if introduced, would be a new forward publication whose
proposed tip must meet the then-current requirements.

### Apply one final projection atomically

The accepted Git tree is authoritative for accepted Portable content. MySQL
holds its current projection and the server-private Note/Folder identities and
associated non-portable data. Start from the identity mapping at the current
accepted publication boundary; preserving Git history does not require a
materialized database revision or entity for every intermediate path.

For a proposed linear range from accepted A to tip T:

1. Authenticate and authorize the update; receive and verify the immutable Git
   objects without advertising T.
2. Require the expected old head to equal the current accepted head and verify
   that the complete range is a permitted linear fast-forward from A.
3. Validate T's Portable tree and business invariants. Do not demand Donut
   representability of each intermediate tree.
4. Determine the correspondence between current private identities and T,
   using relevant evidence from the proposed history. Resolve ambiguity before
   any destructive application operation.
5. Compute the final creates, edits, moves, and removals. Apply that result to
   the current projection once; do not replay each commit through Donut's live
   create/edit/delete handlers or instantiate temporary historical entities.
   Internal dependency ordering within this one final application is allowed.
6. Atomically commit the final projection, identity outcomes, required derived
   state, and accepted head T; then advertise T. Otherwise accept none of the
   publication and leave the previous live state and head intact.

History analysis may traverse commits and carry tentative identity mappings
in memory. That is not replaying intermediate Donut application states. A
note moved in B and edited in C can retain its original identity while only
C's final contents are persisted. An invalid draft repaired at T does not
prevent publication when final state and identity are resolved.

A new tip with the same tree as A still represents new history and can be
accepted. Identity-sensitive deletion/recreation must be resolved even when
the endpoint tree is unchanged. Paths present only in unpublished intermediate
revisions require no temporary Note entities or learning/index side effects.

The final application is itself mutating. Any Portable path it resolves must
come from live entity state, not from a projection snapshot taken before it
began; a snapshot may only be compared against another snapshot.

Git objects must be durable before a head referencing them is advertised. When
objects and MySQL do not share a transaction, stage immutable objects first and
make the MySQL-accepted head the publication authority. Unaccepted objects may
remain unreachable and be garbage-collected; an advertised head must always
have accessible objects and its corresponding committed projection.

Retrying T when it is already the current accepted head returns success without
reapplying changes or allocating identities. If the remote has since advanced,
a retry must not rewind or reapply T. An ancestor's presence alone does not prove
it was previously a materialized publication tip; reporting that earlier result
requires retained publication-boundary evidence, otherwise report the advanced
head clearly. The exact receipt surface remains a protocol refinement question.

Donut does not silently rewrite a proposed tree. Derived indexes may be rebuilt,
but Portable content changes, including web-authored link rewrites, must be
represented in the published commit. A final tree that violates a required
invariant is rejected with an actionable error.

A non-root folder exists in the final projection only while the proposed tip
represents it with tracked content under ADR 0004. Omitting its last represented
path dissolves the Folder; Donut does not preserve invisible server-only folders.

### Use history as identity evidence without replaying it

Git does not store durable file identity. Rename detection compares snapshots
and may produce plausible but incorrect pairings. Donut must not equate Git's
reported rename or similarity score with proof that two paths own the same
learning history.

The identity model must compose supported transitions across the whole range.
For example, an unambiguous exact-content move followed by a same-path edit
retains its candidate lineage even when A and T have very different bytes.
Successive partial edits and renames can expose additional continuity evidence;
they do not remove ambiguity when files are duplicated, rewritten completely,
or deleted and recreated. The actual confidence/admission rules remain open.

Use the authoritative identity mapping at A and relevant paths, blobs, and
parent/child relationships to derive one final mapping at T. Existing
same-path and unique exact-content correspondence can supply evidence under
the supported operation semantics, but are not a permanent completeness claim.
Path equality does not preserve identity across a confirmed deletion gap.
Folder correspondence likewise requires a domain conclusion, not simply
accepting a set of Git file-rename labels.

An unresolved identity conflict must preserve both sides and block acceptance
rather than silently transfer learning history or classify a possible move as
destructive delete/add. A future owner-assisted resolution may supply explicit
intent; its authority, binding to the proposed range, and transport would need
a deliberate protocol decision. V1 does not require rewriting commits or
introduce Donut IDs, a manifest, commit trailers, or filesystem watchers as an
implicit solution.

When identity is resolved as a move, update the original entity and retain its
identity-bound data. Moves into or out of trash follow
[ADR 0004 — Trash](./0004-okf-compatible-notebook-markdown-accepted.md#trash).
When final resolution requires permanent deletion, remove the entity and
its dependent data under the existing deletion semantics. A confirmed committed
deletion ends the identity; recreation starts a new identity, even at the same
path with identical bytes and within one publication. Subsequent moves retain
the new identity. Resolve possible moves before declaring deletion; an invalid
intermediate notebook alone does not end identity. Publication must make
replacement and dependent-data removal explicit even when endpoint trees match.

**Recovery limitation:** `git revert` can restore Portable content, but cannot
recover deleted Donut identities or their dependent data, including learning
history—even when deletion and revert are published together.

### Represent every accepted web edit in Git

Every web editing batch that changes the accepted Portable tree is represented
by exactly one Git commit. In the v1 dedicated repository, Donut authors a
commit directly on `main`. For a future project-repository binding, the
repository policy decides whether Donut may author a direct commit or must open
a pull request; either form is against the whole project repository, and the
notebook projection changes only when the commit reaches the accepted ref.

Each accepted web save appends its commit; Donut never amends or coalesces
already accepted or advertised commits. Web acceptance uses the same expected
head and final-projection boundary as local publication. Competing updates
cannot both replace the same head; the stale attempt must report the conflict
without overwriting accepted work. Autosave draft buffering is not introduced
by this decision.

### Retain Git history without adding a history UI in v1

The remote retains objects reachable from accepted `main` because local
acquisition, synchronization, identity analysis, and audit depend on
that history.
V1 does not need a Donut UI for checking out an old revision, a remote-history
browser, or a Donut revert endpoint. A user may inspect or check out history in
a local Git client. A future Donut revert operation must create a new forward
commit; it must not move accepted `main` backward.

Because reachable Git object IDs are part of the synchronization contract,
Donut cannot compact accepted history by replacing it with equivalent
snapshots. Retention, quota, backup, erasure, and garbage-collection policies
remain operational decisions, but accepted reachable history is retained.

## Unresolved policy

- **Identity admission:** Define which history evidence permits automatic
  identity preservation beyond supported exact correspondence, and which cases
  require refusal or explicit owner intent. Specify deterministic settings and
  a safe outcome when analysis exceeds its budget. Preserve composition of
  supported operations.
- **Atomic failure and receipts:** Define any remaining durable-object and
  multi-head receipt guarantees. A retained ancestor alone is not proof of its
  previous application to Donut.
- **Historical representability:** Resolve whether validating only publication
  tips satisfies ADR 0004's durable-write rule when retained Git ancestors are
  not representable. This draft does not amend ADR 0004.

## Prerequisites / Assumptions

- ADR 0004's codec can render, parse, and validate a Portable notebook tree
  losslessly.
- Every accepted remote mutation of Portable content, including a web edit,
  goes through the Git commit acceptance boundary.
- Stable Note and Folder identities remain server-private.
- The Git server can withhold a proposed ref update until validation and MySQL
  projection succeed.
- Avoiding silent identity corruption is more important than accepting every
  possible combined commit shape.

## Related

- Links:
  - ADR-0000 [Use Architectural Decision Records](./0000-use-adrs-accepted.md)
  - [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md)
    (**Portable notebook tree**, **Portable path**)
  - [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  - [ADR playbook](./README.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Git objects and trees](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)
  - [Git diff](https://git-scm.com/docs/git-diff)
  - [Git log](https://git-scm.com/docs/git-log)
  - [Git pack protocol](https://git-scm.com/docs/gitprotocol-pack.html)
  - [Git rename detection](https://git-scm.com/docs/gitdiffcore)
  - [Git sparse index and sparse checkout](https://git-scm.com/docs/sparse-index)
