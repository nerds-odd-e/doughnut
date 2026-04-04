import { describe, test, vi, beforeEach, afterEach } from 'vitest'

const { runMineruOutlineSubprocess } = vi.hoisted(() => ({
  runMineruOutlineSubprocess: vi.fn(),
}))

vi.mock('../src/commands/read/mineruOutlineSubprocess.js', () => ({
  runMineruOutlineSubprocess,
}))

import * as fs from 'node:fs'
import { NotebookBooksController, NotebookController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
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
    runMineruOutlineSubprocess.mockReset()
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
    await waitForLastFrameToInclude('/attach <path to pdf>')
    await waitForLastFrameToInclude('Attach a PDF to the active notebook')
    await waitForLastFrameToInclude('/exit, exit')
    await waitForLastFrameToInclude('Leave notebook context')
  })

  describe('notebook stage /attach', () => {
    let attachBookSpy: ReturnType<typeof vi.spyOn> | undefined

    beforeEach(() => {
      myNotebooksSpy.mockResolvedValue({
        data: { notebooks: [notebookWithTitle('Top Maths')] },
      } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)
      attachBookSpy = vi.spyOn(NotebookBooksController, 'attachBook')
      runMineruOutlineSubprocess.mockResolvedValue({
        ok: true,
        outline: 'o',
        source: 's',
        layout: {
          roots: [
            {
              title: 'Part One',
              startAnchor: {
                anchorFormat: 'pdf.mineru_outline_v1',
                value: '{}',
              },
              endAnchor: {
                anchorFormat: 'pdf.mineru_outline_v1',
                value: '{}',
              },
              children: [
                {
                  title: 'Part One Child',
                  startAnchor: {
                    anchorFormat: 'pdf.mineru_outline_v1',
                    value: '{}',
                  },
                  endAnchor: {
                    anchorFormat: 'pdf.mineru_outline_v1',
                    value: '{}',
                  },
                },
              ],
            },
          ],
        },
      })
    })

    afterEach(() => {
      attachBookSpy?.mockRestore()
    })

    test('attaches PDF and shows structure excerpt from API book', async () => {
      attachBookSpy.mockResolvedValue({
        data: {
          id: 99,
          bookName: 'top-maths',
          format: 'pdf',
          ranges: [
            { id: 1, title: 'Part One', siblingOrder: 0 },
            {
              id: 2,
              title: 'Part One Child',
              siblingOrder: 0,
              parentRangeId: '1',
            },
          ],
        },
      } as Awaited<ReturnType<typeof NotebookBooksController.attachBook>>)

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write('/attach /tmp/foo.pdf\r')
      await waitForFramesToInclude('Attached "top-maths" to this notebook.')
      await waitForFramesToInclude('Part One')
      await waitForFramesToInclude('Part One Child')
    })

    test('shows user-visible error when attach-book fails', async () => {
      attachBookSpy.mockRejectedValue({ status: 403 })

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write('/attach /tmp/foo.pdf\r')
      await waitForFramesToInclude(
        'Access token does not have permission for this operation.'
      )
    })
  })
})
