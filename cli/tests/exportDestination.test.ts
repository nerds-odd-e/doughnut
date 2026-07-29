import { mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { parseExportDestination } from '../src/sync/exportDestination.js'

describe('parseExportDestination', () => {
  let root: string

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'doughnut-exportDestination-'))
  })

  afterEach(() => {
    rmSync(root, { recursive: true, force: true })
  })

  test('rejects a missing argument', () => {
    expect(parseExportDestination(undefined)).toEqual({
      error: 'Usage: /export <destination directory>',
    })
  })

  test('rejects a blank argument', () => {
    expect(parseExportDestination('   ')).toEqual({
      error: 'Usage: /export <destination directory>',
    })
  })

  test('resolves an existing directory', () => {
    expect(parseExportDestination(root)).toEqual({ directory: root })
  })

  test('rejects a path that does not exist', () => {
    const missing = join(root, 'nowhere')

    expect(parseExportDestination(missing)).toEqual({
      error: `No directory at ${missing}.`,
    })
  })

  test('rejects a path that is a file', () => {
    const file = join(root, 'less.md')
    writeFileSync(file, 'Hello', 'utf8')

    expect(parseExportDestination(file)).toEqual({
      error: `No directory at ${file}.`,
    })
  })

  test('resolves a relative path against the current working directory', () => {
    expect(parseExportDestination('nowhere-relative')).toEqual({
      error: `No directory at ${resolve(process.cwd(), 'nowhere-relative')}.`,
    })
  })

  test('strips surrounding quotes before resolving, as a shell would', () => {
    expect(parseExportDestination(`"${root}"`)).toEqual({ directory: root })
  })

  test('accepts a directory reached through a symlink', () => {
    const link = `${root}-link`
    symlinkSync(root, link)

    try {
      expect(parseExportDestination(link)).toEqual({ directory: link })
    } finally {
      rmSync(link, { force: true })
    }
  })
})
