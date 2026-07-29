import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { writeNotebookExport } from '../src/sync/writeNotebookExport.js'
import { zipOfNotes } from './zipFixture.js'

describe('writeNotebookExport', () => {
  let destination: string

  beforeEach(() => {
    destination = mkdtempSync(join(tmpdir(), 'doughnut-writeExport-'))
  })

  afterEach(() => {
    rmSync(destination, { recursive: true, force: true })
  })

  const write = (
    notes: Record<string, string>,
    fileName = 'Ben Notebook.zip'
  ) =>
    writeNotebookExport({
      notebookId: 1,
      destinationDirectory: destination,
      exportNotebookAsZip: () =>
        Promise.resolve({ bytes: zipOfNotes(notes), fileName }),
    })

  const readBack = (relativePath: string) =>
    readFileSync(join(destination, relativePath), 'utf8')

  test('writes the notebook under a subdirectory named after it', async () => {
    await write({
      'index.md': 'About this notebook',
      'less.md': 'Hello',
      'LeSS in Action/team.md': 'Sprint',
    })

    expect(readBack('Ben Notebook/index.md')).toBe('About this notebook')
    expect(readBack('Ben Notebook/less.md')).toBe('Hello')
    expect(readBack('Ben Notebook/LeSS in Action/team.md')).toBe('Sprint')
  })

  /**
   * Paths are ordered by code point, as `previewPull` and `readWorkspace`
   * already order theirs, so `/export` and `/sync --dry-run` list the same
   * notebook the same way.
   */
  test('reports where it wrote and what it wrote, ordered by path', async () => {
    const summary = await write({
      'index.md': 'About this notebook',
      'LeSS in Action/team.md': 'Sprint',
    })

    expect(summary).toBe(
      [
        `Exported to ${join(destination, 'Ben Notebook')}`,
        '  LeSS in Action/team.md',
        '  index.md',
        '',
        '2 files written.',
      ].join('\n')
    )
  })
})
