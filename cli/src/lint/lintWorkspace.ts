import { readWorkspace } from '../sync/readWorkspace.js'
import { conceptProblems } from './okfConcept.js'

/** Names OKF reserves, each with a structure of its own rather than a concept's. */
const RESERVED = new Set(['index.md', 'log.md'])

function isReserved(path: string): boolean {
  return RESERVED.has(path.slice(path.lastIndexOf('/') + 1))
}

type Problem = { readonly path: string; readonly message: string }

function counted(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`
}

function report(problems: readonly Problem[]): string {
  if (problems.length === 0) return 'Workspace follows the OKF format.'
  const files = new Set(problems.map(({ path }) => path)).size
  return [
    ...problems.map(({ path, message }) => `${path}:1  error  ${message}`),
    '',
    `${counted(problems.length, 'error')} in ${counted(files, 'file')}.`,
  ].join('\n')
}

export function lintWorkspace(workspace: string): string {
  return report(
    [...readWorkspace(workspace)]
      .filter(([path]) => !isReserved(path))
      .flatMap(([path, content]) =>
        conceptProblems(content).map((message) => ({ path, message }))
      )
  )
}
