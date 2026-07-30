import { readWorkspace } from '../sync/readWorkspace.js'
import { inDotFolder, nonMarkdownPaths } from './bundleFiles.js'
import { type Finding, lintReport } from './lintReport.js'
import { conceptProblems } from './okfConcept.js'
import { indexProblems } from './okfIndex.js'
import { logProblems } from './okfLog.js'
import type { OkfProblem } from './okfProblem.js'

function basename(path: string): string {
  return path.slice(path.lastIndexOf('/') + 1)
}

/**
 * Which rules a file answers to. OKF reserves `index.md` and `log.md`, each with
 * a structure of its own rather than a concept's.
 */
function problemsIn(path: string, content: string): OkfProblem[] {
  const name = basename(path)
  if (name === 'index.md') return indexProblems(content, path === name)
  if (name === 'log.md') return logProblems(content)
  return conceptProblems(content)
}

/** OKF reads `.md` and says nothing about the rest, which is worth saying out loud. */
function notAConcept(path: string): Finding {
  return {
    path,
    severity: 'warning',
    message: 'Not an OKF concept; only .md files are checked',
  }
}

export function lintWorkspace(workspace: string): string {
  const inConcepts = [...readWorkspace(workspace)]
    .filter(([path]) => !inDotFolder(path))
    .flatMap(([path, content]) =>
      problemsIn(path, content).map((problem) => ({ ...problem, path }))
    )
  return lintReport([
    ...inConcepts,
    ...nonMarkdownPaths(workspace).map(notAConcept),
  ])
}
