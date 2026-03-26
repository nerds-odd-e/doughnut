import { Text, useInput, type Key } from 'ink'
import { useMemo } from 'react'
import { terminalChalk } from '../terminalChalk.js'

type PatchedTextInputProps = {
  value: string
  caretOffset: number
  placeholder: string
  isActive: boolean
  onChange: (nextValue: string, nextCaretOffset: number) => void
  onSubmit: (value: string) => void
  onUnhandledKey: (input: string, key: Key) => void
}

function renderWithCaret(value: string, caretOffset: number): string {
  const co = Math.max(0, Math.min(caretOffset, value.length))
  if (value.length === 0) return terminalChalk.inverse(' ')
  let index = 0
  let result = ''
  for (const char of value) {
    result += index === co ? terminalChalk.inverse(char) : char
    index++
  }
  if (co === value.length) {
    result += terminalChalk.inverse(' ')
  }
  return result
}

export function PatchedTextInput({
  value,
  caretOffset,
  placeholder,
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
    if (value.length > 0) return renderWithCaret(value, caretOffset)
    return `${terminalChalk.inverse(' ')}${terminalChalk.gray(placeholder)}`
  }, [value, caretOffset, placeholder])

  return <Text>{rendered}</Text>
}
