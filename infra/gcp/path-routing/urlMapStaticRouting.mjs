/**
 * Approximates GCP URL map pathRule matching: first matching rule wins.
 * Used to validate prod routing without calling gcloud.
 */

/**
 * @param {string} pattern GCP path pattern (e.g. /assets/* or /index.html)
 * @param {string} urlPath pathname only, no query
 */
export function gcpPathPatternMatches(pattern, urlPath) {
  if (pattern.endsWith('/*')) {
    const dir = pattern.slice(0, -1)
    return urlPath.startsWith(dir)
  }
  return urlPath === pattern
}

/**
 * @param {string} urlPath
 * @param {Array<{ paths?: string[], service?: string }>} pathRules
 * @returns {string | null} matched service URL, or null if no pathRule matched
 */
export function gcpFirstMatchService(urlPath, pathRules) {
  for (const rule of pathRules) {
    for (const pat of rule.paths ?? []) {
      if (gcpPathPatternMatches(pat, urlPath)) return rule.service ?? ''
    }
  }
  return null
}

export function isBackendBucketRule(service) {
  return typeof service === 'string' && service.includes('backendBuckets')
}

export function gcpRoutesToStaticBucket(urlPath, pathRules) {
  const svc = gcpFirstMatchService(urlPath, pathRules)
  if (svc === null) return false
  return isBackendBucketRule(svc)
}

/**
 * Paths every prod URL map must route to the static (GCS) backend bucket.
 * Not derived from the frontend tree: structural requirements for SPA + Vite assets.
 */
export function mandatoryStaticBucketProbes() {
  return ['/', '/index.html', '/assets/.doughnut-path-routing-probe']
}
