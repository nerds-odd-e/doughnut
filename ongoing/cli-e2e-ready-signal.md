# CLI E2E ready signal

## Problem

Three separate CLI E2E failures (all passing locally, failing on GitHub Actions) share a structural root cause: the test infrastructure reverse-engineers "CLI is ready for input" from raw ANSI terminal output, maintaining its own independent understanding of the CLI's rendering. When the CLI changes its rendering — new placeholder contexts, new animation timing, new UI states — the test breaks silently and only on CI (where timing is slower and the race window is wider).

### The three cases

1. **Loading animation race** — `waitForInputBoxReady` settled during a 400ms ellipsis idle gap because the stability window (30ms) was shorter than the repaint interval. The grey loading box matched `INPUT_BOX_READY_PATTERN`. On faster local machines the HTTP call finished before the race could trigger.

2. **Unrecognised placeholder** — `INPUT_BOX_READY_PATTERN` only knew the default prompt's backtick. Recall prompts (`y or n`, `↑↓ Enter`, `type your answer`) were never matched, so the test timed out after 15s. Locally the timing was fast enough to mask this in some runs.

3. **CI pipe vs PTY divergence** — CI uses piped fallback input (`/stop`) instead of PTY ESC. The `/stop` path skips the "Stop recall? (y/n)" confirmation. The test asserted ESC-specific UI that only exists in PTY mode.

### Common cause

`INPUT_BOX_READY_PATTERN` in `cliPtyRunner.ts` is a **hand-maintained mirror** of rendering decisions in `renderer.ts`. There is no shared contract. Every new `PlaceholderContext`, every timing constant change, every rendering tweak can silently break the test.

The current ANSI-grey structural pattern (`\x1b[90m` after the box border) is better than the old placeholder-text enumeration, but it is still inference — it can match loading states, selection mode transitions, or any future UI that happens to use grey after a box border.

## Goal

Replace inference with an explicit machine-readable signal that the CLI emits when it is ready for user input. The test infrastructure detects this signal directly, with no coupling to rendering details.

## Design

### Ready marker

The CLI emits a **custom OSC (Operating System Command) escape sequence** after every render that leaves the input box ready for user input:

```
\x1b]133;A\x07
```

This is the [FinalTerm / shell integration prompt mark](https://gitlab.freedesktop.org/Per_Bothner/specifications/blob/master/proposals/semantic-prompts.md) `OSC 133 ; A ST`, widely supported by terminal emulators as "prompt start." It is invisible to users and has no visual side-effect.

The CLI writes this marker once, at the end of `drawBox`, when the input is ready (buffer is empty, no pending async operation). The marker is the CLI's declaration: "I am ready for the next input."

### Test detection

`waitForInputBoxReady` in `cliPtyRunner.ts` watches for this marker in new stdout content. No pattern matching against placeholder text, no ANSI grey inference, no stability window to tune against animation timing.

The stability window can be removed entirely for prompt readiness (the marker is emitted exactly once per ready state), though we may keep a small debounce (~50ms) to let any trailing repaints flush.

### Where the marker is emitted

In `renderer.ts` (or `ttyAdapter.ts` — wherever `drawBox` or the final render call lives), after the complete live-region paint, if the CLI is in a state where user input is accepted. The exact predicate: `buffer is empty AND no interactiveFetchWait in progress AND not in a transient animation frame`.

This is a single call site — high cohesion. The CLI owns the definition of "ready." The test just listens.

### What stays, what goes

| Current | After |
|---------|-------|
| `INPUT_BOX_READY_PATTERN` (ANSI grey regex) | Replaced by marker detection |
| `INPUT_BOX_STABLE_MS = 550` (stability window) | Removed or reduced to a small flush debounce |
| `CLI_READY_PATTERN` (`/ commands`) for initial startup | Replaced by the same marker (first occurrence = CLI is ready) |
| Placeholder-text coupling between `renderer.ts` and `cliPtyRunner.ts` | Gone — test has zero knowledge of placeholder content |

## Phases

### Phase 1: Emit the ready marker from the CLI — done

`OSC_133_INPUT_BOX_SETTLED` and `osc133WhenInputBoxSettled` live in `renderer.ts` (`InputBoxSettledForAutomation` describes the predicate). `ttyAdapter` finishes each live-region paint with `positionInputChromeCursorAndMaybeEmitSettledOsc`, using the same-frame `interactiveFetchWaitLine` from `getDisplayContent()` (no second `getInteractiveFetchWaitLine()` call).

**Test:** `cli/tests/renderer.test.ts` covers the suffix helper.

**E2E:** Unchanged — old detection still works.

### Phase 2: Detect the marker in the test infrastructure

In `cliPtyRunner.ts`:
- Replace `INPUT_BOX_READY_PATTERN` with a check for `OSC_133_INPUT_BOX_SETTLED` in new stdout content.
- Replace `CLI_READY_PATTERN` (initial startup) with the same OSC check.
- Remove `INPUT_BOX_STABLE_MS` / stability polling. Keep a small flush debounce if needed.
- Import `OSC_133_INPUT_BOX_SETTLED` from `cli/src/renderer.ts` (or a tiny shared module if import boundaries block it).

**Test:** Run all CLI E2E features locally. They should pass with the new detection. The loading-animation race and placeholder-mismatch failure modes are structurally impossible now.

### Phase 3: Verify on CI

Push and confirm GitHub Actions passes. If the small flush debounce is insufficient (unlikely), increase it — but this is now a single, well-motivated constant, not a workaround for animation timing.

## Cross-package import

`cliPtyRunner.ts` (e2e_test, runs in Cypress Node process) needs the marker constant from `cli/src/renderer.ts`. Options:

1. **Direct relative import** — `cliPtyRunner.ts` already imports from `./cliEnv`; if tsconfig allows reaching `../../cli/src/renderer`, this is simplest.
2. **Shared constants file** — Extract `OSC_133_INPUT_BOX_SETTLED` to a small file (e.g. `cli/src/readyMarker.ts`) with no transitive dependencies, import from both.
3. **Duplicate the constant** — Last resort. A single string constant duplicated in two places is low risk, but violates the "one representation" principle. If we must, add a unit test that imports both and asserts equality.

Prefer option 1 or 2. Investigate at implementation time.
