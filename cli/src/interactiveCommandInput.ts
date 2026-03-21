/**
 * TTY interactive command line: multiline {@link InteractiveCommandInput.lineDraft},
 * caret, and in-memory ↑↓ recall of prior commits (newest first).
 */

/** Cap on recalled lines; same order as chat commits, trimmed non-empty only. */
export const MAX_COMMITTED_COMMANDS = 100

export type InteractiveCommandInput = {
  lineDraft: string
  /** UTF-16 code unit offset into `lineDraft` (matches JS string indexing). */
  caretOffset: number
  /** Newest commit at `[0]`; entries are `trim()`med. */
  committedCommands: string[]
  /**
   * `null` = user is editing the live draft. Otherwise index into `committedCommands`
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
  const trimmed = rawLine.trim()
  if (trimmed.length === 0) return committedCommands
  const next = [trimmed, ...committedCommands]
  if (next.length > MAX_COMMITTED_COMMANDS) next.length = MAX_COMMITTED_COMMANDS
  return next
}

function endHistoryWalk(
  state: InteractiveCommandInput
): Pick<
  InteractiveCommandInput,
  'historyWalkIndex' | 'lineDraftBeforeHistoryWalk'
> {
  return { historyWalkIndex: null, lineDraftBeforeHistoryWalk: null }
}

/** Replace the last logical line of a multiline draft; single-line draft becomes `newLastLine`. */
export function replaceLastLogicalLine(
  lineDraft: string,
  newLastLine: string
): string {
  const lines = lineDraft.split('\n')
  if (lines.length <= 1) return newLastLine
  return [...lines.slice(0, -1), newLastLine].join('\n')
}

/**
 * When recalling a committed line into the box during ↑↓ history walk, an incomplete `/…`
 * prefix that would show slash suggestions is stored without a trailing space. Append one
 * so TTY suggestion mode does not apply to the recalled draft (arrows stay on history).
 */
export function normalizeRecalledLineDraftForSlashSuggestionExit(
  lineDraft: string,
  hasIncompleteSlashSuggestions: boolean
): string {
  if (!hasIncompleteSlashSuggestions) return lineDraft
  const lines = lineDraft.split('\n')
  const last = lines[lines.length - 1] ?? ''
  return replaceLastLogicalLine(lineDraft, `${last} `)
}

function lineDraftAppliedFromHistory(
  recalled: string,
  lineHasIncompleteSlashSuggestions: (lineDraft: string) => boolean
): string {
  return normalizeRecalledLineDraftForSlashSuggestionExit(
    recalled,
    lineHasIncompleteSlashSuggestions(recalled)
  )
}

/** After Esc when the last line is exactly `/`: clear one-line draft, or drop the last line. */
export function lineDraftAfterEscapingBareSlash(lineDraft: string): string {
  const lines = lineDraft.split('\n')
  const last = lines[lines.length - 1] ?? ''
  if (last !== '/') return lineDraft
  if (lines.length === 1) return ''
  return lines.slice(0, -1).join('\n')
}

/** Esc on a bare `/` prefix: clear or drop last line; ends any history walk; caret at end. */
export function afterBareSlashEscape(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const lineDraft = lineDraftAfterEscapingBareSlash(state.lineDraft)
  return {
    ...state,
    ...endHistoryWalk(state),
    lineDraft,
    caretOffset: lineDraft.length,
  }
}

/** Tab completion or Enter-pick: new last line, caret at end, any history walk ends. */
export function applyLastLineEdit(
  state: InteractiveCommandInput,
  newLastLine: string
): InteractiveCommandInput {
  const lineDraft = replaceLastLogicalLine(state.lineDraft, newLastLine)
  return {
    ...state,
    ...endHistoryWalk(state),
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
    ...endHistoryWalk(state),
  }
}

/**
 * Whether the TTY should move slash-command suggestion highlight on ↑/↓ instead of
 * {@link onArrowUp} / {@link onArrowDown}. Slash cycling applies only while editing the live
 * draft (`historyWalkIndex === null`). A caret not at column 0 (for ↑) or not at
 * `lineDraft.length` (for ↓) takes precedence — first ↑ goes home, first ↓ goes to end.
 */
export function ttyArrowKeyUsesSlashSuggestionCycle(
  key: 'up' | 'down',
  state: Pick<
    InteractiveCommandInput,
    'historyWalkIndex' | 'caretOffset' | 'lineDraft'
  >,
  suggestionsDismissed: boolean,
  lineDraftHasSlashSuggestions: boolean
): boolean {
  if (suggestionsDismissed || !lineDraftHasSlashSuggestions) return false
  if (state.historyWalkIndex !== null) return false
  if (key === 'up') return state.caretOffset === 0
  return state.caretOffset === state.lineDraft.length
}

export function onArrowUp(
  state: InteractiveCommandInput,
  lineHasIncompleteSlashSuggestions: (lineDraft: string) => boolean = () =>
    false
): InteractiveCommandInput {
  const { lineDraft, caretOffset, committedCommands, historyWalkIndex } = state
  if (historyWalkIndex !== null) {
    const nextIdx = historyWalkIndex + 1
    if (nextIdx >= committedCommands.length) return state
    const line = lineDraftAppliedFromHistory(
      committedCommands[nextIdx]!,
      lineHasIncompleteSlashSuggestions
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
  const line = lineDraftAppliedFromHistory(
    committedCommands[0]!,
    lineHasIncompleteSlashSuggestions
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
  lineHasIncompleteSlashSuggestions: (lineDraft: string) => boolean = () =>
    false
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
      const line = lineDraftAppliedFromHistory(
        committedCommands[nextIdx]!,
        lineHasIncompleteSlashSuggestions
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
      ...endHistoryWalk(state),
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
    : { ...state, ...endHistoryWalk(state) }
}

export function insertIntoDraft(
  state: InteractiveCommandInput,
  text: string
): InteractiveCommandInput {
  const s = stateForTypingEdit(state)
  const { lineDraft, caretOffset } = s
  const nextDraft =
    lineDraft.slice(0, caretOffset) + text + lineDraft.slice(caretOffset)
  return {
    ...s,
    lineDraft: nextDraft,
    caretOffset: caretOffset + text.length,
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

export function caretToDraftStart(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  return { ...state, caretOffset: 0 }
}

export function caretToDraftEnd(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  return { ...state, caretOffset: state.lineDraft.length }
}
