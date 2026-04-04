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

export type MineruOutlineOk = {
  ok: true
  outline: string
  source: string
  note?: string
  /** Present when stdout JSON includes a valid attach-book `layout` (optional for `/read`). */
  layout?: AttachBookLayoutRequestFull
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
    const candidate = join(
      dir,
      'minerui-spike',
      'spike_mineru_phase_a_outline.py'
    )
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

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v)
}

function validateAnchor(a: unknown, label: string): string | null {
  if (!isRecord(a)) {
    return `${label} must be an object`
  }
  if (typeof a.anchorFormat !== 'string' || a.anchorFormat.trim() === '') {
    return `${label}.anchorFormat is required`
  }
  if (typeof a.value !== 'string') {
    return `${label}.value is required`
  }
  return null
}

function validateLayoutNode(node: unknown): string | null {
  if (!isRecord(node)) {
    return 'each layout node must be an object'
  }
  if (typeof node.title !== 'string' || node.title.trim() === '') {
    return 'each layout node needs a non-empty title'
  }
  const startErr = validateAnchor(node.startAnchor, 'startAnchor')
  if (startErr !== null) {
    return startErr
  }
  const endErr = validateAnchor(node.endAnchor, 'endAnchor')
  if (endErr !== null) {
    return endErr
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

function toMineruResult(raw: unknown): MineruOutlineResult | null {
  if (!isRecord(raw)) {
    return null
  }
  if (raw.ok === true) {
    const outline = typeof raw.outline === 'string' ? raw.outline.trim() : ''
    const source = typeof raw.source === 'string' ? raw.source : ''
    const note = typeof raw.note === 'string' ? raw.note : undefined
    const layoutParsed = parseLayoutFromStdoutJson(raw)
    if ('error' in layoutParsed) {
      return { ok: false, error: layoutParsed.error }
    }
    const out: MineruOutlineOk = { ok: true, outline, source }
    if (note !== undefined) {
      out.note = note
    }
    if ('layout' in layoutParsed) {
      out.layout = layoutParsed.layout
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
      const spawned = await spawnWithTimeout(python, args, timeoutMs, cwd)
      if (spawned.timedOut) {
        return {
          ok: false,
          error: `MinerU outline subprocess timed out after ${timeoutMs}ms`,
          exitCode: spawned.code,
        }
      }
      return mapSpawnOutcome(spawned)
    } finally {
      await rm(tmpDir, { recursive: true, force: true }).catch(() => undefined)
    }
  }

  const spawned = await spawnWithTimeout(python, args, timeoutMs, cwd)
  if (spawned.timedOut) {
    return {
      ok: false,
      error: `MinerU outline subprocess timed out after ${timeoutMs}ms`,
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
      const err = spawned.stderr.trim()
      const excerpt =
        err.length <= MINERU_STDERR_EXCERPT_CHARS
          ? err
          : `${err.slice(0, MINERU_STDERR_EXCERPT_CHARS / 2)}\n…\n${err.slice(-MINERU_STDERR_EXCERPT_CHARS / 2)}`
      return {
        ok: false,
        error: excerpt || `empty stdout (exit ${spawned.code ?? 'unknown'})`,
        exitCode: spawned.code,
      }
    }
    parsed = JSON.parse(trimmedOut) as unknown
  } catch {
    const err = spawned.stderr.trim()
    const excerpt =
      err.length <= MINERU_STDERR_EXCERPT_CHARS
        ? err
        : `${err.slice(0, MINERU_STDERR_EXCERPT_CHARS / 2)}\n…\n${err.slice(-MINERU_STDERR_EXCERPT_CHARS / 2)}`
    return {
      ok: false,
      error: excerpt
        ? `invalid JSON on stdout; stderr:\n${excerpt}`
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
