import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
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

  test('overwrites a file of the same name on a repeated export', async () => {
    await write({ 'less.md': 'Hello world!' })
    await write({ 'less.md': 'Hi' })

    expect(readBack('Ben Notebook/less.md')).toBe('Hi')
  })

  test('leaves files it did not write alone', async () => {
    const notebookDir = join(destination, 'Ben Notebook')
    mkdirSync(notebookDir, { recursive: true })
    writeFileSync(join(notebookDir, 'scratch.md'), 'keep me', 'utf8')
    writeFileSync(join(destination, 'unrelated.txt'), 'not mine', 'utf8')

    await write({ 'less.md': 'Hello' })

    expect(readBack('Ben Notebook/less.md')).toBe('Hello')
    expect(readBack('Ben Notebook/scratch.md')).toBe('keep me')
    expect(readBack('unrelated.txt')).toBe('not mine')
  })
})
