import { renderDiffReport, renderNoteDiff } from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { classifyPreviewPullNotes } from './previewPullActions.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PULL = 'No changes to pull.'

export type PreviewPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

function renderClassifiedNote(
  note: ReturnType<typeof classifyPreviewPullNotes>[number]
): string {
  const diff = renderNoteDiff(
    note.path,
    note.workspaceContent,
    note.exportContent,
    undefined,
    note.action
  )
  if (note.action !== 'move') return diff
  const [heading, ...rest] = diff.split('\n')
  return [`${heading}`, `  from ${note.fromPath}`, ...rest].join('\n')
}

/**
 * Report what pulling the notebook would change in the workspace.
 *
 * The notebook is exported afresh on every run and compared against the
 * workspace as it stands, so nothing is remembered between runs and a
 * difference is reported whichever side it came from. The workspace is only
 * ever read.
 */
export async function previewPull({
  notebookId,
  workspacePath,
  exportNotebookAsZip,
  signal,
}: PreviewPullRequest): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  const exported = unzipToEntries(bytes)

  const reported = classifyPreviewPullNotes(workspace, exported).map(
    (note) => ({
      diff: renderClassifiedNote(note),
    })
  )

  return renderDiffReport(reported, NOTHING_TO_PULL)
}
