import { useCallback, useMemo, useRef, useState } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput, useStdout } from 'ink'
import { BorderedSingleLinePromptInputInk } from '../../commonUIComponents/borderedSingleLinePromptInputInk.js'
import { GuidanceListInk } from '../../commonUIComponents/guidanceListWindowInk.js'
import { cycleListSelectionIndex } from '../../interactions/selectListInteraction.js'
import {
  DEFAULT_INTERACTIVE_GUIDANCE,
  effectiveSlashGuidance,
  getSlashTabCompletion,
  isBareDraftSlash,
  isSlashListArrowKey,
  normalizeInputForSlash,
  slashGuidanceForInk,
} from '../../mainInteractivePrompt/slashCommandCompletion.js'
import { inkTerminalColumns } from '../../terminalColumns.js'
import {
  transcriptAssistantError,
  transcriptUserLine,
} from '../../sessionScrollback/interactiveCliTranscript.js'
import {
  type SessionScrollbackAppendApi,
  useSessionScrollbackAppend,
} from '../../sessionScrollback/sessionScrollbackAppendContext.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'
import { notebookStageSlashCommands } from './notebookStageSlashCommands.js'

const STAGE_PLACEHOLDER = '`/exit` to leave notebook context.'

function invokeNotebookStageRunCommand(cmd: InteractiveSlashCommand): string {
  if (!('run' in cmd)) {
    throw new Error('expected a run slash command')
  }
  const out = cmd.run()
  if (out instanceof Promise) {
    throw new Error('notebook stage slash commands must be synchronous')
  }
  return out.assistantMessage
}

function dispatchNotebookCommittedLine(
  line: string,
  {
    appendScrollbackItem,
    appendScrollbackItems,
  }: Pick<
    SessionScrollbackAppendApi,
    'appendScrollbackItem' | 'appendScrollbackItems'
  >,
  onSettled: (text: string) => void
) {
  if (line === '') return
  const userItem = transcriptUserLine(line)
  if (line === 'exit') {
    const leaveCmd = notebookStageSlashCommands.find(
      (c) => c.literal === '/exit'
    )
    if (leaveCmd === undefined) return
    appendScrollbackItem(userItem)
    onSettled(invokeNotebookStageRunCommand(leaveCmd))
    return
  }
  for (const cmd of notebookStageSlashCommands) {
    if ('run' in cmd && line === cmd.literal) {
      appendScrollbackItem(userItem)
      onSettled(invokeNotebookStageRunCommand(cmd))
      return
    }
  }
  appendScrollbackItems([userItem, transcriptAssistantError('Not supported')])
}

function UseNotebookStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const title = (argument ?? '').trim()
  const { appendScrollbackItem, appendScrollbackItems } =
    useSessionScrollbackAppend()
  const [buffer, setBuffer] = useState('')
  const [caretOffset, setCaretOffset] = useState(0)
  const [slashHighlightIndex, setSlashHighlightIndex] = useState(0)
  const [suggestionsDismissed, setSuggestionsDismissed] = useState(false)
  const bufferRef = useRef('')
  const caretRef = useRef(0)
  const slashHighlightRef = useRef(0)
  const suggestionsDismissedRef = useRef(false)

  const { stdout } = useStdout()
  const cols = inkTerminalColumns(stdout.columns)

  const guidance = useMemo(
    () =>
      effectiveSlashGuidance(
        buffer,
        suggestionsDismissed,
        notebookStageSlashCommands
      ),
    [buffer, suggestionsDismissed]
  )

  const handleInput = useCallback(
    (input: string, key: Key) => {
      const readBuf = () => bufferRef.current
      const readCaret = () => caretRef.current
      const readHighlight = () => slashHighlightRef.current

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

      const commitLine = () => {
        const raw = readBuf()
        setAll('', 0, 0)
        const line = raw.trim()
        dispatchNotebookCommittedLine(
          line,
          {
            appendScrollbackItem,
            appendScrollbackItems,
          },
          onSettled
        )
      }

      if (key.tab) {
        const draft = normalizeInputForSlash(readBuf())
        if (draft.startsWith('/') && !draft.endsWith(' ')) {
          const { completed } = getSlashTabCompletion(
            draft,
            notebookStageSlashCommands
          )
          if (completed !== draft) {
            setAll(completed, completed.length, 0)
          }
        }
        return
      }

      if (key.escape) {
        const raw = readBuf()
        if (isBareDraftSlash(raw)) {
          setAll('', 0, 0)
          return
        }
        const g = slashGuidanceForInk(raw, notebookStageSlashCommands)
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

      if (key.upArrow || key.downArrow) {
        const dir = key.upArrow ? 'up' : 'down'
        const buf = readBuf()
        const caret = readCaret()
        const g = effectiveSlashGuidance(
          buf,
          suggestionsDismissedRef.current,
          notebookStageSlashCommands
        )
        const slashRows = g.show === 'list' ? g.rows : []
        const listVisible = slashRows.length > 0
        if (isSlashListArrowKey(dir, caret, buf, listVisible)) {
          setHighlightOnly(
            cycleListSelectionIndex(
              readHighlight(),
              dir === 'down' ? 1 : -1,
              slashRows.length
            )
          )
          return
        }
        return
      }

      if (key.backspace) {
        const buf = readBuf()
        const c = readCaret()
        if (c === 0) return
        const nextBuf = buf.slice(0, c - 1) + buf.slice(c)
        setAll(nextBuf, c - 1, 0)
        return
      }

      if (key.delete) {
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
          suggestionsDismissedRef.current,
          notebookStageSlashCommands
        )
        const pickRows = gPick.show === 'list' ? gPick.rows : []
        if (pickRows.length > 0) {
          const hi = readHighlight()
          const row = pickRows[hi] ?? pickRows[0]!
          const completed = `${row.completionLine} `
          setAll(completed, completed.length, 0)
          return
        }
        commitLine()
        return
      }

      if (input.length > 0) {
        if (!(input.includes('\r') || input.includes('\n'))) {
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
        let curBuf = readBuf()
        let c = readCaret()
        for (let i = 0; i < input.length; i++) {
          const ch = input[i]!
          if (ch === '\r' || ch === '\n') {
            bufferRef.current = curBuf
            caretRef.current = c
            setBuffer(curBuf)
            setCaretOffset(c)
            commitLine()
            curBuf = bufferRef.current
            c = caretRef.current
            if (ch === '\r' && input[i + 1] === '\n') i++
            continue
          }
          const inserted = normalizeInputForSlash(ch)
          curBuf = curBuf.slice(0, c) + inserted + curBuf.slice(c)
          c += inserted.length
        }
        setAll(curBuf, c, 0)
      }
    },
    [appendScrollbackItem, appendScrollbackItems, onSettled]
  )

  useInput(handleInput)

  return (
    <Box flexDirection="column">
      <Text>Active notebook: {title}</Text>
      <Box flexDirection="column" marginTop={1}>
        <BorderedSingleLinePromptInputInk
          terminalColumns={cols}
          buffer={buffer}
          caretOffset={caretOffset}
          placeholder={STAGE_PLACEHOLDER}
        />
        {guidance.show === 'hint' ? (
          <Text color="gray">{DEFAULT_INTERACTIVE_GUIDANCE}</Text>
        ) : guidance.show === 'list' ? (
          <GuidanceListInk
            mode="slash"
            rows={guidance.rows}
            highlightIndex={slashHighlightIndex}
            rowBudget={5}
            terminalColumns={cols}
          />
        ) : null}
      </Box>
    </Box>
  )
}

const useNotebookDoc: CommandDoc = {
  name: '/use',
  usage: '/use <notebook title>',
  description: 'Set the active notebook for book commands (title only for now)',
}

export const useNotebookSlashCommand: InteractiveSlashCommand = {
  literal: '/use',
  doc: useNotebookDoc,
  argument: { name: 'notebook title', optional: false },
  stageComponent: UseNotebookStage,
  stageIndicator: 'Notebook',
}
