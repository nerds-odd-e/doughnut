import * as fs from 'node:fs'
import * as http from 'node:http'
import * as os from 'node:os'
import * as path from 'node:path'
import { expect, vi } from 'vitest'
import {
  renderInkWhenCommandLineReady,
  waitForFrames,
} from './inkTestHelpers.js'

export const MISSING_OAUTH_SNIPPET =
  'Missing OAuth credentials. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET'

export function parseOAuthLocalhostPort(output: string): number | undefined {
  const m = output.match(/redirect_uri=http%3A%2F%2Flocalhost%3A(\d+)/)
  return m ? Number(m[1]) : undefined
}

export function stubGmailApiFetchForAddAccount(email: string) {
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

export function writeGmailConfig(
  configDir: string,
  data: Record<string, unknown>
): void {
  fs.writeFileSync(
    path.join(configDir, 'gmail.json'),
    JSON.stringify(data, null, 2),
    'utf-8'
  )
}

export function writeLastEmailFixtureGmailConfig(configDir: string): void {
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

export function expectSuccessLineOnceOnScreen(
  successLine: string,
  lastStrippedFrame: () => string
): void {
  const final = lastStrippedFrame()
  expect(final.split(successLine).length - 1).toBe(1)
}

export function createTestConfigDir(): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-test-'))
  vi.stubEnv('DONUT_CONFIG_DIR', dir)
  return dir
}

export function cleanupTestEnv(configDir: string) {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  fs.rmSync(configDir, { recursive: true, force: true })
}

export function captureOAuthLog() {
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

export async function renderApp() {
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

export async function completeOAuthFromLog(getOAuthLog: () => string) {
  await triggerOAuthRedirectCallback(parseOAuthLocalhostPort(getOAuthLog())!)
}

export async function submitAndCompleteOAuth(
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

export async function submitCommandAndWaitForError(
  stdin: { write: (s: string) => void },
  waitForLastFrameToInclude: (
    pattern: string | RegExp,
    maxTicks?: number
  ) => Promise<void>,
  command: string,
  errorSnippet: string
) {
  stdin.write(`${command}\r`)
  await waitForLastFrameToInclude(errorSnippet)
}
