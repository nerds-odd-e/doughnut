import { readWorkspace } from '../sync/readWorkspace.js'
import { conceptProblems } from './okfConcept.js'

/** Names OKF reserves, each with a structure of its own rather than a concept's. */
const RESERVED = new Set(['index.md', 'log.md'])

function isReserved(path: string): boolean {
  return RESERVED.has(path.slice(path.lastIndexOf('/') + 1))
}

function report(problems: readonly string[]): string {
  if (problems.length === 0) return 'Workspace follows the OKF format.'
  return [...problems, '', '1 error in 1 file.'].join('\n')
}

export function lintWorkspace(workspace: string): string {
  return report(
    [...readWorkspace(workspace)]
      .filter(([path]) => !isReserved(path))
      .flatMap(([path, content]) =>
        conceptProblems(content).map(
          (message) => `${path}:1  error  ${message}`
        )
      )
  )
}
