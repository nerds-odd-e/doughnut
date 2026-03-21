import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  CLI_COMMAND_HISTORY_FILENAME,
  cliCommandHistoryPath,
  loadCliCommandHistory,
  saveCliCommandHistory,
} from '../src/cliCommandHistoryFile.js'
import { MAX_COMMITTED_COMMANDS } from '../src/interactiveCommandInput.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-cmd-hist-'))
}

describe('cli command history file', () => {
  let configDir: string

  beforeEach(() => {
    configDir = createTempDir()
  })

  afterEach(() => {
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('load: missing file returns empty array', () => {
    expect(loadCliCommandHistory(configDir)).toEqual([])
  })

  test('load: invalid JSON returns empty array', () => {
    fs.writeFileSync(cliCommandHistoryPath(configDir), 'not json', 'utf-8')
    expect(loadCliCommandHistory(configDir)).toEqual([])
  })

  test('load: non-array JSON returns empty array', () => {
    fs.writeFileSync(
      cliCommandHistoryPath(configDir),
      JSON.stringify({ x: 1 }),
      'utf-8'
    )
    expect(loadCliCommandHistory(configDir)).toEqual([])
  })

  test('load: keeps only string entries in order', () => {
    fs.writeFileSync(
      cliCommandHistoryPath(configDir),
      JSON.stringify(['a', 1, 'b', null, 'c']),
      'utf-8'
    )
    expect(loadCliCommandHistory(configDir)).toEqual(['a', 'b', 'c'])
  })

  test('load: truncates to MAX_COMMITTED_COMMANDS', () => {
    const many = Array.from({ length: MAX_COMMITTED_COMMANDS + 5 }, (_, i) =>
      String(i)
    )
    fs.writeFileSync(
      cliCommandHistoryPath(configDir),
      JSON.stringify(many),
      'utf-8'
    )
    expect(loadCliCommandHistory(configDir)).toHaveLength(
      MAX_COMMITTED_COMMANDS
    )
    expect(loadCliCommandHistory(configDir)[0]).toBe('0')
  })

  test('save creates config dir and round-trips', () => {
    const nested = path.join(configDir, 'nested', 'cfg')
    const lines = ['newest', 'older']
    saveCliCommandHistory(nested, lines)
    expect(loadCliCommandHistory(nested)).toEqual(lines)
    expect(fs.existsSync(path.join(nested, CLI_COMMAND_HISTORY_FILENAME))).toBe(
      true
    )
  })

  test('save then load matches committedCommands order (newest first)', () => {
    saveCliCommandHistory(configDir, ['/help', '/version'])
    expect(loadCliCommandHistory(configDir)).toEqual(['/help', '/version'])
  })
})
