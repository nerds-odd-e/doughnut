# Publish the initial notebook README by itself

Source: [SEED-016 Story 3](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-3).
Status: planned.

## Goal and scope

From a notebook with no folders or live notes and an empty accepted Portable
tree, accept one direct-child commit adding exactly one regular file:
`README.md`. Require valid, nonblank `type: Readme` Markdown, store its authored
bytes as the notebook Readme, and accept the exact commit atomically.

Exclude every second path, folders, ordinary notes, existing notebook content,
README edits, blank Readmes, attachments, multiple unpublished commits,
stale/divergent history, and bulk import. Preserve the already delivered
initial folder shapes and the refusal of root README changes outside this exact
initial case. No CLI or API contract changes are required.

## Decisions and constraints

- Recognize a dedicated shape only when inspected regular files contain one
  added root `README.md`, with no accepted blob and no other path.
- Lock eligibility to an empty accepted projection and a notebook with no
  folders or live notes. The exact proposed projection is the notebook Readme
  alone.
- Reuse the existing typed-Markdown and `type: Readme` validation; do not add a
  permissive general README-update path.
- Keep mutation and binding acceptance inside the publisher's existing
  SERIALIZABLE, `REQUIRES_NEW` transaction so failed validation remains atomic.
- Preserve authored bytes exactly. Accepted ADR 0004 owns the Portable root
  README path and type contract; Accepted ADR 0001 keeps Git plumbing in JGit.

## Ordered slices

### 1. Recognize the sole initial notebook README
Type: Structure
Status: pending

Introduce an exact one-file initial-notebook-Readme shape alongside the
existing initial folder shapes and route it before ordinary-note validation.
The classifier must reject any accepted root README, any second path, and any
non-root README. Keep this slice behavior-preserving by routing to a refusal
until the acceptance operation exists.

Proof: focused shape tests cover the one added root `README.md` match and the
nearest excluded boundaries: an accepted root README and one additional path.
The existing backend suite remains green.

### 2. Accept the sole initial notebook README
Type: Behavior
Status: pending

At the controller boundary, first prove that an empty Git-backed notebook can
publish a proposal whose complete tree is only valid nonblank `README.md` with
`type: Readme`. Implement the narrow acceptance operation: verify the accepted
projection is empty, require no folders or live notes, validate and store the
authored Readme, require the resulting projection to match the proposed head,
then update the binding to that exact head and bundle.

Proof: the controller test observes the stored notebook Readme, no folders or
live notes, and an accepted/downloaded tree and head exactly equal to the
proposal. A neighboring controller test proves a second path does not enter
this acceptance route. The existing backend suite remains green, including
initial folder publication and reserved README refusal outside this case.

## Contract map

| Contract | Producer | Consumers | Proof owner |
| --- | --- | --- | --- |
| Exact sole-added-root-README shape | proposal tree-shape classifier | proposal publisher | focused shape tests |
| Valid authored notebook Readme | typed-path/Markdown validation | notebook Readme persistence | controller publication test |
| Exact accepted head and tree | acceptance operation | binding download and later proposals | controller publication test |

## Planning assessment

The story is one exact one-file boundary with two small proof-owned slices and
no open product or architecture decisions. Further slice-plan refinement is
not needed before execution.
