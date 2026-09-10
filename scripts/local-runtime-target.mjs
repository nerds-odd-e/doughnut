const HOST = '127.0.0.1'

export function runtimeTargetProcessEnv(target) {
  const backendOrigin = `http://${HOST}:${target.backendPort}`
  const env = {
    SERVER_PORT: String(target.backendPort),
    LOCAL_LB_BACKEND: backendOrigin,
    LOCAL_LB_VITE_UPSTREAM: `http://${HOST}:${target.vitePort}`,
    LOCAL_LB_LISTEN_PORT: String(target.lbListenPort),
    FRONTEND_DEV_PORT: String(target.vitePort),
    FRONTEND_BACKEND_ORIGIN: backendOrigin,
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
    { service: 'local LB', host: HOST, port: target.lbListenPort },
    { service: 'frontend vite', host: HOST, port: target.vitePort }
  )
  return {
    tcpChecks,
    readinessUrl: `http://${HOST}:${target.lbListenPort}/__lb__/ready`,
  }
}
