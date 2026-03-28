import * as fs from 'node:fs'
import * as path from 'node:path'

const MAX_USER_INPUT_HISTORY_LINES = 100
export const USER_INPUT_HISTORY_FILENAME = 'user-input-history.json'

export function userInputHistoryPath(configDir: string): string {
  return path.join(configDir, USER_INPUT_HISTORY_FILENAME)
}

function normalizeLoadedHistory(raw: unknown): string[] {
  if (!Array.isArray(raw)) return []
  const strings = raw.filter((x): x is string => typeof x === 'string')
  return strings.slice(0, MAX_USER_INPUT_HISTORY_LINES)
}

export function loadUserInputHistory(configDir: string): string[] {
  const p = userInputHistoryPath(configDir)
  try {
    const data = fs.readFileSync(p, 'utf-8')
    return normalizeLoadedHistory(JSON.parse(data) as unknown)
  } catch {
    return []
  }
}

export function saveUserInputHistory(
  configDir: string,
  lines: readonly string[]
): void {
  const p = userInputHistoryPath(configDir)
  fs.mkdirSync(path.dirname(p), { recursive: true })
  fs.writeFileSync(p, `${JSON.stringify([...lines])}\n`, 'utf-8')
}
