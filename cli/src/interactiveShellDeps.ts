import type { PlaceholderContext } from './renderer.js'
import type { OutputAdapter } from './types.js'

export interface InteractiveShellDeps {
  processInput: (
    input: string,
    output?: OutputAdapter,
    interactiveUi?: boolean
  ) => Promise<boolean>
  getPlaceholderContext: () => PlaceholderContext
  shouldRecordCommittedLineInUserInputHistory: () => boolean
}
