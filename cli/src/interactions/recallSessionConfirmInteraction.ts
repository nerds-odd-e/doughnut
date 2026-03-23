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

export type RecallSessionConfirmKeyEvent = {
  keyName: string | undefined
  str: string | undefined
  ctrl: boolean
  meta: boolean
  shift: boolean
  lineDraft: string
  submitPressed: boolean
}

export type RecallSessionConfirmDispatchResult =
  | { result: 'cancel' }
  | { result: 'submit-yes' }
  | { result: 'submit-no' }
  | { result: 'invalid-submit'; hint: string }
  | { result: 'edit-backspace' }
  | { result: 'edit-char'; char: string }
  | { result: 'redraw' }

export type RecallSessionConfirmEmptySubmit = 'treat-as-no' | 'treat-as-invalid'

/** Maps raw keypress to recall y/n intent; draft edits are returned for the adapter to apply with shared command-line helpers. */
export function dispatchRecallSessionConfirmKey(
  e: RecallSessionConfirmKeyEvent,
  emptySubmit: RecallSessionConfirmEmptySubmit
): RecallSessionConfirmDispatchResult {
  const emptyPolicy: RecallSessionYesNoEmptyPolicy =
    emptySubmit === 'treat-as-no' ? 'empty-is-no' : 'empty-is-invalid'
  if (e.keyName === 'escape') return { result: 'cancel' }
  if (e.submitPressed && !e.shift) {
    const parsed = parseRecallSessionYesNoSubmit(
      e.lineDraft.trim(),
      emptyPolicy
    )
    if (parsed === 'yes') return { result: 'submit-yes' }
    if (parsed === 'no') return { result: 'submit-no' }
    return { result: 'invalid-submit', hint: 'Please answer y or n' }
  }
  if (e.keyName === 'backspace') return { result: 'edit-backspace' }
  if (e.str && !e.ctrl && !e.meta) return { result: 'edit-char', char: e.str }
  return { result: 'redraw' }
}

/** Neutral aliases for TTY deps — keeps product words out of the transport adapter. */
export type SessionYesNoLineKeyEvent = RecallSessionConfirmKeyEvent
export type SessionYesNoLineDispatchResult = RecallSessionConfirmDispatchResult
export type SessionYesNoLineEmptySubmit = RecallSessionConfirmEmptySubmit
export type StopConfirmationLiveView = RecallStopConfirmViewModel
