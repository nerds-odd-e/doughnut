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
import { previewPush } from '../src/sync/previewPush.js'
import { savePushBaseline } from '../src/sync/pushBaseline.js'
import { zipOfNotes } from './zipFixture.js'

/** Shared temp workspace + preview helpers for previewPush unit suites. */
export function usePreviewPushWorkspace() {
  let workspace = ''

  beforeEach(() => {
    workspace = mkdtempSync(join(tmpdir(), 'doughnut-previewPush-'))
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

  const seedBaseline = (notes: Record<string, string>) => {
    savePushBaseline(workspace, 1, new Map(Object.entries(notes)))
  }

  const preview = (notes: Record<string, string>, path = workspace) =>
    previewPush({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes: zipOfNotes(notes),
          fileName: 'Ben Notebook.zip',
        }),
    })

  return {
    get workspace() {
      return workspace
    },
    write,
    readBack,
    seedBaseline,
    preview,
  }
}
