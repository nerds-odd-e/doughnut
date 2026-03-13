import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { Readable } from 'node:stream'
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
      // no-op: data is pushed manually
    },
  })
  stream.push(input)
  stream.push(null)
  return Object.assign(stream, { isTTY: false })
}

function createMockTTYStdin(): NodeJS.ReadableStream & {
  push: (chunk: string) => void
} {
  const stream = new Readable({
    read() {
      // no-op: data is pushed manually
    },
  }) as Readable & { push: (chunk: string) => void }
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

describe('processInput', () => {
  beforeEach(() => {
    resetRecallStateForTesting()
    mockRecallNext.mockClear()
    mockMarkAsRecalled.mockClear()
    mockAnswerQuiz.mockClear()
    mockAnswerSpelling.mockClear()
    mockContestAndRegenerate.mockClear()
  })

  test('returns true for exit', async () => {
    expect(await processInput('exit')).toBe(true)
    expect(await processInput('  exit  ')).toBe(true)
  })

  test('returns true for /exit', async () => {
    expect(await processInput('/exit')).toBe(true)
    expect(await processInput('  /exit  ')).toBe(true)
  })

  test('returns false and does not log for empty input', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('')).toBe(false)
    expect(await processInput('   ')).toBe(false)
    expect(logSpy).not.toHaveBeenCalled()
    logSpy.mockRestore()
  })

  test('returns false and logs "Not supported" for any other input', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    logSpy.mockRestore()
  })

  test('returns false and logs help for /help', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/help')).toBe(false)
    expect(logSpy).toHaveBeenCalled()
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    expect(output).toContain('exit')
    expect(output).not.toContain('Not supported')
    logSpy.mockRestore()
  })

  test('returns false and shows usage for /add-access-token without token', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/add-access-token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
    logSpy.mockRestore()
  })

  test('returns false and shows usage for /add-access-token with trailing space only', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/add-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
    logSpy.mockRestore()
  })

  test('returns false and shows tokens with default marker for /list-access-token', async () => {
    const fs = await import('node:fs')
    const os = await import('node:os')
    const path = await import('node:path')
    const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-test-'))
    const originalEnv = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    fs.writeFileSync(
      path.join(configDir, 'access-tokens.json'),
      JSON.stringify({
        tokens: [
          { label: 'Token A', token: 'a' },
          { label: 'Token B', token: 'b' },
        ],
      })
    )
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/list-access-token')).toBe(false)
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('★ Token A')
    expect(output).toContain('  Token B')
    logSpy.mockRestore()
    process.env.DOUGHNUT_CONFIG_DIR = originalEnv
  })

  test('returns false and removes token for /remove-access-token with label', async () => {
    const fs = await import('node:fs')
    const os = await import('node:os')
    const path = await import('node:path')
    const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-test-'))
    const originalEnv = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    fs.writeFileSync(
      path.join(configDir, 'access-tokens.json'),
      JSON.stringify({
        tokens: [
          { label: 'Token A', token: 'a' },
          { label: 'Token B', token: 'b' },
        ],
      })
    )
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/remove-access-token Token A')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Token A" removed.')
    logSpy.mockRestore()
    process.env.DOUGHNUT_CONFIG_DIR = originalEnv
  })

  test('returns false and shows not found for /remove-access-token with unknown label', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/remove-access-token Unknown')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Unknown" not found.')
    logSpy.mockRestore()
  })

  test('returns false and shows usage for /remove-access-token without label', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/remove-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /remove-access-token <label>')
    logSpy.mockRestore()
  })

  test('returns false and shows usage for /remove-access-token-completely without label', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/remove-access-token-completely ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'Usage: /remove-access-token-completely <label>'
    )
    logSpy.mockRestore()
  })

  test('returns false and shows usage for /create-access-token without label', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/create-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /create-access-token <label>')
    logSpy.mockRestore()
  })

  test('returns false and shows error for /create-access-token with no default token', async () => {
    const fs = await import('node:fs')
    const os = await import('node:os')
    const path = await import('node:path')
    const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-test-'))
    const originalEnv = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/create-access-token My New Token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No default access token. Add one first with /add-access-token.'
    )
    logSpy.mockRestore()
    process.env.DOUGHNUT_CONFIG_DIR = originalEnv
  })

  test('returns false and logs error for /last email when no account configured', async () => {
    const configDir = (await import('node:fs')).mkdtempSync(
      `${(await import('node:os')).tmpdir()}/doughnut-test-`
    )
    const originalEnv = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(await processInput('/last email')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No Gmail account configured. Run /add gmail first.'
    )
    logSpy.mockRestore()
    process.env.DOUGHNUT_CONFIG_DIR = originalEnv
  })

  test('spelling: /recall shows Spell prompt, then spelling answer calls answerSpelling', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'means incite violence',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: true })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Spell: means incite violence')
    )
    logSpy.mockClear()

    await processInput('sedition')
    expect(mockAnswerSpelling).toHaveBeenCalledWith(100, 'sedition')
    expect(logSpy).toHaveBeenCalledWith('Correct!')
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')

    logSpy.mockRestore()
  })

  test('spelling: empty input prompts to type', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: '...',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: false })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    logSpy.mockClear()
    expect(await processInput('')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Please type your spelling')
    )
    await processInput('clear') // clear pending state for subsequent tests
    logSpy.mockRestore()
  })

  test('/recall with no notes prompts load more from next 3 days', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    expect(mockRecallNext).toHaveBeenCalledWith(0)

    logSpy.mockRestore()
  })

  test('/recall load more: user says n, shows 0 notes to recall today', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    logSpy.mockClear()
    await processInput('n')
    expect(logSpy).toHaveBeenCalledWith('0 notes to recall today')
    expect(mockRecallNext).toHaveBeenCalledTimes(1)

    logSpy.mockRestore()
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
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

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

    logSpy.mockRestore()
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
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

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

    logSpy.mockRestore()
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
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    await processInput('y')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Network error')
    )

    logSpy.mockRestore()
  })

  test('/stop when in recall substate exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)

    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Stopped recall')
    expect(isInRecallSubstate()).toBe(false)

    logSpy.mockRestore()
  })

  test('/stop when in recall with pending load more exits recall mode', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)

    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Stopped recall')
    expect(isInRecallSubstate()).toBe(false)

    logSpy.mockRestore()
  })

  test('/stop when not in recall substate shows Not supported', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(isInRecallSubstate()).toBe(false)

    logSpy.mockRestore()
  })

  test('in recall state top-level commands are not available', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

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

    logSpy.mockRestore()
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
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

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

    logSpy.mockRestore()
  })

  test('/contest when no question pending shows /stop hint', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    await processInput('/contest')
    expect(mockContestAndRegenerate).not.toHaveBeenCalled()
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )

    logSpy.mockRestore()
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
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)
    await processInput('/contest')
    expect(logSpy).toHaveBeenCalledWith('Question could not be regenerated')
    expect(isInRecallSubstate()).toBe(true)

    logSpy.mockRestore()
  })
})

describe('visibleLength', () => {
  test('returns length without ANSI codes', () => {
    expect(visibleLength('hello')).toBe(5)
    expect(visibleLength('\x1b[90mhello\x1b[0m')).toBe(5)
    expect(visibleLength('→ \x1b[90m`exit` to quit.\x1b[0m')).toBe(17)
  })
})

describe('renderBox uses full width', () => {
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
})

describe('renderBox', () => {
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
    await new Promise((r) => setImmediate(r))
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('shows past input in grey background box', async () => {
    const stdin = createMockStdin('hello\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
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
    await new Promise((r) => setImmediate(r))
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('/exit command exits the CLI', async () => {
    const stdin = createMockStdin('/exit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    expect(exitSpy).toHaveBeenCalledWith(0)
  })

  test('each line triggers separate response', async () => {
    const stdin = createMockStdin('line1\nline2\nexit\n')
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
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
    await new Promise((r) => setImmediate(r))
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
    await new Promise((r) => setImmediate(r))
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
    await new Promise((r) => setImmediate(r))
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('  / commands')
    expect(output).toContain('\x1b[90m')
  })
})

describe('TTY mode slash command suggestions', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  test('initial display shows grey hint "  / commands" below input box', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('  / commands')
    expect(output).toContain('\x1b[90m')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('typing non-slash keeps hint instead of command list', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', 'h', { name: 'h', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('  / commands')
    expect(output).not.toContain('/help                List available commands')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('typing "/" shows command suggestions below input box', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('/help')
    expect(output).toContain('List available commands')
    expect(output).toContain('/add gmail')
    expect(output).toContain('Add Gmail account')
    expect(output).toContain('/create-access-token')
    expect(output).toContain('↓ more below')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('first candidate is highlighted with reverse video', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    const lines = output.split('\n')
    const suggestionStart = lines.findIndex((l) => l.includes('/help'))
    expect(suggestionStart).toBeGreaterThanOrEqual(0)
    expect(lines[suggestionStart]).toContain('\x1b[7m')
    const laterSuggestion = lines.findIndex(
      (l) => l.includes('/add gmail') && !l.includes('\x1b[7m')
    )
    expect(laterSuggestion).toBeGreaterThan(suggestionStart)

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('Enter inserts highlighted command with space', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', 'x', { name: 'x', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setTimeout(r, 50))

    expect(logSpy).toHaveBeenCalledWith('Not supported')
    logSpy.mockRestore()
    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('prefix filtering shows only matching commands', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    stdin.emit('keypress', 'a', { name: 'a', ctrl: false, meta: false })
    stdin.emit('keypress', 'd', { name: 'd', ctrl: false, meta: false })
    stdin.emit('keypress', 'd', { name: 'd', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('/add gmail')
    const plain = stripAnsi(output)
    const lastDrawStart = plain.lastIndexOf('→ /add')
    expect(lastDrawStart).toBeGreaterThanOrEqual(0)
    expect(plain.slice(lastDrawStart)).not.toContain('/help')
    expect(plain.slice(lastDrawStart)).not.toContain('/last email')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('Enter with prefix inserts first matching command', async () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    stdin.emit('keypress', 'a', { name: 'a', ctrl: false, meta: false })
    stdin.emit('keypress', 'd', { name: 'd', ctrl: false, meta: false })
    stdin.emit('keypress', 'd', { name: 'd', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setTimeout(r, 50))

    expect(writeSpy.mock.calls.map((c) => c[0]).join('')).toContain(
      '/add gmail '
    )
    logSpy.mockRestore()
    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('no suggestions after space when command inserted', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
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

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('up at first wraps to last', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\x1b[A', { name: 'up', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    const lines = output.split('\n')
    const recallLines = lines.filter((l) => l.includes('/recall'))
    const recallLine = recallLines[recallLines.length - 1]
    expect(recallLine).toBeDefined()
    expect(recallLine).toContain('\x1b[7m')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('down at last wraps to first', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    for (let i = 0; i < 9; i++) {
      stdin.emit('keypress', undefined, {
        name: 'down',
        ctrl: false,
        meta: false,
      })
      await new Promise((r) => setImmediate(r))
    }

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    const lines = output.split('\n')
    const helpLine = lines.find((l) => l.includes('/help'))
    expect(helpLine).toBeDefined()
    expect(helpLine).toContain('\x1b[7m')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('ESC when buffer is only "/" dismisses suggestions and clears buffer', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()

    stdin.emit('keypress', undefined, {
      name: 'escape',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('  / commands')
    expect(output).not.toContain('/help')
    expect(output).toContain('`exit` to quit.')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('ESC when partial command "/ex" hides suggestions but keeps buffer', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    stdin.emit('keypress', 'e', { name: 'e', ctrl: false, meta: false })
    stdin.emit('keypress', 'x', { name: 'x', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()

    stdin.emit('keypress', undefined, {
      name: 'escape',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    const plain = stripAnsi(output)
    expect(plain).toContain('→ /ex')
    expect(output).toContain('  / commands')
    expect(output).not.toContain('/exit')
    expect(output).not.toContain('/help')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('Enter inserts highlighted command', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    for (let i = 0; i < 8; i++) {
      stdin.emit('keypress', undefined, {
        name: 'down',
        ctrl: false,
        meta: false,
      })
      await new Promise((r) => setImmediate(r))
    }
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(stripAnsi(output)).toContain('→ /last email ')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })
})

describe('TTY mode slash command suggestions with scroll', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    useManyCommandsForScrollTests = true
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
  })

  afterEach(() => {
    useManyCommandsForScrollTests = false
    vi.restoreAllMocks()
  })

  test('shows "↑ more above" when scrolled down', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    for (let i = 0; i < 8; i++) {
      stdin.emit('keypress', undefined, {
        name: 'down',
        ctrl: false,
        meta: false,
      })
      await new Promise((r) => setImmediate(r))
    }

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('↑ more above')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('hides "↓ more below" when at bottom', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '/', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    for (let i = 0; i < 11; i++) {
      stdin.emit('keypress', undefined, {
        name: 'down',
        ctrl: false,
        meta: false,
      })
      await new Promise((r) => setImmediate(r))
    }

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    const lastMoreBelow = output.lastIndexOf('↓ more below')
    const lastCmd11 = output.lastIndexOf('/cmd11')
    expect(lastCmd11).toBeGreaterThan(lastMoreBelow)

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })
})

describe('TTY token list interactive mode', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let originalConfigDir: string | undefined

  beforeEach(async () => {
    resetRecallStateForTesting()
    const fs = await import('node:fs')
    const os = await import('node:os')
    const path = await import('node:path')
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    const configDir = fs.mkdtempSync(
      path.join(os.tmpdir(), 'doughnut-tty-test-')
    )
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    fs.writeFileSync(
      path.join(configDir, 'access-tokens.json'),
      JSON.stringify({
        tokens: [
          { label: 'Alpha', token: 'a' },
          { label: 'Beta', token: 'b' },
          { label: 'Gamma', token: 'c' },
        ],
      })
    )
    vi.spyOn(console, 'log').mockImplementation(() => undefined)
    writeSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
    vi.spyOn(process, 'exit').mockImplementation(
      (() => undefined) as unknown as typeof process.exit
    )
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
    vi.restoreAllMocks()
  })

  async function submitListCommand(
    stdin: ReturnType<typeof createMockTTYStdin>
  ) {
    for (const ch of '/list-access-token ') {
      stdin.emit('keypress', ch, {
        name: ch === ' ' ? 'space' : undefined,
        ctrl: false,
        meta: false,
      })
    }
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
  }

  test('shows token list with default highlighted after /list-access-token', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()

    await submitListCommand(stdin)

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('Alpha')
    expect(output).toContain('Beta')
    expect(output).toContain('Gamma')
    expect(output).toContain('★')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('Enter sets highlighted token as default and confirms', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitListCommand(stdin)

    stdin.emit('keypress', undefined, {
      name: 'down',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('Default token set to: Beta')

    const { getDefaultTokenLabel } = await import('../src/accessToken.js')
    expect(getDefaultTokenLabel()).toBe('Beta')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('any other key exits token list mode', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitListCommand(stdin)
    writeSpy.mockClear()

    stdin.emit('keypress', 'q', { name: 'q', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('ESC cancels token list selection without modifying tokens', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitRemoveCommand(stdin, '/remove-access-token')
    writeSpy.mockClear()

    stdin.emit('keypress', undefined, {
      name: 'escape',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('/ commands')
    expect(output).not.toContain('Alpha')
    expect(output).not.toContain('Token "Alpha" removed')

    const { listAccessTokens } = await import('../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(3)
    expect(remaining.map((t) => t.label)).toContain('Alpha')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  async function submitRemoveCommand(
    stdin: ReturnType<typeof createMockTTYStdin>,
    command: string
  ) {
    for (const ch of `${command} `) {
      stdin.emit('keypress', ch, {
        name: ch === ' ' ? 'space' : undefined,
        ctrl: false,
        meta: false,
      })
    }
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
  }

  test('/remove-access-token shows token list and Enter removes selected', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitRemoveCommand(stdin, '/remove-access-token')

    const midOutput = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(midOutput).toContain('Alpha')
    expect(midOutput).toContain('Beta')

    writeSpy.mockClear()
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('Token "Alpha" removed.')

    const { listAccessTokens } = await import('../src/accessToken.js')
    const remaining = listAccessTokens()
    expect(remaining).toHaveLength(2)
    expect(remaining.map((t) => t.label)).not.toContain('Alpha')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })
})

describe('TTY MCQ choice selection', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
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
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function submitRecall(stdin: ReturnType<typeof createMockTTYStdin>) {
    for (const ch of '/recall ') {
      stdin.emit('keypress', ch, {
        name: ch === ' ' ? 'space' : undefined,
        ctrl: false,
        meta: false,
      })
    }
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
  }

  test('down arrow moves highlight to second choice', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))
    writeSpy.mockClear()

    await submitRecall(stdin)

    const afterSubmit = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(afterSubmit).toContain('  1. 4')
    expect(afterSubmit).toContain('  2. 3')

    writeSpy.mockClear()
    stdin.emit('keypress', undefined, {
      name: 'down',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('\x1b[7m') // REVERSE on highlighted line
    expect(output).toContain('  2. 3')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('Enter submits highlighted choice and calls answerQuiz', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitRecall(stdin)

    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))
    await new Promise((r) => setTimeout(r, 50))

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0)
    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('Recalled successfully')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('typed number still works', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitRecall(stdin)

    stdin.emit('keypress', '2', { name: undefined, ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 1) // "2" = choiceIndex 1

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('ESC shows stop confirmation, y exits recall mode', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitRecall(stdin)
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    stdin.emit('keypress', undefined, {
      name: 'escape',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    let output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('Stop recall? (y/n)')

    stdin.emit('keypress', 'y', { name: 'y', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('Stopped recall')
    expect(mockAnswerQuiz).not.toHaveBeenCalled()
    expect(isInRecallSubstate()).toBe(false)

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })

  test('ESC then n cancels confirmation and stays in MCQ', async () => {
    const stdin = createMockTTYStdin()
    runInteractive(stdin as NodeJS.ReadableStream)
    await new Promise((r) => setImmediate(r))

    await submitRecall(stdin)
    mockAnswerQuiz.mockClear()
    writeSpy.mockClear()

    stdin.emit('keypress', undefined, {
      name: 'escape',
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    stdin.emit('keypress', 'n', { name: 'n', ctrl: false, meta: false })
    await new Promise((r) => setImmediate(r))
    stdin.emit('keypress', '\r', {
      name: 'return',
      shift: false,
      ctrl: false,
      meta: false,
    })
    await new Promise((r) => setImmediate(r))

    expect(isInRecallSubstate()).toBe(true)
    expect(mockAnswerQuiz).not.toHaveBeenCalled()

    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('  1. 4')
    expect(output).toContain('  2. 3')

    stdin.emit('keypress', '\x03', { name: 'c', ctrl: true, meta: false })
  })
})
