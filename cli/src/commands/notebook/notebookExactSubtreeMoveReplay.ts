import * as fs from 'node:fs'
import * as path from 'node:path'
import {
  mapPathUnderExactSubtree,
  type ExactAcceptedSubtreeMapping,
} from './notebookAcceptedExactSubtreeMapping.js'
import { runSystemGitOrThrow } from './systemGit.js'

const REGULAR_FILE_MODE = '100644'

function replayFailure(
  detail: string | undefined,
  status: number | null
): string {
  return `failed to replay the unpublished local edit onto the accepted folder move${detail ? `: ${detail}` : ` (exit code ${status})`}`
}

/**
 * Builds one child of acceptedHead in the temporary accepted repository: the
 * accepted tree with the local blob installed at its mapped path. Retains the
 * unpublished commit's author, dates, and message. No content merge.
 */
export function buildExactSubtreeMoveReplayCommit(
  acceptedRepoDir: string,
  acceptedHead: string,
  localHead: string,
  localPath: string,
  mapping: ExactAcceptedSubtreeMapping
): string {
  const mappedPath = mapPathUnderExactSubtree(localPath, mapping)
  if (mappedPath === undefined) {
    throw new Error(
      `failed to replay the unpublished local edit onto the accepted folder move: "${localPath}" is outside the moved subtree`
    )
  }

  const blobId = runSystemGitOrThrow(
    ['-C', acceptedRepoDir, 'rev-parse', `${localHead}:${localPath}`],
    replayFailure
  ).trim()

  const identity = runSystemGitOrThrow(
    [
      '-C',
      acceptedRepoDir,
      'log',
      '-1',
      '--format=%an%n%ae%n%aI%n%cn%n%ce%n%cI%n%B',
      localHead,
    ],
    replayFailure
  )
  const [
    authorName = '',
    authorEmail = '',
    authorDate = '',
    committerName = '',
    committerEmail = '',
    committerDate = '',
    ...messageLines
  ] = identity.split('\n')
  const message = messageLines.join('\n').replace(/\n$/, '')

  const indexFile = path.join(
    acceptedRepoDir,
    `donut-exact-subtree-move-replay-${process.pid}.index`
  )
  try {
    const indexEnv = { ...process.env, GIT_INDEX_FILE: indexFile }
    runSystemGitOrThrow(
      ['-C', acceptedRepoDir, 'read-tree', acceptedHead],
      replayFailure,
      { env: indexEnv }
    )
    runSystemGitOrThrow(
      [
        '-C',
        acceptedRepoDir,
        'update-index',
        '--add',
        '--cacheinfo',
        `${REGULAR_FILE_MODE},${blobId},${mappedPath}`,
      ],
      replayFailure,
      { env: indexEnv }
    )
    const treeId = runSystemGitOrThrow(
      ['-C', acceptedRepoDir, 'write-tree'],
      replayFailure,
      { env: indexEnv }
    ).trim()

    return runSystemGitOrThrow(
      ['-C', acceptedRepoDir, 'commit-tree', treeId, '-p', acceptedHead],
      replayFailure,
      {
        env: {
          ...process.env,
          GIT_AUTHOR_NAME: authorName,
          GIT_AUTHOR_EMAIL: authorEmail,
          GIT_AUTHOR_DATE: authorDate,
          GIT_COMMITTER_NAME: committerName || authorName,
          GIT_COMMITTER_EMAIL: committerEmail || authorEmail,
          GIT_COMMITTER_DATE: committerDate || authorDate,
        },
        input: message,
      }
    ).trim()
  } finally {
    fs.rmSync(indexFile, { force: true })
  }
}
