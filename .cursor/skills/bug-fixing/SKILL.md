---
name: bug-fixing
description: >-
  Fix bugs with a test-first workflow. Use when the user reports a bug, asks to
  fix a defect, or describes unexpected behavior. Triggers on: bug, fix, defect,
  broken, regression, not working, unexpected behavior.
---

<objective>
Fix a reported defect with a **test-first** workflow: reproduce in a failing
test, apply the smallest fix, confirm green, then hand off for refactor/commit.

Purpose: Observable-behavior fixes with minimal scope and educational failure
messages.

Output: Passing focused tests + short summary ending with `## BUG FIX COMPLETE`.
</objective>

<context>
**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …`

**Test level choice:**
- Bug matches an **existing E2E scenario** (same feature, same user interaction)
  → extend or add an E2E test.
- Otherwise → **unit/integration** test driving a **high-level entry point**
  (controller, mounted component, CLI `run`/`runInteractive`) — not an internal
  helper.

**Before commit:** run **post-change-refactor**
(`.cursor/skills/post-change-refactor/SKILL.md`) on the full uncommitted change.
</context>

<process>

<step name="locate_bug">
Identify where to change the code — guess where the bug is.
If later steps cannot confirm the failure, return here.
</step>

<step name="write_failing_test">
Write the **minimum** test that reproduces the bug (one test or smallest
addition to an existing test; no duplicate coverage).

- **Assert observable output** — HTTP response, DOM text, terminal output,
  exit code — not internal state.
- **Prefer expected-vs-actual** (`expect(actual).toEqual(expected)`, not
  `expect(condition).toBe(true)`). The diff should tell the reader *what* went
  wrong.
- **Guard against false positives** — the assertion must fail *only* when the
  bug is present. Tighten weak assertions.
- **Prefer updating over adding** — if the bug contradicts or overlaps an
  existing test, update that test instead of creating a new one.
</step>

<step name="confirm_failure">
- **Run the test** and verify it **fails**.
- Verify the failure is for the **right reason** (the bug, not a typo or env
  issue).
- If the failure message is unclear, **improve the assertion or message** before
  proceeding. "Educational" = a human or AI can quickly locate the problem from
  the output. It does not mean verbose.
- If the failure cannot be confirmed, return to `locate_bug`.
</step>

<step name="fix_bug">
Implement the **smallest change** that makes the test pass.
No dead code — remove anything left over from debugging.
</step>

<step name="confirm_green">
- Run the **new test** and confirm it passes.
- Run **related tests** (same file / same feature) and confirm nothing else
  broke.
- For Cypress E2E, use **`cypress run --spec`** for the **relevant**
  `.feature` file(s) only; do **not** run the full E2E suite unless explicitly
  requested.
</step>

<step name="refactor_cleanup">
The new test might overlap with existing ones. Simplify them.
Before commit, run **post-change-refactor** on the full uncommitted change.
</step>

</process>

<success_criteria>
- Failing test reproduced the bug for the right reason before the fix
- Smallest fix makes the new test and related tests pass
- No dead/debug code left in the change
- post-change-refactor run before commit (or delegated to caller wrap-up)
- Final output includes `## BUG FIX COMPLETE`
</success_criteria>

<output>
Report a short summary to the caller, then the completion marker:

1. Bug location and test level chosen (E2E vs unit/integration).
2. Test file(s) added or updated.
3. Fix summary (what changed).
4. Related tests run and confirmed passing.

```
## BUG FIX COMPLETE
```
</output>

<out_of_scope>
- Do not run the full E2E suite unless explicitly requested.
- Do not add tests that only exercise internal helpers when a high-level entry
  point is available.
- Do not skip the failing-test confirmation step.
</out_of_scope>
