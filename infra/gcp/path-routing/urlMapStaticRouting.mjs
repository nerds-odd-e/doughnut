/**
 * Approximates GCP URL map matching for validation without calling gcloud.
 * Supports both legacy pathRules (first-match array order) and routeRules
 * (priority-ordered matchRules).
 */

import { defaultDoughnutRoutingPath, loadDoughnutRouting } from './doughnutRouting.mjs'

/**
 * @param {string} pattern GCP path pattern (e.g. /assets/* or /index.html)
 * @param {string} urlPath pathname only, no query
 */
export function gcpPathPatternMatches(pattern, urlPath) {
  if (pattern.endsWith('*')) {
    const prefix = pattern.slice(0, -1)
    return urlPath.startsWith(prefix)
  }
  return urlPath === pattern
}

/**
 * @param {{
 *   fullPathMatch?: string,
 *   prefixMatch?: string,
 *   pathTemplateMatch?: string,
 * }} matchRule
 * @param {string} urlPath
 */
export function routeMatchRuleMatches(matchRule, urlPath) {
  if (matchRule.fullPathMatch != null) {
    return urlPath === matchRule.fullPathMatch
  }
  if (matchRule.prefixMatch != null) {
    return urlPath.startsWith(matchRule.prefixMatch)
  }
  if (matchRule.pathTemplateMatch != null) {
    // Only /** (match-all) is used in our rendered map today.
    if (matchRule.pathTemplateMatch === '/**') return true
    return false
  }
  return false
}

/**
 * @typedef {{ service: string, matches: (urlPath: string) => boolean }} NormalizedRoutingRule
 */

/**
 * @param {object[]} routeRules
 * @returns {NormalizedRoutingRule[]}
 */
export function normalizeRouteRules(routeRules) {
  return [...routeRules]
    .sort((a, b) => (a.priority ?? 0) - (b.priority ?? 0))
    .map((rule) => ({
      service: rule.service ?? '',
      matches: (urlPath) =>
        (rule.matchRules ?? []).some((m) => routeMatchRuleMatches(m, urlPath)),
    }))
}

/**
 * @param {Array<{ paths?: string[], service?: string }>} pathRules
 * @returns {NormalizedRoutingRule[]}
 */
export function normalizePathRules(pathRules) {
  return pathRules.map((rule) => ({
    service: rule.service ?? '',
    matches: (urlPath) =>
      (rule.paths ?? []).some((pat) => gcpPathPatternMatches(pat, urlPath)),
  }))
}

/**
 * @param {string} urlPath
 * @param {NormalizedRoutingRule[]} routingRules
 * @returns {string | null} matched service URL, or null if no rule matched
 */
export function gcpFirstMatchService(urlPath, routingRules) {
  for (const rule of routingRules) {
    if (rule.matches(urlPath)) return rule.service
  }
  return null
}

export function isBackendBucketRule(service) {
  return typeof service === 'string' && service.includes('backendBuckets')
}

/**
 * @param {string} urlPath
 * @param {NormalizedRoutingRule[]} routingRules
 */
export function gcpRoutesToStaticBucket(urlPath, routingRules) {
  const svc = gcpFirstMatchService(urlPath, routingRules)
  if (svc === null) return false
  return isBackendBucketRule(svc)
}

/**
 * Paths every prod URL map must route to the static (GCS) backend bucket.
 * Declared in doughnut-routing.json (structural SPA + asset probes).
 * @param {string} [routingJsonPath]
 */
export function mandatoryStaticBucketProbes(
  routingJsonPath = defaultDoughnutRoutingPath()
) {
  return loadDoughnutRouting(routingJsonPath).mandatoryStaticBucketProbes
}
