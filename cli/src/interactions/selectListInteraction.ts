/**
 * Shared ↑↓ / Enter / Esc / draft handling for TTY list selection (MCQ choices, access-token list).
 * Ink-shaped: maps to future `Select` / `useInput` list behavior without domain names here.
 */

export function cycleListSelectionIndex(
  current: number,
  delta: number,
  length: number
): number {
  return (current + delta + length) % length
}

/** How Enter combines the highlighted row with the optional input draft. */
export type SelectListDraftPolicy =
  | {
      kind: 'slash-and-number-or-highlight'
      /** Number of choices (1-based indices 1..choiceCount). */
      choiceCount: number
    }
  | { kind: 'highlight-only' }

/** Esc: token-style lists abort; slash-number lists signal so the host can show a nested confirm. */
export type SelectListEscapePolicy = 'abort-list' | 'signal-escape'

export type SelectListKeyEvent = {
  keyName: string | undefined
  str: string | undefined
  ctrl: boolean
  meta: boolean
  shift: boolean
  lineDraft: string
  submitPressed: boolean
}

export type SelectListKeyDispatchResult =
  | { result: 'move-highlight'; delta: 1 | -1 }
  | { result: 'submit-with-line'; lineForProcessInput: string }
  | { result: 'submit-highlight-index'; index: number }
  | { result: 'escape-signaled' }
  | { result: 'edit-backspace' }
  | { result: 'edit-char'; char: string }
  | { result: 'abort-highlight-only-list' }
  | { result: 'redraw' }

/** Line passed to `processInput` for slash-and-number policy: `/stop`, `/contest`, typed 1..n, else highlight. */
export function selectListSubmitLineForSlashAndNumber(
  trimmedDraft: string,
  choiceCount: number,
  selectedIndex: number
): string {
  if (trimmedDraft === '/stop') return '/stop'
  if (trimmedDraft === '/contest') return '/contest'
  const n = Number.parseInt(trimmedDraft, 10)
  if (n >= 1 && n <= choiceCount) return String(n)
  return String(selectedIndex + 1)
}

export function dispatchSelectListKey(
  e: SelectListKeyEvent,
  selectedIndex: number,
  draftPolicy: SelectListDraftPolicy,
  escapePolicy: SelectListEscapePolicy
): SelectListKeyDispatchResult {
  const draftEnabled = draftPolicy.kind === 'slash-and-number-or-highlight'

  if (e.keyName === 'escape') {
    return escapePolicy === 'abort-list'
      ? { result: 'abort-highlight-only-list' }
      : { result: 'escape-signaled' }
  }

  if (e.keyName === 'up' || e.keyName === 'down') {
    const delta = e.keyName === 'up' ? (-1 as const) : (1 as const)
    return { result: 'move-highlight', delta }
  }

  if (e.submitPressed && !e.shift) {
    if (draftEnabled) {
      const trimmed = e.lineDraft.trim()
      return {
        result: 'submit-with-line',
        lineForProcessInput: selectListSubmitLineForSlashAndNumber(
          trimmed,
          draftPolicy.choiceCount,
          selectedIndex
        ),
      }
    }
    return { result: 'submit-highlight-index', index: selectedIndex }
  }

  if (e.keyName === 'backspace') {
    return draftEnabled
      ? { result: 'edit-backspace' }
      : { result: 'abort-highlight-only-list' }
  }

  if (e.str && !e.ctrl && !e.meta) {
    return draftEnabled
      ? { result: 'edit-char', char: e.str }
      : { result: 'abort-highlight-only-list' }
  }

  return draftEnabled
    ? { result: 'redraw' }
    : { result: 'abort-highlight-only-list' }
}
