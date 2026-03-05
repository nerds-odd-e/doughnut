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

export function renderBox(lines: string[], width: number): string {
  const innerWidth = width - 4
  const top = `┌${'─'.repeat(width - 2)}┐`
  const bottom = `└${'─'.repeat(width - 2)}┘`
  const rows = lines.map((line) => `│ ${line.padEnd(innerWidth)} │`)
  return [top, ...rows, bottom].join('\n')
}

function showBanner(width: number): void {
  console.log(formatVersionOutput())
  console.log()
  const placeholderLine = `${PROMPT}${GREY}${PLACEHOLDER}${RESET}`
  console.log(renderBox([placeholderLine], width))
  console.log()
}

async function runInteractiveTTY(stdin: NodeJS.ReadableStream): Promise<void> {
  const width = Math.min(60, Math.max(40, process.stdout.columns ?? 60))
  showBanner(width)

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  readline.emitKeypressEvents(stdin)

  let buffer = ''
  process.stdout.write(PROMPT)

  stdin.on(
    'keypress',
    (
      str: string,
      key: { name: string; shift?: boolean; ctrl?: boolean; meta?: boolean }
    ) => {
      if (key.ctrl && key.name === 'c') {
        process.exit(0)
      }
      if (key.name === 'return') {
        if (key.shift) {
          buffer += '\n'
          process.stdout.write(`\n${PROMPT}`)
        } else {
          process.stdout.write('\n')
          if (processInput(buffer)) {
            process.exit(0)
          }
          buffer = ''
          process.stdout.write(PROMPT)
        }
      } else if (key.name === 'backspace') {
        if (buffer.length > 0) {
          buffer = buffer.slice(0, -1)
          readline.moveCursor(process.stdout, -1, 0)
          process.stdout.write(' ')
          readline.moveCursor(process.stdout, -1, 0)
        }
      } else if (str && !key.ctrl && !key.meta) {
        buffer += str
        process.stdout.write(str)
      }
    }
  )
}

async function runInteractivePiped(
  stdin: NodeJS.ReadableStream
): Promise<void> {
  showBanner(58)

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
