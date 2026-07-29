import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { lintWorkspace } from '../src/lint/lintWorkspace.js'

describe('lintWorkspace', () => {
  let root: string

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'doughnut-lintWorkspace-'))
  })

  afterEach(() => {
    rmSync(root, { recursive: true, force: true })
  })

  const write = (relativePath: string, content: string) => {
    const full = join(root, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  test('names the concept the problem was found in', () => {
    write('banana.md', '# banana')

    expect(lintWorkspace(root)).toContain('banana.md:1')
  })

  test('reports frontmatter the closing `---` is missing from', () => {
    write('apple.md', '---\ntype: concept\n\n# apple')

    expect(lintWorkspace(root)).toContain(
      'Frontmatter is not closed with `---`'
    )
  })

  test('reports frontmatter that does not say what type the concept is', () => {
    write('apple.md', '---\ntitle: apple\n---\n\n# apple')

    expect(lintWorkspace(root)).toContain('Frontmatter has no `type` key')
  })

  test('reports nothing when every concept has frontmatter', () => {
    write('apple.md', '---\ntype: concept\n---\n\n# apple')
    write('fruit/banana.md', '---\ntype: concept\n---\n\n# banana')

    expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
  })
})
