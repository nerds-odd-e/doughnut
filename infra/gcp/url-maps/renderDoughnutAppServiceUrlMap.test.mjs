import assert from 'node:assert'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  FRONTEND_GITHUB_SHA_PLACEHOLDER,
  loadDoughnutRouting,
  renderDoughnutAppServiceUrlMapTemplate,
  renderDoughnutAppServiceUrlMapYamlFromRouting,
} from '../path-routing/doughnutRouting.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

test('render from routing JSON substitutes SHA in static rewrites', () => {
  const routingPath = path.join(
    __dirname,
    '../path-routing/doughnut-routing.json'
  )
  const routing = loadDoughnutRouting(routingPath)
  const sha = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
  const out = renderDoughnutAppServiceUrlMapYamlFromRouting(routing, sha)
  assert.ok(!out.includes(FRONTEND_GITHUB_SHA_PLACEHOLDER))
  assert.ok(out.includes(`/frontend/${sha}/assets/`))
  assert.equal((out.match(new RegExp(sha, 'g')) ?? []).length, 6)
})

test('render rejects non-hex or wrong length', () => {
  assert.throws(() =>
    renderDoughnutAppServiceUrlMapYamlFromRouting(
      loadDoughnutRouting(
        path.join(__dirname, '../path-routing/doughnut-routing.json')
      ),
      'short'
    )
  )
  assert.throws(() =>
    renderDoughnutAppServiceUrlMapYamlFromRouting(
      loadDoughnutRouting(
        path.join(__dirname, '../path-routing/doughnut-routing.json')
      ),
      'g'.repeat(40)
    )
  )
})

test('template string substitution still works for ad-hoc YAML', () => {
  const out = renderDoughnutAppServiceUrlMapTemplate(
    `pathPrefixRewrite: /frontend/${FRONTEND_GITHUB_SHA_PLACEHOLDER}/x`,
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
  )
  assert.ok(out.includes('/frontend/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb/x'))
})
