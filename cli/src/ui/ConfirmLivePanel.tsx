import { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import { ConfirmInput, StatusMessage } from '@inkjs/ui'
import type { SessionYesNoLineDispatchResult } from '../interactions/sessionYesNoInteraction.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

const CONFIRM_LIVE_INK_FOCUS_ID = 'confirm-live'

const INVALID_CHOICE_MESSAGE = 'Please press y or n'

export type ConfirmLivePanelProps = {
  guidanceLines: string[]
  placeholderText: string
  /** Session y/n: Esc opens nested stop-confirm when still in command session. */
  escapeToNestedStopConfirm: boolean
  isInCommandSessionSubstate: () => boolean
  onNestedStopConfirm: () => void
  onInputReadySignal: () => void
  onInterrupt: () => void
  onDispatchResult: (r: SessionYesNoLineDispatchResult) => void | Promise<void>
}

export function ConfirmLivePanel({
  guidanceLines,
  placeholderText,
  escapeToNestedStopConfirm,
  isInCommandSessionSubstate,
  onNestedStopConfirm,
  onInputReadySignal,
  onInterrupt,
  onDispatchResult,
}: ConfirmLivePanelProps) {
  const [hint, setHint] = useState('')
  const skipNextOsc = useRef(true)
  const propsRef = useRef({
    escapeToNestedStopConfirm,
    isInCommandSessionSubstate,
    onNestedStopConfirm,
    onInterrupt,
    onDispatchResult,
  })
  propsRef.current = {
    escapeToNestedStopConfirm,
    isInCommandSessionSubstate,
    onNestedStopConfirm,
    onInterrupt,
    onDispatchResult,
  }

  const { isFocused } = useFocus({
    id: CONFIRM_LIVE_INK_FOCUS_ID,
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

      if (ky.escape) {
        if (p.escapeToNestedStopConfirm && p.isInCommandSessionSubstate()) {
          dispatched = true
          p.onNestedStopConfirm()
          return
        }
        dispatched = true
        Promise.resolve(p.onDispatchResult({ result: 'cancel' })).catch(
          () => undefined
        )
        return
      }

      if (ky.return && !ky.shift) {
        dispatched = true
        Promise.resolve(p.onDispatchResult({ result: 'submit-no' })).catch(
          () => undefined
        )
        return
      }

      const ch = inp.length === 1 ? inp.toLowerCase() : ''
      if (ch === 'y') {
        dispatched = true
        Promise.resolve(p.onDispatchResult({ result: 'submit-yes' })).catch(
          () => undefined
        )
        return
      }
      if (ch === 'n') {
        dispatched = true
        Promise.resolve(p.onDispatchResult({ result: 'submit-no' })).catch(
          () => undefined
        )
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
      <ConfirmInput
        isDisabled
        defaultChoice="cancel"
        submitOnEnter
        onConfirm={() => undefined}
        onCancel={() => undefined}
      />
    </Box>
  )
}
