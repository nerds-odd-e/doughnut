# Retire linked-worktree disposable databases

Inspect and reclaim disposable unit-test and allocated E2E databases for one
linked Git worktree before you remove that checkout. Identity stays in
`.worktree.local.json`; this guide covers target inspection and (as retirement
grows) reclaim behavior.

Related isolation guides: [`docs/worktree-backend-tests.md`](worktree-backend-tests.md),
[`docs/worktree-browser-tests.md`](worktree-browser-tests.md).

## Inspect disposable targets before removing a linked checkout

From a Git linked worktree that already has `.worktree.local.json`, inspect the
exact disposable database names this checkout owns:

```bash
CURSOR_DEV=true nix develop -c pnpm worktree:retire --check
```

`--check` prints the unit target `doughnut_<id>_test` and, when recorded, the
canonical E2E target `doughnut_e2e_<id>`. It validates the linked-worktree
relationship and recorded identity, then reports busy or uncertain evidence from
existing ownership locks (`.worktree.local.lock`, `.sut.local.lock`, startup
ownership, Cypress leases), recorded application listeners, MySQL sessions
against those targets, and surviving supported backend JVMs tied to the checkout
by working directory or command line. Stale or unverifiable ownership records
refuse without using the runners' permissive stale-lock reclamation paths.
Listeners, sessions, and checkout-process evidence are vetoes only — never
ownership authorization. A clear result is an **idle snapshot**, not a deletion
reservation or permission to drop. Mutation mode (`pnpm worktree:retire` without
`--check`) is refused until retirement is enabled. Primary checkouts,
missing/invalid identity, a duplicate identity in another registered worktree,
and non-canonical E2E database names refuse visibly.

Custom E2E names refuse rather than guessing ownership. There is no machine-wide
allocation registry and no recovery from duplicate operator-supplied IDs.
