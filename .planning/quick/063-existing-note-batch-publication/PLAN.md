# Publish a related batch of edits to existing notes

Source: [SEED-009, Story 14](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14).
Status: complete.

## Goal and scope

A notebook owner publishes related edits to two or more existing ordinary notes
as one authored commit, retaining each note's identity and learning history.
Use the existing CLI and a clean bound `main`, exactly one unpublished
single-parent child of accepted `main`, unchanged root/nested paths, and valid
existing body/frontmatter semantics. The starting projection matches accepted
history. Accept all edits and the exact commit together; another clean checkout
receives it through ordinary pull. Invalid content or remote advancement leaves
local work available and changes no remote notes/history through that attempt.

Exclude additions, deletions, moves, renames, README/folder changes, multiple
unpublished commits, divergent batch rebase, drift repair, new commands/UI, and
new identity policy. Preserve delivered mixed additions/edits behavior.

## Proof ownership

| Contract promise | Owner and observation |
| --- | --- |
| Root/nested body/frontmatter edits retain identity/private learning data | Slice 1: `NotebookGitExistingNoteBatchPublicationControllerTest` shown content and retained tracker identity/scheduling fields on both notes |
| Exact authored commit accepted atomically and downloadable | Slice 1: returned/downloaded head and tree equal proposal; existing late-rollback test remains green |
| Invalid batch accepts no subset and local work survives | Slice 2: fresh reads of both original notes and accepted binding after rejection |
| Stale batch preserves local work and accepted web save | Slice 3: CLI ancestry refusal, POST count 0, original local head and both files intact |
| Existing CLI publishes and second clean checkout pulls complete batch | Slice 4: `cli_notebook_existing_note_edits.feature`; Donut contents, receiver head, both files |
| Existing structural/ancestry/drift limits and mixed additions/edits stay intact | Slice 1 controller suites; Slices 3–4 existing CLI/E2E coverage |

## Ordered slices

### 1. Accept an edits-only revision on the original learned notes
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — pass.

Behavior: A matching notebook has learned ordinary notes at root and nested
paths, and one valid direct-child commit edits both → publish → both original
notes carry the authored revision and retain learning data; the exact authored
commit is accepted and downloadable.

### 2. Refuse the complete batch when one edited note is invalid
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — pass.

Behavior: A matching notebook receives one direct-child commit with a valid edit
and an invalid Portable note → publish → neither edit is accepted; fresh reads
show both original note contents/identities and unchanged accepted head/bundle.

### 3. Keep a stale local batch available after remote advancement
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts` — pass.

Behavior: A clean bound checkout holds one two-note edit commit, while accepted
history contains a later web content save from the shared base → publish → an
ancestry refusal is reported without POST; the local commit and both edited
files remain intact. No remote mutation is requested.

### 4. Publish and receive the related revision through the installed CLI
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature,e2e_test/features/cli/cli_notebook_existing_note_edits.feature` — pass.

Behavior: Two clean bound checkouts start at the same accepted head → the owner
commits a root body edit and a nested valid-frontmatter edit together in the
first checkout, publishes, then pulls in the second → Donut shows the revision
and the receiver contains both authored files at the same accepted commit.
