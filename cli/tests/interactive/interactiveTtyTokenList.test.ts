import './interactiveTestMocks.js'
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import { stripAnsi, stripAnsiCsiAndCr } from '../../src/renderer.js'
import {
  maxConsecutiveBlankLines,
  simulatedScreenFromTtyWrites,
} from '../ttyWriteSimulation.js'
import {
  endTTYSession,
  makeTempConfigDir,
  pressEnter,
  pressKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  typeString,
  type TTYStdin,
  withConfigDir,
} from './interactiveTestHelpers.js'

describe('TTY token list interactive mode', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin
  let restoreConfigDir: () => void

  beforeEach(async () => {
    resetRecallStateForTesting()
    restoreConfigDir = withConfigDir(
      makeTempConfigDir([
        { label: 'Alpha', token: 'a' },
        { label: 'Beta', token: 'b' },
        { label: 'Gamma', token: 'c' },
      ])
    )
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    restoreConfigDir()
    endTTYSession(stdin)
  })

  test('cursor is at input box row after /list-access-token with Current prompt', async () => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/list-access-token')

    const output = ttyOutput(writeSpy)
    const cursorUpMatches = output.matchAll(
      new RegExp(`${String.fromCharCode(0x1b)}\\[(\\d+)A`, 'g')
    )
    const cursorUpValues = [...cursorUpMatches].map((m) => Number(m[1]))
    const lastCursorUp = cursorUpValues.at(-1)
    expect(lastCursorUp).toBeDefined()

    const { wrapTextToLines } = await import('../../src/renderer.js')
    const promptText = 'Select and enter to change the default access token'
    const width = process.stdout.columns ?? 80
    const wrappedPromptLines = wrapTextToLines(promptText, width)
    const currentPromptLines = 2 + wrappedPromptLines.length
    const contentLinesLength = 1
    const boxLinesLength = 3
    const suggestionLinesLength = 3
    const newTotalLines =
      currentPromptLines + boxLinesLength + 0 + suggestionLinesLength
    const expectedCursorUp =
      newTotalLines - currentPromptLines - contentLinesLength

    expect(lastCursorUp).toBe(expectedCursorUp)
  })

  test.each([
    {
      name: 'tokens exist',
      expectPrompt: true,
      expectNoTokensMessage: false,
      useEmptyConfig: false,
    },
    {
      name: 'token list empty',
      expectPrompt: false,
      expectNoTokensMessage: true,
      useEmptyConfig: true,
    },
  ])('/list-access-token: $name - Current prompt when tokens exist, no prompt when empty', async ({
    expectPrompt,
    expectNoTokensMessage,
    useEmptyConfig,
  }) => {
    if (useEmptyConfig) {
      restoreConfigDir()
      withConfigDir(makeTempConfigDir([]))
    }

    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/list-access-token')

    const output = ttyOutput(writeSpy)
    if (expectPrompt) {
      expect(output).toContain('Access tokens')
      expect(output).toContain('Select and enter to change the default')
      expect(output).toContain('↑↓ Enter to select; other keys cancel')
      expect(output).toContain('Alpha')
      expect(output).toContain('Beta')
      expect(output).toContain('Gamma')
      expect(output).toContain('★')
    }
    if (expectNoTokensMessage) {
      expect(output).toContain('No access tokens stored')
    }
  })

  test('Current prompt appears once when selection changes in token list, not duplicated', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()

    pressKey(stdin, 'down')
    await tick()
    pressKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    const visualOutput = simulatedScreenFromTtyWrites(output)
    const promptStart = 'Select and enter to change the default'
    const count = visualOutput.split(promptStart).length - 1
    expect(count).toBe(1)
  })

  test('with narrow terminal and wrapped prompt, separator and input box top appear once when selection changes', async () => {
    const originalColumns = process.stdout.columns
    Object.defineProperty(process.stdout, 'columns', {
      value: 40,
      writable: true,
      configurable: true,
    })

    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()

    pressKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    const visualOutput = simulatedScreenFromTtyWrites(output)
    const lines = visualOutput.split('\n')

    const separatorLines = lines.filter((l) => /^─+$/.test(stripAnsi(l).trim()))
    const boxTopLines = lines.filter((l) => stripAnsi(l).trim().startsWith('┌'))

    Object.defineProperty(process.stdout, 'columns', {
      value: originalColumns,
      writable: true,
      configurable: true,
    })

    expect(separatorLines).toHaveLength(1)
    expect(boxTopLines).toHaveLength(1)
  })

  test('Enter sets highlighted token as default and confirms', async () => {
    await submitTTYCommand(stdin, '/list-access-token')

    pressKey(stdin, 'down')
    await tick()
    writeSpy.mockClear()
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Default token set to: Beta')

    const { getDefaultTokenLabel } = await import('../../src/accessToken.js')
    expect(getDefaultTokenLabel()).toBe('Beta')
  })

  test('selection mode: gray borders (no reset before right │), no arrow, hidden cursor, grayed chrome', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    const output = ttyOutput(writeSpy)
    expect(output).toContain('\x1b[?25l')
    expect(output).toContain('\x1b[90m┌')
    const tokenListBoxSection = output.slice(
      output.lastIndexOf('Select and enter')
    )
    expect(tokenListBoxSection).not.toContain('→')
    const boxSection = output.slice(output.indexOf('Select and enter'))
    const boxLines = boxSection
      .split('\n')
      .filter((l) => l.includes('┌') || l.includes('│') || l.includes('└'))
    for (const line of boxLines) {
      // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI escape codes for terminal output assertion
      expect(/\x1b\[0m\s*│/.test(line)).toBe(false)
    }
  })

  test('any other key cancels token list and shows Cancelled by user. in history', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()

    typeString(stdin, 'q')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('Cancelled by user.')
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')
    expect(output).toContain('\x1b[?25h')
  })

  test('ESC cancels token list and shows Cancelled by user. in history', async () => {
    await submitTTYCommand(stdin, '/remove-access-token')
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('Cancelled by user.')
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')
    expect(output).not.toContain('Token "Alpha" removed')

    const { listAccessTokens } = await import('../../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(3)
    expect(remaining.map((t) => t.label)).toContain('Alpha')
  })

  test.each([
    {
      action: 'select',
      setup: async () => {
        pressKey(stdin, 'down')
        await tick()
        pressEnter(stdin)
        await tick()
      },
      expectMsg: 'Default token set to:',
    },
    {
      action: 'cancel',
      setup: async () => {
        typeString(stdin, 'q')
        await tick()
      },
      expectMsg: 'Cancelled by user.',
    },
  ])('token list $action result message appears without leading blank lines', async ({
    setup,
    expectMsg,
  }) => {
    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()
    await setup()

    const lines = stripAnsiCsiAndCr(
      simulatedScreenFromTtyWrites(ttyOutput(writeSpy))
    ).split('\n')
    const resultLineIdx = lines.findIndex((l) => l.includes(expectMsg))
    expect(
      resultLineIdx,
      `Result "${expectMsg}" not found`
    ).toBeGreaterThanOrEqual(0)

    const beforeResult = lines.slice(0, resultLineIdx)
    const blanks = maxConsecutiveBlankLines(beforeResult)
    expect(
      blanks,
      `Expected at most one empty simulated line before result (full redraw uses renderPastInput + optional gap). Got ${blanks}. Lines before result: ${JSON.stringify(beforeResult)}`
    ).toBeLessThanOrEqual(1)
  })

  test('/remove-access-token shows token list and Enter removes selected', async () => {
    await submitTTYCommand(stdin, '/remove-access-token')

    const midOutput = ttyOutput(writeSpy)
    expect(midOutput).toContain('Remove access token')
    expect(midOutput).toContain('Alpha')
    expect(midOutput).toContain('Beta')

    writeSpy.mockClear()
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Token "Alpha" removed.')

    const { listAccessTokens } = await import('../../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(2)
    expect(remaining.map((t) => t.label)).not.toContain('Alpha')
  })
})
