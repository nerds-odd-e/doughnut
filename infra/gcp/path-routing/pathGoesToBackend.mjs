import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const DEFAULT_ROUTING_PATH = path.join(__dirname, 'doughnut-routing.json')

/**
 * @param {string} jsonPath doughnut-routing.json (or compatible doc with backendPathHints)
 * @returns {{
 *   exactPaths: string[],
 *   pathPrefixes: string[],
 *   pathPrefixesAllowBare?: string[]
 * }}
 */
export function loadBackendPathHints(jsonPath = DEFAULT_ROUTING_PATH) {
  const doc = JSON.parse(readFileSync(jsonPath, 'utf8'))
  return doc.backendPathHints ?? doc
}

/**
 * Same classification as prod “default → MIG” for paths not listed as static in the URL map.
 * @param {string} urlPath pathname only, no query
 * @param {ReturnType<typeof loadBackendPathHints>} hints
 */
export function pathGoesToBackend(urlPath, hints) {
  if (hints.exactPaths.includes(urlPath)) return true
  for (const p of hints.pathPrefixes) {
    if (urlPath.startsWith(p)) return true
  }
  for (const p of hints.pathPrefixesAllowBare ?? []) {
    if (urlPath.startsWith(p)) return true
  }
  return false
}
