# Require an ordinary Note in the initial three-path publication

Source: [SEED-016 Story 2](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-2)
and the execution retrospective of completed
[quick/081](../081-initial-folder-with-one-note/PLAN.md).
Status: planned; not executed.

## Goal and scope

A notebook owner publishing the exact initial notebook Readme, root Folder
Readme, and one direct-child note gets the promised ordinary-Note-only
acceptance boundary. Require the third document to carry `type: Note`;
refuse a non-Note type without changing notebook content or the Git binding.
Preserve the valid three-path publication and the two smaller delivered shapes.

Keep this eligibility rule local to initial three-path publication. Do not
change the general typed-Markdown codec, existing ordinary note publication
routes, unknown-type round-trip behavior, endpoint, API schema, or database.
All original tree, ancestry, empty-notebook, and content boundaries remain.

## Evidence and current decisions

- Original execution-ready plan: `f478c48918`; execution commits:
  `bf20006629` (shared note-addition extraction) and `1ce3f01bc5`
  (three-path acceptance and completion). Aggregate boundary:
  `f478c48918..1ce3f01bc5`. Merge: `20f6a4e700`.
- At that boundary and current HEAD, `acceptInitialCreationWithNote` calls
  `NoteAddition.apply` without a Note-type eligibility check. The Markdown
  validator requires only a nonblank scalar type; authored-property validation
  does not restrict type. Persistence and Portable projection preserve those
  bytes. Thus `type: Relationship` can pass despite Story 2's explicit exclusion.
- The controller success proof uses only `type: Note`; it does not distinguish
  this forbidden input. This finding is based on source tracing, not a newly
  executed controller reproduction.
- Accepted ADR 0001 defines the domain terms. Accepted ADR 0004 distinguishes
  `Note`, `Relationship`, and `Readme` and permits unknown types in the format.
  This is a bounded publication eligibility correction, not a format ban or
  ADR change. Preserve the shared note-addition seam.
- Retain the publisher's SERIALIZABLE, REQUIRES_NEW transaction and exact
  final-tree comparison. No new transaction owner is needed.

## Ordered slices

### 1. Refuse a non-Note third document atomically
Type: Behavior
Status: planned

Behavior: An empty matching Git-backed notebook receives a direct-child
proposal with exactly `README.md`, `New Folder/README.md`, and
`New Folder/First note.md`, where the third file has `type: Relationship`
instead of `type: Note` → publication refuses it with a contextual 400 and
leaves the notebook and accepted Git state unchanged.

Proof: Add controller-boundary data cases for `Relationship`, `Readme`, and
an unknown nonblank type at the third path. First demonstrate the missing
refusal against the current implementation, then add the smallest local
eligibility check. Observe the offending path/type in the error, unchanged
notebook Readme and binding head/bundle, and no created Folder or Note through
a fresh committed transaction. Reuse existing rejection helpers where they
already own binding assertions. The existing successful three-path test owns
valid authored-content and exact-head/tree acceptance; smaller-shape tests
remain the compatibility proof.

Verification: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Execution wrap-up: Jidoka, fresh post-change-refactor agent, one coordinator
`./scripts/run.sh pnpm format:changed`, plan update, commit and push, with CI
observed asynchronously. No generated API changes are expected.

Sizing hypothesis: about five to ten active minutes for one eligibility rule
and controller rejection proof, including local refactoring. Backend-suite
runtime is an external-wait exception. If active work exceeds ten minutes,
stop and refine the remaining work rather than widening this slice.

## Assessment

One bounded correction, one rejection outcome, one outside-in proof loop.
No preparatory Structure slice is needed. Broader import support, generic
type restrictions, unrelated cleanup, and process changes are excluded.
Ready for direct execution when separately requested; changes not executed.
