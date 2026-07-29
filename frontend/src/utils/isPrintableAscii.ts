/**
 * Whether every character is printable ASCII (0x20-0x7E).
 *
 * Spring encodes response headers as ISO-8859-1, so a non-ASCII filename in a
 * `Content-Disposition` header comes back mangled rather than absent — this
 * lets a caller detect that and fall back to a value it already trusts.
 */
export function isPrintableAscii(value: string): boolean {
  return /^[\x20-\x7E]+$/.test(value)
}
