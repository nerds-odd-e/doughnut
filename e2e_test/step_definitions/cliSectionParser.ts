/**
 * Parses interactive CLI PTY stdout for E2E assertions.
 *
 * - **Past messages** (`getPastCliAssistantMessagesContent`, `getPastUserMessagesContent`): domain steps for CLI assistant vs user transcript.
 * - **Line-split merge** (`getRecallDisplaySections`): newline-based regions; can retain text after Ink clears the live grid (e.g. MCQ stem after `/stop`). Not the same as {@link ptyTranscriptSimulatedPlainScreen}.
 * - **Simulated screen** (`ptyTranscriptSimulatedPlainScreen`): cursor/erase replay → plain text ≈ user-visible cells.
 */

// --- ANSI stripping ---
const HISTORY_INPUT_BG = '\x1b[48;5;236m'
const CURRENT_PROMPT_SEPARATOR_GREEN = '\x1b[32m'
const CURRENT_PROMPT_HINT = '\x1b[90m'
const COMMAND_LINE_PROMPT_START = '→'
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
function isPastUserMessageLine(line: string): boolean {
  return line.includes(HISTORY_INPUT_BG)
}

function isCurrentPromptSeparatorLine(line: string): boolean {
  return line.includes(CURRENT_PROMPT_SEPARATOR_GREEN) && line.includes('─')
}

function isCurrentPromptHintLine(line: string): boolean {
  return line.includes(CURRENT_PROMPT_HINT) && !line.includes(HISTORY_INPUT_BG)
}

// --- Live command-line row (borderless TTY; phase 10.5) ---
function findLastSeparatorIndex(lines: string[]): number {
  for (let i = lines.length - 1; i >= 0; i--) {
    if (isCurrentPromptSeparatorLine(lines[i]!)) {
      return i
    }
  }
  return -1
}

function findLastInputBoxStart(lines: string[]): number {
  const separatorIdx = findLastSeparatorIndex(lines)
  const scanFrom = separatorIdx >= 0 ? separatorIdx + 1 : 0
  for (let i = lines.length - 1; i >= scanFrom; i--) {
    const plain = stripAllAnsi(lines[i] ?? '').trimStart()
    if (plain.startsWith(COMMAND_LINE_PROMPT_START)) {
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
  return boxStart
}

// --- Section extraction ---
type Boundaries = {
  pastTranscriptLines: string[]
  currentPromptLines: string[]
  currentGuidanceLines: string[]
}

function parseCliOutput(output: string): Boundaries {
  const lines = output.split('\n')
  const separatorIdx = findLastSeparatorIndex(lines)
  const boxStart = findLastInputBoxStart(lines)
  const boxEnd = findLastInputBoxEnd(lines)
  const pastEnd = separatorIdx >= 0 ? separatorIdx : lines.length
  const pastTranscriptLines = lines.slice(0, pastEnd)
  const currentPromptLines =
    separatorIdx >= 0 ? lines.slice(separatorIdx + 1, boxStart) : []
  const currentGuidanceLines = boxEnd >= 0 ? lines.slice(boxEnd + 1) : []
  return {
    pastTranscriptLines,
    currentPromptLines,
    currentGuidanceLines,
  }
}

type Section =
  | 'past-user-messages'
  | 'past-cli-assistant-messages'
  | 'current-prompt'
  | 'current-guidance'

function collectSectionLines(
  boundaries: Boundaries,
  section: Section,
  strip: boolean
): string[] {
  const { pastTranscriptLines, currentPromptLines, currentGuidanceLines } =
    boundaries
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

  if (section === 'past-user-messages') {
    for (const line of pastTranscriptLines) {
      if (isPastUserMessageLine(line)) {
        const content = strip ? stripAnsi(line) : line
        if (content.trim()) {
          result.push(content)
        }
      }
    }
  } else if (section === 'past-cli-assistant-messages') {
    for (const line of pastTranscriptLines) {
      if (!isPastUserMessageLine(line)) {
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

export function getPastCliAssistantMessagesContent(output: string): string {
  return getSectionContent(output, 'past-cli-assistant-messages')
}

export function getPastUserMessagesContent(output: string): string {
  return getSectionContent(output, 'past-user-messages')
}

function parseLiveRegion(
  output: string,
  stripAnsiForAssertions: boolean
): { mergedTranscriptPlain: string; pastCliAssistantMessagesPlain: string } {
  const boundaries = parseCliOutput(output)
  const strip = stripAnsiForAssertions
  const pastCliAssistantMessagesPlain = collectSectionLines(
    boundaries,
    'past-cli-assistant-messages',
    strip
  ).join('\n')
  const mergedTranscriptPlain = [
    collectSectionLines(boundaries, 'current-prompt', strip).join('\n'),
    collectSectionLines(boundaries, 'current-guidance', strip).join('\n'),
    pastCliAssistantMessagesPlain,
  ]
    .join('\n')
    .trim()
  return { mergedTranscriptPlain, pastCliAssistantMessagesPlain }
}

/** Line-split slices for recall edge cases (e.g. stem still in transcript after live UI cleared). */
export type PtyRecallAssertionSlices = Readonly<{
  mergedTranscriptPlain: string
  pastCliAssistantMessagesPlain: string
}>

export function getRecallDisplaySections(
  output: string
): PtyRecallAssertionSlices {
  return parseLiveRegion(output, true)
}

/**
 * Raw ANSI merge of prompt + guidance + past CLI assistant message rows (styling checks).
 * PTY cursor replay drops SGR (`m`), so this is not interchangeable with {@link ptyTranscriptSimulatedPlainScreen}.
 */
export function getRecallMergedTranscriptRaw(output: string): string {
  return parseLiveRegion(output, false).mergedTranscriptPlain
}

/**
 * Plain grid after replaying an interactive CLI PTY transcript through a minimal
 * cursor/erase model and stripping SGR + CSI. Used to approximate what the user sees
 * (e.g. duplicate command-line `→` rows, stray ESC written as text when a sequence is unhandled).
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

/** Plain text after replaying PTY cursor motion and erases — approximates on-screen cells (see `replayInteractiveCliPtyTranscriptOntoGrid`). */
export function ptyTranscriptSimulatedPlainScreen(
  ptyTranscript: string
): string {
  return toInteractiveCliSimulatedPlainScreen(ptyTranscript).asPlainTextGrid
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
  const cursorUpRe = new RegExp(`^${ESC}\\[(\\d*)A`)
  const cursorDownRe = new RegExp(`^${ESC}\\[(\\d*)B`)
  const cursorForwardRe = new RegExp(`^${ESC}\\[(\\d*)C`)
  const cursorBackRe = new RegExp(`^${ESC}\\[(\\d*)D`)
  const eraseLineRe = new RegExp(`^${ESC}\\[2K`)
  const cursorColRe = new RegExp(`^${ESC}\\[(\\d+)G`)
  /** ED: erase display — treat common forms like full clear (Linux PTY often uses 2J ± 3J). */
  const clearScreenRe = new RegExp(`^${ESC}\\[[023]?J`)
  /** CUP: cursor position (1-based row;col). \x1b[H is empty params → 1;1. */
  const cupRe = new RegExp(`^${ESC}\\[(\\d*);?(\\d*)([Hf])`)
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
      const cursorDownMatch = ptyTranscript.slice(i).match(cursorDownRe)
      const cursorForwardMatch = ptyTranscript.slice(i).match(cursorForwardRe)
      const cursorBackMatch = ptyTranscript.slice(i).match(cursorBackRe)
      const eraseLineMatch = ptyTranscript.slice(i).match(eraseLineRe)
      const cursorColMatch = ptyTranscript.slice(i).match(cursorColRe)
      const clearScreenMatch = ptyTranscript.slice(i).match(clearScreenRe)
      const cupMatch = ptyTranscript.slice(i).match(cupRe)
      if (cursorUpMatch) {
        const n = cursorUpMatch[1] === '' ? 1 : Number(cursorUpMatch[1])
        row = Math.max(0, row - n)
        i += cursorUpMatch[0].length
        continue
      }
      if (cursorDownMatch) {
        const n = cursorDownMatch[1] === '' ? 1 : Number(cursorDownMatch[1])
        row += n
        i += cursorDownMatch[0].length
        continue
      }
      if (cursorForwardMatch) {
        const n =
          cursorForwardMatch[1] === '' ? 1 : Number(cursorForwardMatch[1])
        col += n
        i += cursorForwardMatch[0].length
        continue
      }
      if (cursorBackMatch) {
        const n = cursorBackMatch[1] === '' ? 1 : Number(cursorBackMatch[1])
        col = Math.max(0, col - n)
        i += cursorBackMatch[0].length
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
      if (cupMatch) {
        const row1 = cupMatch[1] === '' ? 1 : Number(cupMatch[1])
        const col1 = cupMatch[2] === '' ? 1 : Number(cupMatch[2])
        row = Math.max(0, row1 - 1)
        col = Math.max(0, col1 - 1)
        i += cupMatch[0].length
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

/** Counts live command-line prompt rows (`→` …) in the simulated plain grid (entire transcript). */
export function countInputBoxTopBorderLinesInInteractivePtyTranscript(
  ptyTranscript: string
): number {
  const { asPlainTextGrid } =
    toInteractiveCliSimulatedPlainScreen(ptyTranscript)
  return asPlainTextGrid
    .split('\n')
    .filter((l) => (l ?? '').trimStart().startsWith(COMMAND_LINE_PROMPT_START))
    .length
}
