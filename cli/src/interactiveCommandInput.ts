/**
 * TTY **command line** state: single-line {@link InteractiveCommandInput.lineDraft}, caret,
 * and in-memory ↑↓ recall of **user input history** (newest first). Newlines from paste collapse to spaces.
 */

/** Cap on recalled lines; same order as commits, trimmed non-empty only. */
export const MAX_USER_INPUT_HISTORY_LINES = 100

/** Collapse CR/LF to spaces so the draft stays one logical line. */
export function singleLineCommandDraft(s: string): string {
  return s.replace(/\r\n|\r|\n/g, ' ')
}

export type InteractiveCommandInput = {
  lineDraft: string
  /** UTF-16 code unit offset into `lineDraft` (matches JS string indexing). */
  caretOffset: number
  /** Newest commit at `[0]`; entries are trimmed single lines. */
  userInputHistoryLines: string[]
  /**
   * `null` = editing the live draft. Otherwise index into `userInputHistoryLines`
   * (`0` = most recent commit, larger = older).
   */
  userInputHistoryWalkIndex: number | null
  /**
   * When {@link userInputHistoryWalkIndex} became non-null, a copy of the live `lineDraft`;
   * restored when walking forward past the newest commit.
   */
  lineDraftBeforeUserInputHistoryWalk: string | null
}

export function emptyInteractiveCommandInput(): InteractiveCommandInput {
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
  if (next.length > MAX_USER_INPUT_HISTORY_LINES)
    next.length = MAX_USER_INPUT_HISTORY_LINES
  return next
}

function endUserInputHistoryWalk(): Pick<
  InteractiveCommandInput,
  'userInputHistoryWalkIndex' | 'lineDraftBeforeUserInputHistoryWalk'
> {
  return {
    userInputHistoryWalkIndex: null,
    lineDraftBeforeUserInputHistoryWalk: null,
  }
}

function recalledDraftWithSlashPickerSuppressed(
  recalledCommittedLine: string,
  slashPickerWouldApply: boolean
): string {
  const d = singleLineCommandDraft(recalledCommittedLine)
  if (!slashPickerWouldApply) return d
  return `${d} `
}

function recalledLineForCommandBox(
  recalledCommittedLine: string,
  slashPickerWouldApplyForDraft: (draft: string) => boolean
): string {
  const one = singleLineCommandDraft(recalledCommittedLine)
  return recalledDraftWithSlashPickerSuppressed(
    one,
    slashPickerWouldApplyForDraft(one)
  )
}

function draftAfterBareSlashEscape(lineDraft: string): string {
  const one = singleLineCommandDraft(lineDraft)
  return one === '/' ? '' : one
}

/** User dismissed a lone `/` on Esc: clear draft, end user input history walk, caret at end. */
export function afterBareSlashEscape(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const lineDraft = draftAfterBareSlashEscape(state.lineDraft)
  return {
    ...state,
    ...endUserInputHistoryWalk(),
    lineDraft,
    caretOffset: lineDraft.length,
  }
}

/** Replace the whole live draft (tab complete, slash pick); ends user input history walk. */
export function replaceLiveCommandDraft(
  state: InteractiveCommandInput,
  newDraft: string
): InteractiveCommandInput {
  const lineDraft = singleLineCommandDraft(newDraft)
  return {
    ...state,
    ...endUserInputHistoryWalk(),
    lineDraft,
    caretOffset: lineDraft.length,
  }
}

export function clearLiveCommandLine(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  return {
    ...state,
    lineDraft: '',
    caretOffset: 0,
    ...endUserInputHistoryWalk(),
  }
}

/**
 * Whether ↑/↓ should move the slash suggestion highlight instead of user input history / caret.
 * Only while editing the live draft (`userInputHistoryWalkIndex === null`). First ↑ goes home, first ↓ end.
 */
export function ttyArrowKeyUsesSlashSuggestionCycle(
  key: 'up' | 'down',
  state: Pick<
    InteractiveCommandInput,
    'userInputHistoryWalkIndex' | 'caretOffset' | 'lineDraft'
  >,
  suggestionsDismissed: boolean,
  slashCompletionListVisible: boolean
): boolean {
  if (suggestionsDismissed || !slashCompletionListVisible) return false
  if (state.userInputHistoryWalkIndex !== null) return false
  if (key === 'up') return state.caretOffset === 0
  return state.caretOffset === state.lineDraft.length
}

export function onArrowUp(
  state: InteractiveCommandInput,
  slashPickerWouldApplyForDraft: (draft: string) => boolean = () => false
): InteractiveCommandInput {
  const {
    lineDraft,
    caretOffset,
    userInputHistoryLines,
    userInputHistoryWalkIndex,
  } = state
  if (userInputHistoryWalkIndex !== null) {
    const nextIdx = userInputHistoryWalkIndex + 1
    if (nextIdx >= userInputHistoryLines.length) return state
    const line = recalledLineForCommandBox(
      userInputHistoryLines[nextIdx]!,
      slashPickerWouldApplyForDraft
    )
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
  const line = recalledLineForCommandBox(
    userInputHistoryLines[0]!,
    slashPickerWouldApplyForDraft
  )
  return {
    ...state,
    lineDraftBeforeUserInputHistoryWalk: lineDraft,
    userInputHistoryWalkIndex: 0,
    lineDraft: line,
    caretOffset: 0,
  }
}

export function onArrowDown(
  state: InteractiveCommandInput,
  slashPickerWouldApplyForDraft: (draft: string) => boolean = () => false
): InteractiveCommandInput {
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
      const line = recalledLineForCommandBox(
        userInputHistoryLines[nextIdx]!,
        slashPickerWouldApplyForDraft
      )
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

function stateForTypingEdit(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  return state.userInputHistoryWalkIndex === null
    ? state
    : { ...state, ...endUserInputHistoryWalk() }
}

export function insertIntoDraft(
  state: InteractiveCommandInput,
  text: string
): InteractiveCommandInput {
  const s = stateForTypingEdit(state)
  const chunk = singleLineCommandDraft(text)
  const { lineDraft, caretOffset } = s
  const nextDraft = singleLineCommandDraft(
    lineDraft.slice(0, caretOffset) + chunk + lineDraft.slice(caretOffset)
  )
  return {
    ...s,
    lineDraft: nextDraft,
    caretOffset: caretOffset + chunk.length,
  }
}

export function deleteBeforeCaret(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const s = stateForTypingEdit(state)
  const { lineDraft, caretOffset } = s
  if (caretOffset === 0) return s
  return {
    ...s,
    lineDraft:
      lineDraft.slice(0, caretOffset - 1) + lineDraft.slice(caretOffset),
    caretOffset: caretOffset - 1,
  }
}

export function caretOneLeft(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const o = state.caretOffset
  if (o <= 0) return state
  return { ...state, caretOffset: o - 1 }
}

export function caretOneRight(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const o = state.caretOffset
  if (o >= state.lineDraft.length) return state
  return { ...state, caretOffset: o + 1 }
}
