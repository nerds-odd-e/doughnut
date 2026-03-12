import * as readline from 'node:readline'
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
import { renderMarkdownToTerminal } from './markdown.js'
import {
  answerQuiz,
  answerSpelling,
  contestAndRegenerate,
  markAsRecalled,
  recallNext,
  recallStatus,
} from './recall.js'
import {
  filterCommandsByPrefix,
  formatCommandSuggestionsWithHighlight,
  formatHelp,
  interactiveDocs,
} from './help.js'
import { formatHighlightedList } from './listDisplay.js'
import { formatVersionOutput } from './version.js'

const GREY = '\x1b[90m'
const GREY_BG = '\x1b[48;5;236m'
const RESET = '\x1b[0m'
const PLACEHOLDER = '`exit` to quit.'
const PROMPT = '→ '

let pendingRecallAnswer:
  | { memoryTrackerId: number }
  | { recallPromptId: number; choices: string[] }
  | { recallPromptId: number; type: 'spelling' }
  | null = null

let recallSessionMode = false
let sessionRecallCount = 0
let recallSessionDueDays = 0
let pendingRecallLoadMore = false

export function resetRecallStateForTesting(): void {
  pendingRecallAnswer = null
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
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
}

async function continueRecallSession(fromLoadMore = false): Promise<void> {
  if (!fromLoadMore) sessionRecallCount++
  try {
    const result = await recallNext(recallSessionDueDays)
    if (result.type === 'none') {
      if (recallSessionDueDays === 0) {
        pendingRecallLoadMore = true
        console.log('Load more from next 3 days? (y/n)')
        return
      }
      const msg =
        sessionRecallCount === 0
          ? '0 notes to recall today'
          : sessionRecallCount === 1
            ? 'Recalled 1 note'
            : `Recalled ${sessionRecallCount} notes`
      console.log(msg)
      recallSessionMode = false
      sessionRecallCount = 0
      recallSessionDueDays = 0
      return
    }
    if (result.type === 'spelling') {
      console.log(`Spell: ${result.stem || '...'}`)
      pendingRecallAnswer = {
        recallPromptId: result.recallPromptId,
        type: 'spelling',
      }
      return
    }
    if (result.type === 'mcq') {
      console.log(result.stem)
      for (let i = 0; i < result.choices.length; i++) {
        console.log(`  ${i + 1}. ${result.choices[i]}`)
      }
      console.log(`Enter your choice (1-${result.choices.length}):`)
      pendingRecallAnswer = {
        recallPromptId: result.recallPromptId,
        choices: result.choices,
      }
      return
    }
    console.log(result.title)
    if (result.details) {
      console.log(renderMarkdownToTerminal(result.details))
    }
    console.log('Yes, I remember? (y/n)')
    pendingRecallAnswer = { memoryTrackerId: result.memoryTrackerId }
  } catch (err) {
    recallSessionMode = false
    sessionRecallCount = 0
    console.log(err instanceof Error ? err.message : String(err))
  }
}

function parseCommandWithRequiredParam(
  trimmed: string,
  command: string
): string | 'usage' | null {
  if (trimmed !== command && !trimmed.startsWith(`${command} `)) return null
  const param = trimmed.slice(command.length).trim()
  return param ? param : 'usage'
}

export async function processInput(input: string): Promise<boolean> {
  const trimmed = input.trim()
  if (trimmed === 'exit' || trimmed === '/exit') {
    return true
  }
  if (isInRecallSubstate() && trimmed === '/stop') {
    exitRecallMode()
    console.log('Stopped recall')
    return false
  }
  const contestableRecallPromptId =
    pendingRecallAnswer && 'recallPromptId' in pendingRecallAnswer
      ? pendingRecallAnswer.recallPromptId
      : null
  if (isInRecallSubstate() && trimmed === '/contest') {
    if (contestableRecallPromptId == null) {
      console.log('Type /stop to exit recall')
      return false
    }
    try {
      const outcome = await contestAndRegenerate(contestableRecallPromptId)
      if (!outcome.ok) {
        console.log(outcome.message)
        return false
      }
      const { result } = outcome
      if (result.type === 'mcq') {
        console.log(result.stem)
        for (let i = 0; i < result.choices.length; i++) {
          console.log(`  ${i + 1}. ${result.choices[i]}`)
        }
        console.log(`Enter your choice (1-${result.choices.length}):`)
        pendingRecallAnswer = {
          recallPromptId: result.recallPromptId,
          choices: result.choices,
        }
      } else if (result.type === 'spelling') {
        console.log(`Spell: ${result.stem || '...'}`)
        pendingRecallAnswer = {
          recallPromptId: result.recallPromptId,
          type: 'spelling',
        }
      }
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  if (isInRecallSubstate() && trimmed.startsWith('/')) {
    console.log(
      contestableRecallPromptId
        ? 'Type /stop to exit, /contest to regenerate'
        : 'Type /stop to exit recall'
    )
    return false
  }
  if (trimmed === '/help') {
    console.log(formatHelp())
    return false
  }
  const addTokenParam = parseCommandWithRequiredParam(
    trimmed,
    '/add-access-token'
  )
  if (addTokenParam !== null) {
    if (addTokenParam === 'usage') {
      console.log('Usage: /add-access-token <token>')
      return false
    }
    try {
      await addAccessToken(addTokenParam)
      console.log('Token added')
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  if (trimmed === '/list-access-token') {
    const tokens = listAccessTokens()
    if (tokens.length === 0) {
      console.log('No access tokens stored.')
    } else {
      for (const line of formatTokenLines(tokens, getDefaultTokenLabel())) {
        console.log(line)
      }
    }
    return false
  }
  const createTokenParam = parseCommandWithRequiredParam(
    trimmed,
    '/create-access-token'
  )
  if (createTokenParam !== null) {
    if (createTokenParam === 'usage') {
      console.log('Usage: /create-access-token <label>')
      return false
    }
    try {
      await createAccessToken(createTokenParam)
      console.log('Token created')
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  const removeCompletelyParam = parseCommandWithRequiredParam(
    trimmed,
    '/remove-access-token-completely'
  )
  if (removeCompletelyParam !== null) {
    if (removeCompletelyParam === 'usage') {
      console.log('Usage: /remove-access-token-completely <label>')
      return false
    }
    try {
      await removeAccessTokenCompletely(removeCompletelyParam)
      console.log(
        `Token "${removeCompletelyParam}" removed locally and from server.`
      )
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  const removeTokenParam = parseCommandWithRequiredParam(
    trimmed,
    '/remove-access-token'
  )
  if (removeTokenParam !== null) {
    if (removeTokenParam === 'usage') {
      console.log('Usage: /remove-access-token <label>')
      return false
    }
    if (removeAccessToken(removeTokenParam)) {
      console.log(`Token "${removeTokenParam}" removed.`)
    } else {
      console.log(`Token "${removeTokenParam}" not found.`)
    }
    return false
  }
  if (trimmed === '/add gmail') {
    try {
      await addGmailAccount()
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  if (trimmed === '/last email') {
    try {
      const subject = await getLastEmailSubject()
      console.log(subject)
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
  }
  if (pendingRecallLoadMore) {
    const answer = trimmed.toLowerCase()
    if (answer === 'y' || answer === 'yes') {
      pendingRecallLoadMore = false
      recallSessionDueDays = 3
      await continueRecallSession(true)
    } else if (answer === 'n' || answer === 'no') {
      pendingRecallLoadMore = false
      const msg =
        sessionRecallCount === 0
          ? '0 notes to recall today'
          : sessionRecallCount === 1
            ? 'Recalled 1 note'
            : `Recalled ${sessionRecallCount} notes`
      console.log(msg)
      recallSessionMode = false
      sessionRecallCount = 0
      recallSessionDueDays = 0
    } else {
      console.log('Please answer y or n')
      return false
    }
    return false
  }
  if (pendingRecallAnswer) {
    if ('choices' in pendingRecallAnswer) {
      const choiceNum = Number.parseInt(trimmed, 10)
      const { recallPromptId, choices } = pendingRecallAnswer
      const validRange = choiceNum >= 1 && choiceNum <= choices.length
      if (validRange) {
        try {
          const { correct } = await answerQuiz(recallPromptId, choiceNum - 1)
          console.log(correct ? 'Correct!' : 'Incorrect')
          console.log('Recalled successfully')
        } catch (err) {
          console.log(err instanceof Error ? err.message : String(err))
        }
        pendingRecallAnswer = null
        if (recallSessionMode) await continueRecallSession()
      } else {
        console.log(`Enter a number from 1 to ${choices.length}`)
        return false
      }
    } else if (
      'type' in pendingRecallAnswer &&
      pendingRecallAnswer.type === 'spelling'
    ) {
      const { recallPromptId } = pendingRecallAnswer
      if (!trimmed) {
        console.log('Please type your spelling')
        return false
      }
      try {
        const { correct } = await answerSpelling(recallPromptId, trimmed)
        console.log(correct ? 'Correct!' : 'Incorrect')
        console.log('Recalled successfully')
      } catch (err) {
        console.log(err instanceof Error ? err.message : String(err))
      }
      pendingRecallAnswer = null
      if (recallSessionMode) await continueRecallSession()
    } else {
      const answer = trimmed.toLowerCase()
      if (answer === 'y' || answer === 'yes') {
        try {
          await markAsRecalled(pendingRecallAnswer.memoryTrackerId, true)
          console.log('Recalled successfully')
        } catch (err) {
          console.log(err instanceof Error ? err.message : String(err))
        }
      } else if (answer === 'n' || answer === 'no') {
        try {
          await markAsRecalled(pendingRecallAnswer.memoryTrackerId, false)
          console.log('Marked as not recalled')
        } catch (err) {
          console.log(err instanceof Error ? err.message : String(err))
        }
      } else {
        console.log('Please answer y or n')
        return false
      }
      pendingRecallAnswer = null
      if (recallSessionMode) await continueRecallSession()
    }
    return false
  }
  if (trimmed === '/recall-status') {
    try {
      const message = await recallStatus()
      console.log(message)
    } catch (err) {
      console.log(err instanceof Error ? err.message : String(err))
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
        console.log('Load more from next 3 days? (y/n)')
      } else if (result.type === 'spelling') {
        console.log(`Spell: ${result.stem || '...'}`)
        pendingRecallAnswer = {
          recallPromptId: result.recallPromptId,
          type: 'spelling',
        }
      } else if (result.type === 'mcq') {
        console.log(result.stem)
        for (let i = 0; i < result.choices.length; i++) {
          console.log(`  ${i + 1}. ${result.choices[i]}`)
        }
        console.log(`Enter your choice (1-${result.choices.length}):`)
        pendingRecallAnswer = {
          recallPromptId: result.recallPromptId,
          choices: result.choices,
        }
      } else {
        console.log(result.title)
        if (result.details) {
          console.log(renderMarkdownToTerminal(result.details))
        }
        console.log('Yes, I remember? (y/n)')
        pendingRecallAnswer = { memoryTrackerId: result.memoryTrackerId }
      }
    } catch (err) {
      recallSessionMode = false
      sessionRecallCount = 0
      console.log(err instanceof Error ? err.message : String(err))
    }
    return false
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

export function renderPastInput(input: string, width: number): string {
  const innerWidth = width - 2
  const lines = input.split('\n')
  const emptyRow = `${GREY_BG}${' '.repeat(innerWidth)}${RESET}`
  const contentRows = lines.map(
    (line) => `${GREY_BG} ${padEndVisible(line, innerWidth - 1)}${RESET}`
  )
  return [emptyRow, ...contentRows, emptyRow, ''].join('\n')
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

function getTerminalWidth(): number {
  return process.stdout.columns || 80
}

const COMMANDS_HINT = `${GREY}  / commands${RESET}`
const RECALLING_INDICATOR = `${GREY}Recalling${RESET}`

function formatMcqChoiceLines(choices: string[]): string[] {
  return choices.map((c, i) => `  ${i + 1}. ${c}`)
}

function buildSuggestionLines(
  buffer: string,
  highlightIndex: number
): string[] {
  const bufferLines = buffer.split('\n')
  const lastLine = bufferLines[bufferLines.length - 1]
  if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
    const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
    return formatCommandSuggestionsWithHighlight(filtered, 8, highlightIndex)
  }
  return [COMMANDS_HINT]
}

async function runInteractiveTTY(stdin: NodeJS.ReadableStream): Promise<void> {
  console.log(formatVersionOutput())
  console.log()

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  readline.emitKeypressEvents(stdin)

  let buffer = ''
  let highlightIndex = 0
  let suggestionsDismissed = false
  let linesAboveCursor = 0
  let prevTotalLines = 0
  let tokenListItems: { label: string; token: string }[] | null = null
  let tokenHighlightIndex = 0
  let tokenListAction: 'set-default' | 'remove' | 'remove-completely' =
    'set-default'
  let mcqChoiceHighlightIndex = 0

  function drawBox() {
    const width = getTerminalWidth()
    const contentLines = buildBoxLines(buffer, width)
    const boxLines = renderBox(contentLines, width).split('\n')
    const mcqPending = pendingRecallAnswer && 'choices' in pendingRecallAnswer
    const suggestionLines = tokenListItems
      ? formatHighlightedList(
          formatTokenLines(tokenListItems, getDefaultTokenLabel()),
          8,
          tokenHighlightIndex
        )
      : mcqPending
        ? formatHighlightedList(
            formatMcqChoiceLines(
              (pendingRecallAnswer as { choices: string[] }).choices
            ),
            8,
            mcqChoiceHighlightIndex
          )
        : (() => {
            const bufferLines = buffer.split('\n')
            const lastLine = bufferLines[bufferLines.length - 1]
            const wouldShowSuggestions =
              lastLine.startsWith('/') && !lastLine.endsWith(' ')
            if (suggestionsDismissed && wouldShowSuggestions)
              return [COMMANDS_HINT]
            return buildSuggestionLines(buffer, highlightIndex)
          })()
    const recallingIndicator = isInRecallSubstate() ? [RECALLING_INDICATOR] : []
    const newTotalLines =
      boxLines.length + recallingIndicator.length + suggestionLines.length

    if (linesAboveCursor > 0) {
      process.stdout.write(`\x1b[${linesAboveCursor}A`)
    }
    process.stdout.write('\r')

    for (const line of boxLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    for (const line of recallingIndicator) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    for (const line of suggestionLines) {
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

  process.stdout.on('resize', drawBox)
  const removeResizeListener = () => process.stdout.off('resize', drawBox)

  stdin.on(
    'keypress',
    async (
      str: string,
      key: { name: string; shift?: boolean; ctrl?: boolean; meta?: boolean }
    ) => {
      if (key.ctrl && key.name === 'c') {
        process.stdout.write(`\x1b[${1}B\r\n`)
        removeResizeListener()
        process.exit(0)
      }
      const mcqPending = pendingRecallAnswer && 'choices' in pendingRecallAnswer
      if (mcqPending) {
        const { recallPromptId, choices } = pendingRecallAnswer as {
          recallPromptId: number
          choices: string[]
        }
        if (key.name === 'up' || key.name === 'down') {
          const n = choices.length
          mcqChoiceHighlightIndex =
            key.name === 'up'
              ? (mcqChoiceHighlightIndex - 1 + n) % n
              : (mcqChoiceHighlightIndex + 1) % n
          drawBox()
        } else if (key.name === 'return' && !key.shift) {
          if (buffer.trim() === '/stop') {
            exitRecallMode()
            buffer = ''
            mcqChoiceHighlightIndex = 0
            linesAboveCursor = 0
            prevTotalLines = 0
            process.stdout.write('Stopped recall\n')
            drawBox()
            return
          }
          if (buffer.trim() === '/contest') {
            buffer = ''
            if (linesAboveCursor > 0) {
              process.stdout.write(`\x1b[${linesAboveCursor}A`)
            }
            process.stdout.write('\r')
            for (let i = 0; i < prevTotalLines; i++) {
              process.stdout.write('\x1b[2K\n')
            }
            if (prevTotalLines > 1) {
              process.stdout.write(`\x1b[${prevTotalLines - 1}A`)
            }
            try {
              const outcome = await contestAndRegenerate(recallPromptId)
              if (!outcome.ok) {
                process.stdout.write(`${outcome.message}\n`)
              } else {
                const { result } = outcome
                if (result.type === 'mcq') {
                  process.stdout.write(`${result.stem}\n`)
                  for (let i = 0; i < result.choices.length; i++) {
                    process.stdout.write(`  ${i + 1}. ${result.choices[i]}\n`)
                  }
                  process.stdout.write(
                    `Enter your choice (1-${result.choices.length}):\n`
                  )
                  pendingRecallAnswer = {
                    recallPromptId: result.recallPromptId,
                    choices: result.choices,
                  }
                } else if (result.type === 'spelling') {
                  process.stdout.write(`Spell: ${result.stem || '...'}\n`)
                  pendingRecallAnswer = {
                    recallPromptId: result.recallPromptId,
                    type: 'spelling',
                  }
                }
              }
            } catch (err) {
              process.stdout.write(
                `${err instanceof Error ? err.message : String(err)}\n`
              )
            }
            mcqChoiceHighlightIndex = 0
            linesAboveCursor = 0
            prevTotalLines = 0
            drawBox()
            return
          }
          const choiceNum = Number.parseInt(buffer.trim(), 10)
          const validTyped = choiceNum >= 1 && choiceNum <= choices.length
          const choiceIndex = validTyped
            ? choiceNum - 1
            : mcqChoiceHighlightIndex
          if (linesAboveCursor > 0) {
            process.stdout.write(`\x1b[${linesAboveCursor}A`)
          }
          process.stdout.write('\r')
          for (let i = 0; i < prevTotalLines; i++) {
            process.stdout.write(`\x1b[2K\n`)
          }
          if (prevTotalLines > 1) {
            process.stdout.write(`\x1b[${prevTotalLines - 1}A`)
          }
          buffer = ''
          pendingRecallAnswer = null
          mcqChoiceHighlightIndex = 0
          linesAboveCursor = 0
          prevTotalLines = 0
          try {
            const { correct } = await answerQuiz(recallPromptId, choiceIndex)
            process.stdout.write(
              `${correct ? 'Correct!' : 'Incorrect'}\nRecalled successfully\n`
            )
          } catch (err) {
            process.stdout.write(
              `${err instanceof Error ? err.message : String(err)}\n`
            )
          }
          if (recallSessionMode) await continueRecallSession()
          drawBox()
        } else if (str && !key.ctrl && !key.meta) {
          buffer += str
          drawBox()
        } else if (key.name === 'backspace') {
          if (buffer.length > 0) {
            buffer = buffer.slice(0, -1)
            drawBox()
          }
        } else {
          drawBox()
        }
        return
      }
      if (tokenListItems) {
        if (key.name === 'up' || key.name === 'down') {
          const n = tokenListItems.length
          tokenHighlightIndex =
            key.name === 'up'
              ? (tokenHighlightIndex - 1 + n) % n
              : (tokenHighlightIndex + 1) % n
          drawBox()
        } else if (key.name === 'return' && !key.shift) {
          const selectedLabel = tokenListItems[tokenHighlightIndex]!.label
          if (linesAboveCursor > 0) {
            process.stdout.write(`\x1b[${linesAboveCursor}A`)
          }
          process.stdout.write('\r')
          for (let i = 0; i < prevTotalLines; i++) {
            process.stdout.write('\x1b[2K\n')
          }
          if (prevTotalLines > 1) {
            process.stdout.write(`\x1b[${prevTotalLines - 1}A`)
          }
          const action = tokenListAction
          tokenListItems = null
          tokenHighlightIndex = 0
          tokenListAction = 'set-default'
          linesAboveCursor = 0
          prevTotalLines = 0
          if (action === 'set-default') {
            setDefaultTokenLabel(selectedLabel)
            process.stdout.write(`Default token set to: ${selectedLabel}\n`)
          } else if (action === 'remove') {
            removeAccessToken(selectedLabel)
            process.stdout.write(`Token "${selectedLabel}" removed.\n`)
          } else {
            try {
              await removeAccessTokenCompletely(selectedLabel)
              process.stdout.write(
                `Token "${selectedLabel}" removed locally and from server.\n`
              )
            } catch (err) {
              process.stdout.write(
                `${err instanceof Error ? err.message : String(err)}\n`
              )
            }
          }
          drawBox()
        } else {
          tokenListItems = null
          tokenHighlightIndex = 0
          drawBox()
        }
        return
      }
      if (key.name === 'escape') {
        const bufferLines = buffer.split('\n')
        const lastLine = bufferLines[bufferLines.length - 1]
        const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
        const suggestionsVisible =
          lastLine.startsWith('/') &&
          !lastLine.endsWith(' ') &&
          filtered.length > 0
        if (suggestionsVisible) {
          highlightIndex = 0
          if (lastLine === '/') {
            buffer =
              bufferLines.length === 1
                ? ''
                : bufferLines.slice(0, -1).join('\n')
          } else {
            suggestionsDismissed = true
          }
          drawBox()
        }
        return
      }
      if (key.name === 'return') {
        if (key.shift) {
          buffer += '\n'
          drawBox()
        } else {
          const bufferLines = buffer.split('\n')
          const lastLine = bufferLines[bufferLines.length - 1]
          const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
          const suggestionsVisible =
            lastLine.startsWith('/') &&
            !lastLine.endsWith(' ') &&
            filtered.length > 0

          if (suggestionsVisible) {
            const selectedCommand = `${filtered[highlightIndex].usage} `
            buffer =
              bufferLines.slice(0, -1).concat(selectedCommand).join('\n') || ''
            highlightIndex = 0
            drawBox()
            return
          }

          const width = getTerminalWidth()
          const input = buffer
          buffer = ''

          if (linesAboveCursor > 0) {
            process.stdout.write(`\x1b[${linesAboveCursor}A`)
          }
          process.stdout.write('\r')
          for (let i = 0; i < prevTotalLines; i++) {
            process.stdout.write('\x1b[2K\n')
          }
          if (prevTotalLines > 1) {
            process.stdout.write(`\x1b[${prevTotalLines - 1}A`)
          }

          if (input.trim()) {
            process.stdout.write(renderPastInput(input, width))
            process.stdout.write('\n')
          }

          const trimmedInput = input.trim()
          const tokenSelectAction = (() => {
            if (trimmedInput === '/list-access-token') return 'set-default'
            if (trimmedInput === '/remove-access-token') return 'remove'
            if (trimmedInput === '/remove-access-token-completely')
              return 'remove-completely'
            return null
          })() as 'set-default' | 'remove' | 'remove-completely' | null
          if (tokenSelectAction) {
            const tokens = listAccessTokens()
            if (tokens.length === 0) {
              process.stdout.write('No access tokens stored.\n')
            } else {
              tokenListItems = tokens
              tokenListAction = tokenSelectAction
              const dl = getDefaultTokenLabel()
              tokenHighlightIndex = Math.max(
                0,
                tokens.findIndex((t) => t.label === dl)
              )
            }
            linesAboveCursor = 0
            prevTotalLines = 0
            drawBox()
            return
          }

          if (await processInput(input)) {
            removeResizeListener()
            process.exit(0)
          }
          linesAboveCursor = 0
          prevTotalLines = 0
          if (pendingRecallAnswer && 'choices' in pendingRecallAnswer) {
            mcqChoiceHighlightIndex = 0
          }
          drawBox()
        }
      } else if (key.name === 'backspace') {
        if (buffer.length > 0) {
          buffer = buffer.slice(0, -1)
          highlightIndex = 0
          suggestionsDismissed = false
          drawBox()
        }
      } else if (key.name === 'up' || key.name === 'down') {
        const bufferLines = buffer.split('\n')
        const lastLine = bufferLines[bufferLines.length - 1]
        const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
        const suggestionsVisible =
          lastLine.startsWith('/') &&
          !lastLine.endsWith(' ') &&
          filtered.length > 0
        if (suggestionsVisible) {
          const n = filtered.length
          highlightIndex =
            key.name === 'up'
              ? (highlightIndex - 1 + n) % n
              : (highlightIndex + 1) % n
          drawBox()
        }
      } else if (str && !key.ctrl && !key.meta) {
        buffer += str
        highlightIndex = 0
        suggestionsDismissed = false
        drawBox()
      }
    }
  )
}

async function runInteractivePiped(
  stdin: NodeJS.ReadableStream
): Promise<void> {
  const width = getTerminalWidth()
  console.log(formatVersionOutput())
  console.log()
  const hintLines = buildSuggestionLines('', 0)
  console.log(renderBox(buildBoxLines('', width), width))
  for (const line of hintLines) {
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

export async function runInteractive(
  stdin: NodeJS.ReadableStream = process.stdin
): Promise<void> {
  if (stdin.isTTY) {
    await runInteractiveTTY(stdin)
  } else {
    await runInteractivePiped(stdin)
  }
}
