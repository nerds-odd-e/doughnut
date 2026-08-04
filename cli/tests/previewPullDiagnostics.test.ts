import { describe, expect, test } from 'vitest'
import { usePreviewPullWorkspace } from './previewPullHarness.js'
import { buildZip } from './zipFixture.js'

describe('previewPull diagnostics', () => {
  const ws = usePreviewPullWorkspace()

  test('creates a remote-only note by path when workspace has a different file', async () => {
    ws.write('less.md', 'Hello')

    const report = await ws.preview({ 'scrum.md': 'Sprint plan' })

    expect(report).toContain('scrum.md (create)')
    expect(ws.readBack('less.md')).toBe('Hello')
  })

  test('rejects a reserved log.md basename with a short reason', async () => {
    ws.write('less.md', 'Hello')

    const report = await ws.preview({
      'less.md': 'Hello',
      'log.md': 'Daily standup notes',
    })

    expect(report).toContain('log.md (reject)')
    expect(report.toLowerCase()).toMatch(/reserved/)
    expect(report).not.toMatch(/log\.md \(create\)/)
    expect(report).not.toMatch(/log\.md \(update\)/)
  })

  test('rejects a reserved index.md even when content differs', async () => {
    ws.write('index.md', 'Old readme')

    const report = await ws.preview({ 'index.md': 'New readme' })

    expect(report).toContain('index.md (reject)')
    expect(report.toLowerCase()).toMatch(/reserved/)
    expect(report).not.toMatch(/index\.md \(update\)/)
  })

  test('rejects paths under .doughnut-sync as sync metadata', async () => {
    const report = await ws.preview({
      '.doughnut-sync/baseline.json.md': 'not a note',
    })

    expect(report).toContain('.doughnut-sync/baseline.json.md (reject)')
    expect(report.toLowerCase()).toMatch(/sync metadata|doughnut-sync/)
    expect(report).not.toBe('No changes to pull.')
  })

  test('rejects duplicate export paths', async () => {
    ws.write('less.md', 'Hello')

    const report = await ws.previewZip(
      buildZip([
        { name: 'twin.md', content: 'First' },
        { name: 'twin.md', content: 'Second' },
      ])
    )

    expect(report).toContain('twin.md (reject)')
    expect(report.toLowerCase()).toMatch(/duplicate/)
    expect(report).not.toBe('No changes to pull.')
  })

  test('rejects an unsafe path without writing the workspace', async () => {
    ws.write('less.md', 'Hello')

    const report = await ws.previewZip(
      buildZip([{ name: '../evil.md', content: 'pwned' }])
    )

    expect(report).toContain('../evil.md (reject)')
    expect(report.toLowerCase()).toMatch(/unsafe|invalid/)
    expect(ws.readBack('less.md')).toBe('Hello')
  })

  test('reports rejects-only instead of the clean no-op sentinel', async () => {
    const report = await ws.preview({ 'log.md': 'reserved only' })

    expect(report).toContain('log.md (reject)')
    expect(report).not.toBe('No changes to pull.')
  })
})
