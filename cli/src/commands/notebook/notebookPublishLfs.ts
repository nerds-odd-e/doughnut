import {
  checkoutUsesLfs,
  prepareAuthenticatedLfsCheckout,
  runGitLfsOrThrow,
} from './notebookLfsLocal.js'
import { selectRequiredLfsObjectIds } from './notebookPublishLfsSelection.js'
import { runSystemGitOrThrow } from './systemGit.js'

/**
 * For an LFS checkout, uploads required unpublished objects with standard Git LFS
 * before the caller submits the Git bundle. Raw checkouts are untouched. Does not
 * mutate refs or working files; a failed upload throws and leaves local state as-is.
 */
export function uploadRequiredLfsObjectsBeforeProposal(
  directory: string,
  notebookId: number,
  acceptedHead: string
): void {
  if (!checkoutUsesLfs(directory)) {
    return
  }
  const proposedHead = runSystemGitOrThrow(
    ['-C', directory, 'rev-parse', 'main'],
    (detail, status) =>
      `failed to read local main${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trim()
  const objectIds = selectRequiredLfsObjectIds(
    directory,
    acceptedHead,
    proposedHead
  )
  if (objectIds.length === 0) {
    return
  }
  const retry = 'retry "donut notebook publish"'
  const lfsUrl = prepareAuthenticatedLfsCheckout(
    directory,
    notebookId,
    'publish',
    retry
  )
  runGitLfsOrThrow(
    directory,
    'publish',
    retry,
    ['push', '--object-id', lfsUrl, ...objectIds],
    (detail, status) =>
      `failed to upload notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then ${retry}. Local refs and files were not changed.`
  )
}
