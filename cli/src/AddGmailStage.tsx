import { useEffect } from 'react'
import { Box } from 'ink'
import { Spinner } from '@inkjs/ui'
import { runAddGmailInteractiveAssistantMessage } from './commands/addGmailSlashCommand.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'

export const ADD_GMAIL_STAGE_STATUS_LABEL = 'Connecting Gmail…'

export function AddGmailStage({
  onSettled,
}: {
  readonly onSettled: (assistantText: string) => void
}) {
  useEffect(() => {
    let cancelled = false
    const run = async () => {
      try {
        const text = await runAddGmailInteractiveAssistantMessage()
        if (!cancelled) {
          onSettled(text)
        }
      } catch (err: unknown) {
        if (!cancelled) {
          onSettled(userVisibleSlashCommandError(err))
        }
      }
    }
    run()
    return () => {
      cancelled = true
    }
  }, [onSettled])

  return (
    <Box>
      <Spinner label={ADD_GMAIL_STAGE_STATUS_LABEL} />
    </Box>
  )
}
