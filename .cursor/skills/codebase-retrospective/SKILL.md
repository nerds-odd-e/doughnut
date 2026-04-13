---
name: codebase-retrospective
description: >-
  Retrospective on what made a task unexpectedly difficult.
  Triggered by the 5-minute timer hook. Reviews your own session
  to identify codebase friction and either fixes it or proposes improvements.
  Triggers on: 5-minute hook, slow progress, friction, hard to find, confusing.
---

# Codebase Retrospective — Why Was This Hard?

## When to use

- The 5-minute timer hook fires (you have been working 5+ minutes without finishing).
- You notice repeated confusion, wrong turns, or wasted searches in your session.

## Procedure

### 1. Introspect on your session

Review your own conversation history. Identify **specific moments** where you lost time:

- Searched for something and couldn't find it?
- Misunderstood what code does because of naming?
- Had to read many files to understand one concept?
- Waited a long time for a test or build?
- Couldn't tell which file/class was responsible for a behavior?

### 2. Categorize the friction

For each friction point, classify it:

| Category | Examples |
|----------|---------|
| **Organization / naming** | Misleading file name, concept split across distant files, unclear module boundaries |
| **Missing abstraction** | Duplicated logic, no shared helper for a common pattern, leaky internal details |
| **Missing / stale documentation** | No doc for a non-obvious convention, outdated comments, missing CLAUDE.md guidance |
| **Slow feedback** | Slow test suite, long build, manual steps that could be scripted |

### 3. Decide: fix or propose

**Prefer improvements in this order** (strongest to weakest):

1. **Self-explaining code** — Rename a variable, function, file, or module so the intent is obvious without comments. Extract a well-named helper. Move code so related things are together. This is almost always better than adding documentation.
2. **Automated protection** — Add a test, a lint rule, a script, or a CI check that catches the problem mechanically. A test that fails when the convention is broken is worth more than a paragraph explaining the convention.
3. **Documentation** — Only when code structure alone cannot convey the information (e.g., non-obvious environment setup, cross-repo conventions, architectural rationale). Even then, keep it minimal and close to where it's needed.

**Never** add redundant comments or docs that just repeat what clear code already says.

For **each** friction point:

- **Fix directly** if: the improvement is small (rename, move, extract helper, add a targeted test, add a script alias) and won't break existing tests or workflows. Do it now as part of your current work.
- **Propose to the developer** if: the improvement is structural (extract a module, reorganize a directory, add a new abstraction, speed up a test suite). Write a short description in your response.

### 4. Report

After categorizing and acting, **tell the developer**:

1. What friction you hit (concrete examples from your session).
2. What you fixed directly (if anything).
3. What you propose they consider (if anything structural).

Then **continue working** on the original task. This retrospective should take 1–2 minutes, not derail the session.

## Important

- This is about **codebase quality**, not task planning. If the task itself is too big, that's the phased-planning skill's job (10-minute hook).
- Be specific. "The code was confusing" is not useful. "I searched for the reading progress logic in 4 files before finding it in `BookReadingContent.vue` because the name doesn't suggest it handles progress tracking" is useful.
- Only propose improvements that would have **actually saved you time** in this session.
