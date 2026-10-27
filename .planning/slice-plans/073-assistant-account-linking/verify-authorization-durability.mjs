import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import { runE2eBatch } from '../../../scripts/e2e-runner.mjs'
import { startGithubOAuthFixture } from '../../../e2e_test/support/github_oauth_fixture.mjs'
import {
  callback,
  clientId,
  createDurabilityClient,
} from './authorization-durability-client.mjs'
import { createProcessDurabilityProof } from './authorization-durability-process.mjs'

const root = fileURLToPath(new URL('../../../', import.meta.url))
const github = await startGithubOAuthFixture()
const evidence = { startedAt: new Date().toISOString(), replacements: [] }
let processProof
try {
  const exitCode = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/mcp/mcp_services.feature'],
    https: true,
    paidOpenAi: false,
    timeoutMs: 240000,
    sutEnv: {
      ...process.env,
      SPRING_APPLICATION_JSON: JSON.stringify({
        spring: { security: { oauth2: { client: github.springClient } } },
        donut: {
          oauth: {
            clients: [
              {
                clientId,
                clientName: 'Durable assistant',
                clientSecret: '{noop}durable-secret',
                redirectUris: [callback],
              },
            ],
          },
        },
      }),
    },
    session: async ({ tlsProxy, target, restartApplication }) => {
      const client = createDurabilityClient({ tlsProxy, github })
      const {
        origin,
        resource,
        request,
        form,
        token,
        exchange,
        refreshFields,
        inspect,
        authorize,
        redirected,
        injectPrivateNote,
        connect,
        read,
      } = client
      processProof = await createProcessDurabilityProof({
        root,
        tlsProxy,
        target,
        restartApplication,
        bearerFetch: client.bearerFetch,
        evidence,
      })
      const { replace } = processProof
      try {
        assert.equal(
          (
            await request(
              '/api/testability/clean_db_and_reset_testability_settings',
              { method: 'POST' }
            )
          ).status,
          200
        )
        const login = redirected(await request(authorize))
        assert.equal(login.origin, origin)
        const loginPage = await request(login)
        assert.equal(loginPage.status, 200)
        assert.match(await loginPage.text(), /oauth2\/authorization\/github/)
        const provider = redirected(
          await request('/oauth2/authorization/github')
        )
        assert.equal(provider.origin, github.origin)
        assert.equal(provider.searchParams.get('scope'), 'read:user')
        assert.equal(
          new URL(provider.searchParams.get('redirect_uri')).origin,
          origin
        )
        const githubCallback = redirected(await request(provider))
        assert.equal(githubCallback.origin, origin)
        assert.equal(githubCallback.pathname, '/login/oauth2/code/github')
        const resumed = redirected(await request(githubCallback))
        assert.equal(resumed.origin, origin)
        for (const [key, value] of authorize.searchParams)
          assert.equal(resumed.searchParams.get(key), value)
        const profile = await request('/api/user')
        assert.equal(profile.status, 200)
        const user = await profile.json()
        assert.equal(user.name, 'Old Learner')
        assert.ok(user.id)
        assert.deepEqual(github.visits, ['/authorize', '/token', '/user'])
        const consent = redirected(await request(resumed))
        assert.equal(consent.pathname, '/assistant-consent')
        const contextReply = await request(
          `/api/oauth/consent-context?client_id=${clientId}&state=${encodeURIComponent(consent.searchParams.get('state'))}`
        )
        assert.equal(contextReply.status, 200)
        const context = await contextReply.json()
        assert.equal(context.accountName, user.name)
        assert.equal(context.clientName, 'Durable assistant')
        assert.equal(context.resource, resource)
        assert.deepEqual(context.scopes, ['notes:read'])
        const fields = new URLSearchParams({
          client_id: context.clientId,
          state: context.state,
          scope: 'notes:read',
          [context.csrfParameterName]: context.csrfToken,
        })
        const allowed = redirected(
          await request(context.formAction, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: fields.toString(),
          })
        )
        assert.equal(allowed.origin + allowed.pathname, callback)
        const first = await exchange(allowed.searchParams.get('code'))
        const binding = await inspect(first.access_token)
        await injectPrivateNote()
        await read(await connect(first.access_token))
        await replace()
        const renewed = await token(refreshFields(first.refresh_token))
        assert.notEqual(renewed.access_token, first.access_token)
        assert.notEqual(renewed.refresh_token, first.refresh_token)
        assert.deepEqual(await inspect(renewed.access_token), binding)
        const renewedSdk = await connect(renewed.access_token)
        await read(renewedSdk)
        const retainedProfile = await request('/api/user')
        assert.equal(retainedProfile.status, 200)
        assert.equal((await retainedProfile.json()).id, user.id)
        const remembered = redirected(await request(authorize))
        assert.equal(remembered.origin + remembered.pathname, callback)
        assert.equal(
          remembered.searchParams.get('state'),
          'durability-client-state'
        )
        const additional = await exchange(remembered.searchParams.get('code'))
        assert.deepEqual(await inspect(additional.access_token), binding)
        const additionalSdk = await connect(additional.access_token)
        await read(additionalSdk)
        const managementReply = await request('/api/oauth/assistants')
        assert.equal(managementReply.status, 200)
        const management = await managementReply.json()
        assert.deepEqual(management.assistants, [
          { clientId, clientName: 'Durable assistant', scopes: ['notes:read'] },
        ])
        assert.equal(
          (
            await request(`/api/oauth/assistants/${clientId}`, {
              method: 'DELETE',
              headers: { [management.csrfHeaderName]: management.csrfToken },
            })
          ).status,
          200
        )
        await replace()
        for (const sdk of [renewedSdk, additionalSdk])
          await assert.rejects(
            () => read(sdk),
            (error) => error.code === 401 || /401/.test(error.message)
          )
        for (const credentials of [renewed, additional]) {
          const refused = await form(
            '/api/oauth/token',
            refreshFields(credentials.refresh_token)
          )
          assert.equal(refused.status, 400)
          assert.equal((await refused.json()).error, 'invalid_grant')
          await assert.rejects(
            () => connect(credentials.access_token),
            (error) => error.code === 401 || /401/.test(error.message)
          )
        }
        const reconnect = redirected(await request(authorize))
        assert.equal(reconnect.origin, origin)
        assert.equal(reconnect.pathname, '/assistant-consent')
        assert.equal(reconnect.searchParams.get('client_id'), clientId)
        assert.deepEqual(github.visits, ['/authorize', '/token', '/user'])
        Object.assign(evidence, {
          origin,
          database: target.database,
          binding,
          browserSessionUser: user.id,
          resetCount: 1,
          durableRenewalAndSdkRead: true,
          rememberedConsent: true,
          withdrawalSurvivesRestart: true,
          reconnectRequiresConsent: true,
          githubVisits: github.visits,
        })
      } finally {
        await client.close()
      }
      return 0
    },
  })
  assert.equal(exitCode, 0)
  await processProof.assertClosed()
  console.log(
    JSON.stringify({
      ...evidence,
      completedAt: new Date().toISOString(),
      finalOwnedProcessesGone: true,
      tlsClosed: true,
      ownerReleased: true,
    })
  )
} finally {
  await github.close()
  console.log('Owned GitHub fixture and stack shutdown completed')
}
