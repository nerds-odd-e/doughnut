import { PLACEHOLDER_BY_CONTEXT, type PlaceholderContext } from '../renderer.js'

/**
 * Ink strip for “stop recall?” while the TTY shell is up: grey prompt lines, placeholder, and the
 * question line (also fed into the suggestion strip so layout matches {@link RecallInkConfirmPanel}).
 */
export type RecallStopConfirmInkModel = {
  promptLines: readonly string[]
  placeholder: string
  confirmQuestionLines: readonly string[]
}

export const RECALL_STOP_CONFIRM_QUESTION_LINE = 'Stop recall? (y/n)'

export function recallStopConfirmInkModel(
  visiblePlaceholder: string
): RecallStopConfirmInkModel {
  return {
    promptLines: [],
    placeholder: visiblePlaceholder,
    confirmQuestionLines: [RECALL_STOP_CONFIRM_QUESTION_LINE],
  }
}

export function recallStopConfirmInkModelForContext(
  ctx: PlaceholderContext
): RecallStopConfirmInkModel {
  return recallStopConfirmInkModel(PLACEHOLDER_BY_CONTEXT[ctx])
}

/** Parsed recall y/n from a committed line (Ink confirm dispatches `y` / `n`; words allowed). */
export type RecallYesNoLineParse = 'yes' | 'no' | 'invalid'

export const RECALL_YES_NO_REPROMPT = 'Please answer y or n'

/**
 * Load-more and just-review recall answers. Empty line is invalid — unlike the Ink panel, where
 * Enter alone can mean “no” for some confirms.
 */
export function parseRecallYesNoLine(
  trimmedLine: string
): RecallYesNoLineParse {
  if (trimmedLine === '') return 'invalid'
  const a = trimmedLine.toLowerCase()
  if (a === 'y' || a === 'yes') return 'yes'
  if (a === 'n' || a === 'no') return 'no'
  return 'invalid'
}

/** Outcome of the recall Ink y/N strip (stop-recall or in-session), consumed by the TTY adapter. */
export type RecallInkConfirmChoice =
  | { result: 'cancel' }
  | { result: 'submit-yes' }
  | { result: 'submit-no' }
