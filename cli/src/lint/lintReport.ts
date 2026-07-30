import type { OkfProblem } from './okfProblem.js'

const CONFORMS = 'Workspace follows the OKF format.'

export type Finding = OkfProblem & { readonly path: string }

function counted(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`
}

function at({ path, line }: Finding): string {
  return line === undefined ? path : `${path}:${line}`
}

/**
 * Errors and warnings are counted apart, so the reader can tell a bundle that
 * fell short of what OKF requires from one that only passed up a recommendation.
 */
function summary(findings: readonly Finding[]): string {
  const errors = findings.filter(({ severity }) => severity === 'error').length
  const warnings = findings.length - errors
  const files = new Set(findings.map(({ path }) => path)).size
  const found = [
    ...(errors > 0 ? [counted(errors, 'error')] : []),
    ...(warnings > 0 ? [counted(warnings, 'warning')] : []),
  ].join(', ')
  const counts = `${found} in ${counted(files, 'file')}.`
  return errors > 0 ? counts : `${CONFORMS} ${counts}`
}

export function lintReport(findings: readonly Finding[]): string {
  if (findings.length === 0) return CONFORMS
  return [
    ...findings.map(
      (finding) => `${at(finding)}  ${finding.severity}  ${finding.message}`
    ),
    '',
    summary(findings),
  ].join('\n')
}
