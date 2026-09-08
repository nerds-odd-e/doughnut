const HOST = '127.0.0.1'

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

function sutRuntimeTargetProcessEnv(target) {
  const backendOrigin = `http://${HOST}:${target.backendPort}`
  const env = {
    SERVER_PORT: String(target.backendPort),
    LOCAL_LB_BACKEND: backendOrigin,
    LOCAL_LB_VITE_UPSTREAM: `http://${HOST}:${target.vitePort}`,
    LOCAL_LB_LISTEN_PORT: String(target.lbListenPort),
    FRONTEND_DEV_PORT: String(target.vitePort),
    FRONTEND_BACKEND_ORIGIN: backendOrigin,
    [SUT_RUNTIME_TARGET_ENV]: JSON.stringify(target),
  }
  if (target.databaseUrl) {
    env.INPUT_DB_URL = target.databaseUrl
  }
  return env
}

export function withSutRuntimeTargetEnv(env, target) {
  return { ...env, ...sutRuntimeTargetProcessEnv(target) }
}

export function sutHealthEndpoints(target) {
  return {
    tcpChecks: [
      { service: 'mountebank', host: HOST, port: target.mountebankPort },
      { service: 'backend', host: HOST, port: target.backendPort },
      { service: 'local LB', host: HOST, port: target.lbListenPort },
      { service: 'frontend vite', host: HOST, port: target.vitePort },
    ],
    readinessUrl: `http://${HOST}:${target.lbListenPort}/__lb__/ready`,
  }
}
