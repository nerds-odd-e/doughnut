import http from 'node:http'
import https from 'node:https'
import { Readable } from 'node:stream'
import type { IncomingMessage } from 'node:http'
import { generateMcpTestTlsMaterial } from './mcp_tls_material.ts'

export type McpTlsProxy = {
  httpsOrigin: string
  caCertPem: string
  forwardOrigin: string
  trustedFetch: typeof fetch
  close: () => Promise<void>
}

/**
 * Test-only HTTPS endpoint that terminates TLS with an ephemeral test CA and
 * forwards to the isolated application HTTP origin. Not a product service.
 */
export async function startMcpTlsProxy(
  forwardOrigin: string
): Promise<McpTlsProxy> {
  const upstream = new URL(forwardOrigin)
  if (upstream.protocol !== 'http:') {
    throw new Error(
      `MCP TLS fixture forwards only to http origins, got ${forwardOrigin}`
    )
  }
  const material = generateMcpTestTlsMaterial()
  const server = https.createServer(
    {
      key: material.serverKeyPem,
      cert: material.serverCertPem,
      ca: material.caCertPem,
    },
    (clientReq, clientRes) => {
      const headers = { ...clientReq.headers, host: upstream.host }
      delete headers.connection
      const proxyReq = http.request(
        {
          protocol: upstream.protocol,
          hostname: upstream.hostname,
          port: upstream.port || '80',
          path: clientReq.url,
          method: clientReq.method,
          headers,
        },
        (proxyRes) => {
          clientRes.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers)
          proxyRes.pipe(clientRes)
        }
      )
      proxyReq.on('error', (err) => {
        if (!clientRes.headersSent) {
          clientRes.writeHead(502, { 'content-type': 'text/plain' })
        }
        clientRes.end(`MCP TLS fixture upstream error: ${err.message}`)
      })
      clientReq.pipe(proxyReq)
    }
  )

  await new Promise<void>((resolve, reject) => {
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => resolve())
  })
  const address = server.address()
  if (address === null || typeof address === 'string') {
    throw new Error('MCP TLS fixture failed to bind a TCP port')
  }
  const httpsOrigin = `https://127.0.0.1:${address.port}`

  return {
    httpsOrigin,
    caCertPem: material.caCertPem,
    forwardOrigin,
    trustedFetch: createCaTrustedFetch(material.caCertPem),
    close: () =>
      new Promise<void>((resolve, reject) => {
        server.close((err) => (err ? reject(err) : resolve()))
      }),
  }
}

export function createCaTrustedFetch(caCertPem: string): typeof fetch {
  return async (input, init = {}) => {
    const url = new URL(typeof input === 'string' ? input : input.toString())
    if (url.protocol !== 'https:') {
      throw new Error(`MCP TLS client expects https URLs, got ${url.protocol}`)
    }
    const method = init.method ?? 'GET'
    const headers = new Headers(init.headers)
    const body = await readRequestBody(init.body)

    return await new Promise<Response>((resolve, reject) => {
      if (init.signal?.aborted) {
        reject(init.signal.reason ?? new Error('aborted'))
        return
      }
      const req = https.request(
        {
          protocol: url.protocol,
          hostname: url.hostname,
          port: url.port,
          path: `${url.pathname}${url.search}`,
          method,
          headers: Object.fromEntries(headers.entries()),
          ca: caCertPem,
          servername: 'localhost',
          rejectUnauthorized: true,
        },
        (res) => resolve(nodeResponseToWeb(res))
      )
      const onAbort = () => {
        req.destroy(init.signal?.reason ?? new Error('aborted'))
      }
      init.signal?.addEventListener('abort', onAbort, { once: true })
      req.on('error', reject)
      if (body !== undefined) {
        req.write(body)
      }
      req.end()
    })
  }
}

async function readRequestBody(
  body: BodyInit | null | undefined
): Promise<Buffer | undefined> {
  if (body === undefined || body === null) return undefined
  if (typeof body === 'string') return Buffer.from(body)
  if (body instanceof Uint8Array) return Buffer.from(body)
  if (body instanceof ArrayBuffer) return Buffer.from(body)
  if (typeof Blob !== 'undefined' && body instanceof Blob) {
    return Buffer.from(await body.arrayBuffer())
  }
  throw new Error(
    `Unsupported MCP TLS fetch body type: ${Object.prototype.toString.call(body)}`
  )
}

function nodeResponseToWeb(res: IncomingMessage): Response {
  const headers = new Headers()
  for (const [name, value] of Object.entries(res.headers)) {
    if (value === undefined) continue
    if (Array.isArray(value)) {
      for (const item of value) headers.append(name, item)
    } else {
      headers.set(name, value)
    }
  }
  const webStream = Readable.toWeb(res) as unknown as ReadableStream
  return new Response(webStream, {
    status: res.statusCode ?? 500,
    statusText: res.statusMessage,
    headers,
  })
}
