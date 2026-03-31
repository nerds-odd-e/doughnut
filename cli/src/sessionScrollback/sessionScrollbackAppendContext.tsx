import { createContext, useContext, type ReactNode } from 'react'
import type { SessionScrollbackItem } from './SessionScrollback.js'

export type SessionScrollbackAppendApi = {
  readonly appendScrollbackItem: (item: SessionScrollbackItem) => void
  readonly appendScrollbackItems: (
    items: readonly SessionScrollbackItem[]
  ) => void
}

const SessionScrollbackAppendContext = createContext<
  SessionScrollbackAppendApi | undefined
>(undefined)

export function SessionScrollbackAppendProvider(props: {
  readonly value: SessionScrollbackAppendApi
  readonly children: ReactNode
}) {
  const { value, children } = props
  return (
    <SessionScrollbackAppendContext.Provider value={value}>
      {children}
    </SessionScrollbackAppendContext.Provider>
  )
}

export function useSessionScrollbackAppend(): SessionScrollbackAppendApi {
  const api = useContext(SessionScrollbackAppendContext)
  if (api === undefined) {
    throw new Error(
      'useSessionScrollbackAppend must be used within SessionScrollbackAppendProvider'
    )
  }
  return api
}
