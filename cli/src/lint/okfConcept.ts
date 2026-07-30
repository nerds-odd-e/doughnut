import { isMap, isScalar, parseDocument } from 'yaml'
import { type OkfProblem, error } from './okfProblem.js'

const OPENING = '---\n'
const CLOSING = '\n---'

type Frontmatter = {
  readonly keys: Record<string, unknown>
  /** The file line a key is written on, for the reader to jump to. */
  readonly lineOf: (key: string) => number | undefined
}

/** The block opens on the file's second line, after the opening `---`. */
function lineOfOffset(block: string, offset: number): number {
  return block.slice(0, offset).split('\n').length + 1
}

function keyLines(block: string, doc: ReturnType<typeof parseDocument>) {
  const lines = new Map<string, number>()
  if (!isMap(doc.contents)) return lines
  for (const { key } of doc.contents.items) {
    if (isScalar(key) && typeof key.value === 'string' && key.range) {
      lines.set(key.value, lineOfOffset(block, key.range[0]))
    }
  }
  return lines
}

function frontmatter(block: string): Frontmatter | undefined {
  const doc = parseDocument(block)
  if (doc.errors.length > 0) return

  const keys: unknown = doc.toJS()
  if (typeof keys !== 'object' || keys === null) return

  const lines = keyLines(block, doc)
  return {
    keys: keys as Record<string, unknown>,
    lineOf: (key) => lines.get(key),
  }
}

/** `type` is the one key OKF always requires: a non-empty string. */
function typeProblems({ keys, lineOf }: Frontmatter): OkfProblem[] {
  if (!('type' in keys)) return error('Frontmatter has no `type` key')
  const line = lineOf('type')
  if (keys.type === null || keys.type === '') {
    return error('Frontmatter `type` has no value', line)
  }
  if (typeof keys.type !== 'string') {
    return error('Frontmatter `type` is not a string', line)
  }
  return []
}

/** `tags` is a list of short strings, which OKF recommends rather than requires. */
function tagsProblems({ keys, lineOf }: Frontmatter): OkfProblem[] {
  if (!('tags' in keys) || Array.isArray(keys.tags)) return []
  return [
    {
      severity: 'warning',
      line: lineOf('tags'),
      message: '`tags` is not a list',
    },
  ]
}

/**
 * What a concept breaks in the Open Knowledge Format, from its content alone.
 * Frontmatter nobody can read stands on its own; once read, every key is asked
 * about rather than leaving the author one fix at a time.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function conceptProblems(content: string): OkfProblem[] {
  if (!content.startsWith(OPENING)) return error('Frontmatter is missing')

  const closing = content.indexOf(CLOSING, OPENING.length)
  if (closing === -1) return error('Frontmatter is not closed with `---`')

  const read = frontmatter(content.slice(OPENING.length, closing))
  if (read === undefined) return error('Frontmatter is not valid YAML')

  return [...typeProblems(read), ...tagsProblems(read)]
}
