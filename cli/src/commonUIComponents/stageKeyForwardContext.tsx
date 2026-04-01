import { createContext } from 'react'
import type { Key } from 'ink'

export type StageKeyHandler = (input: string, key: Key) => void

export type SetStageKeyHandler = (handler: StageKeyHandler | null) => void

/** When provided (`InteractiveCliApp`), stage keys are handled at the app root. */
export const SetStageKeyHandlerContext = createContext<
  SetStageKeyHandler | undefined
>(undefined)
