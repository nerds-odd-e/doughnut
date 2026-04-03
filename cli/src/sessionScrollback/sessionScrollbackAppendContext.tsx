import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { Box } from 'ink'
import {
  SessionScrollback,
  type SessionScrollbackItem,
} from './SessionScrollback.js'

function withLeadingGapAfterUserIfNeeded(
  prev: readonly SessionScrollbackItem[],
  item: SessionScrollbackItem
): SessionScrollbackItem {
  const last = prev[prev.length - 1]
  if (last?.endsWithUserLine !== true) return item
  return {
    id: item.id,
    element: (
      <Box flexDirection="column">
        <Box height={1} />
        {item.element}
      </Box>
    ),
    endsWithUserLine: item.endsWithUserLine,
  }
}

export type SessionScrollbackAppendApi = {
  readonly appendScrollbackItem: (item: SessionScrollbackItem) => void
  readonly appendScrollbackItems: (
    items: readonly SessionScrollbackItem[]
  ) => void
}

const SessionScrollbackAppendContext = createContext<
  SessionScrollbackAppendApi | undefined
>(undefined)

export function SessionScrollbackSessionProvider(props: {
  readonly initialItems: readonly SessionScrollbackItem[]
  readonly children: ReactNode
}) {
  const { initialItems, children } = props
  const [items, setItems] = useState<SessionScrollbackItem[]>(() => [
    ...initialItems,
  ])

  const appendScrollbackItem = useCallback((item: SessionScrollbackItem) => {
    setItems((prev) => [...prev, withLeadingGapAfterUserIfNeeded(prev, item)])
  }, [])

  const appendScrollbackItems = useCallback(
    (toAppend: readonly SessionScrollbackItem[]) => {
      if (toAppend.length === 0) return
      setItems((prev) => {
        const acc = [...prev]
        for (const item of toAppend) {
          acc.push(withLeadingGapAfterUserIfNeeded(acc, item))
        }
        return acc
      })
    },
    []
  )

  const api = useMemo(
    () => ({ appendScrollbackItem, appendScrollbackItems }),
    [appendScrollbackItem, appendScrollbackItems]
  )

  return (
    <SessionScrollbackAppendContext.Provider value={api}>
      <Box flexDirection="column">
        <SessionScrollback items={items} />
        {children}
      </Box>
    </SessionScrollbackAppendContext.Provider>
  )
}

export function useSessionScrollbackAppend(): SessionScrollbackAppendApi {
  const api = useContext(SessionScrollbackAppendContext)
  if (api === undefined) {
    throw new Error(
      'useSessionScrollbackAppend must be used within SessionScrollbackSessionProvider'
    )
  }
  return api
}
