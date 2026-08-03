import { existsSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { previewPush } from '../src/sync/previewPush.js'
import { usePreviewPushWorkspace } from './previewPushHarness.js'
import { zipOfNotes } from './zipFixture.js'

describe('previewPush', () => {
  const ws = usePreviewPushWorkspace()

  test('reports a changed note as a diff', async () => {
    ws.write('less.md', 'Hello')

    await expect(ws.preview({ 'less.md': 'Hello world!' })).resolves.toBe(
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
    ws.write('less.md', 'Hello')
    ws.write('scrum.md', 'Sprint')

    await expect(
      ws.preview({ 'less.md': 'Hello world!', 'scrum.md': 'Sprint review' })
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
    ws.write('less.md', 'Hello')
    ws.write('scrum.md', 'Sprint')

    const report = await ws.preview({
      'less.md': 'Hello world!',
      'scrum.md': 'Sprint',
    })

    expect(report).toContain('less.md')
    expect(report).not.toContain('scrum.md')
  })

  test('reports the path of a note in a folder', async () => {
    ws.write('LeSS in Action/team.md', 'Sprint')

    await expect(
      ws.preview({ 'LeSS in Action/team.md': 'Sprint review' })
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

  test('ignores an exported file that is not Markdown', async () => {
    ws.write('less.md', 'Hello')

    await expect(
      ws.preview({ 'less.md': 'Hello', 'notes.txt': 'whatever' })
    ).resolves.toBe('No changes to push.')
  })

  test('reports nothing to push when the two sides match', async () => {
    ws.write('less.md', 'Hello')

    await expect(ws.preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to push.'
    )
  })

  test('reports nothing to push when only the line endings differ', async () => {
    ws.write('less.md', 'Sprint planning\r\nDaily standup')

    await expect(
      ws.preview({ 'less.md': 'Sprint planning\nDaily standup' })
    ).resolves.toBe('No changes to push.')
  })

  test('does not write to the workspace', async () => {
    ws.write('less.md', 'Hello')

    await ws.preview({ 'less.md': 'Hello world!' })

    expect(ws.readBack('less.md')).toBe('Hello')
  })

  test('does not write sync metadata', async () => {
    ws.write('less.md', 'Hello')

    await ws.preview({ 'less.md': 'Hello world!' })

    expect(existsSync(join(ws.workspace, '.doughnut-sync'))).toBe(false)
  })

  test('reports a missing workspace directory', async () => {
    await expect(ws.preview({}, join(ws.workspace, 'nowhere'))).rejects.toThrow(
      `No directory at ${join(ws.workspace, 'nowhere')}.`
    )
  })

  test('reads the workspace before asking for an export', async () => {
    let asked = false

    await expect(
      previewPush({
        notebookId: 1,
        workspacePath: join(ws.workspace, 'nowhere'),
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
    ws.write('less.md', 'Hello')

    await expect(
      previewPush({
        notebookId: 1,
        workspacePath: ws.workspace,
        exportNotebookAsZip: () =>
          Promise.reject(
            new Error('Ben Notebook no longer exists in Doughnut.')
          ),
      })
    ).rejects.toThrow('Ben Notebook no longer exists in Doughnut.')
  })
})
