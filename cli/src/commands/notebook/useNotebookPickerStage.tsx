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

const PICKER_PROMPT =
  'Pick a notebook (type to filter, ↑/↓, Enter to select, Esc to cancel).'

const NO_MATCH_MESSAGE = 'No matching notebooks.'

/** Case-insensitive substring match; empty / whitespace-only filter shows all notebooks. */
function notebookTitleMatchesFilter(title: string, filter: string): boolean {
  const q = filter.trim().toLowerCase()
  if (q === '') return true
  return title.toLowerCase().includes(q)
}

export function UseNotebookPickerStage({
  notebooks,
  onPick,
  onSettled,
}: Pick<InteractiveSlashCommandStageProps, 'onSettled'> & {
  readonly notebooks: readonly Notebook[]
  readonly onPick: (notebook: Notebook) => void
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [filterQuery, setFilterQuery] = useState('')
  const [highlightIndex, setHighlightIndex] = useState(0)

  const filteredNotebooks = useMemo(
    () =>
      notebooks.filter((n) => notebookTitleMatchesFilter(n.title, filterQuery)),
    [notebooks, filterQuery]
  )

  const filteredTitles = useMemo(
    () => filteredNotebooks.map((n) => n.title),
    [filteredNotebooks]
  )

  useLayoutEffect(() => {
    setHighlightIndex((i) => {
      if (filteredNotebooks.length === 0) return 0
      return Math.min(i, filteredNotebooks.length - 1)
    })
  }, [filteredNotebooks])

  const { stdout } = useStdout()
  const width = inkTerminalColumns(stdout.columns)
  const listLines = useMemo(
    () => numberedTerminalListLines(filteredTitles, width),
    [filteredTitles, width]
  )

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (notebooks.length === 0) return

      handleSelectListInkKey(
        input,
        key,
        '',
        highlightIndex,
        filteredNotebooks.length,
        { kind: 'filter-buffer' },
        'abort-list',
        {
          onSetHighlightIndex: setHighlightIndex,
          onSubmitHighlightIndex: (index) => {
            const picked = filteredNotebooks[index]
            if (picked !== undefined) {
              onPick(picked)
            }
          },
          onAbortHighlightOnlyList: () => {
            onSettled(PICKER_CANCELLED_MESSAGE)
          },
          onEditChar: (char) => {
            setFilterQuery((q) => q + char)
          },
          onEditBackspace: () => {
            setFilterQuery((q) => (q.length > 0 ? q.slice(0, -1) : q))
          },
        }
      )
    },
    [filteredNotebooks, highlightIndex, notebooks.length, onPick, onSettled]
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

  const showNoMatch = notebooks.length > 0 && filteredNotebooks.length === 0

  return (
    <Box flexDirection="column">
      <Text>{PICKER_PROMPT}</Text>
      <Text dimColor>Filter: {filterQuery}</Text>
      {showNoMatch ? <Text>{NO_MATCH_MESSAGE}</Text> : null}
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
