import { Text, useInput, type Key } from 'ink'
import { useMemo } from 'react'
import {
  buildCommandInputDraftLines,
  PROMPT,
  type PlaceholderContext,
  truncateToWidth,
  visibleLength,
} from '../renderer.js'

type PatchedTextInputProps = {
  value: string
  caretOffset: number
  placeholderContext: PlaceholderContext
  placeholder: string
  maxWidth: number
  isActive: boolean
  onChange: (nextValue: string, nextCaretOffset: number) => void
  onSubmit: (value: string) => void
  onUnhandledKey: (input: string, key: Key) => void
}

export function PatchedTextInput({
  value,
  caretOffset,
  placeholderContext,
  placeholder,
  maxWidth,
  isActive,
  onChange,
  onSubmit,
  onUnhandledKey,
}: PatchedTextInputProps) {
  useInput(
    (input, key) => {
      if (key.return || input === '\r' || input === '\n') {
        onSubmit(value)
        return
      }
      if (
        key.escape ||
        key.tab ||
        (key.shift && key.tab) ||
        key.upArrow ||
        key.downArrow ||
        (key.ctrl && input === 'c')
      ) {
        onUnhandledKey(input, key)
        return
      }
      if (key.leftArrow) {
        onChange(value, Math.max(0, caretOffset - 1))
        return
      }
      if (key.rightArrow) {
        onChange(value, Math.min(value.length, caretOffset + 1))
        return
      }
      if (key.home) {
        onChange(value, 0)
        return
      }
      if (key.end) {
        onChange(value, value.length)
        return
      }
      if (key.backspace || key.delete) {
        const nextCaret = Math.max(0, caretOffset - 1)
        const nextValue =
          value.slice(0, nextCaret) +
          value.slice(Math.min(value.length, nextCaret + 1))
        onChange(nextValue, nextCaret)
        return
      }
      if (input.length > 0 && !key.ctrl && !key.meta) {
        const nextValue =
          value.slice(0, caretOffset) + input + value.slice(caretOffset)
        onChange(nextValue, caretOffset + input.length)
        return
      }
      onUnhandledKey(input, key)
    },
    { isActive }
  )

  const rendered = useMemo(() => {
    const withPrompt = buildCommandInputDraftLines(value, maxWidth, {
      placeholderContext,
      caretOffset,
    })[0]!
    const withoutPrompt = withPrompt.startsWith(PROMPT)
      ? withPrompt.slice(PROMPT.length)
      : withPrompt
    const prefixOnly = withoutPrompt === withPrompt
    const fallback =
      value.length === 0
        ? `\x1b[7m \x1b[27m\x1b[90m${placeholder}\x1b[0m`
        : value
    const body = prefixOnly ? fallback : withoutPrompt
    return truncateToWidth(body, Math.max(1, maxWidth - visibleLength(PROMPT)))
  }, [value, caretOffset, placeholderContext, placeholder, maxWidth])

  return <Text wrap="truncate-end">{rendered}</Text>
}
