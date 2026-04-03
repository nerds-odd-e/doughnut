import {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
} from 'react'
import type { Key } from 'ink'
import { Box, useInput } from 'ink'
import { Spinner } from '@inkjs/ui'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from '../../commonUIComponents/stageKeyForwardContext.js'

type AsyncAssistantFetchStageProps = Pick<
  InteractiveSlashCommandStageProps,
  'onSettled' | 'onAbortWithError'
> & {
  readonly spinnerLabel: string
  readonly runAssistantMessage: (signal: AbortSignal) => Promise<string>
  /**
   * When set, invoked on successful fetch instead of `onSettled` (e.g. stages
   * that transition to another UI rather than posting an assistant line).
   */
  readonly onFetchSuccess?: () => void
}

export function AsyncAssistantFetchStage({
  spinnerLabel,
  runAssistantMessage,
  onSettled,
  onAbortWithError,
  onFetchSuccess,
}: AsyncAssistantFetchStageProps) {
  const runRef = useRef(runAssistantMessage)
  runRef.current = runAssistantMessage

  const abortControllerRef = useRef<AbortController | null>(null)
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)

  const handleStageInput = useCallback((input: string, key: Key) => {
    const isEscape =
      key.escape === true || key.name === 'escape' || input === '\u001b'
    if (!isEscape) return
    abortControllerRef.current?.abort()
  }, [])

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    setStageKeyHandler(handleStageInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleStageInput])

  useInput(handleStageInput, {
    isActive: setStageKeyHandler === undefined,
  })

  useEffect(() => {
    const ac = new AbortController()
    abortControllerRef.current = ac
    let settled = false
    const settleSuccess = (text: string) => {
      if (settled) return
      settled = true
      if (onFetchSuccess !== undefined) {
        onFetchSuccess()
      } else {
        onSettled(text)
      }
    }
    const settleError = (text: string) => {
      if (settled) return
      settled = true
      onAbortWithError(text)
    }
    const run = async () => {
      try {
        const text = await runRef.current(ac.signal)
        if (!ac.signal.aborted) {
          settleSuccess(text)
        }
      } catch (err: unknown) {
        settleError(userVisibleSlashCommandError(err))
      }
    }
    run().catch(() => undefined)
    return () => {
      ac.abort()
      abortControllerRef.current = null
    }
  }, [onSettled, onAbortWithError, onFetchSuccess])

  return (
    <Box>
      <Spinner label={spinnerLabel} />
    </Box>
  )
}
