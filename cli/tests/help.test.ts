import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { formatHelp } from '../src/help.js'
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
    expect(output).toContain('exit')
  })

  test('includes descriptions for each command', () => {
    const output = formatHelp()
    expect(output).toContain('Show CLI version')
    expect(output).toContain('Update CLI to latest version')
    expect(output).toContain('Show this help')
    expect(output).toContain('Add Gmail account via OAuth')
    expect(output).toContain('Show subject of last email')
    expect(output).toContain('Quit the CLI')
  })

  test('has Subcommands and Interactive commands sections', () => {
    const output = formatHelp()
    expect(output).toContain('Subcommands:')
    expect(output).toContain('Interactive commands (in prompt):')
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
