/**
 * PTY-ready marker used by CLI E2E synchronization.
 *
 * Keep byte-identical to `INTERACTIVE_INPUT_READY_OSC` in `cli/src/renderer.ts`.
 * E2E cannot import `cli/src` directly because `e2e_test/tsconfig.json` constrains rootDir.
 */
export const INTERACTIVE_INPUT_READY_OSC =
  '\x1b]900;doughnut-interactive-input-ready\x07' as const
