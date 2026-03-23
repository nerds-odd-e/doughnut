#!/usr/bin/env node
/**
 * Prod URL map checks (phase 8 + phase 2):
 * - static pathRules must not capture backend-classified paths (backend-path-hints.json)
 * - required static paths (from frontend + mandatory SPA/asset probes) must route to GCS bucket
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { runRepoPathRoutingValidation } from '../infra/gcp/path-routing/validateUrlMapPathRouting.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')

const { failures, backendChecks, staticChecks } = runRepoPathRoutingValidation({
  repoRoot,
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
