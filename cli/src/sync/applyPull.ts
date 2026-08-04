import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, posix } from 'node:path'
import { renderRejectFinding } from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import {
  classifyPreviewPullNotes,
  type ClassifiedPullNote,
} from './previewPullActions.js'
import { loadPushBaseline, savePushBaseline } from './pushBaseline.js'
import { readWorkspace } from './readWorkspace.js'
import { listZipFileNames, unzipToEntries } from './unzip.js'

export const NOTHING_TO_PULL = 'No changes to pull.'

export type ApplyPullRequest = {
  readonly notebookId: number
  readonly workspacePath: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

function summary(mutated: number, rejectLines: readonly string[]): string {
  const parts: string[] = []
  if (mutated > 0) {
    parts.push(mutated === 1 ? '1 note updated.' : `${mutated} notes updated.`)
  }
  if (rejectLines.length > 0) {
    parts.push(rejectLines.join('\n'))
  }
  if (parts.length === 0) return NOTHING_TO_PULL
  return parts.join('\n')
}

function workspaceFullPath(workspacePath: string, relative: string): string {
  return join(workspacePath, ...relative.split(posix.sep))
}

function writeNote(
  workspacePath: string,
  relative: string,
  content: string
): void {
  const full = workspaceFullPath(workspacePath, relative)
  mkdirSync(dirname(full), { recursive: true })
  writeFileSync(full, content, 'utf8')
}

/**
 * Write remote note content into the local Markdown workspace.
 *
 * Classifies the export the same way dry-run does, then applies create and
 * update. Rejects are reported and never written. Sync baseline updates only
 * after at least one successful mutation.
 */
export async function applyPull({
  notebookId,
  workspacePath,
  exportNotebookAsZip,
  signal,
}: ApplyPullRequest): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  const zipFileNames = listZipFileNames(bytes)
  const exported = unzipToEntries(bytes)

  const classified = classifyPreviewPullNotes(workspace, exported, zipFileNames)

  let mutated = 0
  const rejectLines: string[] = []
  const nextBaseline = new Map(loadPushBaseline(workspacePath, notebookId))

  for (const note of classified) {
    applyClassifiedNote(note, workspacePath, nextBaseline, rejectLines, () => {
      mutated++
    })
  }

  if (mutated > 0) {
    savePushBaseline(workspacePath, notebookId, nextBaseline)
  }

  return summary(mutated, rejectLines)
}

function applyClassifiedNote(
  note: ClassifiedPullNote,
  workspacePath: string,
  nextBaseline: Map<string, string>,
  rejectLines: string[],
  onMutate: () => void
): void {
  if (note.action === 'reject') {
    rejectLines.push(renderRejectFinding(note.path, note.reason))
    return
  }

  writeNote(workspacePath, note.path, note.exportContent)
  nextBaseline.set(note.path, note.exportContent)
  onMutate()
}
