---
name: unit-testing
description: Small-test style for unit tests — stable boundary, data over mocks, focused assertions, selected MakeMe guidance (all packages). Use when writing or changing unit tests in any package.
---
# "Small test" style

This repo’s automated tests are **E2E** or **unit tests** — nothing in between. Write unit tests in the **"small test"** style below.

A **"small test"** means: a fast, in-process unit test (JUnit, Vitest, Bach, …) that drives a **stable boundary** with crafted data, exercises real lower-level production code, and mocks only external dependencies. Stack commands, entry points, and tooling stay in the package skills (`frontend` / `frontend-testing`, `backend` / `backend-testing`, `cli`, `mcp-server`).

## Stable boundary

- **Drive the outermost (or domain-stable) interface.** Prefer the public surface under test — e.g. a controller, mounted component, CLI `run` / `runInteractive`, MCP tool — or an interface that is itself a deliberate, domain-stable contract (pure algorithm, validation API). Do not add a test class per internal production class, and do not widen exports only so tests can reach helpers.
- **Cover lower layers with data, not with more test surfaces.** Craft realistic, scenario-specific preconditions and fixtures so one boundary test exercises the real lower-level production routines. Prefer that over isolating each collaborator behind mocks or thin wrapper tests.
- Select fixture conventions through [test-data guidance](#test-data-guidance).
- **Do not mock unless external or exceptional.** Mock only true external dependencies (third-party APIs, network services, and the like) or extremely rare special cases. Package rules name the allowed exceptions (`OpenAIClient`, frontend `mockSdkService` for the backend HTTP API, CLI spies on `donut-api`, etc.). Prefer the real database, real in-process collaborators, and real browser rendering inside the system under test.
- **Loud failures** — Code that is allowed to fail loudly is not a unit-test subject for that failure. Policy: ADR 0006 Usage.

## Focused assertions

- **One behavior per test.** Assert only what this scenario uniquely establishes.
- **Canonical shape once.** Shared post-conditions belong in **one** test. Sibling scenarios assert only their delta.
- **Different precondition ≠ re-assert unrelated post-condition.** If outcome X is clearly independent of what changed in the setup, do not repeat X.
- Prefer asserting the **positive** signal over “not the other message/alert” when the positive already distinguishes the case.
- When a change **removes** something, the tests are those of the behavior that survives or replaces it. Write no test, assertion, fixture, or mock that observes the removed thing (not called, absent, ignored). Apply [removal proof](#removal-proof) to each proof item. Rule: principle 7 in `AGENTS.md` / `CLAUDE.md`.
- In loops (thresholds, retries, frame polls), assert the **loop result**, not the same intermediate outcome every iteration.

```java
// ❌ every related case repeats the full payload
assertFalse(result.getAnswer().getCorrect());
assertThat(result.getAnswer().getOutcome(), is(ACCIDENTAL_MATCH));
assertThat(result.getMatchedNotes(), hasSize(1));

// ✅ canonical case asserts the shape; this case only the delta
assertThat(result.getAnswer().getOutcome(), is(ACCIDENTAL_MATCH));
```

```typescript
// ❌ re-check section when the unique claim is “no link CTAs”
expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(true)
expect(wrapper.findAll('[data-testid^="wiki-link-or-relationship-to-matched-note-"]')).toHaveLength(0)

// ✅
expect(wrapper.findAll('[data-testid^="wiki-link-or-relationship-to-matched-note-"]')).toHaveLength(0)
```

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

For **hard deletes** and **delete migrations**, **fixture completeness beats minimality** — a bare target row with no children only proves row *selection*, not referential *fan-out*.

- **Hard-delete endpoints:** build a row in **every table that holds an FK into the deleted entity** (directly or via CASCADE). Example: `MemoryTrackerDeleteControllerTest` uses `makeMe.aConversation().forARecallPrompt(...)` so deleting a tracker exercises the `conversation → recall_prompt` edge, not just the tracker row.
- **Delete migrations:** same rule — seed every child table in the FK closure, not only rows the `WHERE` clause selects. While a gated migration is pending, pair it with a focused temporary test for the placeholder and row selection; remove that migration-only harness after confirmed production application.
- **Schema guard:** `DeletableEntityFkClosureTest` catches restricting FKs without fixtures; it does not replace completeness tests for runtime delete paths.

## Test-data guidance

Before arranging fixtures for ordinary authoring, extension or implicated
refactoring, follow [Dough MakeMe](../dough-makeme/SKILL.md). It selects each
actual test source through the shared entry rules, including intended new
paths, and loads the shared convention only for enabled entries. Follow its
unresolved-selection handling before convention-dependent edits.

Keep the source's established fixture practice when MakeMe is off. In
particular, script tests keep their Bach or Node fixtures. MakeMe and page
objects are independent selections; a browser-mode unit test does not acquire
page objects from its runtime. Language imports and construction/persistence
mechanics live in the stack testing skills.
