import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import YAML from 'yaml'

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
 * Full prod URL map YAML for `doughnut-app-service-map`, from committed routing JSON.
 * @param {ReturnType<typeof loadDoughnutRouting>} routing
 * @param {string} githubSha
 */
export function renderDoughnutAppServiceUrlMapYamlFromRouting(routing, githubSha) {
  const sha = assertGithubSha40(githubSha)
  const { gcpUrlMap } = routing
  const bucket = gcpUrlMap.staticBackendBucketService
  const migDefault = gcpUrlMap.defaultService
  const subst = (s) =>
    s.split(FRONTEND_GITHUB_SHA_PLACEHOLDER).join(sha)
  const pathRules = gcpUrlMap.staticPathRules.map((rule) => ({
    paths: rule.paths,
    service: bucket,
    routeAction: {
      urlRewrite: {
        pathPrefixRewrite: subst(rule.pathPrefixRewrite),
      },
    },
  }))
  const doc = {
    name: gcpUrlMap.name,
    defaultService: migDefault,
    hostRules: [{ hosts: ['*'], pathMatcher: 'doughnut-paths' }],
    pathMatchers: [
      {
        name: 'doughnut-paths',
        defaultService: migDefault,
        pathRules,
      },
    ],
  }
  return YAML.stringify(doc)
}
