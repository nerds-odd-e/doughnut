import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js'
import { runE2eBatch } from '../../../scripts/e2e-runner.mjs'
import {
  getListenerPids,
  isOwnedByApplicationTree,
} from '../../../scripts/sut-listener-pids.mjs'
import { verifyLiveSutOwner } from '../../../scripts/sut-owner.mjs'
import { startGithubOAuthFixture } from '../../../e2e_test/support/github_oauth_fixture.mjs'

const root = fileURLToPath(new URL('../../../', import.meta.url))
const alive = (pid) => {
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}
const github = await startGithubOAuthFixture()
const clientId = 'durable-assistant'
const callback = 'https://assistant.example/callback'
const verifier = 'abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG'
const basic = `Basic ${Buffer.from(`${clientId}:durable-secret`).toString('base64')}`
const evidence = { startedAt: new Date().toISOString(), replacements: [] }
let finalPids = []
let retainedTls
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
      retainedTls = tlsProxy
      const origin = tlsProxy.httpsOrigin
      const resource = `${origin}/api/mcp`
      const databaseUrl = target.databaseUrl
      assert.match(target.database, /^doughnut_e2e_wt_/)
      const cookies = new Map()
      const clients = []
      const request = async (input, options = {}) => {
        const url = new URL(input, origin)
        assert.ok(url.origin === origin || url.origin === github.origin)
        const headers = { ...options.headers }
        if (url.origin === origin && cookies.size)
          headers.Cookie = [...cookies].map(([k, v]) => `${k}=${v}`).join('; ')
        const reply =
          url.origin === origin
            ? await tlsProxy.trustedFetch(url.href, { ...options, headers })
            : await fetch(url, { ...options, headers, redirect: 'manual' })
        if (url.origin === origin)
          for (const value of reply.headers.getSetCookie()) {
            const [pair] = value.split(';')
            const index = pair.indexOf('=')
            cookies.set(pair.slice(0, index), pair.slice(index + 1))
          }
        return reply
      }
      // This fetch deliberately has no access to the browser cookie jar.
      const bearerFetch = (input, options = {}) => {
        assert.equal(new Headers(options.headers).has('Cookie'), false)
        return tlsProxy.trustedFetch(input, options)
      }
      const form = (route, fields) =>
        bearerFetch(`${origin}${route}`, {
          method: 'POST',
          headers: {
            Authorization: basic,
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: new URLSearchParams(fields).toString(),
        })
      const token = async (fields) => {
        const reply = await form('/api/oauth/token', fields)
        assert.equal(reply.status, 200)
        const value = await reply.json()
        assert.ok(value.access_token && value.refresh_token)
        assert.equal(value.scope, 'notes:read')
        return value
      }
      const exchange = (code) =>
        token({
          grant_type: 'authorization_code',
          code,
          redirect_uri: callback,
          code_verifier: verifier,
          resource,
        })
      const refreshFields = (refreshToken) => ({
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
        resource,
      })
      const inspect = async (access) => {
        const reply = await form('/api/oauth/introspect', { token: access })
        assert.equal(reply.status, 200)
        const claims = await reply.json()
        assert.equal(claims.active, true)
        assert.equal(claims.sub, 'old_learner')
        assert.equal(claims.client_id, clientId)
        assert.equal(claims.scope, 'notes:read')
        assert.deepEqual(claims.aud, [resource])
        return {
          sub: claims.sub,
          client: claims.client_id,
          scope: claims.scope,
          resource: claims.aud,
        }
      }
      const authorize = new URL('/api/oauth/authorize', origin)
      authorize.search = new URLSearchParams({
        response_type: 'code',
        client_id: clientId,
        redirect_uri: callback,
        scope: 'notes:read',
        state: 'durability-client-state',
        resource,
        code_challenge: createHash('sha256')
          .update(verifier)
          .digest('base64url'),
        code_challenge_method: 'S256',
      })
      const redirected = (reply) => {
        assert.equal(reply.status, 302)
        return new URL(reply.headers.get('location'), origin)
      }
      const discovery = async () => {
        const result = []
        for (const route of [
          '/.well-known/oauth-authorization-server',
          '/.well-known/oauth-protected-resource/api/mcp',
        ]) {
          const reply = await bearerFetch(`${origin}${route}`)
          assert.equal(reply.status, 200)
          result.push(await reply.json())
        }
        assert.equal(result[0].issuer, origin)
        assert.equal(result[1].resource, resource)
        return result
      }
      const metadata = await discovery()
      const replace = async () => {
        const before = await verifyLiveSutOwner(root)
        const [oldJvm] = await getListenerPids(target.backendPort)
        assert.ok(oldJvm)
        assert.match(
          execFileSync('ps', ['-p', String(oldJvm), '-o', 'comm='], {
            encoding: 'utf8',
          }),
          /java/
        )
        assert.equal(
          await isOwnedByApplicationTree(oldJvm, before.applicationGroupId),
          true
        )
        const restarted = await restartApplication()
        assert.equal(restarted.ok, true)
        const after = await verifyLiveSutOwner(root)
        const newPids = await getListenerPids(target.backendPort)
        assert.equal(newPids.length, 1)
        const [newJvm] = newPids
        assert.equal(alive(oldJvm), false)
        assert.notEqual(newJvm, oldJvm)
        assert.match(
          execFileSync('ps', ['-p', String(newJvm), '-o', 'comm='], {
            encoding: 'utf8',
          }),
          /java/
        )
        assert.equal(
          await isOwnedByApplicationTree(newJvm, after.applicationGroupId),
          true
        )
        assert.equal(after.pid, before.pid)
        assert.notEqual(after.applicationGroupId, before.applicationGroupId)
        assert.equal(after.applicationGroupId, restarted.applicationGroupId)
        assert.equal(after.runnerLeaseHeld, true)
        assert.equal(tlsProxy.httpsOrigin, origin)
        assert.equal(target.databaseUrl, databaseUrl)
        assert.deepEqual(await discovery(), metadata)
        evidence.replacements.push({
          oldJvm,
          newJvm,
          supervisor: after.pid,
          oldGroup: before.applicationGroupId,
          newGroup: after.applicationGroupId,
        })
        finalPids = [after.pid, newJvm]
        console.log(
          JSON.stringify({
            replacement: evidence.replacements.length,
            oldJvm,
            newJvm,
            stableOriginAndDiscovery: true,
            database: target.database,
          })
        )
      }
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
        const noteReply = await request('/api/testability/inject_notes', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            externalIdentifier: 'old_learner',
            notebookName: 'Durability Notebook',
            noteTestData: [
              {
                Title: 'Durable Evidence',
                Content:
                  'Private MySQL marker retained across two JVM replacements',
              },
            ],
          }),
        })
        assert.equal(noteReply.status, 200)
        const noteId = (await noteReply.json())['Durable Evidence']
        const connect = async (access) => {
          const sdk = new Client(
            { name: 'authorization-durability-proof', version: '1' },
            { capabilities: {} }
          )
          clients.push(sdk)
          await sdk.connect(
            new StreamableHTTPClientTransport(new URL(resource), {
              requestInit: { headers: { Authorization: `Bearer ${access}` } },
              fetch: bearerFetch,
            })
          )
          return sdk
        }
        const read = async (sdk) => {
          const result = JSON.parse(
            (await sdk.callTool({ name: 'read_note', arguments: { noteId } }))
              .content[0].text
          )
          assert.equal(
            result.content,
            'Private MySQL marker retained across two JVM replacements'
          )
        }
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
        for (const sdk of clients) await sdk.close()
      }
      return 0
    },
  })
  assert.equal(exitCode, 0)
  assert.equal(
    finalPids.every((pid) => !alive(pid)),
    true
  )
  assert.equal((await verifyLiveSutOwner(root)).ok, false)
  await assert.rejects(retainedTls.trustedFetch(`${retainedTls.httpsOrigin}/`))
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
