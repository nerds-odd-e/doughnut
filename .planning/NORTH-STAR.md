# Short-term architectural direction

## One final publication result

Accumulate supported Git changes into one final correspondence between Portable
paths/content and Donut identities, then apply that result once in the existing
publication transaction. History inspection carries tentative correspondence;
it does not replay live application mutations. Evolve the current publication
owners so one-commit and multi-commit publication share the same rules and
completion path. Keep content persistence, permanent deletion, folder placement,
and Portable projection with their existing domain owners. No event log,
persisted shadow notebook, second identity service, or dispatch mode per story.

Evidence: the publisher currently chooses mutually exclusive addition, folder
relocation, and note-change paths; exact endpoint matching loses rename-then-edit
continuity. This direction supports accumulated publication and later identity
inference, Readme, and trash compatibility work without implementing those later
promises now. It follows the owner's final-only application direction in
[Proposed ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md#apply-one-final-projection-atomically)
and preserves [Accepted ADR 0004](../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
Domain identity policies remain in the story/ADR; this topic does not settle
ambiguous deletion/recreation or approve the proposed ADR.
