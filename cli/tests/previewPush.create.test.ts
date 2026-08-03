import { describe, expect, test } from 'vitest'
import { usePreviewPushWorkspace } from './previewPushHarness.js'

describe('previewPush creates and reserved paths', () => {
  const ws = usePreviewPushWorkspace()

  test('reports a remote-only note as a create', async () => {
    ws.write('less.md', 'Hello')

    await expect(
      ws.preview({ 'less.md': 'Hello', 'scrum.md': 'Sprint plan' })
    ).resolves.toBe(
      [
        'scrum.md (create)',
        '  --- workspace',
        '  +++ Doughnut',
        '  + Sprint plan',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('reports a local-only note as a create', async () => {
    ws.write('less.md', 'Hello')
    ws.write('scrum.md', 'Sprint plan')

    await expect(ws.preview({ 'less.md': 'Hello' })).resolves.toBe(
      [
        'scrum.md (create)',
        '  --- Doughnut',
        '  +++ workspace',
        '  + Sprint plan',
        '',
        '1 note would change.',
      ].join('\n')
    )
  })

  test('omits reserved index and log paths from ordinary create rows', async () => {
    ws.write('less.md', 'Hello')
    ws.write('index.md', 'Index body')
    ws.write('log.md', 'Log body')

    await expect(ws.preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to push.'
    )
  })

  test('omits sync-metadata paths from ordinary create rows', async () => {
    ws.write('less.md', 'Hello')
    ws.write('.doughnut-sync/note.md', 'Should not appear')

    await expect(ws.preview({ 'less.md': 'Hello' })).resolves.toBe(
      'No changes to push.'
    )
  })
})
