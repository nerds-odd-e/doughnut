import { runInteractiveTtySession } from './ttyAdapters/interactiveTtySession.js'
import { exitCliError } from './cliExit.js'
import {
  getInteractiveFetchWaitLine,
  resetInteractiveFetchWaitForTesting,
} from './interactiveFetchWait.js'
import type { PlaceholderContext } from './renderer.js'
import type { OutputAdapter } from './types.js'

export function resetRecallStateForTesting(): void {
  resetInteractiveFetchWaitForTesting()
}

function getPlaceholderContext(): PlaceholderContext {
  if (getInteractiveFetchWaitLine() !== null) return 'interactiveFetchWait'
  return 'default'
}

const defaultOutput: OutputAdapter = {
  log: (msg) => console.log(msg),
  logError: (err) =>
    console.log(err instanceof Error ? err.message : String(err)),
  logUserNotice: (msg) => console.log(msg),
  writeCurrentPrompt: (msg) => console.log(msg),
}

export async function processInput(
  input: string,
  output: OutputAdapter = defaultOutput,
  interactiveUi = false
): Promise<boolean> {
  const trimmed = input.trim()
  if (trimmed === 'exit' || trimmed === '/exit') {
    if (interactiveUi) {
      output.log('Bye.')
    }
    return true
  }
  if (trimmed) {
    output.log('Not supported')
  }
  return false
}

function buildInteractiveShellDeps() {
  return {
    processInput,
    getPlaceholderContext,
    shouldRecordCommittedLineInUserInputHistory: () => true,
  }
}

export function runInteractive(stdin = process.stdin): void {
  if (!stdin.isTTY) {
    exitCliError('not a terminal (use version or update)')
  }
  runInteractiveTtySession(stdin, buildInteractiveShellDeps())
}
