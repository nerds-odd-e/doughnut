import { useCallback, useRef, useState } from 'react'
import type { Key } from 'ink'

function dispatchLineBufferKeystroke(
  input: string,
  key: Key,
  bufferRef: { current: string },
  setBuffer: (next: string) => void,
  commitLine: () => void
): void {
  if (key.backspace || key.delete) {
    const next = bufferRef.current.slice(0, -1)
    bufferRef.current = next
    setBuffer(next)
    return
  }

  for (let i = 0; i < input.length; i++) {
    const ch = input[i]!
    if (ch === '\r') {
      commitLine()
      if (input[i + 1] === '\n') i++
      continue
    }
    if (ch === '\n') {
      commitLine()
      continue
    }
    const next = bufferRef.current + ch
    bufferRef.current = next
    setBuffer(next)
  }

  if (key.return) {
    commitLine()
  }
}

export function useInteractiveCliLineBuffer() {
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')

  const replaceBuffer = useCallback((next: string) => {
    bufferRef.current = next
    setBuffer(next)
  }, [])

  const readBuffer = useCallback(() => bufferRef.current, [])

  const applyInput = useCallback(
    (input: string, key: Key, onCommittedLine: (line: string) => void) => {
      const commitLine = () => {
        const line = bufferRef.current
        bufferRef.current = ''
        setBuffer('')
        onCommittedLine(line)
      }
      dispatchLineBufferKeystroke(input, key, bufferRef, setBuffer, commitLine)
    },
    []
  )

  return { buffer, applyInput, replaceBuffer, readBuffer }
}
