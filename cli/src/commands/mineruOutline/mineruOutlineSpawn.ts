import { spawn } from 'node:child_process'
import type { MineruOutlineErr } from './mineruOutlineTypes.js'
import { exceptionText } from '../../exceptionText.js'
import { errnoCode } from '../../errnoCode.js'

/** Cap stderr bytes included in user-visible errors (tracebacks are usually < this). */
const MINERU_STDERR_EXCERPT_CHARS = 12_000

/** After MinerU import hint, keep only a short stderr tail (avoid huge tracebacks). */
const MINERU_IMPORT_DETAIL_MAX_CHARS = 800

const MINERU_IMPORT_HINT =
  "MinerU is missing or could not be imported. Install MinerU for PDF outlines (e.g. pip install 'mineru[pipeline]' in the Python environment used by the CLI), or set DONUT_MINERU_OUTLINE_SCRIPT to a different outline script."

export type SpawnOutcome = {
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

function messageForPythonSpawnFailure(
  err: unknown,
  pythonExecutable: string
): string {
  const code = errnoCode(err)
  const msg = exceptionText(err)
  if (code === 'ENOENT') {
    return `Could not run "${pythonExecutable}" (not found on PATH or missing interpreter). Install Python 3 and ensure it is on PATH, or set DONUT_MINERU_PYTHON to the full path of your python3 binary.`
  }
  if (code === 'EACCES') {
    return `Permission denied when starting "${pythonExecutable}". Check that the file is executable, or choose another interpreter via DONUT_MINERU_PYTHON.`
  }
  return `Failed to start outline extraction (${pythonExecutable}): ${msg}`
}

type SpawnOk = { tag: 'spawned'; outcome: SpawnOutcome }

export async function runMineruSpawn(
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

export function isSpawnOk(r: SpawnOk | MineruOutlineErr): r is SpawnOk {
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
export function stderrBodyForUserMessage(stderr: string): string {
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

export function timeoutErrorMessage(timeoutMs: number, isPdf: boolean): string {
  const base = `MinerU outline subprocess timed out after ${timeoutMs}ms`
  if (!isPdf) {
    return base
  }
  return `${base} For large PDFs, set DONUT_MINERU_PDF_END_PAGE to cap the page range.`
}
