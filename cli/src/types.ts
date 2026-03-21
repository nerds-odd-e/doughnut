/** Pending MCQ in recall: user must choose 1–n in the TTY live region. */
export type McqRecallPending = {
  recallPromptId: number
  choices: readonly string[]
  shownAt: number
}

/** User-submitted input in the prompt (what they typed). */
export type ChatHistoryInputEntry = { type: 'input'; content: string }

/**
 * TTY scrollback styling for one command’s output block.
 * `userNotice` = user cancelled a wait or left a picker — not an application failure.
 */
export type ChatHistoryOutputTone = 'plain' | 'error' | 'userNotice'

/** Command output lines (what was displayed in response). */
export type ChatHistoryOutputEntry = {
  type: 'output'
  lines: readonly string[]
  tone?: ChatHistoryOutputTone
}
export type ChatHistoryEntry = ChatHistoryInputEntry | ChatHistoryOutputEntry
/** Ordered log of user inputs and command outputs for re-render on resize. */
export type ChatHistory = ChatHistoryEntry[]

export type OutputAdapter = {
  log: (msg: string) => void
  logError: (err: unknown) => void
  /** Optional: user-facing notice (e.g. cancelled wait); TTY paints as distinct scrollback tone. */
  logUserNotice?: (msg: string) => void
  /** Optional: for Current guidance (e.g. "Please answer y or n"). Defaults to log. */
  writeCurrentPrompt?: (msg: string) => void
  /** Optional: write green separator before first Current guidance content in a turn. TTY only. */
  beginCurrentPrompt?: () => void
  /** Optional: for /clear and resize. TTY provides a callback that clears and redraws. */
  clearAndRedraw?: () => void
  /**
   * TTY only: invoked when interactive fetch-wait starts or finishes (see `runInteractiveFetchWait`).
   * On start: repaint live region and run ellipsis animation. On end: discard line draft typed during the
   * grey disabled input box, reset `/` command-picker highlight state, repaint.
   */
  onInteractiveFetchWaitChanged?: () => void
}
