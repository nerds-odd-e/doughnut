import type { ChatHistory, ChatHistoryOutputTone } from '../types.js'

/**
 * Buffered stdout for one in-flight command turn (between Enter and flush to scrollback).
 * Mirrors {@link OutputAdapter} `log` / `logError` / `logUserNotice` semantics.
 */
export type CommandTurnBuffer = {
  readonly lines: readonly string[]
  readonly tone: ChatHistoryOutputTone
}

export function emptyCommandTurnBuffer(): CommandTurnBuffer {
  return { lines: [], tone: 'plain' }
}

/** `OutputAdapter.log` — sets tone to plain; splits on newlines. */
export function commandTurnBufferAppendLog(
  buffer: CommandTurnBuffer,
  msg: string
): CommandTurnBuffer {
  return {
    tone: 'plain',
    lines: [...buffer.lines, ...msg.split('\n')],
  }
}

/** `OutputAdapter.logError` — sets tone to error. */
export function commandTurnBufferAppendError(
  buffer: CommandTurnBuffer,
  err: unknown
): CommandTurnBuffer {
  const msg = err instanceof Error ? err.message : String(err)
  return {
    tone: 'error',
    lines: [...buffer.lines, ...msg.split('\n')],
  }
}

/** `OutputAdapter.logUserNotice` — sets tone to userNotice. */
export function commandTurnBufferAppendUserNotice(
  buffer: CommandTurnBuffer,
  msg: string
): CommandTurnBuffer {
  return {
    tone: 'userNotice',
    lines: [...buffer.lines, ...msg.split('\n')],
  }
}

/** Append one output block to scrollback (Ink `Static` item; gate 2 — append-only). */
export function scrollbackAppendOutput(
  history: ChatHistory,
  lines: readonly string[],
  tone: ChatHistoryOutputTone = 'plain'
): ChatHistory {
  return [...history, { type: 'output', lines: [...lines], tone }]
}

/** Append one user input row to scrollback (masked content; adapter applies masking before call). */
export function scrollbackCommitInputLine(
  history: ChatHistory,
  content: string
): ChatHistory {
  return [...history, { type: 'input', content }]
}

/**
 * If the turn buffer has lines, append them as one output entry and return an empty buffer.
 * Otherwise leave history and buffer unchanged.
 */
export function scrollbackFlushCommandTurnIfNonEmpty(
  history: ChatHistory,
  turn: CommandTurnBuffer
): { history: ChatHistory; turn: CommandTurnBuffer } {
  if (turn.lines.length === 0) {
    return { history, turn }
  }
  return {
    history: scrollbackAppendOutput(history, turn.lines, turn.tone),
    turn: emptyCommandTurnBuffer(),
  }
}
