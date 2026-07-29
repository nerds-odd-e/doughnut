import type { ExportNotebookAsZip } from './exportNotebook.js'
import { readWorkspace } from './readWorkspace.js'
import { diffLines } from './unifiedDiff.js'
import { unzipToEntries } from './unzip.js'

const NOTHING_TO_PULL = 'No changes to pull.'

const MARKDOWN_SUFFIX = '.md'

export type PreviewPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

function renderNote(
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

function render(changed: readonly string[]): string {
  if (changed.length === 0) return NOTHING_TO_PULL
  const count =
    changed.length === 1
      ? '1 note would change.'
      : `${changed.length} notes would change.`
  return [...changed, count].join('\n')
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
    .map(([path, content]) =>
      renderNote(path, workspace.get(path) ?? '', content)
    )

  return render(changed)
}
