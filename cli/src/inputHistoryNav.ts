/** Max entries kept for ↑↓ recall in interactive TTY (aligned with future persistence). */
export const MAX_INPUT_COMMAND_HISTORY = 100

export type InputNavState = {
  buffer: string
  cursorOffset: number
  submittedLines: string[]
  /** `null` = editing a fresh draft; `0` = newest stored line; larger = older. */
  historyBrowseIndex: number | null
  /** Snapshot of `buffer` when entering history from `null` (first ↑ at column 0). */
  historyDraftCache: string | null
}

export function createInitialInputNavState(): InputNavState {
  return {
    buffer: '',
    cursorOffset: 0,
    submittedLines: [],
    historyBrowseIndex: null,
    historyDraftCache: null,
  }
}

function clampOffset(buffer: string, offset: number): number {
  return Math.max(0, Math.min(offset, buffer.length))
}

/** Append a submitted line (trimmed, non-empty only). Newest first. */
export function pushSubmittedLine(
  submittedLines: string[],
  rawLine: string
): string[] {
  const trimmed = rawLine.trim()
  if (trimmed.length === 0) return submittedLines
  const next = [trimmed, ...submittedLines]
  if (next.length > MAX_INPUT_COMMAND_HISTORY)
    next.length = MAX_INPUT_COMMAND_HISTORY
  return next
}

export function applyArrowUp(state: InputNavState): InputNavState {
  const { buffer, cursorOffset, submittedLines, historyBrowseIndex } = state
  if (historyBrowseIndex !== null) {
    const nextIdx = historyBrowseIndex + 1
    if (nextIdx >= submittedLines.length) return state
    const line = submittedLines[nextIdx]!
    return {
      ...state,
      historyBrowseIndex: nextIdx,
      buffer: line,
      cursorOffset: 0,
    }
  }
  if (cursorOffset > 0) {
    return { ...state, cursorOffset: 0 }
  }
  if (submittedLines.length === 0) return state
  return {
    ...state,
    historyDraftCache: buffer,
    historyBrowseIndex: 0,
    buffer: submittedLines[0]!,
    cursorOffset: 0,
  }
}

export function applyArrowDown(state: InputNavState): InputNavState {
  const {
    buffer,
    cursorOffset,
    submittedLines,
    historyBrowseIndex,
    historyDraftCache,
  } = state
  if (historyBrowseIndex !== null) {
    if (historyBrowseIndex > 0) {
      const nextIdx = historyBrowseIndex - 1
      const line = submittedLines[nextIdx]!
      return {
        ...state,
        historyBrowseIndex: nextIdx,
        buffer: line,
        cursorOffset: line.length,
      }
    }
    const restored = historyDraftCache ?? ''
    return {
      ...state,
      historyBrowseIndex: null,
      historyDraftCache: null,
      buffer: restored,
      cursorOffset: restored.length,
    }
  }
  if (cursorOffset < buffer.length) {
    return { ...state, cursorOffset: buffer.length }
  }
  return state
}

function clearBrowseOnEdit(state: InputNavState): InputNavState {
  if (state.historyBrowseIndex === null) return state
  return {
    ...state,
    historyBrowseIndex: null,
    historyDraftCache: null,
  }
}

export function insertAtCursor(
  state: InputNavState,
  text: string
): InputNavState {
  const s = clearBrowseOnEdit(state)
  const { buffer, cursorOffset } = s
  const nextBuf =
    buffer.slice(0, cursorOffset) + text + buffer.slice(cursorOffset)
  return {
    ...s,
    buffer: nextBuf,
    cursorOffset: cursorOffset + text.length,
  }
}

export function deleteBackward(state: InputNavState): InputNavState {
  const s = clearBrowseOnEdit(state)
  const { buffer, cursorOffset } = s
  if (cursorOffset === 0) return s
  return {
    ...s,
    buffer: buffer.slice(0, cursorOffset - 1) + buffer.slice(cursorOffset),
    cursorOffset: cursorOffset - 1,
  }
}

export function moveCursorLeft(state: InputNavState): InputNavState {
  const o = state.cursorOffset
  if (o <= 0) return state
  return { ...state, cursorOffset: o - 1 }
}

export function moveCursorRight(state: InputNavState): InputNavState {
  const o = state.cursorOffset
  if (o >= state.buffer.length) return state
  return { ...state, cursorOffset: o + 1 }
}

export function moveCursorHome(state: InputNavState): InputNavState {
  return { ...state, cursorOffset: 0 }
}

export function moveCursorEnd(state: InputNavState): InputNavState {
  return { ...state, cursorOffset: state.buffer.length }
}

/** After external buffer replacement (tab completion, command pick), keep cursor valid. */
export function clampCursorToBuffer(state: InputNavState): InputNavState {
  return {
    ...state,
    cursorOffset: clampOffset(state.buffer, state.cursorOffset),
  }
}

export function resetLiveDraftFields(state: InputNavState): InputNavState {
  return {
    ...state,
    buffer: '',
    cursorOffset: 0,
    historyBrowseIndex: null,
    historyDraftCache: null,
  }
}
