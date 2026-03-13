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

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI for assertions
const stripAnsi = (s: string) => s.replace(/\x1b\[[0-9;]*m/g, '')
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
    expect(mockAnswerSpelling).toHaveBeenCalledWith(100, 'sedition')
    expect(logSpy).toHaveBeenCalledWith('Correct!')
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
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
    expect(mockAnswerQuiz).toHaveBeenCalledWith(200, 0)
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

  test('shows grey hint "  / commands" below input box when user has not typed /', async () => {
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
  let logSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
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

  test('initial display shows grey hint "  / commands" below input box', () => {
    const output = ttyOutput(writeSpy)
    expect(output).toContain('  / commands')
    expect(output).toContain('\x1b[90m')
  })

  test('typing non-slash keeps hint instead of command list', async () => {
    writeSpy.mockClear()
    typeString(stdin, 'h')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('  / commands')
    expect(output).not.toContain('/help                List available commands')
  })

  test('typing "/" shows command suggestions below input box', async () => {
    typeString(stdin, '/')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('/help')
    expect(output).toContain('List available commands')
    expect(output).toContain('/add gmail')
    expect(output).toContain('Add Gmail account')
    expect(output).toContain('/create-access-token')
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
      (l) => l.includes('/add gmail') && !l.includes('\x1b[7m')
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

    expect(logSpy).toHaveBeenCalledWith('Not supported')
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
    for (let i = 0; i < 8; i++) {
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

  test('shows token list with default highlighted after /list-access-token', async () => {
    writeSpy.mockClear()
    await submitTTYCommand(stdin, '/list-access-token')

    const output = ttyOutput(writeSpy)
    expect(output).toContain('Alpha')
    expect(output).toContain('Beta')
    expect(output).toContain('Gamma')
    expect(output).toContain('★')
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

  test('any other key exits token list mode', async () => {
    await submitTTYCommand(stdin, '/list-access-token')
    writeSpy.mockClear()

    typeString(stdin, 'q')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')
  })

  test('ESC cancels token list selection without modifying tokens', async () => {
    await submitTTYCommand(stdin, '/remove-access-token')
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    const output = ttyOutput(writeSpy)
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')
    expect(output).not.toContain('Token "Alpha" removed')

    const { listAccessTokens } = await import('../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(3)
    expect(remaining.map((t) => t.label)).toContain('Alpha')
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

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0)
    expect(ttyOutput(writeSpy)).toContain('Recalled successfully')
  })

  test('typed number still works', async () => {
    await submitTTYCommand(stdin, '/recall')

    typeString(stdin, '2')
    await tick()
    pressEnter(stdin)
    await tick()

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 1) // "2" = choiceIndex 1
  })

  test('ESC shows stop confirmation, y exits recall mode', async () => {
    await submitTTYCommand(stdin, '/recall')
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    pressKey(stdin, 'escape')
    await tick()

    expect(ttyOutput(writeSpy)).toContain('Stop recall? (y/n)')

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

  beforeEach(async () => {
    resetRecallStateForTesting()
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
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
    await submitTTYCommand(stdin, '/recall')

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

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
    expect(mockMarkAsRecalled).not.toHaveBeenCalled()
  })

  test('ESC in Load more y/n prompt exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({ type: 'none', message: '0 notes' })
    await submitTTYCommand(stdin, '/recall')

    pressKey(stdin, 'escape')
    await tick()

    expect(isInRecallSubstate()).toBe(false)
  })
})
