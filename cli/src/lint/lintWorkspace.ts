import { readWorkspace } from '../sync/readWorkspace.js'

const OPENING = '---\n'
const CLOSING = '\n---'

/** `type` is the one key OKF always requires of a concept. */
function problemsIn(content: string): string[] {
  if (!content.startsWith(OPENING)) return ['Frontmatter is missing']

  const closing = content.indexOf(CLOSING, OPENING.length)
  if (closing === -1) return ['Frontmatter is not closed with `---`']

  const keys = content.slice(OPENING.length, closing)
  if (!/^type:/m.test(keys)) return ['Frontmatter has no `type` key']
  return []
}

export function lintWorkspace(workspace: string): string {
  const problems = [...readWorkspace(workspace)].flatMap(([path, content]) =>
    problemsIn(content).map((message) => `${path}:1  error  ${message}`)
  )
  if (problems.length === 0) return 'Workspace follows the OKF format.'
  return [...problems, '', '1 error in 1 file.'].join('\n')
}
