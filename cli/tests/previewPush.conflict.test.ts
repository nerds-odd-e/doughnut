import { describe, expect, test } from 'vitest'
import { previewPush } from '../src/sync/previewPush.js'
import { savePushBaseline } from '../src/sync/pushBaseline.js'
import { usePreviewPushWorkspace } from './previewPushHarness.js'
import { zipOfNotes } from './zipFixture.js'

describe('previewPush conflicts', () => {
  const ws = usePreviewPushWorkspace()

  test('labels a note CONFLICT when both sides changed and diverged since the baseline', async () => {
    ws.write('less.md', 'Hello from Obsidian')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await expect(ws.preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md (CONFLICT)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello from Obsidian',
        '  + Hello world!',
        '',
        '1 conflict.',
      ].join('\n')
    )
  })

  test('conflict report has no update action label', async () => {
    ws.write('less.md', 'Hello from Obsidian')
    ws.seedBaseline({ 'less.md': 'Hello' })

    const report = await ws.preview({ 'less.md': 'Hello world!' })

    expect(report).toContain('less.md (CONFLICT)')
    expect(report).not.toMatch(/\(update\)/)
  })

  test('keeps reporting a conflict when the preview runs again with nothing edited', async () => {
    ws.write('less.md', 'Hello from Obsidian')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await ws.preview({ 'less.md': 'Hello world!' })

    await expect(ws.preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      [
        'less.md (CONFLICT)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello from Obsidian',
        '  + Hello world!',
        '',
        '1 conflict.',
      ].join('\n')
    )
  })

  test('leaves a note both sides changed to the same content out of the report', async () => {
    ws.write('less.md', 'Hello world!')
    ws.seedBaseline({ 'less.md': 'Hello' })

    await expect(ws.preview({ 'less.md': 'Hello world!' })).resolves.toBe(
      'No changes to push.'
    )
  })

  test('counts conflicts apart from the notes that would change', async () => {
    ws.write('less.md', 'Hello from Obsidian')
    ws.write('scrum.md', 'Sprint plan A')
    ws.seedBaseline({ 'less.md': 'Hello', 'scrum.md': 'Sprint' })

    await expect(
      ws.preview({ 'less.md': 'Hello', 'scrum.md': 'Sprint plan B' })
    ).resolves.toBe(
      [
        'less.md (push)',
        '  --- Doughnut',
        '  +++ workspace',
        '  - Hello',
        '  + Hello from Obsidian',
        '',
        'scrum.md (CONFLICT)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Sprint plan A',
        '  + Sprint plan B',
        '',
        '1 note would change. 1 conflict.',
      ].join('\n')
    )
  })

  test('falls back to the bootstrap diff when the baseline belongs to a different notebook', async () => {
    ws.write('less.md', 'Hello')
    savePushBaseline(ws.workspace, 1, new Map([['less.md', 'Hello']]))

    await expect(
      previewPush({
        notebookId: 2,
        workspacePath: ws.workspace,
        exportNotebookAsZip: () =>
          Promise.resolve({
            bytes: zipOfNotes({ 'less.md': 'Hello world!' }),
            fileName: 'Other Notebook.zip',
          }),
      })
    ).resolves.toBe(
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
})
