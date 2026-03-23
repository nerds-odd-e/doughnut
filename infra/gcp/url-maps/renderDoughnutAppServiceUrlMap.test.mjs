import assert from 'node:assert'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  FRONTEND_GITHUB_SHA_PLACEHOLDER,
  renderDoughnutAppServiceUrlMapTemplate,
} from './renderDoughnutAppServiceUrlMap.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

test('render substitutes every placeholder with the commit sha', () => {
  const templatePath = path.join(
    __dirname,
    'doughnut-app-service-map.template.yaml'
  )
  const templateText = readFileSync(templatePath, 'utf8')
  const sha = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
  const out = renderDoughnutAppServiceUrlMapTemplate(templateText, sha)
  assert.ok(!out.includes(FRONTEND_GITHUB_SHA_PLACEHOLDER))
  assert.ok(out.includes(`/frontend/${sha}/assets/`))
  assert.equal((out.match(new RegExp(sha, 'g')) ?? []).length, 5)
})

test('render rejects non-hex or wrong length', () => {
  assert.throws(() =>
    renderDoughnutAppServiceUrlMapTemplate('__FRONTEND_GITHUB_SHA__', 'short')
  )
  assert.throws(() =>
    renderDoughnutAppServiceUrlMapTemplate('__FRONTEND_GITHUB_SHA__', 'g'.repeat(40))
  )
})
