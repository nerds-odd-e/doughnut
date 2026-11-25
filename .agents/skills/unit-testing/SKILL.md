---
name: unit-testing
description: Shared test guidance and Donut conventions for removal proof, destructive fixtures and script fixtures. Use when writing or changing unit tests in any package.
---
# Donut test conventions

Follow [Open Dough test guidance](../dough-execute-plan/references/test-entry-rules.md)
for test boundaries, observable assertions, synchronization, and file-specific
convention selection. Use the stack skills for commands, test entry points, and
fixture and mock helpers.

For tests of Donut’s failure paths, follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md#reporting-and-tests).

## Removal proof

For a mixed change, inspect the individual assertions and observations in the
proof table, slice Proof, and implementation brief. A scope statement about
removal does not make a contradictory proof item acceptable.

- **Surviving or replacement behavior:** retain the proof of button states,
  error feedback, and other promised outcomes, and carry it through all three
  handoff forms. A negative assertion can prove a surviving business outcome,
  such as silent completion; classify by what it establishes, not its syntax.
- **Actively enforced refusal:** retain its rejection proof, including a
  read-only user's unavailable editing control. This establishes authorization
  behavior the product still promises.
- **Removed behavior or interface:** delete its assertions and the fixtures,
  helpers, and mocks used only by them. Do not replace them with assertions
  that it is absent, not called, or ignored.
- **Sweep:** record the one-time acceptance search and classified result as a
  reading in the plan, separately from surviving-behavior proof. Create no
  permanent absence test or checker.

The coordinator corrects inherited contradictions before delegation. An
implementer or refactorer encountering one skips and reports that item while
preserving valid proof; accepting a removal must not discard its surviving
behavior's tests.

## Destructive operations (hard deletes, delete migrations)

Follow [Open Dough’s destructive fixture rule](../dough-post-change-refactor/references/refactor-checks.md#write-observable-cases)
for hard deletes and delete migrations. See `MemoryTrackerDeleteControllerTest`
for a tracker fixture that reaches the `conversation → recall_prompt` edge.
`DeletableEntityFkClosureTest` guards the schema against restricting foreign
keys without fixtures. Use runtime delete tests to prove the deletion path.

While a gated migration is pending, pair it with a focused temporary test for
the placeholder and row selection. Remove that migration-only harness after
confirmed production application.

## Test-data guidance

Use [Dough MakeMe](../dough-makeme/SKILL.md) to select shared fixture guidance.
Donut's script tests keep their Bach or Node fixtures.
