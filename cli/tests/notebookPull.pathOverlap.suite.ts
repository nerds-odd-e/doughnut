import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import {
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

const SHARED_NOTE =
  '---\ntype: Note\n---\n# Note\n\nShared opening.\n\nShared closing.\n'
const LOCAL_OPENING_NOTE =
  '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nShared closing.\n'
const REMOTE_CLOSING_NOTE =
  '---\ntype: Note\n---\n# Note\n\nShared opening.\n\nRemote closing.\n'
const COMBINED_NOTE =
  '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nRemote closing.\n'
const ORIGINAL_BODY_NOTE = '---\ntype: Note\n---\n# Note\n\nOriginal body.\n'
const LOCAL_BODY_NOTE = '---\ntype: Note\n---\n# Note\n\nLocal body.\n'
const REMOTE_BODY_NOTE = '---\ntype: Note\n---\n# Note\n\nRemote body.\n'
const NESTED_BASE_NOTE =
  '---\ntype: Note\nauthored: original\n---\n# Nested\n\nOriginal body.\n'
const NESTED_LOCAL_NOTE =
  '---\ntype: Note\nauthored: local\n---\n# Nested\n\nOriginal body.\n'
const NESTED_REMOTE_NOTE =
  '---\ntype: Note\nauthored: original\n---\n# Nested\n\nRemote body.\n'
const NESTED_COMBINED_NOTE =
  '---\ntype: Note\nauthored: local\n---\n# Nested\n\nRemote body.\n'
const SEVERAL_BASE_NOTE =
  '---\ntype: Note\n---\n# Note\n\nOpening.\n\nClosing.\n'
const SEVERAL_LOCAL_NOTE =
  '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nClosing.\n'
const SEVERAL_COMBINED_NOTE =
  '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nRemote closing three.\n'

export function describeNotebookPullPathOverlap(): void {
  describe('notebook pull (same-note content rebase)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-path-overlap-test-'
    )

    test('rebases disjoint same-note paragraph edits onto accepted history', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        SHARED_NOTE,
        LOCAL_OPENING_NOTE
      )
      commitPortableFile(
        setup.source,
        'note.md',
        REMOTE_CLOSING_NOTE,
        'accepted closing'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'disjoint-paragraphs')
      const originalAuthor = runGit(
        ['log', '-1', '--format=%an <%ae>', setup.localTip],
        setup.directory
      )
      const originalMessage = runGit(
        ['log', '-1', '--format=%s', setup.localTip],
        setup.directory
      )

      await run(['notebook', 'pull', setup.directory])

      const localHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(acceptedHead)
      expect(runGit(['rev-parse', acceptedHead], setup.directory)).toBe(
        acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        COMBINED_NOTE
      )
      expect(runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)).toBe(
        originalAuthor
      )
      expect(runGit(['log', '-1', '--format=%s'], setup.directory)).toBe(
        originalMessage
      )
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['rev-parse', 'ORIG_HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(
        runGit(
          ['config', '--local', '--get', 'donut.notebook-id'],
          setup.directory
        )
      ).toBe('42')
      expect(
        runGit(['for-each-ref', '--format=%(refname)'], setup.directory)
      ).toBe('refs/heads/main')
      expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
      expect(ctx.getFetchMock().mock.calls[0]?.[0]).toContain('/git-bundle')
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased onto the accepted history. Local head: ${localHead}. Accepted head: ${acceptedHead}. Inspect the result with "git status".`
      )
    })

    test('rebases when accepted history edited then restored the same note', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        ORIGINAL_BODY_NOTE,
        LOCAL_BODY_NOTE
      )
      commitPortableFile(
        setup.source,
        'note.md',
        REMOTE_BODY_NOTE,
        'accepted remote edit'
      )
      commitPortableFile(
        setup.source,
        'note.md',
        ORIGINAL_BODY_NOTE,
        'accepted remote revert'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'edit-then-revert')

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(acceptedHead)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_BODY_NOTE
      )
    })

    test('rebases a nested same-note frontmatter edit over an accepted body edit', async () => {
      const workDir = ctx.getWorkDir()
      const source = buildSourceRepo(workDir)
      commitPortableFile(
        source,
        'Nested/Cell.md',
        NESTED_BASE_NOTE,
        'add nested note'
      )
      const directory = cloneAsBoundCheckout(
        workDir,
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      commitPortableFile(
        directory,
        'Nested/Cell.md',
        NESTED_LOCAL_NOTE,
        'unpublished nested frontmatter'
      )
      commitPortableFile(
        source,
        'Nested/Cell.md',
        NESTED_REMOTE_NOTE,
        'accepted nested body'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      serveAcceptedBundle(ctx, source, 'nested-frontmatter')

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD^'], directory)).toBe(acceptedHead)
      expect(
        fs.readFileSync(join(directory, 'Nested', 'Cell.md'), 'utf8')
      ).toBe(NESTED_COMBINED_NOTE)
    })

    test('keeps several accepted same-note commits unchanged under one rebased local child', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        SEVERAL_BASE_NOTE,
        SEVERAL_LOCAL_NOTE
      )
      commitPortableFile(
        setup.source,
        'note.md',
        '---\ntype: Note\n---\n# Note\n\nOpening.\n\nRemote closing one.\n',
        'accepted closing one'
      )
      commitPortableFile(
        setup.source,
        'note.md',
        '---\ntype: Note\n---\n# Note\n\nOpening.\n\nRemote closing two.\n',
        'accepted closing two'
      )
      commitPortableFile(
        setup.source,
        'note.md',
        '---\ntype: Note\n---\n# Note\n\nOpening.\n\nRemote closing three.\n',
        'accepted closing three'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], setup.source)
      const olderAccepted = runGit(['rev-parse', 'HEAD^^'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'several-same-note')

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(acceptedHead)
      expect(runGit(['rev-parse', olderAccepted], setup.directory)).toBe(
        olderAccepted
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        SEVERAL_COMBINED_NOTE
      )
    })
  })
}

function cloneWithLocalNoteEdit(
  workDir: string,
  baseBytes: string,
  localBytes: string
): {
  directory: string
  source: string
  localTip: string
} {
  const source = buildSourceRepo(workDir)
  commitPortableFile(source, 'note.md', baseBytes, 'portable shared base')
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  commitPortableFile(directory, 'note.md', localBytes, 'unpublished note edit')
  return {
    directory,
    source,
    localTip: runGit(['rev-parse', 'HEAD'], directory),
  }
}
