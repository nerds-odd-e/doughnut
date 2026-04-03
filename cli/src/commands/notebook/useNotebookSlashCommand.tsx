import { useCallback } from 'react'
import { Box, Text } from 'ink'
import { MainInteractivePrompt } from '../../mainInteractivePrompt/index.js'
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
import type { ResolvedInteractiveSlashCommand } from '../interactiveSlashCommands.js'
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

function dispatchNotebookUncommittedLine(
  line: string,
  {
    appendScrollbackItem,
  }: Pick<SessionScrollbackAppendApi, 'appendScrollbackItem'>
) {
  if (line === '') return
  appendScrollbackItem(transcriptUserLine(line))
  appendScrollbackItem(transcriptAssistantError('Not supported'))
}

function UseNotebookStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const title = (argument ?? '').trim()
  const { appendScrollbackItem } = useSessionScrollbackAppend()

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      const userItem = transcriptUserLine(resolved.line)
      appendScrollbackItem(userItem)
      onSettled(invokeNotebookStageRunCommand(resolved.command))
    },
    [appendScrollbackItem, onSettled]
  )

  const onCommittedLine = useCallback(
    (line: string) => {
      dispatchNotebookUncommittedLine(line.trim(), { appendScrollbackItem })
    },
    [appendScrollbackItem]
  )

  return (
    <Box flexDirection="column">
      <Text>Active notebook: {title}</Text>
      <MainInteractivePrompt
        onCommittedCommand={onCommittedCommand}
        onCommittedLine={onCommittedLine}
        isActive
        slashCommands={notebookStageSlashCommands}
        placeholder={STAGE_PLACEHOLDER}
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
