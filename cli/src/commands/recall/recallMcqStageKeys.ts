import type { MutableRefObject } from 'react'
import type { Key } from 'ink'
import {
  choiceIndexFromSelectListSubmitLine,
  handleSelectListInkKey,
} from '../../interactions/selectListInteraction.js'

function mcqInvalidChoiceHintMessage(choiceCount: number): string {
  return `Not a valid choice. Enter 1–${choiceCount}, use ↑↓, or /contest.`
}

type RecallMcqKeyHandlers = {
  readonly choiceCount: number
  readonly highlightIndex: number
  readonly getLineDraft: () => string
  readonly onSetHighlightIndex: (index: number) => void
  readonly onSubmitHighlightIndex: (index: number) => void
  readonly onSubmitContest: () => void
  readonly onSubmitChoiceIndex: (index: number) => void
  readonly onInvalidChoice: () => void
  readonly onEscape: () => void
  readonly onBackspace: () => void
  readonly onEditChar: (char: string) => void
}

function processRecallMcqKeyEvent(
  input: string,
  key: Key,
  handlers: RecallMcqKeyHandlers
): void {
  handleSelectListInkKey(
    input,
    key,
    handlers.getLineDraft(),
    handlers.highlightIndex,
    handlers.choiceCount,
    {
      kind: 'slash-and-number-or-highlight',
      choiceCount: handlers.choiceCount,
    },
    'signal-escape',
    {
      onSetHighlightIndex: handlers.onSetHighlightIndex,
      onSubmitHighlightIndex: handlers.onSubmitHighlightIndex,
      onSubmitWithLine: (line) => {
        if (line.trim() === '/contest') {
          handlers.onSubmitContest()
          return
        }
        const idx = choiceIndexFromSelectListSubmitLine(
          line,
          handlers.choiceCount
        )
        if (idx === null) return
        handlers.onSubmitChoiceIndex(idx)
      },
      onInvalidSelectListSubmitLine: handlers.onInvalidChoice,
      onEscapeSignaled: handlers.onEscape,
      onEditBackspace: handlers.onBackspace,
      onEditChar: handlers.onEditChar,
    }
  )
}

function handleRecallMcqInput(
  input: string,
  key: Key,
  handlers: RecallMcqKeyHandlers
): void {
  if (input.includes('\r') || input.includes('\n')) {
    const returnKey = { return: true } as Key
    const emptyKey = {} as Key
    for (const ch of input) {
      if (ch === '\r' || ch === '\n') {
        processRecallMcqKeyEvent('\r', returnKey, handlers)
      } else if (!(key.ctrl || key.meta)) {
        processRecallMcqKeyEvent(ch, emptyKey, handlers)
      }
    }
    return
  }

  processRecallMcqKeyEvent(input, key, handlers)
}

export function createRecallMcqInputHandler(args: {
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly choiceCount: number
  readonly highlightIndex: number
  readonly bufferRef: MutableRefObject<string>
  readonly setBuffer: (value: string) => void
  readonly setHighlightIndex: (index: number) => void
  readonly clearToast: () => void
  readonly showToast: (message: string) => void
  readonly runSubmit: (index: number) => Promise<void>
  readonly runContest: () => Promise<void>
  readonly onEscape: () => void
}): (input: string, key: Key) => void {
  return (input, key) => {
    if (args.inputBlockedRef.current) return

    const clearDraft = () => {
      args.clearToast()
      args.bufferRef.current = ''
      args.setBuffer('')
    }

    handleRecallMcqInput(input, key, {
      choiceCount: args.choiceCount,
      highlightIndex: args.highlightIndex,
      getLineDraft: () => args.bufferRef.current,
      onSetHighlightIndex: (index) => {
        args.clearToast()
        args.setHighlightIndex(index)
      },
      onSubmitHighlightIndex: (index) => {
        clearDraft()
        args.runSubmit(index).catch(() => undefined)
      },
      onSubmitContest: () => {
        clearDraft()
        args.runContest().catch(() => undefined)
      },
      onSubmitChoiceIndex: (index) => {
        clearDraft()
        args.runSubmit(index).catch(() => undefined)
      },
      onInvalidChoice: () => {
        args.showToast(mcqInvalidChoiceHintMessage(args.choiceCount))
      },
      onEscape: args.onEscape,
      onBackspace: () => {
        args.clearToast()
        const cur = args.bufferRef.current
        if (cur.length === 0) return
        const next = cur.slice(0, -1)
        args.bufferRef.current = next
        args.setBuffer(next)
      },
      onEditChar: (char) => {
        args.clearToast()
        const next = args.bufferRef.current + char
        args.bufferRef.current = next
        args.setBuffer(next)
      },
    })
  }
}
