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
  scrollbackAssistantTextMessageItem,
  scrollbackErrorItem,
  scrollbackUserMessageItem,
} from './interactiveCliTranscript.js'
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
  readonly appendScrollbackUserMessage: (text: string) => void
  readonly appendScrollbackAssistantTextMessage: (text: string) => void
  readonly appendScrollbackError: (text: string) => void
  readonly appendScrollbackItem: (item: SessionScrollbackItem) => void
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

  const appendScrollbackUserMessage = useCallback(
    (text: string) => {
      appendScrollbackItem(scrollbackUserMessageItem(text))
    },
    [appendScrollbackItem]
  )

  const appendScrollbackAssistantTextMessage = useCallback(
    (text: string) => {
      appendScrollbackItem(scrollbackAssistantTextMessageItem(text))
    },
    [appendScrollbackItem]
  )

  const appendScrollbackError = useCallback(
    (text: string) => {
      appendScrollbackItem(scrollbackErrorItem(text))
    },
    [appendScrollbackItem]
  )

  const api = useMemo(
    () => ({
      appendScrollbackUserMessage,
      appendScrollbackAssistantTextMessage,
      appendScrollbackError,
      appendScrollbackItem,
    }),
    [
      appendScrollbackUserMessage,
      appendScrollbackAssistantTextMessage,
      appendScrollbackError,
      appendScrollbackItem,
    ]
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
