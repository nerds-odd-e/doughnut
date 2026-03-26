/**
 * Single owner of `process.stdout.write` for interactive TTY mode (`runTTY` + Ink shell).
 *
 * Ink writes the live React tree to stdout on its own stream path; this module holds every
 * adapter-emitted byte sequence so ordering stays explicit and grep-friendly.
 *
 * **Belongs here:** shell-integration OSC (`INTERACTIVE_INPUT_READY_OSC`), cursor show/hide
 * coordinated with Ink’s paint callbacks, Ctrl+C line advance
 * before exit, exit farewell lines (grey committed input + toned output after shell unmount),
 * and “current prompt” separator lines from `OutputAdapter` hooks.
 *
 * **Do not reintroduce:** CSI that lays out or positions the live block (Phase J2 — caret and
 * draft chrome live inside Ink).
 */
import { RESET } from '../ansi.js'
import type { ChatHistoryOutputTone } from '../types.js'
import {
  applyChatHistoryOutputTone,
  buildCurrentPromptSeparator,
  GREY,
  HIDE_CURSOR,
  INTERACTIVE_INPUT_READY_OSC,
  interactiveInputReadyOscSuffix,
  type InteractiveInputReadyPaint,
  renderPastInput,
  SHOW_CURSOR,
  type TerminalWidth,
} from '../renderer.js'

function write(chunk: string): void {
  process.stdout.write(chunk)
}

export const interactiveTtyStdout = {
  hideCursor(): void {
    write(HIDE_CURSOR)
  },

  showCursor(): void {
    write(SHOW_CURSOR)
  },

  inputReadyOsc(): void {
    write(INTERACTIVE_INPUT_READY_OSC)
  },

  finalizeDefaultLiveAfterInk(paint: InteractiveInputReadyPaint): void {
    write(HIDE_CURSOR)
    write(interactiveInputReadyOscSuffix(paint))
  },

  greyCurrentPromptLine(msg: string): void {
    write(`${GREY}${msg}${RESET}\n`)
  },

  currentPromptSeparator(width: TerminalWidth): void {
    write(`${buildCurrentPromptSeparator(width)}\n`)
  },

  exitFarewellBlock(options: {
    width: TerminalWidth
    previousInputContent: string | undefined
    outputLines: readonly string[]
    tone: ChatHistoryOutputTone
  }): void {
    const { width, previousInputContent, outputLines, tone } = options
    if (previousInputContent !== undefined) {
      write(renderPastInput(previousInputContent, width))
      write('\n')
    }
    for (const line of outputLines) {
      write(`${applyChatHistoryOutputTone(line, tone)}\n`)
    }
  },

  ctrlCExitNewline(): void {
    write(`\x1b[${1}B\r\n`)
  },
} as const
