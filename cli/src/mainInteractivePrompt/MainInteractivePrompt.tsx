import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput, useStdout } from 'ink'
import { getConfigDir } from '../configDir.js'
import {
  appendUserInputHistoryLine,
  exitHistoryWalkOnDraftEdit,
  maskInteractiveInputLineForStorage,
  onArrowDown,
  onArrowUp,
  type PromptHistoryState,
} from './history.js'
import {
  loadUserInputHistory,
  saveUserInputHistory,
} from './userInputHistoryFile.js'
import { cycleListSelectionIndex } from '../interactions/selectListInteraction.js'
import { GuidanceListInk } from '../guidanceListWindowInk.js'
import {
  DEFAULT_INTERACTIVE_GUIDANCE,
  effectiveSlashGuidance,
  getSlashTabCompletion,
  isBareDraftSlash,
  isSlashListArrowKey,
  normalizeInputForSlash,
  slashGuidanceForInk,
} from './slashCommandCompletion.js'

const MAIN_PROMPT_PLACEHOLDER = '`exit` to quit.'

export function MainInteractivePrompt({
  onCommittedLine,
  isActive = true,
}: {
  readonly onCommittedLine: (line: string) => void
  readonly isActive?: boolean
}) {
  const [buffer, setBuffer] = useState('')
  const [caretOffset, setCaretOffset] = useState(0)
  const [slashHighlightIndex, setSlashHighlightIndex] = useState(0)
  const [suggestionsDismissed, setSuggestionsDismissed] = useState(false)

  const isActiveRef = useRef(isActive)
  isActiveRef.current = isActive

  const bufferRef = useRef('')
  const caretRef = useRef(0)
  const slashHighlightRef = useRef(0)
  const suggestionsDismissedRef = useRef(false)
  const onCommittedLineRef = useRef(onCommittedLine)
  const historyLinesRef = useRef<string[]>([])
  const historyWalkIndexRef = useRef<number | null>(null)
  const draftBeforeWalkRef = useRef<string | null>(null)

  useEffect(() => {
    onCommittedLineRef.current = onCommittedLine
  }, [onCommittedLine])

  useEffect(() => {
    historyLinesRef.current = loadUserInputHistory(getConfigDir())
  }, [])

  const guidance = useMemo(
    () => effectiveSlashGuidance(buffer, suggestionsDismissed),
    [buffer, suggestionsDismissed]
  )

  const handleInput = useCallback((input: string, key: Key) => {
    if (!isActiveRef.current) return
    const readBuf = () => bufferRef.current
    const readCaret = () => caretRef.current
    const readHighlight = () => slashHighlightRef.current

    const historyState = (): PromptHistoryState => ({
      lineDraft: readBuf(),
      caretOffset: readCaret(),
      userInputHistoryLines: historyLinesRef.current,
      userInputHistoryWalkIndex: historyWalkIndexRef.current,
      lineDraftBeforeUserInputHistoryWalk: draftBeforeWalkRef.current,
    })

    const syncWalkFrom = (s: PromptHistoryState) => {
      historyWalkIndexRef.current = s.userInputHistoryWalkIndex
      draftBeforeWalkRef.current = s.lineDraftBeforeUserInputHistoryWalk
    }

    const endWalkBeforeDraftEdit = () => {
      const next = exitHistoryWalkOnDraftEdit(historyState())
      syncWalkFrom(next)
    }

    const setAll = (nextBuf: string, nextCaret: number, nextHi?: number) => {
      suggestionsDismissedRef.current = false
      setSuggestionsDismissed(false)
      const hi = nextHi ?? 0
      bufferRef.current = nextBuf
      caretRef.current = nextCaret
      slashHighlightRef.current = hi
      setBuffer(nextBuf)
      setCaretOffset(nextCaret)
      setSlashHighlightIndex(hi)
    }

    const setCaretOnly = (nextCaret: number) => {
      caretRef.current = nextCaret
      setCaretOffset(nextCaret)
    }

    const setHighlightOnly = (nextHi: number) => {
      slashHighlightRef.current = nextHi
      setSlashHighlightIndex(nextHi)
    }

    const applyHistoryArrow = (dir: 'up' | 'down') => {
      const prev = historyState()
      const next = dir === 'up' ? onArrowUp(prev) : onArrowDown(prev)
      if (
        prev.lineDraft === next.lineDraft &&
        prev.caretOffset === next.caretOffset &&
        prev.userInputHistoryWalkIndex === next.userInputHistoryWalkIndex &&
        prev.lineDraftBeforeUserInputHistoryWalk ===
          next.lineDraftBeforeUserInputHistoryWalk
      ) {
        return
      }
      syncWalkFrom(next)
      const draftChanged = prev.lineDraft !== next.lineDraft
      if (draftChanged) {
        setAll(next.lineDraft, next.caretOffset, 0)
      } else {
        setCaretOnly(next.caretOffset)
      }
    }

    const commitLine = () => {
      const line = readBuf()
      historyLinesRef.current = appendUserInputHistoryLine(
        historyLinesRef.current,
        maskInteractiveInputLineForStorage(line)
      )
      saveUserInputHistory(getConfigDir(), historyLinesRef.current)
      historyWalkIndexRef.current = null
      draftBeforeWalkRef.current = null
      setAll('', 0, 0)
      if (line !== '') {
        onCommittedLineRef.current(line)
      }
    }

    // --- Slash completion: Tab ---
    if (key.tab) {
      endWalkBeforeDraftEdit()
      const draft = normalizeInputForSlash(readBuf())
      if (draft.startsWith('/') && !draft.endsWith(' ')) {
        const { completed } = getSlashTabCompletion(draft)
        if (completed !== draft) {
          setAll(completed, completed.length, 0)
        }
      }
      return
    }

    // --- Slash completion: Esc ---
    if (key.escape) {
      const raw = readBuf()
      if (isBareDraftSlash(raw)) {
        historyWalkIndexRef.current = null
        draftBeforeWalkRef.current = null
        setAll('', 0, 0)
        return
      }
      const g = slashGuidanceForInk(raw)
      if (g.show === 'list') {
        suggestionsDismissedRef.current = true
        setSuggestionsDismissed(true)
        setHighlightOnly(0)
      }
      return
    }

    // --- Caret movement: left, right, home, end ---
    if (key.leftArrow) {
      const c = readCaret()
      if (c > 0) setCaretOnly(c - 1)
      return
    }
    if (key.rightArrow) {
      const buf = readBuf()
      const c = readCaret()
      if (c < buf.length) setCaretOnly(c + 1)
      return
    }
    if (key.home) {
      setCaretOnly(0)
      return
    }
    if (key.end) {
      setCaretOnly(readBuf().length)
      return
    }

    // --- ↑↓: slash list cycling vs history walk ---
    if (key.upArrow || key.downArrow) {
      const dir = key.upArrow ? 'up' : 'down'
      const buf = readBuf()
      const caret = readCaret()

      const g = effectiveSlashGuidance(buf, suggestionsDismissedRef.current)
      const slashRows = g.show === 'list' ? g.rows : []
      const listVisible = slashRows.length > 0
      const walkingHistory = historyWalkIndexRef.current !== null

      if (
        !walkingHistory &&
        isSlashListArrowKey(dir, caret, buf, listVisible)
      ) {
        setHighlightOnly(
          cycleListSelectionIndex(
            readHighlight(),
            dir === 'down' ? 1 : -1,
            slashRows.length
          )
        )
        return
      }
      applyHistoryArrow(dir)
      return
    }

    // --- Editing: backspace, delete ---
    if (key.backspace) {
      endWalkBeforeDraftEdit()
      const buf = readBuf()
      const c = readCaret()
      if (c === 0) return
      const nextBuf = buf.slice(0, c - 1) + buf.slice(c)
      setAll(nextBuf, c - 1, 0)
      return
    }

    if (key.delete) {
      endWalkBeforeDraftEdit()
      const buf = readBuf()
      const c = readCaret()
      if (c === buf.length && c > 0) {
        const nextBuf = buf.slice(0, c - 1) + buf.slice(c)
        setAll(nextBuf, c - 1, 0)
        return
      }
      if (c >= buf.length) return
      const nextBuf = buf.slice(0, c) + buf.slice(c + 1)
      setAll(nextBuf, c, 0)
      return
    }

    // --- Enter: slash completion pick or commit ---
    if (key.return) {
      const bufForPick = readBuf()
      const gPick = effectiveSlashGuidance(
        bufForPick,
        suggestionsDismissedRef.current
      )
      const pickRows = gPick.show === 'list' ? gPick.rows : []
      if (pickRows.length > 0) {
        endWalkBeforeDraftEdit()
        const hi = readHighlight()
        const row = pickRows[hi] ?? pickRows[0]!
        const completed = `${row.completionLine} `
        setAll(completed, completed.length, 0)
        return
      }
      commitLine()
      return
    }

    // --- Character input (including multi-char paste) ---
    if (input.length > 0) {
      if (!(input.includes('\r') || input.includes('\n'))) {
        endWalkBeforeDraftEdit()
        const buf = readBuf()
        const c = readCaret()
        const inserted = normalizeInputForSlash(input)
        setAll(
          buf.slice(0, c) + inserted + buf.slice(c),
          c + inserted.length,
          0
        )
        return
      }
      endWalkBeforeDraftEdit()
      let curBuf = readBuf()
      let c = readCaret()
      for (let i = 0; i < input.length; i++) {
        const ch = input[i]!
        if (ch === '\r') {
          const line = curBuf
          historyLinesRef.current = appendUserInputHistoryLine(
            historyLinesRef.current,
            maskInteractiveInputLineForStorage(line)
          )
          saveUserInputHistory(getConfigDir(), historyLinesRef.current)
          historyWalkIndexRef.current = null
          draftBeforeWalkRef.current = null
          setAll('', 0, 0)
          if (line !== '') onCommittedLineRef.current(line)
          curBuf = ''
          c = 0
          if (input[i + 1] === '\n') i++
          continue
        }
        if (ch === '\n') {
          const line = curBuf
          historyLinesRef.current = appendUserInputHistoryLine(
            historyLinesRef.current,
            maskInteractiveInputLineForStorage(line)
          )
          saveUserInputHistory(getConfigDir(), historyLinesRef.current)
          historyWalkIndexRef.current = null
          draftBeforeWalkRef.current = null
          setAll('', 0, 0)
          if (line !== '') onCommittedLineRef.current(line)
          curBuf = ''
          c = 0
          continue
        }
        curBuf = curBuf.slice(0, c) + ch + curBuf.slice(c)
        c += 1
      }
      setAll(curBuf, c, 0)
      return
    }
  }, [])

  useInput(handleInput)

  const { stdout } = useStdout()
  const cols = stdout.columns > 0 ? stdout.columns : 80

  const beforeCaret = buffer.slice(0, caretOffset)
  const afterCaret = buffer.slice(caretOffset)

  if (!isActive) {
    return null
  }

  return (
    <Box flexDirection="column" marginTop={1}>
      <Box width={cols} borderStyle="single" borderColor="white">
        <Text>
          {'→ '}
          {beforeCaret}
          <Text inverse> </Text>
          {afterCaret}
          {buffer === '' ? (
            <Text color="gray">{MAIN_PROMPT_PLACEHOLDER}</Text>
          ) : null}
        </Text>
      </Box>
      {guidance.show === 'hint' ? (
        <Text>{DEFAULT_INTERACTIVE_GUIDANCE}</Text>
      ) : guidance.show === 'list' ? (
        <GuidanceListInk
          mode="slash"
          rows={guidance.rows}
          highlightIndex={slashHighlightIndex}
          terminalColumns={cols}
        />
      ) : null}
    </Box>
  )
}
