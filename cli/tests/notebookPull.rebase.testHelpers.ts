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

const ONE_SHOT_COMMIT_IDENTITY = [
  '-c',
  'user.name=Donut E2E',
  '-c',
  'user.email=donut-e2e@example.com',
] as const

const AMBIENT_GIT_IDENTITY_AND_EDITOR_KEYS = [
  'GIT_EDITOR',
  'EDITOR',
  'VISUAL',
  'GIT_CONFIG_GLOBAL',
  'GIT_CONFIG_SYSTEM',
  'GIT_CONFIG_NOSYSTEM',
  'GIT_COMMITTER_NAME',
  'GIT_COMMITTER_EMAIL',
  'GIT_AUTHOR_NAME',
  'GIT_AUTHOR_EMAIL',
] as const

export async function withNoAmbientGitIdentityOrEditor(
  body: () => Promise<void>
): Promise<void> {
  const saved = Object.fromEntries(
    AMBIENT_GIT_IDENTITY_AND_EDITOR_KEYS.map((key) => [key, process.env[key]])
  )
  try {
    for (const key of AMBIENT_GIT_IDENTITY_AND_EDITOR_KEYS) {
      delete process.env[key]
    }
    process.env.GIT_CONFIG_GLOBAL = '/dev/null'
    process.env.GIT_CONFIG_SYSTEM = '/dev/null'
    process.env.GIT_CONFIG_NOSYSTEM = '1'
    await body()
  } finally {
    for (const key of AMBIENT_GIT_IDENTITY_AND_EDITOR_KEYS) {
      const value = saved[key]
      if (value === undefined) delete process.env[key]
      else process.env[key] = value
    }
  }
}

export function prepareEligibleDivergence(
  workDir: string,
  options: { remoteEdits: number; oneShotCommitIdentity?: boolean }
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
    'checkout',
    { configureIdentity: !options.oneShotCommitIdentity }
  )
  fs.writeFileSync(join(directory, 'note.md'), LOCAL_NOTE)
  runGit(['add', 'note.md'], directory)
  runGit(
    [
      ...(options.oneShotCommitIdentity ? ONE_SHOT_COMMIT_IDENTITY : []),
      'commit',
      '--quiet',
      '-m',
      'unpublished note edit',
    ],
    directory
  )
  if (options.oneShotCommitIdentity) {
    runGit(['config', '--local', 'user.useConfigOnly', 'true'], directory)
  }
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
