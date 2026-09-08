import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION } from '../src/commands/notebook/notebookLocalCandidate.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { commitPortableFile } from './notebookPull.testHelpers.js'

export const LOCAL_ROOT_NOTE =
  '---\ntype: Note\nauthored: root-local\n---\n# Alpha\n\nLocal root body.\n'
export const LOCAL_NESTED_NOTE =
  '---\ntype: Note\nauthored: nested-local\n---\n# Nested\n\nLocal nested body.\n'
export const ACCEPTED_THIRD_NOTE =
  '---\ntype: Note\nauthored: remote-yaml\n---\n# Gamma\n\nAccepted third-note body.\n'

export const TWO_NOTE_UNSUPPORTED_ACCEPTED =
  'Local main cannot receive the accepted history because the two-note unpublished commit can only rebase over exactly one accepted content save of a different existing ordinary Markdown note. ' +
  LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION

export const NOT_EXISTING_NOTE_CONTENT_EDIT =
  'Local main cannot receive the accepted history because the unpublished commit is not one existing-note content edit. Recreate it as one unpublished commit that edits one existing ordinary Markdown note at an unchanged path, then try again.'

export function prepareTwoNoteBatchDivergence(
  workDir: string,
  options?: {
    remote?: (source: string) => void
    localPaths?: 'canonical' | 'three-notes' | 'overlap-root' | 'overlap-nested'
  }
): {
  directory: string
  source: string
  localTip: string
  acceptedHead: string
} {
  const source = buildSourceRepo(workDir)
  fs.mkdirSync(join(source, 'Nested'))
  commitPortableFile(
    source,
    'Nested/Cell.md',
    '---\ntype: Note\n---\n# Nested\n\nOriginal nested.\n',
    'add nested note'
  )
  commitPortableFile(
    source,
    'gamma.md',
    '---\ntype: Note\n---\n# Gamma\n\nOriginal third.\n',
    'add third note'
  )
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )

  const localShape = options?.localPaths ?? 'canonical'
  if (localShape === 'three-notes') {
    fs.writeFileSync(join(directory, 'note.md'), LOCAL_ROOT_NOTE)
    fs.writeFileSync(join(directory, 'Nested/Cell.md'), LOCAL_NESTED_NOTE)
    fs.writeFileSync(join(directory, 'gamma.md'), ACCEPTED_THIRD_NOTE)
    runGit(['add', 'note.md', 'Nested/Cell.md', 'gamma.md'], directory)
  } else {
    fs.writeFileSync(join(directory, 'note.md'), LOCAL_ROOT_NOTE)
    fs.writeFileSync(join(directory, 'Nested/Cell.md'), LOCAL_NESTED_NOTE)
    runGit(['add', 'note.md', 'Nested/Cell.md'], directory)
  }
  runGit(['commit', '--quiet', '-m', 'unpublished two-note batch'], directory)
  const localTip = runGit(['rev-parse', 'HEAD'], directory)

  if (options?.remote !== undefined) {
    options.remote(source)
  } else if (localShape === 'overlap-root') {
    commitPortableFile(
      source,
      'note.md',
      '---\ntype: Note\n---\n# Alpha\n\nAccepted overlapping root.\n',
      'accepted overlapping root save'
    )
  } else if (localShape === 'overlap-nested') {
    commitPortableFile(
      source,
      'Nested/Cell.md',
      '---\ntype: Note\n---\n# Nested\n\nAccepted overlapping nested.\n',
      'accepted overlapping nested save'
    )
  } else {
    commitPortableFile(
      source,
      'gamma.md',
      ACCEPTED_THIRD_NOTE,
      'accepted third-note save'
    )
  }

  return {
    directory,
    source,
    localTip,
    acceptedHead: runGit(['rev-parse', 'main'], source),
  }
}
