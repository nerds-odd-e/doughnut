import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const MISSING_OAUTH_SNIPPET =
  'Missing OAuth credentials. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET'

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
    const { InteractiveCliApp } = await import('../src/InteractiveCliApp.js')

    const { stdin, frames } = render(<InteractiveCliApp />)

    stdin.write('/add gmail\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/add gmail') &&
        c.includes(MISSING_OAUTH_SNIPPET) &&
        c.includes('\x1b[100m')
    )
    const combined = frames.join('\n')
    expect(combined).toContain(MISSING_OAUTH_SNIPPET)
  })
})
