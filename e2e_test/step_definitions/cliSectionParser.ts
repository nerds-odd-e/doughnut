/**
 * Parses CLI stdout into sections for E2E assertions.
 * Sections: history-input (grey bg), history-output, current-prompt (separator-delimited).
 */

const GREY_BG = '\x1b[48;5;236m'
const GREEN = '\x1b[32m'
const DIM = '\x1b[90m'
// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g

function stripAnsi(str: string): string {
  return str.replace(ANSI_PATTERN, '')
}

function isHistoryInputLine(line: string): boolean {
  return line.includes(GREY_BG)
}

function isCurrentPromptSeparator(line: string): boolean {
  return line.includes(GREEN) && line.includes('─')
}

function isCurrentPromptContentLine(line: string): boolean {
  return line.includes(DIM) && !line.includes(GREY_BG)
}

function findLastSeparatorIndex(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (isCurrentPromptSeparator(lines[i]!)) return i
  }
  return -1
}

function findLastInputBoxStart(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i]?.trimStart().startsWith('┌')) return i
  }
  return lines.length
}

export type Section = 'history-input' | 'history-output' | 'current-prompt'

export function getSectionContent(output: string, section: Section): string {
  const lines = output.split('\n')
  const separatorIdx = findLastSeparatorIndex(lines)
  const boxStart = findLastInputBoxStart(lines)
  const historyEnd = separatorIdx >= 0 ? separatorIdx : lines.length
  const historyLines = lines.slice(0, historyEnd)
  const currentPromptLines =
    separatorIdx >= 0 ? lines.slice(separatorIdx + 1, boxStart) : []

  const result: string[] = []
  if (section === 'history-input') {
    for (const line of historyLines) {
      if (isHistoryInputLine(line)) {
        const stripped = stripAnsi(line)
        if (stripped.trim()) result.push(stripped)
      }
    }
  } else if (section === 'history-output') {
    for (const line of historyLines) {
      if (!isHistoryInputLine(line)) {
        const stripped = stripAnsi(line)
        if (stripped.trim()) result.push(stripped)
      }
    }
    for (const line of currentPromptLines) {
      if (!isCurrentPromptContentLine(line)) {
        const stripped = stripAnsi(line)
        if (stripped.trim()) result.push(stripped)
      }
    }
  } else if (section === 'current-prompt') {
    for (const line of currentPromptLines) {
      if (isCurrentPromptContentLine(line)) {
        const stripped = stripAnsi(line)
        if (stripped.trim()) result.push(stripped)
      }
    }
  }
  return result.join('\n')
}

export function getSectionContentRaw(output: string, section: Section): string {
  const lines = output.split('\n')
  const separatorIdx = findLastSeparatorIndex(lines)
  const boxStart = findLastInputBoxStart(lines)
  const historyEnd = separatorIdx >= 0 ? separatorIdx : lines.length
  const historyLines = lines.slice(0, historyEnd)
  const currentPromptLines =
    separatorIdx >= 0 ? lines.slice(separatorIdx + 1, boxStart) : []

  const result: string[] = []
  if (section === 'history-input') {
    for (const line of historyLines) {
      if (isHistoryInputLine(line) && line.trim()) result.push(line)
    }
  } else if (section === 'history-output') {
    for (const line of historyLines) {
      if (!isHistoryInputLine(line) && line.trim()) result.push(line)
    }
    for (const line of currentPromptLines) {
      if (!isCurrentPromptContentLine(line) && line.trim()) result.push(line)
    }
  } else if (section === 'current-prompt') {
    for (const line of currentPromptLines) {
      if (isCurrentPromptContentLine(line) && line.trim()) result.push(line)
    }
  }
  return result.join('\n')
}

export function getLastCommandOutput(output: string): string {
  const lines = output.split('\n')
  const separatorIdx = findLastSeparatorIndex(lines)
  const boxStart = findLastInputBoxStart(lines)
  const historyEnd = separatorIdx >= 0 ? separatorIdx : lines.length
  const historyLines = lines.slice(0, historyEnd)
  const betweenSeparatorAndBox =
    separatorIdx >= 0 ? lines.slice(separatorIdx + 1, boxStart) : []
  const allOutputLines = [
    ...historyLines,
    ...betweenSeparatorAndBox.filter((l) => !isCurrentPromptContentLine(l)),
  ]

  const blocks: string[] = []
  let current: string[] = []
  for (const line of allOutputLines) {
    const stripped = stripAnsi(line)
    if (isHistoryInputLine(line)) {
      if (current.length > 0) {
        blocks.push(current.join('\n'))
        current = []
      }
    } else if (stripped.trim()) {
      current.push(stripped)
    }
  }
  if (current.length > 0) blocks.push(current.join('\n'))
  return blocks.length > 0 ? blocks[blocks.length - 1]! : ''
}
