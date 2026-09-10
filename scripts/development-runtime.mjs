import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

/** Canonical Development application stack — one target, no Mountebank. */
export const DEVELOPMENT_RUNTIME_TARGET = Object.freeze({
  profile: 'dev',
  database: 'doughnut_development',
  backendPort: 8081,
  vitePort: 5176,
  lbListenPort: 5175,
  logFile: path.join(repoRoot, 'dev.log'),
  pidFile: path.join(repoRoot, 'dev.pid'),
})
