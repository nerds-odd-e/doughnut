import { renderDiffReport, renderNoteDiff } from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PULL = 'No changes to pull.'

const MARKDOWN_SUFFIX = '.md'

export type PreviewPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
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

  const changed = [...exported]
    .filter(([path]) => path.endsWith(MARKDOWN_SUFFIX))
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .filter(([path, content]) => workspace.get(path) !== content)
    .map(([path, content]) => ({
      diff: renderNoteDiff(path, workspace.get(path) ?? '', content),
    }))

  return renderDiffReport(changed, NOTHING_TO_PULL)
}
