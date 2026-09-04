# 0002 — Synchronize Portable notebook trees with revisioned three-way merge

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

These needs imply a common ancestor for comparison, stable private server
identity, a three-way merge, and an atomic remote acceptance boundary. They do
not imply Git, a distributed commit graph, branches, or a Git server.

There is also an information limit. After an ID-free file disappears at one
path and another appears at a new path, final tree contents do not always prove
whether the user renamed, copied, or deleted and recreated a note. A rename
combined with a substantial rewrite can be indistinguishable from delete plus
create. No similarity heuristic can recover intent that is absent from the
tree. Ambiguous identity must therefore be resolved explicitly rather than
guessed.

## Decision

### Keep one accepted remote state and a linear content revision

MySQL remains authoritative for the current accepted notebook, including both
portable content and Donut-only data. Every successful durable mutation that
changes the Portable notebook tree—whether it originates on the web or from a
local synchronization—creates a monotonically ordered **notebook content
revision** in the same transaction as the current note/folder changes.

Each content revision records:

- its parent revision;
- a digest of the exact canonical Portable notebook tree;
- enough immutable state to reconstruct that tree as a merge base;
- the private note/folder identity correspondence for that revision; and
- the accepted logical changes and actor metadata needed for diagnosis.

Full snapshots, deltas with checkpoints, or content-addressed blobs (including
reuse of Git-compatible plumbing) are possible storage encodings. They are
implementation details, not the client protocol. The server's accepted history
is linear because the server already serializes accepted notebook state.
Divergent local trees are proposals based on an earlier revision; they are not
additional authoritative branches.

The server may compact old content bodies only if it retains the revision
digest and private identity correspondence. A client may then supply its saved
base material, which the server verifies against that digest. If neither side
can reproduce B, the operation is recovery/import with explicit identity
review, not an ordinary synchronization that is allowed to guess.

Derived data such as embeddings and search indexes may update asynchronously.
Identity-bearing rows and the canonical Portable tree change atomically.

### Keep synchronization metadata outside the Portable notebook tree

The synchronization client keeps a private **sync envelope** in Donut's client
configuration, outside the notebook directory. It contains at least:

- the remote notebook and server identity;
- the last synchronized content revision and tree digest;
- opaque entry keys mapped to the paths and content digests seen at that
  revision; and
- any verified base material needed for three-way merge or recovery.

No note ID, folder ID, UUID, opaque entry key, or sync manifest is written into
the Portable notebook tree. The tree remains a normal ADR-0004 bundle that
other Markdown and OKF tools can use.

The sync envelope is an aid, not hidden authority. Losing it must not corrupt a
notebook: the client can obtain a fresh envelope for a clean tree, while a
locally changed tree may require explicit identity choices that the lost
metadata would otherwise have supplied.

### Synchronize from base, local, and current remote states

For every synchronization, define:

- **B** — the last content revision synchronized by this local copy;
- **L** — the current local Portable notebook tree; and
- **R** — the current accepted remote Portable notebook tree.

The synchronization service compares both `B → L` and `B → R`. It first
resolves changes into logical note, folder, and Readme operations, then performs
a three-way merge. This works when R contains one change or many accumulated
revisions since B; replaying every remote revision on the local filesystem is
not required.

The initial transport may send complete canonical trees. Later transport may
send hashes and deltas without changing the revision, identity, merge, or
acceptance contracts.

### Resolve identity before merging content and topology

Identity resolution is conservative and produces one of three outcomes:

1. **Known identity** — the unchanged path and sync envelope identify the
   existing entity, or an explicit move/resolution recorded the new
   correspondence.
2. **Safe inferred identity** — evidence forms one unique correspondence, such
   as a one-to-one exact-content rename or a folder move established by the
   identities of its descendants. The preview reports this inference and the
   merge may accept it automatically unless the user overrides it.
3. **Ambiguous identity** — multiple rename/copy/delete-create explanations
   remain plausible. The service returns candidates and requires the user to
   choose rename/move, copy/create, delete-and-create, or restore.

Similarity scores, filesystem watcher events, inode/file IDs, and editor or
CLI move commands may strengthen evidence or provide an explicit assertion.
Similarity alone is a suggestion, not proof. The service must never attach
learning history to a different note merely because it was the closest match.
An item deleted and recreated at the same path is likewise indistinguishable
from an edit after only the final tree is observed; preserving the existing
identity is the default, and deliberate new identity at the same path requires
an explicit user choice.

Once identity is resolved, a rename or move updates the existing Note or
Folder row rather than replacing it. Non-portable identity-bound data remains
attached. Content-derived indexes are rebuilt as needed. Server domain behavior
caused by the move—such as Portable-path wiki-link rewrites—is included in the
accepted result returned to the local copy.

### Merge by logical entity and report structured conflicts

The merge operates on stable server entities after identity resolution, not
only on path strings.

| Local and remote changes since B | Result |
|---|---|
| Changes to different entities or independent fields | Combine |
| Rename/move on one side and content edit on the other | Combine on the same identity |
| Same resulting change on both sides | Coalesce |
| Non-overlapping text changes to one Markdown file | Three-way text merge |
| Different overlapping text changes | Content conflict |
| Different destinations for the same entity | Topology conflict |
| Delete on one side and edit/move on the other | Delete/modify conflict |
| Concurrent identical add at one path | Coalesce to the accepted remote identity |
| Concurrent different adds or any destination collision | Path conflict |
| Uncertain delete/add/rename/copy correspondence | Identity conflict |
| Folder move and descendant edits | Combine after subtree identity is established |

Automatic merge must preserve authored Markdown bytes outside merged hunks and
must pass the ADR-0004 codec and validation rules. A later Markdown- or
frontmatter-aware merge may reduce false conflicts, but must not silently pick
one meaning for genuinely overlapping changes.

Conflicts are returned as structured data containing base, local, and remote
versions plus the affected identity and paths. The client offers a merge tool
or explicit choices and resubmits the resolved proposal. An unresolved preview
does not mutate remote state or overwrite the local tree.

### Accept a merged result atomically and return the canonical result

Acceptance uses optimistic concurrency, not a long-held notebook lock:

1. Authenticate and authorize the notebook operation.
2. Require the remote head to equal the revision used to produce the merge.
3. Validate the proposed tree and all identity resolutions.
4. Apply creates, edits, renames/moves, and deletes through Donut domain
   operations so uniqueness, soft-deletion, wiki-link rewrite, and other
   business rules still run.
5. Atomically update current rows, preserve identity lineage, and append the
   new content revision—or accept nothing.
6. Render the resulting canonical Portable notebook tree, record its digest,
   and return the exact resulting patch/tree and a new sync envelope.

If the remote head changes before step 5, the server rejects the stale attempt
and recomputes against the new R. The local client applies an accepted remote
result through a temporary tree/backup strategy and verifies the resulting
digest before advancing its local sync envelope. Local unmerged work is never
silently overwritten.

### Treat Git as an optional local concern

A user may initialize Git in or around the local Portable notebook tree for
personal history, branching, or collaboration. Donut's synchronization
contract does not require Git, expose Git object IDs as content revisions,
interpret client refs, or expose a Git remote. The server may reuse Git storage
or merge plumbing behind this boundary when it is the simplest implementation;
that plumbing is not the authority, identity model, or wire contract.

A future Git adapter may translate a selected Git tree into the same
synchronization protocol. It must not create a second authority or bypass
identity resolution and transactional acceptance.

### Keep delivery sequencing out of this architectural decision

The six synchronization capabilities in Context are one required product
contract, not “Level 1” versus optional “Level 2” architecture. Safe delivery
may proceed through revisioned checkout/pull, non-divergent publish,
identity-preserving rename/move, and finally divergent three-way merge. Donut
must not describe an intermediate stage as complete two-way synchronization.
Detailed stop-safe slices belong in `.planning/`, not in this ADR.

Transport optimization, background file watching, history compaction, a Git
adapter, and additional binary content are separable later capabilities. They
do not alter the authority, baseline, identity, merge, or acceptance decisions.

## Consequences

- Donut owns a domain-aware synchronization and conflict-resolution protocol.
- A single base revision is sufficient for accumulated divergence; a Git DAG
  and server-side Git implementation are not required.
- Remote web edits remain ordinary database/domain transactions, augmented by
  a content revision rather than translated into commits.
- Rename/move can preserve note identity and its learning data without putting
  identity into portable Markdown.
- Some rename/copy/delete-create cases necessarily stop for human intent.
- Accepted revision history and private identity correspondence need retention,
  compaction, erasure, and quota policies.
- Sync is safe under concurrent web or client edits because acceptance is a
  compare-and-set on the current revision.

## Pros

- Implements the actual synchronization needs with fewer unrelated hosting
  capabilities.
- Keeps one operational authority and one database transaction boundary.
- Leaves the Portable notebook tree clean and usable by ordinary tools.
- Gives conflicts domain meaning, especially for topology and identity.
- Allows Git locally without coupling Donut's web editing model to Git.

## Cons

- Donut must build and test the three-way merge, identity resolution, sync
  envelope, and conflict UX instead of inheriting a Git command-line workflow.
- The server must retain or reconstruct merge-base state.
- External sync state can be lost and then requires recovery or more user
  decisions.
- There is no standard Git clone/fetch/push endpoint as part of this decision.

## Prerequisites / Assumptions

- ADR 0004's codec can render, parse, and validate a Portable notebook tree
  losslessly.
- Every durable remote mutation of portable content goes through the content
  revision boundary.
- Stable Note and Folder identities remain server-private.
- The client can store private state under its Donut configuration directory.
- Conflict resolution favors avoiding silent identity corruption over reducing
  prompts.

## Alternatives considered

### Git as the synchronization authority and server store

Git supplies snapshots, a commit graph, three-way text merge, and mature local
tools. It does not remove Donut's hardest requirement: Git detects renames and
copies by comparing deleted and added content for sufficient similarity; a
rename is not durable file identity. Donut would still need identity
resolution, conflict UX, domain validation, and an atomic MySQL projection.
Making Git objects, refs, pushes, branches, and web-originated commits the
product contract adds capability that the stated needs do not require. Not
selected as the core; reuse of isolated Git plumbing remains allowed.

### Path- and modified-time mirroring

Without a trusted base state, two changed replicas cannot distinguish which
side changed or perform a proper three-way merge. Modified times are transport
metadata, not logical versions. Not selected.

### Generic bidirectional file synchronization

A generic synchronizer can propagate non-conflicting file updates and report
file conflicts, but it does not know Donut Note identities, attached learning
data, domain moves, soft deletion, or transactional acceptance. Useful prior
art, but insufficient as the product boundary.

### CRDT-backed tree and documents

CRDTs are attractive when all replicas exchange identity-bearing operations
and must converge without coordination. Ordinary Markdown editors leave only
file snapshots, not CRDT operations, and the Donut server is already the
coordination point. A CRDT would add hidden identifiers and operation history
while identity ambiguity at the file import boundary would remain. Not
selected for Portable notebook tree synchronization.

## Related

- Links:
  - ADR-0000 [Use Architectural Decision Records](./0000-use-adrs-accepted.md)
  - [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md)
    (**Portable notebook tree**, **Portable path**)
  - [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
  - [ADR playbook](./README.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Git rename detection](https://git-scm.com/docs/gitdiffcore)
  - [Git merge bases](https://git-scm.com/docs/git-merge-base)
  - [Unison synchronization model](https://github.com/bcpierce00/unison/blob/master/doc/unison-manual.tex)
