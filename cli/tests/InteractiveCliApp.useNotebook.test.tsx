import * as fs from 'node:fs'
import { NotebookController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { renderInkWhenCommandLineReady } from './inkTestHelpers.js'

import { tempConfigWithToken } from './tempConfigTestHelpers.js'

function notebookWithTitle(title: string) {
  return makeMe.aNotebook.headNote(makeMe.aNote.title(title).please()).do()
}

describe('InteractiveCliApp /use notebook integration', () => {
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

  test('full app: /use opens notebook stage; / shows nested slash guidance', async () => {
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
})
