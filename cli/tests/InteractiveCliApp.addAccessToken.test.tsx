import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { UserController } from 'doughnut-api'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  pressEscapeAndWait,
  pressEscapeAndWaitForCancelledLine,
  renderInkWhenCommandLineReady,
  waitForFrames,
} from './inkTestHelpers.js'

const EXPECT_GUIDANCE_MORE_BELOW = '↓ more below'
const EXPECT_GUIDANCE_ROW_BUDGET = 5

function rawLineIncludesBoldAndText(raw: string, text: string): boolean {
  return raw
    .split('\n')
    .some((line) => line.includes(text) && line.includes('\x1b[1m'))
}

describe('InteractiveCliApp /add-access-token', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let getTokenInfoSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-at-'))
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    getTokenInfoSpy = vi
      .spyOn(UserController, 'getTokenInfo')
      .mockResolvedValue({
        data: { id: 1, label: 'Vitest token label' },
      } as Awaited<ReturnType<typeof UserController.getTokenInfo>>)
  })

  afterEach(() => {
    getTokenInfoSpy.mockRestore()
    if (savedConfigDir === undefined) delete process.env.DOUGHNUT_CONFIG_DIR
    else process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('valid token line shows success and persists access-tokens.json', async () => {
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFramesToInclude('Token added successfully')
    await waitForFramesToInclude('/add-access-token unit-test-token-value')

    const stored = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[] }
    expect(stored.tokens).toEqual([
      { label: 'Vitest token label', token: 'unit-test-token-value' },
    ])
  })

  test('bare /add-access-token shows usage hint', async () => {
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/add-access-token\r')
    await waitForFramesToInclude('Missing access token')
    await waitForFramesToInclude('/add-access-token <token>')
    expect(getTokenInfoSpy).not.toHaveBeenCalled()
  })

  test('invalid token shows assistant error and does not write access-tokens.json', async () => {
    getTokenInfoSpy.mockRejectedValue({ status: 401 })
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/add-access-token bad-token\r')
    await waitForFramesToInclude('Access token is invalid or expired')

    expect(fs.existsSync(path.join(configDir, 'access-tokens.json'))).toBe(
      false
    )
  })

  test('Esc during verify shows Cancelled when getTokenInfo honors AbortSignal', async () => {
    getTokenInfoSpy.mockImplementation((_opts?: { signal?: AbortSignal }) => {
      return new Promise((_, reject) => {
        const signal = _opts?.signal
        if (signal?.aborted) {
          reject(new DOMException('The operation was aborted', 'AbortError'))
          return
        }
        const onAbort = () => {
          signal?.removeEventListener('abort', onAbort)
          reject(new DOMException('The operation was aborted', 'AbortError'))
        }
        signal?.addEventListener('abort', onAbort, { once: true })
      })
    })

    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFramesToInclude('Verifying token')

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'))

    expect(fs.existsSync(path.join(configDir, 'access-tokens.json'))).toBe(
      false
    )
  })

  test('/list-access-token Esc closes picker with abort line (no list dump)', async () => {
    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFramesToInclude('Token added successfully')

    stdin.write('/list-access-token\r')
    await waitForFramesToInclude(
      /(?=.*Access tokens)(?=.*1\. Vitest token label)/s
    )

    await pressEscapeAndWait(
      stdin,
      () => frames.join('\n'),
      (c) =>
        c.includes('Cancelled.') &&
        c.includes('/list-access-token') &&
        c.includes('→ ') &&
        c.includes('/ commands')
    )

    expect(frames.join('\n')).not.toContain('Stored access tokens:')
  })

  test('/list-access-token with many labels uses a fixed-height list with more-below', async () => {
    fs.writeFileSync(
      path.join(configDir, 'access-tokens.json'),
      JSON.stringify({
        tokens: Array.from({ length: 8 }, (_, i) => ({
          label: `L${i}`,
          token: `t${i}`,
        })),
        defaultLabel: 'L0',
      }),
      'utf-8'
    )

    const { stdin, lastStrippedFrame, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/list-access-token\r')
    await waitForFramesToInclude(
      /(?=.*Access tokens)(?=.*↓ more below)(?=.*1\. L0)/s
    )

    const plain = lastStrippedFrame()
    const rows = plain
      .split('\n')
      .filter(
        (l) => l.includes(EXPECT_GUIDANCE_MORE_BELOW) || /\d+\.\s+L\d/.test(l)
      )
    expect(rows.length).toBe(EXPECT_GUIDANCE_ROW_BUDGET)
  })

  test('/list-access-token Down+Enter sets default token in access-tokens.json', async () => {
    fs.writeFileSync(
      path.join(configDir, 'access-tokens.json'),
      JSON.stringify({
        tokens: [
          { label: 'Alpha', token: 't1' },
          { label: 'Beta', token: 't2' },
        ],
        defaultLabel: 'Alpha',
      }),
      'utf-8'
    )

    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/list-access-token\r')
    await waitForFramesToInclude(/(?=.*Access tokens)(?=.*2\. Beta)/s)

    stdin.write('\u001b[B')
    await waitForFrames(
      () => frames.at(-1) ?? '',
      (f) => rawLineIncludesBoldAndText(f, '2. Beta')
    )

    stdin.write('\r')
    await waitForFramesToInclude('Default token set to: Beta')

    const stored = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[]; defaultLabel?: string }
    expect(stored.defaultLabel).toBe('Beta')
  })

  test('/list-access-token with no tokens commits assistant line without Escape', async () => {
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/list-access-token\r')
    await waitForFramesToInclude('No access tokens stored.')
    await waitForFramesToInclude('/list-access-token')
    await waitForFramesToInclude('→ ')
    await waitForFramesToInclude('/ commands')
  })
})
