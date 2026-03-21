import { runPiped } from './adapters/pipedAdapter.js'
import { runTTY, type TokenListCommandConfig } from './adapters/ttyAdapter.js'
import {
  addAccessToken,
  createAccessToken,
  formatTokenLines,
  getDefaultTokenLabel,
  listAccessTokens,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
} from './accessToken.js'
import { userVisibleOutcomeFromCommandError } from './fetchAbort.js'
import { addGmailAccount, getLastEmailSubject } from './gmail.js'
import {
  filterCommandsByPrefix,
  formatHelp,
  getTabCompletion,
  interactiveDocs,
} from './help.js'
import { renderMarkdownToTerminal } from './markdown.js'
import {
  answerQuiz,
  answerSpelling,
  contestAndRegenerate,
  markAsRecalled,
  recallNext,
  recallStatus,
  type RecallNextResult,
} from './recall.js'
import {
  getInteractiveFetchWaitLine,
  INTERACTIVE_FETCH_WAIT_LINES,
  resetInteractiveFetchWaitForTesting,
  runInteractiveFetchWait,
  type InteractiveFetchWaitLine,
} from './interactiveFetchWait.js'
import { formatVersionOutput } from './version.js'
import {
  buildBoxLines,
  buildCurrentPromptSeparator,
  buildLiveRegionLines,
  needsGapBeforeBox,
  buildSuggestionLines,
  buildTokenListLines,
  formatMcqChoiceLines,
  recallMcqCurrentGuidanceLines,
  wrapMarkdownTerminalToLines,
  getLastLine,
  getTerminalWidth,
  renderBox,
  renderFullDisplay,
  renderPastInput,
  writeFullRedraw,
  GREY,
  HIDE_CURSOR,
  SHOW_CURSOR,
  CLEAR_SCREEN,
  RECALLING_INDICATOR,
  PROMPT,
  RECALL_SESSION_YES_NO_PLACEHOLDER,
  type PlaceholderContext,
} from './renderer.js'
import type {
  McqRecallPending,
  OutputAdapter,
  PendingRecallAnswer,
  SpellingRecallPending,
} from './types.js'

type RecallPromptResult = Exclude<RecallNextResult, { type: 'none' }>

let pendingRecallAnswer: PendingRecallAnswer = null
let recallSessionMode = false
let sessionRecallCount = 0
let recallSessionDueDays = 0
let pendingRecallLoadMore = false
let pendingRecallStopConfirmation = false

export * from './interactiveFetchWait.js'

export function resetRecallStateForTesting(): void {
  pendingRecallAnswer = null
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
  pendingRecallStopConfirmation = false
  resetInteractiveFetchWaitForTesting()
}

function getPendingRecallAnswer(): PendingRecallAnswer {
  return pendingRecallAnswer
}

function isPendingRecallStopConfirmation(): boolean {
  return pendingRecallStopConfirmation
}

function setPendingRecallStopConfirmation(value: boolean): void {
  pendingRecallStopConfirmation = value
}

export function isInRecallSubstate(): boolean {
  return (
    recallSessionMode || pendingRecallAnswer !== null || pendingRecallLoadMore
  )
}

function exitRecallMode(): void {
  recallSessionMode = false
  pendingRecallAnswer = null
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
  pendingRecallStopConfirmation = false
}

function formatRecallSessionSummary(count: number): string {
  if (count === 0) return '0 notes to recall today'
  if (count === 1) return 'Recalled 1 note'
  return `Recalled ${count} notes`
}

function isMcqRecallPending(p: unknown): p is McqRecallPending {
  if (p === null || typeof p !== 'object') return false
  const o = p as Record<string, unknown>
  return (
    typeof o.recallPromptId === 'number' &&
    Array.isArray(o.choices) &&
    typeof o.stemRenderedForTerminal === 'string' &&
    typeof o.shownAt === 'number' &&
    !('type' in o)
  )
}

function isSpellingRecallPending(
  p: PendingRecallAnswer
): p is SpellingRecallPending {
  return p !== null && 'type' in p && p.type === 'spelling'
}

function getContestablePromptId(): number | null {
  return isMcqRecallPending(pendingRecallAnswer) ||
    isSpellingRecallPending(pendingRecallAnswer)
    ? pendingRecallAnswer.recallPromptId
    : null
}

function getPlaceholderContext(inTokenList: boolean): PlaceholderContext {
  if (getInteractiveFetchWaitLine() !== null) return 'interactiveFetchWait'
  if (inTokenList) return 'tokenList'
  if (pendingRecallStopConfirmation) return 'recallStopConfirmation'
  if (pendingRecallLoadMore) return RECALL_SESSION_YES_NO_PLACEHOLDER
  if (isMcqRecallPending(pendingRecallAnswer)) return 'recallMcq'
  if (isSpellingRecallPending(pendingRecallAnswer)) return 'recallSpelling'
  if (pendingRecallAnswer !== null) return RECALL_SESSION_YES_NO_PLACEHOLDER
  return 'default'
}

/**
 * Non-TTY MCQ: stem on `writeCurrentPrompt` (Current prompt analogue); numbered choices and
 * “Enter your choice…” on `log` (Current guidance analogue). Avoids sending choice lines through both hooks.
 */
function emitMcqRecallQuestionForNonInteractiveOutput(
  output: OutputAdapter,
  stemRenderedForTerminal: string,
  choices: readonly string[]
): void {
  const writePrompt = output.writeCurrentPrompt ?? output.log
  writePrompt(stemRenderedForTerminal)
  const width = getTerminalWidth()
  for (const line of formatMcqChoiceLines(choices, width)) {
    output.log(line)
  }
  output.log(`Enter your choice (1-${choices.length}):`)
}

function showRecallPrompt(
  result: RecallPromptResult,
  output: OutputAdapter,
  writeCurrentPrompt: (msg: string) => void
): void {
  if (result.type === 'mcq') {
    const stemRenderedForTerminal = renderMarkdownToTerminal(result.stem)
    pendingRecallAnswer = {
      recallPromptId: result.recallPromptId,
      choices: result.choices,
      stemRenderedForTerminal,
      shownAt: Date.now(),
    }
    if (!output.beginCurrentPrompt) {
      emitMcqRecallQuestionForNonInteractiveOutput(
        output,
        stemRenderedForTerminal,
        result.choices
      )
    }
    return
  }
  output.beginCurrentPrompt?.()
  if (result.type === 'spelling') {
    writeCurrentPrompt(
      `Spell: ${renderMarkdownToTerminal(result.stem || '...')}`
    )
    pendingRecallAnswer = {
      recallPromptId: result.recallPromptId,
      type: 'spelling',
      shownAt: Date.now(),
    }
    return
  }
  writeCurrentPrompt(result.title)
  if (result.details) {
    writeCurrentPrompt(renderMarkdownToTerminal(result.details))
  }
  writeCurrentPrompt('Yes, I remember? (y/n)')
  pendingRecallAnswer = { memoryTrackerId: result.memoryTrackerId }
}

function endRecallSession(): void {
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
}

function parseYesNo(input: string): boolean | null {
  const a = input.trim().toLowerCase()
  if (a === 'y' || a === 'yes') return true
  if (a === 'n' || a === 'no') return false
  return null
}

function parseCommandWithRequiredParam(
  trimmed: string,
  command: string
): string | 'usage' | null {
  if (trimmed !== command && !trimmed.startsWith(`${command} `)) return null
  const param = trimmed.slice(command.length).trim()
  return param ? param : 'usage'
}

type ParamCommandResult = string | undefined

/** Slash-commands that may hit the network while the TTY shows the interactive fetch-wait chrome. */
type ParamCommandWithFetchWait = {
  command: string
  usage: string
  usesInteractiveFetchWait: true
  waitLine: InteractiveFetchWaitLine
  run: (param: string, signal: AbortSignal) => Promise<ParamCommandResult>
}

/** Slash-commands that only touch local config (no fetch-wait chrome). */
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

function logCancelledOrError(err: unknown, output: OutputAdapter): void {
  const { text, tone } = userVisibleOutcomeFromCommandError(err)
  if (tone === 'userNotice') {
    if (output.logUserNotice) output.logUserNotice(text)
    else output.log(text)
  } else {
    output.logError(err)
  }
}

/** Recall session cannot continue after a failed or user-aborted recall load. */
function endRecallSessionAfterFailedRecallLoad(
  err: unknown,
  output: OutputAdapter
): void {
  endRecallSession()
  logCancelledOrError(err, output)
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

async function continueRecallSession(
  fromLoadMore: boolean,
  output: OutputAdapter,
  writeCurrentPrompt: (msg: string) => void
): Promise<void> {
  if (!fromLoadMore) sessionRecallCount++
  try {
    const result = await runInteractiveFetchWait(
      output,
      INTERACTIVE_FETCH_WAIT_LINES.recallNext,
      (signal) => recallNext(recallSessionDueDays, signal)
    )
    if (result.type === 'none') {
      if (recallSessionDueDays === 0) {
        pendingRecallLoadMore = true
        output.beginCurrentPrompt?.()
        writeCurrentPrompt('Load more from next 3 days? (y/n)')
        return
      }
      output.log(formatRecallSessionSummary(sessionRecallCount))
      endRecallSession()
      return
    }
    showRecallPrompt(result, output, writeCurrentPrompt)
  } catch (err) {
    endRecallSessionAfterFailedRecallLoad(err, output)
  }
}

const TOKEN_LIST_COMMANDS: Record<string, TokenListCommandConfig> = {
  '/list-access-token': {
    action: 'set-default',
    currentPrompt: 'Select and enter to change the default access token',
  },
  '/remove-access-token': { action: 'remove' },
  '/remove-access-token-completely': { action: 'remove-completely' },
}

const defaultOutput: OutputAdapter = {
  log: (msg) => console.log(msg),
  logError: (err) =>
    console.log(err instanceof Error ? err.message : String(err)),
  logUserNotice: (msg) => console.log(msg),
  writeCurrentPrompt: (msg) => console.log(msg),
  clearAndRedraw: () => {
    writeFullRedraw(
      [],
      '',
      getTerminalWidth(),
      buildSuggestionLines('', 0, getTerminalWidth()),
      []
    )
  },
}

export async function processInput(
  input: string,
  output: OutputAdapter = defaultOutput,
  interactiveUi = false
): Promise<boolean> {
  const writeCurrentPrompt = output.writeCurrentPrompt ?? output.log
  const trimmed = input.trim()
  if (trimmed === 'exit' || trimmed === '/exit') {
    if (interactiveUi) {
      output.log('Bye.')
    }
    return true
  }
  if (isInRecallSubstate() && trimmed === '/stop') {
    exitRecallMode()
    output.log('Stopped recall')
    return false
  }
  const contestablePromptId = getContestablePromptId()
  if (isInRecallSubstate() && trimmed === '/contest') {
    if (contestablePromptId == null) {
      writeCurrentPrompt('Type /stop to exit recall')
      return false
    }
    try {
      const outcome = await runInteractiveFetchWait(
        output,
        INTERACTIVE_FETCH_WAIT_LINES.contest,
        (signal) => contestAndRegenerate(contestablePromptId, signal)
      )
      if (!outcome.ok) {
        output.log(outcome.message)
        return false
      }
      showRecallPrompt(
        outcome.result as RecallPromptResult,
        output,
        writeCurrentPrompt
      )
    } catch (err) {
      logCancelledOrError(err, output)
    }
    return false
  }
  if (isInRecallSubstate() && trimmed.startsWith('/')) {
    writeCurrentPrompt(
      contestablePromptId
        ? 'Type /stop to exit, /contest to regenerate'
        : 'Type /stop to exit recall'
    )
    return false
  }
  if (trimmed === '/help') {
    output.log(formatHelp())
    return false
  }
  if (trimmed === '/clear') {
    output.clearAndRedraw?.()
    return false
  }
  if (await handleParamCommand(trimmed, output)) {
    return false
  }
  if (trimmed === '/list-access-token') {
    const tokens = listAccessTokens()
    if (tokens.length === 0) {
      output.log('No access tokens stored.')
    } else {
      for (const line of formatTokenLines(tokens, getDefaultTokenLabel())) {
        output.log(line)
      }
    }
    return false
  }
  if (trimmed === '/add gmail') {
    try {
      await runInteractiveFetchWait(
        output,
        INTERACTIVE_FETCH_WAIT_LINES.addGmail,
        (signal) => addGmailAccount(undefined, signal)
      )
    } catch (err) {
      logCancelledOrError(err, output)
    }
    return false
  }
  if (trimmed === '/last email') {
    try {
      const subject = await runInteractiveFetchWait(
        output,
        INTERACTIVE_FETCH_WAIT_LINES.lastEmail,
        (signal) => getLastEmailSubject(undefined, signal)
      )
      output.log(subject)
    } catch (err) {
      logCancelledOrError(err, output)
    }
    return false
  }
  if (pendingRecallLoadMore) {
    const answer = parseYesNo(trimmed)
    if (answer === true) {
      pendingRecallLoadMore = false
      recallSessionDueDays = 3
      await continueRecallSession(true, output, writeCurrentPrompt)
    } else if (answer === false) {
      pendingRecallLoadMore = false
      output.log(formatRecallSessionSummary(sessionRecallCount))
      endRecallSession()
    } else {
      writeCurrentPrompt('Please answer y or n')
      return false
    }
    return false
  }
  if (pendingRecallAnswer) {
    if (isMcqRecallPending(pendingRecallAnswer)) {
      const choiceNum = Number.parseInt(trimmed, 10)
      const { recallPromptId, choices } = pendingRecallAnswer
      const validRange = choiceNum >= 1 && choiceNum <= choices.length
      if (validRange) {
        try {
          const thinkingTimeMs = Date.now() - pendingRecallAnswer.shownAt
          const { correct } = await answerQuiz(
            recallPromptId,
            choiceNum - 1,
            thinkingTimeMs
          )
          output.log(correct ? 'Correct!' : 'Incorrect')
          output.log('Recalled successfully')
        } catch (err) {
          output.logError(err)
        }
        pendingRecallAnswer = null
        if (recallSessionMode)
          await continueRecallSession(false, output, writeCurrentPrompt)
      } else {
        writeCurrentPrompt(`Enter a number from 1 to ${choices.length}`)
        return false
      }
    } else if (isSpellingRecallPending(pendingRecallAnswer)) {
      const { recallPromptId } = pendingRecallAnswer
      if (!trimmed) {
        writeCurrentPrompt('Please type your spelling')
        return false
      }
      try {
        const thinkingTimeMs = Date.now() - pendingRecallAnswer.shownAt
        const { correct } = await answerSpelling(
          recallPromptId,
          trimmed,
          thinkingTimeMs
        )
        output.log(correct ? 'Correct!' : 'Incorrect')
        output.log('Recalled successfully')
      } catch (err) {
        output.logError(err)
      }
      pendingRecallAnswer = null
      if (recallSessionMode)
        await continueRecallSession(false, output, writeCurrentPrompt)
    } else {
      const { memoryTrackerId } = pendingRecallAnswer
      const answer = parseYesNo(trimmed)
      if (answer === true) {
        try {
          await markAsRecalled(memoryTrackerId, true)
          output.log('Recalled successfully')
        } catch (err) {
          output.logError(err)
        }
      } else if (answer === false) {
        try {
          await markAsRecalled(memoryTrackerId, false)
          output.log('Marked as not recalled')
        } catch (err) {
          output.logError(err)
        }
      } else {
        writeCurrentPrompt('Please answer y or n')
        return false
      }
      pendingRecallAnswer = null
      if (recallSessionMode)
        await continueRecallSession(false, output, writeCurrentPrompt)
    }
    return false
  }
  if (trimmed === '/recall-status') {
    try {
      const message = await runInteractiveFetchWait(
        output,
        INTERACTIVE_FETCH_WAIT_LINES.recallStatus,
        (signal) => recallStatus(signal)
      )
      output.log(message)
    } catch (err) {
      logCancelledOrError(err, output)
    }
    return false
  }
  if (trimmed === '/recall') {
    try {
      recallSessionMode = true
      sessionRecallCount = 0
      recallSessionDueDays = 0
      const result = await runInteractiveFetchWait(
        output,
        INTERACTIVE_FETCH_WAIT_LINES.recallNext,
        (signal) => recallNext(0, signal)
      )
      if (result.type === 'none') {
        pendingRecallLoadMore = true
        output.beginCurrentPrompt?.()
        writeCurrentPrompt('Load more from next 3 days? (y/n)')
      } else {
        showRecallPrompt(
          result as RecallPromptResult,
          output,
          writeCurrentPrompt
        )
      }
    } catch (err) {
      endRecallSessionAfterFailedRecallLoad(err, output)
    }
    return false
  }
  if (trimmed) {
    output.log('Not supported')
  }
  return false
}

export {
  visibleLength,
  renderBox,
  renderPastInput,
  isCommittedInteractiveInput,
  buildBoxLines,
  highlightRecognizedCommand,
} from './renderer.js'

function buildTTYDeps() {
  return {
    processInput,
    getPendingRecallAnswer,
    isPendingRecallStopConfirmation,
    setPendingRecallStopConfirmation,
    isInRecallSubstate,
    exitRecallMode,
    isMcqRecallPending,
    buildTokenListLines,
    getDefaultTokenLabel,
    listAccessTokens,
    removeAccessToken,
    removeAccessTokenCompletely,
    setDefaultTokenLabel,
    formatVersionOutput,
    buildBoxLines,
    buildCurrentPromptSeparator,
    buildLiveRegionLines,
    needsGapBeforeBox,
    buildSuggestionLines,
    getLastLine,
    wrapMarkdownTerminalToLines,
    recallMcqCurrentGuidanceLines,
    getTerminalWidth,
    renderFullDisplay,
    renderPastInput,
    GREY,
    HIDE_CURSOR,
    SHOW_CURSOR,
    CLEAR_SCREEN,
    RECALLING_INDICATOR,
    PROMPT,
    filterCommandsByPrefix,
    getTabCompletion,
    interactiveDocs,
    TOKEN_LIST_COMMANDS,
    getPlaceholderContext,
  }
}

function buildPipedDeps() {
  return {
    processInput,
    getTerminalWidth,
    buildBoxLines,
    buildSuggestionLines,
    renderBox,
    renderPastInput,
    formatVersionOutput,
    getPlaceholderContext,
  }
}

export async function runInteractive(stdin = process.stdin): Promise<void> {
  if (stdin.isTTY) {
    await runTTY(stdin, buildTTYDeps())
  } else {
    await runPiped(stdin, buildPipedDeps())
  }
}
