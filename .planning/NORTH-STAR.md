# Short-term architectural direction

## One complete accepted web change

For a synchronized Git-backed notebook, a successful web change must leave the
database and accepted Portable tree describing the same result. Note editing,
ordinary note and same-notebook folder movement, trash, and recovery share the
accepted-change boundary:
lock and load current state, apply the complete domain operation, read its
final projection, and append one accepted commit in the same transaction.
Trash is a move into a location, not Git deletion or a separate synchronization
protocol. Destination construction, collision resolution, placement, and
reference handling finish before the final snapshot. Newly created folders and
all affected in-notebook authored content must be included. Do not commit each
low-level placement or folder creation separately.

Keep placement, folder construction, reference choices, and Portable encoding
with their existing owners. Different domain recipes use one consistency owner;
controllers must not independently reproduce Git history coordination. Existing
web content/title edits, ordinary note moves, and same-notebook folder moves
stay on that owner as trash and its existing recovery routes join it. Preserve
non-Git behavior and the existing policy for pre-existing projection drift;
this direction does not authorize silently adopting unsynchronized work. No new
trash state, identity map, event journal, endpoint-specific snapshot algorithm,
or story-shaped dispatch modes.

Evidence and application: note editing, ordinary note and same-notebook folder
movement, note trash, same-notebook folder Trash, and same-notebook immediate
Undo append one accepted commit after the complete mutation. The snapshot is
built from the current persisted tree, so newly constructed parents are
included. Pre-existing projection drift remains unsynchronized. Local Git
moves of notes and unchanged folder subtrees into or out of `_trash` are
ordinary moves; publication that resolves them keeps the same identities and
retained learning, and constructs destination parents through ordinary folder
ownership. Recovery uses existing location and availability rules. It follows
[Accepted ADR 0004](../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
and keeps publication's final-result direction below intact.

## One final publication result

Accumulate supported Git changes into one final correspondence between Portable
paths/content and Donut identities, then apply that result once in the existing
publication transaction. History inspection carries tentative correspondence;
it does not replay live application mutations. One-commit and multi-commit
publication share the same final application path. Keep content persistence,
permanent deletion, folder placement, and Portable projection with their
existing domain owners. No event log, persisted shadow notebook, second
identity service, or dispatch mode per story.

Evidence: accumulated linear ranges already publish by composing supported tip
correspondence once, including in-place existing notebook and folder Readme
edits applied through the existing container content owners. Trash-boundary
note and exact-subtree folder moves use that same final-only path, including
constructed destination parents. Remaining work still needs this shared
final-only path for broader identity inference. Exact endpoint matching alone is not enough for rename-then-edit
continuity when correspondence must be inferred beyond already supported
transitions. This topic follows the owner's final-only application direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md#apply-one-final-projection-atomically)
and preserves [Accepted ADR 0004](../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
Confirmed deletion/recreation retains its existing identity semantics. The
selected ordinary-note rename admission direction is below; the proposed ADR
itself remains unapproved.


## Git rename correspondence

Owner direction, 2026-09-16: use the existing Eclipse JGit library for ordinary
note rename detection with an explicit 50% similarity threshold, following
Git's established convention and accepting its limits. Do not maintain a
parallel hand-written similarity algorithm or add semantic identity inference.
JGit's published default is 60%; configure the selected policy centrally.
Identical results to native Git for every ambiguous input are not promised.
Keep the existing ambiguous exact-pair and unresolved removal/addition refusal
safeguards; broaden exact-only detection without silently replacing those
preservation rules with destructive delete/add. A detector limit or inability
to resolve correspondence is not evidence of intentional deletion.

Use one correspondence owner for pairwise detection and the existing linear
history composition. Follow parent/child evidence to carry original note
identities into the final tree, even when accepted-to-tip similarity is low.
Endpoint matching must not override known deletion gaps or a carried lineage.
Reuse this detection responsibility across affected Git publication callers;
folder identity and its existing exact-subtree semantics remain distinct domain
responsibilities, not an excuse for duplicated ordinary-note detectors. No
per-story modes, persisted identity journal, or new client-side rename protocol.

Feed resolved moves into existing final publication/application owners. Preserve
current reference handling and authored Portable bytes; preserve learning state
regardless of semantic changes. Known web operations already know the affected
note ID and do not need heuristic inference. Cohesion means shared domain owners
and consistent existing outcomes, not replacing explicit identity with inference.

Evidence: backend JGit dependency, current exact-move history composition, and
existing publication/reference controller tests. This direction governs shared
ordinary-note inference for later Git work. Folder identity keeps existing
exact-subtree semantics. It preserves Accepted ADRs 0004 and 0005 and clarifies
the history-inspection/final-application distinction in Proposed ADR 0002
without changing its status.
