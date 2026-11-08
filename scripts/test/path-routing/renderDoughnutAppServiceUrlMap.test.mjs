import assert from 'node:assert'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  FRONTEND_GITHUB_SHA_PLACEHOLDER,
  loadDoughnutRouting,
  renderDoughnutAppServiceUrlMapTemplate,
  renderDoughnutAppServiceUrlMapYamlFromRouting,
} from '../../../infra/gcp/path-routing/doughnutRouting.mjs'

const routingPath = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  '../../../infra/gcp/path-routing/doughnut-routing.json'
)

test('render from routing JSON substitutes SHA in static rewrites', () => {
  const routing = loadDoughnutRouting(routingPath)
  const sha = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
  const out = renderDoughnutAppServiceUrlMapYamlFromRouting(routing, sha)
  assert.ok(!out.includes(FRONTEND_GITHUB_SHA_PLACEHOLDER))
  assert.ok(out.includes(`/frontend/${sha}/assets/`))
  assert.ok(out.includes(`/frontend/${sha}/index.html`))
  assert.ok(out.includes('pathTemplateRewrite:'))
  assert.ok(out.includes('pathTemplateMatch: /**'))
  // 5 static rewrites with SHA + catch-all pathTemplateRewrite to index.html
  assert.equal((out.match(new RegExp(sha, 'g')) ?? []).length, 6)
})

test('render rejects non-hex or wrong length', () => {
  assert.throws(() =>
    renderDoughnutAppServiceUrlMapYamlFromRouting(
      loadDoughnutRouting(routingPath),
      'short'
    )
  )
  assert.throws(() =>
    renderDoughnutAppServiceUrlMapYamlFromRouting(
      loadDoughnutRouting(routingPath),
      'g'.repeat(40)
    )
  )
})

test('template string substitution still works for ad-hoc YAML', () => {
  const out = renderDoughnutAppServiceUrlMapTemplate(
    `pathPrefixRewrite: /frontend/${FRONTEND_GITHUB_SHA_PLACEHOLDER}/x`,
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
  )
  assert.ok(
    out.includes('/frontend/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb/x')
  )
})
