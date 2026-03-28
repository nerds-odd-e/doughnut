import { runInteractiveTtySession } from './ttyAdapters/interactiveTtySession.js'
import { exitCliError } from './cliExit.js'
import {
  addAccessToken,
  createAccessToken,
  removeAccessToken,
  removeAccessTokenCompletely,
} from './commands/accessToken.js'
import { userVisibleOutcomeFromCommandError } from './fetchAbort.js'
import { formatHelp } from './commands/help.js'
import {
  getInteractiveFetchWaitLine,
  INTERACTIVE_FETCH_WAIT_LINES,
  resetInteractiveFetchWaitForTesting,
  runInteractiveFetchWait,
  type InteractiveFetchWaitLine,
} from './interactiveFetchWait.js'
import type { PlaceholderContext } from './renderer.js'
import type { OutputAdapter } from './types.js'

type ParamCommandResult = string | undefined

type ParamCommandWithFetchWait = {
  command: string
  usage: string
  usesInteractiveFetchWait: true
  waitLine: InteractiveFetchWaitLine
  run: (param: string, signal: AbortSignal) => Promise<ParamCommandResult>
}

type ParamCommandLocalOnly = {
  command: string
  usage: string
  usesInteractiveFetchWait?: false
  run: (param: string) => ParamCommandResult
}

type ParamCommand = ParamCommandWithFetchWait | ParamCommandLocalOnly

const PARAM_COMMANDS: ParamCommand[] = [
  {
    command: '/add-access-token',
    usage: 'Usage: /add-access-token <token>',
    usesInteractiveFetchWait: true,
    waitLine: INTERACTIVE_FETCH_WAIT_LINES.addAccessToken,
    run: async (param, signal) => {
      await addAccessToken(param, signal)
      return 'Token added'
    },
  },
  {
    command: '/create-access-token',
    usage: 'Usage: /create-access-token <label>',
    usesInteractiveFetchWait: true,
    waitLine: INTERACTIVE_FETCH_WAIT_LINES.createAccessToken,
    run: async (param, signal) => {
      await createAccessToken(param, signal)
      return 'Token created'
    },
  },
  {
    command: '/remove-access-token-completely',
    usage: 'Usage: /remove-access-token-completely <label>',
    usesInteractiveFetchWait: true,
    waitLine: INTERACTIVE_FETCH_WAIT_LINES.removeAccessTokenCompletely,
    run: async (param, signal) => {
      await removeAccessTokenCompletely(param, signal)
      return `Token "${param}" removed locally and from server.`
    },
  },
  {
    command: '/remove-access-token',
    usage: 'Usage: /remove-access-token <label>',
    run: (param) => {
      if (removeAccessToken(param)) return `Token "${param}" removed.`
      return `Token "${param}" not found.`
    },
  },
]

export function resetRecallStateForTesting(): void {
  resetInteractiveFetchWaitForTesting()
}

function getPlaceholderContext(): PlaceholderContext {
  if (getInteractiveFetchWaitLine() !== null) return 'interactiveFetchWait'
  return 'default'
}

function parseCommandWithRequiredParam(
  trimmed: string,
  command: string
): string | 'usage' | null {
  if (trimmed !== command && !trimmed.startsWith(`${command} `)) return null
  const param = trimmed.slice(command.length).trim()
  return param ? param : 'usage'
}

function logCancelledOrError(err: unknown, output: OutputAdapter): void {
  const { text, tone } = userVisibleOutcomeFromCommandError(err)
  if (tone === 'userNotice') {
    if (output.logUserNotice) output.logUserNotice(text)
    else output.log(text)
  } else {
    output.logError(err)
  }
}

async function handleParamCommand(
  trimmed: string,
  output: OutputAdapter
): Promise<boolean> {
  for (const entry of PARAM_COMMANDS) {
    const param = parseCommandWithRequiredParam(trimmed, entry.command)
    if (param === null) continue
    if (param === 'usage') {
      output.log(entry.usage)
      return true
    }
    try {
      const msg = entry.usesInteractiveFetchWait
        ? await runInteractiveFetchWait(output, entry.waitLine, (signal) =>
            entry.run(param, signal)
          )
        : entry.run(param)
      if (msg) output.log(msg)
    } catch (err) {
      logCancelledOrError(err, output)
    }
    return true
  }
  return false
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
  if (trimmed === '/help') {
    output.log(formatHelp())
    return false
  }
  if (await handleParamCommand(trimmed, output)) {
    return false
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
