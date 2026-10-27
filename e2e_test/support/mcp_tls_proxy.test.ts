import assert from 'node:assert/strict'
import http from 'node:http'
import { after, describe, test } from 'node:test'
import { MCP_HTTP_PATH } from './mcp_note_context_tools'
import { createCaTrustedFetch, startMcpTlsProxy } from './mcp_tls_proxy'

describe('MCP TLS proxy fixture', () => {
  let proxy: Awaited<ReturnType<typeof startMcpTlsProxy>> | undefined
  let upstream: http.Server | undefined

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

  test('trusted fetch verifies the test CA; global fetch does not', async () => {
    upstream = http.createServer((req, res) => {
      res.writeHead(200, { 'content-type': 'application/json' })
      res.end(
        JSON.stringify({
          path: req.url,
          authorization: req.headers.authorization ?? null,
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
    const url = `${proxy.httpsOrigin}${MCP_HTTP_PATH}`

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
    })
  })
})
