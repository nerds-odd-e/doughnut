import assert from 'node:assert/strict'
import { SUPPORTED_ISOLATED_CYPRESS_SPEC } from './isolated-cypress.mjs'
import { completeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import { isolatedBrowserOrigin } from './sut-runtime-target.mjs'

export const isolatedCypressSpec = /only supports|spec selection/i
export const isolatedOrigin = isolatedBrowserOrigin(completeIsolatedConfig.e2e)

export function cypressArgv(spec = SUPPORTED_ISOLATED_CYPRESS_SPEC) {
  return ['node', 'cypress', 'run', '--spec', spec]
}

export function isolatedCypressOpts(extra = {}) {
  const env = { ...process.env, ...(extra.env ?? {}) }
  if (!(extra.env && Object.hasOwn(extra.env, 'CYPRESS_baseUrl'))) {
    delete env.CYPRESS_baseUrl
  }
  const opts = {
    argv: extra.argv ?? cypressArgv(),
    env,
    on: extra.on,
    startPrivateOpenAiMockFn: extra.startPrivateOpenAiMockFn,
  }
  if (extra.realHealthcheck) {
    return opts
  }
  opts.healthcheckFn = extra.healthcheckFn ?? (async () => ({ ok: true }))
  return opts
}

export function supportedConfig(
  baseUrl = 'http://localhost:5173',
  specPattern = SUPPORTED_ISOLATED_CYPRESS_SPEC
) {
  return { specPattern, baseUrl }
}

export async function assertRefusesBeforeReset(run, pattern) {
  const hooks = { reset: false }
  await assert.rejects(async () => {
    await run()
    hooks.reset = true
  }, pattern)
  assert.equal(hooks.reset, false)
}
