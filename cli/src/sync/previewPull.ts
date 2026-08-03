import {
  renderDiffReport,
  renderNoteDiff,
  renderRejectFinding,
} from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import {
  classifyPreviewPullNotes,
  type ClassifiedPullNote,
} from './previewPullActions.js'
import { readWorkspace } from './readWorkspace.js'
import { listZipFileNames, unzipToEntries } from './unzip.js'

const NOTHING_TO_PULL = 'No changes to pull.'

export type PreviewPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

function renderClassifiedNote(note: ClassifiedPullNote): {
  readonly diff: string
  readonly pullAction: ClassifiedPullNote['action']
} {
  if (note.action === 'reject') {
    return {
      diff: renderRejectFinding(note.path, note.reason),
      pullAction: 'reject',
    }
  }
  const diff = renderNoteDiff(
    note.path,
    note.workspaceContent,
    note.exportContent,
    undefined,
    note.action
  )
  if (note.action !== 'move') {
    return { diff, pullAction: note.action }
  }
  const [heading, ...rest] = diff.split('\n')
  return {
    diff: [heading, `  from ${note.fromPath}`, ...rest].join('\n'),
    pullAction: 'move',
  }
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
  const zipFileNames = listZipFileNames(bytes)
  const exported = unzipToEntries(bytes)

  const reported = classifyPreviewPullNotes(
    workspace,
    exported,
    zipFileNames
  ).map(renderClassifiedNote)

  return renderDiffReport(reported, NOTHING_TO_PULL)
}
