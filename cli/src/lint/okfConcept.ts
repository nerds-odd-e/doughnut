const OPENING = '---\n'
const CLOSING = '\n---'

/**
 * What a concept breaks in the Open Knowledge Format, read from its content
 * alone: where it sits in a bundle and how it is reported are not its concern.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function conceptProblems(content: string): string[] {
  if (!content.startsWith(OPENING)) return ['Frontmatter is missing']

  const closing = content.indexOf(CLOSING, OPENING.length)
  if (closing === -1) return ['Frontmatter is not closed with `---`']

  // `type` is the one key OKF always requires of a concept.
  const keys = content.slice(OPENING.length, closing)
  if (!/^type:/m.test(keys)) return ['Frontmatter has no `type` key']
  if (!/^type:[^\S\n]*\S/m.test(keys)) {
    return ['Frontmatter `type` has no value']
  }
  return []
}
