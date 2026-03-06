import * as crypto from 'node:crypto'
import * as fs from 'node:fs'
import * as http from 'node:http'
import * as os from 'node:os'
import * as path from 'node:path'
import { spawn } from 'node:child_process'
import {
  GOOGLE_CLIENT_ID as BUILTIN_CLIENT_ID,
  GOOGLE_CLIENT_SECRET as BUILTIN_CLIENT_SECRET,
} from './credentials.js'

const GMAIL_SCOPE = 'https://www.googleapis.com/auth/gmail.readonly'
const DEFAULT_OAUTH_TOKEN_URL = 'https://oauth2.googleapis.com/token'
const DEFAULT_GMAIL_BASE = 'https://gmail.googleapis.com'

export interface GmailAccount {
  email: string
  accessToken: string
  refreshToken: string
  expiresAt: number
}

export interface GmailConfig {
  clientId?: string
  clientSecret?: string
  accounts: GmailAccount[]
}

function getConfigDir(): string {
  const dir = process.env.DOUGHNUT_CONFIG_DIR
  if (dir) return dir
  return path.join(os.homedir(), '.config', 'doughnut')
}

function getConfigPath(customPath?: string): string {
  if (customPath) return customPath
  return path.join(getConfigDir(), 'gmail.json')
}

function getGoogleBaseUrl(): string | null {
  return process.env.GOOGLE_BASE_URL || null
}

function getOAuthTokenUrl(): string {
  const base = getGoogleBaseUrl()
  if (base) return `${base.replace(/\/$/, '')}/token`
  return DEFAULT_OAUTH_TOKEN_URL
}

function getGmailApiUrl(pathSuffix: string): string {
  const base = getGoogleBaseUrl()
  if (base) return `${base.replace(/\/$/, '')}/gmail/v1${pathSuffix}`
  return `${DEFAULT_GMAIL_BASE}/gmail/v1${pathSuffix}`
}

export function loadConfig(configPath?: string): GmailConfig {
  const p = getConfigPath(configPath)
  try {
    const data = fs.readFileSync(p, 'utf-8')
    const parsed = JSON.parse(data) as GmailConfig
    return { accounts: [], ...parsed }
  } catch {
    return { accounts: [] }
  }
}

export function saveConfig(config: GmailConfig, configPath?: string): void {
  const p = getConfigPath(configPath)
  const dir = path.dirname(p)
  fs.mkdirSync(dir, { recursive: true })
  fs.writeFileSync(p, JSON.stringify(config, null, 2), 'utf-8')
}

function openBrowser(url: string): void {
  if (process.platform === 'win32') {
    spawn('cmd', ['/c', 'start', '', url], { stdio: 'ignore', detached: true })
  } else {
    const cmd = process.platform === 'darwin' ? 'open' : 'xdg-open'
    spawn(cmd, [url], { stdio: 'ignore', detached: true })
  }
}

async function waitForCallback(server: http.Server): Promise<string> {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      server.close()
      reject(new Error('Authentication timed out. Please try again.'))
    }, 300_000)

    server.on(
      'request',
      (req: http.IncomingMessage, res: http.ServerResponse) => {
        const url = new URL(req.url || '/', `http://localhost`)
        const code = url.searchParams.get('code')
        const error = url.searchParams.get('error')
        res.writeHead(200, { 'Content-Type': 'text/html' })
        res.end(
          '<html><body><h1>Authentication complete. You can close this window.</h1></body></html>'
        )
        clearTimeout(timeout)
        server.close()
        if (error) reject(new Error(`OAuth error: ${error}`))
        else if (code) resolve(code)
        else reject(new Error('No authorization code received'))
      }
    )
  })
}

async function exchangeCodeForTokens(
  code: string,
  clientId: string,
  clientSecret: string,
  redirectUri: string
): Promise<{ accessToken: string; refreshToken: string; expiresIn: number }> {
  const body = new URLSearchParams({
    code,
    client_id: clientId,
    client_secret: clientSecret,
    redirect_uri: redirectUri,
    grant_type: 'authorization_code',
  })
  const res = await fetch(getOAuthTokenUrl(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  })
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as {
      error?: string
      error_description?: string
    }
    throw new Error(
      err.error_description ||
        err.error ||
        `Token exchange failed: ${res.status}`
    )
  }
  const data = (await res.json()) as {
    access_token: string
    refresh_token: string
    expires_in: number
  }
  return {
    accessToken: data.access_token,
    refreshToken: data.refresh_token,
    expiresIn: data.expires_in,
  }
}

async function getProfileEmail(accessToken: string): Promise<string> {
  const res = await fetch(getGmailApiUrl('/users/me/profile'), {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`Failed to get profile: ${res.status} ${text}`)
  }
  const data = (await res.json()) as { emailAddress?: string }
  return data.emailAddress || 'unknown@gmail.com'
}

export async function addGmailAccount(configPath?: string): Promise<void> {
  const config = loadConfig(configPath)

  const clientId = config.clientId || BUILTIN_CLIENT_ID || undefined
  const clientSecret = config.clientSecret || BUILTIN_CLIENT_SECRET || undefined

  if (!(clientId && clientSecret)) {
    throw new Error(
      'Missing OAuth credentials. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables, or run with the bundled CLI.'
    )
  }

  const state = crypto.randomBytes(16).toString('hex')
  const server = http.createServer()
  await new Promise<void>((resolve) => server.listen(0, () => resolve()))
  const address = server.address()
  if (!address || typeof address === 'string') {
    server.close()
    throw new Error('Failed to bind callback server')
  }
  const port = address.port
  const redirectUri = `http://localhost:${port}`

  const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth')
  authUrl.searchParams.set('client_id', clientId)
  authUrl.searchParams.set('redirect_uri', redirectUri)
  authUrl.searchParams.set('response_type', 'code')
  authUrl.searchParams.set('scope', GMAIL_SCOPE)
  authUrl.searchParams.set('access_type', 'offline')
  authUrl.searchParams.set('prompt', 'consent')
  authUrl.searchParams.set('state', state)

  console.log(
    `Opening browser for authentication. If it does not open, visit:\n${authUrl.toString()}`
  )
  if (!process.env.DOUGHNUT_NO_BROWSER) {
    openBrowser(authUrl.toString())
  }

  const code = await waitForCallback(server)
  const tokens = await exchangeCodeForTokens(
    code,
    clientId!,
    clientSecret!,
    redirectUri
  )
  const email = await getProfileEmail(tokens.accessToken)

  const expiresAt = Date.now() + tokens.expiresIn * 1000
  const account: GmailAccount = {
    email,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    expiresAt,
  }

  config.accounts.push(account)
  saveConfig(config, configPath)
  console.log(`Added account ${email}`)
}

async function refreshAccessToken(
  account: GmailAccount,
  config: GmailConfig
): Promise<string> {
  const clientId = config.clientId || BUILTIN_CLIENT_ID || undefined
  const clientSecret = config.clientSecret || BUILTIN_CLIENT_SECRET || undefined
  if (!(clientId && clientSecret)) {
    throw new Error('Missing client credentials. Run /add gmail again.')
  }

  const body = new URLSearchParams({
    refresh_token: account.refreshToken,
    client_id: clientId,
    client_secret: clientSecret,
    grant_type: 'refresh_token',
  })
  const res = await fetch(getOAuthTokenUrl(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  })
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { error?: string }
    if (err.error === 'invalid_grant') {
      throw new Error('Session expired. Run /add gmail to re-authenticate.')
    }
    throw new Error(`Token refresh failed: ${res.status}`)
  }
  const data = (await res.json()) as {
    access_token: string
    expires_in: number
  }
  account.accessToken = data.access_token
  account.expiresAt = Date.now() + data.expires_in * 1000
  return account.accessToken
}

function getSubjectFromMessage(message: {
  payload?: { headers?: { name: string; value: string }[] }
}): string {
  const headers = message.payload?.headers || []
  const subj = headers.find((h) => h.name.toLowerCase() === 'subject')
  return subj?.value ?? '(no subject)'
}

export async function getLastEmailSubject(
  configPath?: string
): Promise<string> {
  const config = loadConfig(configPath)
  const account = config.accounts[0]
  if (!account) {
    throw new Error('No Gmail account configured. Run /add gmail first.')
  }

  let accessToken = account.accessToken
  const bufferMs = 60_000
  if (Date.now() >= account.expiresAt - bufferMs) {
    accessToken = await refreshAccessToken(account, config)
    saveConfig(config, configPath)
  }

  const listRes = await fetch(
    getGmailApiUrl('/users/me/messages?maxResults=1'),
    {
      headers: { Authorization: `Bearer ${accessToken}` },
    }
  )
  if (!listRes.ok) {
    const text = await listRes.text()
    throw new Error(`Failed to list messages: ${listRes.status} ${text}`)
  }
  const listData = (await listRes.json()) as { messages?: { id: string }[] }
  const messages = listData.messages || []
  if (messages.length === 0) {
    return '(no messages)'
  }

  const msgId = messages[0].id
  const msgRes = await fetch(
    getGmailApiUrl(
      `/users/me/messages/${msgId}?format=metadata&metadataHeaders=Subject`
    ),
    { headers: { Authorization: `Bearer ${accessToken}` } }
  )
  if (!msgRes.ok) {
    const text = await msgRes.text()
    throw new Error(`Failed to get message: ${msgRes.status} ${text}`)
  }
  const msgData = (await msgRes.json()) as {
    payload?: { headers?: { name: string; value: string }[] }
  }
  return getSubjectFromMessage(msgData)
}

export const gmailCommandDocs = [
  {
    name: '/add gmail',
    usage: '/add gmail',
    description: 'Add Gmail account via OAuth',
    category: 'interactive' as const,
  },
  {
    name: '/last email',
    usage: '/last email',
    description: 'Show subject of last email',
    category: 'interactive' as const,
  },
]
