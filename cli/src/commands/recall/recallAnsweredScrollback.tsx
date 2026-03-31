import { Text } from 'ink'
import type { ReactElement, ReactNode } from 'react'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'
import { RecallAnsweredBlockShell } from './recallAnsweredInkShared.js'

export function recallAnsweredPlainInk(text: string): ReactElement {
  return (
    <RecallAnsweredBlockShell>
      <Text>{text}</Text>
    </RecallAnsweredBlockShell>
  )
}

export function recallAnsweredScrollbackItem(
  element: ReactNode
): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element,
  }
}
