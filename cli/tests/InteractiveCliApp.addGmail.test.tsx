import { afterEach, beforeEach, describe, test, vi } from 'vitest'
import {
  pressEscapeAndWaitForCancelledLine,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'
import {
  MISSING_OAUTH_SNIPPET,
  captureOAuthLog,
  cleanupTestEnv,
  completeOAuthFromLog,
  createTestConfigDir,
  expectSuccessLineOnceOnScreen,
  parseOAuthLocalhostPort,
  renderApp,
  stubGmailApiFetchForAddAccount,
  submitAndCompleteOAuth,
  submitCommandAndWaitForError,
} from './InteractiveCliApp.gmail.testHelpers.js'

describe('InteractiveCliApp /add gmail (missing credentials)', () => {
  let configDir: string

  beforeEach(() => {
    configDir = createTestConfigDir()
    vi.stubEnv('GOOGLE_CLIENT_ID', '')
    vi.stubEnv('GOOGLE_CLIENT_SECRET', '')
  })

  afterEach(() => cleanupTestEnv(configDir))

  test('shows missing-credentials error in transcript after /add gmail', async () => {
    const { stdin, waitForLastFrameToInclude } = await renderApp()
    await submitCommandAndWaitForError(
      stdin,
      waitForLastFrameToInclude,
      '/add gmail',
      MISSING_OAUTH_SNIPPET
    )
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
    await completeOAuthFromLog(oauthLog.get)
    await waitForLastFrame(
      lastFrame,
      (f) => f.includes('Added account staged@test.com') && f.includes('→ ')
    )
  })

  test('after add gmail completes: one Added account line and main prompt returns', async () => {
    const { stdin, lastFrame, lastStrippedFrame } = await renderApp()
    const successLine = 'Added account staged@test.com'

    await submitAndCompleteOAuth(stdin, oauthLog.get)
    await waitForLastFrame(
      lastFrame,
      (f) => f.includes(successLine) && f.includes('→ ')
    )
    expectSuccessLineOnceOnScreen(successLine, lastStrippedFrame)
  })

  test('Escape during OAuth wait settles Cancelled and returns prompt', async () => {
    const { stdin, lastStrippedFrame, lastFrame } = await renderApp()

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

    await pressEscapeAndWaitForCancelledLine(stdin, lastStrippedFrame)
    await waitForLastFrame(lastFrame, (f) => f.includes('→ '))
  })
})
