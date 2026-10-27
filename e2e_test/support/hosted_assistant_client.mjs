import { randomUUID, createHash } from 'node:crypto'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { UnauthorizedError } from '@modelcontextprotocol/sdk/client/auth.js'
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js'
import { HOSTED_ASSISTANT_CLIENT_NAME } from './hosted_assistant_constants.mjs'

/** Server-held SDK provider. Browser and task replies never contain credentials. */
export function createHostedAssistantClient({
  redirectUrl,
  clientId,
  clientSecret,
}) {
  const directory = mkdtempSync(join(tmpdir(), 'donut-assistant-'))
  const tokenFile = join(directory, 'tokens.json')
  let tlsProxy, transport, client, authorizationUrl, state, verifier
  let saved = false
  let tokenGeneration = 0
  let callbackError
  const requests = []
  const provider = {
    redirectUrl,
    clientMetadata: {
      client_name: HOSTED_ASSISTANT_CLIENT_NAME,
      redirect_uris: [redirectUrl],
      grant_types: ['authorization_code', 'refresh_token'],
      response_types: ['code'],
      token_endpoint_auth_method: 'client_secret_basic',
      scope: 'notes:read',
    },
    state: () => (state = randomUUID()),
    clientInformation: () => ({
      client_id: clientId,
      client_secret: clientSecret,
    }),
    tokens: () =>
      saved ? JSON.parse(readFileSync(tokenFile, 'utf8')) : undefined,
    saveTokens: (tokens) => {
      writeFileSync(tokenFile, JSON.stringify(tokens), { mode: 0o600 })
      saved = true
      tokenGeneration++
    },
    redirectToAuthorization: (url) => {
      authorizationUrl = url
    },
    saveCodeVerifier: (value) => {
      verifier = value
    },
    codeVerifier: () => verifier,
    invalidateCredentials: (scope) => {
      if (scope === 'all' || scope === 'tokens') {
        saved = false
        rmSync(tokenFile, { force: true })
      }
      if (scope === 'all' || scope === 'verifier') verifier = undefined
    },
  }
  const connect = async () => {
    client = new Client(
      { name: 'hosted-sdk-assistant', version: '1' },
      { capabilities: {} }
    )
    transport = new StreamableHTTPClientTransport(
      new URL('/api/mcp', tlsProxy.httpsOrigin),
      {
        authProvider: provider,
        fetch: async (input, init) => {
          if (new Headers(init?.headers).has('cookie'))
            throw new Error('Hosted SDK must use cookie-free requests')
          requests.push({
            path: new URL(String(input)).pathname,
            method: init?.method ?? 'GET',
          })
          return tlsProxy.trustedFetch(input, init)
        },
      }
    )
    await client.connect(transport)
  }
  return {
    configure: (proxy) => {
      tlsProxy = proxy
    },
    async begin() {
      await client?.close()
      authorizationUrl = undefined
      callbackError = undefined
      try {
        await connect()
        return { connected: true }
      } catch (error) {
        if (!(error instanceof UnauthorizedError && authorizationUrl))
          throw error
        const challenge = createHash('sha256')
          .update(verifier)
          .digest('base64url')
        if (
          authorizationUrl.searchParams.get('code_challenge_method') !==
            'S256' ||
          authorizationUrl.searchParams.get('code_challenge') !== challenge
        ) {
          throw new Error('SDK authorization must use the saved S256 verifier')
        }
        return { authorizationUrl: authorizationUrl.href }
      }
    },
    async callback(url) {
      if (!state || url.searchParams.get('state') !== state)
        throw new Error('Hosted callback state mismatch')
      state = undefined
      if (url.searchParams.has('error'))
        throw new Error(
          `Authorization refused: ${url.searchParams.get('error')}`
        )
      const code = url.searchParams.get('code')
      if (!code)
        throw new Error('Hosted callback requires an authorization code')
      try {
        await transport.finishAuth(code)
        await client.close()
        await connect()
      } catch (error) {
        callbackError = error.message
        throw error
      }
    },
    status() {
      if (callbackError) throw new Error(callbackError)
      return { connected: saved, tokenGeneration, requests, redirectUrl }
    },
    callTool: ({ name, arguments: args }) =>
      client.callTool({ name, arguments: args }),
    async close() {
      try {
        await client?.close()
      } finally {
        rmSync(directory, { recursive: true, force: true })
      }
    },
  }
}
