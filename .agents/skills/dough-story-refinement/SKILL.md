---
name: dough-story-refinement
description: >-
  Clarifies selected stories before execution planning by establishing goal,
  scope, and key examples in each story's home seed. Adds UI or architectural
  detail only when needed. Use for selected-story refinement, not broad problem
  decomposition, candidate selection, or execution-leaf sizing.
---

# Story refinement

Build shared understanding of one selected story, or several related stories
whose boundaries need discussion. Record Goal, Scope, and Key examples in each
story's home seed.

## Choose the workflow

If the parent problem, candidate selection, or story ordering needs
reconsideration, use
[dough-story-decomposition](../dough-story-decomposition/SKILL.md).
For smaller or clearer execution leaves, use the project's execution-plan
refinement workflow on the existing plan.

Refinement alone does not authorize planning or implementation. When the user
explicitly requests execution planning, hand off one understood story to the
project's planning workflow and continue without repeating answered questions.

## Resolve required context

Identify the adopting repository root, selected story links and home seeds,
and relevant prior decisions. When a home is missing, resolve the canonical
seed directory, ID and filename conventions, required metadata, and stable
story-anchor convention. Resolve project paths from that repository, not this
skill's location.

Resolve the execution-planning workflow only for a requested handoff, and ADR
context only when the architectural concern requires it under the reference
below. If required context or a linked dependency is unavailable, name what is
missing and stop the affected activity. Do not invent project paths or decisions.

## Refine and report

Read and follow [planning scope and lifecycle](references/planning.md) for the
conversation, scope decisions, optional UI and architecture detail, home updates,
and cleanup after implementation.

Report the story links, material exclusions, and unresolved decisions.
