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
import { renderMarkdownToTerminal } from './markdown.js'
import {
  answerQuiz,
  contestAndRegenerate,
  formatRecallNotebookCurrentPromptLine,
  markAsRecalled,
  recallNext,
  resolveRecallNotebookTitle,
  type RecallNextResult,
} from './commands/recall.js'
import {
  getInteractiveFetchWaitLine,
  INTERACTIVE_FETCH_WAIT_LINES,
  resetInteractiveFetchWaitForTesting,
  runInteractiveFetchWait,
  type InteractiveFetchWaitLine,
} from './interactiveFetchWait.js'
import {
  parseRecallYesNoLine,
  recallStopConfirmInkModelForContext,
  RECALL_YES_NO_REPROMPT,
} from './interactions/recallYesNo.js'
import {
  formatMcqChoiceLinesWithIndices,
  getTerminalWidth,
  RECALL_SESSION_YES_NO_PLACEHOLDER,
  wrapMarkdownTerminalToLines,
  wrapTextToVisibleWidthLines,
  type PlaceholderContext,
} from './renderer.js'
import type {
  McqRecallPending,
  OutputAdapter,
  PendingRecallAnswer,
} from './types.js'

type RecallPromptResult = Exclude<RecallNextResult, { type: 'none' }>

const RECALL_SESSION_LOAD_MORE_LINE = 'Load more from next 3 days? (y/n)'

let pendingRecallAnswer: PendingRecallAnswer = null
let recallSessionMode = false
let sessionRecallCount = 0
let recallSessionDueDays = 0
let pendingRecallLoadMore = false
let pendingRecallStopConfirmation = false
/** TTY: lines shown in RecallInkConfirmPanel for session y/n (grey writeCurrentPrompt is covered by Ink’s live repaint). */
let recallSessionYesNoInkGuidanceLines: readonly string[] = []

export function resetRecallStateForTesting(): void {
  pendingRecallAnswer = null
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
  pendingRecallStopConfirmation = false
  recallSessionYesNoInkGuidanceLines = []
  resetInteractiveFetchWaitForTesting()
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
  recallSessionYesNoInkGuidanceLines = []
}

function formatRecallSessionSummary(count: number): string {
  if (count === 0) return '0 notes to recall today'
  if (count === 1) return 'Recalled 1 note'
  return `Recalled ${count} notes`
}

function isMcqRecallPending(p: PendingRecallAnswer): p is McqRecallPending {
  return p !== null && 'choices' in p
}

function getContestablePromptId(): number | null {
  return isMcqRecallPending(pendingRecallAnswer)
    ? pendingRecallAnswer.recallPromptId
    : null
}

function getPlaceholderContext(): PlaceholderContext {
  if (getInteractiveFetchWaitLine() !== null) return 'interactiveFetchWait'
  if (pendingRecallStopConfirmation) return 'recallStopConfirmation'
  if (pendingRecallLoadMore) return RECALL_SESSION_YES_NO_PLACEHOLDER
  if (isMcqRecallPending(pendingRecallAnswer)) return 'recallMcq'
  if (pendingRecallAnswer !== null) return RECALL_SESSION_YES_NO_PLACEHOLDER
  return 'default'
}

/**
 * Non-TTY MCQ: notebook line + stem on `writeCurrentPrompt`; numbered choices and
 * “Enter your choice…” on `log` (Current guidance analogue). Avoids sending choice lines through both hooks.
 */
function emitMcqRecallQuestionForNonInteractiveOutput(
  output: OutputAdapter,
  notebookTitle: string,
  stemRenderedForTerminal: string,
  choices: readonly string[]
): void {
  const writePrompt = output.writeCurrentPrompt ?? output.log
  writePrompt(formatRecallNotebookCurrentPromptLine(notebookTitle))
  writePrompt(stemRenderedForTerminal)
  const width = getTerminalWidth()
  for (const line of formatMcqChoiceLinesWithIndices(choices, width).lines) {
    output.log(line)
  }
  output.log(`Enter your choice (1-${choices.length}):`)
}

function emitSessionYesNoReprompt(
  output: OutputAdapter,
  writeCurrentPrompt: (msg: string) => void
): void {
  if (output.beginCurrentPrompt) {
    recallSessionYesNoInkGuidanceLines = [RECALL_YES_NO_REPROMPT]
  } else {
    writeCurrentPrompt(RECALL_YES_NO_REPROMPT)
  }
}

function enterRecallLoadMorePromptState(
  output: OutputAdapter,
  writeCurrentPrompt: (msg: string) => void
): void {
  pendingRecallLoadMore = true
  output.beginCurrentPrompt?.()
  if (output.beginCurrentPrompt) {
    recallSessionYesNoInkGuidanceLines = wrapTextToVisibleWidthLines(
      RECALL_SESSION_LOAD_MORE_LINE,
      getTerminalWidth()
    )
  } else {
    writeCurrentPrompt(RECALL_SESSION_LOAD_MORE_LINE)
  }
}

function showRecallPrompt(
  result: RecallPromptResult,
  output: OutputAdapter,
  writeCurrentPrompt: (msg: string) => void
): void {
  recallSessionYesNoInkGuidanceLines = []
  if (result.type === 'question') {
    const p = result.prompt
    if (p.questionType === 'MCQ' && p.multipleChoicesQuestion) {
      const mcq = p.multipleChoicesQuestion
      const stem = mcq.f0__stem ?? ''
      const choices = mcq.f1__choices ?? []
      const notebookTitle = resolveRecallNotebookTitle(p.notebook, p.note)
      const stemRenderedForTerminal = renderMarkdownToTerminal(stem)
      pendingRecallAnswer = {
        recallPromptId: p.id,
        choices,
        stemRenderedForTerminal,
        notebookTitle,
        shownAt: Date.now(),
      }
      if (!output.beginCurrentPrompt) {
        emitMcqRecallQuestionForNonInteractiveOutput(
          output,
          notebookTitle,
          stemRenderedForTerminal,
          choices
        )
      }
      return
    }
    if (p.questionType === 'SPELLING') {
      output.log('Not supported')
      exitRecallMode()
      return
    }
    return
  }
  output.beginCurrentPrompt?.()
  const width = getTerminalWidth()
  if (output.beginCurrentPrompt) {
    recallSessionYesNoInkGuidanceLines = [
      ...wrapTextToVisibleWidthLines(
        formatRecallNotebookCurrentPromptLine(result.notebookTitle),
        width
      ),
      ...wrapTextToVisibleWidthLines(result.title, width),
      ...(result.details
        ? wrapMarkdownTerminalToLines(
            renderMarkdownToTerminal(result.details),
            width
          )
        : []),
      ...wrapTextToVisibleWidthLines('Yes, I remember? (y/n)', width),
    ]
  } else {
    writeCurrentPrompt(
      formatRecallNotebookCurrentPromptLine(result.notebookTitle)
    )
    writeCurrentPrompt(result.title)
    if (result.details) {
      writeCurrentPrompt(renderMarkdownToTerminal(result.details))
    }
    writeCurrentPrompt('Yes, I remember? (y/n)')
  }
  pendingRecallAnswer = { memoryTrackerId: result.memoryTrackerId }
}

function endRecallSession(): void {
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
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
        enterRecallLoadMorePromptState(output, writeCurrentPrompt)
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
      showRecallPrompt(outcome.result, output, writeCurrentPrompt)
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
  if (await handleParamCommand(trimmed, output)) {
    return false
  }
  if (pendingRecallLoadMore) {
    const parsed = parseRecallYesNoLine(trimmed)
    if (parsed === 'yes') {
      pendingRecallLoadMore = false
      recallSessionDueDays = 3
      await continueRecallSession(true, output, writeCurrentPrompt)
    } else if (parsed === 'no') {
      pendingRecallLoadMore = false
      output.log(formatRecallSessionSummary(sessionRecallCount))
      endRecallSession()
    } else {
      emitSessionYesNoReprompt(output, writeCurrentPrompt)
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
    } else {
      const { memoryTrackerId } = pendingRecallAnswer
      const parsed = parseRecallYesNoLine(trimmed)
      if (parsed === 'yes') {
        try {
          await markAsRecalled(memoryTrackerId, true)
          output.log('Recalled successfully')
        } catch (err) {
          output.logError(err)
        }
      } else if (parsed === 'no') {
        try {
          await markAsRecalled(memoryTrackerId, false)
          output.log('Marked as not recalled')
        } catch (err) {
          output.logError(err)
        }
      } else {
        emitSessionYesNoReprompt(output, writeCurrentPrompt)
        return false
      }
      pendingRecallAnswer = null
      if (recallSessionMode)
        await continueRecallSession(false, output, writeCurrentPrompt)
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
        enterRecallLoadMorePromptState(output, writeCurrentPrompt)
      } else {
        showRecallPrompt(result, output, writeCurrentPrompt)
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

/** TTY Current prompt lines for an active recall question (MCQ stem block). */
function getRecallCurrentPromptWrappedLines(width: number): string[] | null {
  const p = pendingRecallAnswer
  if (isMcqRecallPending(p)) {
    return [
      ...wrapTextToVisibleWidthLines(
        formatRecallNotebookCurrentPromptLine(p.notebookTitle),
        width
      ),
      ...wrapMarkdownTerminalToLines(p.stemRenderedForTerminal, width),
    ]
  }
  return null
}

function getNumberedChoiceListChoices(): readonly string[] | null {
  const p = pendingRecallAnswer
  if (!isMcqRecallPending(p)) return null
  return p.choices
}

function buildInteractiveShellDeps() {
  return {
    processInput,
    isPendingStopConfirmation: isPendingRecallStopConfirmation,
    setPendingStopConfirmation: setPendingRecallStopConfirmation,
    isInCommandSessionSubstate: isInRecallSubstate,
    exitCommandSession: exitRecallMode,
    getRecallStopConfirmInkModel: recallStopConfirmInkModelForContext,
    getNumberedChoiceListChoices,
    getRecallCurrentPromptWrappedLines,
    shouldRecordCommittedLineInUserInputHistory: () =>
      pendingRecallAnswer === null,
    getPlaceholderContext,
    getRecallSessionYesNoInkGuidanceLines: () =>
      recallSessionYesNoInkGuidanceLines,
  }
}

export function runInteractive(stdin = process.stdin): void {
  if (!stdin.isTTY) {
    exitCliError('not a terminal (use version or update)')
  }
  runInteractiveTtySession(stdin, buildInteractiveShellDeps())
}
