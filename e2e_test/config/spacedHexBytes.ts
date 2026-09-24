/**
 * Pure spaced-hex byte vocabulary for E2E fixtures (safe for the Cypress
 * browser bundle) — the single source of truth for how a scenario states exact
 * bytes, so the staging side (Node-side Git tasks) and the on-disk assertion
 * side (page objects) cannot drift apart. Bytes never pass through a text
 * codec, so a fixture may hold bytes that are not valid UTF-8.
 */

/** A scenario's spaced hex, such as `89 FF FE 00`, as contiguous lowercase hex. */
export function hexFromSpacedHex(spacedHex: string): string {
  return spacedHex.replace(/\s/g, '').toLowerCase()
}

function hexFromBytes(bytes: Iterable<number>): string {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join(
    ''
  )
}

/** UTF-8 text as the same contiguous lowercase hex, for exact-byte assertions. */
export function hexFromUtf8Text(text: string): string {
  return hexFromBytes(new TextEncoder().encode(text))
}

/** Base64 bytes (such as a Cypress binary fixture) as the same contiguous lowercase hex. */
export function hexFromBase64(base64: string): string {
  return hexFromBytes(Array.from(atob(base64), (char) => char.charCodeAt(0)))
}
