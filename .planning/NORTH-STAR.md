# Short-term architectural direction

## One complete accepted web change

For a synchronized Git-backed notebook, a successful web change must leave the
database and accepted Portable tree describing the same result. Note editing,
ordinary movement, trash, and recovery share the accepted-change boundary:
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
web content/title edits and ordinary moves stay on that owner as trash and its
existing recovery routes join it. Preserve non-Git behavior and the existing
policy for pre-existing projection drift; this direction does not authorize
silently adopting unsynchronized work. No new trash state, identity map, event
journal, endpoint-specific snapshot algorithm, or story-shaped dispatch modes.

Evidence and application: ordinary web moves now append accepted history, while
Trash and immediate Undo still mutate only the database. The current edit
snapshot also retains pre-mutation folder rows; web folder creation already
demonstrates reading newly constructed folders for its final snapshot. The owner
identified this coherence gap on 2026-09-15. This direction governs
[story 28](seeds/SEED-009-git-backed-local-notebook-workflow.md#story-28) and later
web-organization work; the current delivery proves one note's trash/recovery
journey and preserves existing callers. It follows
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
edits applied through the existing container content owners. Remaining work
still needs this shared final-only path for broader identity inference and
trash-compatible publication. Exact endpoint matching alone is not enough for rename-then-edit
continuity when correspondence must be inferred beyond already supported
transitions. This topic follows the owner's final-only application direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md#apply-one-final-projection-atomically)
and preserves [Accepted ADR 0004](../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
Confirmed deletion/recreation identity is settled in that ADR draft; broader
admission rules and the proposed ADR itself remain open.
