import { getApiConfig } from 'donut-api'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { commitPortableFile } from './notebookPull.testHelpers.js'

export function cloneWithLocalNoteAndRemoteOther(
  workDir: string,
  seed?: (source: string) => void
): {
  directory: string
  source: string
} {
  const source = buildSourceRepo(workDir)
  commitPortableFile(
    source,
    'other.md',
    '---\ntype: Note\n---\n# Other\n\nAccepted body.\n',
    'add other note'
  )
  seed?.(source)
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  commitPortableFile(
    directory,
    'note.md',
    '---\ntype: Note\n---\n# Note\n\nLocal body.\n',
    'unpublished note edit'
  )
  return { directory, source }
}
