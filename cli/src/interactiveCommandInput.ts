/**
 * TTY **command line** state: single-line {@link InteractiveCommandInput.lineDraft}, caret,
 * and in-memory ↑↓ recall of prior commits (newest first). Newlines from paste collapse to spaces.
 */

/** Cap on recalled lines; same order as chat commits, trimmed non-empty only. */
export const MAX_COMMITTED_COMMANDS = 100

/** Collapse CR/LF to spaces so the draft stays one logical line. */
export function singleLineCommandDraft(s: string): string {
  return s.replace(/\r\n|\r|\n/g, ' ')
}

export type InteractiveCommandInput = {
  lineDraft: string
  /** UTF-16 code unit offset into `lineDraft` (matches JS string indexing). */
  caretOffset: number
  /** Newest commit at `[0]`; entries are trimmed single lines. */
  committedCommands: string[]
  /**
   * `null` = editing the live draft. Otherwise index into `committedCommands`
   * (`0` = most recent commit, larger = older).
   */
  historyWalkIndex: number | null
  /**
   * When {@link historyWalkIndex} became non-null, a copy of the live `lineDraft`;
   * restored when walking forward past the newest commit.
   */
  lineDraftBeforeHistoryWalk: string | null
}

export function emptyInteractiveCommandInput(): InteractiveCommandInput {
  return {
    lineDraft: '',
    caretOffset: 0,
    committedCommands: [],
    historyWalkIndex: null,
    lineDraftBeforeHistoryWalk: null,
  }
}

export function appendCommittedCommand(
  committedCommands: string[],
  rawLine: string
): string[] {
  const trimmed = singleLineCommandDraft(rawLine).trim()
  if (trimmed.length === 0) return committedCommands
  const next = [trimmed, ...committedCommands]
  if (next.length > MAX_COMMITTED_COMMANDS) next.length = MAX_COMMITTED_COMMANDS
  return next
}

function endHistoryWalk(): Pick<
  InteractiveCommandInput,
  'historyWalkIndex' | 'lineDraftBeforeHistoryWalk'
> {
  return { historyWalkIndex: null, lineDraftBeforeHistoryWalk: null }
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

/** User dismissed a lone `/` on Esc: clear draft, end history walk, caret at end. */
export function afterBareSlashEscape(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const lineDraft = draftAfterBareSlashEscape(state.lineDraft)
  return {
    ...state,
    ...endHistoryWalk(),
    lineDraft,
    caretOffset: lineDraft.length,
  }
}

/** Replace the whole live draft (tab complete, slash pick); ends history walk. */
export function replaceLiveCommandDraft(
  state: InteractiveCommandInput,
  newDraft: string
): InteractiveCommandInput {
  const lineDraft = singleLineCommandDraft(newDraft)
  return {
    ...state,
    ...endHistoryWalk(),
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
    ...endHistoryWalk(),
  }
}

/**
 * Whether ↑/↓ should move the slash suggestion highlight instead of draft history / caret.
 * Only while editing the live draft (`historyWalkIndex === null`). First ↑ goes home, first ↓ end.
 */
export function ttyArrowKeyUsesSlashSuggestionCycle(
  key: 'up' | 'down',
  state: Pick<
    InteractiveCommandInput,
    'historyWalkIndex' | 'caretOffset' | 'lineDraft'
  >,
  suggestionsDismissed: boolean,
  slashCompletionListVisible: boolean
): boolean {
  if (suggestionsDismissed || !slashCompletionListVisible) return false
  if (state.historyWalkIndex !== null) return false
  if (key === 'up') return state.caretOffset === 0
  return state.caretOffset === state.lineDraft.length
}

export function onArrowUp(
  state: InteractiveCommandInput,
  slashPickerWouldApplyForDraft: (draft: string) => boolean = () => false
): InteractiveCommandInput {
  const { lineDraft, caretOffset, committedCommands, historyWalkIndex } = state
  if (historyWalkIndex !== null) {
    const nextIdx = historyWalkIndex + 1
    if (nextIdx >= committedCommands.length) return state
    const line = recalledLineForCommandBox(
      committedCommands[nextIdx]!,
      slashPickerWouldApplyForDraft
    )
    return {
      ...state,
      historyWalkIndex: nextIdx,
      lineDraft: line,
      caretOffset: 0,
    }
  }
  if (caretOffset > 0) {
    return { ...state, caretOffset: 0 }
  }
  if (committedCommands.length === 0) return state
  const line = recalledLineForCommandBox(
    committedCommands[0]!,
    slashPickerWouldApplyForDraft
  )
  return {
    ...state,
    lineDraftBeforeHistoryWalk: lineDraft,
    historyWalkIndex: 0,
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
    committedCommands,
    historyWalkIndex,
    lineDraftBeforeHistoryWalk,
  } = state
  if (historyWalkIndex !== null) {
    if (historyWalkIndex > 0) {
      const nextIdx = historyWalkIndex - 1
      const line = recalledLineForCommandBox(
        committedCommands[nextIdx]!,
        slashPickerWouldApplyForDraft
      )
      return {
        ...state,
        historyWalkIndex: nextIdx,
        lineDraft: line,
        caretOffset: line.length,
      }
    }
    const restored = lineDraftBeforeHistoryWalk ?? ''
    return {
      ...state,
      ...endHistoryWalk(),
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
  return state.historyWalkIndex === null
    ? state
    : { ...state, ...endHistoryWalk() }
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
