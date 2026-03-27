import { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import { StatusMessage } from '@inkjs/ui'
import type { RecallInkConfirmChoice } from '../interactions/recallYesNo.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

const RECALL_INK_CONFIRM_FOCUS_ID = 'recall-ink-confirm'

const INVALID_CHOICE_MESSAGE = 'Please press y or n'

function fireAdapterChoice(
  onResult: (r: RecallInkConfirmChoice) => void | Promise<void>,
  choice: RecallInkConfirmChoice
): void {
  Promise.resolve(onResult(choice)).catch(() => undefined)
}

type RecallInkConfirmPanelSharedProps = {
  guidanceLines: readonly string[]
  placeholderText: string
  onInputReadySignal: () => void
  onInterrupt: () => void
  onResult: (r: RecallInkConfirmChoice) => void | Promise<void>
}

type RecallInkConfirmPanelProps =
  | (RecallInkConfirmPanelSharedProps & { variant: 'stop-recall' })
  | (RecallInkConfirmPanelSharedProps & {
      variant: 'in-session'
      whenInActiveRecallSession: () => boolean
      onEscapeOpensStopRecallSheet: () => void
    })

export function RecallInkConfirmPanel(props: RecallInkConfirmPanelProps) {
  const { onInputReadySignal, guidanceLines, placeholderText } = props
  const [hint, setHint] = useState('')
  const skipNextOsc = useRef(true)
  const propsRef = useRef(props)
  propsRef.current = props

  const { isFocused } = useFocus({
    id: RECALL_INK_CONFIRM_FOCUS_ID,
    autoFocus: true,
  })
  const isFocusedRef = useRef(isFocused)
  isFocusedRef.current = isFocused
  const inkFocusEverEstablishedRef = useRef(false)
  if (isFocused) inkFocusEverEstablishedRef.current = true

  useEffect(() => {
    if (skipNextOsc.current) {
      skipNextOsc.current = false
      return
    }
    onInputReadySignal()
  }, [hint, onInputReadySignal])

  const handleKey = useCallback((input: string, key: Key) => {
    let dispatched = false
    const processOne = (inp: string, ky: Key) => {
      if (dispatched) return
      const p = propsRef.current
      if (ky.ctrl && inp === 'c') {
        dispatched = true
        p.onInterrupt()
        return
      }
      if (!isFocusedRef.current && inkFocusEverEstablishedRef.current) return

      const isEscape = ky.escape || ky.name === 'escape' || inp === '\u001b'
      if (isEscape) {
        if (p.variant === 'in-session' && p.whenInActiveRecallSession()) {
          dispatched = true
          p.onEscapeOpensStopRecallSheet()
          return
        }
        dispatched = true
        fireAdapterChoice(p.onResult, { result: 'cancel' })
        return
      }

      if (ky.return && !ky.shift) {
        dispatched = true
        fireAdapterChoice(p.onResult, { result: 'submit-no' })
        return
      }

      const ch = inp.length === 1 ? inp.toLowerCase() : ''
      if (ch === 'y') {
        dispatched = true
        fireAdapterChoice(p.onResult, { result: 'submit-yes' })
        return
      }
      if (ch === 'n') {
        dispatched = true
        fireAdapterChoice(p.onResult, { result: 'submit-no' })
        return
      }

      if (
        ky.backspace ||
        ky.delete ||
        ky.upArrow ||
        ky.downArrow ||
        ky.leftArrow ||
        ky.rightArrow ||
        ky.tab ||
        ky.pageUp ||
        ky.pageDown ||
        ky.home ||
        ky.end
      ) {
        return
      }

      if (inp.length > 0 && !ky.ctrl && !ky.meta) {
        setHint(INVALID_CHOICE_MESSAGE)
      }
    }

    eachLogicalInkStdinChunk(input, key, processOne)
  }, [])

  useInput(handleKey, { isActive: true })

  return (
    <Box flexDirection="column">
      {guidanceLines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
      {placeholderText ? <Text dimColor>{placeholderText}</Text> : null}
      {hint ? <StatusMessage variant="error">{hint}</StatusMessage> : null}
      <Text dimColor>y/N</Text>
    </Box>
  )
}
