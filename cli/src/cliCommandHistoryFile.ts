import * as fs from 'node:fs'
import * as path from 'node:path'
import { MAX_COMMITTED_COMMANDS } from './interactiveCommandInput.js'

export const CLI_COMMAND_HISTORY_FILENAME = 'cli-command-history.json'

export function cliCommandHistoryPath(configDir: string): string {
  return path.join(configDir, CLI_COMMAND_HISTORY_FILENAME)
}

function normalizeLoadedHistory(raw: unknown): string[] {
  if (!Array.isArray(raw)) return []
  const strings = raw.filter((x): x is string => typeof x === 'string')
  return strings.slice(0, MAX_COMMITTED_COMMANDS)
}

export function loadCliCommandHistory(configDir: string): string[] {
  const p = cliCommandHistoryPath(configDir)
  try {
    const data = fs.readFileSync(p, 'utf-8')
    return normalizeLoadedHistory(JSON.parse(data) as unknown)
  } catch {
    return []
  }
}

export function saveCliCommandHistory(
  configDir: string,
  committedCommands: readonly string[]
): void {
  const p = cliCommandHistoryPath(configDir)
  fs.mkdirSync(path.dirname(p), { recursive: true })
  fs.writeFileSync(p, `${JSON.stringify([...committedCommands])}\n`, 'utf-8')
}
