import { useCallback, useEffect, useMemo, useRef } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import {
  formatNumberedListForTerminal,
  resolvedTerminalWidth,
} from '../terminalColumns.js'
import { getStoredAccessTokenLabels } from './accessToken.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from './interactiveSlashCommand.js'

const listAccessTokenDoc: CommandDoc = {
  name: '/list-access-token',
  usage: '/list-access-token',
  description: 'List stored Doughnut API access tokens',
}

function buildListAccessTokenAssistantMessage(
  labels: readonly string[]
): string {
  if (labels.length === 0) {
    return 'No access tokens stored.'
  }
  const list = formatNumberedListForTerminal(labels, resolvedTerminalWidth())
  return `Stored access tokens:\n\n${list}`
}

export function ListAccessTokenStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const labels = useMemo(() => getStoredAccessTokenLabels(), [])
  const emptySettledRef = useRef(false)

  useEffect(() => {
    if (labels.length > 0 || emptySettledRef.current) return
    emptySettledRef.current = true
    onSettled('No access tokens stored.')
  }, [labels.length, onSettled])

  const listText = useMemo(
    () =>
      labels.length === 0
        ? ''
        : formatNumberedListForTerminal(labels, resolvedTerminalWidth()),
    [labels]
  )

  const handleInput = useCallback(
    (_input: string, key: Key) => {
      if (key.escape) {
        onSettled(buildListAccessTokenAssistantMessage(labels))
      }
    },
    [labels, onSettled]
  )

  useInput(handleInput)

  return (
    <Box flexDirection="column">
      {labels.length === 0 ? (
        <Text>No access tokens stored.</Text>
      ) : (
        <>
          <Text>Stored access tokens:</Text>
          <Text>{listText}</Text>
        </>
      )}
    </Box>
  )
}

export const listAccessTokenSlashCommand: InteractiveSlashCommand = {
  line: '/list-access-token',
  doc: listAccessTokenDoc,
  stageComponent: ListAccessTokenStage,
}
