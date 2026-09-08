#!/usr/bin/env node
import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRotatingLogWriter, LOG_TARGETS } from './log-utils.mjs'
import {
  releaseSutOwnership,
  startSutOwnerControlFromEnv,
} from './sut-owner.mjs'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'
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
  const args = ['exec', 'run-p', '-lnr', 'backend:sut']
  if (target.mountebankPort != null) args.push('start:mb')
  args.push('local:lb:vite', 'frontend:sut')
  return args
}

export const SUT_SERVICE_ARGS = sutServiceArgs(LEGACY_SUT_RUNTIME_TARGET)

function childExitReason(code, signal) {
  return signal ? `signal ${signal}` : `code ${code ?? 'unknown'}`
}

export function runSutServices({
  spawnFn = spawn,
  logFile = process.env.SUT_LOG_FILE ?? LOG_TARGETS.sut,
  logWriter = createRotatingLogWriter(logFile),
  runtimeTarget,
  env = process.env,
  checkoutRoot = env.SUT_CHECKOUT_ROOT ?? repoRoot,
  serviceArgs,
  retainOwnershipOnExit,
} = {}) {
  const target = resolveSutRuntimeTarget({ runtimeTarget, env })
  const child = spawnFn('pnpm', serviceArgs ?? sutServiceArgs(target), {
    cwd: checkoutRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: withSutRuntimeTargetEnv(env, target),
    shell: false,
    detached: true,
  })

  child.stdout?.on('data', (chunk) => logWriter.write(chunk))
  child.stderr?.on('data', (chunk) => logWriter.write(chunk))

  const forwardSignal = (signal) => {
    if (!child.killed) child.kill(signal)
  }

  process.once('SIGINT', forwardSignal)
  process.once('SIGTERM', forwardSignal)

  let finished = false
  const finish = async (code, signal, message) => {
    if (finished) return
    finished = true
    logWriter.write(message)
    await stopOwnedSutProcessTree(child)
    if (env.SUT_OWNER_TOKEN && !retainOwnershipOnExit?.()) {
      await releaseSutOwnership(checkoutRoot)
    }
    logWriter.close()
    process.exit(signal ? 1 : (code ?? 1))
  }

  const reportFinishFailure = (error) => {
    logWriter.write(
      `SUT supervisor cleanup failed: ${error instanceof Error ? error.message : String(error)}\n`
    )
    logWriter.close()
    process.exit(1)
  }

  child.on('error', (error) => {
    finish(1, null, `Failed to start SUT services: ${error.message}\n`).catch(
      reportFinishFailure
    )
  })

  child.on('close', (code, signal) => {
    finish(
      code,
      signal,
      `Forced SUT service child exit (${childExitReason(code, signal)}); releasing owned peers\n`
    ).catch(reportFinishFailure)
  })

  return child
}

export async function startOwnedSutSupervisor(opts = {}) {
  const env = opts.env ?? process.env
  const state = { child: undefined, retain: false }
  await startSutOwnerControlFromEnv(env, {
    onShutdown: async () => {
      state.retain = true
      await stopOwnedSutProcessTree(state.child)
    },
    getApplicationGroupId: () => {
      const pid = state.child?.pid
      return Number.isInteger(pid) && pid > 0 ? pid : undefined
    },
  })
  state.child = runSutServices({
    ...opts,
    env,
    retainOwnershipOnExit: () => state.retain,
  })
  return state.child
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  await startOwnedSutSupervisor()
}
