/**
 * Parses CLI stdout into sections for E2E assertions.
 * Sections: history-input (past user input), history-output (command output),
 * current-prompt (prompt hints between separator and input box),
 * current-guidance (all content below the input box: hints, options, MCQ, etc.).
 */

const HISTORY_INPUT_BG = '\x1b[48;5;236m'
const CURRENT_PROMPT_SEPARATOR_GREEN = '\x1b[32m'
const CURRENT_PROMPT_HINT = '\x1b[90m'
const INPUT_BOX_TOP = '┌'
const INPUT_BOX_BOTTOM = '└'
// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g

function stripAnsi(str: string): string {
  return str.replace(ANSI_PATTERN, '')
}

function isHistoryInputLine(line: string): boolean {
  return line.includes(HISTORY_INPUT_BG)
}

function isCurrentPromptSeparatorLine(line: string): boolean {
  return line.includes(CURRENT_PROMPT_SEPARATOR_GREEN) && line.includes('─')
}

function isCurrentPromptHintLine(line: string): boolean {
  return line.includes(CURRENT_PROMPT_HINT) && !line.includes(HISTORY_INPUT_BG)
}

function findLastSeparatorIndex(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (isCurrentPromptSeparatorLine(lines[i]!)) return i
  }
  return -1
}

function findLastInputBoxStart(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i]?.trimStart().startsWith(INPUT_BOX_TOP)) return i
  }
  return lines.length
}

function findLastInputBoxEnd(lines: string[]): number {
  const boxStart = findLastInputBoxStart(lines)
  if (boxStart >= lines.length) return -1
  for (let i = boxStart + 1; i < lines.length; i++) {
    if (lines[i]?.includes(INPUT_BOX_BOTTOM)) return i
  }
  return -1
}

type Boundaries = {
  historyLines: string[]
  currentPromptLines: string[]
  currentGuidanceLines: string[]
  outputLinesIncludingPromptBlock: string[]
}

function parseCliOutput(output: string): Boundaries {
  const lines = output.split('\n')
  const separatorIdx = findLastSeparatorIndex(lines)
  const boxStart = findLastInputBoxStart(lines)
  const boxEnd = findLastInputBoxEnd(lines)
  const historyEnd = separatorIdx >= 0 ? separatorIdx : lines.length
  const historyLines = lines.slice(0, historyEnd)
  const currentPromptLines =
    separatorIdx >= 0 ? lines.slice(separatorIdx + 1, boxStart) : []
  const currentGuidanceLines = boxEnd >= 0 ? lines.slice(boxEnd + 1) : []
  const outputLinesIncludingPromptBlock = [
    ...historyLines,
    ...currentPromptLines.filter((l) => !isCurrentPromptHintLine(l)),
  ]
  return {
    historyLines,
    currentPromptLines,
    currentGuidanceLines,
    outputLinesIncludingPromptBlock,
  }
}

export type Section =
  | 'history-input'
  | 'history-output'
  | 'current-prompt'
  | 'current-guidance'

function collectSectionLines(
  boundaries: Boundaries,
  section: Section,
  strip: boolean
): string[] {
  const { historyLines, currentPromptLines, currentGuidanceLines } = boundaries
  const result: string[] = []

  if (section === 'current-guidance') {
    for (const line of currentGuidanceLines) {
      const content = strip ? stripAnsi(line) : line
      if (content.trim()) result.push(content)
    }
    return result
  }

  if (section === 'history-input') {
    for (const line of historyLines) {
      if (isHistoryInputLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) result.push(content)
      }
    }
  } else if (section === 'history-output') {
    for (const line of historyLines) {
      if (!isHistoryInputLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) result.push(content)
      }
    }
    for (const line of currentPromptLines) {
      if (!isCurrentPromptHintLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) result.push(content)
      }
    }
  } else if (section === 'current-prompt') {
    for (const line of currentPromptLines) {
      if (isCurrentPromptHintLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) result.push(content)
      }
    }
  }
  return result
}

export function getSectionContent(output: string, section: Section): string {
  const boundaries = parseCliOutput(output)
  return collectSectionLines(boundaries, section, true).join('\n')
}

export function getSectionContentRaw(output: string, section: Section): string {
  const boundaries = parseCliOutput(output)
  return collectSectionLines(boundaries, section, false).join('\n')
}

export function getLastCommandOutput(output: string): string {
  const { outputLinesIncludingPromptBlock } = parseCliOutput(output)
  const blocks: string[] = []
  let current: string[] = []

  for (const line of outputLinesIncludingPromptBlock) {
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

export function getRecallDisplaySections(output: string): {
  currentPromptAndHistory: string
  historyOutput: string
} {
  const currentPrompt = getSectionContent(output, 'current-prompt')
  const historyOutput = getSectionContent(output, 'history-output')
  return {
    currentPromptAndHistory: `${currentPrompt}\n${historyOutput}`,
    historyOutput,
  }
}
