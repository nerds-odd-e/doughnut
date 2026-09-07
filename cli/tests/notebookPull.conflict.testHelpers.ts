import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { runGit } from './notebookClone.testHelpers.js'
import {
  cloneWithLocalNoteEdit,
  commitPortableFile,
} from './notebookPull.testHelpers.js'

export const SPACED_NOTE_PATH = 'Topic Folder/My Note.md'
export const BODY_BASE = '---\ntype: Note\n---\n# Note\n\nShared sentence.\n'
export const BODY_LOCAL = '---\ntype: Note\n---\n# Note\n\nLocal sentence.\n'
export const BODY_ACCEPTED =
  '---\ntype: Note\n---\n# Note\n\nAccepted sentence.\n'
export const BODY_CHOSEN = '---\ntype: Note\n---\n# Note\n\nChosen sentence.\n'
export const NESTED_YAML_PATH = 'Nested/Cell.md'
export const YAML_BASE =
  '---\ntype: Note\nauthored: original\n---\n# Nested\n\nShared body.\n'
export const YAML_LOCAL =
  '---\ntype: Note\nauthored: local\n---\n# Nested\n\nShared body.\n'
export const YAML_ACCEPTED =
  '---\ntype: Note\nauthored: remote\n---\n# Nested\n\nShared body.\n'
export const YAML_CHOSEN =
  '---\ntype: Note\nauthored: chosen\n---\n# Nested\n\nShared body.\n'

export function prepareConflictingSameNote(
  workDir: string,
  relativePath: string,
  baseBytes: string,
  localBytes: string,
  acceptedBytes: string
) {
  const setup = cloneWithLocalNoteEdit(
    workDir,
    baseBytes,
    localBytes,
    relativePath
  )
  commitPortableFile(
    setup.source,
    relativePath,
    acceptedBytes,
    'accepted note edit'
  )
  return {
    ...setup,
    acceptedHead: runGit(['rev-parse', 'main'], setup.source),
  }
}

const TEST_OWNED_REBASE_EDITOR_ENV = {
  GIT_EDITOR: 'true',
  GIT_SEQUENCE_EDITOR: 'true',
} as const

function runTestOwnedRebase(
  directory: string,
  action: 'continue' | 'skip'
): void {
  const result = spawnSync('git', ['rebase', `--${action}`], {
    cwd: directory,
    encoding: 'utf8',
    env: { ...process.env, ...TEST_OWNED_REBASE_EDITOR_ENV },
  })
  if (result.status !== 0) {
    throw new Error(
      `git rebase --${action} failed:\n${result.stdout}\n${result.stderr}`
    )
  }
}

function stageChosenBytes(
  directory: string,
  relativePath: string,
  chosenBytes: string
): void {
  fs.writeFileSync(join(directory, relativePath), chosenBytes)
  runGit(['add', '--', relativePath], directory)
}

export function continuePausedRebaseWithChosenBytes(
  directory: string,
  relativePath: string,
  chosenBytes: string
): void {
  stageChosenBytes(directory, relativePath, chosenBytes)
  runTestOwnedRebase(directory, 'continue')
}

export function skipPausedRebaseWithChosenBytes(
  directory: string,
  relativePath: string,
  chosenBytes: string
): void {
  stageChosenBytes(directory, relativePath, chosenBytes)
  runTestOwnedRebase(directory, 'skip')
}
