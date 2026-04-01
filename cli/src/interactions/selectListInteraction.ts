/**
 * Policy for list selection on the TTY (e.g. access-token picker): map Ink input →
 * move highlight / submit index / abort.
 */

export function cycleListSelectionIndex(
  current: number,
  delta: number,
  length: number
): number {
  return (current + delta + length) % length
}

type SelectListDraftPolicy =
  | {
      kind: 'slash-and-number-or-highlight'
      choiceCount: number
    }
  | { kind: 'highlight-only' }

type SelectListEscapePolicy = 'abort-list' | 'signal-escape'

type SelectListKeyEvent = {
  keyName: string | undefined
  str: string | undefined
  ctrl: boolean
  meta: boolean
  shift: boolean
  lineDraft: string
  submitPressed: boolean
}

type SelectListKeyDispatchResult =
  | { result: 'move-highlight'; delta: 1 | -1 }
  | { result: 'submit-with-line'; lineForProcessInput: string }
  | { result: 'submit-highlight-index'; index: number }
  | { result: 'escape-signaled' }
  | { result: 'edit-backspace' }
  | { result: 'edit-char'; char: string }
  | { result: 'abort-highlight-only-list' }
  | { result: 'redraw' }

type InkLikeKey = {
  escape?: boolean
  name?: string
  return?: boolean
  upArrow?: boolean
  downArrow?: boolean
  backspace?: boolean
  delete?: boolean
  ctrl?: boolean
  meta?: boolean
  shift?: boolean
}

export function selectListSubmitLineForSlashAndNumber(
  trimmedDraft: string,
  choiceCount: number,
  selectedIndex: number
): string {
  if (trimmedDraft === '/stop') return '/stop'
  if (trimmedDraft === '/contest') return '/contest'
  if (trimmedDraft === '') return String(selectedIndex + 1)
  const n = Number.parseInt(trimmedDraft, 10)
  if (n >= 1 && n <= choiceCount) return String(n)
  return trimmedDraft
}

/** 0-based choice index from a submit line produced by the slash-and-number policy, or null if not a numeric choice (/stop, /contest, invalid). */
export function choiceIndexFromSelectListSubmitLine(
  line: string,
  choiceCount: number
): number | null {
  const t = line.trim()
  if (t === '/stop' || t === '/contest') return null
  const n = Number.parseInt(t, 10)
  if (!Number.isFinite(n) || n < 1 || n > choiceCount) return null
  return n - 1
}

/** True when Enter submitted a line that is not /stop, /contest, or a valid 1-based choice index. */
export function selectListSubmitLineIsInvalidChoice(
  line: string,
  choiceCount: number
): boolean {
  const t = line.trim()
  if (t === '/stop' || t === '/contest') return false
  return choiceIndexFromSelectListSubmitLine(t, choiceCount) === null
}

export type SelectListInkHandlers = {
  readonly onSetHighlightIndex: (index: number) => void
  readonly onSubmitHighlightIndex: (index: number) => void
  readonly onSubmitWithLine?: (trimmedLine: string) => void
  readonly onInvalidSelectListSubmitLine?: () => void
  readonly onEscapeSignaled?: () => void
  readonly onAbortHighlightOnlyList?: () => void
  readonly onEditBackspace?: () => void
  readonly onEditChar?: (char: string) => void
  readonly onRedraw?: () => void
  readonly onOtherDispatch?: () => void
}

export function handleSelectListInkKey(
  input: string,
  key: InkLikeKey,
  lineDraft: string,
  highlightIndex: number,
  listLength: number,
  draftPolicy: SelectListDraftPolicy,
  escapePolicy: SelectListEscapePolicy,
  handlers: SelectListInkHandlers
): void {
  const ev = selectListKeyEventFromInk(input, key, lineDraft)
  const d = dispatchSelectListKey(ev, highlightIndex, draftPolicy, escapePolicy)

  switch (d.result) {
    case 'move-highlight':
      handlers.onSetHighlightIndex(
        cycleListSelectionIndex(highlightIndex, d.delta, listLength)
      )
      return
    case 'submit-highlight-index':
      handlers.onSubmitHighlightIndex(d.index)
      return
    case 'submit-with-line': {
      const line = d.lineForProcessInput.trim()
      if (draftPolicy.kind === 'slash-and-number-or-highlight') {
        if (
          handlers.onInvalidSelectListSubmitLine !== undefined &&
          selectListSubmitLineIsInvalidChoice(line, draftPolicy.choiceCount)
        ) {
          handlers.onInvalidSelectListSubmitLine()
          return
        }
      }
      if (handlers.onSubmitWithLine !== undefined) {
        handlers.onSubmitWithLine(line)
        return
      }
      handlers.onOtherDispatch?.()
      return
    }
    case 'escape-signaled':
      handlers.onEscapeSignaled?.()
      return
    case 'edit-backspace':
      if (handlers.onEditBackspace !== undefined) {
        handlers.onEditBackspace()
        return
      }
      handlers.onOtherDispatch?.()
      return
    case 'edit-char':
      if (handlers.onEditChar !== undefined) {
        handlers.onEditChar(d.char)
        return
      }
      handlers.onOtherDispatch?.()
      return
    case 'redraw':
      handlers.onRedraw?.()
      return
    case 'abort-highlight-only-list':
      handlers.onAbortHighlightOnlyList?.()
      return
    default: {
      const _exhaustive: never = d
      return _exhaustive
    }
  }
}

function dispatchSelectListKey(
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

export function selectListKeyEventFromInk(
  input: string,
  key: InkLikeKey,
  lineDraft: string
): SelectListKeyEvent {
  const submitPressed = !!(key.return || input === '\n' || input === '\r')
  const isEscape = key.escape || key.name === 'escape' || input === '\u001b'
  let keyName: string | undefined
  if (isEscape) keyName = 'escape'
  else if (key.upArrow) keyName = 'up'
  else if (key.downArrow) keyName = 'down'
  else if (key.backspace || key.delete) keyName = 'backspace'

  const isNavigationOrEditKey =
    isEscape ||
    key.return ||
    key.backspace ||
    key.delete ||
    key.upArrow ||
    key.downArrow
  const bareLineEnding = input === '\n' || input === '\r'
  let str: string | undefined
  if (
    !isNavigationOrEditKey &&
    input.length > 0 &&
    !key.ctrl &&
    !key.meta &&
    !bareLineEnding
  ) {
    str = input
  }

  return {
    keyName,
    str,
    ctrl: !!key.ctrl,
    meta: !!key.meta,
    shift: !!key.shift,
    lineDraft,
    submitPressed,
  }
}
