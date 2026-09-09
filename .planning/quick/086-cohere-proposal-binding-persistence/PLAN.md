# Cohere proposal binding persistence

Source: [quick/084](../084-cohere-initial-notebook-readme-publication/PLAN.md),
execution commit `ae249e0d24`, and SEED-016 Story 3.
Status: done.

## Outcome

`NotebookGitProposalBindingPersistence` owns proposal bundle, accepted-head,
timestamp, and binding persistence for initial Readme, Folder, and ordinary-note
publication. Caller clock-read points, validation order, exact authored commits,
and publisher SERIALIZABLE REQUIRES_NEW transaction ownership are unchanged.

## Completed slice

1. Preserve exact proposal acceptance through one binding persistence owner — done.
   Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed (55s),
   including exact-head/tree download and late binding-save rollback tests.
   Independent post-change-refactor: no edits; formatter passed.

## CI observation

Codex observer cell 14, coordinator root086, checkout `/Users/terryyin/git/doughnut`,
repository `nerds-odd-e/doughnut`, branch `main`, stream PID 78196.
Startup events reported historical commits outside this execution's pushed set;
they are not verification results for this slice. Final CI status is pending.
