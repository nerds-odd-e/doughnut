import { Box, Text } from 'ink'
import type { ReactElement, ReactNode } from 'react'
import { renderMarkdownToTerminal } from '../../markdown.js'

export const RECALL_ANSWERED_BREADCRUMB_SEP = ' › '

export const RECALL_ANSWERED_CORRECT_COPY = 'Correct!'
export const RECALL_ANSWERED_INCORRECT_COPY = 'Incorrect.'

export function recallAnsweredBreadcrumbText(
  titles: readonly string[]
): string {
  return titles.join(RECALL_ANSWERED_BREADCRUMB_SEP)
}

export function recallAnsweredMarkdownToDisplayLines(
  markdown: string,
  width: number
): readonly string[] {
  const md = markdown.trim()
  if (md.length === 0) {
    return []
  }
  const rendered = renderMarkdownToTerminal(md, width)
  if (rendered.length === 0) {
    return []
  }
  return rendered.split('\n')
}

export function recallAnsweredQuizOutcomeInk(correct: boolean): ReactElement {
  return correct ? (
    <Text color="green">{RECALL_ANSWERED_CORRECT_COPY}</Text>
  ) : (
    <Text color="red">{RECALL_ANSWERED_INCORRECT_COPY}</Text>
  )
}

export function RecallAnsweredBlockShell(props: {
  readonly children: ReactNode
}): ReactElement {
  return (
    <Box flexDirection="column" marginBottom={1}>
      {props.children}
    </Box>
  )
}
