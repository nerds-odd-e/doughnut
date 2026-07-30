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
import { previewPush } from '../src/sync/previewPush.js'
import { zipOfNotes } from './zipFixture.js'

describe('previewPush', () => {
  let workspace: string

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

  test('ignores an exported file that is not Markdown', async () => {
    write('less.md', 'Hello')

    await expect(
      preview({ 'less.md': 'Hello', 'notes.txt': 'whatever' })
    ).resolves.toBe('No changes to push.')
  })

  test('reports nothing to push when the two sides match', async () => {
    write('less.md', 'Hello')

    await expect(preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to push.'
    )
  })

  test('reports nothing to push when only the line endings differ', async () => {
    write('less.md', 'Sprint planning\r\nDaily standup')

    await expect(
      preview({ 'less.md': 'Sprint planning\nDaily standup' })
    ).resolves.toBe('No changes to push.')
  })

  test('does not write to the workspace', async () => {
    write('less.md', 'Hello')

    await preview({ 'less.md': 'Hello world!' })

    expect(readBack('less.md')).toBe('Hello')
  })

  test('writes the baseline file with the exported content', async () => {
    write('less.md', 'Hello')

    await preview({ 'less.md': 'Hello world!' })

    const raw = readBack(join('.doughnut-sync', 'baseline.json'))
    expect(JSON.parse(raw)).toEqual({
      notebookId: 1,
      notes: { 'less.md': 'Hello world!' },
    })
  })

  test('labels a note only Doughnut changed since the last run as a pull', async () => {
    write('less.md', 'Hello')
    await preview({ 'less.md': 'Hello' })

    await expect(preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md (pull)',
        '  - Hello',
        '  + Hello world!',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('labels a note only the workspace changed since the last run as a push', async () => {
    write('less.md', 'Hello')
    await preview({ 'less.md': 'Hello' })

    write('less.md', 'Hello from Obsidian')

    await expect(preview({ 'less.md': 'Hello' })).resolves.toBe(
      [
        'less.md (push)',
        '  - Hello from Obsidian',
        '  + Hello',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('leaves a note neither side changed since the last run out of the report', async () => {
    write('less.md', 'Hello')
    await preview({ 'less.md': 'Hello' })

    await expect(preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to push.'
    )
  })

  test('leaves a note unlabeled while both sides differ from the baseline', async () => {
    write('less.md', 'Hello')
    await preview({ 'less.md': 'Hello' })

    write('less.md', 'Hello from Obsidian')

    await expect(preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md',
        '  - Hello from Obsidian',
        '  + Hello world!',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('reports a missing workspace directory', async () => {
    await expect(preview({}, join(workspace, 'nowhere'))).rejects.toThrow(
      `No directory at ${join(workspace, 'nowhere')}.`
    )
  })

  test('reads the workspace before asking for an export', async () => {
    let asked = false

    await expect(
      previewPush({
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
      previewPush({
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
