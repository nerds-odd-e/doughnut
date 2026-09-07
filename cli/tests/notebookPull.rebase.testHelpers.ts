import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'

export const LOCAL_NOTE =
  '---\ntype: Note\nauthored: local-yaml\n---\n# Local edit\n\nUnpublished body.\n'
export const OTHER_NOTE =
  '---\ntype: Note\nauthored: remote-yaml\n---\n# Other\n\nAccepted edit.\n'
export const LATER_OTHER_NOTE =
  '---\ntype: Note\nauthored: remote-yaml\n---\n# Other\n\nLater accepted edit.\n'
export const NESTED_NOTE =
  '---\ntype: Note\nauthored: nested-yaml\n---\n# Nested\n\nLocal nested body.\n'

export function prepareEligibleDivergence(
  workDir: string,
  options: { remoteEdits: number }
): {
  directory: string
  source: string
  localTip: string
  acceptedHead: string
} {
  const source = buildSourceRepo(workDir)
  fs.writeFileSync(
    join(source, 'other.md'),
    '---\ntype: Note\n---\n# Other\n\nAccepted body.\n'
  )
  runGit(['add', 'other.md'], source)
  runGit(['commit', '--quiet', '-m', 'add other note'], source)
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  fs.writeFileSync(join(directory, 'note.md'), LOCAL_NOTE)
  runGit(['add', 'note.md'], directory)
  runGit(['commit', '--quiet', '-m', 'unpublished note edit'], directory)
  const localTip = runGit(['rev-parse', 'HEAD'], directory)
  for (let i = 1; i <= options.remoteEdits; i += 1) {
    fs.writeFileSync(
      join(source, 'other.md'),
      i === options.remoteEdits
        ? OTHER_NOTE
        : `---\ntype: Note\n---\n# Other\n\nAccepted edit ${i}.\n`
    )
    runGit(['add', 'other.md'], source)
    runGit(['commit', '--quiet', '-m', `accepted other-note edit ${i}`], source)
  }
  return {
    directory,
    source,
    localTip,
    acceptedHead: runGit(['rev-parse', 'main'], source),
  }
}
