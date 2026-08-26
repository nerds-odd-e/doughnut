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
import { NotebookController } from 'donut-api'
import makeMe from 'donut-test-fixtures/makeMe'
import * as doughnutBackendClient from '../src/backendApi/doughnutBackendClient.js'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { renderInkWhenCommandLineReady } from './inkTestHelpers.js'
import { myNotebooksApiRow } from './myNotebooksApiRow.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'
import { openTopMathsNotebook } from './useNotebookInteractive.waits.js'

describe('InteractiveCliApp /use notebook /attach', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let myNotebooksSpy: ReturnType<typeof vi.spyOn>
  let attachBookSpy: ReturnType<typeof vi.spyOn>
  let attachWorkDir: string
  let attachPdfPath: string
  let attachEpubPath: string

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DONUT_CONFIG_DIR
    process.env.DONUT_CONFIG_DIR = configDir
    myNotebooksSpy = vi.spyOn(NotebookController, 'myNotebooks')
    runMineruOutlineSubprocess.mockReset()

    attachWorkDir = fs.mkdtempSync(join(tmpdir(), 'cli-attach-test-'))
    attachPdfPath = join(attachWorkDir, 'stub.pdf')
    attachEpubPath = join(attachWorkDir, 'my-book.epub')
    fs.writeFileSync(attachPdfPath, '')
    fs.writeFileSync(attachEpubPath, '')
    myNotebooksSpy.mockResolvedValue({
      data: { notebooks: [myNotebooksApiRow('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)
    attachBookSpy = vi.spyOn(doughnutBackendClient, 'attachNotebookBookFile')
    runMineruOutlineSubprocess.mockResolvedValue({
      ok: true,
      outline: 'o',
      source: 's',
      bookLayout: {
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
    myNotebooksSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DONUT_CONFIG_DIR
    } else {
      process.env.DONUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
    fs.rmSync(attachWorkDir, { recursive: true, force: true })
  })

  async function attachAndExpect(path: string, expected: string | RegExp) {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)
    await openTopMathsNotebook(ink.stdin, ink)
    ink.stdin.write(`/attach ${path}\r`)
    await ink.waitForLastFrameToInclude(expected)
  }

  test('shows attach spinner and ignores input until attach completes', async () => {
    attachBookSpy.mockImplementation(() => new Promise(() => undefined))

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)
    await openTopMathsNotebook(ink.stdin, ink)
    ink.stdin.write(`/attach ${attachPdfPath}\r`)
    await ink.waitForLastFrameToInclude('Attaching book')
    ink.stdin.write('should-not-appear\r')
    await ink.waitUntilLastFrame(
      (f) => f.includes('Attaching book') && !f.includes('should-not-appear')
    )
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

    await attachAndExpect(
      attachPdfPath,
      /(?=.*Attached "top-maths" to this notebook\.)(?=.*Part One)(?=.*Part One Child)/s
    )
  })

  test('attaches EPUB with API metadata, structure excerpt, and no MinerU', async () => {
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

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)
    await openTopMathsNotebook(ink.stdin, ink)
    ink.stdin.write(`/attach ${attachEpubPath}\r`)
    await ink.waitUntilLastFrame(
      (f) =>
        f.includes('Attached "my-book" to this notebook.') &&
        f.includes('Chapter Alpha') &&
        f.includes('Chapter Beta')
    )

    expect(attachBookSpy).toHaveBeenCalledWith(
      expect.any(Number),
      { bookName: 'my-book', format: 'epub' },
      pathResolve(process.cwd(), attachEpubPath)
    )
    expect(runMineruOutlineSubprocess).not.toHaveBeenCalled()
  })

  test.each([
    [
      'HTTP 403',
      { status: 403 },
      'Access token does not have permission for this operation.',
    ],
    [
      'HTTP 400 with body',
      { status: 400, message: 'Invalid layout roots' },
      'Invalid layout roots',
    ],
    [
      'HTTP 400 without body',
      { status: 400 },
      'The server rejected this request. Check your input or try again in the web app.',
    ],
    [
      'HTTP 404',
      { status: 404 },
      'The resource was not found. It may have been removed, or the link is wrong.',
    ],
    [
      'HTTP 409',
      { status: 409, message: 'This notebook already has a book attached' },
      'This notebook already has a book attached',
    ],
  ] as const)(
    'shows user-visible error for %s',
    async (_label, error, message) => {
      attachBookSpy.mockImplementation(() =>
        doughnutBackendClient.withBackendClient('t', async () => {
          throw error
        })
      )
      await attachAndExpect(attachPdfPath, message)
    }
  )

  test('shows outline subprocess error when MinerU script returns ok: false', async () => {
    runMineruOutlineSubprocess.mockResolvedValue({
      ok: false,
      error: 'outline script reported a parse failure',
    })
    await attachAndExpect(
      attachPdfPath,
      'outline script reported a parse failure'
    )
  })

  test('rejects attach when path is not a file', async () => {
    const dirNamedPdf = join(attachWorkDir, 'folder.pdf')
    fs.mkdirSync(dirNamedPdf)
    await attachAndExpect(
      dirNamedPdf,
      'Attach expects a book file path, not a directory.'
    )
  })

  test('rejects attach when extension is neither .pdf nor .epub', async () => {
    const txtPath = join(attachWorkDir, 'notes.txt')
    fs.writeFileSync(txtPath, 'x')
    await attachAndExpect(txtPath, 'Attach supports .pdf or .epub files.')
  })

  test('rejects attach when book file path is missing', async () => {
    await attachAndExpect(
      join(attachWorkDir, 'missing.pdf'),
      'file not found or not readable:'
    )
  })
})
