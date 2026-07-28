import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { ExportNotebook } from './exportNotebook.js'
import { readWorkspace } from './readWorkspace.js'
import { diffLines } from './unifiedDiff.js'

const SCRATCH_PREFIX = 'doughnut-sync-'

const NOTHING_TO_PULL = 'No changes to pull.'

export type PreviewPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebook: ExportNotebook
  readonly signal?: AbortSignal
}

function renderNote(
  path: string,
  workspaceContent: string,
  notebookContent: string
): string {
  const hunks = diffLines(workspaceContent, notebookContent)
  const body = hunks.flatMap((hunk) => [
    ...(hunk.header === undefined ? [] : [`  @@ line ${hunk.header} @@`]),
    ...hunk.lines.map(({ kind, text }) => {
      if (kind === 'context') return `    ${text}`
      return `  ${kind === 'removed' ? '-' : '+'} ${text}`
    }),
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
 * The notebook is exported afresh into a scratch directory, compared against
 * the workspace, and the scratch directory is removed however the run ends.
 * Nothing is remembered between runs and the workspace is never written to, so
 * a difference is reported whichever side it came from.
 */
export async function previewPull({
  notebookId,
  workspacePath,
  exportNotebook,
  signal,
}: PreviewPullRequest): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const scratch = mkdtempSync(join(tmpdir(), SCRATCH_PREFIX))
  try {
    await exportNotebook(notebookId, scratch, signal)
    const notebook = readWorkspace(scratch)
    const changed = [...notebook]
      .filter(([path, content]) => workspace.get(path) !== content)
      .map(([path, content]) =>
        renderNote(path, workspace.get(path) ?? '', content)
      )
    return render(changed)
  } finally {
    rmSync(scratch, { recursive: true, force: true })
  }
}
