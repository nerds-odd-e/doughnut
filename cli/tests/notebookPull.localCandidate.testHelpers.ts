import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import { cloneAsBoundCheckout } from './notebookPublish.testHelpers.js'

export function prepareUnsupportedLocalHistory(
  workDir: string,
  source: string,
  shape: 'unrelated' | 'merge'
): string {
  if (shape === 'unrelated') {
    return initBoundCheckout(workDir, getApiConfig().apiBaseUrl)
  }

  fs.writeFileSync(
    join(source, 'note.md'),
    '---\ntype: Note\n---\n# Note\n\nAccepted body.\n'
  )
  runGit(['add', 'note.md'], source)
  runGit(['commit', '--quiet', '-m', 'accepted note body'], source)

  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )

  const tree = runGit(['rev-parse', 'HEAD^{tree}'], directory)
  const head = runGit(['rev-parse', 'HEAD'], directory)
  const acceptedParent = runGit(['rev-parse', 'HEAD^'], directory)
  const merge = runGit(
    [
      'commit-tree',
      tree,
      '-p',
      head,
      '-p',
      acceptedParent,
      '-m',
      'unpublished merge',
    ],
    directory
  )
  runGit(['reset', '--quiet', '--hard', merge], directory)
  return directory
}
