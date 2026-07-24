import assert from 'node:assert'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import YAML from 'yaml'
import { gcpRoutesToStaticBucket } from './urlMapStaticRouting.mjs'
import {
  loadDoughnutRouting,
  renderDoughnutAppServiceUrlMapYamlFromRouting,
} from './doughnutRouting.mjs'
import {
  PATH_ROUTING_VALIDATION_DUMMY_SHA,
  pathRulesFromUrlMapDoc,
} from './validateUrlMapPathRouting.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

function renderRepoUrlMap() {
  const routing = loadDoughnutRouting(
    path.join(repoRoot, 'infra/gcp/path-routing/doughnut-routing.json')
  )
  const yamlText = renderDoughnutAppServiceUrlMapYamlFromRouting(
    routing,
    PATH_ROUTING_VALIDATION_DUMMY_SHA
  )
  return { routing, doc: YAML.parse(yamlText) }
}

test('rendered URL map path rule order: MIG hint rules, then static rules, then catch-all', () => {
  const { routing, doc } = renderRepoUrlMap()
  const pr = pathRulesFromUrlMapDoc(doc)
  assert.ok(!('error' in pr))
  const hints = routing.backendPathHints
  const expectedMigPaths = [
    ...hints.exactPaths,
    ...hints.pathPrefixes.map((p) => `${p}*`),
    ...(hints.pathPrefixesAllowBare ?? []).flatMap((p) => [p, `${p}*`]),
  ]
  const expectedStaticPaths = routing.gcpUrlMap.staticPathRules.flatMap(
    (r) => r.paths
  )
  const expected = [...expectedMigPaths, ...expectedStaticPaths, '/*']
  const actual = pr.pathRules.flatMap((r) => r.paths ?? [])
  assert.deepEqual(actual, expected)
})

test('rendered URL map: MIG hint rules target the backend service, not the bucket', () => {
  const { routing, doc } = renderRepoUrlMap()
  const pr = pathRulesFromUrlMapDoc(doc)
  const migRule = pr.pathRules.find((r) => r.paths?.includes('/api/*'))
  assert.ok(migRule, 'expected a rendered pathRule for /api/*')
  assert.equal(migRule.service, routing.gcpUrlMap.backendService)
  assert.ok(!('routeAction' in migRule), 'MIG pathRules should not rewrite the path')
})

test('rendered URL map: backend-classified paths are not routed to the bucket', () => {
  const { doc } = renderRepoUrlMap()
  const pr = pathRulesFromUrlMapDoc(doc)
  for (const urlPath of ['/api/foo', '/attachments/x', '/logout']) {
    assert.ok(
      !gcpRoutesToStaticBucket(urlPath, pr.pathRules),
      `${urlPath} should not route to the static bucket`
    )
  }
})

test('rendered URL map: unknown frontend deep link falls through the catch-all to the bucket', () => {
  const { doc } = renderRepoUrlMap()
  const pr = pathRulesFromUrlMapDoc(doc)
  assert.ok(gcpRoutesToStaticBucket('/settings/recall-stats', pr.pathRules))
})

test('rendered URL map: defaultCustomErrorResponsePolicy serves active SHA index.html with 200 on 404', () => {
  const { routing, doc } = renderRepoUrlMap()
  const matcher = doc.pathMatchers.find((m) => m.name === 'doughnut-paths')
  const policy = matcher.defaultCustomErrorResponsePolicy
  assert.ok(policy, 'expected defaultCustomErrorResponsePolicy on the pathMatcher')
  assert.equal(policy.errorResponseRules.length, 1)
  assert.deepEqual(policy.errorResponseRules[0].matchResponseCodes, ['404'])
  assert.equal(
    policy.errorResponseRules[0].path,
    `/frontend/${PATH_ROUTING_VALIDATION_DUMMY_SHA}/index.html`
  )
  assert.equal(policy.errorResponseRules[0].overrideResponseCode, 200)
  assert.equal(policy.errorService, routing.gcpUrlMap.staticBackendBucketService)
})

test('rendered URL map: defaultService is the backend bucket (backend paths are explicit)', () => {
  const { routing, doc } = renderRepoUrlMap()
  assert.equal(doc.defaultService, routing.gcpUrlMap.staticBackendBucketService)
  const matcher = doc.pathMatchers.find((m) => m.name === 'doughnut-paths')
  assert.equal(matcher.defaultService, routing.gcpUrlMap.staticBackendBucketService)
})
