import { useCallback, useMemo } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { interactiveSlashCommands } from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommand } from './commands/interactiveSlashCommand.js'
import { useInteractiveCliLineBuffer } from './interactiveCliInput.js'

const DEFAULT_INTERACTIVE_GUIDANCE = '/ commands'

const GUIDANCE_LIST_MAX_VISIBLE = 5
const COMPLETION_USAGE_PAD = 20

function normalizedInteractiveDraft(draft: string): string {
  return draft.replace(/\n/g, ' ')
}

function getSlashTabCompletion(
  buffer: string,
  commands: readonly InteractiveSlashCommand[]
): { completed: string; count: number } {
  if (!buffer.startsWith('/')) return { completed: buffer, count: 0 }
  const matches = commands.filter((c) => c.doc.usage.startsWith(buffer))
  if (matches.length === 0) return { completed: buffer, count: 0 }
  if (matches.length === 1)
    return { completed: `${matches[0]!.doc.usage} `, count: 1 }
  const usages = matches.map((m) => m.doc.usage)
  let prefix = usages[0]!
  for (let i = 1; i < usages.length; i++) {
    while (!usages[i]!.startsWith(prefix) && prefix.length > 0) {
      prefix = prefix.slice(0, -1)
    }
  }
  if (prefix.length > buffer.length)
    return { completed: prefix, count: matches.length }
  return { completed: buffer, count: matches.length }
}

function filterSlashCommandsByPrefix(
  commands: readonly InteractiveSlashCommand[],
  prefix: string
): InteractiveSlashCommand[] {
  const searchTerm =
    prefix.startsWith('/') && prefix.length > 1 ? prefix.slice(1) : prefix
  if (!searchTerm) return [...commands]

  return [...commands]
    .filter((c) => c.doc.usage.includes(searchTerm))
    .sort((a, b) => {
      const aBegins =
        a.doc.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && a.doc.usage.startsWith(`/${searchTerm}`))
      const bBegins =
        b.doc.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && b.doc.usage.startsWith(`/${searchTerm}`))
      if (aBegins && !bBegins) return -1
      if (!aBegins && bBegins) return 1
      return 0
    })
}

type SlashGuidanceForInk =
  | { show: 'hint' }
  | { show: 'empty' }
  | {
      show: 'list'
      readonly rows: readonly {
        readonly usage: string
        readonly description: string
      }[]
      readonly highlightIndex: number
    }

function slashGuidanceForInk(draft: string): SlashGuidanceForInk {
  const p = normalizedInteractiveDraft(draft)
  if (!p.startsWith('/') || p.endsWith(' ')) return { show: 'hint' }
  const matches = filterSlashCommandsByPrefix(interactiveSlashCommands, p)
  if (matches.length === 0) return { show: 'empty' }
  const rows = matches.map((c) => ({
    usage: c.doc.usage,
    description: c.doc.description,
  }))
  return { show: 'list', rows, highlightIndex: 0 }
}

function visibleListRows(
  rows: readonly { readonly usage: string; readonly description: string }[],
  highlightIndex: number
): { readonly rows: typeof rows; readonly highlightIndex: number } {
  if (rows.length <= GUIDANCE_LIST_MAX_VISIBLE) {
    return { rows, highlightIndex }
  }
  return {
    rows: rows.slice(0, GUIDANCE_LIST_MAX_VISIBLE),
    highlightIndex: Math.min(highlightIndex, GUIDANCE_LIST_MAX_VISIBLE - 1),
  }
}

export function MainInteractivePrompt({
  onCommittedLine,
}: {
  readonly onCommittedLine: (line: string) => void
}) {
  const { buffer, applyInput, replaceBuffer, readBuffer } =
    useInteractiveCliLineBuffer()

  const guidance = useMemo(() => slashGuidanceForInk(buffer), [buffer])

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (key.tab) {
        const draft = normalizedInteractiveDraft(readBuffer())
        if (draft.startsWith('/') && !draft.endsWith(' ')) {
          const { completed } = getSlashTabCompletion(
            draft,
            interactiveSlashCommands
          )
          if (completed !== draft) {
            replaceBuffer(completed)
          }
        }
        return
      }
      applyInput(input, key, (line) => {
        if (line === '') {
          return
        }
        onCommittedLine(line)
      })
    },
    [applyInput, onCommittedLine, readBuffer, replaceBuffer]
  )

  useInput(handleInput)

  const listWindow =
    guidance.show === 'list'
      ? visibleListRows(guidance.rows, guidance.highlightIndex)
      : null

  return (
    <Box flexDirection="column">
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
      {guidance.show === 'hint' ? (
        <Text>{DEFAULT_INTERACTIVE_GUIDANCE}</Text>
      ) : listWindow ? (
        listWindow.rows.map((row, i) => (
          <Text key={`${row.usage}-${i}`}>
            {'  '}
            {i === listWindow.highlightIndex ? (
              <Text inverse>
                {row.usage.padEnd(COMPLETION_USAGE_PAD)}
                {row.description}
              </Text>
            ) : (
              <Text color="gray">
                {row.usage.padEnd(COMPLETION_USAGE_PAD)}
                {row.description}
              </Text>
            )}
          </Text>
        ))
      ) : null}
    </Box>
  )
}
