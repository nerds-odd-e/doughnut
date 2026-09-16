---
name: dough-bug-fixing
description: >-
  Resolves a reported discrepancy, defect, or regression by gathering expected
  versus actual behavior and passing the report into bounded shared execution,
  or by placing known larger or inconclusive work first in this project's
  product backlog. Use for a bug report, defect, regression, broken or
  unexpected behavior, a mismatch between intended and actual results, or a
  larger or inconclusive bug that needs a first-priority story. Do not use for
  the word "fix" alone, a reporting-only request, or refinement-only work.
---

# Resolve a reported discrepancy

Gather the supplied report, then invoke shared execution early for investigation,
reproduction, and attempted repair unless the report is already known to be
larger than that bounded attempt. Return an evidence-backed disposition to the
coordinator. Route known larger work and incomplete or inconclusive attempts as
first-priority backlog stories. Do not invent a defect, dismiss a report for
lack of confirmation, or treat branch delivery as integration.

## Stay in request authority

A reporting-only or refinement-only request does not authorize execution or code
change. Contribute the report's expectations, evidence, gaps, and acceptance
examples to the existing artifact that request already owns, such as a seed
under [story refinement](../dough-story-refinement/SKILL.md). Stop after that
contribution.

Invoke this skill for an authorized discrepancy, defect, or regression, not
because the instruction contains the word "fix".

## Gather the report

Collect what the report already supplies and name the gaps:

- intended or expected behavior
- actual or observed behavior
- evidence (repro steps, tests, logs, revision, environment)
- remaining uncertainty (validity, cause, or scope)

Resolve this project's reported and expected behavior, relevant code and tests,
tooling wrapper, focused test commands, fixture conventions, and testing rules
from the established task and repository. If required behavior or tooling
context is missing, report the gap and stop the affected work rather than
inventing it.

## Invoke shared execution

If the report is already known to be larger than a ten-minute bounded repair,
skip this handoff and use [Route remaining work](#route-remaining-work). Do not
start an investigation attempt solely to justify that route.

Otherwise invoke [dough-execute-plan](../dough-execute-plan/SKILL.md) immediately
as one planless contextual instruction. Pass `--no-replan` and a ten-minute hard
limit. Carry the gathered expectation, actual behavior, evidence, and gaps. Do
not plan, invent a story, or start a local implement-and-refactor loop.

Debug with available knowledge as needed. Do not require a separate debugging
skill.

The contextual instruction must require:

1. **Reproduce before repair.** Treat the likely defect as a hypothesis until a
   test confirms it. Write the minimum test or smallest addition that reproduces
   the discrepancy. Prefer updating an overlapping or contradictory test over
   adding duplicate coverage.
2. **Stable observable boundary.** If the report matches an existing E2E
   scenario's feature and user interaction, extend or add that E2E test.
   Otherwise write a small test through a stable boundary such as a controller,
   mounted component, CLI entry point, or deliberate domain API. Exercise real
   lower layers with crafted data and this project's fixture helpers; mock only
   external dependencies, subject to this project's testing exceptions. Do not
   test internal helpers when a stable boundary is available or widen exports
   solely for tests.
3. **Useful expected-versus-actual assertion.** Assert observable output such as
   the response, rendered text, terminal output, or exit code. Prefer
   expected-versus-actual assertions to boolean checks so the diff identifies
   the mismatch. Tighten assertions that could pass while the discrepancy
   remains. Run the test and confirm it fails because of the reported behavior,
   not a typo or environment issue. If that failure cannot be confirmed, do not
   claim reproduction or proceed to repair.
4. **Absence needs a product contract.** An absence assertion requires a product
   reason: the current intended behavior is that the result is missing. Removal
   history alone cannot justify it. An explicit promise such as "cancellation
   creates no order" still justifies asserting that no order exists.
5. **Smallest repair and related verification.** After a confirmed failing
   reproduction, make the smallest change that passes the test. Remove debugging
   and dead code. Run the regression test and related tests in the same file or
   feature. Select only relevant E2E specs using this project's test runner. Do
   not run the full E2E suite unless explicitly requested.
6. **Shared refactoring.** Keep the existing pre-commit refactoring obligation.
   Execute-plan delivery already runs
   [dough-post-change-refactor](../dough-post-change-refactor/SKILL.md) on the
   full uncommitted change. Do not replace that step with a local refactor loop.

A supported conclusion that the reported behavior is already correct is a valid
resolution. Use execute-plan's explained-empty-change path. Do not invent a
defect to have something to repair.

## Route remaining work

Use this one rule for known larger work, an incomplete `--no-replan` return, or
an inconclusive report. Do not invent severity categories.

Do not repeat execute-plan preservation, rollback, or retry. Link the
execution-preserved evidence already written under this project's
executable-plan root (see [refine an oversized
slice](../dough-execute-plan/references/execution-decisions.md#refine-an-oversized-slice)
and [resolve execution
context](../dough-slice-planning/SKILL.md#resolve-execution-context)). Carry the
gathered expectation, actual behavior, remaining uncertainty, and acceptance
examples into the canonical story. For an inconclusive report, the story first
asks whether intended behavior is violated, then repairs a confirmed violation.

Choose the canonical home:

- Reuse an existing owning story when moving it first in **Backlog list**
  preserves that story's scope.
- Otherwise create the canonical home with
  [story refinement](../dough-story-refinement/SKILL.md) and
  [seed format](../dough-story-decomposition/references/seed-format.md).
- If promoting an existing owning story would distort broader scope, or
  ownership is ambiguous, stop. Return a Jidoka handoff that names that
  decision. Do not guess the move.

Place the story first under **Backlog list** using
[product backlog](../dough-product-backlog/SKILL.md) canonical references,
deduplication, and Taken placement. Do not list the same work twice. Do not
move or interrupt **Taken** work; leave it running and in place, and report any
contradiction with it to the coordinator.

Do not start execution of the queued work. Later refinement or planning reads
the same linked expectations, evidence, gaps, and examples; do not invent
another tracker.

If the backlog or a required canonical home cannot be identified, name the
missing context and stop rather than inventing a queue entry.

## Report the disposition

Return control to the coordinator with an evidence-backed disposition. Name the
gathered expectation, actual, evidence, and gaps, then what execution returned
or which remaining-work route was taken.

- **Repaired:** the reproduction test failed for the reported mismatch, the
  smallest change made that proof green, related verification passed, and
  shared delivery completed refactoring and branch delivery. Do not report
  that result as integrated. The coordinator invokes
  [story wrap-up](../dough-story-wrap-up/SKILL.md#integrate-committed-story-branch-mode-closure)
  for integration to the selected target (default `main`). Reporter
  confirmation on main is pending when a repair needs it; never fabricate
  that confirmation.
- **Explained no-change:** evidence shows the actual behavior matches the
  intended behavior. Resolve the report without a repair.
- **Unresolved:** validity, cause, or scope remains unconfirmed, and the report
  is not known larger and was not an incomplete or inconclusive execution
  return. Do not claim resolution or invent a queue entry.
- **Recovery:** failed delivery or shutdown remains ordinary recovery. Do not
  claim resolution.
- **Queued:** known larger work, an incomplete `--no-replan` attempt, or an
  inconclusive report now has an actionable canonical story first in the queue.
  Do not claim resolution or start that work.
- **Jidoka:** routing could not move an owning story safely. Name the
  scope-distortion or ownership decision and return it to the coordinator
  without a guessed queue entry. Do not claim resolution.

Use `## BUG REPORT RESOLVED` only for a repaired or explained-no-change
disposition after execute-plan has returned that outcome. That marker is not
main integration and not reporter confirmation. Otherwise report the gap
without the marker.
