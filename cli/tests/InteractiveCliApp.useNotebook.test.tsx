import * as fs from 'node:fs'
import { NotebookController } from 'donut-api'
import { afterEach, beforeEach, describe, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { renderInkWhenCommandLineReady } from './inkTestHelpers.js'
import { myNotebooksApiRow } from './myNotebooksApiRow.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'
import {
  openTopMathsNotebook,
  waitNotebookSlashGuidance,
} from './useNotebookInteractive.waits.js'

describe('InteractiveCliApp /use notebook shell', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let myNotebooksSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DONUT_CONFIG_DIR
    process.env.DONUT_CONFIG_DIR = configDir
    myNotebooksSpy = vi.spyOn(NotebookController, 'myNotebooks')
  })

  afterEach(() => {
    myNotebooksSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DONUT_CONFIG_DIR
    } else {
      process.env.DONUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('notebook stage: slash guidance, nested line, /exit history recall', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [myNotebooksApiRow('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)
    await openTopMathsNotebook(ink.stdin, ink)
    ink.stdin.write('/')
    await waitNotebookSlashGuidance(ink)
    ink.stdin.write('\x7f')
    await ink.waitUntilLastFrame((f) => !f.includes('→ /'))
    ink.stdin.write('nested-history-marker\r')
    await ink.waitForLastFrameToInclude('Not supported')
    ink.stdin.write('/exit\r')
    await ink.waitForLastFrameToInclude(/`exit` to quit\./)
    ink.stdin.write('\x1b[A')
    await ink.waitUntilLastFrame(
      (f) => f.includes('/exit') && !f.includes('→ /')
    )
  })
})
