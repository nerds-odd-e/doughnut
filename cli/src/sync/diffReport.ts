import { diffLines } from './unifiedDiff.js'

/** One note's diff, formatted the way `/sync --dry-run` and `/push --dry-run` both report it. */
export function renderNoteDiff(
  path: string,
  workspaceContent: string,
  notebookContent: string
): string {
  const body = diffLines(workspaceContent, notebookContent).flatMap((hunk) => [
    ...(hunk.header === undefined ? [] : [`  @@ line ${hunk.header} @@`]),
    ...hunk.lines.map(({ kind, text }) =>
      kind === 'context'
        ? `    ${text}`
        : `  ${kind === 'removed' ? '-' : '+'} ${text}`
    ),
  ])
  return [path, ...body, ''].join('\n')
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
