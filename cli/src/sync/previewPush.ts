import { renderDiffReport, renderNoteDiff } from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { loadPushBaseline, savePushBaseline } from './pushBaseline.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PUSH = 'No changes to push.'

const MARKDOWN_SUFFIX = '.md'

/**
 * What a note's difference means: nothing to report, a difference whose
 * direction is not yet known, or the direction it would flow in.
 */
type NoteOutcome = 'nothing' | 'difference' | 'pull' | 'push'

/**
 * Which side moved since the baseline last recorded the remote content.
 *
 * With no baseline for this note there is no history to compare against, so a
 * difference is reported as it stands. With both sides moved, converging on the
 * same content leaves nothing to reconcile and diverging is a conflict — until
 * conflicts get a label of their own, that difference is reported unlabeled
 * rather than guessed at as a pull or a push.
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
  }
  return local === remote ? 'nothing' : 'difference'
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
 * Each note is compared against the remote content the last run recorded, so a
 * difference can say which side moved: `(pull)` when only Doughnut changed,
 * `(push)` when only the workspace did. The first run in a workspace has no
 * such record and reports the plain difference the way `previewPull` does.
 * Doughnut and the workspace are only ever read; the only write is this run's
 * baseline snapshot.
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

  const markdownExported = [...exported].filter(([path]) =>
    path.endsWith(MARKDOWN_SUFFIX)
  )

  const baseline = loadPushBaseline(workspacePath, notebookId)

  const changed = [...markdownExported]
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .flatMap(([path, remote]) => {
      const local = workspace.get(path) ?? ''
      const outcome = classify(baseline.get(path), local, remote)
      if (outcome === 'nothing') return []
      const status = outcome === 'difference' ? undefined : outcome
      return [renderNoteDiff(path, local, remote, status)]
    })

  savePushBaseline(workspacePath, notebookId, new Map(markdownExported))

  return renderDiffReport(changed, NOTHING_TO_PUSH)
}
