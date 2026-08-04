import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { lintWorkspace } from '../src/lint/lintWorkspace.js'
import { useLintWorkspaceFixture } from './lintWorkspaceFixture.js'

describe('lintWorkspace portable knowledge contract', () => {
  const { workspaceRoot, write, concept, writeRootIndex } =
    useLintWorkspaceFixture()

  test('reports a link to a concept that is not in the bundle', () => {
    write('apple.md', `${concept('type: concept', 'apple')}\n\n[go](/pear)`)
    writeRootIndex()

    const report = lintWorkspace(workspaceRoot())
    expect(report).toMatch(/error/i)
    expect(report).toMatch(/pear|link|missing|broken/i)
  })

  test('reports one concept carrying only a `type`, and no index.md', () => {
    write('apple.md', concept('type: concept', 'apple'))

    const report = lintWorkspace(workspaceRoot())
    expect(report).toMatch(/index\.md/i)
    expect(report).not.toBe('Workspace follows the OKF format.')
  })

  test('reports a broken wiki target', () => {
    write(
      'apple.md',
      `${concept('type: concept', 'apple')}\n\nSee [[missing-note]]`
    )
    writeRootIndex()

    const report = lintWorkspace(workspaceRoot())
    expect(report).toMatch(/error/i)
    expect(report).toMatch(/missing-note|link|broken|missing/i)
  })

  test('does not flag remote https or /attachments/ links', () => {
    write(
      'apple.md',
      `${concept('type: concept', 'apple')}\n\n[web](https://example.com/a)\n[img](/attachments/images/1/x.png)`
    )
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('reports an unsafe local link target', () => {
    write(
      'apple.md',
      `${concept('type: concept', 'apple')}\n\n[out](../outside.md)`
    )
    writeRootIndex()

    const report = lintWorkspace(workspaceRoot())
    expect(report).toMatch(/error/i)
    expect(report).toMatch(/unsafe path/i)
  })

  test('ignores empty directories when checking for index.md', () => {
    write('apple.md', concept('type: concept', 'apple'))
    writeRootIndex()
    mkdirSync(join(workspaceRoot(), 'empty-dir'), { recursive: true })

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })
})
