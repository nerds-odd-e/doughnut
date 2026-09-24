import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import {
  commitFileChangeSet,
  commitPortableFile,
  type FileChange,
} from './notebookPull.testHelpers.js'

export const LOCAL_ROOT_NOTE =
  '---\ntype: Note\nauthored: root-local\n---\n# Alpha\n\nLocal root body.\n'
export const LOCAL_NESTED_NOTE =
  '---\ntype: Note\nauthored: nested-local\n---\n# Nested\n\nLocal nested body.\n'
export const ACCEPTED_THIRD_NOTE =
  '---\ntype: Note\nauthored: remote-yaml\n---\n# Gamma\n\nAccepted third-note body.\n'

// The base repo files present on the accepted side before either the local checkout diverges
// or any further accepted commits are applied.
const DEFAULT_BASE_FILES: FileChange[] = [
  {
    path: 'Nested/Cell.md',
    content: '---\ntype: Note\n---\n# Nested\n\nOriginal nested.\n',
  },
  {
    path: 'gamma.md',
    content: '---\ntype: Note\n---\n# Gamma\n\nOriginal third.\n',
  },
]

// The one unpublished local commit's file changes for the canonical root-and-nested batch.
const CANONICAL_LOCAL_CHANGES: FileChange[] = [
  { path: 'note.md', content: LOCAL_ROOT_NOTE },
  { path: 'Nested/Cell.md', content: LOCAL_NESTED_NOTE },
]

// The default single-commit accepted interval: one disjoint third-note save.
const DEFAULT_ACCEPTED_CHANGE_SETS: FileChange[][] = [
  [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
]

export function prepareContentBatchDivergence(
  workDir: string,
  options?: {
    baseFiles?: FileChange[]
    localChanges?: FileChange[]
    acceptedChangeSets?: FileChange[][]
  }
): {
  directory: string
  source: string
  localTip: string
  acceptedHead: string
} {
  const source = buildSourceRepo(workDir)
  for (const file of options?.baseFiles ?? DEFAULT_BASE_FILES) {
    commitPortableFile(source, file.path, file.content, `add ${file.path}`)
  }
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )

  commitFileChangeSet(
    directory,
    options?.localChanges ?? CANONICAL_LOCAL_CHANGES,
    'unpublished content batch'
  )
  const localTip = runGit(['rev-parse', 'HEAD'], directory)

  for (const changeSet of options?.acceptedChangeSets ??
    DEFAULT_ACCEPTED_CHANGE_SETS) {
    commitFileChangeSet(source, changeSet, 'accepted save')
  }

  return {
    directory,
    source,
    localTip,
    acceptedHead: runGit(['rev-parse', 'main'], source),
  }
}
