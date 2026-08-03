import { basename } from 'node:path'
import {
  renderDiffReport,
  renderNoteDiff,
  type NoteDiffStatus,
  type ReportedNoteDiff,
} from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { classifyCreateOrUpdate } from './previewPullActions.js'
import { loadPushBaseline } from './pushBaseline.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PUSH = 'No changes to push.'

const MARKDOWN_SUFFIX = '.md'

/** Reserved basenames that are never ordinary create/update rows. */
const RESERVED_BASENAMES = new Set(['index.md', 'log.md'])

const SYNC_METADATA_SEGMENT = '.doughnut-sync'

/**
 * What a note's difference means: nothing to report, a difference whose
 * direction is not known, the direction it would flow in, or a conflict. Apart
 * from the first two, an outcome is the status the report labels the note by.
 */
type NoteOutcome = 'nothing' | 'difference' | NoteDiffStatus

/**
 * Which side moved since the content the two sides last agreed on.
 *
 * With no such agreement recorded for this note there is no history to compare
 * against, so a difference is reported as it stands, unlabeled. With both sides
 * moved, converging on the same content leaves nothing to reconcile, while
 * diverging is a conflict: neither side can be taken as the newer one.
 */
function classify(
  baseline: string | undefined,
  local: string,
  remote: string
): NoteOutcome {
  if (baseline !== undefined) {
    const remoteChanged = remote !== baseline
    const localChanged = local !== baseline
    if (remoteChanged !== localChanged) return remoteChanged ? 'pull' : 'push'
    if (remoteChanged && local !== remote) return 'conflict'
  }
  return local === remote ? 'nothing' : 'difference'
}

function isOrdinaryNotePath(path: string): boolean {
  if (path.split('/').includes(SYNC_METADATA_SEGMENT)) return false
  return !RESERVED_BASENAMES.has(basename(path))
}

function reportCreate(
  path: string,
  workspaceContent: string,
  notebookContent: string,
  status: NoteDiffStatus | undefined
): ReportedNoteDiff {
  return {
    status,
    diff: renderNoteDiff(
      path,
      workspaceContent,
      notebookContent,
      status,
      'create'
    ),
  }
}

function reportIntersecting(
  path: string,
  local: string,
  remote: string,
  outcome: Exclude<NoteOutcome, 'nothing'>
): ReportedNoteDiff {
  const status = outcome === 'difference' ? undefined : outcome
  return { status, diff: renderNoteDiff(path, local, remote, status) }
}

export type PreviewPushRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

/**
 * Report what pushing the workspace would change in Doughnut.
 *
 * Each note is compared against the content the two sides were last seen to
 * agree on, so a difference can say which side moved: `(pull)` when only
 * Doughnut changed, `(push)` when only the workspace did, `(CONFLICT)` when
 * both did and they disagree. A note the two sides have never yet been seen to
 * agree on has no such merge base, so its difference is reported plainly the
 * way `previewPull` does, unlabeled. Local-only and remote-only Markdown are
 * reported as creates.
 *
 * Doughnut, the workspace, and sync metadata are only ever read — never written.
 */
export async function previewPush({
  notebookId,
  workspacePath,
  exportNotebookAsZip,
  signal,
}: PreviewPushRequest): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  const exported = unzipToEntries(bytes)

  const markdownExported = new Map(
    [...exported]
      .filter(([path]) => path.endsWith(MARKDOWN_SUFFIX))
      .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
  )

  const baseline = loadPushBaseline(workspacePath, notebookId)

  const paths = [
    ...new Set([
      ...markdownExported.keys(),
      ...[...workspace.keys()].filter((path) => path.endsWith(MARKDOWN_SUFFIX)),
    ]),
  ]
    .filter(isOrdinaryNotePath)
    .sort((a, b) => (a < b ? -1 : a > b ? 1 : 0))

  const reported = paths.flatMap((path): ReportedNoteDiff[] => {
    const local = workspace.get(path)
    const remote = markdownExported.get(path)

    if (remote === undefined) {
      if (local === undefined) return []
      return [reportCreate(path, local, '', 'push')]
    }

    if (local === undefined) {
      const action = classifyCreateOrUpdate(undefined, remote)
      return [
        {
          diff: renderNoteDiff(path, '', remote, undefined, action),
        },
      ]
    }

    const outcome = classify(baseline.get(path), local, remote)
    if (outcome === 'nothing') return []
    return [reportIntersecting(path, local, remote, outcome)]
  })

  return renderDiffReport(reported, NOTHING_TO_PUSH)
}
