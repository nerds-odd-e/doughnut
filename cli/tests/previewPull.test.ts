import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import type { ExportNotebook } from '../src/sync/exportNotebook.js'
import { previewPull } from '../src/sync/previewPull.js'

describe('previewPull', () => {
  let workspace: string
  let exportedInto: string | undefined

  beforeEach(() => {
    workspace = mkdtempSync(join(tmpdir(), 'doughnut-previewPull-'))
    exportedInto = undefined
  })

  afterEach(() => {
    rmSync(workspace, { recursive: true, force: true })
  })

  const writeInto = (root: string, relativePath: string, content: string) => {
    const full = join(root, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  const write = (relativePath: string, content: string) =>
    writeInto(workspace, relativePath, content)

  const readBack = (relativePath: string) =>
    readFileSync(join(workspace, relativePath), 'utf8')

  /** An export that writes the given notes and records where it was asked to write. */
  const exportOf =
    (notes: Record<string, string>): ExportNotebook =>
    (_notebookId, targetDirectory) => {
      exportedInto = targetDirectory
      for (const [path, content] of Object.entries(notes)) {
        writeInto(targetDirectory, path, content)
      }
      return Promise.resolve()
    }

  const preview = (notes: Record<string, string>, path = workspace) =>
    previewPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebook: exportOf(notes),
    })

  test('reports a changed note as a diff', async () => {
    write('less.md', 'Hello')

    await expect(preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md',
        '  - Hello',
        '  + Hello world!',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('reports two changed notes', async () => {
    write('less.md', 'Hello')
    write('scrum.md', 'Sprint')

    await expect(
      preview({ 'less.md': 'Hello world!', 'scrum.md': 'Sprint review' })
    ).resolves.toBe(
      [
        'less.md',
        '  - Hello',
        '  + Hello world!',
        '',
        'scrum.md',
        '  - Sprint',
        '  + Sprint review',
        '',
        '2 notes would change.',
      ].join('\n')
    )
  })

  test('leaves an unchanged note out of the report', async () => {
    write('less.md', 'Hello')
    write('scrum.md', 'Sprint')

    const report = await preview({
      'less.md': 'Hello world!',
      'scrum.md': 'Sprint',
    })

    expect(report).toContain('less.md')
    expect(report).not.toContain('scrum.md')
  })

  test('reports the path of a note in a folder', async () => {
    write('LeSS in Action/team.md', 'Sprint')

    await expect(
      preview({ 'LeSS in Action/team.md': 'Sprint review' })
    ).resolves.toBe(
      [
        'LeSS in Action/team.md',
        '  - Sprint',
        '  + Sprint review',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('orders changed notes by path', async () => {
    write('LeSS in Action/team.md', 'Sprint')
    write('Engineering/tech.md', 'Trunk')

    const report = await preview({
      'LeSS in Action/team.md': 'Sprint review',
      'Engineering/tech.md': 'Trunk based',
    })

    expect(report.indexOf('Engineering/tech.md')).toBeLessThan(
      report.indexOf('LeSS in Action/team.md')
    )
  })

  test('reports a locally edited note as what a pull would overwrite', async () => {
    write('less.md', 'Hello from Obsidian')

    await expect(preview({ 'less.md': 'Hello' })).resolves.toBe(
      [
        'less.md',
        '  - Hello from Obsidian',
        '  + Hello',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('reports nothing to pull when the two sides match', async () => {
    write('less.md', 'Hello')

    await expect(preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to pull.'
    )
  })

  test('does not write to the workspace', async () => {
    write('less.md', 'Hello')

    await preview({ 'less.md': 'Hello world!' })

    expect(readBack('less.md')).toBe('Hello')
  })

  test('removes the scratch directory it exported into', async () => {
    write('less.md', 'Hello')

    await preview({ 'less.md': 'Hello world!' })

    expect(exportedInto).toBeDefined()
    expect(existsSync(exportedInto!)).toBe(false)
  })

  test('exports into a scratch directory outside the workspace', async () => {
    write('less.md', 'Hello')

    await preview({ 'less.md': 'Hello world!' })

    expect(exportedInto!.startsWith(workspace)).toBe(false)
  })

  test('reports the same difference when run twice', async () => {
    write('less.md', 'Hello')

    const first = await preview({ 'less.md': 'Hello world!' })
    const second = await preview({ 'less.md': 'Hello world!' })

    expect(second).toBe(first)
  })

  test('reports a missing workspace directory', async () => {
    await expect(preview({}, join(workspace, 'nowhere'))).rejects.toThrow(
      `No directory at ${join(workspace, 'nowhere')}.`
    )
  })

  test('removes the scratch directory when the export fails', async () => {
    write('less.md', 'Hello')
    let attempted: string | undefined

    await expect(
      previewPull({
        notebookId: 1,
        workspacePath: workspace,
        exportNotebook: (_id, targetDirectory) => {
          attempted = targetDirectory
          return Promise.reject(new Error('export blew up'))
        },
      })
    ).rejects.toThrow('export blew up')

    expect(attempted).toBeDefined()
    expect(existsSync(attempted!)).toBe(false)
  })
})
