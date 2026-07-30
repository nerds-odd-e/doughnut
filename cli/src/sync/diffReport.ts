import { diffLines } from './unifiedDiff.js'

/** How a note's difference is labeled, and which way its diff then reads. */
export type NoteDiffStatus = 'pull' | 'push' | 'conflict'

/**
 * A conflict is labeled in uppercase: it needs attention, so it stands out
 * from the lowercase directions among a report's other notes. Casing is the
 * report's business, so the status itself stays lowercase like its siblings.
 */
const labelOf = (status: NoteDiffStatus) =>
  status === 'conflict' ? 'CONFLICT' : status

/**
 * One note's diff, formatted the way `/sync --dry-run` and `/push --dry-run`
 * both report it. `status`, when given, is appended to the path header in
 * parentheses — `less.md (push)` — to say which way the difference would
 * flow. For `push`, the diff is shown notebook-to-workspace (what pushing
 * would write into Doughnut); every other case (including unlabeled) is
 * shown workspace-to-notebook, as `/sync --dry-run` already reads.
 *
 * Because that direction flips, `-` and `+` alone cannot say which side a line
 * came from, so each diff names its two sides `git diff` style — `--- <side
 * removed lines come from>`, `+++ <side added lines come from>`. Those are also
 * the sides the hunk `@@ line N @@` markers number against: the `---` one.
 */
export function renderNoteDiff(
  path: string,
  workspaceContent: string,
  notebookContent: string,
  status?: NoteDiffStatus
): string {
  const workspaceSide = { name: 'workspace', content: workspaceContent }
  const doughnutSide = { name: 'Doughnut', content: notebookContent }
  const [before, after] =
    status === 'push'
      ? [doughnutSide, workspaceSide]
      : [workspaceSide, doughnutSide]
  const body = diffLines(before.content, after.content).flatMap((hunk) => [
    ...(hunk.header === undefined ? [] : [`  @@ line ${hunk.header} @@`]),
    ...hunk.lines.map(({ kind, text }) =>
      kind === 'context'
        ? `    ${text}`
        : `  ${kind === 'removed' ? '-' : '+'} ${text}`
    ),
  ])
  const heading = status === undefined ? path : `${path} (${labelOf(status)})`
  return [
    heading,
    `  --- ${before.name}`,
    `  +++ ${after.name}`,
    ...body,
    '',
  ].join('\n')
}

/** One note as a report holds it: its rendered diff, and how it is labeled. */
export type ReportedNoteDiff = {
  readonly diff: string
  readonly status?: NoteDiffStatus
}

const counted = (count: number, noun: string) =>
  `${count} ${noun}${count === 1 ? '' : 's'}`

/**
 * The reported notes, or `nothingChanged` when there is nothing to report.
 * Conflicts are counted on their own, apart from the notes that would change,
 * because a conflict is not something a run could apply.
 */
export function renderDiffReport(
  reported: readonly ReportedNoteDiff[],
  nothingChanged: string
): string {
  if (reported.length === 0) return nothingChanged
  const conflicts = reported.filter(
    ({ status }) => status === 'conflict'
  ).length
  const changes = reported.length - conflicts
  const summary = [
    ...(changes === 0 ? [] : [`${counted(changes, 'note')} would change.`]),
    ...(conflicts === 0 ? [] : [`${counted(conflicts, 'conflict')}.`]),
  ].join(' ')
  return [...reported.map(({ diff }) => diff), summary].join('\n')
}
