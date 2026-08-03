import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach } from 'vitest'
import { previewPull } from '../src/sync/previewPull.js'
import { zipOfNotes } from './zipFixture.js'

/** Shared temp workspace + preview helpers for previewPull unit suites. */
export function usePreviewPullWorkspace() {
  let workspace = ''

  beforeEach(() => {
    workspace = mkdtempSync(join(tmpdir(), 'doughnut-previewPull-'))
  })

  afterEach(() => {
    rmSync(workspace, { recursive: true, force: true })
  })

  const write = (relativePath: string, content: string) => {
    const full = join(workspace, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  const readBack = (relativePath: string) =>
    readFileSync(join(workspace, relativePath), 'utf8')

  const preview = (notes: Record<string, string>, path = workspace) =>
    previewPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes: zipOfNotes(notes),
          fileName: 'Ben Notebook.zip',
        }),
    })

  const previewZip = (bytes: Buffer, path = workspace) =>
    previewPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes,
          fileName: 'Ben Notebook.zip',
        }),
    })

  return {
    get workspace() {
      return workspace
    },
    write,
    readBack,
    preview,
    previewZip,
  }
}
