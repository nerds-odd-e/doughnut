import type { CliAssistantMessageTone, PastMessages } from '../types.js'

/**
 * Buffered stdout for one in-flight command turn (between Enter and flush to past messages).
 * Mirrors {@link OutputAdapter} `log` / `logError` / `logUserNotice` semantics.
 */
export type CommandTurnBuffer = {
  readonly lines: readonly string[]
  readonly tone: CliAssistantMessageTone
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

/** Append one CLI assistant block as a past message (Ink `Static` item; append-only). */
export function pastMessagesAppendCliAssistantBlock(
  pastMessages: PastMessages,
  lines: readonly string[],
  tone: CliAssistantMessageTone = 'plain'
): PastMessages {
  return [...pastMessages, { role: 'cli-assistant', lines: [...lines], tone }]
}

/** Append one user line as a past message (masked content; adapter applies masking before call). */
export function pastMessagesCommitUserLine(
  pastMessages: PastMessages,
  content: string
): PastMessages {
  return [...pastMessages, { role: 'user', content }]
}

/**
 * If the turn buffer has lines, append them as one CLI assistant past message and return an empty buffer.
 * Otherwise leave past messages and buffer unchanged.
 */
export function pastMessagesFlushCommandTurnIfNonEmpty(
  pastMessages: PastMessages,
  turn: CommandTurnBuffer
): { pastMessages: PastMessages; turn: CommandTurnBuffer } {
  if (turn.lines.length === 0) {
    return { pastMessages, turn }
  }
  return {
    pastMessages: pastMessagesAppendCliAssistantBlock(
      pastMessages,
      turn.lines,
      turn.tone
    ),
    turn: emptyCommandTurnBuffer(),
  }
}
