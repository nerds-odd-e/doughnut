import { diffLines } from './unifiedDiff.js'

/**
 * One note's diff, formatted the way `/sync --dry-run` and `/push --dry-run`
 * both report it. `status`, when given, is appended to the path header in
 * parentheses — `less.md (push)` — to say which way the difference would
 * flow. For `push`, the diff is shown remote-to-local (what pushing would
 * write into Doughnut); every other case (including unlabeled) is shown
 * workspace-to-notebook, as `/sync --dry-run` already reads.
 */
export function renderNoteDiff(
  path: string,
  workspaceContent: string,
  notebookContent: string,
  status?: string
): string {
  const [before, after] =
    status === 'push'
      ? [notebookContent, workspaceContent]
      : [workspaceContent, notebookContent]
  const body = diffLines(before, after).flatMap((hunk) => [
    ...(hunk.header === undefined ? [] : [`  @@ line ${hunk.header} @@`]),
    ...hunk.lines.map(({ kind, text }) =>
      kind === 'context'
        ? `    ${text}`
        : `  ${kind === 'removed' ? '-' : '+'} ${text}`
    ),
  ])
  const heading = status === undefined ? path : `${path} (${status})`
  return [heading, ...body, ''].join('\n')
}

/** The list of changed notes, or `nothingChanged` when there is nothing to report. */
export function renderDiffReport(
  changed: readonly string[],
  nothingChanged: string
): string {
  if (changed.length === 0) return nothingChanged
  const count =
    changed.length === 1
      ? '1 note would change.'
      : `${changed.length} notes would change.`
  return [...changed, count].join('\n')
}
