# Give accurate guidance when a divergent batch cannot be pulled

## Source

[SEED-009 story 18a](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18a),
a bounded correction to delivered story 18 / quick 066.

## Goal and scope

An owner receives truthful guidance after a refused batch pull: local work is
preserved; reconciliation or recreation onto accepted history is required
before publication. Change only the batch-specific error and its observable
assertions. Preserve all eligibility, Git mutation, and publication behavior.
Exclude automatic recovery, recovery tutorials, repeated-pull support, broader
batch rebase, new commands, and backend changes.

## Ordered slices

### 1. Explain the publication prerequisite after a refused batch pull

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`
observes accurate two-note unsupported-accepted refusal guidance, unchanged
checkout on refusal, and the supported one-C-save rebase.

Behavior: One A/B commit diverges from two accepted C saves → pull → refusal
explains preserved work and reconciliation onto accepted history before publish.
