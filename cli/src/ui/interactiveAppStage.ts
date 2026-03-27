import type { TokenSelectionState } from '../shell/shellSessionState.js'

export type AppStage =
  | { kind: 'shell' }
  | { kind: 'accessTokenList'; picker: TokenSelectionState }
