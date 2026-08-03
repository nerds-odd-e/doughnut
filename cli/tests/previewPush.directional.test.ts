import { writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { previewPush } from '../src/sync/previewPush.js'
import { writeNotebookExport } from '../src/sync/writeNotebookExport.js'
import { usePreviewPushWorkspace } from './previewPushHarness.js'
import { zipOfNotes } from './zipFixture.js'

describe('previewPush directional labels', () => {
  const ws = usePreviewPushWorkspace()

  test('does not alter an existing baseline', async () => {
    ws.write('less.md', 'Hello')
    ws.seedBaseline({ 'less.md': 'Hello' })
    const before = ws.readBack(join('.doughnut-sync', 'baseline.json'))

    await ws.preview({ 'less.md': 'Hello world!' })

    expect(ws.readBack(join('.doughnut-sync', 'baseline.json'))).toBe(before)
  })

  test('keeps a note unlabeled when there is no history for it', async () => {
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

  test('labels a note only Doughnut changed since the baseline as a pull', async () => {
    ws.write('less.md', 'Hello')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await expect(ws.preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md (pull)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello',
        '  + Hello world!',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('labels a note only the workspace changed since the baseline as a push', async () => {
    ws.write('less.md', 'Hello from Obsidian')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await expect(ws.preview({ 'less.md': 'Hello' })).resolves.toBe(
      [
        'less.md (push)',
        '  --- Doughnut',
        '  +++ workspace',
        '  - Hello',
        '  + Hello from Obsidian',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('numbers a push diff hunk against Doughnut, the side removed lines come from', async () => {
    const lines = Array.from({ length: 20 }, (_, i) => `line ${i + 1}`)
    const baselineContent = lines.join('\n')
    const editedLines = [...lines]
    editedLines[4] = 'line 5 EDITED'
    editedLines[14] = 'line 15 EDITED'
    ws.write('many.md', editedLines.join('\n'))
    ws.seedBaseline({ 'many.md': baselineContent })

    await expect(ws.preview({ 'many.md': baselineContent })).resolves.toBe(
      [
        'many.md (push)',
        '  --- Doughnut',
        '  +++ workspace',
        '  @@ line 2 @@',
        '    line 2',
        '    line 3',
        '    line 4',
        '  - line 5',
        '  + line 5 EDITED',
        '    line 6',
        '    line 7',
        '    line 8',
        '  @@ line 12 @@',
        '    line 12',
        '    line 13',
        '    line 14',
        '  - line 15',
        '  + line 15 EDITED',
        '    line 16',
        '    line 17',
        '    line 18',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('leaves a note neither side changed since the baseline out of the report', async () => {
    ws.write('less.md', 'Hello')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await expect(ws.preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to push.'
    )
  })

  test('keeps reporting a pull when the preview runs again with nothing edited', async () => {
    ws.write('less.md', 'Hello')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await ws.preview({ 'less.md': 'Hello world!' })

    await expect(ws.preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md (pull)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello',
        '  + Hello world!',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('labels a note (push) on the very first preview when /export primed the workspace', async () => {
    const notes = { 'less.md': 'Hello' }
    const exportNotebookAsZip = () =>
      Promise.resolve({
        bytes: zipOfNotes(notes),
        fileName: 'Ben Notebook.zip',
      })

    await writeNotebookExport({
      notebookId: 1,
      destinationDirectory: ws.workspace,
      exportNotebookAsZip,
    })

    const notebookWorkspace = join(ws.workspace, 'Ben Notebook')
    writeFileSync(
      join(notebookWorkspace, 'less.md'),
      'Hello from Obsidian',
      'utf8'
    )

    await expect(
      previewPush({
        notebookId: 1,
        workspacePath: notebookWorkspace,
        exportNotebookAsZip,
      })
    ).resolves.toBe(
      [
        'less.md (push)',
        '  --- Doughnut',
        '  +++ workspace',
        '  - Hello',
        '  + Hello from Obsidian',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })
})
