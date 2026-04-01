import {
  createElement,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import type { ComponentType } from 'react'
import type { Key } from 'ink'
import { Box, useApp, useInput, useStdout } from 'ink'
import { MainInteractivePrompt } from './mainInteractivePrompt/index.js'
import { resolveInteractiveSlashCommand } from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommandStageProps } from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'
import type { StageKeyHandler } from './commonUIComponents/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commonUIComponents/stageKeyForwardContext.js'
import {
  transcriptAssistantError,
  transcriptAssistantText,
  transcriptUserLine,
} from './sessionScrollback/interactiveCliTranscript.js'
import {
  SessionScrollback,
  type SessionScrollbackItem,
} from './sessionScrollback/SessionScrollback.js'
import { SessionScrollbackAppendProvider } from './sessionScrollback/sessionScrollbackAppendContext.js'
import { StageLiveHeaderInk } from './commonUIComponents/stageLiveHeaderInk.js'
import { inkTerminalColumns } from './terminalColumns.js'

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
    SessionScrollbackItem[]
  >(() => [transcriptAssistantText(formatVersionOutput())])
  const [activeStageComponent, setActiveStageComponent] =
    useState<ComponentType<InteractiveSlashCommandStageProps> | null>(null)
  const [activeStageIndicator, setActiveStageIndicator] = useState<
    string | undefined
  >(undefined)
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const appendScrollbackItem = useCallback((item: SessionScrollbackItem) => {
    setScrollbackItems((prev) => [
      ...prev,
      withLeadingGapAfterUserIfNeeded(prev, item),
    ])
  }, [])

  const appendScrollbackItems = useCallback(
    (items: readonly SessionScrollbackItem[]) => {
      if (items.length === 0) return
      setScrollbackItems((prev) => {
        const acc = [...prev]
        for (const item of items) {
          acc.push(withLeadingGapAfterUserIfNeeded(acc, item))
        }
        return acc
      })
    },
    []
  )

  const clearSlashStage = useCallback(() => {
    setActiveStageComponent(null)
    setActiveStageIndicator(undefined)
    stageArgumentRef.current = undefined
  }, [])

  const handleAsyncSlashSettled = useCallback(
    (assistantText: string) => {
      if (assistantText !== '') {
        setScrollbackItems((prev) => [
          ...prev,
          withLeadingGapAfterUserIfNeeded(
            prev,
            transcriptAssistantText(assistantText)
          ),
        ])
      }
      clearSlashStage()
    },
    [clearSlashStage]
  )

  const handleAsyncSlashAbortWithError = useCallback(
    (message: string) => {
      if (message !== '') {
        setScrollbackItems((prev) => [
          ...prev,
          withLeadingGapAfterUserIfNeeded(
            prev,
            transcriptAssistantError(message)
          ),
        ])
      }
      clearSlashStage()
    },
    [clearSlashStage]
  )

  const onCommittedLine = useCallback((line: string) => {
    const commitUserLineWithAssistant = (
      assistantText: string,
      isError = false
    ) => {
      const user = transcriptUserLine(line)
      const assistant = isError
        ? transcriptAssistantError(assistantText)
        : transcriptAssistantText(assistantText)
      setScrollbackItems((prev) => {
        const withUser = [...prev, withLeadingGapAfterUserIfNeeded(prev, user)]
        return [
          ...withUser,
          withLeadingGapAfterUserIfNeeded(withUser, assistant),
        ]
      })
    }

    const lineOfCommand = line.startsWith('/')
      ? line.slice(1)
      : line.trim() === 'exit'
        ? 'exit'
        : undefined

    if (!lineOfCommand) {
      commitUserLineWithAssistant('Not supported', true)
      return
    }

    const resolved = resolveInteractiveSlashCommand(lineOfCommand)
    if (!resolved) {
      commitUserLineWithAssistant('unsupported command', true)
      return
    }

    const { command, argument } = resolved
    const user = transcriptUserLine(line)
    setScrollbackItems((prev) => [
      ...prev,
      withLeadingGapAfterUserIfNeeded(prev, user),
    ])
    if ('stageComponent' in command) {
      const argumentMissing = argument === undefined || argument === ''
      const argSpec = command.argument
      if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
        const assistant = transcriptAssistantError(
          `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
        )
        setScrollbackItems((prev) => [
          ...prev,
          withLeadingGapAfterUserIfNeeded(prev, assistant),
        ])
        return
      }
      stageArgumentRef.current = argument
      const Stage = command.stageComponent
      const indicator = command.stageIndicator
      setActiveStageIndicator(
        indicator !== undefined && indicator !== '' ? indicator : undefined
      )
      // setState(fn) treats fn as updater; bare `Stage` would be called with prior state as props.
      setActiveStageComponent(() => Stage)
      return
    }
    const argumentMissing = argument === undefined || argument === ''
    const argSpec = command.argument
    if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
      const assistant = transcriptAssistantError(
        `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
      )
      setScrollbackItems((prev) => [
        ...prev,
        withLeadingGapAfterUserIfNeeded(prev, assistant),
      ])
      return
    }
    Promise.resolve(command.run(argument))
      .then((r) => {
        const assistant = transcriptAssistantText(r.assistantMessage)
        setScrollbackItems((prev) => [
          ...prev,
          withLeadingGapAfterUserIfNeeded(prev, assistant),
        ])
        if (command.literal === '/exit') setExitAfterCommit(true)
      })
      .catch((err: unknown) => {
        const assistant = transcriptAssistantError(
          userVisibleSlashCommandError(err)
        )
        setScrollbackItems((prev) => [
          ...prev,
          withLeadingGapAfterUserIfNeeded(prev, assistant),
        ])
      })
  }, [])

  const scrollbackAppendApi = useMemo(
    () => ({ appendScrollbackItem, appendScrollbackItems }),
    [appendScrollbackItem, appendScrollbackItems]
  )

  const { stdout } = useStdout()
  const liveRegionCols = inkTerminalColumns(stdout.columns)

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <SessionScrollbackAppendProvider value={scrollbackAppendApi}>
        <Box flexDirection="column">
          <SessionScrollback items={scrollbackItems} />
          {activeStageComponent && (
            <Box flexDirection="column">
              {activeStageIndicator !== undefined ? (
                <StageLiveHeaderInk
                  title={activeStageIndicator}
                  cols={liveRegionCols}
                />
              ) : null}
              {createElement(activeStageComponent, {
                argument: stageArgumentRef.current,
                onSettled: handleAsyncSlashSettled,
                onAbortWithError: handleAsyncSlashAbortWithError,
              })}
            </Box>
          )}
          {!exitAfterCommit && (
            <MainInteractivePrompt
              onCommittedLine={onCommittedLine}
              isActive={!activeStageComponent}
            />
          )}
        </Box>
      </SessionScrollbackAppendProvider>
    </SetStageKeyHandlerContext.Provider>
  )
}
