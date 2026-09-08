# Planning scope and lifecycle

## Refine through conversation

Read the selected stories and relevant prior discussion. Reuse answers already
given; ask only questions that change understanding, with a concise proposed
answer. Do not turn refinement into a questionnaire or mandatory approval
ceremony. Mark unresolved decisions explicitly; do not present proposals as
human decisions.

For each story, establish:

- **Goal:** beneficiary, desired change, and contribution to the business goal.
  Keep the story's observable outcome distinct from the broader ambition.
- **Scope:** included behavior, material exclusions, and boundary assumptions.
- **Key examples:** concrete pre-condition → trigger → result situations that
  explain the scope. Include boundaries or exceptions when they resolve
  ambiguity; do not enumerate a complete test suite.

Prefer the smallest useful outcome. Clarify uncertain additions when possible;
otherwise exclude them and report what was considered. If exclusion prevents
the stated goal or examples from working, resolve the question before dependent
planning or implementation. Necessary implementation details are not extra
product scope; speculative generality is.

Add **UI** descriptions or sketches only when interaction or presentation needs
agreement. Add **Architecture** only for a new consequential concern; consult
[dough-adr-awareness](../../dough-adr-awareness/SKILL.md) and relevant Accepted
ADRs. Inspect existing behavior or code only to resolve a concrete question,
without turning refinement into technical planning. Omit unused optional sections.

## Update the story's home

Follow the shared
[seed format](../../dough-story-decomposition/references/seed-format.md) for
home ownership, metadata, anchors, and backlog boundaries. Expand each selected
story section with the understanding above, replacing overlapping detail.
Record only open questions that affect that story. When refining several
stories, keep each outcome and boundary separate; do not merge them into one
delivery by implication.

If no home exists, create one using that format. Do not invent parent-problem
decisions to fill it; route unresolved framing or candidate selection to
[dough-story-decomposition](../../dough-story-decomposition/SKILL.md).
Do not create a separate refinement file.

Discuss goal or scope changes with the human and keep the home story and any
active plan aligned; discovery alone does not authorize expansion. Do not
silently cancel remaining scope or change siblings. Preserve compatible work
and evidence when revising boundaries. Use the project's own locations and
workflows for executable plans, phase artifacts, and project memory. Keep
planning-only numbering out of product code, tests, and permanent documentation.

## Clean up after implementation

Keep enduring behavior in tests and product documentation, and enduring design
in code and ADRs. Once that knowledge is captured, reduce each implemented
story's refinement detail to Goal and Scope, including exclusions. Remove spent
examples, UI sketches, and architectural discussion; preserve its anchor,
completion status, and unfinished siblings. Retain still-needed detail until
the enduring knowledge has a home.
