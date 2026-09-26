# Clone test states the clone output instead of what it no longer says

## Source

- Story: [SEED-046#story-11](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-11)
- **Identity:** SEED-046#story-11
- Provenance: execution retrospective of SEED-046#story-3 (plan
  `.planning/slice-plans/003-clone-and-publish-name-next-step/PLAN.md` at
  df5ba26eb0), commit 6d807aa4ea.

## Finding

`cli/tests/notebookClone.test.ts`, first test ("clone downloads the accepted
bundle…"), proves the short clone message with four `toContain` fragments
(one is just `commit`) and two `not.toContain` checks naming deleted text
(`Publishing currently accepts`, `rebases`). The owner's standing rule is that
removed content leaves no negative checks for its absence. An exact assertion
of the whole message proves both "these lines" and "nothing else" without
naming the old text.

## Preserved

The clone message itself and every other assertion in the test.

## Slices

### 1. Assert the exact clone message

Type: Structure
Status: planned
Change: replace the six output assertions with one
`expect(ctx.getLogSpy()).toHaveBeenCalledWith(<four joined lines>)` (or an
equivalent exact equality on the joined output).
Proof: the test still passes and fails if any line changes or extra text is
appended; `CURSOR_DEV=true nix develop -c pnpm cli:test` green.
