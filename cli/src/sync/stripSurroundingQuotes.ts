/** Shell-style quotes are not parsed by the CLI; strip them if the user typed them. */
export function stripSurroundingQuotes(path: string): string {
  if (path.length >= 2) {
    const first = path[0]
    const last = path[path.length - 1]
    if ((first === '"' && last === '"') || (first === "'" && last === "'")) {
      return path.slice(1, -1)
    }
  }
  return path
}
