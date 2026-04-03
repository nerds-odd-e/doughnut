import { useCallback, useRef, useState } from 'react'
import type { ComponentType } from 'react'
import { Box, Text, useStdout } from 'ink'
import { MainInteractivePrompt } from '../../mainInteractivePrompt/index.js'
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
import { SlashCommandStageMount } from '../slashCommandStageMount.js'
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

  const [activeNestedStage, setActiveNestedStage] = useState<{
    component: ComponentType<InteractiveSlashCommandStageProps>
    stageIndicator?: string
  } | null>(null)
  const nestedStageArgumentRef = useRef<string | undefined>(undefined)
  const activeNestedIndicator = activeNestedStage?.stageIndicator

  const clearNestedStage = useCallback(() => {
    setActiveNestedStage(null)
    nestedStageArgumentRef.current = undefined
  }, [])

  const handleNestedSettled = useCallback(
    (assistantText: string) => {
      if (assistantText !== '') {
        appendScrollbackAssistantTextMessage(assistantText)
      }
      clearNestedStage()
    },
    [appendScrollbackAssistantTextMessage, clearNestedStage]
  )

  const handleNestedAbortWithError = useCallback(
    (message: string) => {
      if (message !== '') {
        appendScrollbackError(message)
      }
      clearNestedStage()
    },
    [appendScrollbackError, clearNestedStage]
  )

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      appendScrollbackUserMessage(resolved.line)
      applyResolvedInteractiveSlashCommand(resolved, {
        appendScrollbackError,
        setStageArgumentRef: (arg) => {
          nestedStageArgumentRef.current = arg
        },
        openStage: ({ component, stageIndicator }) => {
          setActiveNestedStage(() => ({
            component,
            stageIndicator,
          }))
        },
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
      {activeNestedStage ? (
        <SlashCommandStageMount
          cols={liveRegionCols}
          stageIndicator={activeNestedIndicator}
          Stage={activeNestedStage.component}
          stageProps={{
            argument: nestedStageArgumentRef.current,
            onSettled: handleNestedSettled,
            onAbortWithError: handleNestedAbortWithError,
          }}
        />
      ) : null}
      <MainInteractivePrompt
        onCommittedCommand={onCommittedCommand}
        onCommittedLine={onCommittedLine}
        isActive={!activeNestedStage}
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
