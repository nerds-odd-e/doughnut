import assert from 'node:assert/strict'
import http from 'node:http'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { test } from 'node:test'

for (const publicOrigin of [undefined, 'https://127.0.0.1:15443']) {
  test(`local LB forwards the ${publicOrigin ? 'configured HTTPS' : 'ordinary HTTP'} origin`, async (t) => {
    const upstream = http.createServer((req, res) => {
      res.setHeader('content-type', 'application/json')
      res.end(JSON.stringify(req.headers))
    })
    upstream.listen(0, '127.0.0.1')
    await once(upstream, 'listening')
    const reservation = http.createServer()
    reservation.listen(0, '127.0.0.1')
    await once(reservation, 'listening')
    const port = reservation.address().port
    await new Promise((resolve) => reservation.close(resolve))
    const env = {
      ...process.env,
      LOCAL_LB_LISTEN_PORT: String(port),
      LOCAL_LB_BACKEND: `http://127.0.0.1:${upstream.address().port}`,
      LOCAL_LB_VITE_UPSTREAM: `http://127.0.0.1:${upstream.address().port}`,
    }
    delete env.LOCAL_LB_PUBLIC_ORIGIN
    if (publicOrigin) env.LOCAL_LB_PUBLIC_ORIGIN = publicOrigin
    const child = spawn(process.execPath, ['scripts/local-lb.mjs'], {
      env,
      stdio: ['ignore', 'pipe', 'inherit'],
    })
    t.after(async () => {
      child.kill('SIGTERM')
      await once(child, 'exit')
      await new Promise((resolve) => upstream.close(resolve))
    })
    await once(child.stdout, 'data')
    const response = await fetch(`http://127.0.0.1:${port}/api/echo`, {
      headers: {
        forwarded: 'host=untrusted.example;proto=http',
        'x-forwarded-host': 'untrusted.example',
        'x-forwarded-proto': 'http',
        ...(publicOrigin
          ? { 'x-forwarded-port': '666', 'x-forwarded-prefix': '/untrusted' }
          : {}),
      },
    })
    const headers = await response.json()
    assert.equal(
      headers['x-forwarded-host'],
      publicOrigin ? '127.0.0.1:15443' : `127.0.0.1:${port}`
    )
    assert.equal(headers['x-forwarded-proto'], publicOrigin ? 'https' : 'http')
    if (publicOrigin) assert.equal(headers['x-forwarded-port'], '15443')
  })
}
