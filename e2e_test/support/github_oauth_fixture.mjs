import http from 'node:http'
import { randomUUID } from 'node:crypto'

/** Owned external provider boundary; Spring still performs the complete OAuth login. */
export async function startGithubOAuthFixture({ userId = 'old_learner' } = {}) {
  const codes = new Map()
  const tokens = new Map()
  const visits = []
  const clientId = 'fixture-github-client'
  const clientSecret = 'fixture-github-secret'
  const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://fixture')
    visits.push(url.pathname)
    if (url.pathname === '/authorize') {
      const code = randomUUID()
      codes.set(code, userId)
      const callback = new URL(url.searchParams.get('redirect_uri'))
      callback.searchParams.set('code', code)
      callback.searchParams.set('state', url.searchParams.get('state'))
      res.writeHead(302, { location: callback.href }).end()
      return
    }
    if (url.pathname === '/token') {
      let body = ''
      for await (const chunk of req) body += chunk
      const params = new URLSearchParams(body)
      const basic = Buffer.from(`${clientId}:${clientSecret}`).toString(
        'base64'
      )
      const authenticated =
        req.headers.authorization === `Basic ${basic}` ||
        (params.get('client_id') === clientId &&
          params.get('client_secret') === clientSecret)
      const subject = codes.get(params.get('code'))
      if (!(authenticated && subject)) {
        res
          .writeHead(400, { 'content-type': 'application/json' })
          .end(JSON.stringify({ error: 'invalid_grant' }))
        return
      }
      codes.delete(params.get('code'))
      const token = randomUUID()
      tokens.set(token, subject)
      res.setHeader('content-type', 'application/json')
      res.end(
        JSON.stringify({
          access_token: token,
          token_type: 'bearer',
          scope: 'read:user',
        })
      )
      return
    }
    if (url.pathname === '/user') {
      const subject = tokens.get(
        req.headers.authorization?.replace(/^Bearer /i, '')
      )
      if (!subject) {
        res.writeHead(401).end()
        return
      }
      res.setHeader('content-type', 'application/json')
      res.end(
        JSON.stringify({
          id: subject,
          login: subject,
          name: 'Fixture GitHub user',
        })
      )
      return
    }
    res.writeHead(404).end()
  })
  await new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(0, '127.0.0.1', resolve)
  })
  const origin = `http://127.0.0.1:${server.address().port}`
  return {
    origin,
    visits,
    springClient: {
      registration: { github: { clientId, clientSecret } },
      provider: {
        github: {
          authorizationUri: `${origin}/authorize`,
          tokenUri: `${origin}/token`,
          userInfoUri: `${origin}/user`,
        },
      },
    },
    close: () =>
      new Promise((resolve, reject) =>
        server.close((err) => (err ? reject(err) : resolve()))
      ),
  }
}
