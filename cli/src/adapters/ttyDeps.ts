import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import type { RecallStopConfirmInkModel } from '../interactions/recallYesNo.js'
import type { PlaceholderContext } from '../renderer.js'
import type { AccessTokenPickerCommandConfig, OutputAdapter } from '../types.js'

export interface TTYDeps {
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
  isNumberedChoiceListActive: () => boolean
  getNumberedChoiceListChoices: () => readonly string[] | null
  getNumberedChoiceListCurrentPromptWrappedLines: (
    width: number
  ) => string[] | null
  usesSessionYesNoInputChrome: (inTokenList: boolean) => boolean
  getDefaultTokenLabel: () => AccessTokenLabel | undefined
  listAccessTokens: () => AccessTokenEntry[]
  removeAccessToken: (label: AccessTokenLabel) => boolean
  removeAccessTokenCompletely: (
    label: AccessTokenLabel,
    signal?: AbortSignal
  ) => Promise<void>
  setDefaultTokenLabel: (label: AccessTokenLabel) => void
  TOKEN_LIST_COMMANDS: Record<string, AccessTokenPickerCommandConfig>
  getPlaceholderContext: (inTokenList: boolean) => PlaceholderContext
  getRecallSessionYesNoInkGuidanceLines: () => readonly string[]
}
