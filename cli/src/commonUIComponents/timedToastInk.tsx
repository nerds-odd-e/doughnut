import { useCallback, useEffect, useRef, useState } from 'react'
import { Text } from 'ink'

export const DEFAULT_TIMED_TOAST_MS = 5000

export function TimedToastInk(props: { readonly message: string | null }) {
  if (props.message === null) return null
  return <Text color="yellow">{props.message}</Text>
}

export function useTimedToastInk(
  defaultDurationMs: number = DEFAULT_TIMED_TOAST_MS
) {
  const [message, setMessage] = useState<string | null>(null)
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const clearToast = useCallback(() => {
    if (timeoutRef.current !== null) {
      clearTimeout(timeoutRef.current)
      timeoutRef.current = null
    }
    setMessage(null)
  }, [])

  const showToast = useCallback(
    (text: string, durationMs: number = defaultDurationMs) => {
      if (timeoutRef.current !== null) {
        clearTimeout(timeoutRef.current)
      }
      setMessage(text)
      timeoutRef.current = setTimeout(() => {
        timeoutRef.current = null
        setMessage(null)
      }, durationMs)
    },
    [defaultDurationMs]
  )

  useEffect(() => {
    return () => {
      if (timeoutRef.current !== null) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  return { message, showToast, clearToast }
}
