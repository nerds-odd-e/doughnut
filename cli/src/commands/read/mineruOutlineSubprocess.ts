import { spawn } from 'node:child_process'
import { constants as fsConstants, existsSync } from 'node:fs'
import { access, mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'

export const MINERU_OUTLINE_DEFAULT_TIMEOUT_MS = 30 * 60 * 1000

export type MineruOutlineOk = {
  ok: true
  outline: string
  source: string
  note?: string
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

function toMineruResult(raw: unknown): MineruOutlineResult | null {
  if (!isRecord(raw)) {
    return null
  }
  if (raw.ok === true) {
    const outline = typeof raw.outline === 'string' ? raw.outline.trim() : ''
    const source = typeof raw.source === 'string' ? raw.source : ''
    const note = typeof raw.note === 'string' ? raw.note : undefined
    const out: MineruOutlineOk = { ok: true, outline, source }
    if (note !== undefined) {
      out.note = note
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
    resolveDefaultScriptPath(cwd)
  if (!scriptPath) {
    return {
      ok: false,
      error:
        'miner outline script not found (expected minerui-spike/spike_mineru_phase_a_outline.py from cwd ancestors); set DOUGHNUT_MINERU_OUTLINE_SCRIPT',
    }
  }

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
      return {
        ok: false,
        error:
          spawned.stderr.trim().slice(-500) ||
          `empty stdout (exit ${spawned.code ?? 'unknown'})`,
        exitCode: spawned.code,
      }
    }
    parsed = JSON.parse(trimmedOut) as unknown
  } catch {
    const tail = spawned.stderr.trim().slice(-500)
    return {
      ok: false,
      error: tail
        ? `invalid JSON on stdout; stderr (tail): ${tail}`
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
