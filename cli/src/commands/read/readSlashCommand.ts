import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { runMineruOutlineSubprocess } from './mineruOutlineSubprocess.js'

/** Spike: keep scrollback bounded for huge outlines (TTY safety). */
export const READ_OUTLINE_ASSISTANT_MAX_CHARS = 12_000

const readDoc: CommandDoc = {
  name: '/read',
  usage: '/read <path to book>',
  description:
    "Print a top-three-layer outline from a local .pdf or .epub. PDF uses MinerU (pip install 'mineru[pipeline]' for the pipeline backend; first run may download models). EPUB uses spine order and HTML h1–h3 only. Layers: MinerU text_level 1–3 / middle.json fallback on PDF; h1–h3 on EPUB. Set DOUGHNUT_READ_PDF_END_PAGE to an inclusive last page index to cap PDF processing. Python: DOUGHNUT_MINERU_PYTHON; script: DOUGHNUT_MINERU_OUTLINE_SCRIPT.",
}

function truncateForAssistant(text: string): string {
  if (text.length <= READ_OUTLINE_ASSISTANT_MAX_CHARS) return text
  return `${text.slice(0, READ_OUTLINE_ASSISTANT_MAX_CHARS)}…`
}

function pdfEndPageFromReadEnv(): number | undefined {
  const raw = process.env.DOUGHNUT_READ_PDF_END_PAGE?.trim()
  if (raw === undefined || raw === '') return undefined
  const n = Number.parseInt(raw, 10)
  if (!Number.isFinite(n) || n < 0) return undefined
  return n
}

export const readSlashCommand: InteractiveSlashCommand = {
  literal: '/read',
  doc: readDoc,
  argument: { name: 'path to book', optional: false },
  async run(bookPath) {
    const path = bookPath?.trim()
    if (!path) {
      throw new Error(`Missing path to book. Usage: ${readDoc.usage}`)
    }
    const result = await runMineruOutlineSubprocess({
      bookPath: path,
      pdfEndPage: pdfEndPageFromReadEnv(),
    })
    if (!result.ok) {
      throw new Error(result.error)
    }
    let body = result.outline
    if (result.note !== undefined && result.note !== '') {
      body = body ? `${body}\n\n${result.note}` : result.note
    }
    return { assistantMessage: truncateForAssistant(body) }
  },
}
