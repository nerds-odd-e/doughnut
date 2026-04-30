import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'

const { runMineruOutlineSubprocess } = vi.hoisted(() => ({
  runMineruOutlineSubprocess: vi.fn(),
}))

vi.mock('../src/commands/mineruOutline/mineruOutlineSubprocess.js', () => ({
  runMineruOutlineSubprocess,
}))

import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve as pathResolve } from 'node:path'
import { NotebookController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import * as doughnutBackendClient from '../src/backendApi/doughnutBackendClient.js'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { renderInkWhenCommandLineReady } from './inkTestHelpers.js'

import { tempConfigWithToken } from './tempConfigTestHelpers.js'

function notebookWithName(name: string) {
  return makeMe.aNotebook.withSeedNote(makeMe.aNote.title(name).please()).do()
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
      data: { notebooks: [notebookWithName('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForFramesToInclude, waitForLastFrameToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Active notebook: Top Maths')
    stdin.write('/')
    await waitForLastFrameToInclude('/attach <path to .pdf or .epub>')
    await waitForLastFrameToInclude('Attach a book file to the active notebook')
    await waitForLastFrameToInclude('(POST attach-book)')
    await waitForLastFrameToInclude('/exit, exit')
    await waitForLastFrameToInclude('Leave notebook context')
  })

  test('after nested plain line and /exit, root up-arrow recalls /exit not stale root prefix', async () => {
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [notebookWithName('Top Maths')] },
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
    let attachBookSpy: ReturnType<typeof vi.spyOn>
    let attachWorkDir: string
    let attachPdfPath: string
    let attachEpubPath: string

    beforeEach(() => {
      attachWorkDir = fs.mkdtempSync(join(tmpdir(), 'cli-attach-test-'))
      attachPdfPath = join(attachWorkDir, 'stub.pdf')
      attachEpubPath = join(attachWorkDir, 'my-book.epub')
      fs.writeFileSync(attachPdfPath, '')
      fs.writeFileSync(attachEpubPath, '')
      myNotebooksSpy.mockResolvedValue({
        data: { notebooks: [notebookWithName('Top Maths')] },
      } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)
      attachBookSpy = vi.spyOn(doughnutBackendClient, 'attachNotebookBookFile')
      runMineruOutlineSubprocess.mockResolvedValue({
        ok: true,
        outline: 'o',
        source: 's',
        layout: {
          roots: [
            {
              title: 'Part One',
              children: [
                {
                  title: 'Part One Child',
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

    test('shows spinner while attach is pending', async () => {
      attachBookSpy.mockImplementation(() => new Promise(() => undefined))

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('Attaching book')
    })

    test('blocks input while attach spinner is visible', async () => {
      attachBookSpy.mockImplementation(() => new Promise(() => undefined))

      const { stdin, waitForFramesToInclude, waitForLastFrameToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('Attaching book')

      stdin.write('should-not-appear\r')
      await waitForLastFrameToInclude('Attaching book')
    })

    test('attaches PDF and shows structure excerpt from API book', async () => {
      attachBookSpy.mockResolvedValue(
        makeMe.aBook
          .id(99)
          .bookName('top-maths')
          .format('pdf')
          .blocks([
            makeMe.aBookBlock
              .id(1)
              .depth(0)
              .title('Part One')
              .contentLocators([])
              .do(),
            makeMe.aBookBlock
              .id(2)
              .depth(1)
              .title('Part One Child')
              .contentLocators([])
              .do(),
          ])
          .do()
      )

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('Attached "top-maths" to this notebook.')
      await waitForFramesToInclude('Part One')
      await waitForFramesToInclude('Part One Child')
    })

    test('attaches EPUB and shows structure excerpt from API book', async () => {
      attachBookSpy.mockResolvedValue(
        makeMe.aBook
          .id(100)
          .bookName('my-book')
          .format('epub')
          .blocks([
            makeMe.aBookBlock
              .id(1)
              .depth(0)
              .title('Chapter Alpha')
              .contentLocators([])
              .do(),
            makeMe.aBookBlock
              .id(2)
              .depth(0)
              .title('Chapter Beta')
              .contentLocators([])
              .do(),
          ])
          .do()
      )

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachEpubPath}\r`)
      await waitForFramesToInclude('Attached "my-book" to this notebook.')
      await waitForFramesToInclude('Chapter Alpha')
      await waitForFramesToInclude('Chapter Beta')
    })

    test('EPUB attach calls attachNotebookBookFile with epub metadata and resolved path', async () => {
      attachBookSpy.mockResolvedValue(
        makeMe.aBook.id(102).bookName('my-book').format('epub').do()
      )

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachEpubPath}\r`)
      await waitForFramesToInclude('Attached "my-book" to this notebook.')

      const expectedAbsPath = pathResolve(process.cwd(), attachEpubPath)
      expect(attachBookSpy).toHaveBeenCalledWith(
        expect.any(Number),
        { bookName: 'my-book', format: 'epub' },
        expectedAbsPath
      )
    })

    test('EPUB attach does not invoke MinerU', async () => {
      attachBookSpy.mockResolvedValue(
        makeMe.aBook.id(101).bookName('only-epub').format('epub').do()
      )

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachEpubPath}\r`)
      await waitForFramesToInclude('Attached "only-epub" to this notebook.')
      expect(runMineruOutlineSubprocess).not.toHaveBeenCalled()
    })

    test('shows user-visible error when attach-book fails', async () => {
      attachBookSpy.mockImplementation(() =>
        doughnutBackendClient.withBackendClient('t', async () => {
          throw { status: 403 }
        })
      )

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
      attachBookSpy.mockImplementation(() =>
        doughnutBackendClient.withBackendClient('t', async () => {
          throw { status: 400, message: 'Invalid layout roots' }
        })
      )

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${attachPdfPath}\r`)
      await waitForFramesToInclude('Invalid layout roots')
    })

    test('shows generic guidance for HTTP 400 without body message', async () => {
      attachBookSpy.mockImplementation(() =>
        doughnutBackendClient.withBackendClient('t', async () => {
          throw { status: 400 }
        })
      )

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
      attachBookSpy.mockImplementation(() =>
        doughnutBackendClient.withBackendClient('t', async () => {
          throw { status: 404 }
        })
      )

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
      attachBookSpy.mockImplementation(() =>
        doughnutBackendClient.withBackendClient('t', async () => {
          throw {
            status: 409,
            message: 'This notebook already has a book attached',
          }
        })
      )

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
        'Attach expects a book file path, not a directory.'
      )
    })

    test('rejects attach when extension is neither .pdf nor .epub', async () => {
      const txtPath = join(attachWorkDir, 'notes.txt')
      fs.writeFileSync(txtPath, 'x')

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${txtPath}\r`)
      await waitForFramesToInclude('Attach supports .pdf or .epub files.')
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

    test('rejects attach when EPUB path is missing', async () => {
      const missing = join(attachWorkDir, 'missing.epub')

      const { stdin, waitForFramesToInclude } =
        await renderInkWhenCommandLineReady(<InteractiveCliApp />)

      stdin.write('/use Top Maths\r')
      await waitForFramesToInclude('Active notebook: Top Maths')
      stdin.write(`/attach ${missing}\r`)
      await waitForFramesToInclude('file not found or not readable:')
    })
  })
})
