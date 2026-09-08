# Describe existing-note batch publication in clone guidance

Source: [SEED-009 Story 14](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14),
execution retrospective of completed quick/063.
Status: planned; ready for direct execution. No implementation performed.

## Goal and scope

After cloning, a notebook owner learns that one direct-child commit can publish
edits to several existing ordinary notes. Correct the obsolete single-edited-note
limit without changing publication, pull, identity, or structural policies.

## Review evidence

- `6e76c03c30`: original Story 14 refinement; planning provenance only.
- `94c83e0ab2`: batch acceptance, learned-note proof, first persisted PLAN.
- `ff9432f00b`: invalid batch rejects without accepting a subset.
- `ac9f4ce0e1`: stale batch stays local with no publication POST.
- `e594d592f4`: installed CLI/second-checkout proof and completed PLAN.
- Intervening `d1f5b6fd06` changes workspace install lifecycle, not this story;
  excluded from the product review. The selected implementation patches were
  reviewed together at `e594d592f4`, which is also current HEAD at review time.

The final tree still says "a single edited Markdown note" in
`cli/src/nonInteractiveCli.ts:106`; `cli/tests/notebookClone.test.ts:75`
asserts that obsolete text. Owners may split a supported related revision or
infer that an added note is required. The batch itself works; guidance is stale.
No other concrete bug or unapproved scope change was found in the selected diff.

## Ordered slices

### 1. Explain the supported existing-note batch after cloning

Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts`

Behavior: An owner clones successfully → reads next-step output → sees that
one commit directly on accepted main may edit one or more existing ordinary
Markdown notes at unchanged paths, with the current structural and pull limits.

Update the existing clone-output assertion through `run(['notebook', 'clone',
...])` to express the supported batch; correct the production wording. Keep
the assertion focused on the capability rather than copying the entire help
paragraph. Preserve existing checks of independent guidance requirements.
No backend changes or new help framework are needed.

Sizing: about five minutes, high confidence; one output/test proof loop.
This leaf owns the corrected publication guidance and preservation of existing
limits. Divergent batch rebase remains a separate future Story 18.
