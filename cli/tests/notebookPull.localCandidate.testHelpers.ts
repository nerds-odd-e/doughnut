import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import { cloneAsBoundCheckout } from './notebookPublish.testHelpers.js'

export function prepareUnsupportedLocalHistory(
  workDir: string,
  source: string,
  shape:
    | 'unrelated'
    | 'multiple-commits'
    | 'merge'
    | 'add'
    | 'rename'
    | 'readme'
    | 'mode'
): string {
  if (shape === 'unrelated') {
    return initBoundCheckout(workDir, getApiConfig().apiBaseUrl)
  }

  if (shape === 'readme') {
    fs.writeFileSync(
      join(source, 'README.md'),
      '---\ntype: Readme\n---\n# Notebook\n'
    )
    runGit(['add', 'README.md'], source)
    runGit(['commit', '--quiet', '-m', 'add notebook readme'], source)
  }
  if (shape === 'merge') {
    fs.writeFileSync(
      join(source, 'note.md'),
      '---\ntype: Note\n---\n# Note\n\nAccepted body.\n'
    )
    runGit(['add', 'note.md'], source)
    runGit(['commit', '--quiet', '-m', 'accepted note body'], source)
  }

  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )

  switch (shape) {
    case 'multiple-commits':
      commitPortableNote(
        directory,
        'note.md',
        '---\ntype: Note\n---\n# First\n\nOne.\n',
        'first unpublished edit'
      )
      commitPortableNote(
        directory,
        'note.md',
        '---\ntype: Note\n---\n# Second\n\nTwo.\n',
        'second unpublished edit'
      )
      return directory
    case 'merge': {
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
    case 'add':
      commitPortableNote(
        directory,
        'Added.md',
        '---\ntype: Note\n---\n# Added\n\nNew note.\n',
        'add note'
      )
      return directory
    case 'rename':
      runGit(['mv', 'note.md', 'Renamed.md'], directory)
      runGit(['commit', '--quiet', '-m', 'rename note'], directory)
      return directory
    case 'readme':
      commitPortableNote(
        directory,
        'README.md',
        '---\ntype: Readme\n---\n# Notebook\n\nLocal readme edit.\n',
        'edit readme'
      )
      return directory
    case 'mode':
      fs.chmodSync(join(directory, 'note.md'), 0o755)
      runGit(['add', 'note.md'], directory)
      runGit(['commit', '--quiet', '-m', 'make note executable'], directory)
      return directory
  }
}

function commitPortableNote(
  directory: string,
  relativePath: string,
  bytes: string,
  message: string
): void {
  fs.writeFileSync(join(directory, relativePath), bytes)
  runGit(['add', relativePath], directory)
  runGit(['commit', '--quiet', '-m', message], directory)
}
