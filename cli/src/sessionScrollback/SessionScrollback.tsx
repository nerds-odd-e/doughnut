import { Static } from 'ink'
import { Fragment, type ReactNode } from 'react'

/**
 * Scrollback entries are pre-rendered Ink trees. Optional `endsWithUserLine` is only for
 * session scrollback append spacing (user block → following row); `<Static>` ignores it.
 */
export type SessionScrollbackItem = {
  readonly id: string
  readonly element: ReactNode
  readonly endsWithUserLine?: boolean
}

type SessionScrollbackProps = {
  readonly items: readonly SessionScrollbackItem[]
}

/** One `<Static>` region; callers supply `{ id, element }` per Ink’s item + keyed root pattern. */
export function SessionScrollback({ items }: SessionScrollbackProps) {
  return (
    <Static items={[...items]}>
      {(item) => <Fragment key={item.id}>{item.element}</Fragment>}
    </Static>
  )
}
