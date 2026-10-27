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
  let expiresAt
  const requests = []
  const replacements = []
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
      const previous = provider.tokens()
      writeFileSync(tokenFile, JSON.stringify(tokens), { mode: 0o600 })
      saved = true
      tokenGeneration++
      expiresAt = Date.now() + tokens.expires_in * 1000
      replacements.push({
        generation: tokenGeneration,
        accessChanged: previous?.access_token !== tokens.access_token,
        refreshChanged: previous?.refresh_token !== tokens.refresh_token,
        issuer: tokens.issuer,
        scope: tokens.scope,
        resource: requests.findLast((request) => request.grantType)?.resource,
        tokenEndpointOrigin: requests.findLast((request) => request.grantType)
          ?.origin,
        lifetimeSeconds: tokens.expires_in,
      })
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
          const form =
            init?.body instanceof URLSearchParams ? init.body : undefined
          const request = {
            path: new URL(String(input)).pathname,
            origin: new URL(String(input)).origin,
            method: init?.method ?? 'GET',
            generation: tokenGeneration,
            grantType: form?.get('grant_type'),
            resource: form?.get('resource'),
          }
          requests.push(request)
          const response = await tlsProxy.trustedFetch(input, init)
          request.status = response.status
          request.serverDate = response.headers.get('date')
          return response
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
      return {
        connected: saved,
        tokenGeneration,
        requests,
        replacements,
        redirectUrl,
        expiresAt,
      }
    },
    async waitForAccessExpiry() {
      if (!(saved && Number.isFinite(expiresAt)))
        throw new Error('No finite SDK access lifetime')
      // The endpoint rounds its advertised lifetime down to whole seconds.
      await new Promise((resolve) =>
        setTimeout(resolve, Math.max(0, expiresAt - Date.now() + 1100))
      )
      return { expired: Date.now() > expiresAt, tokenGeneration }
    },
    async expectWithdrawnTool(params) {
      try {
        await client.callTool(params)
      } catch (error) {
        if (error instanceof UnauthorizedError && authorizationUrl)
          return { authorizationRequired: true }
        throw error
      }
      throw new Error('Withdrawn assistant unexpectedly retained tool access')
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
