# Release state fixture isolation

CI repair observed 2026-11-13, after accepted slice 11
`2ac7cd705315394be7dab50ebee451b3bb7a8e2e`. Repair basis is local merge
`3b80788b5654b8441f1fb3c27a2ce73c4ba1521d`, which incorporates the four
backlog/retrospective records from trunk `c2305a93943ee83228ac919e2af4bec9056d4353`.
The merge adapter retained both histories, validated the backlog and committed
with agent/developer credit. No product code changed in that merge.

## Failure and cause

Run `43402490623`, attempt 1, job `131808535315` (Other Unit Tests), step
Run script unit tests, failed two observations:

- Selected-state transport failure returned status 0 instead of 1.
- Exact succeeded-release admission observed two requests instead of one.

The complete-attempt diagnostic has one failed job and no inventory gaps;
all failures are these two cases. Raw excerpts are retained at
`/tmp/donut-ci43402490623-job131808535315.log:1514` and the final diagnostic at
`/tmp/donut-tracker-ci-43402490623-1-followup.json`.

`runStateCommand` closed its unavailable fixture's ephemeral listener before
spawning the real command. A concurrent fixture could acquire that freed port:
the selection then successfully posted to it, while its own request made the
other fixture's count two. This schedule reproduces both observed assertions;
CI did not retain a TCP trace, so its exact kernel port allocation is inferred.

The fixture now reserves its listener until normal cleanup and destroys accepted
sockets when simulating unavailable transport. Successful HTTP fixtures retain
their behavior. The two-line relocation changes no production code or maintained
test assertion and adds no new fixture framework.

## Reproduction and affected proof

Disposable experiments controlled the collision window outside the repository:

```sh
CURSOR_DEV=true nix develop -c node --test /tmp/donut-ci43402490623-collision.test.mjs
CURSOR_DEV=true nix develop -c node --test /tmp/donut-ci43402490623-collision-green.test.mjs
CURSOR_DEV=true nix develop -c bash scripts/test/application-release.test
```

RED native `47491` exited 1: selected command status 0 and competitor request
count 2, matching both failures. Repaired collision native `14217` exited 0:
selected command status 1 with GCS transport error, no captured writes, and the
companion received only its own GET. Its attempted same-port acquisition was
refused. These scratch observations are not maintained implementation tests.

The existing public release-test entry, native `28839`, exited 0. Its concurrent
first invocation passed all 111 cases; the entry invocation passed all four.
The two exact CI cases, initialization transport failure, retry/replay,
publication and immutable-identity observations retain their assertions.
Raw results and native terminal identities are in
`/tmp/donut-ci43402490623-repair.md` and
`/tmp/donut-ci43402490623-release-entry-green.log`.

The installed consumer trace covers all five direct fixture consumers:
selection, state admission, state initialization, retry and entry tests. They
all ran in that entry. Production state/reconciliation modules do not import the
fixture; the matching observer substring is unrelated. No consumer is pending.
The parent script-test runner and CI workflow invoke this same entry. The earlier
backend proof is unaffected by this fixture-only change.

## Ownership and restoration

All execution writers and proof commands had ended before the preservation
helper. It returned `clean`, OID null, at
`/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-vPH5Kg/record.json`.
No stash was created or another entry changed. The repair agent owned every
verification through its terminal result and returned uncommitted changes.

This local 16-CPU/48-GiB host had load 5.12/9.24/19.13 and 48% free memory before
the affected proof, with no competing active test worker. The required bounded
entry ran once with its concurrency retained; no backend/browser suite was
duplicated. The fixture remains 142 lines. Execute Plan owns the fresh refactor,
formatter, publication and exact preservation-helper restoration before 12.

The fresh independent refactor returned `REFACTOR COMPLETE`, with no edits.
It inspected the listener/child/cleanup lifetime and every consumer disposition;
all accepted observations remain applicable. No duplicate tests ran. Actual
changed-source selection resolved both conventions off, all three owned files
were within 250 lines, and both whitespace checks passed.

Guarded formatter, check-only hook and managed publication accepted
`b7213b7465b3ee84d26fd7d679a7a03735ac9776`, with no reconciliation and the
replacement observer reused. The exact saved-record restore returned `resumed`,
OID null, applied false and no dropped entry. The tree was clean before 12.
