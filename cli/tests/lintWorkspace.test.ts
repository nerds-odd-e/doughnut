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

  /** A well-formed concept, so a test varies only what it is about. */
  const concept = (keys: string, body: string) =>
    `---\n${keys}\n---\n\n# ${body}`

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
    write('apple.md', concept('title: apple', 'apple'))

    expect(lintWorkspace(root)).toContain('Frontmatter has no `type` key')
  })

  test('reports frontmatter no YAML parser can read', () => {
    write('apple.md', concept('type: concept\n\ttitle: apple', 'apple'))

    expect(lintWorkspace(root)).toContain('Frontmatter is not valid YAML')
  })

  test('reports a `type` key left without a value', () => {
    write('apple.md', concept('type:', 'apple'))

    expect(lintWorkspace(root)).toContain('`type` has no value')
  })

  test('reports a `type` that is not a string', () => {
    write('apple.md', concept('type: 123', 'apple'))

    expect(lintWorkspace(root)).toContain('`type` is not a string')
  })

  test('warns about `tags` that are not a list, without failing the check', () => {
    write('apple.md', concept('type: concept\ntags: fruit', 'apple'))

    const report = lintWorkspace(root)

    expect(report).toContain('apple.md:1  warning  `tags` is not a list')
    expect(report).toContain('Workspace follows the OKF format.')
  })

  test('counts a warning apart from an error', () => {
    write('apple.md', concept('type: concept\ntags: fruit', 'apple'))
    write('fruit/banana.md', '# banana')

    expect(lintWorkspace(root)).toContain('1 error, 1 warning in 2 files.')
  })

  test('counts the problems and the files they were found in', () => {
    write('apple.md', '# apple')
    write('fruit/banana.md', '# banana')

    expect(lintWorkspace(root)).toContain('2 errors in 2 files.')
  })

  test('asks nothing of a reserved index.md', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('index.md', '# Fruit\n\n- [apple](/apple)\n')

    expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
  })

  test('asks nothing of a reserved log.md', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('fruit/log.md', '# Log\n\n## 2026-07-30\n')

    expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
  })

  test('reports nothing when every concept has frontmatter', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('fruit/banana.md', concept('type: concept', 'banana'))

    expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
  })

  /**
   * OKF closes with what a consumer must not reject a bundle over. Each of these
   * would be an easy rule to add and a wrong one, so each is nailed down.
   */
  describe('what OKF says a bundle must not be rejected over', () => {
    test('a type nobody recognises', () => {
      write('apple.md', concept('type: greengrocery-invoice', 'apple'))

      expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
    })

    test('keys OKF says nothing about', () => {
      write(
        'apple.md',
        concept('type: concept\nripeness: 7\nfarm: Ben', 'apple')
      )

      expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
    })

    test('a link to a concept that is not in the bundle', () => {
      write('apple.md', `${concept('type: concept', 'apple')}\n\n[go](/pear)`)

      expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
    })

    test('one concept carrying only a `type`, and no index.md', () => {
      write('apple.md', concept('type: concept', 'apple'))

      expect(lintWorkspace(root)).toBe('Workspace follows the OKF format.')
    })
  })
})
