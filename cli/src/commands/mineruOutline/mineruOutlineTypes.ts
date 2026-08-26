import type { AttachBookLayoutRequestFull } from 'donut-api'

export const MINERU_OUTLINE_DEFAULT_TIMEOUT_MS = 30 * 60 * 1000

export type MineruOutlineOk = {
  ok: true
  outline: string
  source: string
  note?: string
  /** Present when stdout JSON includes a valid attach-book `bookLayout`. */
  bookLayout?: AttachBookLayoutRequestFull
  /** MinerU content_list array; server builds layout (PDF content_list path). */
  contentList?: unknown[]
}

export type MineruOutlineErr = {
  ok: false
  error: string
  exitCode?: number | null
}

export type MineruOutlineResult = MineruOutlineOk | MineruOutlineErr

export type RunMineruOutlineOptions = {
  bookPath: string
  cwd?: string
  pythonExecutable?: string
  scriptPath?: string
  pdfStartPage?: number
  pdfEndPage?: number | null
  timeoutMs?: number
}
