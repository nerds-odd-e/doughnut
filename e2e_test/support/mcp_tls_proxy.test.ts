import assert from 'node:assert/strict'
import http from 'node:http'
import tls from 'node:tls'
import { createHash, X509Certificate } from 'node:crypto'
import { after, before, describe, test } from 'node:test'
import { MCP_HTTP_PATH } from './mcp_note_context_tools'
import { createCaTrustedFetch, startMcpTlsProxy } from './mcp_tls_proxy'

describe('MCP TLS proxy fixture', () => {
  let proxy: Awaited<ReturnType<typeof startMcpTlsProxy>>
  let upstream: http.Server | undefined
  let url: string

  after(async () => {
    await proxy?.close()
    await new Promise<void>((resolve, reject) => {
      if (!upstream) {
        resolve()
        return
      }
      upstream.close((err) => (err ? reject(err) : resolve()))
    })
  })

  before(async () => {
    upstream = http.createServer(async (req, res) => {
      let body = ''
      for await (const chunk of req) body += chunk
      res.writeHead(200, { 'content-type': 'application/json' })
      res.end(
        JSON.stringify({
          path: req.url,
          authorization: req.headers.authorization ?? null,
          body,
        })
      )
    })
    await new Promise<void>((resolve) =>
      upstream!.listen(0, '127.0.0.1', () => resolve())
    )
    const address = upstream.address()
    assert.ok(address && typeof address !== 'string')
    const forwardOrigin = `http://127.0.0.1:${address.port}`
    proxy = await startMcpTlsProxy(forwardOrigin)
    url = `${proxy.httpsOrigin}${MCP_HTTP_PATH}`
  })

  test('trusted fetch verifies the test CA; global fetch does not', async () => {
    await assert.rejects(
      () => fetch(url),
      (err: unknown) => {
        const code =
          err &&
          typeof err === 'object' &&
          'cause' in err &&
          err.cause &&
          typeof err.cause === 'object' &&
          'code' in err.cause
            ? String((err.cause as { code: unknown }).code)
            : String(err)
        return /UNABLE_TO_VERIFY|CERT/i.test(code)
      }
    )

    const trusted = createCaTrustedFetch(proxy.caCertPem)
    const response = await trusted(url, {
      method: 'POST',
      headers: {
        Authorization: 'Bearer test-token',
        'content-type': 'application/json',
      },
      body: '{}',
    })
    assert.equal(response.status, 200)
    assert.deepEqual(await response.json(), {
      path: MCP_HTTP_PATH,
      authorization: 'Bearer test-token',
      body: '{}',
    })
  })

  test('trusted fetch forwards SDK URLSearchParams form bytes', async () => {
    const form = await proxy.trustedFetch(url, {
      method: 'POST',
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code: 'fixture-code',
      }),
    })
    assert.equal(
      (await form.json()).body,
      'grant_type=authorization_code&code=fixture-code'
    )
  })

  test('browser certificate pin identifies the served TLS leaf', async () => {
    const peerCertificate = await new Promise<Buffer>((resolve, reject) => {
      const socket = tls.connect(
        {
          host: '127.0.0.1',
          port: Number(new URL(url).port),
          ca: proxy.caCertPem,
          servername: 'localhost',
          rejectUnauthorized: true,
        },
        () => {
          resolve(socket.getPeerCertificate().raw)
          socket.end()
        }
      )
      socket.once('error', reject)
    })
    const servedPublicKey = new X509Certificate(
      peerCertificate
    ).publicKey.export({ type: 'spki', format: 'der' })
    assert.equal(
      proxy.browserCertificatePin,
      createHash('sha256').update(servedPublicKey).digest('base64')
    )
  })
})
