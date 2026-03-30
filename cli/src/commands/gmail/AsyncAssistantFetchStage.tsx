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
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'

export function AsyncAssistantFetchStage({
  spinnerLabel,
  runAssistantMessage,
  onSettled,
}: {
  readonly spinnerLabel: string
  readonly runAssistantMessage: (signal: AbortSignal) => Promise<string>
  readonly onSettled: (assistantText: string) => void
}) {
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
    const settleOnce = (text: string) => {
      if (settled) return
      settled = true
      onSettled(text)
    }
    const run = async () => {
      try {
        const text = await runRef.current(ac.signal)
        if (!ac.signal.aborted) {
          settleOnce(text)
        }
      } catch (err: unknown) {
        settleOnce(userVisibleSlashCommandError(err))
      }
    }
    run().catch(() => undefined)
    return () => {
      ac.abort()
      abortControllerRef.current = null
    }
  }, [onSettled])

  return (
    <Box>
      <Spinner label={spinnerLabel} />
    </Box>
  )
}
