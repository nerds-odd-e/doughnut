import type { PreviewPullAction } from './previewPullActions.js'
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
 * flow. Pull dry-run instead passes `action` (`create` / `update` / …) so the
 * Story 2 taxonomy stays off the push status union. For `push`, the diff is
 * shown notebook-to-workspace (what pushing would write into Doughnut); every
 * other case (including unlabeled) is shown workspace-to-notebook, as
 * `/sync --dry-run` already reads.
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
  status?: NoteDiffStatus,
  action?: PreviewPullAction
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
  const paren =
    action !== undefined
      ? action
      : status !== undefined
        ? labelOf(status)
        : undefined
  const heading = paren === undefined ? path : `${path} (${paren})`
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
  /** Pull dry-run action; rejects are counted apart from notes that would change. */
  readonly pullAction?: PreviewPullAction
}

/** Path + short reason for a pull reject — no unified hunks. */
export function renderRejectFinding(path: string, reason: string): string {
  return [`${path} (reject)`, `  ${reason}`, ''].join('\n')
}

const counted = (count: number, noun: string) =>
  `${count} ${noun}${count === 1 ? '' : 's'}`

/**
 * The reported notes, or `nothingChanged` when there is nothing to report.
 * Conflicts and pull rejects are counted on their own, apart from the notes
 * that would change, because neither is something a run could apply as a
 * content write.
 */
export function renderDiffReport(
  reported: readonly ReportedNoteDiff[],
  nothingChanged: string
): string {
  if (reported.length === 0) return nothingChanged
  const conflicts = reported.filter(
    ({ status }) => status === 'conflict'
  ).length
  const rejects = reported.filter(
    ({ pullAction }) => pullAction === 'reject'
  ).length
  const changes = reported.length - conflicts - rejects
  const summary = [
    ...(changes === 0 ? [] : [`${counted(changes, 'note')} would change.`]),
    ...(rejects === 0 ? [] : [`${counted(rejects, 'reject')}.`]),
    ...(conflicts === 0 ? [] : [`${counted(conflicts, 'conflict')}.`]),
  ].join(' ')
  return [...reported.map(({ diff }) => diff), summary].join('\n')
}
