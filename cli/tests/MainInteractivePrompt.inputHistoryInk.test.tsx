import * as fs from 'node:fs'
import * as path from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { USER_INPUT_HISTORY_FILENAME } from '../src/inputHistory/index.js'
import { pressEscape } from './inkTestHelpers.js'
import {
  installMainInteractivePromptConfig,
  lineWithMainPrompt,
  mainPromptDraftAfterArrow,
  renderMainInteractivePrompt,
} from './mainInteractivePrompt.testHelpers.js'

describe('MainInteractivePrompt user input history (↑↓ recall)', () => {
  installMainInteractivePromptConfig()

  test('after Enter, up recalls committed line; down restores pre-walk draft', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('alpha')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('alpha'))
    stdin.write('\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('alpha'))
    expect(onCommittedLine).toHaveBeenCalledWith('alpha')

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('alpha'))

    stdin.write('\x1b[B')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('alpha'))
  })

  test('with slash list dismissed, up at caret 0 recalls history instead of cycling list', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('memo\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('memo'))
    expect(onCommittedLine).toHaveBeenCalledWith('memo')

    stdin.write('/he')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )
    await pressEscape(stdin)
    await waitUntilLastFrame(
      (f) => f.includes('/ commands') && mainPromptDraftAfterArrow(f) === '/he'
    )

    stdin.write('\x1b[D\x1b[D\x1b[D')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('/he'))

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('memo'))
  })

  test('after Esc hides slash list, up recalls history; down restores pre-walk /re draft', async () => {
    const { stdin, waitUntilLastFrame } = await renderMainInteractivePrompt()

    stdin.write('z\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('z'))

    stdin.write('/re')
    await waitUntilLastFrame((f) => f.includes('/recall'))

    await pressEscape(stdin)
    await waitUntilLastFrame(
      (f) => f.includes('/ commands') && mainPromptDraftAfterArrow(f) === '/re'
    )

    stdin.write('\x1b[D\x1b[D\x1b[D')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('/re'))

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('z'))

    stdin.write('\x1b[B')
    await waitUntilLastFrame(
      (f) =>
        lineWithMainPrompt(f).includes('/re') &&
        !lineWithMainPrompt(f).includes('z') &&
        f.includes('/recall')
    )
  })
})

describe('MainInteractivePrompt user input history persistence', () => {
  const { getPromptConfigDir } = installMainInteractivePromptConfig()

  test('writes user-input-history.json after Enter commits (newest first)', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('first\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('first'))

    stdin.write('second\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('second'))

    const p = path.join(getPromptConfigDir(), USER_INPUT_HISTORY_FILENAME)
    expect(JSON.parse(fs.readFileSync(p, 'utf-8'))).toEqual(['second', 'first'])
  })

  test('fresh mount loads history from disk; up recalls stored line', async () => {
    fs.writeFileSync(
      path.join(getPromptConfigDir(), USER_INPUT_HISTORY_FILENAME),
      `${JSON.stringify(['from-disk'])}\n`,
      'utf-8'
    )

    const { stdin, waitUntilLastFrame } = await renderMainInteractivePrompt()

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('from-disk'))
  })

  test('DONUT_CLI_DISABLE_INPUT_HISTORY=1 skips writing history file', async () => {
    process.env.DONUT_CLI_DISABLE_INPUT_HISTORY = '1'
    try {
      const onCommittedLine = vi.fn()
      const { stdin, waitUntilLastFrame } =
        await renderMainInteractivePrompt(onCommittedLine)

      stdin.write('no-file\r')
      await waitUntilLastFrame(
        (f) => !lineWithMainPrompt(f).includes('no-file')
      )

      const p = path.join(getPromptConfigDir(), USER_INPUT_HISTORY_FILENAME)
      expect(fs.existsSync(p)).toBe(false)
    } finally {
      delete process.env.DONUT_CLI_DISABLE_INPUT_HISTORY
    }
  })
})
