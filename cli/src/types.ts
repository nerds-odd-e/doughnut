/** User-submitted input in the prompt (what they typed). */
export type ChatHistoryInputEntry = { type: 'input'; content: string }
/** Command output lines (what was displayed in response). */
export type ChatHistoryOutputEntry = {
  type: 'output'
  lines: readonly string[]
}
export type ChatHistoryEntry = ChatHistoryInputEntry | ChatHistoryOutputEntry
/** Ordered log of user inputs and command outputs for re-render on resize. */
export type ChatHistory = ChatHistoryEntry[]

export type OutputAdapter = {
  log: (msg: string) => void
  logError: (err: unknown) => void
  /** Optional: for Current Prompt hints (e.g. "Please answer y or n"). Defaults to log. */
  writeCurrentPrompt?: (msg: string) => void
  /** Optional: write green separator before first status in a prompt turn. TTY only. */
  beginCurrentPrompt?: () => void
  /** Optional: for /clear and resize. TTY provides a callback that clears and redraws. */
  clearAndRedraw?: () => void
}
