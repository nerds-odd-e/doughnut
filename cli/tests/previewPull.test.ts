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
import { previewPull } from '../src/sync/previewPull.js'
import { zipOfNotes } from './zipFixture.js'

describe('previewPull', () => {
  let workspace: string

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

  test('reports a changed note as a diff', async () => {
    write('less.md', 'Hello')

    await expect(preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md',
        '  --- workspace',
        '  +++ Doughnut',
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
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello',
        '  + Hello world!',
        '',
        'scrum.md',
        '  --- workspace',
        '  +++ Doughnut',
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
        '  --- workspace',
        '  +++ Doughnut',
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

  test('compares a note the export writes with its title as a heading', async () => {
    write('less.md', '# less\n\nHello')

    await expect(
      preview({ 'less.md': '# less\n\nHello world!' })
    ).resolves.toBe(
      [
        'less.md',
        '  --- workspace',
        '  +++ Doughnut',
        '    # less',
        '    ',
        '  - Hello',
        '  + Hello world!',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('ignores an exported file that is not Markdown', async () => {
    write('less.md', 'Hello')

    await expect(
      preview({ 'less.md': 'Hello', 'notes.txt': 'whatever' })
    ).resolves.toBe('No changes to pull.')
  })

  test('reports a locally edited note as what a pull would overwrite', async () => {
    write('less.md', 'Hello from Obsidian')

    await expect(preview({ 'less.md': 'Hello' })).resolves.toBe(
      [
        'less.md',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello from Obsidian',
        '  + Hello',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  // The side headers name the two sides of the comparison, not two files the
  // way `git diff`'s `/dev/null` does, so the side a pull would write into is
  // still `workspace` when it holds no file for the note yet.
  test('reports a note the pull would create as all added lines', async () => {
    write('less.md', 'Hello')

    await expect(
      preview({ 'less.md': 'Hello', 'scrum.md': 'Sprint plan' })
    ).resolves.toBe(
      [
        'scrum.md',
        '  --- workspace',
        '  +++ Doughnut',
        '  + Sprint plan',
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

  test('reports nothing to pull when only the line endings differ', async () => {
    write('less.md', 'Sprint planning\r\nDaily standup')

    await expect(
      preview({ 'less.md': 'Sprint planning\nDaily standup' })
    ).resolves.toBe('No changes to pull.')
  })

  test('does not write to the workspace', async () => {
    write('less.md', 'Hello')

    await preview({ 'less.md': 'Hello world!' })

    expect(readBack('less.md')).toBe('Hello')
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

  test('reads the workspace before asking for an export', async () => {
    let asked = false

    await expect(
      previewPull({
        notebookId: 1,
        workspacePath: join(workspace, 'nowhere'),
        exportNotebookAsZip: () => {
          asked = true
          return Promise.resolve({
            bytes: zipOfNotes({}),
            fileName: 'Ben Notebook.zip',
          })
        },
      })
    ).rejects.toThrow('No directory at')

    expect(asked).toBe(false)
  })

  test('surfaces a failed export', async () => {
    write('less.md', 'Hello')

    await expect(
      previewPull({
        notebookId: 1,
        workspacePath: workspace,
        exportNotebookAsZip: () =>
          Promise.reject(
            new Error('Ben Notebook no longer exists in Doughnut.')
          ),
      })
    ).rejects.toThrow('Ben Notebook no longer exists in Doughnut.')
  })
})
