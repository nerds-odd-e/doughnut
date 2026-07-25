import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import YAML from 'yaml'
import { backendMigRouteRulesFromHints } from './pathGoesToBackend.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export const FRONTEND_GITHUB_SHA_PLACEHOLDER = '__FRONTEND_GITHUB_SHA__'

export function defaultDoughnutRoutingPath() {
  return path.join(__dirname, 'doughnut-routing.json')
}

/**
 * @param {string} jsonPath
 */
export function loadDoughnutRouting(jsonPath = defaultDoughnutRoutingPath()) {
  return JSON.parse(readFileSync(jsonPath, 'utf8'))
}

/**
 * @param {string} githubSha 40-char lowercase hex
 */
function assertGithubSha40(githubSha) {
  const sha = String(githubSha).toLowerCase()
  if (!/^[0-9a-f]{40}$/.test(sha)) {
    throw new Error(
      'GITHUB_SHA must be a 40-character lowercase hexadecimal commit id'
    )
  }
  return sha
}

/**
 * Substitute SHA into a template string (e.g. hand-supplied URL map YAML with placeholders).
 * @param {string} templateText
 * @param {string} githubSha
 */
export function renderDoughnutAppServiceUrlMapTemplate(templateText, githubSha) {
  const sha = assertGithubSha40(githubSha)
  if (!templateText.includes(FRONTEND_GITHUB_SHA_PLACEHOLDER)) {
    throw new Error(
      `URL map text must contain ${FRONTEND_GITHUB_SHA_PLACEHOLDER}`
    )
  }
  return templateText.split(FRONTEND_GITHUB_SHA_PLACEHOLDER).join(sha)
}

/**
 * Convert a doughnut-routing.json static path pattern to a routeRules matchRule.
 * `/assets/*` → prefixMatch `/assets/`; exact paths → fullPathMatch.
 * @param {string} pattern
 */
export function matchRuleFromStaticPathPattern(pattern) {
  if (pattern.endsWith('/*')) {
    return { prefixMatch: pattern.slice(0, -1) }
  }
  return { fullPathMatch: pattern }
}

/**
 * Full prod URL map YAML for `doughnut-app-service-map`, from committed routing JSON.
 * Emits routeRules (not pathRules) so the SPA catch-all can use pathTemplateRewrite
 * to serve index.html as a normal object fetch (no custom error policy).
 * @param {ReturnType<typeof loadDoughnutRouting>} routing
 * @param {string} githubSha
 */
export function renderDoughnutAppServiceUrlMapYamlFromRouting(routing, githubSha) {
  const sha = assertGithubSha40(githubSha)
  const { gcpUrlMap, backendPathHints } = routing
  const bucket = gcpUrlMap.staticBackendBucketService
  const backendService = gcpUrlMap.backendService
  const subst = (s) => s.split(FRONTEND_GITHUB_SHA_PLACEHOLDER).join(sha)

  const { rules: backendRules, nextPriority } = backendMigRouteRulesFromHints(
    backendPathHints,
    backendService,
    1
  )

  let priority = nextPriority
  const staticRules = gcpUrlMap.staticPathRules.map((rule) => ({
    priority: priority++,
    matchRules: rule.paths.map(matchRuleFromStaticPathPattern),
    service: bucket,
    routeAction: {
      urlRewrite: {
        pathPrefixRewrite: subst(rule.pathPrefixRewrite),
      },
    },
  }))

  const catchAllRule = {
    priority: priority++,
    matchRules: [{ pathTemplateMatch: '/**' }],
    service: bucket,
    routeAction: {
      urlRewrite: {
        pathTemplateRewrite: subst(
          `/frontend/${FRONTEND_GITHUB_SHA_PLACEHOLDER}/index.html`
        ),
      },
    },
  }

  const doc = {
    name: gcpUrlMap.name,
    defaultService: bucket,
    hostRules: [{ hosts: ['*'], pathMatcher: 'doughnut-paths' }],
    pathMatchers: [
      {
        name: 'doughnut-paths',
        defaultService: bucket,
        routeRules: [...backendRules, ...staticRules, catchAllRule],
      },
    ],
  }
  return YAML.stringify(doc)
}
