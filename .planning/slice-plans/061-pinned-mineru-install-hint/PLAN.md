# Pinned MinerU install hint

Work item: **SEED-060#story-1**.
Source: [story 1](../../seeds/SEED-060-mineru-version-for-pdf-books.md#story-1)
(refined 2026-09-29, from project retrospective finding DD-161).

## Goal and scope

A reader who installs MinerU the way Donut advises can attach a PDF book
through the CLI and get its outline.

- Every MinerU install hint names one pinned install that works with
  `cli/python/mineru_book_outline.py`, and the Python range that can install
  it:
  - the CLI's import-failure message (`MINERU_IMPORT_HINT` in
    `cli/src/commands/mineruOutline/mineruOutlineSpawn.ts`);
  - the outline script's docstring;
  - `e2e_test/fixtures/book_reading/regenerate_mineru_output_for_refactoring.sh`
    (setup comment and "cannot import mineru" error).
- The outline script, its `do_parse` call, and the attach flow do not change.
- **Excluded:** MinerU 4.x support; detecting an installed-but-wrong MinerU
  version; a repo command that holds an E2E stack up; rebuilding
  `.venv-mineru`.

## Outside-in proof

| Promise (story key example) | Owning slice | Proof |
| --- | --- | --- |
| MinerU missing → the CLI error names the pinned install and the Python range | 1 | `cli/tests/mineruOutlineSubprocess.errors.suite.ts` test "empty stdout with ModuleNotFoundError stderr uses MinerU install hint" asserts the pinned install string and the Python range instead of bare `mineru[pipeline]` |
| Regeneration script and docstring name the same install as the CLI | 1 | Review of the diff: all three hints carry the identical install command (no test; comments and a shell error message) |
| Fresh Python 3.12 env, follow the hint, attach *Attention Is All You Need* → outline | 1 (premise observed during planning) | See decisive premises: fresh venv built with exactly the hint's command, then the outline script on the 15-page PDF returns a non-empty outline. The CLI `/attach` consumes that script's `--json-result` output unchanged, and SEED-059's UAT attached this PDF through the CLI with MinerU 3.4.5 |

Local gate: the focused CLI test named in slice 1. No broader local check: only a message
string, a Python docstring and shell comments change.

## Decisive premises (observed 2026-09-29)

- **Only one test asserts the hint.** Searched the repo (excluding
  `node_modules`) for `MINERU_IMPORT_HINT`, `could not be imported`, and
  `mineru[pipeline]`: the hint is defined once
  (`mineruOutlineSpawn.ts:12`, used at `:141`), asserted only in
  `mineruOutlineSubprocess.errors.suite.ts:142-144` (run via
  `mineruOutlineSubprocess.test.ts`). No E2E step or feature checks it. The
  regeneration script is referenced only by its own usage comment.
- **MinerU 3.4.5 still has what the script imports.** PyPI metadata: 3.4.5
  declares a `pipeline` extra and `Requires-Python >=3.10,<3.14`. 4.0.10's
  wheel has no `mineru/cli/common.py` and no `pipeline` extra.
- **Fresh-environment install needs `six`.** In a new Homebrew Python 3.12
  venv (`python3.12 -m venv …; pip install 'mineru[pipeline]==3.4.5'`, exit 0),
  `from mineru.cli.common import do_parse, read_fn` imports, but
  `env -u PYTHONPATH <venv>/bin/python cli/python/mineru_book_outline.py
  attention.pdf --json-result` (arXiv 1706.03762, 15 pages) returns
  `{"ok": false, "error": "do_parse failed: No module named 'six'"}`. After
  `pip install six` in that venv, the same command exits 0 in 21 s with
  `ok: true`, `source: content_list`, and an outline from
  `[L1 p0] Attention Is All You Need` through `[L2 p9] References`
  (25 headings). MinerU models were likely already cached in the user cache
  from DD-161, so a first-time user also waits for a model download.
- **The focused proof command runs.** `CURSOR_DEV=true nix develop -c pnpm -C
  cli exec vitest run mineruOutlineSubprocess` in this worktree: 2 files,
  18 tests passed (272 ms), including the errors suite.
- **So the pinned install is**
  `pip install 'mineru[pipeline]==3.4.5' six` on Python 3.10–3.13.

## Current decisions

- Pin MinerU 3.4.5 (story decision; 4.x is a rewrite with no
  `mineru.cli.common`).
- The hint names `six` explicitly: MinerU 3.4.5's pipeline imports it without
  declaring it.
- The key example's fresh-environment attach is settled by the premise
  observation above, not re-run as slice proof. The slice must keep the hint's
  command identical to the observed one.

## Slices

### 1. Every MinerU install hint names the working pinned install

Type: Behavior
Status: done
Proof: focused CLI test
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run mineruOutlineSubprocess`
passes with the updated assertion; `grep -rn "mineru\[pipeline\]'"` (the
unpinned quoted install) outside `.planning/` and
`DonutRetrospectiveFindings.md` finds nothing.

Behavior: the CLI's Python has no MinerU → attaching a PDF → the error says to
install `'mineru[pipeline]==3.4.5' six` with Python 3.10–3.13 in the Python
the CLI uses. The outline script's docstring and the regeneration script's
setup comment and import error name the same command.

- Change the assertion in the errors suite first (red), then
  `MINERU_IMPORT_HINT`.
- Update the docstring line in `cli/python/mineru_book_outline.py` and the
  two places in `regenerate_mineru_output_for_refactoring.sh` (its setup
  comment's `python3 -m venv` becomes a 3.10–3.13 interpreter, for example
  `python3.12 -m venv`).

Accepted proof (2026-09-29): the focused CLI test failed on the new pinned-install
assertion before `MINERU_IMPORT_HINT` changed and passed after (2 files, 18 tests);
the unpinned-install grep finds nothing. All three hints read
`pip install 'mineru[pipeline]==3.4.5' six` with Python 3.10–3.13. The regeneration
script's "When to re-run" comment still mentions `mineru[pipeline]` as a version
bump trigger, not an install hint, and stays.

## Execution complete

Product advice: no backlog change. The story's goal is met: every install hint
names `pip install 'mineru[pipeline]==3.4.5' six` on Python 3.10–3.13, and the
fresh-environment attach was observed during planning with exactly that command.
The deferred items (MinerU 4.x support, a clearer message for an installed but
wrong MinerU) stay deferred; nothing in this execution raised their impact.
