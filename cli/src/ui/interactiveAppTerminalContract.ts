import type { ShellSessionState } from '../shell/shellSessionState.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'
import type { CliAssistantMessageTone } from '../types.js'

export type InteractiveAppTerminalContract = {
  writeCurrentPromptLine: (msg: string) => void
  beginCurrentPrompt: () => void
  onShellSessionLayoutEffect: (
    session: ShellSessionState,
    deps: InteractiveShellDeps
  ) => void
  writeExitFarewellBlock: (options: {
    previousInputContent: string | undefined
    outputLines: readonly string[]
    tone: CliAssistantMessageTone
  }) => void
  writeCtrlCExitNewline: () => void
}
