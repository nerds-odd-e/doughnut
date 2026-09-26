# Commit a finished slice while other slices are still in progress

**Identity:** SEED-043#story-1

## Source

- Story: [SEED-043#story-1](../../seeds/SEED-043-commit-gate-checks-committed-changes.md#story-1),
  refined 2026-09-26 with owner decisions (always check an index copy; the
  frontend `format` script drops `vue-tsc`).
- Finding: [DD-121](../../../DonutRetrospectiveFindings.md#dd-121).

## Goal and scope

A coordinator or contributor can format and commit a finished, staged frontend
change while other agents' unfinished edits sit unstaged or untracked in the
same checkout. The committed content is checked as strictly as today.

Included:

- `scripts/quality_changed.sh` in `lint` mode checks the frontend against a
  temporary copy of the index (`git checkout-index --all --prefix=…`) on every
  commit that selects the frontend, and removes the copy afterwards, pass or
  fail. The dependency install stays where it is today (the real checkout);
  the copy borrows the checkout's `node_modules` by symlink.
- The frontend `format` script runs Biome only.
- Guidance that describes the hook and the typecheck evidence
  (`.agents/skills/linting_formating/SKILL.md` "Format vs Lint",
  `.agents/skills/frontend/SKILL.md` "Frontend proof") says what they now do.

Excluded:

- Every other component's check (backend, CLI, mcp-server, test-fixtures,
  root, OpenAPI) keeps checking the working tree.
- Which files `format:changed` selects; Biome rewriting other slices' files.
- CI checks; the shared Open Dough concurrency rule; `vue-tsc` speed or cache.
- Workspace packages that `frontend/node_modules` links by relative path
  (`donut-test-fixtures`) are still read from the working tree. A staged change
  there does not select the frontend today either.

Assumption with representative evidence (2026-09-26, this machine): from the
repo root, `git checkout-index -a --prefix=<tmp>/`, then symlinks
`<tmp>/node_modules` and `<tmp>/frontend/node_modules` to the checkout's, then
`npx vue-tsc --noEmit` and `npx biome check .` in `<tmp>/frontend` → both exit
0; copy 0.8 s, `vue-tsc` 19.5 s cold, Biome 0.8 s, 50 MB. Not yet checked:
running them through `pnpm -C <tmp>/frontend lint` instead of `npx`; the first
step of slice 1 confirms which invocation works and keeps the simpler one.

## Outside-in proof

The hook is the entry point: `./scripts/git-hooks/pre-commit` run from the
execution checkout (it only runs `lint:changed` on staged components). Each
arrangement is made with a scratch spec under `frontend/tests/` and removed
afterwards; none is committed. After every run, `git status --porcelain` and
`git diff --cached | shasum` match their values before the run, and no copy
remains in the temporary directory (example 6).

| Seed example | Slice | Arrangement → observable result |
| --- | --- | --- |
| 1 | 1 | Clean staged frontend spec + unstaged spec with a Biome format error → hook exits 0 |
| 2 (hook) | 1 | Clean staged spec + untracked spec importing a missing export (`TS2305`) → hook exits 0 |
| 3 | 1 | Staged spec with a type error → exit non-zero, `vue-tsc` error printed; staged spec with a format error → exit non-zero, Biome error printed |
| 4 | 1 | Staged change removes an export used by an unchanged committed file → exit non-zero |
| 5 | 1 | Staged spec imports an export that exists only in an unstaged file → exit non-zero, while `pnpm -C frontend exec vue-tsc --noEmit` in the working tree passes |
| 6 | 1 | Index and working tree unchanged and no copy left, after both passing and failing runs |
| 2 (format) | 2 | Untracked spec with a type error under `frontend/` → `./scripts/run.sh pnpm format:changed` exits 0 and formats it; `pnpm frontend:lint` still reports the type error |

## Slices

### 1. The commit gate checks the frontend content being committed
Type: Behavior
Status: planned
Proof: examples 1–6 through `./scripts/git-hooks/pre-commit`, as in the table,
run through `./scripts/run.sh`; then `./scripts/run.sh pnpm frontend:lint`
passes on the clean checkout.

Behavior: staged frontend changes, with unrelated unstaged or untracked
frontend edits in the checkout → commit hook → passes or fails on the index
content only, leaving index and working tree untouched and no copy behind.

Change: in `quality_changed.sh`, route the frontend in `lint` mode through one
function that installs as today, copies the index to a `mktemp -d` directory
removed by a trap, links the two `node_modules` directories, and runs the
frontend's Biome and `vue-tsc` there. Other components and `format` mode are
unchanged. Update the hook description in the linting skill's "Format vs
Lint" (the frontend check reads the committed content, so unrelated unstaged
work does not affect it).

Sizing: one script function plus one guidance sentence; the proof runs six
arrangements at ~20 s each. About 10 minutes, driven by the proof runs, which
are not separable from the change; accepted.

### 2. Formatting no longer type-checks the frontend
Type: Behavior
Status: planned
Proof: the "2 (format)" row; `frontend/package.json` `format` is
`biome check --write .`.

Behavior: another slice's unfinished frontend file with a type error in the
checkout → coordinator runs `format:changed` → formatting succeeds; the type
error is left to proof, the commit gate and CI.

Change: drop `&& vue-tsc --noEmit` from the frontend `format` script; in the
frontend skill's "Frontend proof", reuse typecheck evidence from a `lint` or
`build` run (no longer `format`).

Sizing: two one-line edits and one run; under five minutes.

## Current decisions

- The hook stays check-only and always uses the index copy for the frontend;
  about 15–20 s more per frontend commit is accepted (owner, 2026-09-26).
- No permanent automated test is added for the hook: it has none today, CI
  does not run it, and each case needs a real `vue-tsc` run of ~20 s. The
  proof is the recorded demonstration above. The owner may ask for a test.
- The copy is the whole index, not only `frontend/`: the frontend reads the
  root `biome.json` and `tsconfig` files, and copying everything took 0.8 s.
