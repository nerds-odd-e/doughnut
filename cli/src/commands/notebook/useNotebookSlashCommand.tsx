import { useCallback, useRef, useState } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput, useStdout } from 'ink'
import { BorderedSingleLinePromptInputInk } from '../../commonUIComponents/borderedSingleLinePromptInputInk.js'
import { normalizeInputForSlash } from '../../mainInteractivePrompt/slashCommandCompletion.js'
import { inkTerminalColumns } from '../../terminalColumns.js'
import {
  transcriptAssistantError,
  transcriptUserLine,
} from '../../sessionScrollback/interactiveCliTranscript.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'

const STAGE_PLACEHOLDER = '`/exit` to leave notebook context.'

function UseNotebookStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const title = (argument ?? '').trim()
  const { appendScrollbackItem, appendScrollbackItems } =
    useSessionScrollbackAppend()
  const [buffer, setBuffer] = useState('')
  const [caretOffset, setCaretOffset] = useState(0)
  const bufferRef = useRef('')
  const caretRef = useRef(0)

  const { stdout } = useStdout()
  const cols = inkTerminalColumns(stdout.columns)

  const handleInput = useCallback(
    (input: string, key: Key) => {
      const readBuf = () => bufferRef.current
      const readCaret = () => caretRef.current

      const setAll = (nextBuf: string, nextCaret: number) => {
        bufferRef.current = nextBuf
        caretRef.current = nextCaret
        setBuffer(nextBuf)
        setCaretOffset(nextCaret)
      }

      const commitLine = () => {
        const raw = readBuf()
        setAll('', 0)
        const line = raw.trim()
        if (line === '') return

        const userItem = transcriptUserLine(line)
        if (line === '/exit' || line === 'exit') {
          appendScrollbackItem(userItem)
          onSettled('Left notebook context.')
          return
        }
        appendScrollbackItems([
          userItem,
          transcriptAssistantError('Not supported'),
        ])
      }

      if (key.leftArrow) {
        const c = readCaret()
        if (c > 0) setAll(readBuf(), c - 1)
        return
      }
      if (key.rightArrow) {
        const buf = readBuf()
        const c = readCaret()
        if (c < buf.length) setAll(buf, c + 1)
        return
      }
      if (key.home) {
        setAll(readBuf(), 0)
        return
      }
      if (key.end) {
        const buf = readBuf()
        setAll(buf, buf.length)
        return
      }

      if (key.backspace) {
        const buf = readBuf()
        const c = readCaret()
        if (c === 0) return
        setAll(buf.slice(0, c - 1) + buf.slice(c), c - 1)
        return
      }

      if (key.delete) {
        const buf = readBuf()
        const c = readCaret()
        if (c === buf.length && c > 0) {
          setAll(buf.slice(0, c - 1) + buf.slice(c), c - 1)
          return
        }
        if (c >= buf.length) return
        setAll(buf.slice(0, c) + buf.slice(c + 1), c)
        return
      }

      if (key.return) {
        commitLine()
        return
      }

      if (input.length > 0) {
        if (!(input.includes('\r') || input.includes('\n'))) {
          const buf = readBuf()
          const c = readCaret()
          const inserted = normalizeInputForSlash(input)
          setAll(buf.slice(0, c) + inserted + buf.slice(c), c + inserted.length)
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
        setAll(curBuf, c)
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
