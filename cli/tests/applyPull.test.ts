import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { applyPull, NOTHING_TO_PULL } from '../src/sync/applyPull.js'
import { buildZip, zipOfNotes } from './zipFixture.js'

describe('applyPull', () => {
  let workspace: string

  beforeEach(() => {
    workspace = mkdtempSync(join(tmpdir(), 'doughnut-applyPull-'))
  })

  afterEach(() => {
    rmSync(workspace, { recursive: true, force: true })
  })

  const write = (relativePath: string, content: string) => {
    const full = join(workspace, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  const readBack = (relativePath: string) =>
    readFileSync(join(workspace, relativePath), 'utf8')

  const baselinePath = () => join(workspace, '.doughnut-sync', 'baseline.json')

  const readBaseline = () => JSON.parse(readFileSync(baselinePath(), 'utf8'))

  const pull = (notes: Record<string, string>, path = workspace) =>
    applyPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes: zipOfNotes(notes),
          fileName: 'Ben Notebook.zip',
        }),
    })

  const pullZip = (bytes: Buffer, path = workspace) =>
    applyPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes,
          fileName: 'Ben Notebook.zip',
        }),
    })

  test('updates a changed note on disk', async () => {
    write('less.md', 'Hello')

    await expect(pull({ 'less.md': 'Hello world!' })).resolves.toBe(
      '1 note updated.'
    )
    expect(readBack('less.md')).toBe('Hello world!')
  })

  test('creates a file for a remote-only note', async () => {
    write('less.md', 'Hello')

    const result = await pull({ 'less.md': 'Hello', 'scrum.md': 'Sprint' })
    expect(result).not.toBe(NOTHING_TO_PULL)
    expect(result).toMatch(/1 note updated/)
    expect(readBack('scrum.md')).toBe('Sprint')
    expect(readBack('less.md')).toBe('Hello')
  })

  test('rejects a reserved log.md without writing it', async () => {
    write('less.md', 'Hello')

    const result = await pull({
      'less.md': 'Hello',
      'log.md': 'Daily standup notes',
    })

    expect(result).toContain('log.md (reject)')
    expect(result.toLowerCase()).toMatch(/reserved/)
    expect(() => readBack('log.md')).toThrow()
    expect(existsSync(baselinePath())).toBe(false)
  })

  test('rejects duplicate export paths without writing them', async () => {
    write('less.md', 'Hello')

    const result = await pullZip(
      buildZip([
        { name: 'twin.md', content: 'First' },
        { name: 'twin.md', content: 'Second' },
      ])
    )

    expect(result).toContain('twin.md (reject)')
    expect(result.toLowerCase()).toMatch(/duplicate/)
    expect(() => readBack('twin.md')).toThrow()
    expect(existsSync(baselinePath())).toBe(false)
  })

  test('rejects an unsafe path without writing the workspace', async () => {
    write('less.md', 'Hello')

    const result = await pullZip(
      buildZip([{ name: '../evil.md', content: 'pwned' }])
    )

    expect(result).toContain('../evil.md (reject)')
    expect(result.toLowerCase()).toMatch(/unsafe|invalid/)
    expect(readBack('less.md')).toBe('Hello')
    expect(existsSync(baselinePath())).toBe(false)
  })

  test('writes baseline after a mutating create', async () => {
    write('less.md', 'Hello')

    await pull({ 'less.md': 'Hello', 'scrum.md': 'Sprint' })

    expect(readBaseline()).toEqual({
      notebookId: 1,
      notes: { 'scrum.md': 'Sprint' },
    })
  })

  test('does not write baseline on matching-content no-op', async () => {
    write('less.md', 'Hello')

    await expect(pull({ 'less.md': 'Hello' })).resolves.toBe(NOTHING_TO_PULL)
    expect(existsSync(baselinePath())).toBe(false)
  })

  test('leaves a local-only file unchanged when another note updates', async () => {
    write('less.md', 'Hello')
    write('Less 2.md', 'local only')

    await expect(pull({ 'less.md': 'Hello world!' })).resolves.toBe(
      '1 note updated.'
    )
    expect(readBack('less.md')).toBe('Hello world!')
    expect(readBack('Less 2.md')).toBe('local only')
  })

  test('reports nothing to pull when intersecting content matches', async () => {
    write('less.md', 'Hello')

    await expect(pull({ 'less.md': 'Hello' })).resolves.toBe(NOTHING_TO_PULL)
  })

  test('updates one changed note among 1000 within 5 seconds', async () => {
    const notes: Record<string, string> = {}
    for (let i = 1; i <= 1000; i++) {
      const path = `note-${String(i).padStart(4, '0')}.md`
      const body = i === 500 ? 'seed' : 'seed'
      write(path, body)
      notes[path] = body
    }
    notes['note-0500.md'] = 'changed body'

    const started = performance.now()
    await expect(pull(notes)).resolves.toBe('1 note updated.')
    expect(performance.now() - started).toBeLessThan(5000)
    expect(readBack('note-0500.md')).toBe('changed body')
  })
})
