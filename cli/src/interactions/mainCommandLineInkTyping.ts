/**
 * Main command-line **typing** keys (caret motion, insert, delete). Matches the keys
 * `@inkjs/ui` `TextInput` handles internally; up/down/tab/escape (and return) stay on
 * {@link ShellSessionRoot} / `handleCommandLineInkInput`.
 */

import type { Key } from 'ink'
import {
  caretOneLeft,
  caretOneRight,
  deleteBeforeCaret,
  insertIntoDraft,
  type InteractiveCommandInput,
} from '../interactiveCommandInput.js'

export type MainCommandLineInkTypingApply = {
  nextCommandInput: InteractiveCommandInput
  /** When true, reset slash-picker highlight like the printable branch in the TTY handler. */
  resetSlashPicker: boolean
}

export function tryApplyMainCommandLineInkTyping(
  cmd: InteractiveCommandInput,
  input: string,
  key: Key
): MainCommandLineInkTypingApply | null {
  if (
    key.return ||
    key.escape ||
    key.tab ||
    (key.shift && key.tab) ||
    key.upArrow ||
    key.downArrow
  ) {
    return null
  }
  if (input === '\r' || input === '\n') {
    return null
  }
  if (key.backspace || key.delete) {
    const prevLen = cmd.lineDraft.length
    const nextCommandInput = deleteBeforeCaret(cmd)
    return {
      nextCommandInput,
      resetSlashPicker: nextCommandInput.lineDraft.length !== prevLen,
    }
  }
  if (key.leftArrow) {
    return {
      nextCommandInput: caretOneLeft(cmd),
      resetSlashPicker: false,
    }
  }
  if (key.rightArrow) {
    return {
      nextCommandInput: caretOneRight(cmd),
      resetSlashPicker: false,
    }
  }
  if (key.home) {
    return {
      nextCommandInput: { ...cmd, caretOffset: 0 },
      resetSlashPicker: false,
    }
  }
  if (key.end) {
    return {
      nextCommandInput: { ...cmd, caretOffset: cmd.lineDraft.length },
      resetSlashPicker: false,
    }
  }
  if (input.length > 0 && !key.ctrl && !key.meta) {
    return {
      nextCommandInput: insertIntoDraft(cmd, input),
      resetSlashPicker: true,
    }
  }
  return null
}
