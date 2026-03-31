import * as fs from 'node:fs'
import * as http from 'node:http'
import * as os from 'node:os'
import * as path from 'node:path'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  pressEscapeAndWaitForCancelledLine,
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'

const MISSING_OAUTH_SNIPPET =
  'Missing OAuth credentials. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET'

function parseOAuthLocalhostPort(output: string): number | undefined {
  const m = output.match(/redirect_uri=http%3A%2F%2Flocalhost%3A(\d+)/)
  return m ? Number(m[1]) : undefined
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

function expectSuccessLineOnceOnScreen(
  successLine: string,
  lastFrame: () => string | undefined
): void {
  const final = stripAnsi(lastFrame() ?? '')
  expect(final.split(successLine).length - 1).toBe(1)
}

function createTestConfigDir(): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-test-'))
  vi.stubEnv('DOUGHNUT_CONFIG_DIR', dir)
  return dir
}

function cleanupTestEnv(configDir: string) {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  vi.resetModules()
  fs.rmSync(configDir, { recursive: true, force: true })
}

function captureOAuthLog() {
  let tee = ''
  const origLog = console.log.bind(console)
  const logSpy = vi
    .spyOn(console, 'log')
    .mockImplementation((...args: unknown[]) => {
      tee += `${args.map(String).join(' ')}\n`
      origLog(...args)
    })
  return {
    get: () => tee,
    restore: () => logSpy.mockRestore(),
  }
}

async function renderApp() {
  const { InteractiveCliApp } = await import('../src/InteractiveCliApp.js')
  return renderInkWhenCommandLineReady(<InteractiveCliApp />)
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

async function completeOAuthFromLog(getOAuthLog: () => string) {
  await triggerOAuthRedirectCallback(parseOAuthLocalhostPort(getOAuthLog())!)
}

async function submitAndCompleteOAuth(
  stdin: { write: (s: string) => void },
  getOAuthLog: () => string
) {
  stdin.write('/add gmail\r')
  await waitForFrames(
    getOAuthLog,
    (s) => parseOAuthLocalhostPort(s) !== undefined
  )
  await completeOAuthFromLog(getOAuthLog)
}

async function submitCommandAndExpectError(
  stdin: { write: (s: string) => void },
  frames: string[],
  lastFrame: () => string | undefined,
  command: string,
  errorSnippet: string
) {
  stdin.write(`${command}\r`)
  await waitForFrames(
    () => frames.join('\n'),
    (c) =>
      c.includes(command) && c.includes(errorSnippet) && c.includes('\x1b[100m')
  )
  await waitForLastFrame(lastFrame, (f) => f.includes('→ '))
}

describe('InteractiveCliApp /add gmail (missing credentials)', () => {
  let configDir: string

  beforeEach(() => {
    configDir = createTestConfigDir()
    vi.stubEnv('GOOGLE_CLIENT_ID', '')
    vi.stubEnv('GOOGLE_CLIENT_SECRET', '')
  })

  afterEach(() => cleanupTestEnv(configDir))

  test('shows missing-credentials error in transcript after /add gmail', async () => {
    const { stdin, frames, lastFrame } = await renderApp()
    await submitCommandAndExpectError(
      stdin,
      frames,
      lastFrame,
      '/add gmail',
      MISSING_OAUTH_SNIPPET
    )
    expect(frames.join('\n')).toContain(MISSING_OAUTH_SNIPPET)
  })
})

describe('InteractiveCliApp /add gmail (mocked HTTP APIs)', () => {
  let configDir: string
  let oauthLog: ReturnType<typeof captureOAuthLog>

  beforeEach(() => {
    configDir = createTestConfigDir()
    vi.stubEnv('GOOGLE_CLIENT_ID', 'unit-test-client-id')
    vi.stubEnv('GOOGLE_CLIENT_SECRET', 'unit-test-client-secret')
    vi.stubEnv('DOUGHNUT_NO_BROWSER', '1')
    oauthLog = captureOAuthLog()
    stubGmailApiFetchForAddAccount('staged@test.com')
  })

  afterEach(() => {
    oauthLog.restore()
    cleanupTestEnv(configDir)
  })

  test('while add gmail is in flight: shows stage status and hides main command line', async () => {
    const { stdin, lastFrame } = await renderApp()

    stdin.write('/add gmail\r')
    await waitForFrames(
      oauthLog.get,
      (s) => parseOAuthLocalhostPort(s) !== undefined
    )
    await waitForLastFrame(
      lastFrame,
      (f) =>
        f.includes('Connecting Gmail') &&
        f.includes('/add gmail') &&
        !f.includes('→ ')
    )
    expect(parseOAuthLocalhostPort(oauthLog.get())).toBeDefined()
    await completeOAuthFromLog(oauthLog.get)
    await waitForLastFrame(
      lastFrame,
      (f) => f.includes('Added account staged@test.com') && f.includes('→ ')
    )
  })

  test('after add gmail completes: one Added account line and main prompt returns', async () => {
    const { stdin, lastFrame } = await renderApp()
    const successLine = 'Added account staged@test.com'

    await submitAndCompleteOAuth(stdin, oauthLog.get)
    await waitForLastFrame(
      lastFrame,
      (f) => f.includes(successLine) && f.includes('→ ')
    )
    expectSuccessLineOnceOnScreen(successLine, lastFrame)
  })

  test('Escape during OAuth wait settles Cancelled and returns prompt', async () => {
    const { stdin, frames, lastFrame } = await renderApp()

    stdin.write('/add gmail\r')
    await waitForFrames(
      oauthLog.get,
      (s) => parseOAuthLocalhostPort(s) !== undefined
    )
    await waitForLastFrame(
      lastFrame,
      (f) =>
        f.includes('Connecting Gmail') &&
        f.includes('/add gmail') &&
        !f.includes('→ ')
    )

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'), {
      normalize: stripAnsi,
    })
    await waitForLastFrame(lastFrame, (f) => f.includes('→ '))
  })
})

describe('InteractiveCliApp /last email (mocked HTTP APIs)', () => {
  let configDir: string

  beforeEach(() => {
    configDir = createTestConfigDir()
  })

  afterEach(() => cleanupTestEnv(configDir))

  test('while last email is in flight: shows stage status and hides main command line', async () => {
    writeLastEmailFixtureGmailConfig(configDir)
    vi.stubGlobal(
      'fetch',
      vi.fn(
        () =>
          new Promise<Response>(() => {
            /* never resolves — in-flight UI */
          })
      )
    )

    const { stdin, lastFrame, unmount } = await renderApp()

    stdin.write('/last email\r')
    await waitForLastFrame(
      lastFrame,
      (f) =>
        f.includes('Loading last email') &&
        f.includes('/last email') &&
        !f.includes('→ ')
    )
    unmount()
  })

  test('Escape during last-email fetch settles Cancelled and returns prompt', async () => {
    writeLastEmailFixtureGmailConfig(configDir)
    vi.stubGlobal(
      'fetch',
      vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
        const { signal } = init ?? {}
        return new Promise<Response>((_resolve, reject) => {
          if (signal?.aborted) {
            reject(new DOMException('The operation was aborted', 'AbortError'))
            return
          }
          signal?.addEventListener(
            'abort',
            () => {
              reject(
                new DOMException('The operation was aborted', 'AbortError')
              )
            },
            { once: true }
          )
        })
      })
    )

    const { stdin, frames, lastFrame } = await renderApp()

    stdin.write('/last email\r')
    await waitForLastFrame(
      lastFrame,
      (f) =>
        f.includes('Loading last email') &&
        f.includes('/last email') &&
        !f.includes('→ ')
    )

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'), {
      normalize: stripAnsi,
    })
    await waitForLastFrame(lastFrame, (f) => f.includes('→ '))
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

    const { stdin, lastFrame } = await renderApp()
    const successLine = 'Welcome to Doughnut'

    stdin.write('/last email\r')
    await waitForLastFrame(
      lastFrame,
      (f) => f.includes(successLine) && f.includes('→ ')
    )
    expectSuccessLineOnceOnScreen(successLine, lastFrame)
  })

  test('shows no-account error in transcript after /last email', async () => {
    writeGmailConfig(configDir, { accounts: [] })

    const { stdin, frames, lastFrame } = await renderApp()
    await submitCommandAndExpectError(
      stdin,
      frames,
      lastFrame,
      '/last email',
      'No Gmail account configured.'
    )
  })
})
