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
})
