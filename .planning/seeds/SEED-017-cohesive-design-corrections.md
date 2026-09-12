---
id: SEED-017
status: dormant
planted: 2026-09-10
planted_during: Five-day completed-plan design retrospective
trigger_when: selecting broader notebook synchronization or identity-safe composition
scope: L
---

# SEED-017: Remaining notebook synchronization and composition directions

## Why This Matters

Notebook owners need ordinary authoring and organization to remain usable
across Donut and a local checkout. The active append-only candidates have one
canonical home in [SEED-009](SEED-009-git-backed-local-notebook-workflow.md).
This seed retains unresolved directions outside that selected scope.

## Open product decision

Should every web mutation of Portable content advance accepted Git history, or
should the product deliberately expose a partial synchronization boundary?

The current selection proposes ordinary-note rename, deletion and move
synchronization in SEED-009. Other web authoring paths, including container
Readme changes and assisted or relationship creation, still need concrete
beneficiaries and examples before selection. Do not silently rebuild accepted
history from live state. Existing projection-drift recovery is a separate
preservation decision.

## Deferred Candidates

- **Broader divergent-history reconciliation:** preserve independent local
  revisions while receiving structural remote history. Rebase and conflict
  recovery remain possible later directions, outside the current no-rebase
  scope. Do not queue them merely because they were present in the old audit.
- **Richer identity-preserving organization:** changed-content moves, broader
  folder-subtree composition, and new-folder destinations. Establish meaningful
  owner examples and unambiguous identity evidence before selecting a story.
- **Earlier projection-drift recovery:** help an owner resume synchronization
  without losing either representation. Canonical framing remains in
  SEED-009; no second recovery story is created here.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns priority. These directions
remain unqueued. Example counts alone must not become rejection rules; new
stories should demonstrate a coherent operation rather than admit one more
hard-coded layout. Preserve identity and work when correspondence is ambiguous.

## When to Surface

When an owner encounters an unsupported workflow beyond the selected
append-only stories. Refine the blocked journey before creating a plan.
