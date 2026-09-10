#!/usr/bin/env node
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import { createRotatingLogWriter } from './log-utils.mjs'
import { withRuntimeTargetEnv } from './local-runtime-target.mjs'
import {
  childExitReason,
  runSupervisedServiceGroup,
} from './supervised-service-group.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

/** Reload-capable Development stack — never includes Mountebank. */
export const DEVELOPMENT_SERVICE_ARGS = [
  'exec',
  'run-p',
  '-lnr',
  'backend:dev',
  'local:lb:vite',
  'frontend:dev',
]

export function runDevelopmentServices({
  spawnFn,
  logFile = process.env.DEV_LOG_FILE ?? DEVELOPMENT_RUNTIME_TARGET.logFile,
  logWriter = createRotatingLogWriter(logFile),
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  env = process.env,
  checkoutRoot = repoRoot,
  serviceArgs = DEVELOPMENT_SERVICE_ARGS,
} = {}) {
  return runSupervisedServiceGroup({
    spawnFn,
    serviceArgs,
    cwd: checkoutRoot,
    env: withRuntimeTargetEnv(env, runtimeTarget),
    logWriter,
    startFailureMessage: (error) =>
      `Failed to start Development services: ${error.message}\n`,
    exitMessage: (code, signal) =>
      `Forced Development service child exit (${childExitReason(code, signal)})\n`,
    cleanupFailurePrefix: 'Development supervisor cleanup failed',
  })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  runDevelopmentServices()
}
