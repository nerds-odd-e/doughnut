import * as readline from 'node:readline'
import { maskInteractiveInputForHistory } from '../inputHistoryMask.js'
import {
  buildBoxLines,
  buildSuggestionLines,
  getTerminalWidth,
  isCommittedInteractiveInput,
  RECALL_SESSION_YES_NO_PLACEHOLDER,
  renderBox,
  renderPastInput,
  type PlaceholderContext,
} from '../renderer.js'
import type { OutputAdapter } from '../types.js'
import { formatVersionOutput } from '../version.js'

export interface PipedDeps {
  processInput: (
    input: string,
    output?: OutputAdapter,
    interactiveUi?: boolean
  ) => Promise<boolean>
  getPlaceholderContext: (inTokenList: boolean) => PlaceholderContext
}

export async function runPiped(
  stdin: NodeJS.ReadableStream,
  deps: PipedDeps
): Promise<void> {
  const { processInput, getPlaceholderContext } = deps

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
    if (
      isCommittedInteractiveInput(line) &&
      getPlaceholderContext(false) !== RECALL_SESSION_YES_NO_PLACEHOLDER
    ) {
      console.log(
        renderPastInput(
          maskInteractiveInputForHistory(line),
          getTerminalWidth()
        )
      )
    }
    if (await processInput(line, undefined, true)) {
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
