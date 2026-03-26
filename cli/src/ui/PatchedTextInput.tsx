import { Text, useInput, type Key } from 'ink'
import { useMemo } from 'react'
import {
  buildCommandInputDraftLines,
  PROMPT,
  type PlaceholderContext,
  truncateToWidth,
  visibleLength,
} from '../renderer.js'
import { applyPatchedTextInputKey } from './patchedTextInputKey.js'

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
      const result = applyPatchedTextInputKey(
        { value, caretOffset },
        input,
        key
      )
      if (result.kind === 'submit') {
        onSubmit(value)
        return
      }
      if (result.kind === 'unhandled') {
        onUnhandledKey(input, key)
        return
      }
      onChange(result.next.value, result.next.caretOffset)
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
