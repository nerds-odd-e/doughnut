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

  const onCommittedLine = useCallback(
    (line: string) => {
      dispatchNotebookCommittedLine(
        line.trim(),
        { appendScrollbackItem, appendScrollbackItems },
        onSettled
      )
    },
    [appendScrollbackItem, appendScrollbackItems, onSettled]
  )

  return (
    <Box flexDirection="column">
      <Text>Active notebook: {title}</Text>
      <MainInteractivePrompt
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
