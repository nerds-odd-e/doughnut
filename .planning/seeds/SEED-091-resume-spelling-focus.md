# Resume spelling questions ready for typing

<a id="story-1"></a>

### Story 1: Focus the spelling answer after Resume

**Identity:** SEED-091#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** A learner returning to a spelling question with the main menu's Resume button can immediately type an answer.

**Scope:** Diagnose the reported missing input focus, reproduce it through the existing recall journey, and make a bounded repair under the bug-fixing workflow's planless authority and ten-minute repair limit.

**Expectations:** Starting a spelling question, leaving it for the main menu, and clicking Resume focuses the spelling answer input.

**Reported actual:** After Resume the answer input does not have focus.

**Evidence and uncertainty:** Reported by the developer on 2026-10-28. Cause and automated reproduction remain to be established.
