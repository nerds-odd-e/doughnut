import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { UserController } from 'doughnut-api'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'

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
    if (predicate(getCombined())) return
    await new Promise<void>((resolve) => {
      setImmediate(resolve)
    })
  }
  throw new Error(
    `Output condition not met within ${maxTicks} event-loop turns. Last frames:\n${getCombined()}`
  )
}

async function renderApp() {
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
    const { stdin, frames } = await renderApp()
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
    const { stdin, frames } = await renderApp()

    stdin.write('/add-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Missing access token') &&
        c.includes('/add-access-token <token>')
    )
    expect(getTokenInfoSpy).not.toHaveBeenCalled()
  })

  test('/list-access-token shows list until Escape; then transcript and default guidance', async () => {
    const { stdin, frames } = await renderApp()

    stdin.write('/add-access-token unit-test-token-value\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Token added successfully')
    )

    stdin.write('/list-access-token\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Stored access tokens:') &&
        c.includes('1. Vitest token label')
    )

    stdin.write('\u001b')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/list-access-token') &&
        c.includes('1. Vitest token label') &&
        c.includes('> ') &&
        c.includes('/ commands')
    )
  })
})
