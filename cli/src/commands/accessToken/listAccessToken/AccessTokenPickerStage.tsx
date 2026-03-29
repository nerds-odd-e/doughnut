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
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListKeyEventFromInk,
} from '../../../interactions/selectListInteraction.js'
import type { TokenListCommandConfig } from '../../../shell/tokenListCommands.js'
import {
  numberedTerminalListLines,
  resolvedTerminalWidth,
} from '../../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../../interactiveSlashCommand.js'
import {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  setDefaultTokenLabel,
} from '../accessToken.js'
import { SetStageKeyHandlerContext } from '../../../stageKeyForwardContext.js'

/** Same short copy as `userVisibleSlashCommandError` for user-initiated abort. */
const PICKER_ABORTED_MESSAGE = 'Cancelled.'

function initialHighlightIndex(labels: readonly string[]): number {
  if (labels.length === 0) return 0
  const d = getDefaultTokenLabel()
  const idx = d ? labels.indexOf(d) : -1
  return Math.max(0, idx)
}

export function AccessTokenPickerStage({
  onSettled,
  tokenListConfig,
}: InteractiveSlashCommandStageProps & {
  readonly tokenListConfig: TokenListCommandConfig
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const labels = useMemo(() => getStoredAccessTokenLabels(), [])
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

      const ev = selectListKeyEventFromInk(input, key, '')
      const listDispatch = dispatchSelectListKey(
        ev,
        highlightIndex,
        { kind: 'highlight-only' },
        'abort-list'
      )

      switch (listDispatch.result) {
        case 'abort-highlight-only-list':
          onSettled(PICKER_ABORTED_MESSAGE)
          return
        case 'move-highlight':
          setHighlightIndex((hi) =>
            cycleListSelectionIndex(hi, listDispatch.delta, labels.length)
          )
          return
        case 'submit-highlight-index': {
          const selectedLabel = labels[listDispatch.index]!
          if (tokenListConfig.action === 'set-default') {
            setDefaultTokenLabel(selectedLabel)
            onSettled(`Default token set to: ${selectedLabel}`)
            return
          }
          throw new Error(
            `AccessTokenPickerStage: action ${tokenListConfig.action} not implemented`
          )
        }
        default:
          onSettled(PICKER_ABORTED_MESSAGE)
      }
    },
    [highlightIndex, labels, onSettled, tokenListConfig.action]
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
      <Text>{tokenListConfig.stageIndicator}</Text>
      {tokenListConfig.currentPrompt ? (
        <Text>{tokenListConfig.currentPrompt}</Text>
      ) : null}
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
