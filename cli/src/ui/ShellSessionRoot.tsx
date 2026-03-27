import React from 'react'
import type { Key } from 'ink'
import { Box, Newline, Static, Text } from 'ink'
import type { RecallInkConfirmChoice } from '../interactions/recallYesNo.js'
import type { InteractiveCommandInput } from '../interactiveCommandInput.js'
import {
  applyCliAssistantMessageTone,
  buildSuggestionLinesForInk,
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
  formatCurrentStageIndicatorLine,
  getTerminalWidth,
  greyCurrentStageIndicatorLabel,
  RECALL_SESSION_YES_NO_PLACEHOLDER,
  interactiveFetchWaitStageIndicatorLine,
  needsGapBeforeLiveRegion,
  renderPastUserMessage,
  wrapTextToVisibleWidthLines,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'
import { hasInteractiveSlashCompletions } from '../slashCompletion.js'
import { getDefaultTokenLabel } from '../commands/accessToken.js'
import type {
  ShellSessionState,
  TokenSelectionState,
} from '../shell/shellSessionState.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'
import { TOKEN_LIST_COMMANDS } from '../shell/tokenListCommands.js'
import {
  getInteractiveFetchWaitLine,
  type InteractiveFetchWaitLine,
} from '../interactiveFetchWait.js'
import type { AccessTokenPickerCommandConfig, PastMessage } from '../types.js'
import { RecallInkConfirmPanel } from './RecallInkConfirmPanel.js'
import { FetchWaitDisplay } from './FetchWaitDisplay.js'
import {
  AccessTokenPickerLivePanel,
  CommandLineLivePanel,
  RecallMcqChoicesLivePanel,
} from './liveColumnInk.js'

export function isInkSubmitPressed(key: Key, input: string): boolean {
  return key.return || input === '\n' || input === '\r'
}

/**
 * Stage band + wrapped **Current prompt** copy (above the command line). Used for past-messages gap
 * and passed through to the default command-line Ink panel.
 */
type LiveColumnLeadingSnapshot = {
  terminalWidth: TerminalWidth
  placeholderContext: PlaceholderContext
  currentPromptWrappedLines: string[]
  currentStageIndicatorLines: string[]
}

/** Default live column only: {@link LiveColumnLeadingSnapshot} + **Current guidance** rows. */
type DefaultCommandLineInkLayout = LiveColumnLeadingSnapshot & {
  currentGuidanceLines: string[]
}

function currentStageIndicatorLinesForLiveRegion(
  waitLine: InteractiveFetchWaitLine | null,
  tokenPicker: AccessTokenPickerCommandConfig | undefined,
  sessionPayloadLoading: boolean
): string[] {
  if (waitLine != null) {
    return [interactiveFetchWaitStageIndicatorLine(waitLine)]
  }
  if (tokenPicker != null) {
    return [greyCurrentStageIndicatorLabel(tokenPicker.stageIndicator)]
  }
  if (sessionPayloadLoading) {
    return [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
  }
  return []
}

function computeLiveColumnLeadingSnapshot(
  deps: InteractiveShellDeps,
  tokenSelection: TokenSelectionState | null
): LiveColumnLeadingSnapshot {
  const terminalWidth = getTerminalWidth()
  const placeholderContext = deps.getPlaceholderContext()
  const stopRecallConfirm = deps.isPendingStopConfirmation()
    ? deps.getRecallStopConfirmInkModel(placeholderContext)
    : null
  const recallQuestionPromptLines =
    deps.getRecallCurrentPromptWrappedLines(terminalWidth)
  const waitLine = getInteractiveFetchWaitLine()
  const tokenListConfig = tokenSelection
    ? TOKEN_LIST_COMMANDS[tokenSelection.command]
    : undefined
  let currentPromptWrappedLines: string[]
  if (waitLine) {
    currentPromptWrappedLines = []
  } else if (stopRecallConfirm) {
    currentPromptWrappedLines = [...stopRecallConfirm.promptLines]
  } else {
    const currentPromptText = tokenListConfig?.currentPrompt
    if (
      recallQuestionPromptLines !== null &&
      !deps.isPendingStopConfirmation()
    ) {
      currentPromptWrappedLines = recallQuestionPromptLines
    } else if (currentPromptText) {
      currentPromptWrappedLines = wrapTextToVisibleWidthLines(
        currentPromptText,
        terminalWidth
      )
    } else {
      currentPromptWrappedLines = []
    }
  }
  const currentStageIndicatorLines = currentStageIndicatorLinesForLiveRegion(
    waitLine,
    tokenListConfig,
    deps.isInCommandSessionSubstate()
  )
  return {
    terminalWidth,
    placeholderContext,
    currentPromptWrappedLines,
    currentStageIndicatorLines,
  }
}

export type ShellSessionInkHandlers = {
  onInterrupt: () => void
  onFetchWaitCancel: () => void
  onStopConfirmResult: (d: RecallInkConfirmChoice) => Promise<void>
  onSessionYesNoResult: (d: RecallInkConfirmChoice) => Promise<void>
  onRecallMcqGuidanceKey: (input: string, key: Key) => Promise<void>
  onTokenPickerGuidanceKey: (input: string, key: Key) => Promise<void>
  onCommandLineKey: (input: string, key: Key) => Promise<void>
  onCommandLineTyping: (next: InteractiveCommandInput) => void
  onEnterStopConfirmationFromEsc: () => void
  whenInActiveRecallSession: () => boolean
}

function PastMessageBlock({
  entry,
  width,
}: {
  entry: PastMessage
  width: number
}): React.ReactElement {
  if (entry.role === 'user') {
    const raw = renderPastUserMessage(entry.content, width)
    const lines = raw.split('\n')
    while (lines.length > 0 && lines[lines.length - 1] === '') {
      lines.pop()
    }
    return (
      <Box flexDirection="column">
        {lines.map((line, i) => (
          <Text key={i}>{line}</Text>
        ))}
      </Box>
    )
  }
  const tone = entry.tone ?? 'plain'
  return (
    <Box flexDirection="column">
      {entry.lines.map((line, i) => (
        <Text key={i}>{applyCliAssistantMessageTone(line, tone)}</Text>
      ))}
    </Box>
  )
}

function buildLivePanel(
  session: ShellSessionState,
  deps: InteractiveShellDeps,
  tokenSelection: TokenSelectionState | null,
  defaultCommandLineLayout: DefaultCommandLineInkLayout,
  handlers: ShellSessionInkHandlers
): React.ReactElement {
  const waitLine = getInteractiveFetchWaitLine()
  if (waitLine !== null) {
    return React.createElement(FetchWaitDisplay, {
      waitLine,
      onCancelWait: handlers.onFetchWaitCancel,
    })
  }
  if (deps.isPendingStopConfirmation()) {
    const placeholderCtx = deps.getPlaceholderContext()
    const model = deps.getRecallStopConfirmInkModel(placeholderCtx)
    const stageLines = deps.isInCommandSessionSubstate()
      ? [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
      : []
    return React.createElement(RecallInkConfirmPanel, {
      key: 'confirm-stop-recall',
      variant: 'stop-recall',
      guidanceLines: [
        ...stageLines,
        ...model.promptLines,
        ...model.confirmQuestionLines,
      ],
      placeholderText: model.placeholder,
      onInterrupt: handlers.onInterrupt,
      onResult: (d) => handlers.onStopConfirmResult(d),
    })
  }
  if (deps.getPlaceholderContext() === RECALL_SESSION_YES_NO_PLACEHOLDER) {
    return React.createElement(RecallInkConfirmPanel, {
      key: 'confirm-session-yes-no',
      variant: 'in-session',
      guidanceLines: deps.getRecallSessionYesNoInkGuidanceLines(),
      placeholderText: 'y or n; /stop to exit recall',
      whenInActiveRecallSession: handlers.whenInActiveRecallSession,
      onEscapeOpensStopRecallSheet: handlers.onEnterStopConfirmationFromEsc,
      onInterrupt: handlers.onInterrupt,
      onResult: (d) => handlers.onSessionYesNoResult(d),
    })
  }
  const numberedChoices = deps.getNumberedChoiceListChoices()
  if (numberedChoices !== null) {
    const width = getTerminalWidth()
    const promptLines = deps.getRecallCurrentPromptWrappedLines(width) ?? []
    const recallStageLine = formatCurrentStageIndicatorLine(
      DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
      width
    )
    return React.createElement(RecallMcqChoicesLivePanel, {
      stageIndicatorLine: recallStageLine,
      currentPromptLines: promptLines,
      choices: numberedChoices,
      highlightIndex: session.numberedChoiceHighlightIndex,
      lineDraft: session.commandInput.lineDraft,
      caretOffset: session.commandInput.caretOffset,
      width,
      onInterrupt: handlers.onInterrupt,
      onGuidanceListKey: (inp, ky) =>
        Promise.resolve(handlers.onRecallMcqGuidanceKey(inp, ky)).catch(
          () => undefined
        ),
    })
  }
  if (tokenSelection) {
    const tokenListConfig = TOKEN_LIST_COMMANDS[tokenSelection.command]
    const width = getTerminalWidth()
    const promptLines = tokenListConfig?.currentPrompt
      ? wrapTextToVisibleWidthLines(tokenListConfig.currentPrompt, width)
      : []
    const stageIndicatorLine = tokenListConfig
      ? formatCurrentStageIndicatorLine(
          greyCurrentStageIndicatorLabel(tokenListConfig.stageIndicator),
          width
        )
      : ''
    return React.createElement(AccessTokenPickerLivePanel, {
      stageIndicatorLine,
      currentPromptLines: promptLines,
      lineDraft: session.commandInput.lineDraft,
      caretOffset: session.commandInput.caretOffset,
      width,
      items: tokenSelection.items,
      defaultLabel: getDefaultTokenLabel(),
      highlightIndex: tokenSelection.highlightIndex,
      onInterrupt: handlers.onInterrupt,
      onGuidanceListKey: (inp, ky) =>
        Promise.resolve(handlers.onTokenPickerGuidanceKey(inp, ky)).catch(
          () => undefined
        ),
    })
  }
  const layout = defaultCommandLineLayout
  return React.createElement(CommandLineLivePanel, {
    commandInput: session.commandInput,
    width: layout.terminalWidth,
    currentPromptWrappedLines: layout.currentPromptWrappedLines,
    currentGuidanceLines: layout.currentGuidanceLines,
    currentStageIndicatorLines: layout.currentStageIndicatorLines,
    placeholderContext: layout.placeholderContext,
    onCommandKey: (input, key) =>
      Promise.resolve(handlers.onCommandLineKey(input, key)).catch(
        () => undefined
      ),
    onCommandLineTyping: handlers.onCommandLineTyping,
    onInterrupt: handlers.onInterrupt,
  })
}

type ShellSessionRootProps = {
  session: ShellSessionState
  tokenSelection: TokenSelectionState | null
  deps: InteractiveShellDeps
  handlers: ShellSessionInkHandlers
}

export function ShellSessionRoot({
  session,
  tokenSelection,
  deps,
  handlers,
}: ShellSessionRootProps): React.ReactElement {
  const leading = computeLiveColumnLeadingSnapshot(deps, tokenSelection)
  const liveLeadingGap = needsGapBeforeLiveRegion(
    session.pastMessages,
    leading.currentPromptWrappedLines,
    leading.currentStageIndicatorLines
  )
  const defaultCommandLineLayout: DefaultCommandLineInkLayout = {
    ...leading,
    currentGuidanceLines: buildSuggestionLinesForInk(
      session.commandInput.lineDraft,
      session.highlightIndex,
      {
        forceCommandsHint:
          session.suggestionsDismissed &&
          hasInteractiveSlashCompletions(session.commandInput.lineDraft),
      }
    ),
  }
  const livePanel = buildLivePanel(
    session,
    deps,
    tokenSelection,
    defaultCommandLineLayout,
    handlers
  )
  const terminalWidth = getTerminalWidth()
  return (
    <Box flexDirection="column">
      <Static items={session.pastMessages} style={{ position: 'relative' }}>
        {(entry, index) => (
          <Box key={index} flexDirection="column">
            <PastMessageBlock entry={entry} width={terminalWidth} />
          </Box>
        )}
      </Static>
      <Box flexDirection="column">
        {liveLeadingGap ? <Newline /> : null}
        {livePanel}
      </Box>
    </Box>
  )
}
