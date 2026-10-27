import https from 'node:https'
import { randomUUID } from 'node:crypto'
import {
  generateMcpTestTlsMaterial,
  certificateSpkiPin,
} from './mcp_tls_material.ts'
import { createCaTrustedFetch } from './mcp_tls_proxy.ts'
import { createHostedAssistantClient } from './hosted_assistant_client.mjs'
import { startGithubOAuthFixture } from './github_oauth_fixture.mjs'
import {
  HOSTED_ASSISTANT_CLIENT_NAME,
  HOSTED_ASSISTANT_CONTROL,
} from './hosted_assistant_constants.mjs'

/** Callback, provider boundary, SDK credentials and private controls share one owner. */
export async function startHostedAssistantFixture() {
  const material = generateMcpTestTlsMaterial()
  const capability = randomUUID()
  const clientId = 'hosted-sdk-assistant'
  const clientSecret = randomUUID()
  let assistant, github
  const server = https.createServer(
    { key: material.serverKeyPem, cert: material.serverCertPem },
    async (req, res) => {
      try {
        const url = new URL(req.url, 'https://fixture')
        if (url.pathname === '/callback' && req.method === 'GET') {
          await assistant.callback(url)
          res
            .writeHead(200, { 'content-type': 'text/html' })
            .end('<h1>Assistant connected</h1>')
          return
        }
        if (
          req.method !== 'POST' ||
          req.headers.authorization !== `Bearer ${capability}`
        ) {
          res.writeHead(404).end()
          return
        }
        let body = ''
        for await (const chunk of req) body += chunk
        const params = JSON.parse(body || '{}')
        let result
        if (url.pathname === '/control/begin') result = await assistant.begin()
        else if (url.pathname === '/control/status')
          result = { ...assistant.status(), githubVisits: github.visits }
        else if (url.pathname === '/control/tool')
          result = await assistant.callTool(params)
        else {
          res.writeHead(404).end()
          return
        }
        res.setHeader('content-type', 'application/json')
        res.end(JSON.stringify(result))
      } catch (error) {
        res.writeHead(400, { 'content-type': 'text/plain' }).end(error.message)
      }
    }
  )
  try {
    github = await startGithubOAuthFixture()
    await new Promise((resolve, reject) => {
      server.once('error', reject)
      server.listen(0, '127.0.0.1', resolve)
    })
    const origin = `https://127.0.0.1:${server.address().port}`
    const redirectUrl = `${origin}/callback`
    assistant = createHostedAssistantClient({
      redirectUrl,
      clientId,
      clientSecret,
    })
    return {
      configure(proxy) {
        assistant.configure(proxy)
        return [
          proxy.browserCertificatePin,
          certificateSpkiPin(material.serverCertPem),
        ].join(',')
      },
      control: { origin, capability, caCertPem: material.caCertPem },
      springConfiguration: {
        spring: { security: { oauth2: { client: github.springClient } } },
        donut: {
          oauth: {
            clients: [
              {
                clientId,
                clientName: HOSTED_ASSISTANT_CLIENT_NAME,
                clientSecret: `{noop}${clientSecret}`,
                redirectUris: [redirectUrl],
              },
            ],
          },
        },
      },
      async close() {
        try {
          await assistant.close()
        } finally {
          await Promise.all([
            github.close(),
            new Promise((resolve, reject) =>
              server.close((error) => (error ? reject(error) : resolve()))
            ),
          ])
        }
      },
    }
  } catch (error) {
    server.close()
    await github?.close()
    throw error
  }
}

/** Used only in the Cypress Node process; its capability is never exposed. */
export function hostedAssistantTasks(env = process.env) {
  const invoke = async (action, params = {}) => {
    const control = JSON.parse(env[HOSTED_ASSISTANT_CONTROL])
    const response = await createCaTrustedFetch(control.caCertPem)(
      `${control.origin}/control/${action}`,
      {
        method: 'POST',
        headers: {
          authorization: `Bearer ${control.capability}`,
          'content-type': 'application/json',
        },
        body: JSON.stringify(params),
      }
    )
    if (!response.ok) throw new Error(await response.text())
    return response.json()
  }
  return {
    assistantBegin: () => invoke('begin'),
    assistantStatus: () => invoke('status'),
    assistantCallTool: (params) => invoke('tool', params),
  }
}
