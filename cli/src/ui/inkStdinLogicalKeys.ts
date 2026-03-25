import type { Key } from 'ink'

/**
 * Ink’s `Key` type is all booleans, all required — there is no “empty” key.
 * Reused for replaying coalesced printable bytes: one character + this shape means “typed char”.
 */
const inkKeyNeutral: Key = {
  upArrow: false,
  downArrow: false,
  leftArrow: false,
  rightArrow: false,
  pageDown: false,
  pageUp: false,
  home: false,
  end: false,
  return: false,
  escape: false,
  ctrl: false,
  shift: false,
  tab: false,
  backspace: false,
  delete: false,
  meta: false,
  super: false,
  hyper: false,
  capsLock: false,
  numLock: false,
}

/**
 * Ink may merge several printable bytes plus CR/LF into one `useInput` callback.
 * Invoke `fn` once per logical key so list/confirm policy matches one key at a time.
 */
export function eachLogicalInkStdinChunk(
  input: string,
  key: Key,
  fn: (input: string, key: Key) => void
): void {
  const last = input.at(-1)
  if (
    input.length >= 2 &&
    (last === '\r' || last === '\n') &&
    /^[\x20-\x7e]+$/.test(input.slice(0, -1))
  ) {
    for (const ch of input.slice(0, -1)) {
      fn(ch, inkKeyNeutral)
    }
    fn(last!, { ...inkKeyNeutral, return: true })
    return
  }
  fn(input, key)
}
