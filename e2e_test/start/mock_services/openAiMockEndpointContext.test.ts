import assert from 'node:assert/strict'
import http from 'node:http'
import { after, before, describe, test } from 'node:test'
import MountebankWrapper from '../../support/MountebankWrapper'
import ServiceMocker from '../../support/ServiceMocker'
import { fetchOpenAiImposterRequests } from './openAiImposterRecordedRequests'
import {
  type OpenAiMockEndpointContext,
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
  SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT,
  openAiImposterRequestsUrl,
  resolveOpenAiMockEndpointContext,
} from './openAiMockEndpointContext'

type RecordedHttpCall = {
  method: string
  url: string
  body: string
}

const readBody = (req: http.IncomingMessage): Promise<string> =>
  new Promise((resolve, reject) => {
    const chunks: Buffer[] = []
    req.on('data', (chunk) => chunks.push(chunk))
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')))
    req.on('error', reject)
  })

describe('OpenAI mock endpoint context', () => {
  const servingPort = 18001
  const recorded: RecordedHttpCall[] = []
  let managementUrl = ''
  let server: http.Server
  let endpoint: OpenAiMockEndpointContext

  before(async () => {
    server = http.createServer(async (req, res) => {
      const body = await readBody(req)
      const method = req.method ?? ''
      const url = req.url ?? ''
      recorded.push({ method, url, body })

      if (method === 'DELETE' && url.startsWith('/imposters/')) {
        res.writeHead(200, { 'Content-Type': 'application/json' })
        res.end('{}')
        return
      }
      if (method === 'POST' && url === '/imposters') {
        res.writeHead(201, { 'Content-Type': 'application/json' })
        res.end('{}')
        return
      }
      if (method === 'GET' && url === `/imposters/${servingPort}`) {
        res.writeHead(200, { 'Content-Type': 'application/json' })
        res.end(
          JSON.stringify({
            requests: [
              { method: 'POST', path: '/responses', body: '{"marker":"own"}' },
            ],
          })
        )
        return
      }
      res.writeHead(404)
      res.end('not found')
    })

    await new Promise<void>((resolve) => {
      server.listen(0, '127.0.0.1', () => resolve())
    })
    const address = server.address()
    assert.ok(address && typeof address === 'object')
    managementUrl = `http://127.0.0.1:${address.port}`
    endpoint = { managementUrl, servingPort }
  })

  after(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((err) => (err ? reject(err) : resolve()))
    })
  })

  test('mock setup posts the serving imposter through the explicit management URL', async () => {
    recorded.length = 0
    const wrapper = new MountebankWrapper(
      endpoint.servingPort,
      endpoint.managementUrl
    )
    await wrapper.createImposter()

    assert.equal(wrapper.serviceUrl, `http://localhost:${servingPort}`)
    const create = recorded.find(
      (call) => call.method === 'POST' && call.url === '/imposters'
    )
    assert.ok(create, 'expected POST /imposters on the management endpoint')
    assert.match(create.body, new RegExp(`"port"\\s*:\\s*${servingPort}`))
  })

  test('ServiceMocker exposes the serving URL from the same endpoint context', () => {
    const mocker = new ServiceMocker(
      'openAi',
      endpoint.servingPort,
      endpoint.managementUrl
    )
    assert.equal(mocker.serviceUrl, `http://localhost:${servingPort}`)
  })

  test('recorded-request fetch reads through the same management and serving endpoint', async () => {
    recorded.length = 0
    const requests = await fetchOpenAiImposterRequests(
      endpoint,
      async (url) => {
        assert.equal(url, openAiImposterRequestsUrl(endpoint))
        const res = await fetch(url)
        return { status: res.status, body: await res.json() }
      }
    )

    assert.deepEqual(requests, [
      { method: 'POST', path: '/responses', body: '{"marker":"own"}' },
    ])
    assert.ok(
      recorded.some(
        (call) =>
          call.method === 'GET' && call.url === `/imposters/${servingPort}`
      )
    )
  })

  test('shared primary/CI defaults remain selectable at the OpenAI boundary', () => {
    assert.deepEqual(SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT, {
      managementUrl: 'http://localhost:2525',
      servingPort: 5001,
    })
    const mocker = new ServiceMocker(
      'openAi',
      SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT.servingPort,
      SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT.managementUrl
    )
    assert.equal(mocker.serviceUrl, 'http://localhost:5001')
    assert.equal(
      openAiImposterRequestsUrl(SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT),
      'http://localhost:2525/imposters/5001'
    )
  })

  test('resolveOpenAiMockEndpointContext keeps shared defaults without injection', () => {
    assert.deepEqual(
      resolveOpenAiMockEndpointContext(() => undefined),
      SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT
    )
  })

  test('isolated flag refuses incomplete context instead of shared fallback', () => {
    assert.throws(
      () =>
        resolveOpenAiMockEndpointContext((key) =>
          key === ISOLATED_OPEN_AI_MOCK_ENV_KEY ? true : undefined
        ),
      /refusing shared 2525\/5001 fallback/
    )
  })

  test('resolveOpenAiMockEndpointContext requires a complete injected context when isolated', () => {
    const endpoint = {
      managementUrl: 'http://127.0.0.1:18025',
      servingPort: 18001,
    }
    assert.deepEqual(
      resolveOpenAiMockEndpointContext((key) => {
        if (key === ISOLATED_OPEN_AI_MOCK_ENV_KEY) return true
        if (key === OPEN_AI_MOCK_ENDPOINT_ENV_KEY) return endpoint
        return
      }),
      endpoint
    )
  })
})
