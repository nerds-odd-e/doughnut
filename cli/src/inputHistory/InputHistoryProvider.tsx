import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  type MutableRefObject,
  type ReactNode,
} from 'react'
import { getConfigDir } from '../configDir.js'
import { appendUserInputHistoryLine } from '../mainInteractivePrompt/history.js'
import {
  loadUserInputHistory,
  saveUserInputHistory,
} from './persistUserInputHistory.js'

export type InputHistoryContextValue = {
  readonly linesRef: MutableRefObject<string[]>
  readonly appendCommittedLine: (masked: string) => void
}

const InputHistoryContext = createContext<InputHistoryContextValue | null>(null)

export function InputHistoryProvider({
  children,
}: {
  readonly children: ReactNode
}) {
  const linesRef = useRef<string[]>([])

  useEffect(() => {
    linesRef.current = loadUserInputHistory(getConfigDir())
  }, [])

  const appendCommittedLine = useCallback((masked: string) => {
    linesRef.current = appendUserInputHistoryLine(linesRef.current, masked)
    saveUserInputHistory(getConfigDir(), linesRef.current)
  }, [])

  const value = useMemo<InputHistoryContextValue>(
    () => ({ linesRef, appendCommittedLine }),
    [appendCommittedLine]
  )

  return (
    <InputHistoryContext.Provider value={value}>
      {children}
    </InputHistoryContext.Provider>
  )
}

export function useInputHistory(): InputHistoryContextValue {
  const ctx = useContext(InputHistoryContext)
  if (ctx === null) {
    throw new Error('useInputHistory must be used within InputHistoryProvider')
  }
  return ctx
}
