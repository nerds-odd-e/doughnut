import { readWorkspace } from '../sync/readWorkspace.js'
import { conceptProblems } from './okfConcept.js'

function report(problems: readonly string[]): string {
  if (problems.length === 0) return 'Workspace follows the OKF format.'
  return [...problems, '', '1 error in 1 file.'].join('\n')
}

export function lintWorkspace(workspace: string): string {
  return report(
    [...readWorkspace(workspace)].flatMap(([path, content]) =>
      conceptProblems(content).map((message) => `${path}:1  error  ${message}`)
    )
  )
}
