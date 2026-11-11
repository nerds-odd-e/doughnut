# Doughnut CI repair restoration

Follow the CI monitor’s pause, repair, and publication steps. Replace
`<recovery-cli>` below with the absolute path to `scripts/ci-repair-stash.mjs`
in the Doughnut execution checkout.

Once the writers are quiescent, save the unfinished work and keep the returned
record path.

```sh
node "<recovery-cli>" save --checkout "<execution-checkout>" --label "<repair-label>"
```

## Restore unfinished owned work

Publish the repair first. You can also proceed when focused proof shows HEAD
already contains the fix or all failures came from infrastructure.

```sh
node "<recovery-cli>" restore --record "<record-file>"
```

Restore applies the recorded OID with its saved index state, verifies the
returned work, and drops only that entry on success. On `resumed`, give the
same agents the repair commit or no-change finding, affected files, and saved
handoff.

On `conflict`, keep the entry and work from the current checkout. Do not
reapply. If `applied` is `none`, report the OID and paths and stop. If it is
`partial`, restage each `stagedNotRestored` path with
`git restore --staged --source="<saved-oid>^2" -- "<path>"`. Resolve plain
overlaps so both the saved work and the repair survive. If their meaning is
ambiguous, retain the entry and report the conflict.

## Finish a reviewed resolution

An older conflict record may still say `applied: none` after a manual merge.
Compare the complete saved diff with the resolved checkout. Confirm every
saved change and the repair survives and the original staging is restored.
Record that review with the exact saved OID.

```sh
node "<recovery-cli>" resolve --record "<record-file>" --reviewed-oid "<saved-oid>"
```

The command requires the recorded checkout and branch, a resolved index, and
the original staged paths. It leaves the entry in place. After a `resolved`
receipt, or after completing the partial restore above, finish with the
recorded drop command.

```sh
node "<recovery-cli>" drop --record "<record-file>"
```

On `missing`, `unapplied`, `ambiguous`, or `mismatch`, report the OIDs and
stop. If Git dropped another writer’s entry, its `droppedOid` identifies the
recoverable object.
