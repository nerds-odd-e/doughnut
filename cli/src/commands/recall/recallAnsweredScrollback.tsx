import { Box, Text } from 'ink'
import type { RecallPrompt } from 'doughnut-api'
import type { ReactElement, ReactNode } from 'react'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'
import { numberedMcqMarkdownLinesForTerminal } from './numberedMcqMarkdownLines.js'

export const RECALL_ANSWERED_BREADCRUMB_SEP = ' › '

export function recallAnsweredPlainInk(text: string): ReactElement {
  return (
    <Box>
      <Text>{text}</Text>
    </Box>
  )
}

export function recallAnsweredMcqInk(args: {
  readonly breadcrumbTitles: readonly string[]
  readonly stem: string
  readonly choices: readonly string[]
  readonly selectedChoiceIndex: number
  readonly updatedPrompt: RecallPrompt
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = args.breadcrumbTitles.join(RECALL_ANSWERED_BREADCRUMB_SEP)
  const correct = args.updatedPrompt.answer?.correct === true
  const fromPredefined =
    args.updatedPrompt.predefinedQuestion?.correctAnswerIndex
  const correctChoiceIndex =
    fromPredefined !== undefined && fromPredefined !== null
      ? fromPredefined
      : correct && args.updatedPrompt.answer?.choiceIndex !== undefined
        ? args.updatedPrompt.answer.choiceIndex
        : undefined

  const stemRendered = renderMarkdownToTerminal(args.stem.trim(), width)
  const stemLines =
    stemRendered.length > 0 ? stemRendered.split('\n') : ([] as string[])
  const listLines = numberedMcqMarkdownLinesForTerminal(args.choices, width)
  const sel = args.selectedChoiceIndex

  const choiceLineColor = (itemIndex: number): undefined | 'green' | 'red' => {
    if (correctChoiceIndex !== undefined && itemIndex === correctChoiceIndex) {
      return 'green'
    }
    if (!correct && itemIndex === sel) {
      return 'red'
    }
    return undefined
  }

  return (
    <Box flexDirection="column">
      <Text>{crumb}</Text>
      {stemLines.map((line, i) => (
        <Text key={`st-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
      {listLines.map((line, i) => (
        <Text key={`ch-${i}`} color={choiceLineColor(line.itemIndex)}>
          {line.text}
        </Text>
      ))}
      {correct ? (
        <Text color="green">Correct!</Text>
      ) : (
        <Text color="red">Incorrect.</Text>
      )}
    </Box>
  )
}

export function recallAnsweredScrollbackItem(
  element: ReactNode
): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element,
  }
}
