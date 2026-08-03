import { describe, expect, test } from 'vitest'
import { usePreviewPullWorkspace } from './previewPullHarness.js'
import { buildZip } from './zipFixture.js'

describe('previewPull diagnostics', () => {
  const ws = usePreviewPullWorkspace()

  test('reports a move when the same doughnut_id is at a different path', async () => {
    ws.write('less.md', '---\ndoughnut_id: 42\n---\n\n# less\n\nHello')

    const report = await ws.preview({
      'scrum.md': '---\ndoughnut_id: 42\n---\n\n# scrum\n\nHello',
    })

    expect(report).toContain('scrum.md (move)')
    expect(report).toContain('less.md')
    expect(report).not.toMatch(/scrum\.md \(create\)/)
    expect(ws.readBack('less.md')).toBe(
      '---\ndoughnut_id: 42\n---\n\n# less\n\nHello'
    )
  })

  test('does not infer a move when the export note lacks doughnut_id', async () => {
    ws.write('less.md', 'Hello')

    const report = await ws.preview({ 'scrum.md': 'Sprint plan' })

    expect(report).toContain('scrum.md (create)')
    expect(report).not.toContain('(move)')
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
