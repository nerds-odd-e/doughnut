import { parse } from 'yaml'
import { type OkfProblem, error } from './okfProblem.js'

const OPENING = '---\n'
const CLOSING = '\n---'

/** The frontmatter as a mapping, or undefined when no parser could read it. */
function parsedKeys(keys: string): Record<string, unknown> | undefined {
  try {
    const parsed: unknown = parse(keys)
    return typeof parsed === 'object' && parsed !== null
      ? (parsed as Record<string, unknown>)
      : undefined
  } catch {
    return
  }
}

/**
 * What a concept breaks in the Open Knowledge Format, read from its content
 * alone: where it sits in a bundle and how it is reported are not its concern.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function conceptProblems(content: string): OkfProblem[] {
  if (!content.startsWith(OPENING)) return error('Frontmatter is missing')

  const closing = content.indexOf(CLOSING, OPENING.length)
  if (closing === -1) return error('Frontmatter is not closed with `---`')

  const keys = parsedKeys(content.slice(OPENING.length, closing))
  if (keys === undefined) return error('Frontmatter is not valid YAML')

  // `type` is the one key OKF always requires: a non-empty string.
  if (!('type' in keys)) return error('Frontmatter has no `type` key')
  if (keys.type === null || keys.type === '') {
    return error('Frontmatter `type` has no value')
  }
  if (typeof keys.type !== 'string') {
    return error('Frontmatter `type` is not a string')
  }

  // `tags` is a list of short strings, which OKF recommends rather than requires.
  if ('tags' in keys && !Array.isArray(keys.tags)) {
    return [{ severity: 'warning', message: '`tags` is not a list' }]
  }
  return []
}
