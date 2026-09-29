---
id: SEED-060
status: dormant
planted: 2026-09-29
planted_during: owner request to queue the highest-priority open project retrospective finding
trigger_when: attaching PDF books through the CLI, or preparing real-book manual acceptance
scope: small
---

# SEED-060: Name a MinerU version that extracts PDF book outlines

## Why This Matters

Project retrospective finding DD-161 in
[DonutRetrospectiveFindings.md](../../DonutRetrospectiveFindings.md) cost about
28 minutes of one manual acceptance slice, mostly repairing MinerU. Checking it
showed the problem reaches CLI users, not only agents:

- The released CLI bundles `cli/python/mineru_book_outline.py`, which imports
  `mineru.cli.common`. When MinerU is missing, the CLI tells the user to
  `pip install 'mineru[pipeline]'`.
- MinerU 4.0.0 (2026-09-16) and later have no `pipeline` extra and no
  `mineru.cli.common`; 3.4.5 has both (PyPI, checked 2026-09-29 against 4.0.10).
- So anyone installing MinerU as Donut advises since 2026-09-16 gets a setup
  that cannot extract a PDF book's outline.

The correction lands in Donut's CLI, scripts and docs, not in shared Open Dough
guidance.

## Story Decomposition

One story. This seed captures backlog work, not an execution plan.

<a id="story-1"></a>

### Attach a PDF book after installing MinerU as Donut advises

**Identity:** SEED-060#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/061-pinned-mineru-install-hint/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"65304a47fd6c7dbe890dc14dc77cfc0d2f7433d3274f9f5c02e2858c2e6dd468","plan":"325f14da083fd2f91a8367c59be515ecc7ddad9cb43a7925deab0c821697205f"}}
```

**Goal**

A reader who installs MinerU the way Donut's CLI and docs advise can attach a
PDF book through the CLI and get its outline, instead of an import failure.
This keeps PDF book attachment usable for every new CLI user since MinerU
4.0.0 (2026-09-16) broke the unpinned advice.

**Scope**

- Decision: pin MinerU 3.4.5 (`'mineru[pipeline]==3.4.5'`). MinerU 4.x is a
  rewrite: no `pipeline` extra, no `mineru.cli.common.do_parse`, a new
  `mineru` command and a new middle-JSON/content-list schema. Donut's stored
  content locators and the E2E MinerU fixtures come from 3.x output, so moving
  to 4.x is a separate, larger story, not a version-hint fix.
- Every MinerU install hint names that same pinned install plus `six`
  (`pip install 'mineru[pipeline]==3.4.5' six`): a fresh 3.12 venv without
  `six` fails in `do_parse` with "No module named 'six'" (observed while
  planning). The hints are: the CLI message in
  `cli/src/commands/mineruOutline/mineruOutlineSpawn.ts` (and its test in
  `cli/tests/mineruOutlineSubprocess.errors.suite.ts`), the script's docstring
  in `cli/python/mineru_book_outline.py`, and
  `e2e_test/fixtures/book_reading/regenerate_mineru_output_for_refactoring.sh`
  (both its comment and its error message).
- The hint also says which Python works: MinerU 3.4.5 requires Python
  3.10–3.13, so a default 3.14 interpreter cannot install it.
- Boundary assumption: the outline script itself, its `do_parse` call, and the
  attach flow do not change.
- Deferred: supporting MinerU 4.x; detecting an installed-but-wrong MinerU
  version with a clearer message than today's import failure; a repo command
  that keeps a disposable E2E stack running for manual use (DD-161's
  `hold-stack.mjs`); rebuilding a `.venv-mineru` whose Python lived in a
  garbage-collected Nix store path. These are lower-impact conveniences or a
  larger migration.

**Key examples**

- Fresh Python 3.12 environment, no MinerU → run the install command the CLI's
  MinerU hint names → attach *Attention Is All You Need* (15-page PDF) through
  the CLI `/attach` → the book gets its outline, as it did with MinerU 3.4.5 in
  SEED-059's UAT. (One-time manual check, not an automated test.)
- CLI Python has no MinerU → attach a PDF → the error names the pinned
  `'mineru[pipeline]==3.4.5' six` install and the supported Python range, not the
  unpinned `mineru[pipeline]`.
- A contributor reads the regeneration script's setup comment or hits its
  "cannot import mineru" error → both name the same pinned install as the CLI.

**Source finding:** DD-161.

**Plan:** [061-pinned-mineru-install-hint](../slice-plans/061-pinned-mineru-install-hint/PLAN.md)
