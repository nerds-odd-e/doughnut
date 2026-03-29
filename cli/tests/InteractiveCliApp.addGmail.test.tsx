import * as fs from 'node:fs'
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

describe('InteractiveCliApp /add gmail (real addGmailAccount, no mocks)', () => {
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
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/add gmail') &&
        c.includes(MISSING_OAUTH_SNIPPET) &&
        c.includes('\x1b[100m')
    )
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> ')
    )
    const combined = frames.join('\n')
    expect(combined).toContain(MISSING_OAUTH_SNIPPET)
  })
})

describe('InteractiveCliApp /add gmail staging (mocked addGmailAccount)', () => {
  afterEach(() => {
    vi.doUnmock('../src/commands/gmail.js')
    vi.unstubAllEnvs()
    vi.resetModules()
  })

  test('while add gmail is in flight: shows stage status and hides main command line', async () => {
    const addGmailAccount = vi.fn(
      () =>
        new Promise<string>((resolve) => {
          queueMicrotask(() => {
            queueMicrotask(() => resolve('staged@test.com'))
          })
        })
    )
    vi.doMock('../src/commands/gmail.js', () => ({
      addGmailAccount,
      formatAddedGmailAccountMessage: (email: string) =>
        `Added account ${email}`,
    }))

    const { stdin, lastFrame } = await renderApp()

    stdin.write('/add gmail\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) =>
        f.includes('Connecting Gmail') &&
        f.includes('/add gmail') &&
        !f.includes('> ')
    )
  })

  test('after add gmail completes: one Added account line and main prompt returns', async () => {
    const addGmailAccount = vi.fn(
      () =>
        new Promise<string>((resolve) => {
          queueMicrotask(() => {
            queueMicrotask(() => resolve('staged@test.com'))
          })
        })
    )
    vi.doMock('../src/commands/gmail.js', () => ({
      addGmailAccount,
      formatAddedGmailAccountMessage: (email: string) =>
        `Added account ${email}`,
    }))

    const { stdin, frames, lastFrame } = await renderApp()

    stdin.write('/add gmail\r')
    const successLine = 'Added account staged@test.com'
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes(successLine) && f.includes('> ')
    )
    const final = stripAnsi(lastFrame() ?? '')
    expect(final.split(successLine).length - 1).toBe(1)
    const combined = stripAnsi(frames.join('\n'))
    expect(combined.split(successLine).length - 1).toBe(1)
  })
})
