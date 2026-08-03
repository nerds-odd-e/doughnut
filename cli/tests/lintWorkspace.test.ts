import { describe, expect, test } from 'vitest'
import { lintWorkspace } from '../src/lint/lintWorkspace.js'
import { useLintWorkspaceFixture } from './lintWorkspaceFixture.js'

describe('lintWorkspace', () => {
  const { workspaceRoot, write, concept, writeRootIndex } =
    useLintWorkspaceFixture()

  test('names the concept the problem was found in', () => {
    write('banana.md', '# banana')
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain('banana.md:1')
  })

  test('reports frontmatter the closing `---` is missing from', () => {
    write('apple.md', '---\ntype: concept\n\n# apple')
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain(
      'Frontmatter is not closed with `---`'
    )
  })

  test('reports frontmatter that does not say what type the concept is', () => {
    write('apple.md', concept('title: apple', 'apple'))
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain(
      'Frontmatter has no `type` key'
    )
  })

  test('reports frontmatter no YAML parser can read', () => {
    write('apple.md', concept('type: concept\n\ttitle: apple', 'apple'))
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain(
      'Frontmatter is not valid YAML'
    )
  })

  test('reports a `type` key left without a value', () => {
    write('apple.md', concept('type:', 'apple'))
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain('`type` has no value')
  })

  test('reports a `type` that is not a string', () => {
    write('apple.md', concept('type: 123', 'apple'))
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain('`type` is not a string')
  })

  test('names the line a `type` without a value is on', () => {
    write('apple.md', concept('title: apple\ntype:', 'apple'))
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain('apple.md:3  error')
  })

  test('names the line `tags` is on', () => {
    write('apple.md', concept('type: concept\ntitle: apple\ntags: fruit', 'a'))
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toContain('apple.md:4  warning')
  })

  test('warns about `tags` that are not a list, without failing the check', () => {
    write('apple.md', concept('type: concept\ntags: fruit', 'apple'))
    writeRootIndex()

    const report = lintWorkspace(workspaceRoot())

    expect(report).toContain('warning  `tags` is not a list')
    expect(report).toContain('Workspace follows the OKF format.')
  })

  test('reports every problem a concept has, not only the first', () => {
    write('apple.md', concept('tags: fruit', 'apple'))
    writeRootIndex()

    const report = lintWorkspace(workspaceRoot())

    expect(report).toContain('Frontmatter has no `type` key')
    expect(report).toContain('`tags` is not a list')
  })

  test('counts a warning apart from an error', () => {
    write('apple.md', concept('type: concept\ntags: fruit', 'apple'))
    write('fruit/banana.md', '# banana')
    writeRootIndex()
    write('fruit/index.md', '# Fruit\n')

    expect(lintWorkspace(workspaceRoot())).toContain(
      '1 error, 1 warning in 2 files.'
    )
  })

  test('counts the problems and the files they were found in', () => {
    write('apple.md', '# apple')
    write('fruit/banana.md', '# banana')
    writeRootIndex()
    write('fruit/index.md', '# Fruit\n')

    expect(lintWorkspace(workspaceRoot())).toContain('2 errors in 2 files.')
  })

  test('asks nothing of a reserved index.md', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('index.md', '# Fruit\n\n- [apple](/apple)\n')

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('reports an index.md carrying frontmatter', () => {
    write('fruit/index.md', concept('type: concept', 'Fruit'))

    expect(lintWorkspace(workspaceRoot())).toContain(
      'fruit/index.md:1  error  An index carries no frontmatter'
    )
  })

  test('accepts `okf_version` in the frontmatter of the root index.md', () => {
    write('index.md', '---\nokf_version: 0.2\n---\n\n# Fruit\n')

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('reports the root index.md carrying more than `okf_version`', () => {
    write('index.md', '---\nokf_version: 0.2\ntitle: Fruit\n---\n\n# Fruit\n')

    expect(lintWorkspace(workspaceRoot())).toContain(
      'index.md:1  error  An index carries no frontmatter beyond `okf_version`'
    )
  })

  test('warns about a file OKF has no rules for, naming no line', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('a.json', '{}')
    writeRootIndex()

    const report = lintWorkspace(workspaceRoot())

    expect(report).toContain('a.json  warning  Not an OKF concept')
    expect(report).toContain(
      'Workspace follows the OKF format. 1 warning in 1 file.'
    )
  })

  test('walks past a dot file as readily as a dot folder', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('.hidden.md', 'no frontmatter here')
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('walks past a dot folder that tooling keeps its own files in', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('.git/config.md', 'no frontmatter here')
    write('.obsidian/plugins/notes.md', 'nor here')
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('reports a log date heading that is not ISO 8601', () => {
    write('fruit/log.md', '# Log\n\n## July 30, 2026\n\n* Added apple\n')

    expect(lintWorkspace(workspaceRoot())).toContain(
      'A log date heading is not `YYYY-MM-DD`'
    )
  })

  test('reports a log date heading shaped right but naming no day', () => {
    write('fruit/log.md', '# Log\n\n## 2026-02-30\n\n## 2026-13-01\n')

    expect(lintWorkspace(workspaceRoot())).toContain('2 errors in 1 file.')
  })

  test('names the line a log date heading is on', () => {
    write('fruit/log.md', '# Log\n\n## 2026-07-30\n\n## July 23, 2026\n')

    expect(lintWorkspace(workspaceRoot())).toContain('fruit/log.md:5  error')
  })

  test('asks nothing of a reserved log.md', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('fruit/log.md', '# Log\n\n## 2026-07-30\n')
    writeRootIndex()

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('reports nothing when every concept has frontmatter', () => {
    write('apple.md', concept('type: concept', 'apple'))
    write('fruit/banana.md', concept('type: concept', 'banana'))
    writeRootIndex()
    write('fruit/index.md', '# Fruit\n')

    expect(lintWorkspace(workspaceRoot())).toBe(
      'Workspace follows the OKF format.'
    )
  })

  /**
   * OKF closes with what a consumer must not reject a bundle over. Each of these
   * would be an easy rule to add and a wrong one, so each is nailed down.
   */
  describe('what OKF says a bundle must not be rejected over', () => {
    test('a type nobody recognises', () => {
      write('apple.md', concept('type: greengrocery-invoice', 'apple'))
      writeRootIndex()

      expect(lintWorkspace(workspaceRoot())).toBe(
        'Workspace follows the OKF format.'
      )
    })

    test('keys OKF says nothing about', () => {
      write(
        'apple.md',
        concept('type: concept\nripeness: 7\nfarm: Ben', 'apple')
      )
      writeRootIndex()

      expect(lintWorkspace(workspaceRoot())).toBe(
        'Workspace follows the OKF format.'
      )
    })
  })
})
