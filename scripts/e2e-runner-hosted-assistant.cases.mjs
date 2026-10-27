import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { LEGACY_SUT_RUNTIME_TARGET } from './sut-runtime-target.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import { createCaTrustedFetch } from '../e2e_test/support/mcp_tls_proxy.ts'
import { hostedAssistantTasks } from '../e2e_test/support/hosted_assistant_fixture.mjs'
import {
  ASSISTANT_LINKING_SPEC,
  HOSTED_ASSISTANT_CONTROL,
  HOSTED_ASSISTANT_BROWSER_PINS,
  HOSTED_ASSISTANT_BROWSER_ORIGIN,
} from '../e2e_test/support/hosted_assistant_constants.mjs'

for (const { ready, spec, browser } of [
  { ready: true, spec: ASSISTANT_LINKING_SPEC },
  { ready: false, spec: ASSISTANT_LINKING_SPEC },
  { ready: true, spec: 'e2e_test/features/mcp/**' },
  { ready: true, spec: ASSISTANT_LINKING_SPEC, browser: 'electron' },
]) {
  test(`hosted SDK fixture ${spec} (${browser ?? 'default browser'}) owns configuration and closes on ${ready ? 'completion' : 'readiness failure'}`, async (t) => {
    const checkout = makePrimaryCheckout(t)
    let control,
      callback,
      githubOrigin,
      configuration,
      shutdown = false
    const code = await runE2eBatch({
      argv: ['--spec', spec, ...(browser ? ['--browser', browser] : [])],
      checkoutRoot: checkout.root,
      runtimeTarget: LEGACY_SUT_RUNTIME_TARGET,
      isPortOccupiedFn: async () => false,
      env: {},
      errLog: () => undefined,
      startLifetime: async (options) => {
        assert.equal(options.https, true)
        configuration = JSON.parse(options.env.SPRING_APPLICATION_JSON)
        const configured = configuration.donut.oauth.clients[0]
        callback = configured.redirectUris[0]
        githubOrigin = new URL(
          configuration.spring.security.oauth2.client.provider.github
            .authorizationUri
        ).origin
        assert.match(callback, /^https:\/\/127\.0\.0\.1:\d+\/callback$/)
        assert.match(configured.clientSecret, /^\{noop\}.+/)
        assert.equal((await fetch(`${githubOrigin}/unknown`)).status, 404)
        return {
          child: new EventEmitter(),
          ready: Promise.resolve({ ok: ready, exitCode: ready ? 0 : 1 }),
          tlsProxy: {
            httpsOrigin: 'https://127.0.0.1:1',
            browserCertificatePin: 'owned-mcp-leaf-pin',
          },
          shutdown: async () => {
            shutdown = true
          },
        }
      },
      session: async ({ env, browser: selectedBrowser }) => {
        assert.equal(selectedBrowser, browser ?? 'chrome')
        assert.equal(
          env[HOSTED_ASSISTANT_BROWSER_ORIGIN],
          'https://127.0.0.1:1'
        )
        control = JSON.parse(env[HOSTED_ASSISTANT_CONTROL])
        assert.match(
          env[HOSTED_ASSISTANT_BROWSER_PINS],
          /^owned-mcp-leaf-pin,[A-Za-z0-9+/]+=*$/
        )
        assert.equal(
          env.ELECTRON_EXTRA_LAUNCH_ARGS,
          `--ignore-certificate-errors-spki-list=${env[HOSTED_ASSISTANT_BROWSER_PINS]}`
        )
        const request = createCaTrustedFetch(control.caCertPem)
        assert.equal(
          (
            await request(`${control.origin}/control/status`, {
              method: 'POST',
            })
          ).status,
          404
        )
        const invalid = await request(`${callback}?code=unused&state=wrong`)
        assert.equal(invalid.status, 400)
        assert.equal(await invalid.text(), 'Hosted callback state mismatch')
        assert.deepEqual(await hostedAssistantTasks(env).assistantStatus(), {
          connected: false,
          tokenGeneration: 0,
          requests: [],
          redirectUrl: callback,
          githubVisits: ['/unknown'],
        })
        return 0
      },
    })
    assert.equal(code, ready ? 0 : 1)
    assert.equal(shutdown, true)
    await assert.rejects(fetch(`${githubOrigin}/unknown`))
    if (control)
      await assert.rejects(createCaTrustedFetch(control.caCertPem)(callback))
  })
}
