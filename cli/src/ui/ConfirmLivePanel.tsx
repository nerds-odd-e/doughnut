import { useEffect, useRef, useState, useCallback } from 'react'
import { useFocus, useInput, type Key } from 'ink'
import {
  dispatchRecallSessionConfirmKey,
  type SessionYesNoLineDispatchResult,
  type SessionYesNoLineEmptySubmit,
  type SessionYesNoLineKeyEvent,
} from '../interactions/sessionYesNoInteraction.js'
import { ConfirmDisplay } from './ConfirmDisplay.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

const CONFIRM_LIVE_INK_FOCUS_ID = 'confirm-live'

function isSubmitKeyInk(key: Key, input: string): boolean {
  return !!(key.return || input === '\n' || input === '\r')
}

function sessionKeyEventFromInk(
  input: string,
  key: Key,
  lineDraft: string
): SessionYesNoLineKeyEvent {
  const submitPressed = isSubmitKeyInk(key, input)
  let keyName: string | undefined
  if (key.escape) keyName = 'escape'
  else if (key.return || submitPressed) keyName = 'return'
  else if (key.backspace || key.delete) keyName = 'backspace'

  const bareLineEnding = input === '\n' || input === '\r'
  let str: string | undefined
  if (
    !(key.escape || key.return || key.backspace || key.delete) &&
    input.length > 0 &&
    !key.ctrl &&
    !key.meta &&
    !bareLineEnding
  ) {
    str = input
  }

  return {
    keyName,
    str,
    ctrl: !!key.ctrl,
    meta: !!key.meta,
    shift: !!key.shift,
    lineDraft,
    submitPressed,
  }
}

export type ConfirmLivePanelProps = {
  guidanceLines: string[]
  placeholderText: string
  emptySubmit: SessionYesNoLineEmptySubmit
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
  emptySubmit,
  escapeToNestedStopConfirm,
  isInCommandSessionSubstate,
  onNestedStopConfirm,
  onInputReadySignal,
  onInterrupt,
  onDispatchResult,
}: ConfirmLivePanelProps) {
  const [draft, setDraft] = useState('')
  const [hint, setHint] = useState('')
  const skipNextOsc = useRef(true)
  /** Authoritative draft for the next key; state can lag one frame behind stdin. */
  const draftRef = useRef('')
  const propsRef = useRef({
    emptySubmit,
    escapeToNestedStopConfirm,
    isInCommandSessionSubstate,
    onNestedStopConfirm,
    onInterrupt,
    onDispatchResult,
  })
  propsRef.current = {
    emptySubmit,
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
  }, [draft, hint, onInputReadySignal])

  const handleKey = useCallback((input: string, key: Key) => {
    const p0 = propsRef.current
    if (key.ctrl && input === 'c') {
      p0.onInterrupt()
      return
    }
    if (!isFocusedRef.current && inkFocusEverEstablishedRef.current) return

    const processOne = (inp: string, ky: Key) => {
      const p = propsRef.current
      const lineDraft = draftRef.current
      if (ky.ctrl && inp === 'c') {
        p.onInterrupt()
        return
      }
      if (ky.escape) {
        if (p.escapeToNestedStopConfirm && p.isInCommandSessionSubstate()) {
          p.onNestedStopConfirm()
          return
        }
        const ev = sessionKeyEventFromInk(inp, ky, lineDraft)
        Promise.resolve(
          p.onDispatchResult(dispatchRecallSessionConfirmKey(ev, p.emptySubmit))
        ).catch(() => undefined)
        return
      }

      const submitPressed = isSubmitKeyInk(ky, inp)
      if (
        p.emptySubmit === 'treat-as-invalid' &&
        submitPressed &&
        !ky.shift &&
        lineDraft.trim() === ''
      ) {
        return
      }

      const ev = sessionKeyEventFromInk(inp, ky, lineDraft)
      const dispatch = dispatchRecallSessionConfirmKey(ev, p.emptySubmit)

      switch (dispatch.result) {
        case 'edit-backspace': {
          const next = lineDraft.slice(0, -1)
          draftRef.current = next
          setDraft(next)
          setHint('')
          return
        }
        case 'edit-char': {
          const next = lineDraft + dispatch.char
          draftRef.current = next
          setDraft(next)
          setHint('')
          return
        }
        case 'invalid-submit':
          draftRef.current = ''
          setDraft('')
          setHint(dispatch.hint)
          return
        case 'redraw':
          return
        default:
          Promise.resolve(p.onDispatchResult(dispatch)).catch(() => undefined)
      }
    }

    eachLogicalInkStdinChunk(input, key, processOne)
  }, [])

  useInput(handleKey, { isActive: true })

  return (
    <ConfirmDisplay
      guidanceLines={guidanceLines}
      placeholderText={placeholderText}
      hint={hint}
      draft={draft}
    />
  )
}
