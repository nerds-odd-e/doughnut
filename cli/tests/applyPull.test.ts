import {
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
import { zipOfNotes } from './zipFixture.js'

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

  test('updates a changed note on disk', async () => {
    write('less.md', 'Hello')

    await expect(pull({ 'less.md': 'Hello world!' })).resolves.toBe(
      '1 note updated.'
    )
    expect(readBack('less.md')).toBe('Hello world!')
  })

  test('does not create a file for a remote-only note', async () => {
    write('less.md', 'Hello')

    await expect(
      pull({ 'less.md': 'Hello', 'scrum.md': 'Sprint' })
    ).resolves.toBe(NOTHING_TO_PULL)
    expect(() => readBack('scrum.md')).toThrow()
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
