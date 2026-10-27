import http from 'node:http'

export function targetPort(target) {
  if (target.port) return target.port
  return target.protocol === 'https:' ? '443' : '80'
}

export function proxyHttp(
  clientReq,
  clientRes,
  target,
  logLabel,
  publicOrigin,
  fallbackHost
) {
  const headers = { ...clientReq.headers }
  const incomingHost = clientReq.headers.host ?? fallbackHost
  const tp = targetPort(target)
  headers.host = `${target.hostname}:${tp}`
  headers['x-forwarded-host'] = publicOrigin?.host ?? incomingHost
  headers['x-forwarded-proto'] = publicOrigin?.protocol.slice(0, -1) ?? 'http'
  delete headers.forwarded
  if (publicOrigin) {
    headers['x-forwarded-port'] = targetPort(publicOrigin)
    delete headers['x-forwarded-prefix']
  }
  delete headers.connection

  const opts = {
    protocol: target.protocol,
    hostname: target.hostname,
    port: tp,
    path: clientReq.url,
    method: clientReq.method,
    headers,
  }

  const pReq = http.request(opts, (pRes) => {
    clientRes.writeHead(pRes.statusCode ?? 502, pRes.headers)
    pRes.pipe(clientRes)
  })
  pReq.on('error', (err) => {
    console.error(`[local-lb] ${logLabel} error:`, err.message)
    if (!clientRes.headersSent) {
      clientRes.statusCode = 502
      clientRes.end('Bad Gateway')
    }
  })
  clientReq.pipe(pReq)
}

function rawUpgradeHead(res) {
  const code = res.statusCode ?? 101
  const msg = res.statusMessage || 'Switching Protocols'
  let raw = `HTTP/1.1 ${code} ${msg}\r\n`
  for (const [key, value] of Object.entries(res.headers)) {
    if (value === undefined) continue
    if (Array.isArray(value)) {
      for (const v of value) raw += `${key}: ${v}\r\n`
    } else {
      raw += `${key}: ${value}\r\n`
    }
  }
  raw += '\r\n'
  return raw
}

export function proxyUpgrade(clientReq, clientSocket, head, target, logLabel) {
  const tp = targetPort(target)
  const headers = { ...clientReq.headers }
  headers.host = `${target.hostname}:${tp}`

  const opts = {
    hostname: target.hostname,
    port: tp,
    path: clientReq.url,
    method: clientReq.method,
    headers,
  }

  const pReq = http.request(opts)
  pReq.on('upgrade', (pRes, pSocket, pHead) => {
    clientSocket.write(rawUpgradeHead(pRes))
    const endSocket = () => {
      clientSocket.destroy()
      pSocket.destroy()
    }
    pSocket.on('error', endSocket)
    clientSocket.on('error', endSocket)
    if (pHead?.length) pSocket.unshift(pHead)
    pSocket.pipe(clientSocket)
    clientSocket.pipe(pSocket)
  })
  pReq.on('error', (err) => {
    console.error(`[local-lb] ${logLabel} upgrade error:`, err.message)
    clientSocket.destroy()
  })
  pReq.end(head)
}
