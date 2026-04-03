import { useCallback } from 'react'
import { Box, Text } from 'ink'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'
import { commitNotebookStagePlainLine } from '../slashCommandShellPlainLineCommit.js'
import { SlashCommandShellLiveColumn } from '../slashCommandShellLiveColumn.js'
import { useSlashCommandShellLiveColumnHandlers } from '../useSlashCommandShellLiveColumnHandlers.js'
import {
  leaveNotebookStageSlashCommand,
  notebookStageSlashCommands,
} from './notebookStageSlashCommands.js'

const STAGE_PLACEHOLDER = '`/exit` to leave notebook context.'

function UseNotebookStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const title = (argument ?? '').trim()
  const {
    liveRegionCols,
    activeStage,
    stageArgumentRef,
    handleStageSettled,
    handleStageAbortWithError,
    onCommittedCommand,
    appendScrollbackError,
    appendScrollbackUserMessage,
  } = useSlashCommandShellLiveColumnHandlers({
    onRunSuccess: useCallback(
      (command, assistantMessage, { appendScrollbackAssistantTextMessage }) => {
        if (command === leaveNotebookStageSlashCommand) {
          onSettled(assistantMessage)
        } else {
          appendScrollbackAssistantTextMessage(assistantMessage)
        }
      },
      [onSettled]
    ),
  })

  const onCommittedLine = useCallback(
    (line: string) => {
      commitNotebookStagePlainLine(line, {
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
