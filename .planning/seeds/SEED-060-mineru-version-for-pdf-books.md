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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

A reader who installs MinerU the way Donut's CLI and docs advise can attach a
PDF book through the CLI and get its outline, instead of an import failure.

**Scope**

- Every MinerU install hint names one version that works with
  `cli/python/mineru_book_outline.py`: the CLI message in
  `cli/src/commands/mineruOutline/mineruOutlineSpawn.ts` (and its test in
  `cli/tests/mineruOutlineSubprocess.errors.suite.ts`), the script's own
  docstring, and
  `e2e_test/fixtures/book_reading/regenerate_mineru_output_for_refactoring.sh`.
  DD-161 worked with `mineru[pipeline]==3.4.5`; whether to pin 3.x or move the
  script to 4.x is a refinement decision.
- Deferred: a repo command that keeps a disposable E2E stack running for manual
  use (DD-161's `hold-stack.mjs`), and rebuilding a `.venv-mineru` whose Python
  lived in a garbage-collected Nix store path. These are agent conveniences with
  lower impact.

**Key example**

In a fresh Python environment, follow the CLI's MinerU hint, then attach
*Attention Is All You Need* (a 15-page PDF) through the CLI `/attach`: the book
gets its outline, as it did with MinerU 3.4.5 in SEED-059's UAT.

**Source finding:** DD-161.
