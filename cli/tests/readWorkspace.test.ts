import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { readWorkspace } from '../src/sync/readWorkspace.js'

describe('readWorkspace', () => {
  let root: string

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'doughnut-readWorkspace-'))
  })

  afterEach(() => {
    rmSync(root, { recursive: true, force: true })
  })

  const write = (relativePath: string, content: string) => {
    const full = join(root, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  test('reads a note at the root', () => {
    write('less.md', 'Hello')

    expect(readWorkspace(root)).toEqual(new Map([['less.md', 'Hello']]))
  })

  test('reads notes in folders under their relative path', () => {
    write('intro.md', 'Hello')
    write('LeSS in Action/team.md', 'Sprint')
    write('Engineering/deep/tech.md', 'Trunk')

    expect(readWorkspace(root)).toEqual(
      new Map([
        ['Engineering/deep/tech.md', 'Trunk'],
        ['LeSS in Action/team.md', 'Sprint'],
        ['intro.md', 'Hello'],
      ])
    )
  })

  test('orders entries by path', () => {
    write('LeSS in Action/team.md', 'Sprint')
    write('Engineering/tech.md', 'Trunk')

    expect([...readWorkspace(root).keys()]).toEqual([
      'Engineering/tech.md',
      'LeSS in Action/team.md',
    ])
  })

  test('ignores files that are not Markdown', () => {
    write('less.md', 'Hello')
    write('notes.txt', 'ignored')
    write('.DS_Store', 'ignored')

    expect([...readWorkspace(root).keys()]).toEqual(['less.md'])
  })

  test('reads an empty file as empty content', () => {
    write('less.md', '')

    expect(readWorkspace(root).get('less.md')).toBe('')
  })

  test('reads an empty directory as no notes', () => {
    expect(readWorkspace(root)).toEqual(new Map())
  })

  test('throws when the directory does not exist', () => {
    expect(() => readWorkspace(join(root, 'nowhere'))).toThrow(
      `No directory at ${join(root, 'nowhere')}.`
    )
  })

  test('throws when the path is a file', () => {
    write('less.md', 'Hello')

    expect(() => readWorkspace(join(root, 'less.md'))).toThrow(
      `No directory at ${join(root, 'less.md')}.`
    )
  })
})
