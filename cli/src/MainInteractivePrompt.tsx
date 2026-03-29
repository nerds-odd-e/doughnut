import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { interactiveSlashCommands } from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommand } from './commands/interactiveSlashCommand.js'
import {
  appendUserInputHistoryLine,
  exitHistoryWalkOnDraftEdit,
  onArrowDown,
  onArrowUp,
  type MainInteractivePromptHistoryState,
} from './mainInteractivePromptHistory.js'

const DEFAULT_INTERACTIVE_GUIDANCE = '/ commands'

const GUIDANCE_LIST_MAX_VISIBLE = 5
const COMPLETION_USAGE_PAD = 20

function normalizedInteractiveDraft(draft: string): string {
  return draft.replace(/\n/g, ' ')
}

function getSlashTabCompletion(
  buffer: string,
  commands: readonly InteractiveSlashCommand[]
): { completed: string; count: number } {
  if (!buffer.startsWith('/')) return { completed: buffer, count: 0 }
  const matches = commands.filter((c) => c.doc.usage.startsWith(buffer))
  if (matches.length === 0) return { completed: buffer, count: 0 }
  if (matches.length === 1)
    return { completed: `${matches[0]!.doc.usage} `, count: 1 }
  const usages = matches.map((m) => m.doc.usage)
  let prefix = usages[0]!
  for (let i = 1; i < usages.length; i++) {
    while (!usages[i]!.startsWith(prefix) && prefix.length > 0) {
      prefix = prefix.slice(0, -1)
    }
  }
  if (prefix.length > buffer.length)
    return { completed: prefix, count: matches.length }
  return { completed: buffer, count: matches.length }
}

function filterSlashCommandsByPrefix(
  commands: readonly InteractiveSlashCommand[],
  prefix: string
): InteractiveSlashCommand[] {
  const searchTerm =
    prefix.startsWith('/') && prefix.length > 1 ? prefix.slice(1) : prefix
  if (!searchTerm) return [...commands]

  return [...commands]
    .filter((c) => c.doc.usage.includes(searchTerm))
    .sort((a, b) => {
      const aBegins =
        a.doc.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && a.doc.usage.startsWith(`/${searchTerm}`))
      const bBegins =
        b.doc.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && b.doc.usage.startsWith(`/${searchTerm}`))
      if (aBegins && !bBegins) return -1
      if (!aBegins && bBegins) return 1
      return 0
    })
}

type SlashGuidanceForInk =
  | { show: 'hint' }
  | { show: 'empty' }
  | {
      show: 'list'
      readonly rows: readonly {
        readonly usage: string
        readonly description: string
      }[]
    }

function slashGuidanceForInk(draft: string): SlashGuidanceForInk {
  const p = normalizedInteractiveDraft(draft)
  if (!p.startsWith('/') || p.endsWith(' ')) return { show: 'hint' }
  const matches = filterSlashCommandsByPrefix(interactiveSlashCommands, p)
  if (matches.length === 0) return { show: 'empty' }
  const rows = matches.map((c) => ({
    usage: c.doc.usage,
    description: c.doc.description,
  }))
  return { show: 'list', rows }
}

function effectiveSlashGuidance(
  draft: string,
  suggestionsDismissed: boolean
): SlashGuidanceForInk {
  const g = slashGuidanceForInk(draft)
  if (suggestionsDismissed && g.show === 'list') return { show: 'hint' }
  return g
}

function ttyArrowKeyUsesSlashSuggestionCycle(
  key: 'up' | 'down',
  caretOffset: number,
  lineDraft: string,
  slashCompletionListVisible: boolean
): boolean {
  if (!slashCompletionListVisible) return false
  if (key === 'up') return caretOffset === 0
  return caretOffset === lineDraft.length
}

function visibleListRows(
  rows: readonly { readonly usage: string; readonly description: string }[],
  highlightIndex: number
): { readonly rows: typeof rows; readonly highlightIndex: number } {
  const max = GUIDANCE_LIST_MAX_VISIBLE
  if (rows.length <= max) {
    return { rows, highlightIndex }
  }
  const maxStart = Math.max(0, rows.length - max)
  const start = Math.min(
    Math.max(0, highlightIndex - Math.floor(max / 2)),
    maxStart
  )
  return {
    rows: rows.slice(start, start + max),
    highlightIndex: highlightIndex - start,
  }
}

export function MainInteractivePrompt({
  onCommittedLine,
}: {
  readonly onCommittedLine: (line: string) => void
}) {
  const [buffer, setBuffer] = useState('')
  const [caretOffset, setCaretOffset] = useState(0)
  const [slashHighlightIndex, setSlashHighlightIndex] = useState(0)
  const [suggestionsDismissed, setSuggestionsDismissed] = useState(false)

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

  const guidance = useMemo(
    () => effectiveSlashGuidance(buffer, suggestionsDismissed),
    [buffer, suggestionsDismissed]
  )

  /**
   * Input precedence: Tab / Esc / arrows / editing use the same buffer+caret refs.
   * ↑↓: If walking user input history, history owns ↑↓ (slash list does not steal).
   * Else if slash completion list is visible and caret is at the list-cycle boundary
   * (caret 0 for ↑, EOL for ↓), cycle slash highlight.
   * Else apply history+caret rules from mainInteractivePromptHistory (recall, caret jump).
   * When a history step changes the draft, reset slash highlight and suggestionsDismissed.
   * Typing, backspace, delete, Tab, and Esc that clear `/` end the history walk first.
   */
  const handleInput = useCallback((input: string, key: Key) => {
    const readBuf = () => bufferRef.current
    const readCaret = () => caretRef.current
    const readHighlight = () => slashHighlightRef.current

    const historyState = (): MainInteractivePromptHistoryState => ({
      lineDraft: readBuf(),
      caretOffset: readCaret(),
      userInputHistoryLines: historyLinesRef.current,
      userInputHistoryWalkIndex: historyWalkIndexRef.current,
      lineDraftBeforeUserInputHistoryWalk: draftBeforeWalkRef.current,
    })

    const syncWalkFrom = (s: MainInteractivePromptHistoryState) => {
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
        suggestionsDismissedRef.current = false
        setSuggestionsDismissed(false)
        bufferRef.current = next.lineDraft
        caretRef.current = next.caretOffset
        slashHighlightRef.current = 0
        setBuffer(next.lineDraft)
        setCaretOffset(next.caretOffset)
        setSlashHighlightIndex(0)
      } else {
        setCaretOnly(next.caretOffset)
      }
    }

    const commitLine = () => {
      const line = readBuf()
      historyLinesRef.current = appendUserInputHistoryLine(
        historyLinesRef.current,
        line
      )
      historyWalkIndexRef.current = null
      draftBeforeWalkRef.current = null
      setAll('', 0, 0)
      if (line !== '') {
        onCommittedLineRef.current(line)
      }
    }

    if (key.tab) {
      endWalkBeforeDraftEdit()
      const draft = normalizedInteractiveDraft(readBuf())
      if (draft.startsWith('/') && !draft.endsWith(' ')) {
        const { completed } = getSlashTabCompletion(
          draft,
          interactiveSlashCommands
        )
        if (completed !== draft) {
          const c = completed.length
          setAll(completed, c, 0)
        }
      }
      return
    }

    if (key.escape) {
      const raw = readBuf()
      const draft = normalizedInteractiveDraft(raw)
      if (draft === '/') {
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

    const g = effectiveSlashGuidance(readBuf(), suggestionsDismissedRef.current)
    const slashRows = g.show === 'list' ? g.rows : []
    const listVisible = slashRows.length > 0

    if (key.upArrow || key.downArrow) {
      const dir = key.upArrow ? 'up' : 'down'
      const buf = readBuf()
      const caret = readCaret()
      const walkingHistory = historyWalkIndexRef.current !== null
      if (
        !walkingHistory &&
        ttyArrowKeyUsesSlashSuggestionCycle(dir, caret, buf, listVisible)
      ) {
        const rowCount = slashRows.length
        const hi = readHighlight()
        const next =
          dir === 'down' ? (hi + 1) % rowCount : (hi - 1 + rowCount) % rowCount
        setHighlightOnly(next)
        return
      }
      applyHistoryArrow(dir)
      return
    }

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
        const completed = `${row.usage} `
        setAll(completed, completed.length, 0)
        return
      }
      commitLine()
      return
    }

    if (input.length > 0) {
      if (!(input.includes('\r') || input.includes('\n'))) {
        endWalkBeforeDraftEdit()
        const buf = readBuf()
        const c = readCaret()
        const inserted = normalizedInteractiveDraft(input)
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
            line
          )
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
            line
          )
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

  const listWindow =
    guidance.show === 'list'
      ? visibleListRows(guidance.rows, slashHighlightIndex)
      : null

  const beforeCaret = buffer.slice(0, caretOffset)
  const afterCaret = buffer.slice(caretOffset)

  return (
    <Box flexDirection="column">
      <Text>
        {'> '}
        {beforeCaret}
        <Text inverse> </Text>
        {afterCaret}
      </Text>
      {guidance.show === 'hint' ? (
        <Text>{DEFAULT_INTERACTIVE_GUIDANCE}</Text>
      ) : listWindow ? (
        listWindow.rows.map((row, i) => (
          <Text key={`${row.usage}-${i}`}>
            {'  '}
            {i === listWindow.highlightIndex ? (
              <Text inverse>
                {row.usage.padEnd(COMPLETION_USAGE_PAD)}
                {row.description}
              </Text>
            ) : (
              <Text color="gray">
                {row.usage.padEnd(COMPLETION_USAGE_PAD)}
                {row.description}
              </Text>
            )}
          </Text>
        ))
      ) : null}
    </Box>
  )
}
