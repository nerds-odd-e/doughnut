import { type OkfProblem, error } from './okfProblem.js'

const OPENING = '---\n'
const CLOSING = '\n---'

/**
 * What an index file breaks in the Open Knowledge Format. An index lists what a
 * directory holds, so OKF gives it no frontmatter at all — save `okf_version`,
 * which the one at the bundle root may carry to declare the version it follows.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function indexProblems(
  content: string,
  atBundleRoot: boolean
): OkfProblem[] {
  if (!content.startsWith(OPENING)) return []
  if (!atBundleRoot) return error('An index carries no frontmatter')

  const closing = content.indexOf(CLOSING, OPENING.length)
  const keys = content.slice(
    OPENING.length,
    closing === -1 ? undefined : closing
  )
  return /^(?!okf_version:)\S/m.test(keys)
    ? error('An index carries no frontmatter beyond `okf_version`')
    : []
}
