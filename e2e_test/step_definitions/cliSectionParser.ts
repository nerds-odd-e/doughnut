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

const INPUT_BOX_TOP_BORDER_LINE = /^┌─*┐$/

/**
 * Plain grid after replaying an interactive CLI PTY transcript through a minimal
 * cursor/erase model and stripping SGR + CSI. Used to approximate what the user sees
 * (e.g. duplicate `┌─┐`, stray ESC written as text when a sequence is unhandled).
 */
export type InteractiveCliSimulatedPlainScreen = Readonly<{
  asPlainTextGrid: string
}>

function toInteractiveCliSimulatedPlainScreen(
  ptyTranscript: string
): InteractiveCliSimulatedPlainScreen {
  return {
    asPlainTextGrid: stripAllAnsi(
      replayInteractiveCliPtyTranscriptOntoGrid(ptyTranscript)
    ),
  }
}

/** Applies cursor motion, line erase, and screen clear from a PTY byte stream onto a logical character grid. */
function replayInteractiveCliPtyTranscriptOntoGrid(
  ptyTranscript: string
): string {
  const lines: string[] = []
  let row = 0
  let col = 0
  let i = 0
  const ESC = '\x1b' as const
  const cursorUpRe = new RegExp(`^${ESC}\\[(\\d+)A`)
  const eraseLineRe = new RegExp(`^${ESC}\\[2K`)
  const cursorColRe = new RegExp(`^${ESC}\\[(\\d+)G`)
  const clearScreenRe = new RegExp(`^${ESC}\\[[02]?J`) // \x1b[J, \x1b[2J clear screen
  const cursorHomeRe = new RegExp(`^${ESC}\\[H`) // \x1b[H cursor home
  /** DEC private mode CSI: SHOW_CURSOR / HIDE_CURSOR (\x1b[?25h), etc. */
  const decPrivateCsiRe = new RegExp(`^${ESC}\\[[?][0-9;]*[a-zA-Z]`)
  const ansiRe = new RegExp(`^${ESC}\\[[0-9;]*[A-Za-z]`)
  while (i < ptyTranscript.length) {
    if (ptyTranscript.startsWith(`${ESC}]`, i)) {
      let j = i + 2
      while (j < ptyTranscript.length) {
        if (ptyTranscript[j] === '\x07') {
          j++
          break
        }
        if (ptyTranscript[j] === ESC && ptyTranscript[j + 1] === '\\') {
          j += 2
          break
        }
        j++
      }
      i = j
      continue
    }
    if (ptyTranscript.startsWith('\x1b[', i)) {
      const cursorUpMatch = ptyTranscript.slice(i).match(cursorUpRe)
      const eraseLineMatch = ptyTranscript.slice(i).match(eraseLineRe)
      const cursorColMatch = ptyTranscript.slice(i).match(cursorColRe)
      const clearScreenMatch = ptyTranscript.slice(i).match(clearScreenRe)
      const cursorHomeMatch = ptyTranscript.slice(i).match(cursorHomeRe)
      if (cursorUpMatch) {
        row = Math.max(0, row - Number(cursorUpMatch[1]))
        i += cursorUpMatch[0].length
        continue
      }
      if (eraseLineMatch) {
        while (lines.length <= row) {
          lines.push('')
        }
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
      const decPrivateMatch = ptyTranscript.slice(i).match(decPrivateCsiRe)
      if (decPrivateMatch) {
        i += decPrivateMatch[0].length
        continue
      }
      const ansiMatch = ptyTranscript.slice(i).match(ansiRe)
      if (ansiMatch) {
        i += ansiMatch[0].length
        continue
      }
    }
    if (ptyTranscript[i] === '\r') {
      col = 0
      i++
      continue
    }
    if (ptyTranscript[i] === '\n') {
      row++
      col = 0
      i++
      continue
    }
    while (lines.length <= row) {
      lines.push('')
    }
    const line = lines[row] ?? ''
    const newLine = line.slice(0, col) + ptyTranscript[i] + line.slice(col + 1)
    lines[row] = newLine
    col++
    i++
  }
  return lines.join('\n')
}

function interactiveCliSimulatedScreenEscapeLeakFailureMessage(
  screen: InteractiveCliSimulatedPlainScreen
): string | null {
  const lines = screen.asPlainTextGrid.split('\n')
  const corrupt = lines
    .map((line, rowIndex) => ({ line, rowIndex }))
    .filter(({ line }) => line.includes('\u001b'))
  if (corrupt.length === 0) {
    return null
  }
  const examples = corrupt
    .slice(0, 3)
    .map(
      ({ line, rowIndex }) =>
        `  row ${rowIndex}: ${JSON.stringify(line.slice(0, 80))}`
    )
    .join('\n')
  return (
    `Interactive CLI PTY simulator still has raw ESC (\\x1b) after stripping —` +
    ` an escape was treated as printable text. Add handling in replayInteractiveCliPtyTranscriptOntoGrid ` +
    `(e.g. DEC private CSI \\x1b[?25h, OSC \\x1b]…\\x07). Sample rows:\n${examples}`
  )
}

/** After each interactive “input ready” paint, the PTY transcript must replay to a grid without escape leaks. */
export function assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
  ptyTranscript: string
): void {
  const screen = toInteractiveCliSimulatedPlainScreen(ptyTranscript)
  const msg = interactiveCliSimulatedScreenEscapeLeakFailureMessage(screen)
  if (msg !== null) {
    throw new Error(msg)
  }
}

/** Counts input box top borders (`┌─┐`) in the simulated plain grid (entire transcript). */
export function countInputBoxTopBorderLinesInInteractivePtyTranscript(
  ptyTranscript: string
): number {
  const { asPlainTextGrid } =
    toInteractiveCliSimulatedPlainScreen(ptyTranscript)
  return asPlainTextGrid
    .split('\n')
    .filter((l) => INPUT_BOX_TOP_BORDER_LINE.test((l ?? '').trim())).length
}
