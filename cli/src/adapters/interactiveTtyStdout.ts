/**
 * Single owner of `process.stdout.write` for interactive TTY mode (`runTTY` + Ink shell).
 *
 * Ink writes the live React tree to stdout on its own stream path; when `render()` uses
 * **`patchConsole`** (enabled on real Node TTY; skipped when `console.Console` is not
 * constructible, e.g. Vitest `spyOn(console, 'log')`), `console.log` in that session is routed
 * through Ink — avoid ad-hoc logging on the interactive hot path. This module holds
 * adapter-emitted byte sequences Ink does not own so ordering stays explicit and grep-friendly.
 *
 * **Belongs here:** shell-integration OSC (`INTERACTIVE_INPUT_READY_OSC`), explicit cursor
 * visibility for exception flows (fetch-wait / shutdown), Ctrl+C line advance
 * before exit, exit farewell lines (grey committed input + toned output after shell unmount),
 * and “current prompt” separator lines from `OutputAdapter` hooks.
 *
 * **Do not reintroduce:** CSI that lays out or positions the live block (Phase J2 — caret and
 * draft chrome live inside Ink).
 */
import type { CliAssistantMessageTone } from '../types.js'
import {
  applyCliAssistantMessageTone,
  buildCurrentPromptSeparator,
  HIDE_CURSOR,
  INTERACTIVE_INPUT_READY_OSC,
  interactiveInputReadyOscSuffix,
  type InteractiveInputReadyPaint,
  renderPastUserMessage,
  SHOW_CURSOR,
  type TerminalWidth,
} from '../renderer.js'
import { terminalChalk } from '../terminalChalk.js'

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

  /**
   * Default command-line contract:
   * - emits OSC when the draft is empty and fetch-wait is inactive
   * - emits nothing while typing or fetch-wait is active
   * Consumers in E2E use this to decide when keystrokes are safe.
   */
  finalizeDefaultLiveAfterInk(paint: InteractiveInputReadyPaint): void {
    write(interactiveInputReadyOscSuffix(paint))
  },

  greyCurrentPromptLine(msg: string): void {
    write(`${terminalChalk.gray(msg)}\n`)
  },

  currentPromptSeparator(width: TerminalWidth): void {
    write(`${buildCurrentPromptSeparator(width)}\n`)
  },

  exitFarewellBlock(options: {
    width: TerminalWidth
    previousInputContent: string | undefined
    outputLines: readonly string[]
    tone: CliAssistantMessageTone
  }): void {
    const { width, previousInputContent, outputLines, tone } = options
    if (previousInputContent !== undefined) {
      write(renderPastUserMessage(previousInputContent, width))
      write('\n')
    }
    for (const line of outputLines) {
      write(`${applyCliAssistantMessageTone(line, tone)}\n`)
    }
  },

  /** Cursor down one row + CRLF — raw CUD CSI, not chalk styling. */
  ctrlCExitNewline(): void {
    write(`\x1b[${1}B\r\n`)
  },
} as const
