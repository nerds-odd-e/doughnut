const HOST = '127.0.0.1'

export function runtimeTargetProcessEnv(target) {
  const backendOrigin = `http://${HOST}:${target.backendPort}`
  const env = {
    SERVER_PORT: String(target.backendPort),
    LOCAL_LB_BACKEND: backendOrigin,
    LOCAL_LB_LISTEN_PORT: String(target.lbListenPort),
    FRONTEND_BACKEND_ORIGIN: backendOrigin,
  }
  // Built-asset target: the frontend is served statically from `frontend/dist`
  // by the local LB (no Vite dev server). Omit the Vite upstream + dev port so
  // `local-lb.mjs` falls back to its static root. The `built` flag is launch
  // data on the target — not a different lifecycle or shutdown algorithm.
  if (!target.built) {
    env.LOCAL_LB_VITE_UPSTREAM = `http://${HOST}:${target.vitePort}`
    env.FRONTEND_DEV_PORT = String(target.vitePort)
  }
  if (target.databaseUrl) {
    env.INPUT_DB_URL = target.databaseUrl
  }
  return env
}

export function withRuntimeTargetEnv(env, target) {
  return { ...env, ...runtimeTargetProcessEnv(target) }
}

export function browserOrigin(target) {
  return `http://${HOST}:${target.lbListenPort}`
}

/** Application stack ports only — excludes optional Mountebank. */
function applicationPortEntries(target) {
  const entries = [
    ['backend', target.backendPort],
    ['local LB', target.lbListenPort],
  ]
  // Built-asset target has no Vite dev server; its port is neither started nor
  // checked for foreign occupation.
  if (!target.built) {
    entries.splice(1, 0, ['frontend vite', target.vitePort])
  }
  return entries
}

/** Application stack port numbers only — excludes optional Mountebank. */
export function applicationPorts(target) {
  return applicationPortEntries(target).map(([, port]) => port)
}

export async function listOccupiedApplicationPorts(target, isPortOccupiedFn) {
  const occupied = []
  for (const [service, port] of applicationPortEntries(target)) {
    if (await isPortOccupiedFn(port)) {
      occupied.push(`${service} ${port}`)
    }
  }
  return occupied
}

export function healthEndpoints(target) {
  const tcpChecks = []
  if (target.mountebankPort != null) {
    tcpChecks.push({
      service: 'mountebank',
      host: HOST,
      port: target.mountebankPort,
    })
  }
  tcpChecks.push(
    { service: 'backend', host: HOST, port: target.backendPort },
    { service: 'local LB', host: HOST, port: target.lbListenPort }
  )
  // Built-asset target has no Vite listener; readiness is the LB + backend.
  if (!target.built) {
    tcpChecks.push({
      service: 'frontend vite',
      host: HOST,
      port: target.vitePort,
    })
  }
  return {
    tcpChecks,
    readinessUrl: `http://${HOST}:${target.lbListenPort}/__lb__/ready`,
  }
}
