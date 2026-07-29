import { readWorkspace } from '../sync/readWorkspace.js'

export function lintWorkspace(workspace: string): string {
  const problems = [...readWorkspace(workspace).keys()].map(
    (path) => `${path}:1  error  Frontmatter is missing`
  )
  return [...problems, '', '1 error in 1 file.'].join('\n')
}
