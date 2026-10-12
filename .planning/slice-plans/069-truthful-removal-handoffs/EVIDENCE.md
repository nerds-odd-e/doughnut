# Truthful removal handoff evidence

## Slice 1: historical proof-item review

Input: `git show 7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md`,
the whole historical plan, especially its mixed outside-in proof row and slice
5 Proof. Both prescribe `no [role="status"]` despite the scope citing principle
7. This review reconstructs that handoff; it does not execute the historical
change or establish universal future compliance.

Reviewed at execution base `0a0def3db215338e3e323741db85d58bf5ec50ed` with the
uncommitted slice 1 guidance. The resulting proof-table rows are:

| Promise | Proof | Slice |
| --- | --- | --- |
| Button states, feedback, and busy editing rules | Mounted spoken-title specs: idle accessible name and unpressed state; listening name, pressed state and style; converting disabled then idle; silent/empty response leaves title unchanged, idle and no toast; microphone refusal shows the existing error toast then idle; conversion failure shows its error toast, preserves title/caret, returns idle, and permits a fresh recorder; New note Submit disabled while busy and Enter inert while listening. E2E clicks by accessible names; by-eye placement reading; documentation describes these states. | 5 |
| Read-only authorization remains enforced | Mounted read-only note provides no title-editing button. | 5 |

Resulting slice Proof: mounted spoken-title specs observe all states, feedback,
title/caret preservation, fresh-recorder recovery, and busy editing rules in
the first row; E2E name, by-eye placement, and documentation observations stay.
Separately, mount a read-only note and observe that it provides no title-editing
button. Record the one-time whole-product sweep as an acceptance reading.

Resulting implementation brief: make the title microphone an icon button and
delete the status-line behavior and exclusively dependent test support
transitively. Retarget existing mounted specs and E2E helpers to accessible
button names. Carry every observation in the first proof row into the
implementation and acceptance, including silent completion and both error
paths. Separately retain the read-only authorization proof in the second row.
Perform the historical slice's whole-product sweep as a one-time acceptance
reading. Skip the inherited `no [role="status"]` assertion.

Classification: idle unpressed state, `noToastShown`, and inert Enter prove
surviving outcomes; negative syntax alone is not grounds for deletion. The
read-only control restriction proves an actively enforced authorization rule.
The status-element assertion observes the removed interface and is omitted
from all three forms. Helper deletion is implementation scope; the sweep is a
separate reading.

Implementation returned uncommitted edits in `AGENTS.md`, `CLAUDE.md`, and the
Donut-authored `unit-testing` skill. The coordinator inspected those diffs and
the historical row and slice Proof, confirmed the two principle 7 additions
match and `unit-testing#removal-proof` resolves, and accepted the bounded
classification above. `./scripts/run.sh bash scripts/check_diff_whitespace.sh`
passed (exit 0). Runtime proof was unchanged; no product suite was run for prose.

Fresh independent refactor reviewed the five changed files, found no cohesive
refactor candidate, made no edits, and returned `## REFACTOR COMPLETE` with
accepted proof unchanged. `./scripts/run.sh pnpm format:changed` passed (exit 0).
