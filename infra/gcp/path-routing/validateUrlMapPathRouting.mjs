import { readFileSync } from 'node:fs'
import path from 'node:path'
import YAML from 'yaml'
import { loadBackendPathHints, pathGoesToBackend } from './pathGoesToBackend.mjs'
import {
  gcpRoutesToStaticBucket,
  mandatoryStaticBucketProbes,
} from './urlMapStaticRouting.mjs'
import { collectRequiredStaticPathsFromFrontend } from './requiredStaticPathsFromFrontend.mjs'

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

/**
 * @param {object} doc parsed URL map YAML
 * @returns {{ pathRules: object[] } | { error: string }}
 */
export function pathRulesFromUrlMapDoc(doc) {
  const matcher = doc.pathMatchers?.find((m) => m.name === 'doughnut-paths')
  if (!matcher?.pathRules) {
    return { error: 'missing pathMatchers.doughnut-paths.pathRules' }
  }
  return { pathRules: matcher.pathRules }
}

/**
 * @param {{
 *   urlMapYamlText: string,
 *   hints: ReturnType<typeof loadBackendPathHints>,
 *   requiredStaticPaths: string[],
 * }} args
 * @returns {{ failures: string[], backendChecks: number, staticChecks: number }}
 */
export function validateUrlMapAgainstHintsAndStaticPaths({
  urlMapYamlText,
  hints,
  requiredStaticPaths,
}) {
  const doc = YAML.parse(urlMapYamlText)
  const rulesInfo = pathRulesFromUrlMapDoc(doc)
  if ('error' in rulesInfo) {
    return {
      failures: [rulesInfo.error],
      backendChecks: 0,
      staticChecks: 0,
    }
  }
  const { pathRules } = rulesInfo
  const failures = []

  let backendChecks = 0
  for (const urlPath of backendProbePaths(hints)) {
    if (!pathGoesToBackend(urlPath, hints)) continue
    backendChecks++
    if (gcpRoutesToStaticBucket(urlPath, pathRules)) {
      failures.push(
        `backend-classified path <${urlPath}> would match a static (backend bucket) pathRule`
      )
    }
  }

  const staticCandidates = [
    ...new Set([
      ...mandatoryStaticBucketProbes(),
      ...requiredStaticPaths,
    ]),
  ].sort()

  let staticChecks = 0
  for (const urlPath of staticCandidates) {
    staticChecks++
    if (pathGoesToBackend(urlPath, hints)) {
      failures.push(
        `required static path <${urlPath}> is classified as backend in backend-path-hints.json`
      )
      continue
    }
    if (!gcpRoutesToStaticBucket(urlPath, pathRules)) {
      failures.push(
        `required static path <${urlPath}> would not be served from the static backend bucket (default would hit the MIG)`
      )
    }
  }

  return { failures, backendChecks, staticChecks }
}

/**
 * @param {{
 *   repoRoot: string,
 *   urlMapPath?: string,
 *   hintsPath?: string,
 * }} args
 */
export function runRepoPathRoutingValidation({ repoRoot, urlMapPath, hintsPath }) {
  const mapPath =
    urlMapPath ??
    path.join(repoRoot, 'infra/gcp/url-maps/doughnut-app-service-map.yaml')
  const hintsFile =
    hintsPath ??
    path.join(repoRoot, 'infra/gcp/path-routing/backend-path-hints.json')
  const raw = readFileSync(mapPath, 'utf8')
  const hints = loadBackendPathHints(hintsFile)
  const requiredStaticPaths = collectRequiredStaticPathsFromFrontend(repoRoot)
  return validateUrlMapAgainstHintsAndStaticPaths({
    urlMapYamlText: raw,
    hints,
    requiredStaticPaths,
  })
}
