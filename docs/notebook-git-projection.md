# Notebook Git projection and identity contract

This contract details [ADR 0002](./adrs/0002-git-native-portable-notebook-synchronization-accepted.md) and shares its Accepted status. Repository, history and publication guarantees live in the [synchronization contract](./notebook-git-synchronization.md).

## Domain operation ownership

One accepted-change owner coordinates complete web domain operations. Lock and
load current state, apply the complete operation, capture the projection rows
it inserted, updated or deleted, derive the new tree from the accepted head's
tree and those rows, then append one accepted commit in the publication
transaction when the root tree differs. Derivation opens the accepted root and
the ancestor directories of the paths that captured change affects, rewrites
those directories from the changed leaf toward the root, and reuses untouched
child-tree object IDs. It does not load unrelated subtrees, note bodies, or
attachment bytes in order to publish history. Notebook creation, history reset,
drift checks, and bundle download may read a complete tree. An unchanged root
tree ID does not append a commit or insert new Git objects. Destination
construction, collision resolution, placement, and authored reference handling
finish before that capture closes; newly created folders and all affected
in-notebook content are captured with it. Do not publish individual low-level
placement steps.

Accepted repository persistence receives the commit ID already produced or validated
by its caller; a web save does not reread the accepted ref after appending.
The shared store sets the binding head and update time. A new binding is persisted
before its objects are copied, and the final binding save remains in the publication
transaction so a late persistence failure rolls back the publication.

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
movement, same-notebook folder creation, rename, move and dissolve, note and
folder moves to another notebook over {source, destination}, and relationship
reduction over its touched-notebook set. Note rename, move and trash, and
folder move and dissolve, also lock the editable notebooks whose links they
rewrite. A web action changes linking notes only in notebooks the acting user
owns (directly or through a circle); a linking note in a notebook the user only
subscribes to keeps its old text and gets no commit. Folder rename rewrites no
notebook-qualified link, so it changes no other notebook. After the operation flushes, the owner refuses (409, retry) a change
that touched a bound notebook it did not lock, so a caller that leaves a
notebook out of its set fails loudly instead of leaving that notebook out of
step with its Git. Changes to unbound notebooks outside the set are not
checked.

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
