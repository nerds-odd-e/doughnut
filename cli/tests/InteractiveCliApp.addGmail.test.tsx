import * as fs from 'node:fs'
import * as http from 'node:http'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const MISSING_OAUTH_SNIPPET =
  'Missing OAuth credentials. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET'

function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

async function waitForFrames(
  getCombined: () => string,
  predicate: (combined: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  for (let i = 0; i < maxTicks; i++) {
    if (predicate(getCombined())) {
      return
    }
    await new Promise<void>((resolve) => {
      setImmediate(resolve)
    })
  }
  const combined = getCombined()
  throw new Error(
    `Output condition not met within ${maxTicks} event-loop turns. Last frames:\n${combined}`
  )
}

function parseOAuthLocalhostPort(output: string): number | undefined {
  const m = output.match(/redirect_uri=http%3A%2F%2Flocalhost%3A(\d+)/)
  return m ? Number(m[1]) : undefined
}

function triggerOAuthRedirectCallback(port: number): Promise<void> {
  return new Promise((resolve, reject) => {
    http
      .get(`http://127.0.0.1:${port}/?code=unit-test-auth-code`, (res) => {
        res.resume()
        resolve()
      })
      .on('error', reject)
  })
}

async function waitForOAuthRedirectInLog(
  getOAuthLog: () => string
): Promise<void> {
  await waitForFrames(
    getOAuthLog,
    (s) => parseOAuthLocalhostPort(s) !== undefined
  )
}

function callbackOAuthFromLog(getOAuthLog: () => string): Promise<void> {
  return triggerOAuthRedirectCallback(parseOAuthLocalhostPort(getOAuthLog())!)
}

function stubGmailApiFetchForAddAccount(email: string) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url =
        typeof input === 'string'
          ? input
          : input instanceof URL
            ? input.href
            : input.url
      if (
        url.includes('oauth2.googleapis.com/token') &&
        init?.method === 'POST'
      ) {
        return {
          ok: true,
          json: () =>
            Promise.resolve({
              access_token: 'unit-at',
              refresh_token: 'unit-rt',
              expires_in: 3600,
            }),
        } as Response
      }
      if (url.includes('/gmail/v1/users/me/profile')) {
        return {
          ok: true,
          json: () => Promise.resolve({ emailAddress: email }),
        } as Response
      }
      throw new Error(
        `unexpected fetch in add-gmail flow: ${url} ${init?.method}`
      )
    })
  )
}

function writeGmailConfig(
  configDir: string,
  data: Record<string, unknown>
): void {
  fs.writeFileSync(
    path.join(configDir, 'gmail.json'),
    JSON.stringify(data, null, 2),
    'utf-8'
  )
}

function writeLastEmailFixtureGmailConfig(configDir: string): void {
  writeGmailConfig(configDir, {
    clientId: 'c',
    clientSecret: 's',
    accounts: [
      {
        email: 'u@gmail.com',
        accessToken: 'at',
        refreshToken: 'rt',
        expiresAt: Date.now() + 3_600_000,
      },
    ],
  })
}

async function waitForTranscriptErrorThenPrompt(
  getTranscript: () => string,
  lastFrame: () => string | undefined,
  transcriptPredicate: (c: string) => boolean
): Promise<void> {
  await waitForFrames(getTranscript, transcriptPredicate)
  await waitForFrames(
    () => stripAnsi(lastFrame() ?? ''),
    (f) => f.includes('> ')
  )
}

function expectSuccessLineOnceInOutput(
  successLine: string,
  frames: string[],
  lastFrame: () => string | undefined
): void {
  const final = stripAnsi(lastFrame() ?? '')
  expect(final.split(successLine).length - 1).toBe(1)
  expect(stripAnsi(frames.join('\n')).split(successLine).length - 1).toBe(1)
}

async function renderApp() {
  const { InteractiveCliApp } = await import('../src/InteractiveCliApp.js')
  const result = render(<InteractiveCliApp />)
  result.stdin.write('|')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('> |')
  )
  result.stdin.write('\x7f')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('> ') && !f.includes('> |')
  )
  return result
}

describe('InteractiveCliApp /add gmail (real gmail module, missing credentials)', () => {
  let configDir: string

  beforeEach(() => {
    configDir = fs.mkdtempSync(
      path.join(os.tmpdir(), 'doughnut-cli-addgmail-isolated-')
    )
    vi.stubEnv('GOOGLE_CLIENT_ID', '')
    vi.stubEnv('GOOGLE_CLIENT_SECRET', '')
    vi.stubEnv('DOUGHNUT_CONFIG_DIR', configDir)
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('shows missing-credentials error in transcript after /add gmail', async () => {
    const { stdin, frames, lastFrame } = await renderApp()

    stdin.write('/add gmail\r')
    await waitForTranscriptErrorThenPrompt(
      () => frames.join('\n'),
      lastFrame,
      (c) =>
        c.includes('/add gmail') &&
        c.includes(MISSING_OAUTH_SNIPPET) &&
        c.includes('\x1b[100m')
    )
    expect(frames.join('\n')).toContain(MISSING_OAUTH_SNIPPET)
  })
})

describe('InteractiveCliApp /add gmail (real gmail module, mocked HTTP APIs)', () => {
  let configDir: string
  let oauthLogTee: string
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    oauthLogTee = ''
    configDir = fs.mkdtempSync(
      path.join(os.tmpdir(), 'doughnut-cli-addgmail-oauth-')
    )
    vi.stubEnv('GOOGLE_CLIENT_ID', 'unit-test-client-id')
    vi.stubEnv('GOOGLE_CLIENT_SECRET', 'unit-test-client-secret')
    vi.stubEnv('DOUGHNUT_CONFIG_DIR', configDir)
    vi.stubEnv('DOUGHNUT_NO_BROWSER', '1')
    const origLog = console.log.bind(console)
    logSpy = vi
      .spyOn(console, 'log')
      .mockImplementation((...args: unknown[]) => {
        oauthLogTee += `${args.map(String).join(' ')}\n`
        origLog(...args)
      })
    stubGmailApiFetchForAddAccount('staged@test.com')
  })

  afterEach(() => {
    logSpy.mockRestore()
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
    vi.resetModules()
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('while add gmail is in flight: shows stage status and hides main command line', async () => {
    const { stdin, lastFrame } = await renderApp()

    stdin.write('/add gmail\r')
    await waitForOAuthRedirectInLog(() => oauthLogTee)
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) =>
        f.includes('Connecting Gmail') &&
        f.includes('/add gmail') &&
        !f.includes('> ')
    )
    const port = parseOAuthLocalhostPort(oauthLogTee)
    expect(port).toBeDefined()
    await callbackOAuthFromLog(() => oauthLogTee)
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('Added account staged@test.com') && f.includes('> ')
    )
  })

  test('after add gmail completes: one Added account line and main prompt returns', async () => {
    const { stdin, frames, lastFrame } = await renderApp()
    const successLine = 'Added account staged@test.com'

    stdin.write('/add gmail\r')
    await waitForOAuthRedirectInLog(() => oauthLogTee)
    await callbackOAuthFromLog(() => oauthLogTee)
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes(successLine) && f.includes('> ')
    )
    expectSuccessLineOnceInOutput(successLine, frames, lastFrame)
  })
})

describe('InteractiveCliApp /last email (real gmail module, mocked HTTP APIs)', () => {
  let configDir: string

  beforeEach(() => {
    configDir = fs.mkdtempSync(
      path.join(os.tmpdir(), 'doughnut-cli-lastemail-')
    )
    vi.stubEnv('DOUGHNUT_CONFIG_DIR', configDir)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
    vi.resetModules()
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('while last email is in flight: shows stage status and hides main command line', async () => {
    writeLastEmailFixtureGmailConfig(configDir)
    vi.stubGlobal(
      'fetch',
      vi.fn(
        () =>
          new Promise<Response>(() => {
            /* never resolves — in-flight fetch */
          })
      )
    )

    const { stdin, lastFrame, unmount } = await renderApp()

    stdin.write('/last email\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) =>
        f.includes('Loading last email') &&
        f.includes('/last email') &&
        !f.includes('> ')
    )
    unmount()
  })

  test('after last email completes: subject line once and main prompt returns', async () => {
    writeLastEmailFixtureGmailConfig(configDir)
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ messages: [{ id: 'msg-1' }] }),
        })
        .mockResolvedValueOnce({
          ok: true,
          json: () =>
            Promise.resolve({
              payload: {
                headers: [{ name: 'Subject', value: 'Welcome to Doughnut' }],
              },
            }),
        })
    )

    const { stdin, frames, lastFrame } = await renderApp()
    const successLine = 'Welcome to Doughnut'

    stdin.write('/last email\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes(successLine) && f.includes('> ')
    )
    expectSuccessLineOnceInOutput(successLine, frames, lastFrame)
  })

  test('shows no-account error in transcript after /last email', async () => {
    writeGmailConfig(configDir, { accounts: [] })

    const { stdin, frames, lastFrame } = await renderApp()

    stdin.write('/last email\r')
    await waitForTranscriptErrorThenPrompt(
      () => frames.join('\n'),
      lastFrame,
      (c) =>
        c.includes('/last email') &&
        c.includes('No Gmail account configured.') &&
        c.includes('\x1b[100m')
    )
  })
})
