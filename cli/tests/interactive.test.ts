import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { Readable } from 'node:stream'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  buildBoxLines,
  highlightRecognizedCommand,
  isInRecallSubstate,
  processInput,
  renderBox,
  renderPastInput,
  resetRecallStateForTesting,
  runInteractive,
  visibleLength,
} from '../src/interactive.js'
import { buildSuggestionLines, stripAnsi } from '../src/renderer.js'
const tick = () => new Promise<void>((r) => setImmediate(r))

let useManyCommandsForScrollTests = false
const {
  mockRecallNext,
  mockAnswerQuiz,
  mockAnswerSpelling,
  mockMarkAsRecalled,
  mockContestAndRegenerate,
} = vi.hoisted(() => ({
  mockRecallNext: vi.fn(),
  mockAnswerQuiz: vi.fn(),
  mockAnswerSpelling: vi.fn(),
  mockMarkAsRecalled: vi.fn(),
  mockContestAndRegenerate: vi.fn(),
}))
vi.mock('../src/recall.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/recall.js')>()
  return {
    ...actual,
    recallNext: mockRecallNext,
    answerQuiz: mockAnswerQuiz,
    answerSpelling: mockAnswerSpelling,
    markAsRecalled: mockMarkAsRecalled,
    contestAndRegenerate: mockContestAndRegenerate,
  }
})
vi.mock('../src/help.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/help.js')>()
  const manyCommands = Array.from({ length: 12 }, (_, i) => ({
    name: `cmd${i}`,
    usage: `/cmd${i}`,
    description: `Command ${i}`,
    category: 'interactive' as const,
  }))
  return {
    ...actual,
    filterCommandsByPrefix: (_: unknown, prefix: string) => {
      if (useManyCommandsForScrollTests && prefix.startsWith('/'))
        return manyCommands
      return actual.filterCommandsByPrefix(actual.interactiveDocs, prefix)
    },
  }
})

function createMockStdin(input: string): NodeJS.ReadableStream {
  const stream = new Readable({
    read() {
      /* no-op */
    },
  })
  stream.push(input)
  stream.push(null)
  return Object.assign(stream, { isTTY: false })
}

function createMockTTYStdin() {
  const stream = new Readable({
    read() {
      /* no-op */
    },
  }) as Readable & {
    push: (chunk: string) => void
  }
  return Object.assign(stream, {
    isTTY: true,
    setRawMode: () => {
      /* no-op */
    },
    resume: () => {
      /* no-op */
    },
    setEncoding: () => {
      /* no-op */
    },
  })
}

type TTYStdin = ReturnType<typeof createMockTTYStdin>

function typeString(stdin: TTYStdin, str: string) {
  for (const ch of str) {
    stdin.emit('keypress', ch, {
      name: ch === ' ' ? 'space' : ch === '/' ? undefined : ch,
      ctrl: false,
      meta: false,
    })
  }
}

function pressEnter(stdin: TTYStdin) {
  stdin.emit('keypress', '\r', {
    name: 'return',
    shift: false,
    ctrl: false,
    meta: false,
  })
}

function pressKey(
  stdin: TTYStdin,
  name: string,
  extra: Record<string, unknown> = {}
) {
  stdin.emit('keypress', undefined, {
    name,
    ctrl: false,
    meta: false,
    ...extra,
  })
}

async function submitTTYCommand(stdin: TTYStdin, command: string) {
  typeString(stdin, `${command} `)
  await tick()
  pressEnter(stdin)
  await tick()
}

function ttyOutput(writeSpy: ReturnType<typeof vi.spyOn>) {
  return writeSpy.mock.calls.map((c) => c[0]).join('')
}

function stripAllAnsi(str: string): string {
  const esc = String.fromCharCode(0x1b)
  return str
    .replace(new RegExp(`${esc}\\[[0-9;]*m`, 'g'), '')
    .replace(new RegExp(`${esc}\\[[0-9;]*[A-Za-z]`, 'g'), '')
    .replace(/\r/g, '')
}

const TOP_BORDER_PATTERN = /^┌─*┐$/

function countTopBorderLinesBeforeFirstInputBox(output: string): number {
  const normalized = stripAllAnsi(output)
  const lines = normalized.split('\n')
  let searchStart = 0
  for (let i = 0; i < lines.length; i++) {
    if (/doughnut \d+\.\d+\.\d+/.test(lines[i] ?? '')) {
      searchStart = i + 1
      break
    }
  }
  let count = 0
  for (let i = searchStart; i < lines.length; i++) {
    const line = (lines[i] ?? '').trim()
    if (line.includes('│')) {
      break
    }
    if (TOP_BORDER_PATTERN.test(line)) {
      count++
    }
  }
  return count
}

/** Simulates terminal overwrite: cursor-up causes subsequent writes to overwrite previous content. */
function simulateTerminalOverwrite(output: string): string {
  const lines: string[] = []
  let row = 0
  let col = 0
  let i = 0
  const ESC = '\x1b'
  const cursorUpRe = new RegExp(`^${ESC}\\[(\\d+)A`)
  const eraseLineRe = new RegExp(`^${ESC}\\[2K`)
  const cursorColRe = new RegExp(`^${ESC}\\[(\\d+)G`)
  const ansiRe = new RegExp(`^${ESC}\\[[0-9;]*[A-Za-z]`)
  while (i < output.length) {
    if (output.startsWith('\x1b[', i)) {
      const cursorUpMatch = output.slice(i).match(cursorUpRe)
      const eraseLineMatch = output.slice(i).match(eraseLineRe)
      const cursorColMatch = output.slice(i).match(cursorColRe)
      if (cursorUpMatch) {
        row = Math.max(0, row - Number(cursorUpMatch[1]))
        i += cursorUpMatch[0].length
        continue
      }
      if (eraseLineMatch) {
        while (lines.length <= row) lines.push('')
        lines[row] = ''
        col = 0
        i += eraseLineMatch[0].length
        continue
      }
      if (cursorColMatch) {
        col = Number(cursorColMatch[1]) - 1
        i += cursorColMatch[0].length
        continue
      }
      const ansiMatch = output.slice(i).match(ansiRe)
      if (ansiMatch) {
        i += ansiMatch[0].length
        continue
      }
    }
    if (output[i] === '\r') {
      col = 0
      i++
      continue
    }
    if (output[i] === '\n') {
      row++
      col = 0
      i++
      continue
    }
    while (lines.length <= row) lines.push('')
    const line = lines[row] ?? ''
    const newLine = line.slice(0, col) + output[i] + line.slice(col + 1)
    lines[row] = newLine
    col++
    i++
  }
  return lines.join('\n')
}

function makeTempConfigDir(tokens: Array<{ label: string; token: string }>) {
  const configDir = mkdtempSync(join(tmpdir(), 'doughnut-test-'))
  writeFileSync(
    join(configDir, 'access-tokens.json'),
    JSON.stringify({ tokens })
  )
  return configDir
}

function withConfigDir(configDir: string): () => void {
  const original = process.env.DOUGHNUT_CONFIG_DIR
  process.env.DOUGHNUT_CONFIG_DIR = configDir
  return () => {
    if (original === undefined) delete process.env.DOUGHNUT_CONFIG_DIR
    else process.env.DOUGHNUT_CONFIG_DIR = original
  }
}

describe('processInput', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    mockRecallNext.mockClear()
    mockMarkAsRecalled.mockClear()
    mockAnswerQuiz.mockClear()
    mockAnswerSpelling.mockClear()
    mockContestAndRegenerate.mockClear()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    logSpy.mockRestore()
  })

  test('returns true for exit commands', async () => {
    expect(await processInput('exit')).toBe(true)
    expect(await processInput('  exit  ')).toBe(true)
    expect(await processInput('/exit')).toBe(true)
    expect(await processInput('  /exit  ')).toBe(true)
  })

  test('returns false and does not log for empty input', async () => {
    expect(await processInput('')).toBe(false)
    expect(await processInput('   ')).toBe(false)
    expect(logSpy).not.toHaveBeenCalled()
  })

  test('returns false and logs "Not supported" for any other input', async () => {
    expect(await processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })

  test('returns false and logs help for /help', async () => {
    expect(await processInput('/help')).toBe(false)
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    expect(output).toContain('exit')
    expect(output).not.toContain('Not supported')
  })

  test('returns false and writes clear-screen sequence and prompt box for /clear', async () => {
    const writeSpy = vi
      .spyOn(process.stdout, 'write')
      .mockImplementation(() => true)
    expect(await processInput('/clear')).toBe(false)
    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('\x1b[H\x1b[2J')
    expect(output).toContain('doughnut')
    expect(output).toContain('┌')
    expect(output).toContain('┘')
    expect(output).toContain('→')
    writeSpy.mockRestore()
  })

  test('returns false and shows usage for /add-access-token without token', async () => {
    expect(await processInput('/add-access-token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
    logSpy.mockClear()
    expect(await processInput('/add-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
  })

  test('returns false and shows tokens with default marker for /list-access-token', async () => {
    const configDir = makeTempConfigDir([
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ])
    const restore = withConfigDir(configDir)
    expect(await processInput('/list-access-token')).toBe(false)
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('★ Token A')
    expect(output).toContain('  Token B')
    restore()
  })

  test('returns false and removes token for /remove-access-token with label', async () => {
    const configDir = makeTempConfigDir([
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ])
    const restore = withConfigDir(configDir)
    expect(await processInput('/remove-access-token Token A')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Token A" removed.')
    restore()
  })

  test('returns false and shows not found for /remove-access-token with unknown label', async () => {
    expect(await processInput('/remove-access-token Unknown')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Unknown" not found.')
  })

  test('returns false and shows usage for /remove-access-token without label', async () => {
    expect(await processInput('/remove-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /remove-access-token <label>')
  })

  test('returns false and shows usage for /remove-access-token-completely without label', async () => {
    expect(await processInput('/remove-access-token-completely ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'Usage: /remove-access-token-completely <label>'
    )
  })

  test('returns false and shows usage for /create-access-token without label', async () => {
    expect(await processInput('/create-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /create-access-token <label>')
  })

  test('returns false and shows error for /create-access-token with no default token', async () => {
    const configDir = makeTempConfigDir([])
    const restore = withConfigDir(configDir)
    expect(await processInput('/create-access-token My New Token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No default access token. Add one first with /add-access-token.'
    )
    restore()
  })

  test('returns false and logs error for /last email when no account configured', async () => {
    const configDir = mkdtempSync(`${tmpdir()}/doughnut-test-`)
    const restore = withConfigDir(configDir)
    expect(await processInput('/last email')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No Gmail account configured. Run /add gmail first.'
    )
    restore()
  })

  test('spelling: prompt with markdown stem shows ANSI codes, not raw markdown', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: '**bold** and *italic*',
    })

    await processInput('/recall')
    const spellCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes('Spell:')
    )
    expect(spellCall).toBeDefined()
    const output = spellCall![0] as string
    expect(output).toContain('\x1b[')
    expect(output).not.toContain('**')
    expect(output).not.toContain('*italic*')
    expect(stripAnsi(output)).toContain('bold')
    expect(stripAnsi(output)).toContain('italic')
  })

  test('spelling: /recall shows Spell prompt, then spelling answer calls answerSpelling', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'means incite violence',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: true })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Spell: means incite violence')
    )
    logSpy.mockClear()

    await processInput('sedition')
    expect(mockAnswerSpelling).toHaveBeenCalledWith(
      100,
      'sedition',
      expect.any(Number)
    )
    expect(mockAnswerSpelling.mock.calls[0]![2]).toBeGreaterThanOrEqual(0)
    expect(logSpy).toHaveBeenCalledWith('Correct!')
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
  })

  test('spelling: passes thinkingTimeMs from prompt display to answer', async () => {
    vi.useFakeTimers()
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'means incite violence',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: true })

    await processInput('/recall')
    vi.advanceTimersByTime(5000)
    await processInput('sedition')

    expect(mockAnswerSpelling).toHaveBeenCalledWith(100, 'sedition', 5000)
    vi.useRealTimers()
  })

  test('spelling: empty input prompts to type', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: '...',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: false })

    await processInput('/recall')
    logSpy.mockClear()
    expect(await processInput('')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Please type your spelling')
    )
    await processInput('clear') // clear pending state for subsequent tests
  })

  test('/recall with no notes prompts load more from next 3 days', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    expect(mockRecallNext).toHaveBeenCalledWith(0)
  })

  test('/recall load more: user says n, shows 0 notes to recall today', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })

    await processInput('/recall')
    logSpy.mockClear()
    await processInput('n')
    expect(logSpy).toHaveBeenCalledWith('0 notes to recall today')
    expect(mockRecallNext).toHaveBeenCalledTimes(1)
  })

  test('/recall load more: user says y, fetches with dueindays 3 and continues', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
      .mockResolvedValueOnce({
        type: 'just-review',
        memoryTrackerId: 1,
        title: 'Future note',
      })
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockMarkAsRecalled.mockResolvedValue(undefined)

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    logSpy.mockClear()

    await processInput('y')
    expect(mockRecallNext).toHaveBeenNthCalledWith(1, 0)
    expect(mockRecallNext).toHaveBeenNthCalledWith(2, 3)
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('Future note'))
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Yes, I remember? (y/n)')
    )
    logSpy.mockClear()

    await processInput('y')
    expect(mockMarkAsRecalled).toHaveBeenCalledWith(1, true)
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
    expect(logSpy).toHaveBeenCalledWith('Recalled 1 note')
  })

  test('/recall session: one note, answer y, shows Recalled 1 note', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'just-review',
        memoryTrackerId: 1,
        title: 'Note 1',
      })
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockMarkAsRecalled.mockResolvedValue(undefined)

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('Note 1'))
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Yes, I remember? (y/n)')
    )
    logSpy.mockClear()

    await processInput('y')
    expect(mockMarkAsRecalled).toHaveBeenCalledWith(1, true)
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    logSpy.mockClear()

    await processInput('n')
    expect(logSpy).toHaveBeenCalledWith('Recalled 1 note')
  })

  test('/recall session: error in continueRecallSession clears mode and logs', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'just-review',
        memoryTrackerId: 1,
        title: 'Note 1',
      })
      .mockRejectedValueOnce(new Error('Network error'))
    mockMarkAsRecalled.mockResolvedValue(undefined)

    await processInput('/recall')
    await processInput('y')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Network error')
    )
  })

  test('/stop when in recall substate exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)

    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Stopped recall')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('/stop when in recall with pending load more exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)

    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Stopped recall')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('/stop when not in recall substate shows Not supported', async () => {
    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('in recall state top-level commands are not available', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)

    await processInput('/help')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )
    expect(logSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('Subcommands')
    )

    logSpy.mockClear()
    await processInput('/recall-status')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )
    expect(logSpy).not.toHaveBeenCalledWith(
      expect.stringMatching(/notes to recall today/)
    )
  })

  test('MCQ: prompt with markdown stem shows ANSI codes', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'What is **2+2**?',
      choices: ['4', '3', '5'],
    })

    await processInput('/recall')
    const stemCall = logSpy.mock.calls.find(
      (c) =>
        typeof c[0] === 'string' &&
        (c[0] as string).includes('2+2') &&
        !(c[0] as string).includes('  1.')
    )
    expect(stemCall).toBeDefined()
    const output = stemCall![0] as string
    expect(output).toContain('\x1b[')
    expect(output).not.toContain('**')
    expect(stripAnsi(output)).toContain('2+2')
  })

  test('MCQ: prompt with markdown choices shows ANSI codes', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'Pick one',
      choices: ['*A*', '**B**', '`C`'],
    })

    await processInput('/recall')
    const choiceCalls = logSpy.mock.calls.filter(
      (c) =>
        typeof c[0] === 'string' &&
        ((c[0] as string).includes('  1.') ||
          (c[0] as string).includes('  2.') ||
          (c[0] as string).includes('  3.'))
    )
    expect(choiceCalls.length).toBeGreaterThanOrEqual(3)
    const allChoices = choiceCalls.map((c) => c[0]).join(' ')
    expect(allChoices).toContain('\x1b[')
    expect(allChoices).not.toContain('*A*')
    expect(allChoices).not.toContain('**B**')
    expect(allChoices).not.toContain('`C`')
    expect(stripAnsi(allChoices)).toContain('A')
    expect(stripAnsi(allChoices)).toContain('B')
    expect(stripAnsi(allChoices)).toContain('C')
  })

  test('MCQ: passes thinkingTimeMs from prompt display to answer', async () => {
    vi.useFakeTimers()
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'What is 2+2?',
      choices: ['4', '3', '5'],
    })
    mockAnswerQuiz.mockResolvedValue({ correct: true })

    await processInput('/recall')
    vi.advanceTimersByTime(3000)
    await processInput('1')

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0, 3000)
    vi.useRealTimers()
  })

  test('/contest when in recall with MCQ contests and shows new question', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'mcq',
        recallPromptId: 100,
        stem: 'First question?',
        choices: ['A', 'B', 'C'],
      })
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockContestAndRegenerate.mockResolvedValue({
      ok: true,
      result: {
        type: 'mcq',
        recallPromptId: 200,
        stem: 'Regenerated question?',
        choices: ['X', 'Y', 'Z'],
      },
    })
    mockAnswerQuiz.mockResolvedValue({ correct: true })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('First question?')
    )
    logSpy.mockClear()

    await processInput('/contest')
    expect(mockContestAndRegenerate).toHaveBeenCalledWith(100)
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Regenerated question?')
    )
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('  1. X'))
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('  2. Y'))
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('  3. Z'))
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Enter your choice (1-3):')
    )
    logSpy.mockClear()

    await processInput('1')
    expect(mockAnswerQuiz).toHaveBeenCalledWith(200, 0, expect.any(Number))
    expect(mockAnswerQuiz.mock.calls[0]![2]).toBeGreaterThanOrEqual(0)
    expect(logSpy).toHaveBeenCalledWith('Correct!')
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
  })

  test('/contest when no question pending shows /stop hint', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })

    await processInput('/recall')
    await processInput('/contest')
    expect(mockContestAndRegenerate).not.toHaveBeenCalled()
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )
  })

  test('/contest when contest fails shows error message', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'Q?',
      choices: ['A', 'B'],
    })
    mockContestAndRegenerate.mockResolvedValue({
      ok: false,
      message: 'Question could not be regenerated',
    })

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)
    await processInput('/contest')
    expect(logSpy).toHaveBeenCalledWith('Question could not be regenerated')
    expect(isInRecallSubstate()).toBe(true)
  })
})

describe('visibleLength', () => {
  test('returns length without ANSI codes', () => {
    expect(visibleLength('hello')).toBe(5)
    expect(visibleLength('\x1b[90mhello\x1b[0m')).toBe(5)
    expect(visibleLength('→ \x1b[90m`exit` to quit.\x1b[0m')).toBe(17)
  })
})

describe('renderBox', () => {
  test('box top border matches the given width', () => {
    const result = renderBox(['hi'], 100)
    const top = result.split('\n')[0]
    expect(top.length).toBe(100)
  })

  test('box content row matches the given width', () => {
    const result = renderBox(['hi'], 120)
    const row = result.split('\n')[1]
    expect(visibleLength(row)).toBe(120)
  })

  test('renders a single-line box', () => {
    const result = renderBox(['hello'], 20)
    const lines = result.split('\n')
    expect(lines).toHaveLength(3)
    expect(lines[0]).toBe('┌──────────────────┐')
    expect(lines[1]).toContain('hello')
    expect(lines[1]).toMatch(/^│.*│$/)
    expect(lines[2]).toBe('└──────────────────┘')
  })

  test('renders a multi-line box (box expands with newlines)', () => {
    const result = renderBox(['line 1', 'line 2', 'line 3'], 20)
    const lines = result.split('\n')
    expect(lines).toHaveLength(5)
    expect(lines[0]).toBe('┌──────────────────┐')
    expect(lines[1]).toContain('line 1')
    expect(lines[2]).toContain('line 2')
    expect(lines[3]).toContain('line 3')
    expect(lines[4]).toBe('└──────────────────┘')
  })

  test('pads short lines to fill the box width', () => {
    const result = renderBox(['hi'], 20)
    const lines = result.split('\n')
    expect(lines[1]).toBe('│ hi               │')
    expect(lines[1].length).toBe(20)
  })

  test('pads correctly when line contains ANSI codes', () => {
    const grey = '\x1b[90m'
    const reset = '\x1b[0m'
    const result = renderBox([`${grey}hi${reset}`], 20)
    const lines = result.split('\n')
    expect(visibleLength(lines[1])).toBe(20)
    expect(lines[1]).toContain('hi')
  })
})

describe('highlightRecognizedCommand', () => {
  const boldCyan = '\x1b[1;36m'
  const reset = '\x1b[0m'

  test('returns plain text when line does not start with /', () => {
    expect(highlightRecognizedCommand('hello')).toBe('hello')
  })

  test('does not highlight incomplete prefix', () => {
    expect(highlightRecognizedCommand('/he')).toBe('/he')
  })

  test('highlights exact command match', () => {
    const result = highlightRecognizedCommand('/help')
    expect(result).toStrictEqual(`${boldCyan}/help${reset}`)
  })

  test('highlights only command part when param follows', () => {
    const result = highlightRecognizedCommand('/add-access-token x')
    expect(result).toStrictEqual(`${boldCyan}/add-access-token${reset} x`)
  })

  test('returns plain when no match', () => {
    expect(highlightRecognizedCommand('/unknown')).toBe('/unknown')
  })

  test('does not highlight lone slash', () => {
    expect(highlightRecognizedCommand('/')).toBe('/')
  })
})

describe('buildBoxLines', () => {
  const boldCyan = '\x1b[1;36m'
  const reset = '\x1b[0m'

  test('empty buffer shows placeholder with prompt', () => {
    const lines = buildBoxLines('', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test('single-line buffer shows prompt + text', () => {
    const lines = buildBoxLines('hello', 40)
    expect(lines).toHaveLength(1)
    expect(lines[0]).toBe('→ hello')
  })

  test('multi-line buffer produces one line per newline', () => {
    const lines = buildBoxLines('line1\nline2\nline3', 40)
    expect(lines).toHaveLength(3)
    expect(lines[0]).toBe('→ line1')
    expect(lines[1]).toBe('  line2')
    expect(lines[2]).toBe('  line3')
  })

  test('recognized command gets bold+colored in first line', () => {
    const lines = buildBoxLines('/help', 40)
    expect(lines[0]).toContain('→')
    expect(lines[0]).toContain(`${boldCyan}/help${reset}`)
  })

  test('non-command line has no ANSI highlight', () => {
    const lines = buildBoxLines('hello', 40)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: checking ANSI codes
    expect(lines[0]).not.toMatch(/\x1b\[1;36m/)
  })

  test('command with param highlights only command part', () => {
    const lines = buildBoxLines('/add-access-token mylabel', 40)
    expect(lines[0]).toContain(`${boldCyan}/add-access-token${reset} mylabel`)
  })

  test('empty buffer in selection mode shows placeholder without arrow', () => {
    const lines = buildBoxLines('', 80, { placeholderContext: 'tokenList' })
    expect(lines).toHaveLength(1)
    expect(lines[0]).not.toContain('→')
    expect(lines[0]).toContain('↑↓ Enter to select; other keys cancel')
  })

  test('empty buffer with default context shows default placeholder', () => {
    const lines = buildBoxLines('', 80, { placeholderContext: 'default' })
    expect(lines[0]).toContain('`exit` to quit.')
  })

  test.each([
    ['recallMcq', '↑↓ Enter or number to select; Esc to cancel'],
    ['recallStopConfirmation', 'y or n; Esc to go back'],
    ['recallYesNo', 'y or n; /stop to exit recall'],
    ['recallSpelling', 'type your answer; /stop to exit recall'],
  ] as const)('placeholderContext %s shows correct placeholder', (ctx, phrase) => {
    const lines = buildBoxLines('', 80, { placeholderContext: ctx })
    expect(lines[0]).toContain(phrase)
  })

  test('placeholder truncates in narrow window for any long context', () => {
    const width = 25
    const lines = buildBoxLines('', width, { placeholderContext: 'tokenList' })
    const box = renderBox(lines, width)
    const boxLines = box.split('\n')
    for (let i = 1; i < boxLines.length - 1; i++) {
      expect(visibleLength(boxLines[i])).toBeLessThanOrEqual(width)
    }
    expect(lines[0]).toContain('↑↓')
    expect(boxLines[1]).toContain('...')
  })
})

describe('buildSuggestionLines', () => {
  test('with /rec and narrow width (40), each line has visible length ≤ width or ends with "..."', () => {
    const lines = buildSuggestionLines('/rec', 0, 40)
    expect(lines.length).toBeGreaterThan(0)
    for (const line of lines) {
      expect(visibleLength(line)).toBeLessThanOrEqual(40)
      if (visibleLength(line) === 40) {
        expect(line).toContain('...')
      }
    }
  })

  test('with /rec and wide width (120), no truncation', () => {
    const lines = buildSuggestionLines('/rec', 0, 120)
    expect(lines.length).toBeGreaterThan(0)
    expect(lines.some((l) => l.endsWith('...'))).toBe(false)
  })

  test('with /recall-status prefix, narrow width truncates long descriptions', () => {
    const lines = buildSuggestionLines('/recall-status', 0, 30)
    expect(lines.length).toBeGreaterThan(0)
    for (const line of lines) {
      expect(visibleLength(line)).toBeLessThanOrEqual(30)
    }
  })

  test('without slash prefix returns / commands hint, truncated when narrow', () => {
    const lines = buildSuggestionLines('hello', 0, 5)
    expect(lines).toHaveLength(1)
    expect(visibleLength(lines[0])).toBeLessThanOrEqual(5)
    expect(lines[0]).toContain('...')
  })

  test('with slash prefix but no match returns empty', () => {
    const lines = buildSuggestionLines('/unknown', 0, 80)
    expect(lines).toHaveLength(0)
  })

  test('Current guidance lines with ANSI end with RESET (no state bleed)', () => {
    const widths = [25, 30] as const
    const buffers = ['/list', '/']
    for (const buffer of buffers) {
      for (const width of widths) {
        const lines = buildSuggestionLines(buffer, 0, width)
        for (const line of lines) {
          if (line.includes('\x1b')) {
            // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI RESET escape, intentional
            expect(line).toMatch(/\x1b\[0m$/)
          }
        }
      }
    }
  })
})

describe('renderPastInput', () => {
  test('renders text in grey background with no border', () => {
    const result = renderPastInput('hello', 30)
    expect(result).not.toContain('┌')
    expect(result).not.toContain('│')
    expect(result).toContain('hello')
    expect(result).toContain('\x1b[48;5;236m')
  })

  test('has empty-line vertical padding inside the box', () => {
    const result = renderPastInput('hello', 30)
    const lines = result.split('\n')
    expect(visibleLength(lines[0])).toBe(28)
    // biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
    expect(lines[0].replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
    const lastBgLine = lines[lines.length - 2]
    // biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
    expect(lastBgLine.replace(/\x1b\[[0-9;]*m/g, '').trim()).toBe('')
  })

  test('has trailing blank line margin', () => {
    const result = renderPastInput('hello', 30)
    const lines = result.split('\n')
    expect(lines[lines.length - 1]).toBe('')
  })

  test('does not include prompt arrow', () => {
    const result = renderPastInput('hello', 30)
    expect(result).not.toContain('→')
  })

  test('handles multi-line input', () => {
    const result = renderPastInput('line1\nline2', 30)
    expect(result).toContain('line1')
    expect(result).toContain('line2')
    const lines = result.split('\n')
    const bgLines = lines.filter((l) => l.includes('\x1b[48;5;236m'))
    expect(bgLines).toHaveLength(4)
  })
})

describe('interactive CLI (e2e style)', () => {
  let logSpy: ReturnType<typeof vi.spyOn>
  let exitSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    exitSpy = vi
      .spyOn(process, 'exit')
      .mockImplementation((() => undefined) as unknown as typeof process.exit)
  })

  afterEach(() => {
    logSpy.mockRestore()
    exitSpy.mockRestore()
  })

  test('responds "Not supported" to any input', async () => {
    const stdin = createMockStdin('hello\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('shows past input in grey background box', async () => {
    const stdin = createMockStdin('hello\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    const pastInputCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes('\x1b[48;5;236m')
    )
    expect(pastInputCall).toBeDefined()
    expect(pastInputCall![0]).toContain('hello')
    expect(pastInputCall![0]).not.toContain('→')
  })

  test('exit command exits the CLI', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('/exit command exits the CLI', async () => {
    const stdin = createMockStdin('/exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('each line triggers separate response', async () => {
    const stdin = createMockStdin('line1\nline2\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    const notSupportedCalls = logSpy.mock.calls.filter(
      (c) => c[0] === 'Not supported'
    )
    expect(notSupportedCalls).toHaveLength(2)
  })

  test('box uses full terminal width in piped mode', async () => {
    Object.defineProperty(process.stdout, 'columns', {
      value: 100,
      writable: true,
      configurable: true,
    })
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    const boxCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes('┌')
    )
    expect(boxCall).toBeDefined()
    const topBorder = boxCall![0].split('\n')[0]
    expect(topBorder.length).toBe(100)
  })

  test('shows version, box with placeholder and prompt', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('doughnut')
    expect(output).toContain('→')
    expect(output).toContain('`exit` to quit.')
    expect(output).toContain('┌')
    expect(output).toContain('┘')
  })

  test('shows "  / commands" in the Current guidance when user has not typed /', async () => {
    const stdin = createMockStdin('exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('  / commands')
    expect(output).toContain('\x1b[90m')
  })
})

describe('TTY mode slash command suggestions', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let _logSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    _logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
    stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
  })

  afterEach(() => {
    pressKey(stdin, 'c', { ctrl: true })
    vi.restoreAllMocks()
  })

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
    expect(output).not.toContain('/help                List available commands')
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

  test('up at first wraps to last', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    pressKey(stdin, 'up')
    await tick()

    const output = ttyOutput(writeSpy)
    const lines = output.split('\n')
    const recallLines = lines.filter((l) => l.includes('/recall'))
    const recallLine = recallLines[recallLines.length - 1]
    expect(recallLine).toBeDefined()
    expect(recallLine).toContain('\x1b[7m')
  })

  test('down at last wraps to first', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 9; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    const lines = output.split('\n')
    const helpLine = lines.find((l) => l.includes('/help'))
    expect(helpLine).toBeDefined()
    expect(helpLine).toContain('\x1b[7m')
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

  test('Enter inserts highlighted command', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 9; i++) {
      pressKey(stdin, 'down')
      await tick()
    }
    pressEnter(stdin)
    await tick()

    expect(stripAnsi(ttyOutput(writeSpy))).toContain('→ /last email ')
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
    const visualOutput = simulateTerminalOverwrite(output)
    const boxTopLines = visualOutput
      .split('\n')
      .filter((l) => TOP_BORDER_PATTERN.test(stripAllAnsi(l).trim()))
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
    expect(countTopBorderLinesBeforeFirstInputBox(output)).toBe(1)
  })
})

describe('TTY mode resize', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
    stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
  })

  afterEach(() => {
    pressKey(stdin, 'c', { ctrl: true })
    vi.restoreAllMocks()
  })

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

describe('TTY mode slash command suggestions with scroll', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    useManyCommandsForScrollTests = true
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
    stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
  })

  afterEach(() => {
    useManyCommandsForScrollTests = false
    pressKey(stdin, 'c', { ctrl: true })
    vi.restoreAllMocks()
  })

  test('shows "↑ more above" when scrolled down', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 8; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    expect(ttyOutput(writeSpy)).toContain('↑ more above')
  })

  test('hides "↓ more below" when at bottom', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 11; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    const lastMoreBelow = output.lastIndexOf('↓ more below')
    const lastCmd11 = output.lastIndexOf('/cmd11')
    expect(lastCmd11).toBeGreaterThan(lastMoreBelow)
  })
})

describe('TTY token list interactive mode', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin
  let restoreConfigDir: () => void

  beforeEach(async () => {
    resetRecallStateForTesting()
    const configDir = makeTempConfigDir([
      { label: 'Alpha', token: 'a' },
      { label: 'Beta', token: 'b' },
      { label: 'Gamma', token: 'c' },
    ])
    restoreConfigDir = withConfigDir(configDir)
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
    stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
  })

  afterEach(() => {
    restoreConfigDir()
    pressKey(stdin, 'c', { ctrl: true })
    vi.restoreAllMocks()
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

    const { wrapTextToLines } = await import('../src/renderer.js')
    const promptText = 'Select and enter to change the default access token'
    const width = process.stdout.columns ?? 80
    const wrappedPromptLines = wrapTextToLines(promptText, width)
    const currentPromptLines = 1 + wrappedPromptLines.length
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
    const visualOutput = simulateTerminalOverwrite(output)
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
    const visualOutput = simulateTerminalOverwrite(output)
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

    const { getDefaultTokenLabel } = await import('../src/accessToken.js')
    expect(getDefaultTokenLabel()).toBe('Beta')
  })

  test('in selection mode, input box borders are all gray including right', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    const output = ttyOutput(writeSpy)
    const boxSection = output.slice(output.indexOf('Select and enter'))
    const boxLines = boxSection
      .split('\n')
      .filter((l) => l.includes('┌') || l.includes('│') || l.includes('└'))
    for (const line of boxLines) {
      // biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI escape codes for terminal output assertion
      const resetBeforeRightBorder = /\x1b\[0m\s*│/.test(line)
      expect(resetBeforeRightBorder).toBe(false)
    }
  })

  test('in selection mode, input box has no arrow prompt', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    const output = ttyOutput(writeSpy)
    const tokenListBoxSection = output.slice(
      output.lastIndexOf('Select and enter')
    )
    expect(tokenListBoxSection).not.toContain('→')
  })

  test('in token list mode, cursor is hidden', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    expect(ttyOutput(writeSpy)).toContain('\x1b[?25l')
  })

  test('in token list mode, input box is grayed out', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    expect(ttyOutput(writeSpy)).toContain('\x1b[90m┌')
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

    const { listAccessTokens } = await import('../src/accessToken.js')
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
  ])('token list $action has no two consecutive blank lines after result', async ({
    setup,
    expectMsg,
  }) => {
    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()
    await setup()

    const rawOutput = ttyOutput(writeSpy)
    const visualOutput = simulateTerminalOverwrite(rawOutput)
    const lines = stripAllAnsi(visualOutput).split('\n')

    const resultLineIdx = lines.findIndex((l) => l.includes(expectMsg))
    expect(
      resultLineIdx,
      `Result "${expectMsg}" not found`
    ).toBeGreaterThanOrEqual(0)

    const afterResult = lines.slice(resultLineIdx + 1)
    const maxConsecutiveBlanks = (() => {
      let max = 0
      let curr = 0
      for (const l of afterResult) {
        curr = l.trim() ? 0 : curr + 1
        max = Math.max(max, curr)
      }
      return max
    })()
    expect(
      maxConsecutiveBlanks,
      `Expected no two consecutive blank lines after result. Max: ${maxConsecutiveBlanks}. Lines: ${JSON.stringify(afterResult.slice(0, 10))}`
    ).toBeLessThan(2)
  })

  test('/remove-access-token shows token list and Enter removes selected', async () => {
    await submitTTYCommand(stdin, '/remove-access-token')

    const midOutput = ttyOutput(writeSpy)
    expect(midOutput).toContain('Alpha')
    expect(midOutput).toContain('Beta')

    writeSpy.mockClear()
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Token "Alpha" removed.')

    const { listAccessTokens } = await import('../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(2)
    expect(remaining.map((t) => t.label)).not.toContain('Alpha')
  })
})

describe('TTY MCQ choice selection', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'What is 2+2?',
      choices: ['4', '3', '5'],
    })
    mockAnswerQuiz.mockResolvedValue({ correct: true })
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
    stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
  })

  afterEach(() => {
    pressKey(stdin, 'c', { ctrl: true })
    vi.restoreAllMocks()
  })

  test('down arrow moves highlight to second choice', async () => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/recall')

    const afterSubmit = ttyOutput(writeSpy)
    expect(afterSubmit).toContain('↑↓ Enter or number to select; Esc to cancel')
    expect(afterSubmit).toContain('  1. 4')
    expect(afterSubmit).toContain('  2. 3')

    writeSpy.mockClear()
    pressKey(stdin, 'down')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('\x1b[7m') // REVERSE on highlighted line
    expect(output).toContain('  2. 3')
  })

  test('Enter submits highlighted choice and calls answerQuiz', async () => {
    await submitTTYCommand(stdin, '/recall')

    pressEnter(stdin)
    await tick()
    await new Promise((r) => setTimeout(r, 50))

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0, expect.any(Number))
    expect(ttyOutput(writeSpy)).toContain('Recalled successfully')
  })

  test('typed number still works', async () => {
    await submitTTYCommand(stdin, '/recall')

    typeString(stdin, '2')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 1, expect.any(Number)) // "2" = choiceIndex 1
  })

  test('ESC shows stop confirmation, y exits recall mode', async () => {
    await submitTTYCommand(stdin, '/recall')
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stop recall? (y/n)')
    expect(ttyOutput(writeSpy)).toContain('y or n; Esc to go back')

    typeString(stdin, 'y')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stopped recall')
    expect(mockAnswerQuiz).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(false)
  })

  test('ESC then n cancels confirmation and stays in MCQ', async () => {
    await submitTTYCommand(stdin, '/recall')
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    typeString(stdin, 'n')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(isInRecallSubstate()).toBe(true)
    expect(mockAnswerQuiz).not.toHaveBeenCalled()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('  1. 4')
    expect(output).toContain('  2. 3')
  })
})

describe('TTY recall substates ESC (spelling, y/n, load-more)', () => {
  let stdin: TTYStdin
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    resetRecallStateForTesting()
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
    stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await tick()
  })

  afterEach(() => {
    pressKey(stdin, 'c', { ctrl: true })
    vi.restoreAllMocks()
  })

  test('ESC in spelling prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'test',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: true })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain(
      'type your answer; /stop to exit recall'
    )

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
    expect(mockAnswerSpelling).not.toHaveBeenCalled()
  })

  test('ESC in Yes I remember y/n prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 42,
      title: 'Test note',
    })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
  })

  test('ESC in Load more y/n prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    expect(ttyOutput(writeSpy)).toContain('y or n; /stop to exit recall')

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
  })
})
