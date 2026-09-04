# 0002 — Git-native Portable notebook tree synchronization

**Status:** Proposed

**Date:** 2026-09-04

**Decision makers:** Terry Yin

**Consulted:** None

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
5. Accumulated local and remote changes can be merged, with human resolution
   when automatic merge is unsafe.
6. A local note or folder rename/move updates the corresponding existing remote
   entity when that identity conclusion is sound, so non-portable data remains
   attached to the correct entity.

The local usability constraint is equally important: the working tree must not
require a `.donut` directory, a manifest, a local database, extended attributes,
or any other Donut-specific state beside the Portable notebook tree. Standard
Git manages local commits, history, rebases, and conflicts. V1 may require the
Donut CLI to acquire and synchronize the repository; direct `git clone`,
`git fetch`, and `git push` against Donut are a later capability. Ordinary Git
repository metadata and minimal binding/authentication data in Git
configuration or the normal credential store are allowed because none of that
state belongs to the Portable tree.

There is an information limit. Git commits contain snapshots, not durable file
identities or rename operations. After an ID-free file disappears at one path
and another appears at a new path, the snapshots do not always prove whether
the user renamed, copied, or deleted and recreated a note. A rename combined
with a substantial rewrite can be indistinguishable from delete plus create.
Donut must preserve identity when the Git history makes the conclusion sound
and must not guess when it does not.

The first delivery can use one dedicated repository per notebook. The
architecture must also permit a later notebook to map to one directory inside
a larger project repository. In that form Git still synchronizes and commits
the whole repository; Donut projects only the configured notebook directory.

## Decision

### Make Git the Portable tree synchronization contract

The accepted commit at the notebook binding's Git ref is authoritative for the
accepted Portable notebook tree. Git object IDs are the revision identifiers;
the commit graph is the retained content history; Git object and ref exchange
is the transport model; and Git rebase is the v1 accumulated-change integration
mechanism. V1 may use the Donut CLI to mediate repository acquisition and
synchronization. Direct standard-Git remote access is a later transport surface;
adding it must not introduce a second revision, history, or merge model.

Donut does not create a parallel notebook revision number, tree digest
protocol, sync envelope, delta format, or custom three-way merge protocol.
Where integrity requires a tree digest, the Git tree object ID supplies it.
Where synchronization requires a common ancestor, the Git commit graph
supplies it.

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
rebases, and pull requests then apply to the whole project repository. Donut
validates and projects only the bound subtree. A commit outside it advances the
repository without changing that notebook; a commit touching multiple bound
notebooks changes their projections at the same repository-commit boundary.
Moving a file across a binding boundary is a deletion from one Portable tree
and an addition to the other.

Sparse checkout may make a local working tree smaller, but it does not turn a
subdirectory into an independent Git repository. Serving an arbitrary subtree
as if it had independent commit IDs would require filtered or synthetic
history and is a separate adapter, not part of this decision.

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

Users may create any local branches they want. To publish accumulated local
work after remote changes, the supported synchronization flow obtains the
advertised remote `main`, rebases unpublished commits onto it with ordinary Git,
allows the user to resolve ordinary Git conflicts, and proposes the
fast-forward result. Only unpublished commits are rebased. Donut never rebases
or amends an already accepted and advertised commit.

This policy deliberately chooses a linear public history for v1. It closes the
merge algorithm and conflict-file questions by using standard Git behavior
without committing Donut to remote branch hosting, merge commits, or pull
request infrastructure for dedicated repositories.

### Project accepted commits into Donut atomically

MySQL remains authoritative for Donut-only data and for the current application
projection. Note/folder content rows are a projection of the accepted Git
tree, not a second independently editable authority. Donut stores the accepted
Git head and the private identity lineage needed to associate paths at accepted
commits with stable Note and Folder entities.

For a pushed linear range, Donut evaluates each commit from its accepted parent
to its child:

1. Receive immutable Git objects without advertising the proposed ref update.
2. Authenticate and authorize the repository update.
3. Require the proposed old head to equal the currently accepted head and the
   new head to be a permitted fast-forward linear range.
4. Validate every affected Portable notebook tree against ADR 0004.
5. Resolve note/folder identity changes conservatively for every commit.
6. Apply the accepted creates, edits, moves, and deletes to the MySQL
   projection and identity lineage.
7. Atomically record the new accepted head with those projection changes, then
   advertise that head—or accept none of the range.

The advertised `main` is derived from the accepted head. Uploaded objects that
fail acceptance remain unreachable and may be garbage-collected.

Donut does not silently rewrite a pushed tree. Derived indexes may be rebuilt,
but any Portable content transformation required by a Donut-authored operation,
including link rewrites performed by a web move, must already be represented
in a commit. A pushed tree that violates a required invariant is rejected with
an actionable error.

### Infer private identity from commit-to-commit changes

Identity resolution uses the ordered commits, not only the final pushed tree:

- An entry that remains at the same Portable path keeps its entity identity.
- A unique deleted/add pair with the same blob, or a unique consistent folder
  relocation with exact descendant correspondence, is an identity-preserving
  rename/move.
- A later edit commit after an accepted move keeps the moved entity identity.
- A deletion accepted in one commit followed by an addition in a later commit
  means delete then create; the new entry receives a new identity.
- Similarity may identify a possible correspondence for diagnostics, but
  similarity alone is not proof of identity.

If a single commit can plausibly mean either rename-with-edit or
delete-and-create, or if several exact candidates exist, Donut rejects it as an
identity conflict before advancing `main`. The user resolves the ambiguity
using ordinary Git history:

- split a move-and-edit into an unambiguous move-only commit followed by an edit
  commit to preserve identity;
- split deletion and later creation into separate commits to request new
  identity; or
- split multiple moves into smaller commits until each correspondence is
  unambiguous.

The user can rewrite these unpublished commits with standard Git tools and
synchronize again. No Donut-specific metadata, commit trailer, or filesystem
watcher is required. V1 may require the Donut CLI for transport, but the CLI
does not own the working tree or replace Git history. This makes commit
boundaries meaningful for private identity while keeping the Portable tree
ID-free.

Once a move is accepted, Donut updates the existing Note or Folder entity.
Identity-bound learning data stays attached. When a delete-and-create is
accepted, the old entity's non-portable data stays with the deleted entity and
is not transferred to the new one.

### Represent every accepted web edit in Git

Every web editing batch that changes the accepted Portable tree is represented
by exactly one Git commit. In the v1 dedicated repository, Donut authors a
commit directly on `main`. For a future project-repository binding, the
repository policy decides whether Donut may author a direct commit or must open
a pull request; either form is against the whole project repository, and the
notebook projection changes only when the commit reaches the accepted ref.

Rapid autosaves from one continuous editing session on the same note may be
coalesced into one commit. Before publication they are a durable server-side
edit batch, not an accepted Portable tree revision. The batch is sealed into a
commit when the continuous edit ends, when another note or structural
operation begins, or when a competing commit cuts the sequence. The batch seal
and every push compare-and-set the same accepted head. Whichever acceptance
attempt advances `main` first wins; a stale push must fetch and rebase, while a
stale web batch must be reapplied to the new head and pass validation before
Donut can commit it.

The web UI must distinguish a saved draft from a committed/synchronized edit.
Once a web commit is accepted or advertised on `main`, Donut never amends it.
Commit coalescing therefore cannot rewrite history already visible to a Git
client.

### Retain Git history without adding a history UI in v1

The remote retains objects reachable from accepted `main` because local
acquisition, synchronization, rebase, conflict resolution, and audit depend on
that history.
V1 does not need a Donut UI for checking out an old revision, a remote-history
browser, or a Donut revert endpoint. A user may inspect or check out history in
a local Git client. A future Donut revert operation must create a new forward
commit; it must not move accepted `main` backward.

Because reachable Git object IDs are part of the synchronization contract,
Donut cannot compact accepted history by replacing it with equivalent
snapshots. Retention, quota, backup, erasure, and garbage-collection policies
remain operational decisions, but accepted reachable history is retained.

### Treat v1 restrictions as scope, not synchronization levels

The six requirements in Context form one synchronization contract. V1 is
complete two-way synchronization for its supported dedicated-repository,
linear-`main` binding; it is not a “Level 1” substitute for a separately
defined “Level 2” protocol. Remote branches, historical checkout UI, arbitrary
project-subdirectory bindings, and pull-request integration are later scope
expansions on the same contract.

Detailed implementation slices and their delivery order belong in
`.planning/`, not in this ADR.

## Consequences

- Git is a required product boundary for two-way Portable notebook
  synchronization, not merely an optional storage implementation.
- A local repository is managed with standard Git. V1 synchronization may be
  mediated by the Donut CLI without adding Donut metadata to the Portable tree;
  direct standard-Git remote access can be added later over the same history.
- Accepted portable content has one authority: the accepted Git tree. MySQL
  supplies the transactional current projection and private Donut identity.
- Local conflicts are resolved with rebase and ordinary Git tools. The remote
  never rewrites published history.
- Remote web editing necessarily produces commits, including in a future
  project repository that binds only one subdirectory.
- Donut still owns conservative path-to-entity identity projection because Git
  does not record renames as durable identity.
- Some combined rename/edit or copy/delete cases must be split into clearer
  unpublished commits before Donut will accept them.
- V1 stores reachable Git history even though the Donut application has no
  historical checkout feature.
- The v1 dedicated-repository restriction can later be relaxed to a subtree
  binding without changing Portable paths, Git revisions, or the projection
  boundary.

## Pros

- Uses a mature standard for snapshots, history, common ancestors, transport,
  rebasing, conflict presentation, integrity, and local tooling.
- Removes the proposed custom revision service, sync envelope, local database,
  and custom merge protocol. A v1 CLI transport remains thin over Git objects,
  refs, and rebase.
- Keeps the local Portable notebook tree clean and usable by ordinary Markdown
  and OKF tools.
- Gives users and developers one revision and conflict model to understand.
- Makes history available from the first version without requiring Donut to
  build a history UI.
- Preserves a path from dedicated notebook repositories to notebook
  directories inside project repositories.

## Cons

- Donut must persist Git repositories, provide the v1 CLI transport, and make
  accepted ref updates consistent with the MySQL projection. A standard Git
  remote remains later scope.
- Git's snapshot model does not eliminate Donut's private identity ambiguity.
- The linear v1 policy excludes remote branches, tags, merge commits, and force
  pushes.
- A project-subdirectory binding makes the whole project repository—not only
  the notebook directory—the unit of permissions, commit history, rebase, and
  pull request review.
- Reachable accepted history consumes storage and cannot be compacted without
  changing commit IDs.

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

## Alternatives considered

### A bespoke revision and synchronization protocol

A linear notebook revision, local sync envelope, custom tree transport, and
three-way merge could satisfy the functional requirements. It would duplicate
Git concepts and require Donut-specific local state or make a Donut client own
the working tree and merge model. A thin v1 CLI that transports Git objects and
refs does neither. The bespoke protocol is not selected.

### Make one notebook equal one repository permanently

This is the simplest v1 deployment, but encoding it as the permanent domain
model would obstruct a notebook directory inside an existing project
repository. V1 uses one repository per notebook while the architecture binds a
notebook to a Git tree. Permanent equality is not selected.

### Serve every notebook subtree as an independent repository

Filtering a project repository to a subdirectory can produce a separate
history, but it changes commit identity and introduces bidirectional history
translation. That may be useful as a future adapter, but it is not equivalent
to binding Donut directly to the project's own commits. Not selected as the
core model.

### Path- and modified-time mirroring

Without a trusted common ancestor, two changed replicas cannot distinguish
which side changed or perform a safe merge. Modified times are transport
metadata, not logical revisions. Not selected.

### Generic bidirectional file synchronization

A generic synchronizer can propagate files and report file conflicts, but it
does not provide the shared commit history, server acceptance policy, or
private Donut identity projection required here. Not selected.

### CRDT-backed tree and documents

Ordinary Markdown editors and Git clients do not emit the identity-bearing
operations a CRDT would require. A CRDT would add another history and local
metadata model while the Git import boundary would retain the same identity
ambiguity. Not selected.

## Related

- Links:
  - ADR-0000 [Use Architectural Decision Records](./0000-use-adrs-accepted.md)
  - [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md)
    (**Portable notebook tree**, **Portable path**)
  - [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  - [ADR playbook](./README.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Git objects and trees](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)
  - [Git rebase](https://git-scm.com/docs/git-rebase.html)
  - [Git pack protocol](https://git-scm.com/docs/gitprotocol-pack.html)
  - [Git rename detection](https://git-scm.com/docs/gitdiffcore)
  - [Git sparse index and sparse checkout](https://git-scm.com/docs/sparse-index)
