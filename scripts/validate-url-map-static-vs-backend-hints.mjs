#!/usr/bin/env node
/**
 * Ensures prod URL map static pathRules do not capture paths classified as backend
 * in infra/gcp/path-routing/backend-path-hints.json (phase 8).
 */
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import YAML from 'yaml'
import {
  loadBackendPathHints,
  pathGoesToBackend,
} from '../infra/gcp/path-routing/pathGoesToBackend.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')

function gcpPathPatternMatches(pattern, urlPath) {
  if (pattern.endsWith('/*')) {
    const dir = pattern.slice(0, -1)
    return urlPath.startsWith(dir)
  }
  return urlPath === pattern
}

function gcpFirstMatchService(urlPath, pathRules) {
  for (const rule of pathRules) {
    for (const pat of rule.paths ?? []) {
      if (gcpPathPatternMatches(pat, urlPath)) return rule.service ?? ''
    }
  }
  return null
}

function isBackendBucketRule(service) {
  return typeof service === 'string' && service.includes('backendBuckets')
}

function gcpRoutesToStaticBucket(urlPath, pathRules) {
  const svc = gcpFirstMatchService(urlPath, pathRules)
  if (svc === null) return false
  return isBackendBucketRule(svc)
}

function backendProbePaths(hints) {
  const out = new Set(hints.exactPaths)
  for (const p of hints.pathPrefixes) {
    out.add(p.endsWith('/') ? `${p}probe` : `${p}/probe`)
  }
  for (const p of hints.pathPrefixesAllowBare ?? []) {
    out.add(p)
    out.add(`${p}/`)
    out.add(`${p}more`)
  }
  return [...out]
}

const urlMapPath = path.join(
  repoRoot,
  'infra/gcp/url-maps/doughnut-app-service-map.yaml'
)
const raw = readFileSync(urlMapPath, 'utf8')
const doc = YAML.parse(raw)
const matcher = doc.pathMatchers?.find((m) => m.name === 'doughnut-paths')
if (!matcher?.pathRules) {
  console.error(
    'validate-url-map: missing pathMatchers.doughnut-paths.pathRules'
  )
  process.exit(1)
}

const hints = loadBackendPathHints()
const pathRules = matcher.pathRules
const failures = []

for (const urlPath of backendProbePaths(hints)) {
  if (!pathGoesToBackend(urlPath, hints)) continue
  if (gcpRoutesToStaticBucket(urlPath, pathRules)) {
    failures.push(
      `backend-classified path <${urlPath}> would match a static (backend bucket) pathRule`
    )
  }
}

if (failures.length) {
  console.error('validate-url-map-static-vs-backend-hints: FAILED')
  for (const f of failures) console.error(`  - ${f}`)
  process.exit(1)
}

console.log(
  'validate-url-map-static-vs-backend-hints: OK (',
  backendProbePaths(hints).filter((p) => pathGoesToBackend(p, hints)).length,
  'backend probe paths checked)'
)
