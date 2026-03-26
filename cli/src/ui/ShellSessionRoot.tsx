import React from 'react'
import type { Key } from 'ink'
import { filterCommandsByPrefix, interactiveDocs } from '../help.js'
import type { RecallInkConfirmChoice } from '../interactions/recallYesNo.js'
import {
  buildSuggestionLinesForInk,
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
  formatCurrentStageIndicatorLine,
  getLastLine,
  getTerminalWidth,
  greyCurrentStageIndicatorLabel,
  interactiveFetchWaitStageIndicatorLine,
  needsGapBeforeLiveRegion,
  wrapTextToVisibleWidthLines,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'
import type { ShellSessionState } from '../shell/shellSessionState.js'
import type { TTYDeps } from '../adapters/ttyDeps.js'
import {
  getInteractiveFetchWaitLine,
  type InteractiveFetchWaitLine,
} from '../interactiveFetchWait.js'
import type { AccessTokenPickerCommandConfig } from '../types.js'
import { CommandLineLivePanel } from './CommandLineLivePanel.js'
import { RecallInkConfirmPanel } from './RecallInkConfirmPanel.js'
import { FetchWaitDisplay } from './FetchWaitDisplay.js'
import { InteractiveShellDisplay } from './InteractiveShellDisplay.js'
import {
  AccessTokenPickerLivePanel,
  RecallMcqChoicesLivePanel,
} from './liveSelectionGuidanceInk.js'

export function isInkSubmitPressed(key: Key, input: string): boolean {
  return key.return || input === '\n' || input === '\r'
}

/**
 * Stage band + wrapped **Current prompt** copy (above the command line). Used for scrollback gap
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

/** MCQ recall **Current prompt** lines; empty array when the adapter has no stem to show. */
function mcqRecallCurrentPromptLines(
  deps: TTYDeps,
  width: TerminalWidth
): string[] {
  return deps.getNumberedChoiceListCurrentPromptWrappedLines(width) ?? []
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

export function isCommandPrefixWithSuggestions(lineDraft: string): boolean {
  const lastLine = getLastLine(lineDraft)
  if (!lastLine.startsWith('/') || lastLine.endsWith(' ')) return false
  const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
  return filtered.length > 0
}

export function isAlternateLivePanel(
  session: ShellSessionState,
  deps: TTYDeps
): boolean {
  if (getInteractiveFetchWaitLine() !== null) return true
  if (deps.isPendingStopConfirmation()) return true
  if (deps.usesSessionYesNoInputChrome(!!session.tokenSelection)) return true
  if (deps.getNumberedChoiceListChoices() !== null) return true
  if (session.tokenSelection) return true
  return false
}

function computeLiveColumnLeadingSnapshot(
  session: ShellSessionState,
  deps: TTYDeps
): LiveColumnLeadingSnapshot {
  const terminalWidth = getTerminalWidth()
  const placeholderContext = deps.getPlaceholderContext(
    !!session.tokenSelection
  )
  const stopRecallConfirm = deps.isPendingStopConfirmation()
    ? deps.getRecallStopConfirmInkModel(placeholderContext)
    : null
  const numberedChoicePromptLines =
    deps.getNumberedChoiceListCurrentPromptWrappedLines(terminalWidth)
  const spellingPromptLines =
    deps.getSpellingRecallCurrentPromptWrappedLines(terminalWidth)
  const waitLine = getInteractiveFetchWaitLine()
  const tokenListConfig = session.tokenSelection
    ? deps.TOKEN_LIST_COMMANDS[session.tokenSelection.command]
    : undefined
  let currentPromptWrappedLines: string[]
  if (waitLine) {
    currentPromptWrappedLines = []
  } else if (stopRecallConfirm) {
    currentPromptWrappedLines = [...stopRecallConfirm.promptLines]
  } else {
    const currentPromptText = tokenListConfig?.currentPrompt
    if (
      !session.tokenSelection &&
      numberedChoicePromptLines !== null &&
      !deps.isPendingStopConfirmation()
    ) {
      currentPromptWrappedLines = numberedChoicePromptLines
    } else if (
      !session.tokenSelection &&
      spellingPromptLines !== null &&
      !deps.isPendingStopConfirmation()
    ) {
      currentPromptWrappedLines = spellingPromptLines
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
  onStopConfirmResult: (d: RecallInkConfirmChoice) => Promise<void>
  onSessionYesNoResult: (d: RecallInkConfirmChoice) => Promise<void>
  onRecallMcqGuidanceKey: (input: string, key: Key) => Promise<void>
  onTokenPickerGuidanceKey: (input: string, key: Key) => Promise<void>
  onCommandLineKey: (input: string, key: Key) => Promise<void>
  signalConfirmInputReady: () => void
  onEnterStopConfirmationFromEsc: () => void
  whenInActiveRecallSession: () => boolean
}

function buildLivePanel(
  session: ShellSessionState,
  deps: TTYDeps,
  defaultCommandLineLayout: DefaultCommandLineInkLayout | undefined,
  handlers: ShellSessionInkHandlers
): React.ReactElement {
  const waitLine = getInteractiveFetchWaitLine()
  if (waitLine !== null) {
    return React.createElement(FetchWaitDisplay, { waitLine })
  }
  if (deps.isPendingStopConfirmation()) {
    const placeholderCtx = deps.getPlaceholderContext(false)
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
      onInputReadySignal: handlers.signalConfirmInputReady,
      onInterrupt: handlers.onInterrupt,
      onResult: (d) => handlers.onStopConfirmResult(d),
    })
  }
  if (deps.usesSessionYesNoInputChrome(!!session.tokenSelection)) {
    return React.createElement(RecallInkConfirmPanel, {
      key: 'confirm-session-yes-no',
      variant: 'in-session',
      guidanceLines: deps.getRecallSessionYesNoInkGuidanceLines(),
      placeholderText: 'y or n; /stop to exit recall',
      whenInActiveRecallSession: handlers.whenInActiveRecallSession,
      onEscapeOpensStopRecallSheet: handlers.onEnterStopConfirmationFromEsc,
      onInputReadySignal: handlers.signalConfirmInputReady,
      onInterrupt: handlers.onInterrupt,
      onResult: (d) => handlers.onSessionYesNoResult(d),
    })
  }
  const numberedChoices = deps.getNumberedChoiceListChoices()
  if (numberedChoices !== null) {
    const width = getTerminalWidth()
    const promptLines = mcqRecallCurrentPromptLines(deps, width)
    return React.createElement(RecallMcqChoicesLivePanel, {
      stageIndicatorLine: DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
      currentPromptLines: promptLines,
      choices: numberedChoices,
      highlightIndex: session.numberedChoiceHighlightIndex,
      lineDraft: session.commandInput.lineDraft,
      width,
      onInterrupt: handlers.onInterrupt,
      onGuidanceListKey: (inp, ky) =>
        Promise.resolve(handlers.onRecallMcqGuidanceKey(inp, ky)).catch(
          () => undefined
        ),
    })
  }
  if (session.tokenSelection) {
    const tokenListConfig =
      deps.TOKEN_LIST_COMMANDS[session.tokenSelection.command]
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
      width,
      items: session.tokenSelection.items,
      defaultLabel: deps.getDefaultTokenLabel(),
      highlightIndex: session.tokenSelection.highlightIndex,
      onInterrupt: handlers.onInterrupt,
      onGuidanceListKey: (inp, ky) =>
        Promise.resolve(handlers.onTokenPickerGuidanceKey(inp, ky)).catch(
          () => undefined
        ),
    })
  }
  const layout = defaultCommandLineLayout!
  return React.createElement(CommandLineLivePanel, {
    buffer: session.commandInput.lineDraft,
    caretOffset: session.commandInput.caretOffset,
    width: layout.terminalWidth,
    currentPromptWrappedLines: layout.currentPromptWrappedLines,
    currentGuidanceLines: layout.currentGuidanceLines,
    currentStageIndicatorLines: layout.currentStageIndicatorLines,
    placeholderContext: layout.placeholderContext,
    onCommandKey: (input, key) =>
      Promise.resolve(handlers.onCommandLineKey(input, key)).catch(
        () => undefined
      ),
    onInterrupt: handlers.onInterrupt,
  })
}

export type ShellSessionRootProps = {
  session: ShellSessionState
  deps: TTYDeps
  handlers: ShellSessionInkHandlers
}

export function ShellSessionRoot({
  session,
  deps,
  handlers,
}: ShellSessionRootProps): React.ReactElement {
  const leading = computeLiveColumnLeadingSnapshot(session, deps)
  const liveLeadingGap = needsGapBeforeLiveRegion(
    session.chatHistory,
    leading.currentPromptWrappedLines,
    leading.currentStageIndicatorLines
  )
  const defaultCommandLineLayout: DefaultCommandLineInkLayout | undefined =
    isAlternateLivePanel(session, deps)
      ? undefined
      : {
          ...leading,
          currentGuidanceLines: buildSuggestionLinesForInk(
            session.commandInput.lineDraft,
            session.highlightIndex,
            {
              forceCommandsHint:
                session.suggestionsDismissed &&
                isCommandPrefixWithSuggestions(session.commandInput.lineDraft),
            }
          ),
        }
  const livePanel = buildLivePanel(
    session,
    deps,
    defaultCommandLineLayout,
    handlers
  )
  return React.createElement(InteractiveShellDisplay, {
    history: session.chatHistory,
    terminalWidth: getTerminalWidth(),
    liveLeadingGap,
    livePanel,
  })
}
