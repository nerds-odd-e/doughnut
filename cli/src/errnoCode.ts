/** Extracts a Node.js error `code` (e.g. `ENOENT`, `EACCES`, `EXDEV`) from a caught value, if present. */
export function errnoCode(e: unknown): string | undefined {
  if (e !== null && typeof e === 'object' && 'code' in e) {
    const c = (e as { code: unknown }).code
    return typeof c === 'string' ? c : undefined
  }
  return
}
