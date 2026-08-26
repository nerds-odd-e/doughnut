import { constants as fsConstants, existsSync } from 'node:fs'
import { access, mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { materializeEmbeddedMineruOutlineScript } from './embeddedMineruOutlineScript.js'
import {
  isSpawnOk,
  runMineruSpawn,
  stderrBodyForUserMessage,
  timeoutErrorMessage,
  type SpawnOutcome,
} from './mineruOutlineSpawn.js'
import { toMineruResult } from './mineruOutlineStdoutJson.js'
import {
  MINERU_OUTLINE_DEFAULT_TIMEOUT_MS,
  type MineruOutlineResult,
  type RunMineruOutlineOptions,
} from './mineruOutlineTypes.js'

export {
  MINERU_OUTLINE_DEFAULT_TIMEOUT_MS,
  type MineruOutlineErr,
  type MineruOutlineOk,
  type MineruOutlineResult,
  type RunMineruOutlineOptions,
} from './mineruOutlineTypes.js'

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
    process.env.DONUT_MINERU_OUTLINE_SCRIPT ??
    resolveDefaultScriptPath(cwd) ??
    materializeEmbeddedMineruOutlineScript()

  const python =
    options.pythonExecutable ?? process.env.DONUT_MINERU_PYTHON ?? 'python3'
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
    const tmpDir = await mkdtemp(join(tmpdir(), 'donut-mineru-out-'))
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
