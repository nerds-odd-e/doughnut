import {
  browserOrigin,
  healthEndpoints,
  runtimeTargetProcessEnv,
} from './local-runtime-target.mjs'

export const LEGACY_SUT_RUNTIME_TARGET = Object.freeze({
  backendPort: 9081,
  vitePort: 5174,
  lbListenPort: 5173,
  mountebankPort: 2525,
})

const SUT_RUNTIME_TARGET_ENV = 'SUT_RUNTIME_TARGET'

export function resolveSutRuntimeTarget({
  runtimeTarget,
  env = process.env,
} = {}) {
  if (runtimeTarget) return runtimeTarget
  const encoded = env[SUT_RUNTIME_TARGET_ENV]
  if (encoded) return JSON.parse(encoded)
  return LEGACY_SUT_RUNTIME_TARGET
}

export function sutRuntimeTargetProcessEnv(target) {
  return {
    ...runtimeTargetProcessEnv(target),
    [SUT_RUNTIME_TARGET_ENV]: JSON.stringify(target),
  }
}

export function withSutRuntimeTargetEnv(env, target) {
  return { ...env, ...sutRuntimeTargetProcessEnv(target) }
}

export const isolatedBrowserOrigin = browserOrigin

export const sutHealthEndpoints = healthEndpoints
