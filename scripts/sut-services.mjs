#!/usr/bin/env node
import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRotatingLogWriter, LOG_TARGETS } from './log-utils.mjs'
import { startSutOwnerControlFromEnv } from './sut-owner.mjs'
import {
  LEGACY_SUT_RUNTIME_TARGET,
  resolveSutRuntimeTarget,
  withSutRuntimeTargetEnv,
} from './sut-runtime-target.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export function sutServiceArgs(target) {
  const args = ['exec', 'run-p', '-clnr', 'backend:sut']
  if (target.mountebankPort != null) args.push('start:mb')
  args.push('local:lb:vite', 'frontend:sut')
  return args
}

export const SUT_SERVICE_ARGS = sutServiceArgs(LEGACY_SUT_RUNTIME_TARGET)

export function runSutServices({
  spawnFn = spawn,
  logFile = process.env.SUT_LOG_FILE ?? LOG_TARGETS.sut,
  logWriter = createRotatingLogWriter(logFile),
  runtimeTarget,
  env = process.env,
} = {}) {
  const target = resolveSutRuntimeTarget({ runtimeTarget, env })
  const child = spawnFn('pnpm', sutServiceArgs(target), {
    cwd: repoRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: withSutRuntimeTargetEnv(env, target),
    shell: false,
  })

  child.stdout?.on('data', (chunk) => logWriter.write(chunk))
  child.stderr?.on('data', (chunk) => logWriter.write(chunk))

  const forwardSignal = (signal) => {
    if (!child.killed) child.kill(signal)
  }

  process.once('SIGINT', forwardSignal)
  process.once('SIGTERM', forwardSignal)

  child.on('error', (error) => {
    logWriter.write(`Failed to start SUT services: ${error.message}\n`)
    logWriter.close()
    process.exit(1)
  })

  child.on('close', (code, signal) => {
    logWriter.close()
    process.exit(signal ? 1 : (code ?? 1))
  })

  return child
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  await startSutOwnerControlFromEnv()
  runSutServices()
}
