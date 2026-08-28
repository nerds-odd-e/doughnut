import { afterEach, beforeEach, describe, test, vi } from 'vitest'
import {
  pressEscapeAndWaitForCancelledLine,
  waitForLastFrame,
} from './inkTestHelpers.js'
import {
  cleanupTestEnv,
  createTestConfigDir,
  expectSuccessLineOnceOnScreen,
  renderApp,
  submitCommandAndWaitForError,
  writeGmailConfig,
  writeLastEmailFixtureGmailConfig,
} from './InteractiveCliApp.gmail.testHelpers.js'

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

    const { stdin, lastStrippedFrame, lastFrame } = await renderApp()

    stdin.write('/last email\r')
    await waitForLastFrame(
      lastFrame,
      (f) =>
        f.includes('Loading last email') &&
        f.includes('/last email') &&
        !f.includes('→ ')
    )

    await pressEscapeAndWaitForCancelledLine(stdin, lastStrippedFrame)
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
                headers: [{ name: 'Subject', value: 'Welcome to Donut' }],
              },
            }),
        })
    )

    const { stdin, lastFrame, lastStrippedFrame } = await renderApp()
    const successLine = 'Welcome to Donut'

    stdin.write('/last email\r')
    await waitForLastFrame(
      lastFrame,
      (f) => f.includes(successLine) && f.includes('→ ')
    )
    expectSuccessLineOnceOnScreen(successLine, lastStrippedFrame)
  })

  test('shows no-account error in transcript after /last email', async () => {
    writeGmailConfig(configDir, { accounts: [] })

    const { stdin, waitForLastFrameToInclude } = await renderApp()
    await submitCommandAndWaitForError(
      stdin,
      waitForLastFrameToInclude,
      '/last email',
      'No Gmail account configured.'
    )
  })
})
