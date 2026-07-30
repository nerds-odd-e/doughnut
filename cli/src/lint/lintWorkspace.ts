import { readWorkspace } from '../sync/readWorkspace.js'
import { conceptProblems } from './okfConcept.js'
import { indexProblems } from './okfIndex.js'
import type { OkfProblem } from './okfProblem.js'

function basename(path: string): string {
  return path.slice(path.lastIndexOf('/') + 1)
}

/**
 * Which rules a file answers to. OKF reserves `index.md` and `log.md`, each with
 * a structure of its own rather than a concept's; a `log.md` has no rule here yet.
 */
function problemsIn(path: string, content: string): OkfProblem[] {
  const name = basename(path)
  if (name === 'index.md') return indexProblems(content, path === name)
  if (name === 'log.md') return []
  return conceptProblems(content)
}

const CONFORMS = 'Workspace follows the OKF format.'

type Problem = OkfProblem & { readonly path: string }

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
    [...readWorkspace(workspace)].flatMap(([path, content]) =>
      problemsIn(path, content).map((problem) => ({ ...problem, path }))
    )
  )
}
