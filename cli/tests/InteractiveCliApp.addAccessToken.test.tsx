import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { UserController } from 'doughnut-api'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'
import {
  pressEscapeAndWait,
  pressEscapeAndWaitForCancelledLine,
  renderInkWhenCommandLineReady,
  waitForFrames,
} from './inkTestHelpers.js'

describe('InteractiveCliApp /add-access-token', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let getTokenInfoSpy: ReturnType<typeof vi.spyOn>
  let revokeTokenSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-at-'))
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    getTokenInfoSpy = vi
      .spyOn(UserController, 'getTokenInfo')
      .mockResolvedValue({
        data: { id: 1, label: 'Vitest token label' },
      } as Awaited<ReturnType<typeof UserController.getTokenInfo>>)
    revokeTokenSpy = vi
      .spyOn(UserController, 'revokeToken')
      .mockResolvedValue(
        {} as Awaited<ReturnType<typeof UserController.revokeToken>>
      )
  })

  afterEach(() => {
    getTokenInfoSpy.mockRestore()
    revokeTokenSpy.mockRestore()
    if (savedConfigDir === undefined) delete process.env.DOUGHNUT_CONFIG_DIR
    else process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('valid token line shows success and persists access-tokens.json', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    expect(frames.join('\n')).toContain(formatVersionOutput())

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Token added successfully') &&
        c.includes('/add-access-token unit-test-token-value')
    )

    const stored = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[] }
    expect(stored.tokens).toEqual([
      { label: 'Vitest token label', token: 'unit-test-token-value' },
    ])
  })

  test('bare /add-access-token shows usage hint', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/add-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Missing access token') &&
        c.includes('/add-access-token <token>')
    )
    expect(getTokenInfoSpy).not.toHaveBeenCalled()
  })

  test('invalid token shows assistant error and does not write access-tokens.json', async () => {
    getTokenInfoSpy.mockRejectedValue({ status: 401 })
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/add-access-token bad-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Access token is invalid or expired')
    )

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

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Verifying token')
    )

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'))

    expect(fs.existsSync(path.join(configDir, 'access-tokens.json'))).toBe(
      false
    )
  })

  test('/list-access-token Esc closes picker with abort line (no list dump)', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Token added successfully')
    )

    stdin.write('/list-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Access tokens') && c.includes('1. Vitest token label')
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

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/list-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Access tokens') && c.includes('2. Beta')
    )

    stdin.write('\u001b[B')
    await waitForFrames(
      () => frames.at(-1) ?? '',
      (f) => f.includes('\x1b[7m2.')
    )

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Default token set to: Beta')
    )

    const stored = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[]; defaultLabel?: string }
    expect(stored.defaultLabel).toBe('Beta')
  })

  test('/list-access-token with no tokens commits assistant line without Escape', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/list-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('No access tokens stored.') &&
        c.includes('/list-access-token') &&
        c.includes('→ ') &&
        c.includes('/ commands')
    )
  })

  test('bare /remove-access-token opens picker; Down+Enter removes selected token from file', async () => {
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

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/remove-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Remove access token') &&
        c.includes('1. Alpha') &&
        c.includes('2. Beta')
    )

    stdin.write('\u001b[B')
    await waitForFrames(
      () => frames.at(-1) ?? '',
      (f) => f.includes('\x1b[7m2.')
    )

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Token "Beta" removed.')
    )

    const stored = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[]; defaultLabel?: string }
    expect(stored.tokens).toEqual([{ label: 'Alpha', token: 't1' }])
    expect(stored.defaultLabel).toBe('Alpha')
    expect(revokeTokenSpy).not.toHaveBeenCalled()
  })

  test('bare /remove-access-token-completely opens picker; Down+Enter revokes selected token', async () => {
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

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/remove-access-token-completely\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Remove access token completely') && c.includes('2. Beta')
    )

    stdin.write('\u001b[B')
    await waitForFrames(
      () => frames.at(-1) ?? '',
      (f) => f.includes('\x1b[7m2.')
    )

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Token "Beta" removed locally and from server.') &&
        revokeTokenSpy.mock.calls.length >= 1
    )

    const stored = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[]; defaultLabel?: string }
    expect(stored.tokens).toEqual([{ label: 'Alpha', token: 't1' }])
    expect(stored.defaultLabel).toBe('Alpha')
  })

  test('/remove-access-token removes stored label and list is empty in transcript', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Token added successfully')
    )

    stdin.write('/remove-access-token Vitest token label\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Token "Vitest token label" removed.')
    )

    stdin.write('/list-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('No access tokens stored.')
    )

    const afterList = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[] }
    expect(afterList.tokens).toEqual([])
    expect(revokeTokenSpy).not.toHaveBeenCalled()
  })

  test('/remove-access-token-completely revokes then clears config', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Token added successfully')
    )

    stdin.write('/remove-access-token-completely Vitest token label\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes(
          'Token "Vitest token label" removed locally and from server.'
        ) && revokeTokenSpy.mock.calls.length >= 1
    )

    stdin.write('/list-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('No access tokens stored.')
    )

    const afterList = JSON.parse(
      fs.readFileSync(path.join(configDir, 'access-tokens.json'), 'utf-8')
    ) as { tokens: { label: string; token: string }[] }
    expect(afterList.tokens).toEqual([])
  })
})
