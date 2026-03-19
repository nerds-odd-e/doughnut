import * as readline from 'node:readline'

export interface PipedDeps {
  processInput: (input: string) => Promise<boolean>
  getTerminalWidth: () => number
  buildBoxLines: (buffer: string, width: number) => string[]
  buildSuggestionLines: (
    buffer: string,
    highlightIndex: number,
    width: number
  ) => string[]
  renderBox: (lines: string[], width: number) => string
  renderPastInput: (input: string, width: number) => string
  formatVersionOutput: () => string
}

export async function runPiped(
  stdin: NodeJS.ReadableStream,
  deps: PipedDeps
): Promise<void> {
  const {
    processInput,
    getTerminalWidth,
    buildBoxLines,
    buildSuggestionLines,
    renderBox,
    renderPastInput,
    formatVersionOutput,
  } = deps

  const width = getTerminalWidth()
  console.log(formatVersionOutput())
  console.log()
  const currentGuidanceLines = buildSuggestionLines('', 0, width)
  console.log(renderBox(buildBoxLines('', width), width))
  for (const line of currentGuidanceLines) {
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
