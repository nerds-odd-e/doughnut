import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  USER_INPUT_HISTORY_FILENAME,
  userInputHistoryPath,
  loadUserInputHistory,
  saveUserInputHistory,
} from '../src/userInputHistoryFile.js'
import { MAX_USER_INPUT_HISTORY_LINES } from '../src/interactiveCommandInput.js'

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-user-input-hist-'))
}

describe('user input history file', () => {
  let configDir: string

  beforeEach(() => {
    configDir = createTempDir()
  })

  afterEach(() => {
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('load: missing file returns empty array', () => {
    expect(loadUserInputHistory(configDir)).toEqual([])
  })

  test('load: invalid JSON returns empty array', () => {
    fs.writeFileSync(userInputHistoryPath(configDir), 'not json', 'utf-8')
    expect(loadUserInputHistory(configDir)).toEqual([])
  })

  test('load: non-array JSON returns empty array', () => {
    fs.writeFileSync(
      userInputHistoryPath(configDir),
      JSON.stringify({ x: 1 }),
      'utf-8'
    )
    expect(loadUserInputHistory(configDir)).toEqual([])
  })

  test('load: keeps only string entries in order', () => {
    fs.writeFileSync(
      userInputHistoryPath(configDir),
      JSON.stringify(['a', 1, 'b', null, 'c']),
      'utf-8'
    )
    expect(loadUserInputHistory(configDir)).toEqual(['a', 'b', 'c'])
  })

  test('load: truncates to MAX_USER_INPUT_HISTORY_LINES', () => {
    const many = Array.from(
      { length: MAX_USER_INPUT_HISTORY_LINES + 5 },
      (_, i) => String(i)
    )
    fs.writeFileSync(
      userInputHistoryPath(configDir),
      JSON.stringify(many),
      'utf-8'
    )
    expect(loadUserInputHistory(configDir)).toHaveLength(
      MAX_USER_INPUT_HISTORY_LINES
    )
    expect(loadUserInputHistory(configDir)[0]).toBe('0')
  })

  test('save creates config dir and round-trips', () => {
    const nested = path.join(configDir, 'nested', 'cfg')
    const lines = ['newest', 'older']
    saveUserInputHistory(nested, lines)
    expect(loadUserInputHistory(nested)).toEqual(lines)
    expect(fs.existsSync(path.join(nested, USER_INPUT_HISTORY_FILENAME))).toBe(
      true
    )
  })

  test('save then load matches user input history order (newest first)', () => {
    saveUserInputHistory(configDir, ['/help', '/version'])
    expect(loadUserInputHistory(configDir)).toEqual(['/help', '/version'])
  })
})
