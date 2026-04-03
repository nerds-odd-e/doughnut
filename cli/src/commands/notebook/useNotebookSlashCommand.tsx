import { useCallback } from 'react'
import { Box, Text, useStdout } from 'ink'
import {
  type SessionScrollbackAppendApi,
  useSessionScrollbackAppend,
} from '../../sessionScrollback/sessionScrollbackAppendContext.js'
import { inkTerminalColumns } from '../../terminalColumns.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'
import type { ResolvedInteractiveSlashCommand } from '../interactiveSlashCommands.js'
import { applyResolvedInteractiveSlashCommand } from '../interactiveSlashCommandDispatch.js'
import { SlashCommandShellLiveColumn } from '../slashCommandShellLiveColumn.js'
import { useSlashCommandShellState } from '../useSlashCommandShellState.js'
import {
  leaveNotebookStageSlashCommand,
  notebookStageSlashCommands,
} from './notebookStageSlashCommands.js'

const STAGE_PLACEHOLDER = '`/exit` to leave notebook context.'

function dispatchNotebookUncommittedLine(
  line: string,
  {
    appendScrollbackError,
    appendScrollbackUserMessage,
  }: Pick<
    SessionScrollbackAppendApi,
    'appendScrollbackError' | 'appendScrollbackUserMessage'
  >
) {
  if (line === '') return
  appendScrollbackUserMessage(line)
  appendScrollbackError('Not supported')
}

function UseNotebookStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const title = (argument ?? '').trim()
  const {
    appendScrollbackAssistantTextMessage,
    appendScrollbackError,
    appendScrollbackUserMessage,
  } = useSessionScrollbackAppend()
  const { stdout } = useStdout()
  const liveRegionCols = inkTerminalColumns(stdout.columns)

  const {
    activeStage,
    stageArgumentRef,
    handleStageSettled,
    handleStageAbortWithError,
    openStage,
    setStageArgumentRef,
  } = useSlashCommandShellState(
    appendScrollbackAssistantTextMessage,
    appendScrollbackError
  )

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      appendScrollbackUserMessage(resolved.line)
      applyResolvedInteractiveSlashCommand(resolved, {
        appendScrollbackError,
        setStageArgumentRef,
        openStage,
        onRunSuccess: (command, assistantMessage) => {
          if (command === leaveNotebookStageSlashCommand) {
            onSettled(assistantMessage)
          } else {
            appendScrollbackAssistantTextMessage(assistantMessage)
          }
        },
      })
    },
    [
      appendScrollbackAssistantTextMessage,
      appendScrollbackError,
      appendScrollbackUserMessage,
      onSettled,
      openStage,
      setStageArgumentRef,
    ]
  )

  const onCommittedLine = useCallback(
    (line: string) => {
      dispatchNotebookUncommittedLine(line.trim(), {
        appendScrollbackError,
        appendScrollbackUserMessage,
      })
    },
    [appendScrollbackError, appendScrollbackUserMessage]
  )

  return (
    <Box flexDirection="column">
      <Text>Active notebook: {title}</Text>
      <SlashCommandShellLiveColumn
        cols={liveRegionCols}
        activeStage={activeStage}
        stageArgumentRef={stageArgumentRef}
        onStageSettled={handleStageSettled}
        onStageAbortWithError={handleStageAbortWithError}
        showMainPrompt
        mainPromptIsActive={!activeStage}
        slashCommands={notebookStageSlashCommands}
        placeholder={STAGE_PLACEHOLDER}
        onCommittedCommand={onCommittedCommand}
        onCommittedLine={onCommittedLine}
      />
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
