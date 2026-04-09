import type { Stats } from 'node:fs'
import { stat } from 'node:fs/promises'
import { basename, extname, resolve } from 'node:path'
import { useEffect, useRef } from 'react'
import { Box } from 'ink'
import { Spinner } from '@inkjs/ui'
import type { BookBlockFull, Notebook } from 'doughnut-api'
import { attachNotebookBookWithPdf } from '../../backendApi/doughnutBackendClient.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'
import { runMineruOutlineSubprocess } from '../mineruOutline/mineruOutlineSubprocess.js'
import { truncateForBookOutlineAssistant } from '../mineruOutline/outlineAssistantLimits.js'

const attachNotebookDoc: CommandDoc = {
  name: '/attach',
  usage: '/attach <path to pdf>',
  description:
    'Attach a PDF to the active notebook (outline extraction and POST attach-book). Set DOUGHNUT_MINERU_PDF_END_PAGE to an inclusive last page index to cap large PDFs. Python: DOUGHNUT_MINERU_PYTHON; script: DOUGHNUT_MINERU_OUTLINE_SCRIPT.',
}

function pdfEndPageFromMineruEnv(): number | undefined {
  const raw = process.env.DOUGHNUT_MINERU_PDF_END_PAGE?.trim()
  if (raw === undefined || raw === '') return undefined
  const n = Number.parseInt(raw, 10)
  if (!Number.isFinite(n) || n < 0) return undefined
  return n
}

function normParentId(v: string | number | undefined | null): string | null {
  if (v === undefined || v === null || v === '') return null
  return String(v)
}

function bookBlocksTreeLines(blocks: BookBlockFull[] | undefined): string {
  if (blocks === undefined || blocks.length === 0) {
    return ''
  }
  const roots = blocks
    .filter((r) => normParentId(r.parentBlockId) === null)
    .sort((a, b) => Number(a.siblingOrder ?? 0) - Number(b.siblingOrder ?? 0))

  const lines: string[] = []
  function walk(nodes: BookBlockFull[], depth: number): void {
    const sorted = [...nodes].sort(
      (a, b) => Number(a.siblingOrder ?? 0) - Number(b.siblingOrder ?? 0)
    )
    for (const r of sorted) {
      lines.push(`${'  '.repeat(depth)}${r.title}`)
      const idStr = String(r.id)
      const children = blocks.filter(
        (x) => normParentId(x.parentBlockId) === idStr
      )
      walk(children, depth + 1)
    }
  }
  walk(roots, 0)
  return lines.join('\n')
}

async function runNotebookAttachPdf(
  notebook: Notebook,
  bookPath: string
): Promise<{ assistantMessage: string }> {
  const path = bookPath.trim()
  if (!path) {
    throw new Error(`Missing path to PDF. Usage: ${attachNotebookDoc.usage}`)
  }
  if (!path.toLowerCase().endsWith('.pdf')) {
    throw new Error('Attach only supports .pdf files.')
  }

  const absPdf = resolve(process.cwd(), path)
  let st: Stats
  try {
    st = await stat(absPdf)
  } catch {
    throw new Error(`file not found or not readable: ${absPdf}`)
  }
  if (!st.isFile()) {
    throw new Error('Attach expects a PDF file path, not a directory.')
  }

  const minerResult = await runMineruOutlineSubprocess({
    bookPath: path,
    pdfEndPage: pdfEndPageFromMineruEnv(),
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

  const book = await attachNotebookBookWithPdf(
    notebook.id,
    {
      bookName,
      format: 'pdf',
      layout: minerResult.layout,
    },
    absPdf
  )

  const tree = bookBlocksTreeLines(book.blocks)
  const excerpt = truncateForBookOutlineAssistant(
    tree === '' ? book.bookName : tree
  )
  return {
    assistantMessage: `Attached "${book.bookName}" to this notebook.\n\n${excerpt}`,
  }
}

export function attachNotebookSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function NotebookAttachStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const onSettledRef = useRef(onSettled)
    const onAbortWithErrorRef = useRef(onAbortWithError)
    onSettledRef.current = onSettled
    onAbortWithErrorRef.current = onAbortWithError

    useEffect(() => {
      let cancelled = false
      runNotebookAttachPdf(notebook, argument ?? '').then(
        ({ assistantMessage }) => {
          if (!cancelled) onSettledRef.current(assistantMessage)
        },
        (err: unknown) => {
          if (!cancelled)
            onAbortWithErrorRef.current(userVisibleSlashCommandError(err))
        }
      )
      return () => {
        cancelled = true
      }
      // argument is stable for the lifetime of a stage mount; callback refs handle the rest
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [argument])

    return (
      <Box>
        <Spinner label="Attaching PDF…" />
      </Box>
    )
  }

  return {
    literal: '/attach',
    doc: attachNotebookDoc,
    argument: { name: 'path to PDF', optional: false },
    stageComponent: NotebookAttachStage,
    stageIndicator: 'Attach',
  }
}
