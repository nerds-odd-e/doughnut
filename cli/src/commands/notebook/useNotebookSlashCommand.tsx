import {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { Spinner } from '@inkjs/ui'
import {
  NotebookController,
  type Notebook,
  type NotebooksViewedByUser,
} from 'doughnut-api'
import { SetStageKeyHandlerContext } from '../../commonUIComponents/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
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
  const abortControllerRef = useRef<AbortController | null>(null)
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)

  const handleStageInput = useCallback((input: string, key: Key) => {
    const isEscape = key.escape === true || input === '\u001b'
    if (!isEscape) return
    abortControllerRef.current?.abort()
  }, [])

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined || resolvedNotebook !== null) return
    setStageKeyHandler(handleStageInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleStageInput, resolvedNotebook])

  useInput(handleStageInput, {
    isActive: setStageKeyHandler === undefined && resolvedNotebook === null,
  })

  useEffect(() => {
    if (title === '') {
      onAbortWithError('No notebook title given.')
      return
    }

    const ac = new AbortController()
    abortControllerRef.current = ac
    let cancelled = false
    let finished = false

    const finishError = (message: string) => {
      if (cancelled || finished) return
      finished = true
      onAbortWithError(message)
    }

    const run = async () => {
      try {
        const view = await runDefaultBackendJson<NotebooksViewedByUser>(() =>
          NotebookController.myNotebooks({
            ...doughnutSdkOptions(ac.signal),
          })
        )
        if (cancelled || ac.signal.aborted) return

        const matches = view.notebooks.filter(
          (n: Notebook) => n.title === title
        )
        if (matches.length === 0) {
          finishError(NOTEBOOK_NOT_FOUND)
          return
        }
        if (matches.length > 1) {
          finishError(
            `Multiple notebooks match "${title}". Rename one in the web app so the title is unique.`
          )
          return
        }
        if (cancelled || ac.signal.aborted || finished) return
        finished = true
        setResolvedNotebook(matches[0])
      } catch (err: unknown) {
        if (cancelled) return
        finishError(userVisibleSlashCommandError(err))
      }
    }

    run().catch(() => undefined)

    return () => {
      cancelled = true
      ac.abort()
      abortControllerRef.current = null
    }
  }, [title, onAbortWithError])

  if (resolvedNotebook !== null) {
    return (
      <UseNotebookActiveShell
        notebook={resolvedNotebook}
        onSettled={onSettled}
      />
    )
  }

  return (
    <Box>
      <Spinner label="Loading notebooks…" />
    </Box>
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
