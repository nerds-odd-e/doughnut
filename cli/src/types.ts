/** Styling for one **past CLI assistant message** block (Ink `Static` item). */
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
   * Optional: short prompts. Defaults to `log`.
   */
  writeCurrentPrompt?: (msg: string) => void
  /**
   * TTY only: starts a turn whose **Current prompt** block (optional **Current Stage Indicator**, separator,
   * wrapped lines) and input box are painted in the live region.
   */
  beginCurrentPrompt?: () => void
  /**
   * TTY only: invoked when interactive fetch-wait starts or finishes (see `runInteractiveFetchWait`).
   */
  onInteractiveFetchWaitChanged?: () => void
}
