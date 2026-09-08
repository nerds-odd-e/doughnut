# Retire linked-worktree disposable databases

Inspect and reclaim disposable unit-test and allocated E2E databases for one
linked Git worktree before you remove that checkout. Identity stays in
`.worktree.local.json`; this guide covers target inspection and unit-test
reclaim. Allocated E2E reclaim is not enabled yet.

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
reservation. Primary checkouts, missing/invalid identity, a duplicate identity
in another registered worktree, and non-canonical E2E database names refuse
visibly.

Custom E2E names refuse rather than guessing ownership. There is no machine-wide
allocation registry and no recovery from duplicate operator-supplied IDs.

## Retire an idle unit-test-only allocation

When `--check` reports an idle snapshot and the checkout has **no** recorded E2E
database, reclaim the unit target before you remove the checkout:

```bash
CURSOR_DEV=true nix develop -c pnpm worktree:retire
```

Run this from the linked checkout being retired, through local Nix. The command:

1. Takes a short checkout-local admission gate that serializes with backend test
   / SUT start.
2. Re-validates identity, targets, and idle evidence under that gate.
3. Writes a durable `.worktree.retire.marker` **before** the first `DROP`.
4. Drops only `doughnut_<id>_test` (missing schemas count as already absent;
   nothing is provisioned).
5. Releases only the gate this invocation acquired on controlled exit.
6. Leaves `.worktree.local.json`, the checkout, processes, and port claims
   untouched.

Recorded E2E allocations refuse before any marker or DROP; reclaim both unit and
E2E in a later command version.

### Failure and retry

MySQL `DROP DATABASE` is not an atomic multi-target operation. If a DROP fails
after the marker is written, the command reports partial progress, retains the
marker and exact targets, and exits non-zero. Supported backend/SUT starts refuse
the marker instead of recreating databases — including after an interrupted or
failed drop. Retry the same command against the same identity to complete missing
drops; the marker is never cleared automatically. An abandoned admission gate
(for example after a hard crash mid-run) also refuses starts visibly; there is
no automatic crash repair or unretirement.

After a successful retirement, remove the checkout yourself (for example
`git worktree remove`). Do not expect this command to stop processes, delete the
identity file, or clean port claims.
