import * as readline from 'node:readline'
import { formatVersionOutput } from './version.js'

const PLACEHOLDER = 'Plan, search, build anything'
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

function showBanner(width: number): void {
  const versionLine = formatVersionOutput()
  console.log(versionLine)
  console.log()
  console.log(`┌${'─'.repeat(width - 2)}┐`)
  console.log(`│ ${(PROMPT + PLACEHOLDER).padEnd(width - 4)} │`)
  console.log(`└${'─'.repeat(width - 2)}┘`)
  console.log('exit to quit')
  console.log()
}

async function runInteractiveTTY(): Promise<void> {
  const width = Math.min(60, Math.max(40, process.stdout.columns ?? 60))
  showBanner(width)

  process.stdin.setRawMode(true)
  process.stdin.resume()
  process.stdin.setEncoding('utf8')
  readline.emitKeypressEvents(process.stdin)

  let buffer = ''
  process.stdout.write(PROMPT)

  process.stdin.on('keypress', (str, key) => {
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
  })
}

async function runInteractivePiped(): Promise<void> {
  showBanner(58)

  const rl = readline.createInterface({
    input: process.stdin,
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

export async function runInteractive(): Promise<void> {
  if (process.stdin.isTTY) {
    await runInteractiveTTY()
  } else {
    await runInteractivePiped()
  }
}
