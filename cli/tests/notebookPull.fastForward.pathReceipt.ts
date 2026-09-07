import * as fs from 'node:fs'
import { dirname, join } from 'node:path'
import { expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleGetResponse,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import type { installNotebookPullAcceptedHistoryTest } from './notebookPull.testHelpers.js'

type AcceptedHistoryFixture = ReturnType<
  typeof installNotebookPullAcceptedHistoryTest
>

async function pullAcceptedPathChangeThenEdit(
  ctx: AcceptedHistoryFixture,
  source: string,
  directory: string,
  fromPath: string,
  toPath: string,
  editedBytes: Buffer,
  bundleName: string
): Promise<{ acceptedHead: string; acceptedTree: string }> {
  const destDir = dirname(toPath)
  if (destDir !== '.') {
    fs.mkdirSync(join(source, destDir))
  }
  runGit(['mv', fromPath, toPath], source)
  runGit(['commit', '--quiet', '-m', 'accepted path change'], source)
  fs.writeFileSync(join(source, toPath), editedBytes)
  runGit(['add', toPath], source)
  runGit(['commit', '--quiet', '-m', 'accepted edit after path change'], source)
  const acceptedHead = runGit(['rev-parse', 'main'], source)
  const acceptedTree = runGit(['rev-parse', 'main^{tree}'], source)
  const bundleFile = join(ctx.getWorkDir(), bundleName)
  bundleMain(source, bundleFile)
  ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))
  await run(['notebook', 'pull', directory])
  return { acceptedHead, acceptedTree }
}

export function describeNotebookPullFastForwardPathReceipt(
  ctx: AcceptedHistoryFixture
): void {
  test('receives an accepted rename and a later edit with exact bytes, no leftover old filename, and no Portable metadata', async () => {
    const source = buildSourceRepo(ctx.getWorkDir())
    const directory = cloneAsBoundCheckout(
      ctx.getWorkDir(),
      source,
      getApiConfig().apiBaseUrl,
      'checkout'
    )
    const originalHead = runGit(['rev-parse', 'HEAD'], directory)
    const editedBytes = Buffer.from(
      '---\ntype: Note\nauthored: retained\n---\n# Renamed note\n\nEdited body.\n'
    )
    const { acceptedHead, acceptedTree } = await pullAcceptedPathChangeThenEdit(
      ctx,
      source,
      directory,
      'note.md',
      'Renamed note.md',
      editedBytes,
      'accepted-rename-then-edit.bundle'
    )

    expect(fs.existsSync(join(directory, 'note.md'))).toBe(false)
    expect(fs.readFileSync(join(directory, 'Renamed note.md'))).toEqual(
      editedBytes
    )
    expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
    expect(runGit(['rev-parse', 'HEAD^{tree}'], directory)).toBe(acceptedTree)
    expect(() =>
      runGit(['merge-base', '--is-ancestor', originalHead, 'HEAD'], directory)
    ).not.toThrow()
    expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
    expect(runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory)).toBe(
      'Renamed note.md'
    )
  })

  test('receives an accepted relocation and a later edit at the final folder path', async () => {
    const source = buildSourceRepo(ctx.getWorkDir())
    fs.mkdirSync(join(source, 'Source'))
    runGit(['mv', 'note.md', 'Source/note.md'], source)
    runGit(['commit', '--quiet', '-m', 'place note in Source'], source)
    const directory = cloneAsBoundCheckout(
      ctx.getWorkDir(),
      source,
      getApiConfig().apiBaseUrl,
      'checkout'
    )
    const originalHead = runGit(['rev-parse', 'HEAD'], directory)
    const editedBytes = Buffer.from(
      '---\ntype: Note\nauthored: retained\n---\n# Relocated note\n\nEdited body.\n'
    )
    const { acceptedHead, acceptedTree } = await pullAcceptedPathChangeThenEdit(
      ctx,
      source,
      directory,
      'Source/note.md',
      'Dest/note.md',
      editedBytes,
      'accepted-relocation-then-edit.bundle'
    )

    expect(fs.existsSync(join(directory, 'Source/note.md'))).toBe(false)
    expect(fs.readFileSync(join(directory, 'Dest/note.md'))).toEqual(
      editedBytes
    )
    expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
    expect(runGit(['rev-parse', 'HEAD^{tree}'], directory)).toBe(acceptedTree)
    expect(() =>
      runGit(['merge-base', '--is-ancestor', originalHead, 'HEAD'], directory)
    ).not.toThrow()
    expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
    expect(runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory)).toBe(
      'Dest/note.md'
    )
  })
}
