import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { loadPushBaseline, savePushBaseline } from '../src/sync/pushBaseline.js'

describe('pushBaseline', () => {
  let workspace: string

  beforeEach(() => {
    workspace = mkdtempSync(join(tmpdir(), 'doughnut-pushBaseline-'))
  })

  afterEach(() => {
    rmSync(workspace, { recursive: true, force: true })
  })

  test('is empty for a workspace that has never saved a baseline', () => {
    expect(loadPushBaseline(workspace, 1)).toEqual(new Map())
  })

  test('round-trips the notes it was given', () => {
    savePushBaseline(
      workspace,
      1,
      new Map([
        ['less.md', 'Hello world!'],
        ['Engineering/tech.md', 'Trunk based'],
      ])
    )

    expect(loadPushBaseline(workspace, 1)).toEqual(
      new Map([
        ['less.md', 'Hello world!'],
        ['Engineering/tech.md', 'Trunk based'],
      ])
    )
  })

  test('treats a baseline saved for a different notebook as absent', () => {
    savePushBaseline(workspace, 1, new Map([['less.md', 'Hello world!']]))

    expect(loadPushBaseline(workspace, 2)).toEqual(new Map())
  })

  test('overwrites a previous save', () => {
    savePushBaseline(workspace, 1, new Map([['less.md', 'Hello']]))
    savePushBaseline(workspace, 1, new Map([['less.md', 'Hello world!']]))

    expect(loadPushBaseline(workspace, 1)).toEqual(
      new Map([['less.md', 'Hello world!']])
    )
  })

  test('writes under a hidden .doughnut-sync directory', () => {
    savePushBaseline(workspace, 1, new Map([['less.md', 'Hello world!']]))

    const raw = readFileSync(
      join(workspace, '.doughnut-sync', 'baseline.json'),
      'utf8'
    )
    expect(JSON.parse(raw)).toEqual({
      notebookId: 1,
      notes: { 'less.md': 'Hello world!' },
    })
  })
})
