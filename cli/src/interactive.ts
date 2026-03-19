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
import { addGmailAccount, getLastEmailSubject } from './gmail.js'
import {
  filterCommandsByPrefix,
  formatHelp,
  getTabCompletion,
  interactiveDocs,
} from './help.js'
import { formatHighlightedList } from './listDisplay.js'
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
import { formatVersionOutput } from './version.js'
import {
  buildBoxLines,
  buildCurrentPromptSeparator,
  buildSuggestionLines,
  buildTokenListLines,
  getLastLine,
  formatMcqChoiceLines,
  getTerminalWidth,
  renderBox,
  renderFullDisplay,
  renderPastInput,
  writeFullRedraw,
  grayBoxLinesForSelectionMode,
  isSelectionMode,
  GREY,
  HIDE_CURSOR,
  SHOW_CURSOR,
  CLEAR_SCREEN,
  RECALLING_INDICATOR,
  PROMPT,
  type PlaceholderContext,
} from './renderer.js'
import type { OutputAdapter } from './types.js'

type RecallPromptResult = Exclude<RecallNextResult, { type: 'none' }>
type McqPrompt = { recallPromptId: number; choices: string[]; shownAt: number }
type SpellingPrompt = {
  recallPromptId: number
  type: 'spelling'
  shownAt: number
}
type JustReviewPrompt = { memoryTrackerId: number }

type PendingRecallAnswer =
  | { memoryTrackerId: number }
  | { recallPromptId: number; choices: string[]; shownAt: number }
  | { recallPromptId: number; type: 'spelling'; shownAt: number }
  | null

let pendingRecallAnswer: PendingRecallAnswer = null
let recallSessionMode = false
let sessionRecallCount = 0
let recallSessionDueDays = 0
let pendingRecallLoadMore = false
let pendingRecallStopConfirmation = false

export function resetRecallStateForTesting(): void {
  pendingRecallAnswer = null
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
  pendingRecallStopConfirmation = false
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

function isMcqPrompt(p: unknown): p is McqPrompt {
  return p !== null && typeof p === 'object' && 'choices' in p
}

function isSpellingPrompt(p: PendingRecallAnswer): p is SpellingPrompt {
  return p !== null && 'type' in p && p.type === 'spelling'
}

function getContestablePromptId(): number | null {
  return isMcqPrompt(pendingRecallAnswer) ||
    isSpellingPrompt(pendingRecallAnswer)
    ? pendingRecallAnswer.recallPromptId
    : null
}

export function getPlaceholderContext(
  inTokenList: boolean
): PlaceholderContext {
  if (inTokenList) return 'tokenList'
  if (pendingRecallStopConfirmation) return 'recallStopConfirmation'
  if (pendingRecallLoadMore) return 'recallYesNo'
  if (isMcqPrompt(pendingRecallAnswer)) return 'recallMcq'
  if (isSpellingPrompt(pendingRecallAnswer)) return 'recallSpelling'
  if (pendingRecallAnswer !== null) return 'recallYesNo'
  return 'default'
}

function showRecallPrompt(
  result: RecallPromptResult,
  output: OutputAdapter,
  writeCurrentPrompt: (msg: string) => void
): void {
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
  if (result.type === 'mcq') {
    writeCurrentPrompt(renderMarkdownToTerminal(result.stem))
    for (const line of formatMcqChoiceLines(result.choices)) {
      writeCurrentPrompt(line)
    }
    writeCurrentPrompt(`Enter your choice (1-${result.choices.length}):`)
    pendingRecallAnswer = {
      recallPromptId: result.recallPromptId,
      choices: result.choices,
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

const PARAM_COMMANDS: Array<{
  command: string
  usage: string
  run: (param: string) => Promise<ParamCommandResult> | ParamCommandResult
}> = [
  {
    command: '/add-access-token',
    usage: 'Usage: /add-access-token <token>',
    run: async (param) => {
      await addAccessToken(param)
      return 'Token added'
    },
  },
  {
    command: '/create-access-token',
    usage: 'Usage: /create-access-token <label>',
    run: async (param) => {
      await createAccessToken(param)
      return 'Token created'
    },
  },
  {
    command: '/remove-access-token-completely',
    usage: 'Usage: /remove-access-token-completely <label>',
    run: async (param) => {
      await removeAccessTokenCompletely(param)
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

async function handleParamCommand(
  trimmed: string,
  output: OutputAdapter
): Promise<boolean> {
  for (const { command, usage, run } of PARAM_COMMANDS) {
    const param = parseCommandWithRequiredParam(trimmed, command)
    if (param === null) continue
    if (param === 'usage') {
      output.log(usage)
      return true
    }
    try {
      const msg = await run(param)
      if (msg) output.log(msg)
    } catch (err) {
      output.logError(err)
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
    const result = await recallNext(recallSessionDueDays)
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
    endRecallSession()
    output.logError(err)
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
  output: OutputAdapter = defaultOutput
): Promise<boolean> {
  const writeCurrentPrompt = output.writeCurrentPrompt ?? output.log
  const trimmed = input.trim()
  if (trimmed === 'exit' || trimmed === '/exit') {
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
      const outcome = await contestAndRegenerate(contestablePromptId)
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
      output.logError(err)
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
      await addGmailAccount()
    } catch (err) {
      output.logError(err)
    }
    return false
  }
  if (trimmed === '/last email') {
    try {
      const subject = await getLastEmailSubject()
      output.log(subject)
    } catch (err) {
      output.logError(err)
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
    if (isMcqPrompt(pendingRecallAnswer)) {
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
    } else if (isSpellingPrompt(pendingRecallAnswer)) {
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
      const { memoryTrackerId } = pendingRecallAnswer as JustReviewPrompt
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
      const message = await recallStatus()
      output.log(message)
    } catch (err) {
      output.logError(err)
    }
    return false
  }
  if (trimmed === '/recall') {
    try {
      recallSessionMode = true
      sessionRecallCount = 0
      recallSessionDueDays = 0
      const result = await recallNext(0)
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
      endRecallSession()
      output.logError(err)
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
    isMcqPrompt,
    buildTokenListLines,
    getDefaultTokenLabel,
    listAccessTokens,
    removeAccessToken,
    removeAccessTokenCompletely,
    setDefaultTokenLabel,
    formatVersionOutput,
    buildBoxLines,
    buildCurrentPromptSeparator,
    buildSuggestionLines,
    getLastLine,
    formatMcqChoiceLines,
    getTerminalWidth,
    renderBox,
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
    formatHighlightedList,
    TOKEN_LIST_COMMANDS,
    getPlaceholderContext,
    grayBoxLinesForSelectionMode,
    isSelectionMode,
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
  }
}

export async function runInteractive(
  stdin: NodeJS.ReadableStream = process.stdin
): Promise<void> {
  if (stdin.isTTY) {
    await runTTY(stdin, buildTTYDeps())
  } else {
    await runPiped(stdin, buildPipedDeps())
  }
}
