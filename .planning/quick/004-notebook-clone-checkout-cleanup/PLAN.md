# CLI notebook clone finishes the checkout before installing it

## Source

- Follow-up correction from `execution-retrospective` (2026-09-05) on the
  completed `.planning/quick/002-open-existing-notebook-locally/PLAN.md`
  (SEED-009, Story 1 — "Open an existing Donut notebook in Obsidian and an AI
  IDE"). That plan's own "Current decisions" stated: "The local checkout has
  no configured standard remote in Story 1. The CLI removes the temporary
  bundle origin," and its Slice 15 boundary stated failed acquisition "fails
  before installing a destination." Both claims are contradicted by the
  shipped code; this plan corrects `cli/src/commands/notebook/notebookAcquisition.ts`
  so the implementation matches the story's own stated contract.
- Reviewed commit set (original plan's execution, for context; not re-executed
  here): `e139206ef4` (CLI staging primitives), `40de67b23c` (EXDEV fallback),
  `48d5dda363` (clone command wiring), `f5bb3b6a9d` (local binding +
  publish-limitation message), `bfd05182b6` (failure-path error surfacing).

## Outcome and boundaries

For an owner running `donut notebook clone <notebook-id> <destination>`, the
resulting local checkout is genuinely finished — and genuinely absent on
failure — by the time the command exits, with no exceptions carved out for
steps that currently run after the destination already exists on disk.

Representative behavior:

- **Pre-condition:** a configured owner access token, a real `git`
  executable, an automatically Git-backed notebook, and a destination that
  does not yet exist.
- **Trigger:** the owner runs `donut notebook clone <notebook-id> <destination>`
  and it completes successfully.
- **Post-condition:** the checkout at `destination` has no `origin` (or any
  other) Git remote and no stale `remotes/*` tracking refs — nothing points at
  the deleted temporary bundle file.

- **Pre-condition:** the same setup, but recording the local Git config
  binding (`donut.notebook-id` / `donut.api-origin`) fails after an otherwise
  successful download and clone (e.g. the filesystem refuses to write
  `.git/config`).
- **Trigger:** the owner runs the clone command.
- **Post-condition:** `destination` does not exist afterward — exactly as for
  every other acquisition failure this CLI already handles — and the CLI
  reports one actionable error.

Both defects share one root cause: `acquireNotebookGitCheckout` currently
finishes preparing the checkout (recording local Git config) **after**
`moveCheckoutIntoDestination` has already installed it at `destinationPath`,
and never removes the remote that `git clone <bundle-file> <target>` creates
by default. The fix is to finish preparing the checkout — local Git config
*and* removing the bundle-file remote — while it is still the staged
`checkoutDir`, so the atomic move is genuinely the last thing that can happen,
and nothing after it can leave a half-finished destination behind.

Boundaries:

- No change to the four failure cases Slice 15 already covers (existing
  destination, missing git, denied/failed download, invalid bundle) — their
  current behavior and tests stay as-is.
- No change to the local Git config binding's keys/values or to the
  publish-limitation message.
- No publish/remote-sync behavior — that remains Story 2/3's scope.

## Outside-in proof

Focused CLI unit tests only (no new E2E scenario — this is an internal
ordering correction to behavior the existing E2E scenario doesn't
distinguish):

1. Extend the existing real-git happy-path test (`cli/tests/notebookClone.test.ts`)
   to assert `git -C <destination> remote` prints nothing (no `origin`, no any
   other remote) after a successful clone.
2. Extend the existing `cli/tests/notebookAcquisition.test.ts` test "git
   config recording failure surfaces an error" to also assert
   `fs.existsSync(destinationPath)` is `false` after the rejection (today this
   assertion would fail — that is the confirmed bug).

## Ordered slices

### 1. Notebook clone checkout is finished before it is installed
Type: Behavior
Status: done
Proof: The two proof points above, both green; all of Slice 8-15's existing
`notebookAcquisition.test.ts` and `notebookClone*.test.ts` cases remain green.

Learnings: `recordLocalNotebookBinding`'s parameter was renamed to `checkoutDir`
and a new `removeOriginRemote` helper (same `runSystemGitOrThrow` pattern) was
added; both now run against the staged `checkoutDir` before
`moveCheckoutIntoDestination`. Config recording runs before remote removal to
preserve the existing "config recording failure" test's failure-mode
assertion. No new public surface; `moveCheckoutIntoDestination`'s existing
refusal/EXDEV-fallback behavior is untouched. Post-change-refactor made a
comment-only doc update (JSDoc above `acquireNotebookGitCheckout`) and found
no duplication or structural issues.

proof:
  command: CURSOR_DEV=true nix develop -c bash -c "cd cli && pnpm exec vitest run tests/notebookAcquisition.test.ts tests/notebookClone.test.ts tests/notebookClone.failures.test.ts"
  covers: acquireNotebookGitCheckout reordering (config + remote-remove before move), no-dangling-origin-remote proof, config-recording-failure leaves no destination proof, plus all pre-existing notebookAcquisition/notebookClone/notebookClone.failures cases
  result: pass

Behavior: Given a notebook clone that downloads and clones successfully,
when `acquireNotebookGitCheckout` finishes preparing the checkout, it removes
the `origin` remote that `git clone <bundle-file> <target>` creates and
records the local Git config binding while the checkout is still the staged
`checkoutDir` — before the atomic move into `destinationPath` — so a
successful checkout has no remote pointing at the deleted temporary bundle,
and a config-recording failure never leaves anything at `destinationPath`.

Execution notes:

- Add a `git remote remove origin` step (via the existing
  `runSystemGitOrThrow` helper, same pattern as `recordLocalNotebookBinding`)
  run against `checkoutDir`, and move `recordLocalNotebookBinding`'s existing
  call so both it and the new remote-removal run before
  `moveCheckoutIntoDestination`, not after.
- `recordLocalNotebookBinding` currently takes `destinationPath` as the path
  to run `git config` against; it will need to run against `checkoutDir`
  instead (the path doesn't matter to `git config --local`, only that it's
  the checkout being finished) — rename the parameter if that improves
  clarity, but do not change its recorded key/value semantics.
- Keep `moveCheckoutIntoDestination`'s existing "destination already exists"
  refusal and EXDEV fallback exactly as they are; only reorder what runs
  before vs. after the move.
- This is a pure reordering plus one new `git remote remove origin` call —
  no new public surface, no change to `acquireNotebookGitCheckout`'s
  signature or its documented contract (only correcting the implementation to
  match what that contract already says).
