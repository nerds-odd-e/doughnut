---
name: bug-fixing
description: >-
  Fix bugs with a test-first workflow. Use when the user reports a bug, asks to
  fix a defect, or describes unexpected behavior. Triggers on: bug, fix, defect,
  broken, regression, not working, unexpected behavior.
---

# Bug-Fixing Workflow

## 1. Choose test level

- If the bug matches an **existing E2E scenario** (same feature, same level of user interaction), extend or add an E2E test.
- Otherwise, use a **unit/integration test** that drives a **high-level entry point** (controller, mounted component, CLI `run`/`runInteractive`) — not an internal helper.

## 2. Write a failing test that reproduces the bug

- **Minimum test** — one test (or the smallest addition to an existing test) that covers the bug. No duplicate coverage.
- **Assert observable output** — HTTP response, DOM text, terminal output, exit code — not internal state.
- **Prefer expected-vs-actual** over boolean (`expect(actual).toEqual(expected)`, not `expect(condition).toBe(true)`). The diff should tell the reader *what* went wrong.
- **Guard against false positives** — the assertion must fail *only* when the bug is present. If a weaker assertion would also pass on buggy code, tighten it.

## 3. Confirm the failure

- **Run the test** and verify it **fails**.
- Verify the failure is for the **right reason** (the bug, not a typo or env issue).
- If the failure message is unclear, **improve the assertion or message** before proceeding. "Educational" = a human or AI can quickly locate the problem from the output. It does not mean verbose.

## 4. Fix the bug

- Implement the **smallest change** that makes the test pass.
- No dead code — remove anything left over from debugging.

## 5. Confirm green

- Run the **new test** and confirm it passes.
- Run **related tests** (same file / same feature) and confirm nothing else broke.
