# Restore unfinished owned work

Publish a new repair first, or proceed once focused proof shows HEAD already fixed or
all failures proved infrastructure. Then run
`node '/ABSOLUTE/RESOLVED/SKILL/scripts/ci-repair-stash.mjs' restore --record RECORD_FILE`:
it applies the recorded OID with its index state, not `pop`, verifies its content
returned, and drops only that entry, never assuming `stash@{0}`. On `resumed`,
resume the same agents with the repair commit or no-change finding, affected
files, and saved handoff under the pause contract. `conflict` keeps the entry,
names its paths and OID, and reports `applied`; never reapply. On `none`, nothing
was put back: report OID and paths, keep the entry, and stop. On `partial`, restage
each `stagedNotRestored` path: `git restore --staged --source=OID^2 -- PATH`.
Resolve plain overlaps keeping both changes, then finish with
`node '/ABSOLUTE/RESOLVED/SKILL/scripts/ci-repair-stash.mjs' drop --record RECORD_FILE`,
never a hand drop; if meaning is ambiguous, keep the entry and report.
When a conflict was resolved by combining saved work and the repair, exact
content comparison may leave an older record marked `applied: none`. Inspect
the complete saved diff against the resolved checkout, confirming every saved
change and the repair survives, and preserve the saved index state. Then use
`node '/ABSOLUTE/RESOLVED/SKILL/scripts/ci-repair-stash.mjs' resolve --record
RECORD_FILE --reviewed-oid SAVED_OID` to record that review without reapplying
or dropping anything. This requires the recorded checkout/branch, no unmerged
paths and restored staged paths. Follow its `resolved` receipt with the exact
`drop --record RECORD_FILE` command above; other results retain the entry. On
`missing` (entry gone), `unapplied`, `ambiguous`, or `mismatch` (Git dropped
another writer's entry, recoverable by `droppedOid`), report OIDs and stop.
