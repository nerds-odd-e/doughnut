import {
  mkdirSync,
  mkdtempSync,
  rmSync,
  symlinkSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { parseExportDestination } from '../src/sync/exportDestination.js'

const homedir = vi.hoisted(() => vi.fn())
vi.mock('node:os', async (importOriginal) => {
  const os = await importOriginal<typeof import('node:os')>()
  return { ...os, homedir }
})

describe('parseExportDestination', () => {
  let root: string

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'doughnut-exportDestination-'))
    homedir.mockReturnValue(root)
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

  test('expands a bare ~ to the home directory', () => {
    expect(parseExportDestination('~')).toEqual({ directory: root })
  })

  test('expands ~/... against the home directory', () => {
    const sub = join(root, 'download')
    mkdirSync(sub)

    expect(parseExportDestination('~/download')).toEqual({ directory: sub })
  })

  test("rejects another user's home directory shorthand rather than creating a literal directory", () => {
    expect(parseExportDestination('~otheruser')).toEqual({
      error:
        "Cannot expand ~otheruser: only the current user's home directory (~) is supported.",
    })
  })
})
