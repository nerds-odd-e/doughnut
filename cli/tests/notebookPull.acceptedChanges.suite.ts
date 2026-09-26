import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleMain,
  cloneAsBoundCheckout,
  postCount,
  stubFetchWithBundleFile,
} from './notebookPublish.testHelpers.js'
import { abortPausedRebase } from './notebookPull.conflict.testHelpers.js'
import {
  commitFileChangeSet,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
  type FileChange,
} from './notebookPull.testHelpers.js'

const note = (body: string) => `---\ntype: Note\n---\n# Note\n\n${body}\n`
/** Publish uploads every non-empty attachment at the proposed tip via Git LFS, so these stay empty. */
const attachment = ''

function commitGit(directory: string, args: string[], message: string): void {
  runGit(args, directory)
  runGit(['commit', '--quiet', '-m', message], directory)
}

function divergedCheckout(
  workDir: string,
  shape: {
    base: readonly FileChange[]
    accepted: (source: string) => void
    local: (directory: string) => void
  }
) {
  const source = buildSourceRepo(workDir)
  if (shape.base.length > 0) {
    commitFileChangeSet(source, shape.base, 'shared base')
  }
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  shape.local(directory)
  shape.accepted(source)
  return {
    source,
    directory,
    localTip: runGit(['rev-parse', 'HEAD'], directory),
    acceptedHead: runGit(['rev-parse', 'main'], source),
  }
}

export function describeNotebookPullAcceptedChanges(): void {
  describe('notebook pull (any accepted file or folder change)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-accepted-changes-test-'
    )

    test.each([
      {
        shape: 'a local note edit over a web deletion of a root file',
        base: [{ path: 'fake.png', content: attachment }],
        accepted: (source: string) =>
          commitGit(source, ['rm', '--quiet', 'fake.png'], 'web delete'),
        local: (directory: string) =>
          commitFileChangeSet(
            directory,
            [{ path: 'note.md', content: note('Local edit.') }],
            'local edit'
          ),
        present: { 'note.md': note('Local edit.') },
        absent: ['fake.png'],
      },
      {
        shape: 'a local note addition over another checkout adding a root file',
        base: [],
        accepted: (source: string) =>
          commitFileChangeSet(
            source,
            [{ path: 'd2.bin', content: attachment }],
            'other checkout file'
          ),
        local: (directory: string) =>
          commitFileChangeSet(
            directory,
            [{ path: 'Local.md', content: note('New local note.') }],
            'local note'
          ),
        present: { 'd2.bin': attachment, 'Local.md': note('New local note.') },
        absent: [],
      },
      {
        shape: 'a local root file addition over a folder rename elsewhere',
        base: [{ path: 'Folder/inner/doc.pdf', content: attachment }],
        accepted: (source: string) =>
          commitGit(source, ['mv', 'Folder', 'Renamed'], 'web rename'),
        local: (directory: string) =>
          commitFileChangeSet(
            directory,
            [{ path: 'local.bin', content: attachment }],
            'local file'
          ),
        present: {
          'Renamed/inner/doc.pdf': attachment,
          'local.bin': attachment,
        },
        absent: ['Folder'],
      },
      {
        shape: 'a local note and file over another checkout note and file',
        base: [],
        accepted: (source: string) =>
          commitFileChangeSet(
            source,
            [
              { path: 'Other.md', content: note('Other checkout.') },
              { path: 'other.bin', content: attachment },
            ],
            'other checkout note and file'
          ),
        local: (directory: string) =>
          commitFileChangeSet(
            directory,
            [
              { path: 'Mine.md', content: note('Mine.') },
              { path: 'mine.bin', content: attachment },
            ],
            'local note and file'
          ),
        present: {
          'Other.md': note('Other checkout.'),
          'other.bin': attachment,
          'Mine.md': note('Mine.'),
          'mine.bin': attachment,
        },
        absent: [],
      },
      {
        shape: 'a local edit of a note inside a folder renamed elsewhere',
        base: [{ path: 'Old/a.md', content: note('Original.') }],
        accepted: (source: string) =>
          commitGit(source, ['mv', 'Old', 'New'], 'web rename'),
        local: (directory: string) =>
          commitFileChangeSet(
            directory,
            [{ path: 'Old/a.md', content: note('Local edit.') }],
            'local edit'
          ),
        present: { 'New/a.md': note('Local edit.') },
        absent: ['Old'],
      },
    ])(
      'rebases $shape, then publishes',
      async ({ base, accepted, local, present, absent }) => {
        const setup = divergedCheckout(ctx.getWorkDir(), {
          base,
          accepted,
          local,
        })
        serveAcceptedBundle(ctx, setup.source, 'accepted-changes')

        await run(['notebook', 'pull', setup.directory])

        expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
          setup.acceptedHead
        )
        expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
        for (const [path, content] of Object.entries(present)) {
          expect(fs.readFileSync(join(setup.directory, path), 'utf8')).toBe(
            content
          )
        }
        for (const path of absent) {
          expect(fs.existsSync(join(setup.directory, path))).toBe(false)
        }

        const acceptedBundle = join(ctx.getWorkDir(), 'accepted.bundle')
        bundleMain(setup.source, acceptedBundle)
        const fetchMock = stubFetchWithBundleFile(acceptedBundle)

        await run(['notebook', 'publish', setup.directory])

        expect(postCount(fetchMock)).toBe(1)
      }
    )

    test('pauses when accepted history deletes a note the local work edited, and abort restores the local work', async () => {
      const setup = divergedCheckout(ctx.getWorkDir(), {
        base: [{ path: 'gone.md', content: note('Original.') }],
        accepted: (source) =>
          commitGit(source, ['rm', '--quiet', 'gone.md'], 'web delete'),
        local: (directory) =>
          commitFileChangeSet(
            directory,
            [{ path: 'gone.md', content: note('Local edit.') }],
            'local edit'
          ),
      })
      serveAcceptedBundle(ctx, setup.source, 'modify-delete')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining(
          'Git paused a rebase with a conflict in "gone.md".'
        )
      )
      expect(runGit(['ls-files', '-u'], setup.directory)).toContain('gone.md')

      abortPausedRebase(setup.directory)

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(fs.readFileSync(join(setup.directory, 'gone.md'), 'utf8')).toBe(
        note('Local edit.')
      )
    })
  })
}
