---
name: codebase-retrospective
description: >-
  Retrospective on what made a task unexpectedly difficult.
  Reviews your own session to identify codebase friction and either fixes it
  or proposes improvements.
  Triggers on: slow progress, friction, hard to find, confusing, discoverability.
---

<objective>
Identify codebase friction from your current session and either fix it directly
or propose structural improvements — then continue the original task.

Purpose: discoverability and code quality, not task planning (use
**phased-planning** when the task itself is too large).

Output: Short friction report ending with `## RETROSPECTIVE COMPLETE`, then
resume the original work.
</objective>

<context>
**When to run:**

- You notice repeated confusion, wrong turns, or wasted searches in your session.
- The developer asks for a retrospective on friction or discoverability.

**This is about codebase quality**, not task planning. If the task itself is too
big, use **phased-planning**.

Take 1–2 minutes — do not derail the session.
</context>

<process>

<step name="introspect">
Review your conversation history. Identify **specific moments** where you lost time:

- Searched for something and couldn't find it?
- Misunderstood code because of naming?
- Had to read many files to understand one concept?
- Waited a long time for a test or build?
- Couldn't tell which file/class was responsible for a behavior?
</step>

<step name="categorize">
For each friction point, classify it:

| Category | Examples |
|----------|----------|
| **Organization / naming** | Misleading file name, concept split across distant files, unclear module boundaries |
| **Missing abstraction** | Duplicated logic, no shared helper, leaky internal details |
| **Missing / stale documentation** | No doc for non-obvious convention, outdated comments, missing CLAUDE.md guidance |
| **Slow feedback** | Slow test suite, long build, manual steps that could be scripted |
</step>

<step name="fix_or_propose">
**Prefer improvements in this order** (strongest to weakest):

1. **Self-explaining code** — Rename, extract well-named helper, move related code
   together. Almost always better than documentation.
2. **Automated protection** — Test, lint rule, script, or CI check that catches the
   problem mechanically.
3. **Documentation** — Only when code structure alone cannot convey the information
   (env setup, cross-repo conventions, architectural rationale). Keep minimal and
   close to where needed.

**Never** add redundant comments or docs that repeat what clear code already says.

For **each** friction point:

- **Fix directly** if: small improvement (rename, move, extract helper, targeted
  test, script alias) and won't break existing tests or workflows. Do it now.
- **Propose to the developer** if: structural (extract module, reorganize directory,
  new abstraction, speed up test suite). Write a short description in your response.

Be specific. "The code was confusing" is not useful. "I searched for reading progress
logic in 4 files before finding `BookReadingContent.vue` because the name doesn't
suggest progress tracking" is useful.

Only propose improvements that would have **actually saved you time** in this session.
</step>

<step name="report_and_continue">
Tell the developer:

1. What friction you hit (concrete examples from your session).
2. What you fixed directly (if anything).
3. What you propose they consider (if anything structural).

Then **continue working** on the original task.
</step>

</process>

<success_criteria>
- Specific friction points identified (not vague complaints)
- Each point categorized and addressed (fix or propose)
- Session continues on original task after report
- Final output includes `## RETROSPECTIVE COMPLETE`
</success_criteria>

<output>
Short report to the developer:

1. Friction points (concrete examples).
2. Direct fixes applied (if any).
3. Structural proposals (if any).

```
## RETROSPECTIVE COMPLETE
```

Then resume the original task.
</output>

<out_of_scope>
- Do not replan the task (use phased-planning for that).
- Do not spend more than 1–2 minutes on the retrospective.
- Do not add documentation that merely repeats clear code.
</out_of_scope>
