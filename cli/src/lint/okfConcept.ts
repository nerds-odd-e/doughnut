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

/** `type` is the one key OKF always requires: a non-empty string. */
function typeProblems(keys: Record<string, unknown>): OkfProblem[] {
  if (!('type' in keys)) return error('Frontmatter has no `type` key')
  if (keys.type === null || keys.type === '') {
    return error('Frontmatter `type` has no value')
  }
  if (typeof keys.type !== 'string') {
    return error('Frontmatter `type` is not a string')
  }
  return []
}

/** `tags` is a list of short strings, which OKF recommends rather than requires. */
function tagsProblems(keys: Record<string, unknown>): OkfProblem[] {
  if (!('tags' in keys) || Array.isArray(keys.tags)) return []
  return [{ severity: 'warning', line: 1, message: '`tags` is not a list' }]
}

/**
 * What a concept breaks in the Open Knowledge Format, read from its content
 * alone: where it sits in a bundle and how it is reported are not its concern.
 *
 * Frontmatter that cannot be read stands on its own — there is nothing further
 * to say about keys nobody can see. Once read, every key is asked about, so one
 * fix at a time is not what the author is left with.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function conceptProblems(content: string): OkfProblem[] {
  if (!content.startsWith(OPENING)) return error('Frontmatter is missing')

  const closing = content.indexOf(CLOSING, OPENING.length)
  if (closing === -1) return error('Frontmatter is not closed with `---`')

  const keys = parsedKeys(content.slice(OPENING.length, closing))
  if (keys === undefined) return error('Frontmatter is not valid YAML')

  return [...typeProblems(keys), ...tagsProblems(keys)]
}
