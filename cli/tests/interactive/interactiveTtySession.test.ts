import './interactiveTestMocks.js'
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { stripAnsi, stripAnsiCsiAndCr } from '../../src/renderer.js'
import {
  countInputBoxTopOutlinesBeforeFirstBoxContent,
  INPUT_BOX_TOP_OUTLINE_PATTERN,
  liveRegionRepaintHasStaleCursorUpBeforeBoxTop,
  simulatedScreenFromTtyWrites,
} from '../ttyWriteSimulation.js'
import { filterCommandsByPrefix, interactiveDocs } from '../../src/help.js'
import {
  endTTYSession,
  GREY_BG_PAST_INPUT,
  pressEnter,
  pressKey,
  submitTTYCommand,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

function lastStdoutLineContaining(
  output: string,
  needle: string
): string | undefined {
  let found: string | undefined
  for (const line of output.split('\n')) {
    if (line.includes(needle)) found = line
  }
  return found
}

describe('TTY: shared interactive session', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  describe('slash command suggestions', () => {
    test('initial display shows "  / commands" in the Current guidance', () => {
      const output = ttyOutput(writeSpy)
      expect(output).toContain('  / commands')
      expect(output).toContain('\x1b[90m')
    })

    test('typing non-slash keeps Current guidance hint instead of command list', async () => {
      writeSpy.mockClear()
      typeString(stdin, 'h')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('  / commands')
      expect(output).not.toContain(
        '/help                List available commands'
      )
    })

    test('typing "/" shows command suggestions in the Current guidance', async () => {
      typeString(stdin, '/')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('/help')
      expect(output).toContain('List available commands')
      expect(output).toContain('/clear')
      expect(output).toContain('Clear screen and chat history')
      expect(output).toContain('/list-access-token')
      expect(output).toContain('↓ more below')
    })

    test('first candidate is highlighted with reverse video', async () => {
      writeSpy.mockClear()
      typeString(stdin, '/')
      await tick()

      const output = ttyOutput(writeSpy)
      const lines = output.split('\n')
      const suggestionStart = lines.findIndex((l) => l.includes('/help'))
      expect(suggestionStart).toBeGreaterThanOrEqual(0)
      expect(lines[suggestionStart]).toContain('\x1b[7m')
      const laterSuggestion = lines.findIndex(
        (l) => l.includes('/clear') && !l.includes('\x1b[7m')
      )
      expect(laterSuggestion).toBeGreaterThan(suggestionStart)
    })

    test('Enter inserts highlighted command with space', async () => {
      typeString(stdin, '/')
      await tick()
      pressEnter(stdin)
      await tick()
      typeString(stdin, 'x')
      await tick()
      pressEnter(stdin)
      await new Promise((r) => setTimeout(r, 50))

      expect(ttyOutput(writeSpy)).toContain('Not supported')
    })

    test('prefix filtering shows only matching commands', async () => {
      writeSpy.mockClear()
      typeString(stdin, '/add')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('/add gmail')
      const plain = stripAnsi(output)
      const lastDrawStart = plain.lastIndexOf('→ /add')
      expect(lastDrawStart).toBeGreaterThanOrEqual(0)
      expect(plain.slice(lastDrawStart)).not.toContain('/help')
      expect(plain.slice(lastDrawStart)).not.toContain('/last email')
    })

    test('Enter with prefix inserts first matching command', async () => {
      typeString(stdin, '/add')
      await tick()
      pressEnter(stdin)
      await tick()
      pressEnter(stdin)
      await new Promise((r) => setTimeout(r, 50))

      expect(ttyOutput(writeSpy)).toContain('/add gmail ')
    })

    test('no suggestions after space when command inserted', async () => {
      typeString(stdin, '/')
      await tick()
      pressEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      const plain = stripAnsi(output)
      expect(plain).toContain('→ /help ')
      const lines = output.split('\n')
      const lastBoxLineIdx = lines.findLastIndex((l) =>
        stripAnsi(l).includes('→ /help ')
      )
      const afterInsert = lines.slice(lastBoxLineIdx + 1).join('\n')
      const suggestionLinesAfterInsert = afterInsert
        .split('\n')
        .filter(
          (l) => l.includes('/help') && l.includes('List available commands')
        )
      expect(suggestionLinesAfterInsert).toHaveLength(0)
    })

    test('first Up from `/` wraps highlight to the last matching command', async () => {
      const filtered = filterCommandsByPrefix(interactiveDocs, '/')
      expect(filtered.length).toBeGreaterThanOrEqual(2)
      const lastUsage = filtered[filtered.length - 1]!.usage

      writeSpy.mockClear()
      typeString(stdin, '/')
      await tick()
      pressKey(stdin, 'up')
      await tick()

      const output = ttyOutput(writeSpy)
      const lastLine = lastStdoutLineContaining(output, `  ${lastUsage}`)
      expect(lastLine).toBeDefined()
      expect(lastLine).toContain('\x1b[7m')
    })

    test('Down moves highlight from the first slash command to the next', async () => {
      const filtered = filterCommandsByPrefix(interactiveDocs, '/')
      expect(filtered.length).toBeGreaterThanOrEqual(2)
      const secondUsage = filtered[1]!.usage

      writeSpy.mockClear()
      typeString(stdin, '/')
      await tick()
      pressKey(stdin, 'down')
      await tick()

      const output = ttyOutput(writeSpy)
      const helpLine = lastStdoutLineContaining(output, '  /help')
      expect(helpLine).toBeDefined()
      expect(helpLine).not.toContain('\x1b[7m')
      expect(lastStdoutLineContaining(output, `  ${secondUsage}`)).toContain(
        '\x1b[7m'
      )
    })

    test('ESC when buffer is only "/" dismisses suggestions and clears buffer', async () => {
      typeString(stdin, '/')
      await tick()
      writeSpy.mockClear()

      pressKey(stdin, 'escape')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('  / commands')
      expect(output).not.toContain('/help')
      expect(output).toContain('`exit` to quit.')
    })

    test('ESC when partial command "/ex" hides suggestions but keeps buffer', async () => {
      typeString(stdin, '/ex')
      await tick()
      writeSpy.mockClear()

      pressKey(stdin, 'escape')
      await tick()

      const output = ttyOutput(writeSpy)
      const plain = stripAnsi(output)
      expect(plain).toContain('→ /ex')
      expect(output).toContain('  / commands')
      expect(output).not.toContain('/exit')
      expect(output).not.toContain('/help')
    })

    test('Enter with only `/` inserts first highlighted command', async () => {
      writeSpy.mockClear()
      typeString(stdin, '/')
      await tick()
      pressEnter(stdin)
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /help ')
    })

    test('Tab with /he completes to /help with space', async () => {
      typeString(stdin, '/he')
      await tick()
      writeSpy.mockClear()

      pressKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /help ')
    })

    test('Tab with /rec completes to common prefix /recall', async () => {
      typeString(stdin, '/rec')
      await tick()
      writeSpy.mockClear()

      pressKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /recall')
    })

    test('Tab with /unknown does nothing', async () => {
      typeString(stdin, '/unknown')
      await tick()

      pressKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /unknown')
    })

    test('Tab when buffer has no leading slash does nothing', async () => {
      typeString(stdin, 'hello')
      await tick()

      pressKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ hello')
    })

    test('Tab with single match shows completed command', async () => {
      typeString(stdin, '/add-a')
      await tick()
      writeSpy.mockClear()

      pressKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /add-access-token ')
    })

    test('/clear clears screen and redraws prompt box', async () => {
      typeString(stdin, '/help ')
      await tick()
      pressEnter(stdin)
      await tick()
      typeString(stdin, '/clear')
      await tick()
      writeSpy.mockClear()

      pressEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('\x1b[H\x1b[2J')
      expect(output).toContain('doughnut')
      expect(output).toContain('┌')
      expect(output).toContain('┘')
      expect(output).toContain('→')
    })

    test('after /clear, Enter on empty input does not show duplicate input box top border', async () => {
      typeString(stdin, '/clear')
      await tick()
      pressEnter(stdin)
      await tick()
      writeSpy.mockClear()

      pressEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      const visualOutput = simulatedScreenFromTtyWrites(output)
      const boxTopLines = visualOutput
        .split('\n')
        .filter((l) =>
          INPUT_BOX_TOP_OUTLINE_PATTERN.test(stripAnsiCsiAndCr(l).trim())
        )
      expect(boxTopLines).toHaveLength(1)
    })

    test('after /help then /clear, run /help again shows help output', async () => {
      typeString(stdin, '/help ')
      await tick()
      pressEnter(stdin)
      await tick()
      typeString(stdin, '/clear')
      await tick()
      pressEnter(stdin)
      await tick()
      writeSpy.mockClear()

      typeString(stdin, '/help ')
      await tick()
      pressEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      expect(stripAnsi(output)).toContain('/help')
      expect(stripAnsi(output)).toContain('List available commands')
    })

    test('after /clear then Enter, input box has exactly one top border (no double border)', async () => {
      typeString(stdin, '/clear')
      await tick()
      pressEnter(stdin)
      await tick()
      writeSpy.mockClear()
      pressEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      expect(countInputBoxTopOutlinesBeforeFirstBoxContent(output)).toBe(1)
    })

    test('after /help, there is one empty line between history output and input box', async () => {
      writeSpy.mockClear()
      await submitTTYCommand(stdin, '/help')

      const visualOutput = simulatedScreenFromTtyWrites(ttyOutput(writeSpy))
      const lines = visualOutput.split('\n').map((l) => stripAnsiCsiAndCr(l))
      const boxTopIndex = lines.findIndex((l) => /^┌─+┐$/.test(l.trim()))
      expect(boxTopIndex).toBeGreaterThan(0)
      expect(lines[boxTopIndex - 1]).toBe('')
    })
  })

  describe('empty Enter redraw (regression)', () => {
    test('empty Enter redraw must not emit a stale cursor-up before the input box top', async () => {
      writeSpy.mockClear()
      pressEnter(stdin)
      await tick()
      await tick()

      const output = ttyOutput(writeSpy)
      expect(
        liveRegionRepaintHasStaleCursorUpBeforeBoxTop(output),
        `Stale double cursor-up before \\r\\x1b[2K┌ — input box shifts up (prefix tail): ${JSON.stringify(output.slice(Math.max(0, output.indexOf('\r\x1b[2K┌') - 40), output.indexOf('\r\x1b[2K┌') + 5))}`
      ).toBe(false)
    })

    test('after blank submits, full redraw must not paint past-input grey blocks (history parity)', async () => {
      writeSpy.mockClear()
      process.stdout.emit('resize')
      await tick()
      const redrawBaseline = ttyOutput(writeSpy)
      const baselineMatches =
        redrawBaseline.split(GREY_BG_PAST_INPUT).length - 1
      expect(
        baselineMatches,
        'baseline resize should not use GREY_BG (only renderPastInput uses this code)'
      ).toBe(0)

      writeSpy.mockClear()
      pressEnter(stdin)
      await tick()
      pressEnter(stdin)
      await tick()

      writeSpy.mockClear()
      process.stdout.emit('resize')
      await tick()

      const redrawAfterEmptySubmits = ttyOutput(writeSpy)
      expect(
        redrawAfterEmptySubmits.split(GREY_BG_PAST_INPUT).length - 1,
        'blank Enter must not add history rows that only appear on full redraw'
      ).toBe(baselineMatches)
    })
  })

  describe('resize', () => {
    test('resize triggers full clear and re-render with new width', async () => {
      typeString(stdin, 'hello')
      await tick()
      pressEnter(stdin)
      await tick()

      writeSpy.mockClear()
      Object.defineProperty(process.stdout, 'columns', {
        value: 50,
        writable: true,
        configurable: true,
      })
      process.stdout.emit('resize')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('\x1b[H\x1b[2J')
      const boxTopMatch = output.match(/┌─+┐/)
      expect(boxTopMatch).toBeTruthy()
      expect(stripAnsi(boxTopMatch![0]).length).toBe(50)
      expect(output).toContain('hello')
    })
  })
})
