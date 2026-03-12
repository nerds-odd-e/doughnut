import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  filterCommandsByPrefix,
  formatCommandSuggestions,
  formatHelp,
  type CommandDoc,
} from '../src/help.js'
import { processInput } from '../src/interactive.js'
import { run } from '../src/index.js'

describe('formatHelp', () => {
  test('includes all command names', () => {
    const output = formatHelp()
    expect(output).toContain('version')
    expect(output).toContain('update')
    expect(output).toContain('help')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    expect(output).toContain('/recall-status')
    expect(output).toContain('/recall')
    expect(output).toContain('exit')
  })

  test('includes descriptions for each command', () => {
    const output = formatHelp()
    expect(output).toContain('Show CLI version')
    expect(output).toContain('Update CLI to latest version')
    expect(output).toContain('List available commands')
    expect(output).toContain('Add Gmail account via OAuth')
    expect(output).toContain('Show subject of last email')
    expect(output).toContain('Show how many notes to recall today')
    expect(output).toContain('Recall all due notes in a session')
    expect(output).toContain('Quit the CLI')
  })

  test('has Subcommands and Interactive commands sections', () => {
    const output = formatHelp()
    expect(output).toContain('Subcommands:')
    expect(output).toContain('Interactive commands (in prompt):')
  })
})

describe('filterCommandsByPrefix', () => {
  const commands: CommandDoc[] = [
    {
      name: 'help',
      usage: '/help',
      description: 'Help',
      category: 'interactive',
    },
    {
      name: 'exit',
      usage: '/exit',
      description: 'Exit',
      category: 'interactive',
    },
    {
      name: 'recall-status',
      usage: '/recall-status',
      description: 'Recall status',
      category: 'interactive',
    },
    {
      name: 'recall',
      usage: '/recall',
      description: 'Recall',
      category: 'interactive',
    },
  ]

  test('matches from beginning', () => {
    const result = filterCommandsByPrefix(commands, '/recall')
    expect(result.map((c) => c.usage)).toEqual(['/recall-status', '/recall'])
  })

  test('matches anywhere with beginning prioritized', () => {
    const result = filterCommandsByPrefix(commands, 'recall')
    expect(result.map((c) => c.usage)).toEqual(['/recall-status', '/recall'])
  })

  test('prioritizes beginning match over substring match', () => {
    const cmds: CommandDoc[] = [
      {
        name: 'x',
        usage: 'get-help',
        description: 'X',
        category: 'subcommand',
      },
      { name: 'y', usage: 'help', description: 'Y', category: 'subcommand' },
    ]
    const result = filterCommandsByPrefix(cmds, 'help')
    expect(result.map((c) => c.usage)).toEqual(['help', 'get-help'])
  })

  test('empty prefix returns all commands', () => {
    const result = filterCommandsByPrefix(commands, '')
    expect(result).toHaveLength(4)
  })

  test('no match returns empty array', () => {
    const result = filterCommandsByPrefix(commands, 'xyz')
    expect(result).toHaveLength(0)
  })

  test('substring match returns matching commands', () => {
    const result = filterCommandsByPrefix(commands, 'help')
    expect(result.map((c) => c.usage)).toEqual(['/help'])
  })

  test('/e matches /help and /exit with /exit first', () => {
    const result = filterCommandsByPrefix(commands, '/e')
    expect(result.map((c) => c.usage)).toContain('/help')
    expect(result.map((c) => c.usage)).toContain('/exit')
    expect(result[0].usage).toBe('/exit')
  })
})

describe('formatCommandSuggestions', () => {
  test('returns all lines when ≤8 commands, no "↓ more below"', () => {
    const commands: CommandDoc[] = [
      { name: 'a', usage: '/a', description: 'A', category: 'interactive' },
      { name: 'b', usage: '/b', description: 'B', category: 'interactive' },
    ]
    const lines = formatCommandSuggestions(commands)
    expect(lines).toHaveLength(2)
    expect(lines[0]).toContain('/a')
    expect(lines[0]).toContain('A')
    expect(lines[1]).toContain('/b')
    expect(lines[1]).toContain('B')
    expect(lines.some((l) => l.includes('↓ more below'))).toBe(false)
  })

  test('returns 8 command lines plus "↓ more below" when 9 commands', () => {
    const commands: CommandDoc[] = Array.from({ length: 9 }, (_, i) => ({
      name: `cmd${i}`,
      usage: `/cmd${i}`,
      description: `Desc ${i}`,
      category: 'interactive' as const,
    }))
    const lines = formatCommandSuggestions(commands)
    expect(lines).toHaveLength(9)
    expect(lines.slice(0, 8).every((l, i) => l.includes(`/cmd${i}`))).toBe(true)
    expect(lines[8]).toBe('  ↓ more below')
  })
})

describe('processInput with /help', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    logSpy.mockRestore()
  })

  test('logs help output and returns false', async () => {
    const result = await processInput('/help')
    expect(result).toBe(false)
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    expect(output).toContain('exit')
    expect(output).toContain('update')
    expect(output).toContain('version')
  })

  test('does not log "Not supported"', async () => {
    await processInput('/help')
    const notSupportedCalls = logSpy.mock.calls.filter(
      (c) => c[0] === 'Not supported'
    )
    expect(notSupportedCalls).toHaveLength(0)
  })
})

describe('run with help subcommand', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    logSpy.mockRestore()
  })

  test('prints help and does not start interactive mode', async () => {
    await run(['help'])
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    expect(output).toContain('exit')
    expect(output).toContain('update')
    expect(output).toContain('version')
  })
})
