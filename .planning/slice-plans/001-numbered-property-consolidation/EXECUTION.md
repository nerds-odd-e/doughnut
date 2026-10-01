# Execution context

- Workspace: `/Users/terryyin/git/doughnut/.worktrees/existing-numbered-property-keys-become-one-list`
- Branch: `codex/existing-numbered-property-keys-become-one-list`
- Mode: story-branch; remote: `origin`; integration target: `main`.
- Publisher: `dashboard-mac.lan-doughnut`; assigned agent: Rio-chan.
- Starting revision: `a11dfaee415d7fdbaf4bb916f0d125581da37a03`.
- Published claim and initial candidate: `28442ce9d9ea3e50d1f002081a6b93907f0f57f1`.
- Remote execution branch confirmed at the published claim. Claim CI on trunk
  is unobserved; subsequent increments observe the execution branch.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` passed;
  `CURSOR_DEV=true nix develop -c pnpm exec node --version` passed (`v26.10.0`).
- Replanning remains authorized within this plan's scope and sizing rules.
- CI source: GitHub Actions, `nerds-odd-e/doughnut`, workflow `ci.yml`
  (`donut CI`), verified push selector. Managed delivery owns observation setup.
- Slice 1 accepted revision: `2a87cb0e014abfc7cc007af35fbe8bd8c89ed23c`.
- CI observer: `/tmp/dough-ci-501/watch-Z3lvDT`, PID 8731, this checkout and
  execution branch, with exact revision registration. Live Codex notifications
  are unavailable: its stream entry creates another observer rather than
  attaching. Extra mailbox `watch-bX3jiL` was stopped with no unread events;
  use the retained managed observer for the bounded completion operation.

## Slice 2 sizing reassessment

The first attempt became test-ready at approximately nine active minutes, then
acceptance inspection found two correctness gaps. The target was exceeded; the
hard limit was not crossed. All five attempt-owned algorithm/test edits are
safely parked at `/tmp/numbered-property-family-attempt.patch` and restored to
HEAD. Slice 1 and coordinator planning edits remain intact.

`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed on the prototype
(2744 tests, zero failures/errors, two skipped). Ten authored-frontmatter cases
observe scalar/list mappings, order/deduplication, case distinction, url and
structural eligibility, shared scalar meanings, unsupported outcomes and BOM /
CRLF / unrelated source bytes. This is incomplete acceptance evidence: whitespace
keys were incorrectly grouped and large suffixes threw NumberFormatException.

The sizing assumption missed exact authored naming and source normalization
inside reused owners. The corrected slice reuses the compatible prototype and
has one remaining proof loop for those gaps (target five active minutes; complete
suite wait remains excepted). Twelve slices remain in this cumulative design;
no story boundary or ADR change is required. Record the original nine-minute
attempt when reporting continuation; a retry does not erase it. All later slices
are retained because this evidence concerns the common transformation contract.

## Slice 2 accepted proof

Corrected continuation took four active minutes after the recorded nine-minute
attempt. Independent refactoring separated exact family policy from shared YAML
source-edit mechanics, with the public authored-document contract preserved.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after all edits:
2746 tests, zero failures/errors, two skipped. XML for
`NoteContentMarkdownNumberedPropertiesTest` confirms twelve selected cases.

The authored-Markdown fixtures and exact output/focus assertions observe forced
lists, scalar and list-item mappings, stable deduplication and numeric ordering,
missing bases, case/whitespace distinction, suffixes beyond Long range, url and
structural/word eligibility, shared scalar meanings, diagnostic-only unsupported
shapes, repeated-run stability and BOM/CRLF/unrelated source preservation.
`PropertyKeyNaming` retains one suffix representation using BigInteger;
structural consumers trim explicitly. Maps reuse the existing PropertyFocus.
The refactor's full-suite proof also covers existing source-edit callers.
Independent refactor completed; coordinator formatter passed. Migration tracker
and accepted-publication integration remain slice 3's obligation.
