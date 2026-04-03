import * as fs from 'node:fs'
import { render } from 'ink-testing-library'
import { useStdout } from 'ink'
import { useCallback, useState } from 'react'
import { NotebookController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { useNotebookSlashCommand } from '../src/commands/notebook/useNotebookSlashCommand.js'
import { SlashCommandStageMount } from '../src/commands/slashCommandStageMount.js'
import { inkTerminalColumns } from '../src/terminalColumns.js'
import {
  SessionScrollbackSessionProvider,
  useSessionScrollbackAppend,
} from '../src/sessionScrollback/sessionScrollbackAppendContext.js'
import {
  extendInkRenderForInteractiveTests,
  inkCommandLineProbeUndelete,
  pressEscapeAndWait,
  StageKeyRoot,
  stripAnsi,
  waitForFrames,
} from './inkTestHelpers.js'

import { tempConfigWithToken } from './tempConfigTestHelpers.js'

function notebookWithTitle(title: string) {
  return makeMe.aNotebook.headNote(makeMe.aNote.title(title).please()).do()
}

function UseNotebookStageTestShell(props: { readonly argument: string }) {
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

function notebookStageTestAppElement(argument: string) {
  return (
    <SessionScrollbackSessionProvider initialItems={[]}>
      <StageKeyRoot>
        <UseNotebookStageTestShell argument={argument} />
      </StageKeyRoot>
    </SessionScrollbackSessionProvider>
  )
}

/** Mounts `useNotebookSlashCommand` stage with real scrollback + stage key forwarding; waits for the nested shell prompt. */
async function renderNotebookStageWhenPromptReady(notebookArgument: string) {
  const element = notebookStageTestAppElement(notebookArgument)
  const result = render(element)
  await waitForFrames(
    () => stripAnsi(result.frames.join('\n')),
    (c) => c.includes('Active notebook:')
  )
  await inkCommandLineProbeUndelete(result, {
    probeChar: '|',
    probeVisible: (f) => f.includes('→ |') || f.includes('> |'),
    probeHidden: (f) =>
      (f.includes('→') && !f.includes('→ |')) ||
      (f.includes('>') && !f.includes('> |')),
  })
  return { ...result, ...extendInkRenderForInteractiveTests(result) }
}

describe('useNotebookSlashCommand stage', () => {
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

  test('active stage /exit clears notebook shell and records assistant line', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithTitle('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, lastStrippedFrame, waitForFramesToInclude } =
      await renderNotebookStageWhenPromptReady('Top Maths')

    stdin.write('/exit\r')
    await waitForFramesToInclude('Left notebook context.')

    expect(lastStrippedFrame()).not.toContain('Active notebook: Top Maths')
  })

  test('unknown notebook title shows error and does not enter stage', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithTitle('Other')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const result = render(notebookStageTestAppElement('Missing Notebook'))
    const { frames, lastStrippedFrame, waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('No notebook found with that title.')

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('No notebook found with that title.')
    expect(combined).not.toContain('Active notebook: Missing Notebook')
    expect(lastStrippedFrame()).not.toContain('Active notebook:')
  })

  test('myNotebooks HTTP 401 maps to user-visible auth error', async () => {
    myNotebooksSpy.mockRejectedValue({ status: 401 })

    const result = render(notebookStageTestAppElement('Top Maths'))
    const { waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude(
      'Access token is invalid or expired. Run doughnut login or configure a new token.'
    )
  })

  test('Escape during notebook list load shows Cancelled when fetch honors signal', async () => {
    myNotebooksSpy.mockImplementation(
      async (options: { signal?: AbortSignal }) => {
        const { signal } = options
        if (signal === undefined) {
          throw new Error('expected AbortSignal from /use notebook resolve')
        }
        await new Promise<never>((_, reject) => {
          signal.addEventListener(
            'abort',
            () => {
              reject(new DOMException('Aborted', 'AbortError'))
            },
            { once: true }
          )
        })
      }
    )

    const result = render(notebookStageTestAppElement('Top Maths'))
    const { stdin, frames, waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('Loading notebooks')

    await pressEscapeAndWait(
      stdin,
      () => frames.join('\n'),
      (c) => stripAnsi(c).includes('Cancelled.')
    )

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('Cancelled.')
  })

  test('duplicate notebook titles show ambiguity error', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: {
        notebooks: [notebookWithTitle('Same'), notebookWithTitle('Same')],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const result = render(notebookStageTestAppElement('Same'))
    const { waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('Multiple notebooks match')
    await waitForFramesToInclude('Same')
  })
})
