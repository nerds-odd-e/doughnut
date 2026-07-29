import { useEffect, useRef } from 'react'
import type { InteractiveSlashCommandSettleProps } from './interactiveSlashCommand.js'

type UsageErrorStageProps = { readonly message: string } & Pick<
  InteractiveSlashCommandSettleProps,
  'onAbortWithError'
>

/**
 * Report a usage error without a spinner, since nothing is being waited for.
 */
export function UsageErrorStage({
  message,
  onAbortWithError,
}: UsageErrorStageProps) {
  const reported = useRef(false)

  useEffect(() => {
    if (reported.current) return
    reported.current = true
    onAbortWithError(message)
  }, [message, onAbortWithError])

  return null
}
