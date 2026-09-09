# Publish the initial notebook README by itself

Source: [SEED-016 Story 3](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-3).
Status: done.

## Outcome

Empty Git-backed notebooks can publish one direct-child commit that adds only
root `README.md` with valid nonblank `type: Readme`. Authored bytes become the
notebook Readme; the binding accepts that exact head and tree. Other shapes
still refuse; initial folder publication is unchanged.

## Slices

1. Recognize sole initial notebook README — done (shape + temporary refusal)
2. Accept sole initial notebook README — done (acceptance + controller proof;
   initial-structure tests extracted to
   `NotebookGitProposalInitialNotebookStructureControllerTest`)

## Contract map

| Contract | Producer | Consumers | Proof owner |
| --- | --- | --- | --- |
| Exact sole-added-root-README shape | proposal tree-shape classifier | proposal publisher | focused shape tests |
| Valid authored notebook Readme | typed-path/Markdown validation | notebook Readme persistence | initial-structure controller test |
| Exact accepted head and tree | acceptance operation | binding download and later proposals | initial-structure controller test |
