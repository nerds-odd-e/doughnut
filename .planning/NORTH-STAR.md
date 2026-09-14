# Short-term architectural direction

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
correspondence once; remaining work still needs this shared final-only path for
broader identity inference, existing-Readme editing, and trash-compatible
publication. Exact endpoint matching alone is not enough for rename-then-edit
continuity when correspondence must be inferred beyond already supported
transitions. This topic follows the owner's final-only application direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md#apply-one-final-projection-atomically)
and preserves [Accepted ADR 0004](../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
Confirmed deletion/recreation identity is settled in that ADR draft; broader
admission rules and the proposed ADR itself remain open.
