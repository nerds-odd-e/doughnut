import './interactiveTestMocks.js'
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  buildCurrentPromptSeparatorForStageBand,
  getTerminalWidth,
  stripAnsiCsiAndCr,
} from '../../src/renderer.js'

/** Ink may close colors with `39m`/`49m` instead of trailing `0m`; assert the band + rule body only. */
function bandedCurrentPromptSeparatorBody(width: number): string {
  const ansiResetAtEnd = new RegExp(`${String.fromCharCode(0x1b)}\\[0m$`)
  return buildCurrentPromptSeparatorForStageBand(width).replace(
    ansiResetAtEnd,
    ''
  )
}
import {
  maxConsecutiveBlankLines,
  simulatedScreenFromTtyWrites,
} from '../ttyWriteSimulation.js'

/** First replayed row (naive CUU/EL replay) whose plain text includes `substring`. */
function firstRowIndexContainingPlain(
  lines: readonly string[],
  substring: string
): number {
  for (let i = 0; i < lines.length; i++) {
    if (stripAnsiCsiAndCr(lines[i] ?? '').includes(substring)) return i
  }
  return -1
}
import {
  endTTYSession,
  makeTempConfigDir,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  pushTTYCommandEscape,
  pushTTYCommandKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
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

  test('/list-access-token: banded Current prompt separator between stage line and instruction (TTY bytes)', async () => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/list-access-token')

    const output = ttyOutput(writeSpy)
    const width = getTerminalWidth()
    const bandedSeparatorBody = bandedCurrentPromptSeparatorBody(width)
    const stageLabel = 'Access tokens'
    const instructionNeedle = 'Select and enter to change the default'

    const stageIdx = output.indexOf(stageLabel)
    const instructionIdx = output.indexOf(instructionNeedle)
    expect(
      stageIdx,
      `TTY capture must include stage label "${stageLabel}" (token list Current Stage indicator).`
    ).toBeGreaterThanOrEqual(0)
    expect(
      instructionIdx,
      `TTY capture must include instruction starting with "${instructionNeedle}".`
    ).toBeGreaterThanOrEqual(0)
    expect(
      instructionIdx,
      `Instruction must render after "${stageLabel}" in the live Current prompt block. stageIdx=${stageIdx}, instructionIdx=${instructionIdx}`
    ).toBeGreaterThan(stageIdx)

    const betweenStageAndInstruction = output.slice(stageIdx, instructionIdx)
    expect(
      betweenStageAndInstruction.includes(bandedSeparatorBody),
      `Missing banded separator (grey bg + ${width} green box-drawing columns) between stage and instruction — same as CommandLineLivePanel under a stage indicator. Preview after stage: ${JSON.stringify(betweenStageAndInstruction.slice(0, 160))}`
    ).toBe(true)
  })

  test('Ink renders token list content after /list-access-token', async () => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/list-access-token')

    const output = ttyOutput(writeSpy)
    expect(output).toContain('Access tokens')
    expect(output).toContain(
      'Select and enter to change the default access token'
    )
    expect(output).toContain('Alpha')
    expect(output).toContain('Beta')
    expect(output).toContain('Gamma')
    expect(output).toContain('↑↓ Enter to select; other keys cancel')
  })

  test('/list-access-token: disabled input row sits between Current prompt and token choices (TTY replay)', async () => {
    const instruction = 'Select and enter to change the default access token'
    const navigationHint = '↑↓ Enter to select; other keys cancel'
    const firstTokenLabel = 'Alpha'

    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/list-access-token')

    const lines = simulatedScreenFromTtyWrites(ttyOutput(writeSpy)).split('\n')
    const instructionRow = firstRowIndexContainingPlain(lines, instruction)
    const hintRow = firstRowIndexContainingPlain(lines, navigationHint)
    const tokenRow = firstRowIndexContainingPlain(lines, firstTokenLabel)

    expect(
      instructionRow,
      'Replayed screen must include /list-access-token Current prompt instruction.'
    ).toBeGreaterThanOrEqual(0)
    expect(
      tokenRow,
      'Replayed screen must include a token list row (fixture label Alpha).'
    ).toBeGreaterThanOrEqual(0)
    expect(
      hintRow,
      'Replayed screen must include token-list navigation copy (also tokenList placeholder).'
    ).toBeGreaterThanOrEqual(0)

    expect(
      instructionRow,
      'Instruction must be above the first token row in the live column.'
    ).toBeLessThan(tokenRow)

    expect(
      hintRow,
      `Navigation/placeholder row (${hintRow}) must be above first token row (${tokenRow}), same as CommandLineLivePanel: grey disabled box between prompt and Current guidance. Got instruction@${instructionRow}, hint@${hintRow}, token@${tokenRow}.`
    ).toBeLessThan(tokenRow)
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

    pushTTYCommandKey(stdin, 'down')
    await tick()
    pushTTYCommandKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    const visualOutput = simulatedScreenFromTtyWrites(output)
    const promptStart = 'Select and enter to change the default'
    const count = visualOutput.split(promptStart).length - 1
    expect(count).toBe(1)
  })

  test('with narrow terminal, token list content appears once when selection changes', async () => {
    const originalColumns = process.stdout.columns
    Object.defineProperty(process.stdout, 'columns', {
      value: 40,
      writable: true,
      configurable: true,
    })

    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()

    pushTTYCommandKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    const visualOutput = simulatedScreenFromTtyWrites(output)

    Object.defineProperty(process.stdout, 'columns', {
      value: originalColumns,
      writable: true,
      configurable: true,
    })

    // After ↓, the rerender should show the guidance text exactly once
    const guidanceCount = visualOutput.split('↑↓ Enter to select').length - 1
    expect(guidanceCount).toBe(1)
  })

  test('Enter sets highlighted token as default and confirms', async () => {
    await submitTTYCommand(stdin, '/list-access-token')

    pushTTYCommandKey(stdin, 'down')
    await tick()
    writeSpy.mockClear()
    pushTTYCommandEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Default token set to: Beta')

    const { getDefaultTokenLabel } = await import('../../src/accessToken.js')
    expect(getDefaultTokenLabel()).toBe('Beta')
  })

  test('selection mode: Ink renders token list items and guidance, no input box borders', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    const output = ttyOutput(writeSpy)
    // Ink token list panel renders items and guidance
    expect(output).toContain('Alpha')
    expect(output).toContain('Beta')
    expect(output).toContain('↑↓ Enter to select; other keys cancel')
    // Ink display does not render an input box (no box-drawing → prompt)
    const tokenListSection = output.slice(
      output.lastIndexOf('Select and enter')
    )
    expect(tokenListSection).not.toContain('→')
  })

  test('any other key cancels token list and shows Cancelled by user. in history', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()

    pushTTYCommandBytes(stdin, 'q')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('Cancelled by user.')
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')
    expect(output).not.toContain('\x1b[?25l')
    expect(output).toContain('\x1b[7m')
  })

  test('ESC cancels token list and shows Cancelled by user. in history', async () => {
    await submitTTYCommand(stdin, '/remove-access-token')
    writeSpy.mockClear()

    await pushTTYCommandEscape(stdin)

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
        pushTTYCommandKey(stdin, 'down')
        await tick()
        pushTTYCommandEnter(stdin)
        await tick()
      },
      expectMsg: 'Default token set to:',
    },
    {
      action: 'cancel',
      setup: async () => {
        pushTTYCommandBytes(stdin, 'q')
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
      `Expected at most one empty simulated line before result (full redraw uses renderPastUserMessage + optional gap). Got ${blanks}. Lines before result: ${JSON.stringify(beforeResult)}`
    ).toBeLessThanOrEqual(1)
  })

  test('/remove-access-token shows token list and Enter removes selected', async () => {
    await submitTTYCommand(stdin, '/remove-access-token')

    const midOutput = ttyOutput(writeSpy)
    expect(midOutput).toContain('Remove access token')
    expect(midOutput).toContain('Alpha')
    expect(midOutput).toContain('Beta')

    writeSpy.mockClear()
    pushTTYCommandEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Token "Alpha" removed.')

    const { listAccessTokens } = await import('../../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(2)
    expect(remaining.map((t) => t.label)).not.toContain('Alpha')
  })
})
