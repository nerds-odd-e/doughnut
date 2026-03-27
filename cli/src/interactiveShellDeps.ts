import type { AccessTokenLabel } from './commands/accessToken.js'
import type { RecallStopConfirmInkModel } from './interactions/recallYesNo.js'
import type { PlaceholderContext } from './renderer.js'
import type { AccessTokenPickerCommandConfig, OutputAdapter } from './types.js'

export interface InteractiveShellDeps {
  processInput: (
    input: string,
    output?: OutputAdapter,
    interactiveUi?: boolean
  ) => Promise<boolean>
  isPendingStopConfirmation: () => boolean
  setPendingStopConfirmation: (value: boolean) => void
  isInCommandSessionSubstate: () => boolean
  exitCommandSession: () => void
  getStopConfirmationYesOutcomeLines: () => readonly string[]
  getRecallStopConfirmInkModel: (
    ctx: PlaceholderContext
  ) => RecallStopConfirmInkModel
  getNumberedChoiceListChoices: () => readonly string[] | null
  /** MCQ notebook+stem or spelling notebook+Spell line; `null` if no such recall question is pending. */
  getRecallCurrentPromptWrappedLines: (width: number) => string[] | null
  /**
   * False while a recall answer is expected (MCQ, spelling, session y/n); committed lines are not
   * copied to user input history.
   */
  shouldRecordCommittedLineInUserInputHistory: () => boolean
  getDefaultTokenLabel: () => AccessTokenLabel | undefined
  setDefaultTokenLabel: (label: AccessTokenLabel) => void
  TOKEN_LIST_COMMANDS: Record<string, AccessTokenPickerCommandConfig>
  getPlaceholderContext: (inTokenList: boolean) => PlaceholderContext
  getRecallSessionYesNoInkGuidanceLines: () => readonly string[]
}
