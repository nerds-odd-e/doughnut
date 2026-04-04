import { describe, test, vi, beforeEach, afterEach } from 'vitest'

const { runMineruOutlineSubprocess } = vi.hoisted(() => ({
  runMineruOutlineSubprocess: vi.fn(),
}))

vi.mock('../src/commands/mineruOutline/mineruOutlineSubprocess.js', () => ({
  runMineruOutlineSubprocess,
}))

import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
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

  test('after nested plain line and /exit, root up-arrow recalls /exit not stale root prefix', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithTitle('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForFramesToInclude, waitForLastFrameToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Active notebook: Top Maths')
    stdin.write('nested-history-marker\r')
    await waitForFramesToInclude('Not supported')
    stdin.write('/exit\r')
    await waitForLastFrameToInclude('`exit` to quit.')

    stdin.write('\x1b[A')
    await waitForLastFrameToInclude('/exit')
  })

  describe('notebook stage /attach', () => {
    let attachBookSpy: ReturnType<typeof vi.spyOn> | undefined
    let attachWorkDir: string
    let attachPdfPath: string

    beforeEach(() => {
      attachWorkDir = fs.mkdtempSync(join(tmpdir(), 'cli-attach-test-'))
      attachPdfPath = join(attachWorkDir, 'stub.pdf')
      fs.writeFileSync(attachPdfPath, '')
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
      fs.rmSync(attachWorkDir, { recursive: true, force: true })
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
      stdin.write(`/attach ${attachPdfPath}\r`)
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
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude(
        'Access token does not have permission for this operation.'
      )
    })

    test('shows API validation message for HTTP 400 when present', async () => {
      attachBookSpy.mockRejectedValue({
        status: 400,
        message: 'Invalid layout roots',
      })

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('Invalid layout roots')
    })

    test('shows generic guidance for HTTP 400 without body message', async () => {
      attachBookSpy.mockRejectedValue({ status: 400 })

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude(
        'The server rejected this request. Check your input or try again in the web app.'
      )
    })

    test('shows not-found guidance for HTTP 404', async () => {
      attachBookSpy.mockRejectedValue({ status: 404 })

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude(
        'The resource was not found. It may have been removed, or the link is wrong.'
      )
    })

    test('shows server conflict message for HTTP 409', async () => {
      attachBookSpy.mockRejectedValue({
        status: 409,
        message: 'This notebook already has a book attached',
      })

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('This notebook already has a book attached')
    })

    test('shows outline subprocess error when MinerU script returns ok: false', async () => {
      runMineruOutlineSubprocess.mockResolvedValue({
        ok: false,
        error: 'outline script reported a parse failure',
      })

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('outline script reported a parse failure')
    })

    test('rejects attach when path is not a file', async () => {
      const dirNamedPdf = join(attachWorkDir, 'folder.pdf')
      fs.mkdirSync(dirNamedPdf)

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${dirNamedPdf}\r`)
      await waitForFramesToInclude(
        'Attach expects a PDF file path, not a directory.'
      )
    })

    test('rejects attach when PDF path is missing', async () => {
      const missing = join(attachWorkDir, 'missing.pdf')

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${missing}\r`)
      await waitForFramesToInclude('file not found or not readable:')
    })
  })
})
