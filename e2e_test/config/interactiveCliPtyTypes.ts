/**
 * Contract between Cypress (browser) and the Node PTY runner for interactive CLI E2E.
 * Kept free of Node-only imports so page objects can import the task name + types safely.
 */

/** Cypress `task` registered in `cliE2ePluginTasks.ts` for all interactive PTY keystrokes. */
export const INTERACTIVE_CLI_PTY_KEYSTROKE_TASK =
  'applyInteractiveCliPtyKeystroke' as const

/**
 * One logical TTY action once the CLI has visibly drawn a ready input surface.
 * Maps to exact bytes in `cliPtyRunner` — no trimming or slash detection here.
 */
export type InteractiveCliPtyKeystroke =
  | {
      kind: 'slashCommand'
      /** Full slash line as typed (e.g. `/recall`); runner writes `line + ` then `\r` in a second chunk. */
      commandLine: string
    }
  | {
      kind: 'line'
      /** One line of input; runner writes text then `\r` in a second chunk (Ink stdin). */
      text: string
    }
  | { kind: 'enter' }
  | {
      /** One UTF-16 code unit, sent alone (no Enter). For list modes that cancel on “other keys”. */
      kind: 'rawKey'
      char: string
    }
