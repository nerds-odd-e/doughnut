import * as fs from 'node:fs'
import { render } from 'ink-testing-library'
import { useStdout } from 'ink'
import { useCallback, useState } from 'react'
import { NotebookController } from 'doughnut-api'
import { afterEach, beforeEach, vi } from 'vitest'
import { useNotebookSlashCommand } from '../src/commands/notebook/useNotebookSlashCommand.js'
import { SlashCommandStageMount } from '../src/commands/slashCommandStageMount.js'
import { InputHistoryProvider } from '../src/inputHistory/index.js'
import { inkTerminalColumns } from '../src/terminalColumns.js'
import {
  SessionScrollbackSessionProvider,
  useSessionScrollbackAppend,
} from '../src/sessionScrollback/sessionScrollbackAppendContext.js'
import {
  extendInkRenderForInteractiveTests,
  StageKeyRoot,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'
import { waitNotebookPickerVisible } from './useNotebookSlashCommand.waits.js'

function UseNotebookStageTestShell(props: { readonly argument?: string }) {
  const { argument } = props
  const [stageOpen, setStageOpen] = useState(true)
  const { stdout } = useStdout()
  const cols = inkTerminalColumns(stdout.columns)
  const { appendScrollbackAssistantTextMessage, appendScrollbackError } =
    useSessionScrollbackAppend()

  const handleStageSettled = useCallback(
    (assistantText: string) => {
      if (assistantText !== '') {
        appendScrollbackAssistantTextMessage(assistantText)
      }
      setStageOpen(false)
    },
    [appendScrollbackAssistantTextMessage]
  )

  const handleStageAbortWithError = useCallback(
    (message: string) => {
      if (message !== '') {
        appendScrollbackError(message)
      }
      setStageOpen(false)
    },
    [appendScrollbackError]
  )

  const Stage = useNotebookSlashCommand.stageComponent
  if (!stageOpen) {
    return null
  }
  return (
    <SlashCommandStageMount
      cols={cols}
      stageIndicator={useNotebookSlashCommand.stageIndicator}
      Stage={Stage}
      stageProps={{
        argument,
        onSettled: handleStageSettled,
        onAbortWithError: handleStageAbortWithError,
      }}
    />
  )
}

export function notebookStageTestAppElement(argument?: string) {
  return (
    <SessionScrollbackSessionProvider initialItems={[]}>
      <InputHistoryProvider>
        <StageKeyRoot>
          <UseNotebookStageTestShell argument={argument} />
        </StageKeyRoot>
      </InputHistoryProvider>
    </SessionScrollbackSessionProvider>
  )
}

export async function renderNotebookStageWhenPickerVisible() {
  const result = render(notebookStageTestAppElement(undefined))
  const ink = extendInkRenderForInteractiveTests(result)
  await waitNotebookPickerVisible(ink)
  return { ...result, ...ink }
}

export function installUseNotebookStageConfig() {
  let configDir: string
  let savedConfigDir: string | undefined
  let myNotebooksSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    myNotebooksSpy = vi.spyOn(NotebookController, 'myNotebooks')
  })

  afterEach(() => {
    myNotebooksSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  return {
    getMyNotebooksSpy: () => myNotebooksSpy,
  }
}
