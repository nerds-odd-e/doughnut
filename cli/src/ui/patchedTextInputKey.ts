import type { Key } from 'ink'

export type PatchedTextInputState = {
  value: string
  caretOffset: number
}

export type PatchedTextInputApplyResult =
  | { kind: 'submit' }
  | { kind: 'change'; next: PatchedTextInputState }
  | { kind: 'unhandled' }

export function applyPatchedTextInputKey(
  current: PatchedTextInputState,
  input: string,
  key: Key
): PatchedTextInputApplyResult {
  if (key.return || input === '\r' || input === '\n') {
    return { kind: 'submit' }
  }
  if (
    key.escape ||
    key.tab ||
    (key.shift && key.tab) ||
    key.upArrow ||
    key.downArrow ||
    (key.ctrl && input === 'c')
  ) {
    return { kind: 'unhandled' }
  }
  if (key.leftArrow) {
    return {
      kind: 'change',
      next: {
        value: current.value,
        caretOffset: Math.max(0, current.caretOffset - 1),
      },
    }
  }
  if (key.rightArrow) {
    return {
      kind: 'change',
      next: {
        value: current.value,
        caretOffset: Math.min(current.value.length, current.caretOffset + 1),
      },
    }
  }
  if (key.home) {
    return { kind: 'change', next: { value: current.value, caretOffset: 0 } }
  }
  if (key.end) {
    return {
      kind: 'change',
      next: { value: current.value, caretOffset: current.value.length },
    }
  }
  if (key.backspace || key.delete) {
    const nextCaret = Math.max(0, current.caretOffset - 1)
    return {
      kind: 'change',
      next: {
        value:
          current.value.slice(0, nextCaret) +
          current.value.slice(Math.min(current.value.length, nextCaret + 1)),
        caretOffset: nextCaret,
      },
    }
  }
  if (input.length > 0 && !key.ctrl && !key.meta) {
    return {
      kind: 'change',
      next: {
        value:
          current.value.slice(0, current.caretOffset) +
          input +
          current.value.slice(current.caretOffset),
        caretOffset: current.caretOffset + input.length,
      },
    }
  }
  return { kind: 'unhandled' }
}
