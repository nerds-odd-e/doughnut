import { PLACEHOLDER_BY_CONTEXT, type PlaceholderContext } from '../renderer.js'

/**
 * Ink-shaped view model for the stop-recall y/n step: declarative strings the TTY paints via {@link ../renderer.ts}.
 * Business layer remains responsible for when this mode is active; this module only describes what to show and how keys map to intent.
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

export function parseRecallStopConfirmSubmit(
  trimmedLine: string
): 'yes' | 'no' | 'invalid' {
  if (trimmedLine === '') return 'no'
  const a = trimmedLine.toLowerCase()
  if (a === 'y' || a === 'yes') return 'yes'
  if (a === 'n' || a === 'no') return 'no'
  return 'invalid'
}

export type RecallStopConfirmKeyEvent = {
  keyName: string | undefined
  str: string | undefined
  ctrl: boolean
  meta: boolean
  shift: boolean
  lineDraft: string
  submitPressed: boolean
}

export type RecallStopConfirmDispatchResult =
  | { result: 'cancel' }
  | { result: 'submit-yes' }
  | { result: 'submit-no' }
  | { result: 'invalid-submit'; hint: string }
  | { result: 'edit-backspace' }
  | { result: 'edit-char'; char: string }
  | { result: 'redraw' }

/** Maps raw keypress to stop-recall confirm intent; draft edits are returned for the adapter to apply with shared command-line helpers. */
export function dispatchRecallStopConfirmKey(
  e: RecallStopConfirmKeyEvent
): RecallStopConfirmDispatchResult {
  if (e.keyName === 'escape') return { result: 'cancel' }
  if (e.submitPressed && !e.shift) {
    const parsed = parseRecallStopConfirmSubmit(e.lineDraft.trim())
    if (parsed === 'yes') return { result: 'submit-yes' }
    if (parsed === 'no') return { result: 'submit-no' }
    return { result: 'invalid-submit', hint: 'Please answer y or n' }
  }
  if (e.keyName === 'backspace') return { result: 'edit-backspace' }
  if (e.str && !e.ctrl && !e.meta) return { result: 'edit-char', char: e.str }
  return { result: 'redraw' }
}
