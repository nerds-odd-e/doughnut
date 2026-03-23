import type {
  MemoryTrackerLite,
  MultipleChoicesQuestion,
  RecallPrompt,
} from 'doughnut-api'

/** Choice strings from a recall multiple-choice prompt (API payload; may embed line breaks). */
export type RecallMcqChoiceTexts = Readonly<
  MultipleChoicesQuestion['f1__choices']
>

/** User is answering a multiple-choice recall question (choices in Current guidance; TTY stem in Current prompt). */
export type McqRecallPending = {
  recallPromptId: RecallPrompt['id']
  choices: RecallMcqChoiceTexts
  /** Stem with markdown applied for the terminal (may include ANSI SGR). */
  stemRenderedForTerminal: string
  /** Resolved notebook name; first Current prompt line is emoji + this title. */
  notebookTitle: string
  shownAt: number
}

/** User is answering the spelling variant of a recall prompt. */
export type SpellingRecallPending = {
  recallPromptId: RecallPrompt['id']
  type: 'spelling'
  shownAt: number
}

/** Just-review step: user answers y/n on whether they remember the note. */
export type RecallJustReviewPending = Required<
  Pick<MemoryTrackerLite, 'memoryTrackerId'>
>

/** What recall is waiting for next, if anything. */
export type PendingRecallAnswer =
  | RecallJustReviewPending
  | McqRecallPending
  | SpellingRecallPending
  | null

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
  /**
   * Optional: short prompts (e.g. "Please answer y or n"). For non-TTY MCQ recall, notebook line then stem
   * are sent here; numbered choices and the "Enter your choice" line use `log`. Defaults to log.
   * On TTY, MCQ stem and notebook line are part of the **Current prompt** live region (above the input box),
   * not **Current guidance**; recall loading uses a separate **Current Stage Indicator** line there.
   */
  writeCurrentPrompt?: (msg: string) => void
  /**
   * TTY only: green separator before the first Current prompt line in a turn.
   * When set, recall MCQ is painted entirely in the live region (stem + separator + box), not via grey `writeCurrentPrompt` lines.
   */
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
