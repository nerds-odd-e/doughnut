import { Box, Text } from 'ink'
import type { ReactElement, ReactNode } from 'react'
import { renderMarkdownToTerminal } from '../../markdown.js'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'

export const RECALL_ANSWERED_BREADCRUMB_SEP = ' › '

export function recallAnsweredPlainInk(text: string): ReactElement {
  return (
    <Box>
      <Text>{text}</Text>
    </Box>
  )
}

export function recallAnsweredSpellingInk(args: {
  readonly breadcrumbTitles: readonly string[]
  readonly detailsMarkdown: string
  readonly spellingAnswerDisplay: string
  readonly correct: boolean
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = args.breadcrumbTitles.join(RECALL_ANSWERED_BREADCRUMB_SEP)
  const md = args.detailsMarkdown.trim()
  const rendered = md.length > 0 ? renderMarkdownToTerminal(md, width) : ''
  const detailLines =
    rendered.length > 0 ? rendered.split('\n') : ([] as string[])
  const ans = args.spellingAnswerDisplay
  return (
    <Box flexDirection="column">
      <Text>{crumb}</Text>
      {detailLines.map((line, i) => (
        <Text key={i}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>{`Your answer: ${ans}`}</Text>
      {args.correct ? (
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
