import { renderDiffReport, renderNoteDiff } from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { savePushBaseline } from './pushBaseline.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PUSH = 'No changes to push.'

const MARKDOWN_SUFFIX = '.md'

export type PreviewPushRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

/**
 * Report what pushing the workspace would change in Doughnut.
 *
 * Bootstrap only: every note is compared straight to the fresh export, the
 * same way `previewPull` compares its two sides, with no distinction yet
 * between a remote change and a local one — that needs the baseline this run
 * persists, and is added in a later phase. Doughnut and the workspace are
 * only ever read; the only write is this run's baseline snapshot.
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

  const changed = [...markdownExported]
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .filter(([path, content]) => workspace.get(path) !== content)
    .map(([path, content]) =>
      renderNoteDiff(path, workspace.get(path) ?? '', content)
    )

  savePushBaseline(workspacePath, notebookId, new Map(markdownExported))

  return renderDiffReport(changed, NOTHING_TO_PUSH)
}
