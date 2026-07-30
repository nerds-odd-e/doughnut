import { readWorkspace } from '../sync/readWorkspace.js'
import { type ConceptProblem, conceptProblems } from './okfConcept.js'

/** Names OKF reserves, each with a structure of its own rather than a concept's. */
const RESERVED = new Set(['index.md', 'log.md'])

function isReserved(path: string): boolean {
  return RESERVED.has(path.slice(path.lastIndexOf('/') + 1))
}

const CONFORMS = 'Workspace follows the OKF format.'

type Problem = ConceptProblem & { readonly path: string }

function counted(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`
}

/**
 * Errors and warnings are counted apart, so the reader can tell a bundle that
 * fell short of what OKF requires from one that only passed up a recommendation.
 */
function summary(problems: readonly Problem[]): string {
  const errors = problems.filter(({ severity }) => severity === 'error').length
  const warnings = problems.length - errors
  const files = new Set(problems.map(({ path }) => path)).size
  const found = [
    ...(errors > 0 ? [counted(errors, 'error')] : []),
    ...(warnings > 0 ? [counted(warnings, 'warning')] : []),
  ].join(', ')
  const counts = `${found} in ${counted(files, 'file')}.`
  return errors > 0 ? counts : `${CONFORMS} ${counts}`
}

function report(problems: readonly Problem[]): string {
  if (problems.length === 0) return CONFORMS
  return [
    ...problems.map(
      ({ path, severity, message }) => `${path}:1  ${severity}  ${message}`
    ),
    '',
    summary(problems),
  ].join('\n')
}

export function lintWorkspace(workspace: string): string {
  return report(
    [...readWorkspace(workspace)]
      .filter(([path]) => !isReserved(path))
      .flatMap(([path, content]) =>
        conceptProblems(content).map((problem) => ({ ...problem, path }))
      )
  )
}
