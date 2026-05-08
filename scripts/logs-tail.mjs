#!/usr/bin/env node
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  DEFAULT_TAIL_LINES,
  LOG_TARGETS,
  resolveLogTarget,
  tailLogFile,
} from './log-utils.mjs'

const MOUNTEBANK_FILTER = /\b(mountebank|mb)\b/i

function parseArgs(argv) {
  const [target = 'sut', ...rest] = argv
  let lines = DEFAULT_TAIL_LINES

  for (let index = 0; index < rest.length; index++) {
    const arg = rest[index]
    if (arg === '--lines' || arg === '-n') {
      lines = Number(rest[index + 1])
      index++
    } else if (arg.startsWith('--lines=')) {
      lines = Number(arg.slice('--lines='.length))
    } else {
      throw new Error(`Unknown option "${arg}"`)
    }
  }

  if (!Number.isInteger(lines) || lines <= 0) {
    throw new Error('--lines must be a positive integer')
  }

  return { target, lines }
}

export async function runLogsTail({
  argv = process.argv.slice(2),
  out = process.stdout,
  err = process.stderr,
  resolveTarget = resolveLogTarget,
} = {}) {
  try {
    const { target, lines } = parseArgs(argv)
    const logFile = resolveTarget(target)
    const filter = target === 'mountebank' ? MOUNTEBANK_FILTER : undefined
    let output
    try {
      output = await tailLogFile(logFile, lines, { filter })
    } catch (error) {
      if (error?.code === 'ENOENT') {
        err.write(`Log file not found: ${logFile}\n`)
        return 0
      }
      throw error
    }
    out.write(output.endsWith('\n') || output === '' ? output : `${output}\n`)
    return 0
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    err.write(`${message}\n`)
    err.write(
      `Usage: pnpm logs:tail [${Object.keys(LOG_TARGETS).join('|')}] [--lines ${DEFAULT_TAIL_LINES}]\n`
    )
    err.write(
      `Default target is sut. Log paths are relative to ${path.basename(process.cwd())}.\n`
    )
    return 1
  }
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  process.exit(await runLogsTail())
}
