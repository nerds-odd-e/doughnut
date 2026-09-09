# Publish a minimal initial container with one note

Source: [SEED-016 Story 4](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-4).
Status: done.

## Outcome

Empty Git-backed notebooks can publish either exact two-file initial
container-and-note commit: root `README.md` plus one root ordinary Note, or
one root Folder `README.md` plus one ordinary Note inside that Folder (no
notebook Readme). Authored bytes and projection are stored; the binding
accepts that exact head and tree. After supported shapes decline, another
safe typed role-correct initial Readme/Note composition fails loudly under
ADR 0006 instead of reserved-README 400. Invalid Markdown, unsafe paths,
wrong types, and related deliberate client outcomes are unchanged.

## Slices

1. Publish initial notebook Readme with one root Note — done
   (`NotebookGitProposalInitialNotebookReadmePublication` +
   `NotebookGitProposalInitialNotebookReadmeControllerTest`)
2. Publish initial Folder Readme with one contained Note — done
   (`NotebookGitProposalFolderCreationShape` /
   `FolderAcceptance` +
   `NotebookGitProposalInitialFolderAndContainedNoteControllerTest`)
3. Identify valid unmatched initial compositions — done
   (`NotebookGitProposalInitialComposition.findValidUnmatched`)
4. Let the next valid initial composition fail loudly — done
   (publisher `IllegalStateException`; temporary probe removed; lifecycle
   owned by `ControllerSetup` / `FailureReportFactory`)

## Contract map

| Contract | Producer | Consumers | Proof owner |
| --- | --- | --- | --- |
| Exact notebook-Readme + root-Note shape | initial notebook publication recognition | proposal publisher | Slice 1 controller proof |
| Exact Folder-Readme + contained-Note shape | Folder creation recognition | proposal publisher | Slice 2 controller proof |
| Authored container and Note persistence | initial publication services | notebook projection and bundle download | Slices 1–2 controller proofs |
| Valid unmatched initial composition classification | initial proposal composition boundary | publisher fallback | Slice 3 regression suite |
| Uncaught failure reaches Failure-report lifecycle | publisher fallback and `ControllerSetup` | developer Failure report | Slice 4 demonstration plus existing failure-report tests |
| Invalid requests retain deliberate client outcomes | existing validators | CLI/API caller | existing validation suites |
