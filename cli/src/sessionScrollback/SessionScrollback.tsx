import { Static } from 'ink'
import type { ReactNode } from 'react'

/** Minimal shape for Ink `<Static>` item keys; scrollback layer stays content-agnostic. */
export type SessionScrollbackItem = {
  readonly id: string
}

type SessionScrollbackProps<T extends SessionScrollbackItem> = {
  readonly items: readonly T[]
  readonly children: (item: T, index: number) => ReactNode
}

export function SessionScrollback<T extends SessionScrollbackItem>({
  items,
  children: renderItem,
}: SessionScrollbackProps<T>) {
  return (
    <Static items={items as T[]}>
      {(item, index) => renderItem(item, index)}
    </Static>
  )
}
