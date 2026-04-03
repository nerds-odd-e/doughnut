import * as fs from 'node:fs'
import { NotebookController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  pressEscapeAndWait,
  renderInkWhenCommandLineReady,
  stripAnsi,
} from './inkTestHelpers.js'

import { tempConfigWithToken } from './tempConfigTestHelpers.js'

function notebookWithTitle(title: string) {
  return makeMe.aNotebook.headNote(makeMe.aNote.title(title).please()).do()
}

describe('InteractiveCliApp /use notebook stage', () => {
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

  test('in notebook stage, / shows slash sub-command guidance', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithTitle('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForFramesToInclude, waitForLastFrameToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Active notebook: Top Maths')
    stdin.write('/')
    await waitForLastFrameToInclude('/exit, exit')
    await waitForLastFrameToInclude('Leave notebook context')
  })

  test('enters notebook stage then /exit clears stage indicator', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithTitle('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const {
      stdin,
      lastStrippedFrame,
      waitForLastFrameToInclude,
      waitForFramesToInclude,
    } = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Active notebook: Top Maths')
    stdin.write('/exit\r')
    await waitForLastFrameToInclude('Left notebook context.')
    await waitForLastFrameToInclude('`exit` to quit.')

    expect(lastStrippedFrame()).not.toContain('Active notebook: Top Maths')
  })

  test('unknown notebook title shows error and does not enter stage', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithTitle('Other')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, frames, waitForFramesToInclude, lastStrippedFrame } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Missing Notebook\r')
    await waitForFramesToInclude('No notebook found with that title.')

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('/use Missing Notebook')
    expect(combined).toContain('No notebook found with that title.')
    expect(combined).not.toContain('Active notebook: Missing Notebook')
    expect(lastStrippedFrame()).not.toContain('Active notebook:')
  })

  test('myNotebooks HTTP 401 maps to user-visible auth error', async () => {
    myNotebooksSpy.mockRejectedValue({ status: 401 })

    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
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

    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Loading notebooks')

    await pressEscapeAndWait(
      stdin,
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('/use Top Maths') &&
        stripAnsi(c).includes('Cancelled.')
    )

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('/use Top Maths')
    expect(combined).toContain('Cancelled.')
  })

  test('duplicate notebook titles show ambiguity error', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: {
        notebooks: [notebookWithTitle('Same'), notebookWithTitle('Same')],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Same\r')
    await waitForFramesToInclude('Multiple notebooks match')
    await waitForFramesToInclude('Same')
  })
})
