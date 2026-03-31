import { Static } from 'ink'
import type { ScrollbackEntry } from './scrollbackEntry.js'
import { ScrollbackLine } from './ScrollbackLine.js'

type SessionScrollbackProps = {
  readonly entries: readonly ScrollbackEntry[]
}

export function SessionScrollback({ entries }: SessionScrollbackProps) {
  return (
    <Static items={entries}>
      {(entry, index) => (
        <ScrollbackLine
          key={entry.id}
          entry={entry}
          nextEntry={entries[index + 1]}
        />
      )}
    </Static>
  )
}
