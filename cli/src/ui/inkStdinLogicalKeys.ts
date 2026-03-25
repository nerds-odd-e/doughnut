import type { Key } from 'ink'

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
      fn(ch, {} as Key)
    }
    fn(last!, {} as Key)
    return
  }
  fn(input, key)
}
