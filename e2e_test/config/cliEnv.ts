/** CLI environment for E2E: API base URL plus optional overrides. */
import { E2E_APP_BASE_URL } from './constants'

function e2eNoProxyMerged(overrides?: NodeJS.ProcessEnv): string {
  const parts = [
    '127.0.0.1',
    'localhost',
    '::1',
    process.env.NO_PROXY,
    process.env.no_proxy,
    overrides?.NO_PROXY,
    overrides?.no_proxy,
  ]
    .filter(Boolean)
    .join(',')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return [...new Set(parts)].join(',')
}

export function cliEnv(overrides?: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  return {
    DOUGHNUT_API_BASE_URL: E2E_APP_BASE_URL,
    ...overrides,
    NO_PROXY: e2eNoProxyMerged(overrides),
  }
}

/** For `install.sh` + `curl` (reads `NO_PROXY` / `no_proxy`). */
export function cliInstallShellEnv(
  extra: NodeJS.ProcessEnv
): NodeJS.ProcessEnv {
  const noProxy = e2eNoProxyMerged()
  return {
    ...process.env,
    ...extra,
    NO_PROXY: noProxy,
    no_proxy: noProxy,
  }
}
