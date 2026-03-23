#!/usr/bin/env node
/**
 * Prod URL map checks (phase 8 + phase 2):
 * - static pathRules must not capture backend-classified paths (backend-path-hints.json)
 * - required static paths (from frontend + mandatory SPA/asset probes) must route to GCS bucket
 *
 * Optional: --url-map <file> (fully rendered YAML, e.g. deploy-time output)
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { runRepoPathRoutingValidation } from '../infra/gcp/path-routing/validateUrlMapPathRouting.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')

function parseUrlMapArg(argv) {
  const i = argv.indexOf('--url-map')
  if (i === -1) return {}
  const p = argv[i + 1]
  if (!p) {
    console.error('validate-url-map-path-routing: --url-map requires a path')
    process.exit(1)
  }
  return { urlMapPath: path.resolve(p) }
}

const { urlMapPath } = parseUrlMapArg(process.argv)

const { failures, backendChecks, staticChecks } = runRepoPathRoutingValidation({
  repoRoot,
  ...(urlMapPath ? { urlMapPath } : {}),
})

if (failures.length) {
  console.error('validate-url-map-path-routing: FAILED')
  for (const f of failures) console.error(`  - ${f}`)
  process.exit(1)
}

console.log(
  'validate-url-map-path-routing: OK (',
  backendChecks,
  'backend probes,',
  staticChecks,
  'static paths checked)'
)
