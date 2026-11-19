import http from 'node:http'

function postJson(url, body) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(body)
    const target = new URL(url)
    const req = http.request(
      {
        hostname: target.hostname,
        port: target.port,
        path: target.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload),
        },
      },
      (res) => {
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () => {
          resolve({
            statusCode: res.statusCode ?? 0,
            body: Buffer.concat(chunks).toString('utf8'),
          })
        })
      }
    )
    req.on('error', reject)
    req.write(payload)
    req.end()
  })
}

/**
 * Create a recording imposter on `servingPort` through `managementUrl`.
 * The `serviceLabel` names the service in failure messages. When `stubs` is
 * supplied the imposter is created with those stubs; by default the imposter
 * is empty (records requests, serves no canned response).
 */
export async function createEmptyRecordingImposter(
  managementUrl,
  servingPort,
  { serviceLabel = 'mock', stubs = [] } = {}
) {
  const response = await postJson(`${managementUrl}/imposters`, {
    protocol: 'http',
    port: servingPort,
    recordRequests: true,
    stubs,
  })
  if (response.statusCode !== 201) {
    throw new Error(
      `Private ${serviceLabel} mock failed to create recording imposter on port ${servingPort}: ` +
        `status ${response.statusCode} ${response.body}`
    )
  }
}
