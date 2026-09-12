#!/usr/bin/env node
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
import {
  childExitReason,
  runSupervisedServiceGroup,
} from './supervised-service-group.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export function sutServiceArgs(target) {
  const args = [
    'exec',
    'run-p',
    '-lnr',
    target.built ? 'backend:sut:ci' : 'backend:sut',
  ]
  if (target.mountebankPort != null) args.push('start:mb')
  // Built-asset target: serve the frontend from `frontend/dist` via the local
  // LB (no Vite dev server). `local:lb` and `local:lb:vite` are the same
  // script; the difference is whether `LOCAL_LB_VITE_UPSTREAM` is set, which
  // `runtimeTargetProcessEnv` omits for the built target. Either name works;
  // `local:lb` documents the no-Vite intent.
  args.push(target.built ? 'local:lb' : 'local:lb:vite')
  if (!target.built) args.push('frontend:sut')
  return args
}

export const SUT_SERVICE_ARGS = sutServiceArgs(LEGACY_SUT_RUNTIME_TARGET)

export function runSutServices({
  spawnFn,
  logFile = process.env.SUT_LOG_FILE ?? LOG_TARGETS.sut,
  logWriter = createRotatingLogWriter(logFile),
  runtimeTarget,
  env = process.env,
  checkoutRoot = env.SUT_CHECKOUT_ROOT ?? repoRoot,
  serviceArgs,
  retainOwnershipOnExit,
  stopOwnedTree = stopOwnedSutProcessTree,
} = {}) {
  const target = resolveSutRuntimeTarget({ runtimeTarget, env })
  return runSupervisedServiceGroup({
    spawnFn,
    serviceArgs: serviceArgs ?? sutServiceArgs(target),
    cwd: checkoutRoot,
    env: withSutRuntimeTargetEnv(env, target),
    logWriter,
    onBeforeExit: async (child) => {
      await stopOwnedTree(child)
      if (env.SUT_OWNER_TOKEN && !retainOwnershipOnExit?.()) {
        await releaseSutOwnership(checkoutRoot)
      }
    },
    startFailureMessage: (error) =>
      `Failed to start SUT services: ${error.message}\n`,
    exitMessage: (code, signal) =>
      `Forced SUT service child exit (${childExitReason(code, signal)}); releasing owned peers\n`,
    cleanupFailurePrefix: 'SUT supervisor cleanup failed',
  })
}

export async function startOwnedSutSupervisor(opts = {}) {
  const env = opts.env ?? process.env
  const state = { child: undefined, retain: false, shutdown: undefined }
  const sharedStop = (child) => {
    if (!state.shutdown) {
      state.shutdown = stopOwnedSutProcessTree(child ?? state.child)
    }
    return state.shutdown
  }
  await startSutOwnerControlFromEnv(env, {
    onShutdown: async () => {
      state.retain = true
      await sharedStop(state.child)
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
    stopOwnedTree: sharedStop,
  })
  return state.child
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  await startOwnedSutSupervisor()
}
