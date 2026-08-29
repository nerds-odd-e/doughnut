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
  routingRulesFromUrlMapDoc,
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

test('rendered URL map routeRules: MIG, then static, then pathTemplateRewrite catch-all', () => {
  const { routing, doc } = renderRepoUrlMap()
  const matcher = doc.pathMatchers.find((m) => m.name === 'doughnut-paths')
  assert.ok(matcher.routeRules, 'expected routeRules (not pathRules)')
  assert.ok(!matcher.pathRules, 'pathRules must not be mixed with routeRules')
  assert.ok(
    !matcher.defaultCustomErrorResponsePolicy,
    'SPA fallback is rewrite-based; no custom error policy'
  )

  const sorted = [...matcher.routeRules].sort(
    (a, b) => a.priority - b.priority
  )
  const last = sorted[sorted.length - 1]
  assert.deepEqual(last.matchRules, [{ pathTemplateMatch: '/**' }])
  assert.equal(
    last.routeAction.urlRewrite.pathTemplateRewrite,
    `/frontend/${PATH_ROUTING_VALIDATION_DUMMY_SHA}/index.html`
  )
  assert.equal(last.service, routing.gcpUrlMap.staticBackendBucketService)

  const apiPrefix = sorted.find((r) =>
    r.matchRules?.some((m) => m.prefixMatch === '/api/')
  )
  assert.ok(apiPrefix, 'expected MIG prefixMatch for /api/')
  assert.equal(apiPrefix.service, routing.gcpUrlMap.backendService)
  assert.ok(!('routeAction' in apiPrefix))
})

test('rendered URL map: backend-classified paths are not routed to the bucket', () => {
  const { doc } = renderRepoUrlMap()
  const rr = routingRulesFromUrlMapDoc(doc)
  assert.ok(!('error' in rr))
  for (const urlPath of [
    '/api/foo',
    '/attachments/x',
    '/logout',
    '/login/continue',
  ]) {
    assert.ok(
      !gcpRoutesToStaticBucket(urlPath, rr.routingRules),
      `${urlPath} should not route to the static bucket`
    )
  }
})

test('rendered URL map: unknown frontend deep link hits catch-all bucket rewrite', () => {
  const { doc } = renderRepoUrlMap()
  const rr = routingRulesFromUrlMapDoc(doc)
  assert.ok(!('error' in rr))
  assert.ok(gcpRoutesToStaticBucket('/settings/recall-stats', rr.routingRules))
  assert.ok(gcpRoutesToStaticBucket('/notebooks', rr.routingRules))
  assert.ok(gcpRoutesToStaticBucket('/recall', rr.routingRules))
  assert.ok(gcpRoutesToStaticBucket('/circles', rr.routingRules))
})

test('rendered URL map: defaultService is the backend bucket (backend paths are explicit)', () => {
  const { routing, doc } = renderRepoUrlMap()
  assert.equal(doc.defaultService, routing.gcpUrlMap.staticBackendBucketService)
  const matcher = doc.pathMatchers.find((m) => m.name === 'doughnut-paths')
  assert.equal(matcher.defaultService, routing.gcpUrlMap.staticBackendBucketService)
})
