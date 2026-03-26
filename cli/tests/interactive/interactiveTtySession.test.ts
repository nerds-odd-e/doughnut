import './interactiveTestMocks.js'
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { stripAnsi, stripAnsiCsiAndCr } from '../../src/renderer.js'
import {
  liveRegionRepaintHasStaleCursorUpBeforeBoxTop,
  ttyShowCaretCuUThenInk2KEraseBeforeNextHide,
  simulatedScreenFromTtyWrites,
} from '../ttyWriteSimulation.js'
import { filterCommandsByPrefix, interactiveDocs } from '../../src/help.js'
import {
  endTTYSession,
  GREY_BG_PAST_INPUT,
  lastStdoutLineContaining,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  pushTTYCommandEscape,
  pushTTYCommandKey,
  submitTTYCommand,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  type TTYStdin,
} from './interactiveTestHelpers.js'

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
      pushTTYCommandBytes(stdin, 'h')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('  / commands')
      expect(output).not.toContain(
        '/help                List available commands'
      )
    })

    test('typing "/" shows command suggestions in the Current guidance', async () => {
      pushTTYCommandBytes(stdin, '/')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(output).toContain('/help')
      expect(output).toContain('List available commands')
      expect(output).toContain('/exit')
      expect(output).toContain('Quit the CLI')
      expect(output).toContain('/list-access-token')
      expect(output).toContain('↓ more below')
    })

    test('first candidate is highlighted with reverse video', async () => {
      writeSpy.mockClear()
      pushTTYCommandBytes(stdin, '/')
      await tick()

      const output = ttyOutput(writeSpy)
      const lines = output.split('\n')
      const suggestionStart = lines.findIndex((l) => l.includes('/help'))
      expect(suggestionStart).toBeGreaterThanOrEqual(0)
      expect(lines[suggestionStart]).toContain('\x1b[7m')
      const laterSuggestion = lines.findIndex(
        (l) => l.includes('/exit') && !l.includes('\x1b[7m')
      )
      expect(laterSuggestion).toBeGreaterThan(suggestionStart)
    })

    test('Enter inserts highlighted command with space', async () => {
      pushTTYCommandBytes(stdin, '/')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()
      pushTTYCommandBytes(stdin, 'x')
      await tick()
      pushTTYCommandEnter(stdin)
      await new Promise((r) => setTimeout(r, 50))

      expect(ttyOutput(writeSpy)).toContain('Not supported')
    })

    test('prefix filtering shows only matching commands', async () => {
      writeSpy.mockClear()
      pushTTYCommandBytes(stdin, '/add')
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
      pushTTYCommandBytes(stdin, '/add')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()
      pushTTYCommandEnter(stdin)
      await new Promise((r) => setTimeout(r, 50))

      expect(ttyOutput(writeSpy)).toContain('/add gmail ')
    })

    test('no suggestions after space when command inserted', async () => {
      pushTTYCommandBytes(stdin, '/')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      const plain = stripAnsi(output)
      expect(plain).toContain('→ /help ')
      const lines = output.split('\n')
      const lastBoxLineIdx = lines.findLastIndex((l) =>
        stripAnsi(l).includes('→ /help ')
      )
      const afterInsert = lines.slice(lastBoxLineIdx + 1).join('\n')
      const guidanceLinesAfterInsert = afterInsert
        .split('\n')
        .filter(
          (l) => l.includes('/help') && l.includes('List available commands')
        )
      expect(guidanceLinesAfterInsert).toHaveLength(0)
    })

    test('first Up from `/` moves caret home; second Up wraps highlight to the last matching command', async () => {
      const filtered = filterCommandsByPrefix(interactiveDocs, '/')
      expect(filtered.length).toBeGreaterThanOrEqual(2)
      const firstUsage = filtered[0]!.usage
      const lastUsage = filtered[filtered.length - 1]!.usage

      writeSpy.mockClear()
      pushTTYCommandBytes(stdin, '/')
      await tick()
      pushTTYCommandKey(stdin, 'up')
      await tick()

      const afterFirstUp = ttyOutput(writeSpy)
      const firstLineAfterFirst = lastStdoutLineContaining(
        afterFirstUp,
        `  ${firstUsage}`
      )
      expect(firstLineAfterFirst).toBeDefined()
      expect(
        firstLineAfterFirst,
        'Phase A1: first ↑ with caret after `/` must move to column 0, not cycle slash highlights yet'
      ).toContain('\x1b[7m')

      pushTTYCommandKey(stdin, 'up')
      await tick()

      const afterSecondUp = ttyOutput(writeSpy)
      const lastLineAfterSecond = lastStdoutLineContaining(
        afterSecondUp,
        `  ${lastUsage}`
      )
      expect(lastLineAfterSecond).toBeDefined()
      expect(lastLineAfterSecond).toContain('\x1b[7m')
    })

    test('first Down after caret-home on `/` moves caret to end without cycling slash highlight (phase A2)', async () => {
      const filtered = filterCommandsByPrefix(interactiveDocs, '/')
      expect(filtered.length).toBeGreaterThanOrEqual(2)
      const firstUsage = filtered[0]!.usage
      const secondUsage = filtered[1]!.usage

      writeSpy.mockClear()
      pushTTYCommandBytes(stdin, '/')
      await tick()
      pushTTYCommandKey(stdin, 'up')
      await tick()
      pushTTYCommandKey(stdin, 'down')
      await tick()

      const out = ttyOutput(writeSpy)
      expect(
        lastStdoutLineContaining(out, `  ${firstUsage}`),
        'Phase A2: first ↓ with caret before end of `/` must not cycle slash highlights yet'
      ).toContain('\x1b[7m')
      expect(
        lastStdoutLineContaining(out, `  ${secondUsage}`),
        'Slash highlight must stay on the first row until the caret is already at the draft end'
      ).not.toContain('\x1b[7m')
    })

    test('slash picker: ArrowDown moves highlight; first Up moves caret home, second Up moves highlight back', async () => {
      const filtered = filterCommandsByPrefix(interactiveDocs, '/')
      expect(filtered.length).toBeGreaterThanOrEqual(2)
      const firstUsage = filtered[0]!.usage
      const secondUsage = filtered[1]!.usage
      const needleFirst = `  ${firstUsage}`
      const needleSecond = `  ${secondUsage}`

      writeSpy.mockClear()
      pushTTYCommandBytes(stdin, '/')
      await tick()
      pushTTYCommandKey(stdin, 'down')
      await tick()

      const afterDown = ttyOutput(writeSpy)
      expect(
        lastStdoutLineContaining(afterDown, needleSecond),
        'With "/" list open, Down must highlight the next command (reverse video), not only move the caret.'
      ).toContain('\x1b[7m')
      const firstLineAfterDown = lastStdoutLineContaining(
        afterDown,
        needleFirst
      )
      expect(firstLineAfterDown).toBeDefined()
      expect(
        firstLineAfterDown,
        'After Down, the first command row must no longer be highlighted.'
      ).not.toContain('\x1b[7m')

      pushTTYCommandKey(stdin, 'up')
      await tick()

      const afterFirstUp = ttyOutput(writeSpy)
      expect(
        lastStdoutLineContaining(afterFirstUp, needleSecond),
        'Phase A1: first ↑ after Down must not move slash highlight yet (caret leaves end of `/` first).'
      ).toContain('\x1b[7m')
      expect(
        lastStdoutLineContaining(afterFirstUp, needleFirst),
        'First row stays unhighlighted after the caret-only ↑.'
      ).not.toContain('\x1b[7m')

      pushTTYCommandKey(stdin, 'up')
      await tick()

      const afterSecondUp = ttyOutput(writeSpy)
      expect(
        lastStdoutLineContaining(afterSecondUp, needleFirst),
        'Second ↑ with caret at column 0 must highlight the first command again.'
      ).toContain('\x1b[7m')
      const secondLineAfterSecondUp = lastStdoutLineContaining(
        afterSecondUp,
        needleSecond
      )
      expect(secondLineAfterSecondUp).toBeDefined()
      expect(
        secondLineAfterSecondUp,
        'After the second Up, the second command row must no longer be highlighted.'
      ).not.toContain('\x1b[7m')
    })

    test('ESC when buffer is only "/" dismisses suggestions and clears buffer', async () => {
      pushTTYCommandBytes(stdin, '/')
      await tick()
      writeSpy.mockClear()

      await pushTTYCommandEscape(stdin)

      const output = ttyOutput(writeSpy)
      expect(output).toContain('  / commands')
      expect(output).not.toContain('/help')
      expect(output).toContain('`exit` to quit.')
    })

    test('ESC when partial command "/ex" hides suggestions but keeps buffer', async () => {
      pushTTYCommandBytes(stdin, '/ex')
      await tick()
      writeSpy.mockClear()

      await pushTTYCommandEscape(stdin)

      const output = ttyOutput(writeSpy)
      const plain = stripAnsi(output)
      expect(plain).toContain('→ /ex')
      expect(output).toContain('  / commands')
      expect(output).not.toContain('/exit')
      expect(output).not.toContain('/help')
    })

    test('after ESC on a partial slash draft, the next typed character still edits the line (Ink focus)', async () => {
      pushTTYCommandBytes(stdin, '/ex')
      await tick()
      await pushTTYCommandEscape(stdin)
      await tick()
      pushTTYCommandBytes(stdin, 't')
      await tick()
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /ext')
    })

    test('Enter with only `/` inserts first highlighted command', async () => {
      writeSpy.mockClear()
      pushTTYCommandBytes(stdin, '/')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /help ')
    })

    test('Tab with /he completes to /help with space', async () => {
      pushTTYCommandBytes(stdin, '/he')
      await tick()
      writeSpy.mockClear()

      pushTTYCommandKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /help ')
    })

    test('Tab with /rec completes to common prefix /recall', async () => {
      pushTTYCommandBytes(stdin, '/rec')
      await tick()
      writeSpy.mockClear()

      pushTTYCommandKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /recall')
    })

    test('Tab with /unknown does nothing', async () => {
      pushTTYCommandBytes(stdin, '/unknown')
      await tick()

      pushTTYCommandKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /unknown')
    })

    test('Tab when buffer has no leading slash does nothing', async () => {
      pushTTYCommandBytes(stdin, 'hello')
      await tick()

      pushTTYCommandKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ hello')
    })

    test('Tab with single match shows completed command', async () => {
      pushTTYCommandBytes(stdin, '/add-a')
      await tick()
      writeSpy.mockClear()

      pushTTYCommandKey(stdin, 'tab')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /add-access-token ')
    })

    test('after /help, consecutive Enter on empty input keeps a single command-line prompt row', async () => {
      await submitTTYCommand(stdin, '/help')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()
      writeSpy.mockClear()

      pushTTYCommandEnter(stdin)
      await tick()

      const output = ttyOutput(writeSpy)
      const visualOutput = simulatedScreenFromTtyWrites(output)
      const promptRows = visualOutput
        .split('\n')
        .filter((l) => stripAnsiCsiAndCr(l).trimStart().startsWith('→'))
      expect(promptRows).toHaveLength(1)
    })

    test('second /help after first still shows help output', async () => {
      await submitTTYCommand(stdin, '/help')
      await tick()
      writeSpy.mockClear()

      await submitTTYCommand(stdin, '/help')
      await tick()

      const output = ttyOutput(writeSpy)
      expect(stripAnsi(output)).toContain('/help')
      expect(stripAnsi(output)).toContain('List available commands')
    })

    test('after /help, there is one empty line between past CLI assistant messages and command line', async () => {
      writeSpy.mockClear()
      await submitTTYCommand(stdin, '/help')

      const visualOutput = simulatedScreenFromTtyWrites(ttyOutput(writeSpy))
      const lines = visualOutput.split('\n').map((l) => stripAnsiCsiAndCr(l))
      const promptIndex = lines.findIndex((l) => l.trimStart().startsWith('→'))
      expect(promptIndex).toBeGreaterThan(0)
      expect(lines[promptIndex - 1]).toBe('')
    })
  })

  describe('command-line prompt row (regression)', () => {
    test('initial paint: command-line row has no leading spaces before →', async () => {
      await tick()
      await tick()

      const raw = ttyOutput(writeSpy)
      const lines = simulatedScreenFromTtyWrites(raw)
        .split('\n')
        .map((l) => stripAnsiCsiAndCr(l))

      let promptRow: string | undefined
      for (let i = lines.length - 1; i >= 0; i--) {
        const line = lines[i] ?? ''
        if (line.trimStart().startsWith('→')) {
          promptRow = line
          break
        }
      }

      expect(
        promptRow !== undefined,
        [
          'Expected the final TTY frame to include a command line row starting with "→".',
          'If this fails, the replayed screen may be missing the live prompt — update the test only if the product intentionally changed the chrome.',
        ].join('\n')
      ).toBe(true)

      expect(
        promptRow === promptRow!.trimStart(),
        [
          'The command line must start at terminal column 0: no leading spaces before "→" (Ink flex next to <Static> history can otherwise indent the live block).',
          `Replayed prompt row (JSON): ${JSON.stringify(promptRow)}`,
        ].join('\n')
      ).toBe(true)
    })
  })

  describe('command-line caret (reverse video in Ink)', () => {
    test('after initial paint, prompt line exists and stdout includes reverse-video caret', async () => {
      await tick()
      await tick()

      const raw = ttyOutput(writeSpy)
      const visual = simulatedScreenFromTtyWrites(raw)
      const plainLines = visual.split('\n').map((l) => stripAnsiCsiAndCr(l))

      let promptRow = -1
      for (let i = plainLines.length - 1; i >= 0; i--) {
        const p = plainLines[i] ?? ''
        if (p.trimStart().startsWith('→')) {
          promptRow = i
          break
        }
      }

      expect(
        promptRow >= 0,
        [
          'Expected the replayed TTY frame to contain a command line row starting with "→".',
          'If this fails, the chrome or prompt glyph changed — narrow the matcher only if the product intentionally changed how the command line is drawn.',
        ].join('\n')
      ).toBe(true)

      expect(
        raw.includes('\x1b[7m'),
        'Caret is drawn as reverse video (\\x1b[7m) inside the Ink live region; hardware cursor stays hidden.'
      ).toBe(true)
    })
  })

  describe('input box vertical stability on draft edits (regression)', () => {
    test('must not place caret with CUU/CHA and then let Ink 2K+CUU run before the next hide', async () => {
      await tick()
      await tick()

      pushTTYCommandBytes(stdin, 'x')
      await tick()
      await tick()

      const raw = ttyOutput(writeSpy)
      expect(
        ttyShowCaretCuUThenInk2KEraseBeforeNextHide(raw),
        [
          'After SHOW_CURSOR, the adapter must not emit manual cursor-up + CHA to sit the caret in the command line and then allow Ink log-update to run \\x1b[2K\\x1b[1A (erase line + cursor up) before the next HIDE_CURSOR.',
          'Ink assumes the cursor is still where it left off after the last render; moving the caret up for the user breaks that contract, so the next incremental repaint erases from the wrong row and the live block walks up the screen (~a few lines per keystroke).',
          'A real fix hides the cursor before Ink rerenders, avoids post-render CUU, or otherwise resyncs vertical state — do not weaken this check to only the final replayed frame (that snapshot can look fine while the protocol is wrong).',
        ].join('\n')
      ).toBe(false)
    })
  })

  describe('empty Enter redraw (regression)', () => {
    test('empty Enter redraw must not emit a stale cursor-up before the command-line row', async () => {
      writeSpy.mockClear()
      pushTTYCommandEnter(stdin)
      await tick()
      await tick()

      const output = ttyOutput(writeSpy)
      expect(
        liveRegionRepaintHasStaleCursorUpBeforeBoxTop(output),
        'Stale double cursor-up before \\r\\x1b[2K on the command-line row — live block shifts up'
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
        'baseline resize should not use GREY_BG (only renderPastUserMessage uses this code)'
      ).toBe(0)

      writeSpy.mockClear()
      pushTTYCommandEnter(stdin)
      await tick()
      pushTTYCommandEnter(stdin)
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
    test('resize rerenders Ink shell with new width without full-screen clear', async () => {
      pushTTYCommandBytes(stdin, 'hello')
      await tick()
      pushTTYCommandEnter(stdin)
      await tick()

      expect(ttyOutput(writeSpy)).toContain('hello')

      writeSpy.mockClear()
      Object.defineProperty(process.stdout, 'columns', {
        value: 50,
        writable: true,
        configurable: true,
      })
      process.stdout.emit('resize')
      await tick()

      const output = ttyOutput(writeSpy)
      const promptMatch = output.match(/→[^\n]*/)
      expect(promptMatch).toBeTruthy()
      expect(stripAnsi(promptMatch![0]).length).toBeLessThanOrEqual(50)
    })
  })
})
