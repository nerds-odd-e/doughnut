import { Box, Text } from 'ink'
import type { ReactElement, ReactNode } from 'react'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'
import type { RecallJustReviewPayload } from './nextRecallCardLoad.js'

const JUST_REVIEW_BREADCRUMB_SEP = ' › '

function justReviewOutcomeLine(
  outcome: 'remembered' | 'reduced',
  noteTitle: string
): string {
  if (outcome === 'reduced') {
    return 'Reduced memory index.'
  }
  return `Reviewed: ${noteTitle}`
}

export function recallAnsweredPlainInk(text: string): ReactElement {
  return (
    <Box>
      <Text>{text}</Text>
    </Box>
  )
}

export type RecallAnsweredJustReviewInkOpts = {
  /** When `false`, omit note details markdown between breadcrumb and outcome. Default: show details. */
  readonly showDetails?: boolean
}

export function recallAnsweredJustReviewInk(
  payload: RecallJustReviewPayload,
  remembered: boolean,
  opts?: RecallAnsweredJustReviewInkOpts
): ReactElement {
  const showDetails = opts?.showDetails !== false
  const width = resolvedTerminalWidth()
  const crumb = payload.breadcrumbTitles.join(JUST_REVIEW_BREADCRUMB_SEP)
  const md = payload.detailsMarkdown.trim()
  const rendered =
    showDetails && md.length > 0 ? renderMarkdownToTerminal(md, width) : ''
  const detailLines =
    rendered.length > 0 ? rendered.split('\n') : ([] as string[])
  const outcome: 'remembered' | 'reduced' = remembered
    ? 'remembered'
    : 'reduced'
  return (
    <Box flexDirection="column">
      <Text>{crumb}</Text>
      {detailLines.map((line, i) => (
        <Text key={i}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>{justReviewOutcomeLine(outcome, payload.noteTitle)}</Text>
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
