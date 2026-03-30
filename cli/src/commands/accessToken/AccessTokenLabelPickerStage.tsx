import {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { handleSelectListInkKey } from '../../interactions/selectListInteraction.js'
import {
  numberedTerminalListLines,
  resolvedTerminalWidth,
} from '../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from './stageKeyForwardContext.js'

/** Same short copy as list picker / user abort patterns. */
const PICKER_ABORTED_MESSAGE = 'Cancelled.'

export function AccessTokenLabelPickerStage({
  onSettled,
  labels,
  stageIndicator,
  currentPrompt,
  initialHighlightIndex,
  onPick,
}: InteractiveSlashCommandStageProps & {
  readonly labels: readonly string[]
  readonly stageIndicator: string
  readonly currentPrompt: string
  readonly initialHighlightIndex: (labels: readonly string[]) => number
  readonly onPick: (label: string) => void
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const emptySettledRef = useRef(false)

  useEffect(() => {
    if (labels.length > 0 || emptySettledRef.current) return
    emptySettledRef.current = true
    onSettled('No access tokens stored.')
  }, [labels.length, onSettled])

  const [highlightIndex, setHighlightIndex] = useState(() =>
    initialHighlightIndex(labels)
  )

  const width = resolvedTerminalWidth()
  const listLines = useMemo(
    () => numberedTerminalListLines(labels, width),
    [labels, width]
  )

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (labels.length === 0) return

      handleSelectListInkKey(
        input,
        key,
        '',
        highlightIndex,
        labels.length,
        { kind: 'highlight-only' },
        'abort-list',
        {
          onSetHighlightIndex: setHighlightIndex,
          onSubmitHighlightIndex: (index) => {
            onPick(labels[index]!)
          },
          onAbortHighlightOnlyList: () => {
            onSettled(PICKER_ABORTED_MESSAGE)
          },
          onOtherDispatch: () => {
            onSettled(PICKER_ABORTED_MESSAGE)
          },
        }
      )
    },
    [highlightIndex, labels, onPick, onSettled]
  )

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    setStageKeyHandler(handleInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleInput])

  useInput(handleInput, {
    isActive: setStageKeyHandler === undefined,
  })

  if (labels.length === 0) {
    return (
      <Box flexDirection="column">
        <Text>No access tokens stored.</Text>
      </Box>
    )
  }

  return (
    <Box flexDirection="column">
      <Text>{stageIndicator}</Text>
      <Text>{currentPrompt}</Text>
      <Box flexDirection="column">
        {listLines.map((line, row) => (
          <Text key={row} inverse={line.itemIndex === highlightIndex}>
            {line.text}
          </Text>
        ))}
      </Box>
    </Box>
  )
}
