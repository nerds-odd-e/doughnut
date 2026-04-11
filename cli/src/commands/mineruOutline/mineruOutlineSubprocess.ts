import { spawn } from 'node:child_process'
import { constants as fsConstants, existsSync } from 'node:fs'
import { access, mkdtemp, rm } from 'node:fs/promises'
import type { AttachBookLayoutRequestFull } from 'doughnut-api'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { materializeEmbeddedMineruOutlineScript } from './embeddedMineruOutlineScript.js'

export const MINERU_OUTLINE_DEFAULT_TIMEOUT_MS = 30 * 60 * 1000

/** Cap stderr bytes included in user-visible errors (tracebacks are usually < this). */
const MINERU_STDERR_EXCERPT_CHARS = 12_000

/** After MinerU import hint, keep only a short stderr tail (avoid huge tracebacks). */
const MINERU_IMPORT_DETAIL_MAX_CHARS = 800

const MINERU_IMPORT_HINT =
  "MinerU is missing or could not be imported. Install MinerU for PDF outlines (e.g. pip install 'mineru[pipeline]' in the Python environment used by the CLI), or set DOUGHNUT_MINERU_OUTLINE_SCRIPT to a different outline script."

export type MineruOutlineOk = {
  ok: true
  outline: string
  source: string
  note?: string
  /** Present when stdout JSON includes a valid attach-book `layout`. */
  layout?: AttachBookLayoutRequestFull
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

function resolveDefaultScriptPath(startDir: string): string | null {
  let dir = resolve(startDir)
  for (;;) {
    const candidate = join(dir, 'cli', 'python', 'mineru_book_outline.py')
    if (existsSync(candidate)) {
      return candidate
    }
    const parent = dirname(dir)
    if (parent === dir) {
      return null
    }
    dir = parent
  }
}

type SpawnOutcome = {
  code: number | null
  signal: NodeJS.Signals | null
  stdout: string
  stderr: string
  timedOut: boolean
}

function spawnWithTimeout(
  command: string,
  args: string[],
  timeoutMs: number,
  cwd?: string
): Promise<SpawnOutcome> {
  return new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(command, args, {
      cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
    })
    const outChunks: Buffer[] = []
    const errChunks: Buffer[] = []
    child.stdout?.on('data', (c: Buffer) => {
      outChunks.push(c)
    })
    child.stderr?.on('data', (c: Buffer) => {
      errChunks.push(c)
    })

    let timedOut = false
    const timer = setTimeout(() => {
      timedOut = true
      child.kill('SIGTERM')
    }, timeoutMs)

    let exitCode: number | null = null
    let exitSignal: NodeJS.Signals | null = null

    const finish = () => {
      clearTimeout(timer)
      resolvePromise({
        code: exitCode,
        signal: exitSignal,
        stdout: Buffer.concat(outChunks).toString('utf8'),
        stderr: Buffer.concat(errChunks).toString('utf8'),
        timedOut,
      })
    }

    child.on('error', (err) => {
      clearTimeout(timer)
      rejectPromise(err)
    })
    child.on('close', (code, signal) => {
      exitCode = code
      exitSignal = signal ?? null
      finish()
    })
  })
}

function errnoCode(err: unknown): string | undefined {
  if (err !== null && typeof err === 'object' && 'code' in err) {
    const c = (err as { code: unknown }).code
    return typeof c === 'string' ? c : undefined
  }
  return undefined
}

function messageForPythonSpawnFailure(
  err: unknown,
  pythonExecutable: string
): string {
  const code = errnoCode(err)
  const msg = err instanceof Error ? err.message : String(err)
  if (code === 'ENOENT') {
    return `Could not run "${pythonExecutable}" (not found on PATH or missing interpreter). Install Python 3 and ensure it is on PATH, or set DOUGHNUT_MINERU_PYTHON to the full path of your python3 binary.`
  }
  if (code === 'EACCES') {
    return `Permission denied when starting "${pythonExecutable}". Check that the file is executable, or choose another interpreter via DOUGHNUT_MINERU_PYTHON.`
  }
  return `Failed to start outline extraction (${pythonExecutable}): ${msg}`
}

type SpawnOk = { tag: 'spawned'; outcome: SpawnOutcome }

async function runMineruSpawn(
  python: string,
  args: string[],
  timeoutMs: number,
  cwd: string
): Promise<SpawnOk | MineruOutlineErr> {
  try {
    const outcome = await spawnWithTimeout(python, args, timeoutMs, cwd)
    return { tag: 'spawned', outcome }
  } catch (err) {
    return {
      ok: false,
      error: messageForPythonSpawnFailure(err, python),
    }
  }
}

function isSpawnOk(r: SpawnOk | MineruOutlineErr): r is SpawnOk {
  return 'tag' in r && r.tag === 'spawned'
}

function boundedStderrExcerpt(stderr: string): string {
  const err = stderr.trim()
  if (err.length <= MINERU_STDERR_EXCERPT_CHARS) {
    return err
  }
  const half = MINERU_STDERR_EXCERPT_CHARS / 2
  return `${err.slice(0, half)}\n…\n${err.slice(-half)}`
}

function looksLikeMineruImportFailure(stderr: string): boolean {
  return (
    /ModuleNotFoundError/.test(stderr) ||
    /No module named ['"]?mineru/.test(stderr) ||
    (/ImportError/.test(stderr) && /mineru/.test(stderr))
  )
}

/** User-visible stderr body; applies MinerU import hint when stderr matches. */
function stderrBodyForUserMessage(stderr: string): string {
  const trimmed = stderr.trim()
  if (trimmed === '') {
    return ''
  }
  if (looksLikeMineruImportFailure(trimmed)) {
    const detail = boundedStderrExcerpt(trimmed).slice(
      0,
      MINERU_IMPORT_DETAIL_MAX_CHARS
    )
    return `${MINERU_IMPORT_HINT}\n\nDetails:\n${detail}`
  }
  return boundedStderrExcerpt(trimmed)
}

function timeoutErrorMessage(timeoutMs: number, isPdf: boolean): string {
  const base = `MinerU outline subprocess timed out after ${timeoutMs}ms`
  if (!isPdf) {
    return base
  }
  return `${base} For large PDFs, set DOUGHNUT_MINERU_PDF_END_PAGE to cap the page range.`
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v)
}

function validateLayoutNode(node: unknown): string | null {
  if (!isRecord(node)) {
    return 'each layout node must be an object'
  }
  if (typeof node.title !== 'string' || node.title.trim() === '') {
    return 'each layout node needs a non-empty title'
  }
  if (node.children !== undefined) {
    if (!Array.isArray(node.children)) {
      return 'layout node children must be an array'
    }
    for (const child of node.children) {
      const err = validateLayoutNode(child)
      if (err !== null) {
        return err
      }
    }
  }
  return null
}

function parseContentListFromStdoutJson(
  raw: Record<string, unknown>
): { contentList: unknown[] } | { error: string } | { omit: true } {
  if (!('contentList' in raw)) {
    return { omit: true }
  }
  const cl = raw.contentList
  if (!Array.isArray(cl)) {
    return { error: 'contentList must be an array' }
  }
  if (cl.length === 0) {
    return { error: 'contentList must be a non-empty array' }
  }
  for (const item of cl) {
    if (!isRecord(item)) {
      return { error: 'each contentList item must be an object' }
    }
  }
  return { contentList: cl }
}

function parseLayoutFromStdoutJson(
  raw: Record<string, unknown>
):
  | { layout: AttachBookLayoutRequestFull }
  | { error: string }
  | { omit: true } {
  if (!('layout' in raw)) {
    return { omit: true }
  }
  const layoutRaw = raw.layout
  if (!isRecord(layoutRaw)) {
    return { error: 'layout must be an object' }
  }
  const roots = layoutRaw.roots
  if (!Array.isArray(roots) || roots.length === 0) {
    return { error: 'layout.roots must be a non-empty array' }
  }
  for (const root of roots) {
    const err = validateLayoutNode(root)
    if (err !== null) {
      return { error: err }
    }
  }
  return { layout: layoutRaw as AttachBookLayoutRequestFull }
}

function parseAttachPayloadFromStdoutJson(
  raw: Record<string, unknown>
):
  | { layout: AttachBookLayoutRequestFull }
  | { contentList: unknown[] }
  | { error: string }
  | { omit: true } {
  const clParsed = parseContentListFromStdoutJson(raw)
  const layoutParsed = parseLayoutFromStdoutJson(raw)
  const clKey = 'contentList' in raw
  const layoutKey = 'layout' in raw

  if (clKey && layoutKey) {
    const clOk = 'contentList' in clParsed
    const loOk = 'layout' in layoutParsed
    if (clOk && loOk) {
      return {
        error: 'cannot send both layout and contentList in outline JSON',
      }
    }
  }

  if (clKey && 'error' in clParsed) {
    return clParsed
  }
  if (layoutKey && 'error' in layoutParsed) {
    return layoutParsed
  }
  if ('contentList' in clParsed) {
    return clParsed
  }
  if ('layout' in layoutParsed) {
    return layoutParsed
  }
  return { omit: true }
}

function toMineruResult(raw: unknown): MineruOutlineResult | null {
  if (!isRecord(raw)) {
    return null
  }
  if (raw.ok === true) {
    const outline = typeof raw.outline === 'string' ? raw.outline.trim() : ''
    const source = typeof raw.source === 'string' ? raw.source : ''
    const note = typeof raw.note === 'string' ? raw.note : undefined
    const attachParsed = parseAttachPayloadFromStdoutJson(raw)
    if ('error' in attachParsed) {
      return { ok: false, error: attachParsed.error }
    }
    const out: MineruOutlineOk = { ok: true, outline, source }
    if (note !== undefined) {
      out.note = note
    }
    if ('layout' in attachParsed) {
      out.layout = attachParsed.layout
    }
    if ('contentList' in attachParsed) {
      out.contentList = attachParsed.contentList
    }
    return out
  }
  if (raw.ok === false && typeof raw.error === 'string') {
    return { ok: false, error: raw.error }
  }
  return null
}

export async function runMineruOutlineSubprocess(
  options: RunMineruOutlineOptions
): Promise<MineruOutlineResult> {
  const cwd = options.cwd ?? process.cwd()
  const bookPath = resolve(cwd, options.bookPath)
  try {
    await access(bookPath, fsConstants.R_OK)
  } catch {
    return { ok: false, error: `file not found or not readable: ${bookPath}` }
  }

  const scriptPath =
    options.scriptPath ??
    process.env.DOUGHNUT_MINERU_OUTLINE_SCRIPT ??
    resolveDefaultScriptPath(cwd) ??
    materializeEmbeddedMineruOutlineScript()

  const python =
    options.pythonExecutable ?? process.env.DOUGHNUT_MINERU_PYTHON ?? 'python3'
  const timeoutMs = options.timeoutMs ?? MINERU_OUTLINE_DEFAULT_TIMEOUT_MS
  const ext = bookPath.toLowerCase().endsWith('.pdf')
    ? 'pdf'
    : bookPath.toLowerCase().endsWith('.epub')
      ? 'epub'
      : null
  if (!ext) {
    return { ok: false, error: 'expected .pdf or .epub' }
  }

  const args = [scriptPath, bookPath, '--json-result']
  if (ext === 'pdf') {
    const tmpDir = await mkdtemp(join(tmpdir(), 'doughnut-mineru-out-'))
    args.push('--output-dir', tmpDir)
    const start = options.pdfStartPage ?? 0
    if (start !== 0) {
      args.push('--start-page', String(start))
    }
    if (options.pdfEndPage != null) {
      args.push('--end-page', String(options.pdfEndPage))
    }
    try {
      const r = await runMineruSpawn(python, args, timeoutMs, cwd)
      if (!isSpawnOk(r)) {
        return r
      }
      const spawned = r.outcome
      if (spawned.timedOut) {
        return {
          ok: false,
          error: timeoutErrorMessage(timeoutMs, true),
          exitCode: spawned.code,
        }
      }
      return mapSpawnOutcome(spawned)
    } finally {
      await rm(tmpDir, { recursive: true, force: true }).catch(() => undefined)
    }
  }

  const r = await runMineruSpawn(python, args, timeoutMs, cwd)
  if (!isSpawnOk(r)) {
    return r
  }
  const spawned = r.outcome
  if (spawned.timedOut) {
    return {
      ok: false,
      error: timeoutErrorMessage(timeoutMs, false),
      exitCode: spawned.code,
    }
  }
  return mapSpawnOutcome(spawned)
}

function mapSpawnOutcome(spawned: SpawnOutcome): MineruOutlineResult {
  const trimmedOut = spawned.stdout.trim()
  let parsed: unknown
  try {
    if (!trimmedOut) {
      const body = stderrBodyForUserMessage(spawned.stderr)
      return {
        ok: false,
        error: body || `empty stdout (exit ${spawned.code ?? 'unknown'})`,
        exitCode: spawned.code,
      }
    }
    parsed = JSON.parse(trimmedOut) as unknown
  } catch {
    const body = stderrBodyForUserMessage(spawned.stderr)
    return {
      ok: false,
      error: body
        ? `invalid JSON on stdout; stderr:\n${body}`
        : 'invalid JSON on stdout',
      exitCode: spawned.code,
    }
  }

  const fromJson = toMineruResult(parsed)
  if (!fromJson) {
    return {
      ok: false,
      error: 'stdout JSON did not match expected shape',
      exitCode: spawned.code,
    }
  }
  if (fromJson.ok) {
    if (spawned.code !== 0) {
      return {
        ok: false,
        error: `unexpected non-zero exit ${spawned.code} with ok=true in JSON`,
        exitCode: spawned.code,
      }
    }
    return fromJson
  }
  return { ...fromJson, exitCode: spawned.code }
}
