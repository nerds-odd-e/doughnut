import { useCallback, useRef, useState } from 'react'
import type { ComponentType } from 'react'
import type { InteractiveSlashCommandStageProps } from './interactiveSlashCommand.js'
import type { OpenSlashStageParams } from './interactiveSlashCommandDispatch.js'

export type ActiveSlashCommandStage = {
  readonly component: ComponentType<InteractiveSlashCommandStageProps>
  readonly stageIndicator?: string
}

export function useSlashCommandShellState(
  appendScrollbackAssistantTextMessage: (message: string) => void,
  appendScrollbackError: (message: string) => void
) {
  const [activeStage, setActiveStage] =
    useState<ActiveSlashCommandStage | null>(null)
  const stageArgumentRef = useRef<string | undefined>(undefined)

  const clearStage = useCallback(() => {
    setActiveStage(null)
    stageArgumentRef.current = undefined
  }, [])

  const handleStageSettled = useCallback(
    (assistantText: string) => {
      if (assistantText !== '') {
        appendScrollbackAssistantTextMessage(assistantText)
      }
      clearStage()
    },
    [appendScrollbackAssistantTextMessage, clearStage]
  )

  const handleStageAbortWithError = useCallback(
    (message: string) => {
      if (message !== '') {
        appendScrollbackError(message)
      }
      clearStage()
    },
    [appendScrollbackError, clearStage]
  )

  const openStage = useCallback((params: OpenSlashStageParams) => {
    setActiveStage(() => ({
      component: params.component,
      stageIndicator: params.stageIndicator,
    }))
  }, [])

  const setStageArgumentRef = useCallback((argument: string | undefined) => {
    stageArgumentRef.current = argument
  }, [])

  return {
    activeStage,
    stageArgumentRef,
    clearStage,
    handleStageSettled,
    handleStageAbortWithError,
    openStage,
    setStageArgumentRef,
  }
}
