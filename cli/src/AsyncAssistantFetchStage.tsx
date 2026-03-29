import { useEffect, useRef } from 'react'
import { Box } from 'ink'
import { Spinner } from '@inkjs/ui'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'

export function AsyncAssistantFetchStage({
  spinnerLabel,
  runAssistantMessage,
  onSettled,
}: {
  readonly spinnerLabel: string
  readonly runAssistantMessage: () => Promise<string>
  readonly onSettled: (assistantText: string) => void
}) {
  const runRef = useRef(runAssistantMessage)
  runRef.current = runAssistantMessage

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      try {
        const text = await runRef.current()
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
      <Spinner label={spinnerLabel} />
    </Box>
  )
}
