# 0002 — Git-native Portable notebook tree synchronization

**Status:** Proposed

**Date:** 2026-09-04

**Draft revised:** 2026-09-14 — final-revision projection and history-informed identity; pending human review

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
the commit graph is the retained content history; and Git object and ref
exchange is the transport model. V1 integration advances one linear mainline
without rewriting history. V1 may use the Donut CLI to mediate repository acquisition and
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
and any future integration policy then apply to the whole project repository. Donut
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

The supported Donut workflow uses this one mainline, with no branching,
merging, rebasing, squashing, or amendment of local or accepted commits to make
publication possible. Git itself permits local branches; that does not make
publication of a divergent branch supported. If both sides independently
advance from a common ancestor, v1 fails clearly and preserves both histories.
A future reconciliation policy requires a separate decision: original commit
IDs, a single linear history, and arbitrary divergent integration cannot all
be guaranteed together.

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

### Use history as identity evidence without replaying it

Git does not store durable file identity. Rename detection compares snapshots
and may produce plausible but incorrect pairings. Donut must not equate Git's
reported rename or similarity score with proof that two paths own the same
learning history. The [research and controlled examples](../notebook-git-identity-research.md)
show both the value and limits of inspecting intermediate commits.

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
A persistent path alone does not settle deletion and later recreation at that
path. Folder correspondence likewise requires a domain conclusion, not simply
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
its dependent data under the existing deletion semantics; recreating a file
later does not recover learning data from Git. The meaning of a deletion gap
wholly inside an unpublished range remains an explicit open decision below.

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

### Treat v1 restrictions as scope, not synchronization levels

The six requirements in Context form one synchronization contract. V1 targets
two-way synchronization for its supported dedicated-repository,
linear-`main` binding, subject to unresolved identity policy and the explicit
exclusion of divergent integration. It is not a claim that all Git histories
or authoring operations are already implemented. Remote branches, historical checkout UI, arbitrary
project-subdirectory bindings, and pull-request integration are later scope
expansions on the same contract.

Detailed implementation slices and their delivery order belong in
`.planning/`, not in this ADR.

The accumulated-publication increment delivers free composition of already
supported edits: multiple edits within each commit and multiple such commits
in one publication. It does not promise implementation of this entire ADR,
general support for every nonrepresentable intermediate draft, or new identity
inference and owner-assisted resolution capabilities. Necessary history analysis
for composing supported transitions remains part of that increment. These
delivery boundaries do not change the final-only projection direction or
introduce a requirement to validate every historical tree as a Donut notebook.

## Open decisions for this draft

The owner proposed preserving every original commit while projecting only the
final revision. The following recommendations and remaining questions need
human review before this ADR can be accepted; this draft does not authorize
implementation.

- **Identity admission:** Which history evidence permits automatic preservation,
  and which cases require refusal or explicit owner intent? Similarity may help
  find candidates but is not a correctness guarantee. Specify deterministic
  settings and a safe outcome when analysis cannot finish within its budget.
- **Deletion gaps and path reuse:** Within one unpublished range, does a removal
  followed by reappearance mean replacement, undo, or unresolved intent? A
  default must not silently destroy learning data. Separately published
  permanent deletion has already removed that data; batching cannot undo it.
- **Operation composition:** Any combination of already supported operations is
  the accumulated-publication goal. History analysis needed to compose an exact
  move followed by an edit belongs to that goal. Broader same-transition
  rename/edit inference remains separate product work; ambiguity policies must
  prevent the two scopes from contradicting each other.
- **Atomic failure and receipts:** Confirm all-or-nothing acceptance including
  required indexes and durable objects, and the response when a successful
  publication is retried after later heads have been accepted. Do not confuse
  a retained ancestor with proof of its previous application to Donut.
- **Historical representability:** ADR 0004 remains the final Portable-tree
  contract. This draft scopes application validation to publication tips while
  allowing nonrepresentable Git ancestors. Confirm that interpretation of its
  durable-write rule; this draft does not amend the Accepted format ADR.

## Consequences

- Git is a required product boundary for two-way Portable notebook
  synchronization, not merely an optional storage implementation.
- A local repository is managed with standard Git. V1 synchronization may be
  mediated by the Donut CLI without adding Donut metadata to the Portable tree;
  direct standard-Git remote access can be added later over the same history.
- Accepted portable content has one authority: the accepted Git tree. MySQL
  supplies the transactional current projection and private Donut identity.
- Supported synchronization preserves local and remote commit IDs. Divergence
  is reported without rewriting either history; automatic recovery is deferred.
- Remote web editing necessarily produces commits, including in a future
  project repository that binds only one subdirectory.
- Donut still owns conservative path-to-entity identity projection because Git
  does not record renames as durable identity.
- Identity analysis may use intermediate commits without materializing them.
  Some rename/edit or copy/delete cases remain ambiguous; the product must
  resolve or refuse them without requiring history rewriting.
- V1 stores reachable original Git history even when intermediate trees are
  not valid Donut notebooks. Only publication tips are projected; historical
  content retention and historical application representability are distinct.
- The v1 dedicated-repository restriction can later be relaxed to a subtree
  binding without changing Portable paths, Git revisions, or the projection
  boundary.

## Pros

- Uses a mature standard for snapshots, history, common ancestors, transport,
  diff inspection, integrity, and local tooling.
- Removes the proposed custom revision service, sync envelope, local database,
  and custom merge protocol. A v1 CLI transport remains thin over Git objects,
  refs, and fast-forward updates.
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
  the notebook directory—the unit of permissions and commit history; any future
  integration and pull request policy must respect that boundary.
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

### Replay every intermediate revision into the application

This would require each draft to be a valid Donut state and would run temporary
entity creation, deletion, and indexing. Keeping those writes in one transaction
would hide intermediate states from readers but would not remove their
validation requirements, cost, or destructive identity decisions. Inspect the
history for evidence and apply only the resolved final state instead.

### Infer identity solely from the accepted and proposed tip trees

This is simpler but loses supported rename-then-edit continuity and can mistake
deletion followed by recreation for a rename. It also misses identity-sensitive
histories with unchanged endpoint trees. Use endpoint comparison for the final
content result together with relevant range evidence for identity; do not
equate a content diff with the complete domain change.

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
  - [Identity research](../notebook-git-identity-research.md)
  - [Git diff](https://git-scm.com/docs/git-diff)
  - [Git log](https://git-scm.com/docs/git-log)
  - [Git pack protocol](https://git-scm.com/docs/gitprotocol-pack.html)
  - [Git rename detection](https://git-scm.com/docs/gitdiffcore)
  - [Git sparse index and sparse checkout](https://git-scm.com/docs/sparse-index)
