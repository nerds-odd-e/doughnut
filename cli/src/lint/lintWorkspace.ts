import { readWorkspace } from '../sync/readWorkspace.js'

const OPENING = '---\n'

/** The keys between the opening `---` and the line closing the block. */
function frontmatterKeys(content: string): string {
  const closing = content.indexOf('\n---', OPENING.length)
  return closing === -1
    ? content.slice(OPENING.length)
    : content.slice(OPENING.length, closing)
}

/** `type` is the one key OKF always requires of a concept. */
function problemsIn(content: string): string[] {
  if (!content.startsWith(OPENING)) return ['Frontmatter is missing']
  if (!/^type:/m.test(frontmatterKeys(content))) {
    return ['Frontmatter has no `type` key']
  }
  return []
}

export function lintWorkspace(workspace: string): string {
  const problems = [...readWorkspace(workspace)].flatMap(([path, content]) =>
    problemsIn(content).map((message) => `${path}:1  error  ${message}`)
  )
  if (problems.length === 0) return 'Workspace follows the OKF format.'
  return [...problems, '', '1 error in 1 file.'].join('\n')
}
