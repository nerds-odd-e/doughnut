import { execFileSync } from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  bindNotebookCheckout,
  configureTestGitIdentity,
} from './notebookGit.testHelpers.js'
import {
  bundleMain,
  stubFetchForSubmission,
} from './notebookPublish.testHelpers.js'

const acceptedContent = '---\ntype: Note\n---\nAccepted body\n'
const editedContent = '---\ntype: Note\n---\nSmall authored edit\n'

function appendNotes(
  directory: string,
  paths: string[],
  message: string,
  content: string,
  parent?: string
) {
  let stream = `commit refs/heads/main\ncommitter T <t@example.com> 1 +0000\ndata ${message.length}\n${message}\n`
  if (parent) stream += `from ${parent}\n`
  for (const path of paths) {
    stream += `M 100644 inline ${path}\ndata ${Buffer.byteLength(content)}\n${content}\n`
  }
  execFileSync('git', ['fast-import', '--quiet'], {
    cwd: directory,
    input: stream,
  })
  return runGit(['rev-parse', 'main'], directory)
}

export function describeNotebookPublishLargeTree() {
  describe('publishing a small commit from a large notebook', () => {
    const ctx = installNotebookCliRunFixture('donut-publish-large-tree-')

    test('submits three committed Markdown edits above a one MiB accepted listing and retains local history', async () => {
      const directory = join(ctx.getWorkDir(), 'checkout')
      fs.mkdirSync(directory)
      runGit(['init', '--quiet', '-b', 'main'], directory)
      configureTestGitIdentity(directory)
      bindNotebookCheckout(directory, getApiConfig().apiBaseUrl)
      const folder = [
        'accepted-folder-',
        'retained-folder-',
        'notebook-folder-',
      ]
        .map((name) => name.repeat(14))
        .join('/')
      const paths = Array.from(
        { length: 1500 },
        (_, index) => `${folder}/note-${String(index).padStart(5, '0')}.md`
      )
      const accepted = appendNotes(
        directory,
        paths,
        'accepted',
        acceptedContent
      )
      const listing = execFileSync('git', ['ls-tree', '-r', '-z', accepted], {
        cwd: directory,
        maxBuffer: 4 * 1024 * 1024,
      })
      expect(listing.byteLength).toBeGreaterThan(1024 * 1024)
      const bundle = join(ctx.getWorkDir(), 'accepted.bundle')
      bundleMain(directory, bundle)
      const changedPaths = paths.slice(0, 3)
      const proposed = appendNotes(
        directory,
        changedPaths,
        'pending',
        editedContent,
        accepted
      )
      runGit(['reset', '--quiet', '--hard', 'main'], directory)
      expect(
        runGit(['diff', '--name-only', accepted, proposed], directory).split(
          '\n'
        )
      ).toEqual(changedPaths)
      const backend = stubFetchForSubmission(bundle, {
        status: 200,
        ok: true,
        text: () => Promise.resolve(proposed),
      })
      const output = vi
        .spyOn(console, 'log')
        .mockImplementation(() => undefined)

      try {
        await run(['notebook', 'publish', directory])
      } catch (cause) {
        throw new Error(
          `Publishing over ${listing.byteLength} accepted listing bytes failed: ${ctx.getErrorSpy().mock.calls.flat().join('\n')}`,
          { cause }
        )
      }

      const posts = backend.mock.calls.filter(
        ([, init]) => init?.method === 'POST'
      )
      expect(posts).toHaveLength(1)
      expect(String(posts[0]![0])).toContain(
        `/api/notebooks/42/git-bundle?expectedHead=${accepted}`
      )
      const submitted = join(ctx.getWorkDir(), 'submitted.bundle')
      fs.writeFileSync(submitted, (posts[0]![1] as { body: Buffer }).body)
      expect(runGit(['bundle', 'list-heads', submitted], directory)).toBe(
        `${proposed} refs/heads/main`
      )
      expect(output).toHaveBeenCalledWith(
        `Published notebook. Accepted head: ${proposed}.`
      )
      expect(runGit(['rev-parse', 'main'], directory)).toBe(proposed)
      expect(runGit(['rev-parse', 'main^'], directory)).toBe(accepted)
      expect(runGit(['branch', '--show-current'], directory)).toBe('main')
      expect(runGit(['status', '--porcelain'], directory)).toBe('')
      expect(
        changedPaths.map((path) =>
          fs.readFileSync(join(directory, path), 'utf8')
        )
      ).toEqual(changedPaths.map(() => editedContent))
      output.mockRestore()
    })
  })
}
