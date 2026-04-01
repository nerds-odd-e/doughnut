import { createContext } from 'react'
import type { Key } from 'ink'

export type StageKeyHandler = (input: string, key: Key) => void

export type SetStageKeyHandler = (handler: StageKeyHandler | null) => void

/**
 * Stage keyboard routing: one active handler registered from the shell
 * (`InteractiveCliApp`) so keys like Esc reach the current stage without
 * competing `useInput` listeners. Stages install a handler via this context
 * (e.g. `AsyncAssistantFetchStage`); when the provider is absent they fall back
 * to local `useInput`.
 */
export const SetStageKeyHandlerContext = createContext<
  SetStageKeyHandler | undefined
>(undefined)
