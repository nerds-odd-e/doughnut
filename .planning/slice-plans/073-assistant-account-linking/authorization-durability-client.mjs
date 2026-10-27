import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js'

export const clientId = 'durable-assistant'
export const callback = 'https://assistant.example/callback'
const verifier = 'abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG'
const basic = `Basic ${Buffer.from(`${clientId}:durable-secret`).toString('base64')}`

export function createDurabilityClient({ tlsProxy, github }) {
  const origin = tlsProxy.httpsOrigin
  const resource = `${origin}/api/mcp`
  const cookies = new Map()
  const clients = []
  let noteId
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
    code_challenge: createHash('sha256').update(verifier).digest('base64url'),
    code_challenge_method: 'S256',
  })
  const redirected = (reply) => {
    assert.equal(reply.status, 302)
    return new URL(reply.headers.get('location'), origin)
  }
  const injectPrivateNote = async () => {
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
    noteId = (await noteReply.json())['Durable Evidence']
  }
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
  return {
    origin,
    resource,
    request,
    bearerFetch,
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
    close: async () => {
      for (const sdk of clients) await sdk.close()
    },
  }
}
