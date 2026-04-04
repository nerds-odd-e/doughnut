import { basename, extname } from 'node:path'
import {
  NotebookBooksController,
  type BookFull,
  type BookRangeFull,
  type Notebook,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { runMineruOutlineSubprocess } from '../read/mineruOutlineSubprocess.js'
import { READ_OUTLINE_ASSISTANT_MAX_CHARS } from '../read/readSlashCommand.js'

const attachNotebookDoc: CommandDoc = {
  name: '/attach',
  usage: '/attach <path to pdf>',
  description:
    'Attach a PDF to the active notebook (outline extraction and POST attach-book).',
}

function pdfEndPageFromReadEnv(): number | undefined {
  const raw = process.env.DOUGHNUT_READ_PDF_END_PAGE?.trim()
  if (raw === undefined || raw === '') return undefined
  const n = Number.parseInt(raw, 10)
  if (!Number.isFinite(n) || n < 0) return undefined
  return n
}

function truncateForAssistant(text: string): string {
  if (text.length <= READ_OUTLINE_ASSISTANT_MAX_CHARS) return text
  return `${text.slice(0, READ_OUTLINE_ASSISTANT_MAX_CHARS)}…`
}

function normParentId(v: string | number | undefined | null): string | null {
  if (v === undefined || v === null || v === '') return null
  return String(v)
}

function bookRangesTreeLines(ranges: BookRangeFull[] | undefined): string {
  if (ranges === undefined || ranges.length === 0) {
    return ''
  }
  const roots = ranges
    .filter((r) => normParentId(r.parentRangeId) === null)
    .sort((a, b) => Number(a.siblingOrder ?? 0) - Number(b.siblingOrder ?? 0))

  const lines: string[] = []
  function walk(nodes: BookRangeFull[], depth: number): void {
    const sorted = [...nodes].sort(
      (a, b) => Number(a.siblingOrder ?? 0) - Number(b.siblingOrder ?? 0)
    )
    for (const r of sorted) {
      lines.push(`${'  '.repeat(depth)}${r.title}`)
      const idStr = String(r.id)
      const children = ranges.filter(
        (x) => normParentId(x.parentRangeId) === idStr
      )
      walk(children, depth + 1)
    }
  }
  walk(roots, 0)
  return lines.join('\n')
}

function attachSlashCommandFor(notebook: Notebook): InteractiveSlashCommand {
  return {
    literal: '/attach',
    doc: attachNotebookDoc,
    argument: { name: 'path to PDF', optional: false },
    async run(bookPath) {
      const path = bookPath?.trim()
      if (!path) {
        throw new Error(
          `Missing path to PDF. Usage: ${attachNotebookDoc.usage}`
        )
      }
      if (!path.toLowerCase().endsWith('.pdf')) {
        throw new Error('Attach only supports .pdf files.')
      }

      const minerResult = await runMineruOutlineSubprocess({
        bookPath: path,
        pdfEndPage: pdfEndPageFromReadEnv(),
      })
      if (!minerResult.ok) {
        throw new Error(minerResult.error)
      }
      if (
        minerResult.layout === undefined ||
        minerResult.layout.roots.length === 0
      ) {
        throw new Error(
          'Outline script did not return layout.roots; attach requires nested layout JSON from the MinerU script.'
        )
      }

      const ext = extname(path)
      const bookName =
        ext.toLowerCase() === '.pdf' ? basename(path, ext) : basename(path)

      const book = await runDefaultBackendJson<BookFull>(() =>
        NotebookBooksController.attachBook({
          ...doughnutSdkOptions(),
          path: { notebook: notebook.id },
          body: {
            bookName,
            format: 'pdf',
            layout: minerResult.layout,
          },
        })
      )

      const tree = bookRangesTreeLines(book.ranges)
      const excerpt = truncateForAssistant(tree === '' ? book.bookName : tree)
      const assistantMessage = `Attached "${book.bookName}" to this notebook.\n\n${excerpt}`
      return { assistantMessage }
    },
  }
}

const leaveNotebookDoc: CommandDoc = {
  name: '/exit',
  usage: '/exit, exit',
  description: 'Leave notebook context',
}

export const leaveNotebookStageSlashCommand: InteractiveSlashCommand = {
  literal: '/exit',
  doc: leaveNotebookDoc,
  run() {
    return { assistantMessage: 'Left notebook context.' }
  },
}

export function notebookStageSlashCommandsFor(
  notebook: Notebook
): readonly InteractiveSlashCommand[] {
  return [attachSlashCommandFor(notebook), leaveNotebookStageSlashCommand]
}
