/**
 * TTY interactive command line: single-line {@link InteractiveCommandInput.lineDraft},
 * caret, and in-memory ↑↓ recall of prior commits (newest first). Newlines from paste are
 * collapsed to spaces; Shift+Enter does not insert a newline.
 */

/** Cap on recalled lines; same order as chat commits, trimmed non-empty only. */
export const MAX_COMMITTED_COMMANDS = 100

/** Collapse CR/LF to spaces so the draft stays one logical line. */
export function singleLineCommandDraft(s: string): string {
  return s.replace(/\r\n|\r|\n/g, ' ')
}

/**
 * Whether `lineDraft` would open the TTY slash-command suggestion list (same contract as
 * `isCommandPrefixWithSuggestions` in `renderer.ts`).
 */
export type SlashSuggestionPickerApplies = (lineDraft: string) => boolean

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

/**
 * When recalling a committed line into the box during ↑↓ history walk, an incomplete `/…`
 * prefix that would show slash suggestions is stored without a trailing space. Append one
 * so TTY suggestion mode does not apply to the recalled draft (arrows stay on history).
 */
export function normalizeRecalledLineDraftForSlashSuggestionExit(
  lineDraft: string,
  slashPickerWouldApplyToRecalledLine: boolean
): string {
  const d = singleLineCommandDraft(lineDraft)
  if (!slashPickerWouldApplyToRecalledLine) return d
  return `${d} `
}

/** Recalled committed line as it should appear in the input box (exit slash picker when needed). */
function recalledDraftForInputBox(
  recalled: string,
  slashPickerApplies: SlashSuggestionPickerApplies
): string {
  const one = singleLineCommandDraft(recalled)
  return normalizeRecalledLineDraftForSlashSuggestionExit(
    one,
    slashPickerApplies(one)
  )
}

/** Esc on a bare `/` prefix: clear the draft; otherwise normalize to a single line. */
export function lineDraftAfterEscapingBareSlash(lineDraft: string): string {
  const one = singleLineCommandDraft(lineDraft)
  return one === '/' ? '' : one
}

/** Esc on a bare `/` prefix: clear or drop last line; ends any history walk; caret at end. */
export function afterBareSlashEscape(
  state: InteractiveCommandInput
): InteractiveCommandInput {
  const lineDraft = lineDraftAfterEscapingBareSlash(state.lineDraft)
  return {
    ...state,
    ...endHistoryWalk(),
    lineDraft,
    caretOffset: lineDraft.length,
  }
}

/** Tab completion or Enter-pick: new draft, caret at end, any history walk ends. */
export function applyLastLineEdit(
  state: InteractiveCommandInput,
  newLine: string
): InteractiveCommandInput {
  const lineDraft = singleLineCommandDraft(newLine)
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
  slashSuggestionPickerVisibleForDraft: boolean
): boolean {
  if (suggestionsDismissed || !slashSuggestionPickerVisibleForDraft)
    return false
  if (state.historyWalkIndex !== null) return false
  if (key === 'up') return state.caretOffset === 0
  return state.caretOffset === state.lineDraft.length
}

export function onArrowUp(
  state: InteractiveCommandInput,
  slashPickerApplies: SlashSuggestionPickerApplies = () => false
): InteractiveCommandInput {
  const { lineDraft, caretOffset, committedCommands, historyWalkIndex } = state
  if (historyWalkIndex !== null) {
    const nextIdx = historyWalkIndex + 1
    if (nextIdx >= committedCommands.length) return state
    const line = recalledDraftForInputBox(
      committedCommands[nextIdx]!,
      slashPickerApplies
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
  const line = recalledDraftForInputBox(
    committedCommands[0]!,
    slashPickerApplies
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
  slashPickerApplies: SlashSuggestionPickerApplies = () => false
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
      const line = recalledDraftForInputBox(
        committedCommands[nextIdx]!,
        slashPickerApplies
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
