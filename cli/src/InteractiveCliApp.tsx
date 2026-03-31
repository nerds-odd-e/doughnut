import { createElement, useCallback, useEffect, useRef, useState } from 'react'
import type { ComponentType } from 'react'
import type { Key } from 'ink'
import { Box, Text, useApp, useInput } from 'ink'
import { MainInteractivePrompt } from './mainInteractivePrompt/index.js'
import { resolveInteractiveSlashCommand } from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommandStageProps } from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'
import type { StageKeyHandler } from './commands/accessToken/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commands/accessToken/stageKeyForwardContext.js'
import { SessionScrollback } from './sessionScrollback/SessionScrollback.js'

type InteractiveCliTranscriptItem =
  | { readonly kind: 'user_line'; readonly id: string; readonly text: string }
  | {
      readonly kind: 'assistant_text'
      readonly id: string
      readonly text: string
    }

function transcriptUserLine(text: string): InteractiveCliTranscriptItem {
  return { kind: 'user_line', id: crypto.randomUUID(), text }
}

function transcriptAssistantText(text: string): InteractiveCliTranscriptItem {
  return { kind: 'assistant_text', id: crypto.randomUUID(), text }
}

function InteractiveCliTranscriptItemView({
  entry,
  nextEntry,
}: {
  readonly entry: InteractiveCliTranscriptItem
  readonly nextEntry: InteractiveCliTranscriptItem | undefined
}) {
  if (entry.kind === 'user_line') {
    const gapBeforeAssistant = nextEntry?.kind === 'assistant_text'
    return (
      <Box flexDirection="column">
        <PastUserMessageBlock text={entry.text} />
        {gapBeforeAssistant ? <Box height={1} /> : null}
      </Box>
    )
  }
  return (
    <Box>
      <Text>{entry.text}</Text>
    </Box>
  )
}

export function InteractiveCliApp() {
  const { exit } = useApp()
  const stageKeyHandlerRef = useRef<StageKeyHandler | null>(null)
  const stageArgumentRef = useRef<string | undefined>(undefined)
  const setStageKeyHandler = useCallback((handler: StageKeyHandler | null) => {
    stageKeyHandlerRef.current = handler
  }, [])

  useInput(
    useCallback((input: string, key: Key) => {
      stageKeyHandlerRef.current?.(input, key)
    }, [])
  )
  const [scrollbackItems, setScrollbackItems] = useState<
    InteractiveCliTranscriptItem[]
  >(() => [transcriptAssistantText(formatVersionOutput())])
  const [activeStageComponent, setActiveStageComponent] =
    useState<ComponentType<InteractiveSlashCommandStageProps> | null>(null)
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const handleAsyncSlashSettled = useCallback((assistantText: string) => {
    const assistant = transcriptAssistantText(assistantText)
    setScrollbackItems((prev) => [...prev, assistant])
    setActiveStageComponent(null)
    stageArgumentRef.current = undefined
  }, [])

  const onCommittedLine = useCallback((line: string) => {
    const commitUserLineWithAssistant = (assistantText: string) => {
      const user = transcriptUserLine(line)
      const assistant = transcriptAssistantText(assistantText)
      setScrollbackItems((prev) => [...prev, user, assistant])
    }

    const lineOfCommand = line.startsWith('/')
      ? line.slice(1)
      : line.trim() === 'exit'
        ? 'exit'
        : undefined

    if (!lineOfCommand) {
      commitUserLineWithAssistant('Not supported')
      return
    }

    const resolved = resolveInteractiveSlashCommand(lineOfCommand)
    if (!resolved) {
      commitUserLineWithAssistant('unsupported command')
      return
    }

    const { command, argument } = resolved
    const user = transcriptUserLine(line)
    setScrollbackItems((prev) => [...prev, user])
    const Stage = command.stageComponent
    if (Stage) {
      const argumentMissing = argument === undefined || argument === ''
      const argSpec = command.argument
      if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
        const assistant = transcriptAssistantText(
          `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
        )
        setScrollbackItems((prev) => [...prev, assistant])
        return
      }
      stageArgumentRef.current = argument
      // setState(fn) treats fn as updater; bare `Stage` would be called with prior state as props.
      setActiveStageComponent(() => Stage)
      return
    }
    const argumentMissing = argument === undefined || argument === ''
    const argSpec = command.argument
    if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
      const assistant = transcriptAssistantText(
        `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
      )
      setScrollbackItems((prev) => [...prev, assistant])
      return
    }
    const run = command.run
    if (!run) {
      const assistant = transcriptAssistantText(
        'Internal error: command has no run handler.'
      )
      setScrollbackItems((prev) => [...prev, assistant])
      return
    }
    Promise.resolve(run(argument))
      .then((r) => {
        const assistant = transcriptAssistantText(r.assistantMessage)
        setScrollbackItems((prev) => [...prev, assistant])
        if (command.line === '/exit') setExitAfterCommit(true)
      })
      .catch((err: unknown) => {
        const assistant = transcriptAssistantText(
          userVisibleSlashCommandError(err)
        )
        setScrollbackItems((prev) => [...prev, assistant])
      })
  }, [])

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <Box flexDirection="column">
        <SessionScrollback items={scrollbackItems}>
          {(entry, index) => (
            <InteractiveCliTranscriptItemView
              key={entry.id}
              entry={entry}
              nextEntry={scrollbackItems[index + 1]}
            />
          )}
        </SessionScrollback>
        {activeStageComponent &&
          createElement(activeStageComponent, {
            argument: stageArgumentRef.current,
            onSettled: handleAsyncSlashSettled,
          })}
        {!exitAfterCommit && (
          <MainInteractivePrompt
            onCommittedLine={onCommittedLine}
            isActive={!activeStageComponent}
          />
        )}
      </Box>
    </SetStageKeyHandlerContext.Provider>
  )
}
