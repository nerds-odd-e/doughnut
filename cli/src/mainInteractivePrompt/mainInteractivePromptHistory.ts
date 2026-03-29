/**
 * Pure command-line draft, caret, and ↑↓ user input history (newest first).
 * Single-line draft only; used by MainInteractivePrompt in this folder.
 */

const ADD_ACCESS_TOKEN = '/add-access-token'

/** Match `/add-access-token` + whitespace + non-empty argument (case-insensitive on the command). */
const ADD_ACCESS_TOKEN_WITH_SECRET = /^\/add-access-token\s+(.+)$/i

/**
 * Display-safe line before it is stored in user input history (phase 4: same for disk).
 * Unchanged lines are returned as-is (including original whitespace shape per segment).
 */
export function maskInteractiveInputLineForStorage(line: string): string {
  const segments = line.split('\n')
  let changed = false
  const out = segments.map((segment) => {
    const t = segment.trim()
    if (t.length === 0) return segment
    const m = t.match(ADD_ACCESS_TOKEN_WITH_SECRET)
    if (!m || m[1]!.trim().length === 0) return segment
    changed = true
    return `${ADD_ACCESS_TOKEN} <redacted>`
  })
  return changed ? out.join('\n') : line
}

export const MAX_USER_INPUT_HISTORY_LINES = 100

export function singleLineCommandDraft(s: string): string {
  return s.replace(/\r\n|\r|\n/g, ' ')
}

export type MainInteractivePromptHistoryState = {
  lineDraft: string
  caretOffset: number
  userInputHistoryLines: string[]
  userInputHistoryWalkIndex: number | null
  lineDraftBeforeUserInputHistoryWalk: string | null
}

export function emptyMainInteractivePromptHistoryState(): MainInteractivePromptHistoryState {
  return {
    lineDraft: '',
    caretOffset: 0,
    userInputHistoryLines: [],
    userInputHistoryWalkIndex: null,
    lineDraftBeforeUserInputHistoryWalk: null,
  }
}

export function appendUserInputHistoryLine(
  lines: string[],
  rawLine: string
): string[] {
  const trimmed = singleLineCommandDraft(rawLine).trim()
  if (trimmed.length === 0) return lines
  if (lines[0] === trimmed) return lines
  const next = [trimmed, ...lines]
  if (next.length > MAX_USER_INPUT_HISTORY_LINES) {
    next.length = MAX_USER_INPUT_HISTORY_LINES
  }
  return next
}

function endUserInputHistoryWalk(): Pick<
  MainInteractivePromptHistoryState,
  'userInputHistoryWalkIndex' | 'lineDraftBeforeUserInputHistoryWalk'
> {
  return {
    userInputHistoryWalkIndex: null,
    lineDraftBeforeUserInputHistoryWalk: null,
  }
}

/** End history walk before mutating the draft so edits apply to a normal line. */
export function exitHistoryWalkOnDraftEdit(
  state: MainInteractivePromptHistoryState
): MainInteractivePromptHistoryState {
  if (state.userInputHistoryWalkIndex === null) return state
  return { ...state, ...endUserInputHistoryWalk() }
}

export function onArrowUp(
  state: MainInteractivePromptHistoryState
): MainInteractivePromptHistoryState {
  const {
    lineDraft,
    caretOffset,
    userInputHistoryLines,
    userInputHistoryWalkIndex,
  } = state
  if (userInputHistoryWalkIndex !== null) {
    const nextIdx = userInputHistoryWalkIndex + 1
    if (nextIdx >= userInputHistoryLines.length) return state
    const line = singleLineCommandDraft(userInputHistoryLines[nextIdx]!)
    return {
      ...state,
      userInputHistoryWalkIndex: nextIdx,
      lineDraft: line,
      caretOffset: 0,
    }
  }
  if (caretOffset > 0) {
    return { ...state, caretOffset: 0 }
  }
  if (userInputHistoryLines.length === 0) return state
  const line = singleLineCommandDraft(userInputHistoryLines[0]!)
  return {
    ...state,
    lineDraftBeforeUserInputHistoryWalk: lineDraft,
    userInputHistoryWalkIndex: 0,
    lineDraft: line,
    caretOffset: 0,
  }
}

export function onArrowDown(
  state: MainInteractivePromptHistoryState
): MainInteractivePromptHistoryState {
  const {
    lineDraft,
    caretOffset,
    userInputHistoryLines,
    userInputHistoryWalkIndex,
    lineDraftBeforeUserInputHistoryWalk,
  } = state
  if (userInputHistoryWalkIndex !== null) {
    if (userInputHistoryWalkIndex > 0) {
      const nextIdx = userInputHistoryWalkIndex - 1
      const line = singleLineCommandDraft(userInputHistoryLines[nextIdx]!)
      return {
        ...state,
        userInputHistoryWalkIndex: nextIdx,
        lineDraft: line,
        caretOffset: line.length,
      }
    }
    const restored = lineDraftBeforeUserInputHistoryWalk ?? ''
    return {
      ...state,
      ...endUserInputHistoryWalk(),
      lineDraft: restored,
      caretOffset: restored.length,
    }
  }
  if (caretOffset < lineDraft.length) {
    return { ...state, caretOffset: lineDraft.length }
  }
  return state
}
