import {
  renderDiffReport,
  renderNoteDiff,
  type NoteDiffStatus,
} from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { loadPushBaseline, savePushBaseline } from './pushBaseline.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PUSH = 'No changes to push.'

const MARKDOWN_SUFFIX = '.md'

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

/**
 * The baseline the next run will compare against: the content the two sides
 * were last seen to agree on for a note — a merge base — never simply the
 * newest remote content.
 *
 * So an entry is recorded only for a note whose two sides agree in this very
 * run, and otherwise the entry already there is carried forward untouched.
 * Recording a side of a difference the run is reporting would make the next
 * run — with nothing edited in between — see only the other side as changed,
 * and name a direction nothing had established: a conflict or a pull read as a
 * push, which is the stale-local-over-newer-remote push this command exists to
 * prevent. A note that has never yet agreed therefore gets no entry at all,
 * this run's remote content included, and stays an unlabeled difference until
 * the two sides do agree once. Paths the export no longer holds are dropped,
 * keeping the baseline to the notebook as it stands.
 */
function nextBaseline(
  baseline: ReadonlyMap<string, string>,
  workspace: ReadonlyMap<string, string>,
  exported: readonly (readonly [string, string])[]
): ReadonlyMap<string, string> {
  return new Map(
    exported.flatMap(([path, remote]) => {
      if (workspace.get(path) === remote) return [[path, remote] as const]
      const agreed = baseline.get(path)
      return agreed === undefined ? [] : [[path, agreed] as const]
    })
  )
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
 * way `previewPull` does, unlabeled.
 * Doughnut and the workspace are only ever read; the only write is the updated
 * baseline.
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

  const markdownExported = [...exported]
    .filter(([path]) => path.endsWith(MARKDOWN_SUFFIX))
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))

  const baseline = loadPushBaseline(workspacePath, notebookId)

  const reported = markdownExported.flatMap(([path, remote]) => {
    const local = workspace.get(path)
    if (local === undefined) return []
    const outcome = classify(baseline.get(path), local, remote)
    if (outcome === 'nothing') return []
    const status = outcome === 'difference' ? undefined : outcome
    return [{ status, diff: renderNoteDiff(path, local, remote, status) }]
  })

  savePushBaseline(
    workspacePath,
    notebookId,
    nextBaseline(baseline, workspace, markdownExported)
  )

  return renderDiffReport(reported, NOTHING_TO_PUSH)
}
