import { useCallback } from 'react'
import { Box, Text } from 'ink'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'
import type { InteractiveRunSlashCommand } from '../interactiveSlashCommandDispatch.js'
import { commitNotebookStagePlainLine } from '../slashCommandShellPlainLineCommit.js'
import { SlashCommandShellHost } from '../slashCommandShellHost.js'
import type { SlashCommandShellRunSuccessContext } from '../useSlashCommandShellLiveColumnHandlers.js'
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
  const onRunSuccess = useCallback(
    (
      command: InteractiveRunSlashCommand,
      assistantMessage: string,
      {
        appendScrollbackAssistantTextMessage,
      }: SlashCommandShellRunSuccessContext
    ) => {
      if (command === leaveNotebookStageSlashCommand) {
        onSettled(assistantMessage)
      } else {
        appendScrollbackAssistantTextMessage(assistantMessage)
      }
    },
    [onSettled]
  )

  return (
    <Box flexDirection="column">
      <Text>Active notebook: {title}</Text>
      <SlashCommandShellHost
        onRunSuccess={onRunSuccess}
        slashCommands={notebookStageSlashCommands}
        placeholder={STAGE_PLACEHOLDER}
        showMainPrompt
        commitPlainLine={commitNotebookStagePlainLine}
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
