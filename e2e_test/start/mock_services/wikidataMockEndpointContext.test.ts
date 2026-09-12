import assert from 'node:assert/strict'
import http from 'node:http'
import { after, before, describe, test } from 'node:test'
import MountebankWrapper from '../../support/MountebankWrapper'
import ServiceMocker from '../../support/ServiceMocker'
import {
  type WikidataMockEndpointContext,
  ISOLATED_WIKIDATA_MOCK_ENV_KEY,
  WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
  SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT,
  wikidataImposterRequestUrl,
  resolveWikidataMockEndpointContext,
} from './wikidataMockEndpointContext'

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

describe('Wikidata mock endpoint context', () => {
  const servingPort = 18002
  const recorded: RecordedHttpCall[] = []
  let managementUrl = ''
  let server: http.Server
  let endpoint: WikidataMockEndpointContext

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
      'wikidata',
      endpoint.servingPort,
      endpoint.managementUrl
    )
    assert.equal(mocker.serviceUrl, `http://localhost:${servingPort}`)
  })

  test('shared primary/CI defaults remain selectable at the Wikidata boundary', () => {
    assert.deepEqual(SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT, {
      managementUrl: 'http://localhost:2525',
      servingPort: 5002,
    })
    const mocker = new ServiceMocker(
      'wikidata',
      SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT.servingPort,
      SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT.managementUrl
    )
    assert.equal(mocker.serviceUrl, 'http://localhost:5002')
    assert.equal(
      wikidataImposterRequestUrl(SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT),
      'http://localhost:2525/imposters/5002'
    )
  })

  test('resolveWikidataMockEndpointContext keeps shared defaults without injection', () => {
    assert.deepEqual(
      resolveWikidataMockEndpointContext(() => undefined),
      SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT
    )
  })

  test('isolated flag refuses incomplete context instead of shared fallback', () => {
    assert.throws(
      () =>
        resolveWikidataMockEndpointContext((key) =>
          key === ISOLATED_WIKIDATA_MOCK_ENV_KEY ? true : undefined
        ),
      /refusing shared 2525\/5002 fallback/
    )
  })

  test('resolveWikidataMockEndpointContext requires a complete injected context when isolated', () => {
    const endpoint = {
      managementUrl: 'http://127.0.0.1:18026',
      servingPort: 18002,
    }
    assert.deepEqual(
      resolveWikidataMockEndpointContext((key) => {
        if (key === ISOLATED_WIKIDATA_MOCK_ENV_KEY) return true
        if (key === WIKIDATA_MOCK_ENDPOINT_ENV_KEY) return endpoint
        return
      }),
      endpoint
    )
  })
})
