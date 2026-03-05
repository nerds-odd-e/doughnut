import * as readline from 'node:readline'
import { formatVersionOutput } from './version.js'

const GREY = '\x1b[90m'
const RESET = '\x1b[0m'
const PLACEHOLDER = '`exit` to quit.'
const PROMPT = '→ '

export function processInput(input: string): boolean {
  const trimmed = input.trim()
  if (trimmed === 'exit') {
    return true
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

async function runInteractiveTTY(stdin: NodeJS.ReadableStream): Promise<void> {
  const width = Math.min(60, Math.max(40, process.stdout.columns ?? 60))
  console.log(formatVersionOutput())
  console.log()

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  readline.emitKeypressEvents(stdin)

  let buffer = ''
  let linesAboveCursor = 0
  let prevTotalLines = 0

  function drawBox() {
    const contentLines = buildBoxLines(buffer, width)
    const boxLines = renderBox(contentLines, width).split('\n')
    const newTotalLines = boxLines.length

    if (linesAboveCursor > 0) {
      process.stdout.write(`\x1b[${linesAboveCursor}A`)
    }
    process.stdout.write('\r')

    for (const line of boxLines) {
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

  stdin.on(
    'keypress',
    (
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
          process.stdout.write(`\x1b[${1}B\r\n`)
          const input = buffer
          buffer = ''
          if (processInput(input)) {
            process.exit(0)
          }
          linesAboveCursor = 0
          prevTotalLines = 0
          drawBox()
        }
      } else if (key.name === 'backspace') {
        if (buffer.length > 0) {
          buffer = buffer.slice(0, -1)
          drawBox()
        }
      } else if (str && !key.ctrl && !key.meta) {
        buffer += str
        drawBox()
      }
    }
  )
}

async function runInteractivePiped(
  stdin: NodeJS.ReadableStream
): Promise<void> {
  console.log(formatVersionOutput())
  console.log()
  const contentLines = buildBoxLines('', 58)
  console.log(renderBox(contentLines, 58))
  console.log()

  const rl = readline.createInterface({
    input: stdin,
    output: process.stdout,
    terminal: false,
  })

  rl.on('line', (line) => {
    if (processInput(line)) {
      rl.close()
      process.exit(0)
    }
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
