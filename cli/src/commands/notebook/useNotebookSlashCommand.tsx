import { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Text } from 'ink'
import {
  NotebookController,
  type Notebook,
  type NotebooksViewedByUser,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
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

const NOTEBOOK_NOT_FOUND = 'No notebook found with that title.'

function UseNotebookActiveShell({
  notebook,
  onSettled,
}: {
  readonly notebook: Notebook
  readonly onSettled: (assistantText: string) => void
}) {
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
      <Text>Active notebook: {notebook.title}</Text>
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

function UseNotebookStage({
  argument,
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  const title = (argument ?? '').trim()
  const [resolvedNotebook, setResolvedNotebook] = useState<Notebook | null>(
    null
  )
  const resolvedNotebookRef = useRef<Notebook | null>(null)

  useEffect(() => {
    if (title === '') {
      onAbortWithError('No notebook title given.')
    }
  }, [title, onAbortWithError])

  const runResolveNotebook = useCallback(
    async (signal: AbortSignal) => {
      const view = await runDefaultBackendJson<NotebooksViewedByUser>(() =>
        NotebookController.myNotebooks({
          ...doughnutSdkOptions(signal),
        })
      )

      const matches = view.notebooks.filter((n: Notebook) => n.title === title)
      if (matches.length === 0) {
        throw new Error(NOTEBOOK_NOT_FOUND)
      }
      if (matches.length > 1) {
        throw new Error(
          `Multiple notebooks match "${title}". Rename one in the web app so the title is unique.`
        )
      }
      resolvedNotebookRef.current = matches[0]
      return ''
    },
    [title]
  )

  const handleFetchSuccess = useCallback(() => {
    const next = resolvedNotebookRef.current
    if (next !== null) {
      setResolvedNotebook(next)
    }
  }, [])

  if (resolvedNotebook !== null) {
    return (
      <UseNotebookActiveShell
        notebook={resolvedNotebook}
        onSettled={onSettled}
      />
    )
  }

  if (title === '') {
    return null
  }

  return (
    <AsyncAssistantFetchStage
      spinnerLabel="Loading notebooks…"
      runAssistantMessage={runResolveNotebook}
      onFetchSuccess={handleFetchSuccess}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}

const useNotebookDoc: CommandDoc = {
  name: '/use',
  usage: '/use <notebook title>',
  description:
    'Set the active notebook for book commands. Title must match one of your notebooks exactly (case-sensitive). Not found, auth, and network errors are shown as assistant errors.',
}

export const useNotebookSlashCommand: InteractiveSlashCommand = {
  literal: '/use',
  doc: useNotebookDoc,
  argument: { name: 'notebook title', optional: false },
  stageComponent: UseNotebookStage,
  stageIndicator: 'Notebook',
}
