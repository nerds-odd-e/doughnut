import {
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useState,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput, useStdout } from 'ink'
import type { Notebook } from 'doughnut-api'
import { GuidanceListInk } from '../../commonUIComponents/guidanceListWindowInk.js'
import { SetStageKeyHandlerContext } from '../../commonUIComponents/stageKeyForwardContext.js'
import { handleSelectListInkKey } from '../../interactions/selectListInteraction.js'
import {
  inkTerminalColumns,
  numberedTerminalListLines,
} from '../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'

const PICKER_CANCELLED_MESSAGE = 'Cancelled.'

const PICKER_PROMPT = 'Pick a notebook (↑/↓, Enter to select, Esc to cancel).'

export function UseNotebookPickerStage({
  notebooks,
  onPick,
  onSettled,
}: Pick<InteractiveSlashCommandStageProps, 'onSettled'> & {
  readonly notebooks: readonly Notebook[]
  readonly onPick: (notebook: Notebook) => void
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const titles = useMemo(() => notebooks.map((n) => n.title), [notebooks])
  const [highlightIndex, setHighlightIndex] = useState(0)

  const { stdout } = useStdout()
  const width = inkTerminalColumns(stdout.columns)
  const listLines = useMemo(
    () => numberedTerminalListLines(titles, width),
    [titles, width]
  )

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (notebooks.length === 0) return

      handleSelectListInkKey(
        input,
        key,
        '',
        highlightIndex,
        notebooks.length,
        { kind: 'highlight-only' },
        'abort-list',
        {
          onSetHighlightIndex: setHighlightIndex,
          onSubmitHighlightIndex: (index) => {
            onPick(notebooks[index]!)
          },
          onAbortHighlightOnlyList: () => {
            onSettled(PICKER_CANCELLED_MESSAGE)
          },
          onOtherDispatch: () => {
            onSettled(PICKER_CANCELLED_MESSAGE)
          },
        }
      )
    },
    [highlightIndex, notebooks, onPick, onSettled]
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

  return (
    <Box flexDirection="column">
      <Text>{PICKER_PROMPT}</Text>
      <Box flexDirection="column">
        <GuidanceListInk
          mode="numbered"
          lines={listLines}
          highlightItemIndex={highlightIndex}
          rowBudget={5}
        />
      </Box>
    </Box>
  )
}
