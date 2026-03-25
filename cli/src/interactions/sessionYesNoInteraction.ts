import { PLACEHOLDER_BY_CONTEXT, type PlaceholderContext } from '../renderer.js'

/**
 * Ink-shaped view model for recall y/n steps: declarative strings the TTY paints via {@link ../renderer.ts}.
 * Stop-recall confirm uses {@link recallStopConfirmViewModelForContext}; load-more / memory y/n use grey
 * `writeCurrentPrompt` lines from the business layer instead of this guidance-only shape.
 */
export type RecallStopConfirmViewModel = {
  promptLines: string[]
  placeholder: string
  guidance: string[]
}

export const RECALL_STOP_CONFIRM_GUIDANCE_LINE = 'Stop recall? (y/n)'

export function recallStopConfirmViewModel(
  visiblePlaceholder: string
): RecallStopConfirmViewModel {
  return {
    promptLines: [],
    placeholder: visiblePlaceholder,
    guidance: [RECALL_STOP_CONFIRM_GUIDANCE_LINE],
  }
}

/** Builds the view model using the same placeholder string as the live region for the given context. */
export function recallStopConfirmViewModelForContext(
  ctx: PlaceholderContext
): RecallStopConfirmViewModel {
  return recallStopConfirmViewModel(PLACEHOLDER_BY_CONTEXT[ctx])
}

/** How an empty submitted line is interpreted for recall session y/n. */
export type RecallSessionYesNoEmptyPolicy = 'empty-is-no' | 'empty-is-invalid'

export function parseRecallSessionYesNoSubmit(
  trimmedLine: string,
  emptyPolicy: RecallSessionYesNoEmptyPolicy
): 'yes' | 'no' | 'invalid' {
  if (trimmedLine === '') {
    return emptyPolicy === 'empty-is-no' ? 'no' : 'invalid'
  }
  const a = trimmedLine.toLowerCase()
  if (a === 'y' || a === 'yes') return 'yes'
  if (a === 'n' || a === 'no') return 'no'
  return 'invalid'
}

/** Outcomes from recall TTY confirm panels (stop-recall and in-session y/n) to the adapter. */
export type SessionYesNoLineDispatchResult =
  | { result: 'cancel' }
  | { result: 'submit-yes' }
  | { result: 'submit-no' }

export type StopConfirmationLiveView = RecallStopConfirmViewModel
