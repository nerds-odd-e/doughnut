# Repair the North Star attachment-classification link

Status: planned retrospective correction.
Work item: `.planning/quick/008-repair-attachment-contract-link/PLAN.md`.

## Source and bounded outcome

- Source: execution retrospective of
  [SEED-035#story-9](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-9)
  and plan 003. Reviewed published implementation commits:
  `41432e75e9`, `eb45391b29`, `c3ef810931`, `2971b7eb08`, `af23aba73b`,
  `c8ce42da17`, `53bf499f7a`, `474b8e1a69`, `cbd8d0bdb1`, `34fd2fe9bc`,
  `70b9116e13`, and `6125fe0c3d`.
- Finding: slice 9 extracted **Classification and references** from
  `docs/notebook-git-synchronization.md` into
  `docs/notebook-git-attachments.md`, but `.planning/NORTH-STAR.md` still
  links to the old document and therefore to an anchor that no longer exists.
- Beneficiary and outcome: maintainers and agents following the North Star's
  **One format boundary** reach the current attachment-classification contract
  instead of a dead anchor.
- Scope: update that one North Star link to the existing heading in the focused
  attachment contract.
- Preserved promises and constraints: keep the documentation split, North Star
  direction, attachment behavior, ADR references, and all product code and
  tests unchanged. This correction follows Accepted ADRs 0001 and 0004; it
  makes no architectural decision.
- Exclusions: a repository-wide Markdown link checker, further documentation
  reorganization, wording changes, and product behavior.

## Outside-in proof

The proof reads the maintained direction entry and its destination heading.
Both checks must pass:

```sh
CURSOR_DEV=true nix develop -c sh -c \
  "rg -q 'notebook-git-attachments\.md#classification-and-references' .planning/NORTH-STAR.md && \
   rg -q '^## Classification and references$' docs/notebook-git-attachments.md"
```

## Slices

### 1. North Star navigation reaches the attachment classification contract
Type: Behavior
Status: planned

Behavior: given the North Star's **One format boundary** direction, when a
maintainer follows its integration-details link, the destination is the
**Classification and references** section in the current attachment contract.

Change: repoint the existing link in `.planning/NORTH-STAR.md`; do not duplicate
or move the destination text.

Proof: the literal focused command above, plus `git diff --check`.

## Current decisions

- The existing `docs/notebook-git-attachments.md` split is retained. This plan
  corrects its missed incoming reference; it does not reverse the refactor.

## Learnings

- Default `rg` skips hidden `.planning/`; incoming-link checks for maintained
  product direction must include that directory explicitly or use `--hidden`.

## Cumulative design and sizing

One existing direction link owns the navigation and one existing heading owns
the contract. The slice has one proof loop and no integration uncertainty.
