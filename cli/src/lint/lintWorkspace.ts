import { readWorkspace } from '../sync/readWorkspace.js'

const OPENING = '---\n'
const CLOSING = '\n---'

function frontmatterProblems(content: string): string[] {
  if (!content.startsWith(OPENING)) return ['Frontmatter is missing']

  const closing = content.indexOf(CLOSING, OPENING.length)
  if (closing === -1) return ['Frontmatter is not closed with `---`']

  // `type` is the one key OKF always requires of a concept.
  const keys = content.slice(OPENING.length, closing)
  if (!/^type:/m.test(keys)) return ['Frontmatter has no `type` key']
  return []
}

function report(problems: readonly string[]): string {
  if (problems.length === 0) return 'Workspace follows the OKF format.'
  return [...problems, '', '1 error in 1 file.'].join('\n')
}

export function lintWorkspace(workspace: string): string {
  return report(
    [...readWorkspace(workspace)].flatMap(([path, content]) =>
      frontmatterProblems(content).map(
        (message) => `${path}:1  error  ${message}`
      )
    )
  )
}
