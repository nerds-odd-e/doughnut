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
  notebookTitle: string
  /** `Spell: …` with markdown rendered for terminal (may include SGR). */
  spellLineRenderedForTerminal: string
  /** TTY: empty submit — show only this reprompt line until the next valid answer attempt. */
  ttyRepromptLine?: string
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

/**
 * Styling for one **past CLI assistant message** block (Ink `Static` item).
 * `userNotice` = user cancelled a wait or left a picker — not an application failure.
 */
export type CliAssistantMessageTone = 'plain' | 'error' | 'userNotice'

/** One **past user message** in the transcript (what they typed; masked before append when required). */
export type PastUserMessage = { role: 'user'; content: string }

/** One **past CLI assistant message** in the transcript (command output lines). */
export type PastCliAssistantMessage = {
  role: 'cli-assistant'
  lines: readonly string[]
  tone?: CliAssistantMessageTone
}

export type PastMessage = PastUserMessage | PastCliAssistantMessage

/** Ordered transcript (Ink `Static` items; append-only via `shell/pastMessagesModel.ts`). */
export type PastMessages = PastMessage[]

/** Slash-command token-picker modes for `/list-access-token` and related TTY flows. */
export type AccessTokenPickerAction =
  | 'set-default'
  | 'remove'
  | 'remove-completely'

/**
 * One access-token picker command: **Current Stage Indicator** label, optional wrapped **Current prompt**
 * under the band (`shell/tokenListCommands.ts` → `TOKEN_LIST_COMMANDS`).
 */
export interface AccessTokenPickerCommandConfig {
  action: AccessTokenPickerAction
  stageIndicator: string
  currentPrompt?: string
}

export type OutputAdapter = {
  log: (msg: string) => void
  logError: (err: unknown) => void
  /** Optional: user-facing notice (e.g. cancelled wait); TTY paints as distinct tone on CLI assistant messages. */
  logUserNotice?: (msg: string) => void
  /**
   * Optional: short prompts (e.g. "Please answer y or n"). For non-TTY MCQ recall, notebook line then stem
   * are sent here; numbered choices and the "Enter your choice" line use `log`. Defaults to log.
   * On TTY, MCQ stem and notebook line are part of the **Current prompt** live region (above the input box),
   * not **Current guidance**; recall loading uses a separate **Current Stage Indicator** line there.
   */
  writeCurrentPrompt?: (msg: string) => void
  /**
   * TTY only: starts a turn whose **Current prompt** block (optional **Current Stage Indicator**, separator,
   * wrapped lines) and input box are painted in the live region. When unset, non-TTY MCQ uses `writeCurrentPrompt` / `log` instead.
   */
  beginCurrentPrompt?: () => void
  /**
   * TTY only: invoked when interactive fetch-wait starts or finishes (see `runInteractiveFetchWait`).
   * On start: repaint live region (Ink `Spinner` animates in `FetchWaitDisplay`). On end: discard line draft typed during the
   * grey disabled input box, reset `/` command-picker highlight state, repaint.
   */
  onInteractiveFetchWaitChanged?: () => void
}
