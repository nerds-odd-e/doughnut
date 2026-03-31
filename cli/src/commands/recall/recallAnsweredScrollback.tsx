import { Box, Text } from 'ink'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'
import type { RecallAnsweredRowPayload } from './recallAnsweredRowPayload.js'

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

export function RecallAnsweredRow(props: {
  readonly payload: RecallAnsweredRowPayload
}) {
  const { payload } = props
  switch (payload.kind) {
    case 'plain':
      return (
        <Box>
          <Text>{payload.text}</Text>
        </Box>
      )
    case 'just-review': {
      const width = resolvedTerminalWidth()
      const crumb = payload.breadcrumbTitles.join(JUST_REVIEW_BREADCRUMB_SEP)
      const md = payload.detailsMarkdown.trim()
      const rendered = md.length > 0 ? renderMarkdownToTerminal(md, width) : ''
      const detailLines =
        rendered.length > 0 ? rendered.split('\n') : ([] as string[])
      return (
        <Box flexDirection="column">
          <Text>{crumb}</Text>
          {detailLines.map((line, i) => (
            <Text key={i}>{line.length > 0 ? line : ' '}</Text>
          ))}
          <Text>
            {justReviewOutcomeLine(payload.outcome, payload.noteTitle)}
          </Text>
        </Box>
      )
    }
  }
}

export function recallAnsweredScrollbackItem(
  payload: RecallAnsweredRowPayload
): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element: <RecallAnsweredRow payload={payload} />,
  }
}
