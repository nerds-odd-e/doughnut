import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, posix } from 'node:path'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'

const MARKDOWN_SUFFIX = '.md'

export const NOTHING_TO_PULL = 'No changes to pull.'

export type ApplyPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

function summary(updated: number): string {
  if (updated === 0) return NOTHING_TO_PULL
  return updated === 1 ? '1 note updated.' : `${updated} notes updated.`
}

/**
 * Write remote note content into matching local Markdown files.
 *
 * Only paths that already exist in the workspace are considered. Remote-only
 * notes are not created locally; local-only files are not removed or changed
 * unless the export also contains that path with different content.
 */
export async function applyPull({
  notebookId,
  workspacePath,
  exportNotebookAsZip,
  signal,
}: ApplyPullRequest): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  const exported = unzipToEntries(bytes)

  let updated = 0
  for (const [path, localContent] of workspace) {
    if (!path.endsWith(MARKDOWN_SUFFIX)) continue
    const remote = exported.get(path)
    if (remote === undefined || remote === localContent) continue

    const full = join(workspacePath, ...path.split(posix.sep))
    mkdirSync(dirname(full), { recursive: true })
    writeFileSync(full, remote, 'utf8')
    updated++
  }

  return summary(updated)
}
