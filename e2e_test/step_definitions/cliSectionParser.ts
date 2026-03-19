/**
 * Parses CLI stdout into domain sections for E2E assertions.
 *
 * Non-interactive output: entire stdout when running non-interactively, no parsing.
 *
 * Interactive-only sections:
 * - History input: Past user input lines
 * - History output: Past command results
 * - Current guidance: Prompts, hints, options for the current input (above input box:
 *   recall, MCQ, y/n; below: / commands, token list)
 */

// --- ANSI stripping ---
const HISTORY_INPUT_BG = '\x1b[48;5;236m'
const CURRENT_PROMPT_SEPARATOR_GREEN = '\x1b[32m'
const CURRENT_PROMPT_HINT = '\x1b[90m'
const INPUT_BOX_TOP = '┌'
const INPUT_BOX_BOTTOM = '└'
// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g
// CSI sequences like \x1b[2K (erase line), \x1b[3A (cursor up)
// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping CSI escapes
const CSI_PATTERN = /\x1b\[[0-9;]*[A-Za-z]/g

function stripAnsi(str: string): string {
  return str.replace(ANSI_PATTERN, '')
}

function stripAllAnsi(str: string): string {
  return str
    .replace(ANSI_PATTERN, '')
    .replace(CSI_PATTERN, '')
    .replace(/\r/g, '')
}

// --- Line classification ---
function isHistoryInputLine(line: string): boolean {
  return line.includes(HISTORY_INPUT_BG)
}

function isCurrentPromptSeparatorLine(line: string): boolean {
  return line.includes(CURRENT_PROMPT_SEPARATOR_GREEN) && line.includes('─')
}

function isCurrentPromptHintLine(line: string): boolean {
  return line.includes(CURRENT_PROMPT_HINT) && !line.includes(HISTORY_INPUT_BG)
}

// --- Input box boundaries ---
function findLastSeparatorIndex(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (isCurrentPromptSeparatorLine(lines[i]!)) {
      return i
    }
  }
  return -1
}

function findLastInputBoxStart(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i]?.includes(INPUT_BOX_TOP)) {
      return i
    }
  }
  return lines.length
}

function findLastInputBoxEnd(lines: string[]): number {
  const boxStart = findLastInputBoxStart(lines)
  if (boxStart >= lines.length) {
    return -1
  }
  for (let i = boxStart + 1; i < lines.length; i++) {
    if (lines[i]?.includes(INPUT_BOX_BOTTOM)) {
      return i
    }
  }
  return -1
}

// --- Section extraction ---
type Boundaries = {
  historyLines: string[]
  currentPromptLines: string[]
  currentGuidanceLines: string[]
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
  return {
    historyLines,
    currentPromptLines,
    currentGuidanceLines,
  }
}

type Section =
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
      if (content.trim()) {
        result.push(content)
      }
    }
    return result
  }

  if (section === 'history-input') {
    for (const line of historyLines) {
      if (isHistoryInputLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) {
          result.push(content)
        }
      }
    }
  } else if (section === 'history-output') {
    for (const line of historyLines) {
      if (!isHistoryInputLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) {
          result.push(content)
        }
      }
    }
    for (const line of currentPromptLines) {
      if (!isCurrentPromptHintLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) {
          result.push(content)
        }
      }
    }
  } else if (section === 'current-prompt') {
    for (const line of currentPromptLines) {
      if (isCurrentPromptHintLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) {
          result.push(content)
        }
      }
    }
  }
  return result
}

// --- Public API ---

function getSectionContent(output: string, section: Section): string {
  const boundaries = parseCliOutput(output)
  return collectSectionLines(boundaries, section, true).join('\n')
}

export function getHistoryOutputContent(output: string): string {
  return getSectionContent(output, 'history-output')
}

export function getHistoryInputContent(output: string): string {
  return getSectionContent(output, 'history-input')
}

export function getCurrentGuidanceDebug(output: string): {
  currentGuidanceContent: string
  inputBoxLineRange: { start: number; end: number }
  lineCount: number
  rawTail: string
} {
  const lines = output.split('\n')
  const boxStart = findLastInputBoxStart(lines)
  const boxEnd = findLastInputBoxEnd(lines)
  const { currentGuidanceAndHistory } = getRecallDisplaySections(output)
  return {
    currentGuidanceContent: stripAllAnsi(currentGuidanceAndHistory).trim(),
    inputBoxLineRange: { start: boxStart, end: boxEnd },
    lineCount: lines.length,
    rawTail: output.slice(-1200).replace(/\r/g, '\\r').replace(/\n/g, '\\n '),
  }
}

function getCurrentGuidanceCombined(
  boundaries: Boundaries,
  stripAnsi: boolean
): string {
  const parts = [
    collectSectionLines(boundaries, 'current-prompt', stripAnsi),
    collectSectionLines(boundaries, 'current-guidance', stripAnsi),
    collectSectionLines(boundaries, 'history-output', stripAnsi),
  ].map((lines) => lines.join('\n'))
  return `${parts[0]}\n${parts[1]}\n${parts[2]}`.trim()
}

export function getRecallDisplaySections(output: string): {
  currentGuidanceAndHistory: string
  historyOutput: string
} {
  const boundaries = parseCliOutput(output)
  const currentGuidanceAndHistory = getCurrentGuidanceCombined(boundaries, true)
  const historyOutput = collectSectionLines(
    boundaries,
    'history-output',
    true
  ).join('\n')
  return {
    currentGuidanceAndHistory,
    historyOutput,
  }
}

export function getCurrentGuidanceAndHistoryRaw(output: string): string {
  const boundaries = parseCliOutput(output)
  return getCurrentGuidanceCombined(boundaries, false)
}

const TOP_BORDER_PATTERN = /^┌─*┐$/

/** Simulates terminal overwrite: cursor/erase sequences alter displayed content. Used to detect visual bugs (e.g. double border). */
function simulateTerminalOverwrite(output: string): string {
  const lines: string[] = []
  let row = 0
  let col = 0
  let i = 0
  const ESC = '\x1b'
  const cursorUpRe = new RegExp(`^${ESC}\\[(\\d+)A`)
  const eraseLineRe = new RegExp(`^${ESC}\\[2K`)
  const cursorColRe = new RegExp(`^${ESC}\\[(\\d+)G`)
  const clearScreenRe = new RegExp(`^${ESC}\\[[02]?J`) // \x1b[J, \x1b[2J clear screen
  const cursorHomeRe = new RegExp(`^${ESC}\\[H`) // \x1b[H cursor home
  const ansiRe = new RegExp(`^${ESC}\\[[0-9;]*[A-Za-z]`)
  while (i < output.length) {
    if (output.startsWith('\x1b[', i)) {
      const cursorUpMatch = output.slice(i).match(cursorUpRe)
      const eraseLineMatch = output.slice(i).match(eraseLineRe)
      const cursorColMatch = output.slice(i).match(cursorColRe)
      const clearScreenMatch = output.slice(i).match(clearScreenRe)
      const cursorHomeMatch = output.slice(i).match(cursorHomeRe)
      if (cursorUpMatch) {
        row = Math.max(0, row - Number(cursorUpMatch[1]))
        i += cursorUpMatch[0].length
        continue
      }
      if (eraseLineMatch) {
        while (lines.length <= row) lines.push('')
        lines[row] = ''
        col = 0
        i += eraseLineMatch[0].length
        continue
      }
      if (cursorColMatch) {
        col = Number(cursorColMatch[1]) - 1
        i += cursorColMatch[0].length
        continue
      }
      if (clearScreenMatch) {
        lines.length = 0
        row = 0
        col = 0
        i += clearScreenMatch[0].length
        continue
      }
      if (cursorHomeMatch) {
        row = 0
        col = 0
        i += cursorHomeMatch[0].length
        continue
      }
      const ansiMatch = output.slice(i).match(ansiRe)
      if (ansiMatch) {
        i += ansiMatch[0].length
        continue
      }
    }
    if (output[i] === '\r') {
      col = 0
      i++
      continue
    }
    if (output[i] === '\n') {
      row++
      col = 0
      i++
      continue
    }
    while (lines.length <= row) lines.push('')
    const line = lines[row] ?? ''
    const newLine = line.slice(0, col) + output[i] + line.slice(col + 1)
    lines[row] = newLine
    col++
    i++
  }
  return lines.join('\n')
}

export function countTopBorderLinesBeforeFirstInputBox(output: string): number {
  const visualOutput = simulateTerminalOverwrite(output)
  const normalized = stripAllAnsi(visualOutput)
  const lines = normalized.split('\n')
  const boxTopLines = lines.filter((l) =>
    TOP_BORDER_PATTERN.test((l ?? '').trim())
  )
  return boxTopLines.length
}
