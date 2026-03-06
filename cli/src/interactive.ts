import * as readline from 'node:readline'
import { addGmailAccount, getLastEmailSubject } from './gmail.js'
import {
  filterCommandsByPrefix,
  formatCommandSuggestionsWithHighlight,
  formatHelp,
  interactiveDocs,
} from './help.js'
import { formatVersionOutput } from './version.js'

const GREY = '\x1b[90m'
const GREY_BG = '\x1b[48;5;236m'
const RESET = '\x1b[0m'
const PLACEHOLDER = '`exit` to quit.'
const PROMPT = '→ '

export async function processInput(input: string): Promise<boolean> {
  const trimmed = input.trim()
  if (trimmed === 'exit' || trimmed === '/exit') {
    return true
  }
  if (trimmed === '/help') {
    console.log(formatHelp())
    return false
  }
  if (trimmed === '/add gmail') {
    try {
      await addGmailAccount()
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  if (trimmed === '/last email') {
    try {
      const subject = await getLastEmailSubject()
      console.log(subject)
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  if (trimmed) {
    console.log('Not supported')
  }
  return false
}

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g

export function visibleLength(str: string): number {
  return str.replace(ANSI_PATTERN, '').length
}

function padEndVisible(str: string, targetLen: number): string {
  const pad = targetLen - visibleLength(str)
  return pad > 0 ? str + ' '.repeat(pad) : str
}

export function renderBox(lines: string[], width: number): string {
  const innerWidth = width - 4
  const top = `┌${'─'.repeat(width - 2)}┐`
  const bottom = `└${'─'.repeat(width - 2)}┘`
  const rows = lines.map((line) => `│ ${padEndVisible(line, innerWidth)} │`)
  return [top, ...rows, bottom].join('\n')
}

export function renderPastInput(input: string, width: number): string {
  const innerWidth = width - 2
  const lines = input.split('\n')
  const emptyRow = `${GREY_BG}${' '.repeat(innerWidth)}${RESET}`
  const contentRows = lines.map(
    (line) => `${GREY_BG} ${padEndVisible(line, innerWidth - 1)}${RESET}`
  )
  return [emptyRow, ...contentRows, emptyRow, ''].join('\n')
}

export function buildBoxLines(buffer: string, width: number): string[] {
  const bufferLines = buffer.split('\n')
  return bufferLines.map((line, i) => {
    const prefix = i === 0 ? PROMPT : '  '
    if (i === 0 && buffer === '') {
      return `${prefix}${GREY}${PLACEHOLDER}${RESET}`
    }
    return prefix + line
  })
}

function getTerminalWidth(): number {
  return process.stdout.columns || 80
}

const COMMANDS_HINT = `${GREY}  / commands${RESET}`

function buildSuggestionLines(
  buffer: string,
  highlightIndex: number
): string[] {
  const bufferLines = buffer.split('\n')
  const lastLine = bufferLines[bufferLines.length - 1]
  if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
    const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
    return formatCommandSuggestionsWithHighlight(filtered, 8, highlightIndex)
  }
  return [COMMANDS_HINT]
}

async function runInteractiveTTY(stdin: NodeJS.ReadableStream): Promise<void> {
  console.log(formatVersionOutput())
  console.log()

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  readline.emitKeypressEvents(stdin)

  let buffer = ''
  let highlightIndex = 0
  let linesAboveCursor = 0
  let prevTotalLines = 0

  function drawBox() {
    const width = getTerminalWidth()
    const contentLines = buildBoxLines(buffer, width)
    const boxLines = renderBox(contentLines, width).split('\n')
    const suggestionLines = buildSuggestionLines(buffer, highlightIndex)
    const newTotalLines = boxLines.length + suggestionLines.length

    if (linesAboveCursor > 0) {
      process.stdout.write(`\x1b[${linesAboveCursor}A`)
    }
    process.stdout.write('\r')

    for (const line of boxLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    for (const line of suggestionLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    const extra = prevTotalLines - newTotalLines
    for (let i = 0; i < extra; i++) {
      process.stdout.write('\x1b[2K\n')
    }

    const totalWritten = Math.max(newTotalLines, prevTotalLines)
    const cursorRow = contentLines.length
    process.stdout.write(`\x1b[${totalWritten - cursorRow}A`)

    const bufferLines = buffer.split('\n')
    const lastLine = bufferLines[bufferLines.length - 1]
    const lastPrefix = bufferLines.length === 1 ? PROMPT : '  '
    const col = 3 + lastPrefix.length + lastLine.length
    process.stdout.write(`\x1b[${col}G`)

    linesAboveCursor = contentLines.length
    prevTotalLines = newTotalLines
  }

  drawBox()

  process.stdout.on('resize', drawBox)

  stdin.on(
    'keypress',
    async (
      str: string,
      key: { name: string; shift?: boolean; ctrl?: boolean; meta?: boolean }
    ) => {
      if (key.ctrl && key.name === 'c') {
        process.stdout.write(`\x1b[${1}B\r\n`)
        process.exit(0)
      }
      if (key.name === 'return') {
        if (key.shift) {
          buffer += '\n'
          drawBox()
        } else {
          const bufferLines = buffer.split('\n')
          const lastLine = bufferLines[bufferLines.length - 1]
          const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
          const suggestionsVisible =
            lastLine.startsWith('/') &&
            !lastLine.endsWith(' ') &&
            filtered.length > 0

          if (suggestionsVisible) {
            const selectedCommand = `${filtered[highlightIndex].usage} `
            buffer =
              bufferLines.slice(0, -1).concat(selectedCommand).join('\n') || ''
            highlightIndex = 0
            drawBox()
            return
          }

          const width = getTerminalWidth()
          const input = buffer
          buffer = ''

          if (linesAboveCursor > 0) {
            process.stdout.write(`\x1b[${linesAboveCursor}A`)
          }
          process.stdout.write('\r')
          for (let i = 0; i < prevTotalLines; i++) {
            process.stdout.write('\x1b[2K\n')
          }
          if (prevTotalLines > 1) {
            process.stdout.write(`\x1b[${prevTotalLines - 1}A`)
          }

          if (input.trim()) {
            process.stdout.write(renderPastInput(input, width))
            process.stdout.write('\n')
          }

          if (await processInput(input)) {
            process.exit(0)
          }
          linesAboveCursor = 0
          prevTotalLines = 0
          drawBox()
        }
      } else if (key.name === 'backspace') {
        if (buffer.length > 0) {
          buffer = buffer.slice(0, -1)
          highlightIndex = 0
          drawBox()
        }
      } else if (key.name === 'up' || key.name === 'down') {
        const bufferLines = buffer.split('\n')
        const lastLine = bufferLines[bufferLines.length - 1]
        const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
        const suggestionsVisible =
          lastLine.startsWith('/') &&
          !lastLine.endsWith(' ') &&
          filtered.length > 0
        if (suggestionsVisible) {
          const n = filtered.length
          highlightIndex =
            key.name === 'up'
              ? (highlightIndex - 1 + n) % n
              : (highlightIndex + 1) % n
          drawBox()
        }
      } else if (str && !key.ctrl && !key.meta) {
        buffer += str
        highlightIndex = 0
        drawBox()
      }
    }
  )
}

async function runInteractivePiped(
  stdin: NodeJS.ReadableStream
): Promise<void> {
  const width = getTerminalWidth()
  console.log(formatVersionOutput())
  console.log()
  const hintLines = buildSuggestionLines('', 0)
  console.log(renderBox(buildBoxLines('', width), width))
  for (const line of hintLines) {
    console.log(line)
  }
  console.log()

  const rl = readline.createInterface({
    input: stdin,
    output: process.stdout,
    terminal: false,
  })

  let processing = false
  const lineQueue: string[] = []
  async function processNextLine() {
    if (processing || lineQueue.length === 0) return
    processing = true
    const line = lineQueue.shift()!
    if (line.trim()) {
      console.log(renderPastInput(line, getTerminalWidth()))
    }
    if (await processInput(line)) {
      rl.close()
      process.exit(0)
    }
    processing = false
    if (lineQueue.length > 0) processNextLine()
  }

  rl.on('line', (line) => {
    lineQueue.push(line)
    processNextLine()
  })
}

export async function runInteractive(
  stdin: NodeJS.ReadableStream = process.stdin
): Promise<void> {
  if (stdin.isTTY) {
    await runInteractiveTTY(stdin)
  } else {
    await runInteractivePiped(stdin)
  }
}
