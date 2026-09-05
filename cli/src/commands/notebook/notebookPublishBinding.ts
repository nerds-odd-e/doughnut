import { spawnSync } from 'node:child_process'
import { loadAuthenticatedFetchContext } from '../../backendApi/donutBackendClient.js'

/** Reads one local-only Git config key, or `undefined` if unset or not inside a Git repo. */
function readLocalGitConfig(
  directory: string,
  key: string
): string | undefined {
  const result = spawnSync(
    'git',
    ['-C', directory, 'config', '--local', '--get', key],
    { encoding: 'utf8' }
  )
  if (result.error || result.status !== 0) return undefined
  const value = result.stdout.trim()
  return value === '' ? undefined : value
}

export interface NotebookPublishBinding {
  notebookId: string
  apiOrigin: string
}

/**
 * Reads the local-only Git config binding that `notebook clone` records
 * (`recordLocalNotebookBinding` in notebookAcquisition.ts: `donut.notebook-id` and
 * `donut.api-origin`) and confirms it matches the currently configured authenticated API
 * origin. Throws an actionable error when `directory` isn't a bound checkout, or when it's
 * bound to a different Donut server than the one currently configured.
 */
export function resolveNotebookPublishBinding(
  directory: string
): NotebookPublishBinding {
  const notebookId = readLocalGitConfig(directory, 'donut.notebook-id')
  const apiOrigin = readLocalGitConfig(directory, 'donut.api-origin')
  if (!(notebookId && apiOrigin)) {
    throw new Error(
      `${directory} is not a Donut notebook checkout (no local "notebook clone" binding found). Run "donut notebook clone <notebook-id> ${directory}" first.`
    )
  }

  const { apiBaseUrl } = loadAuthenticatedFetchContext()
  if (apiOrigin !== apiBaseUrl) {
    throw new Error(
      `${directory} was cloned from ${apiOrigin}, but the currently configured Donut server is ${apiBaseUrl}. Publish from a checkout cloned against that server, or reconfigure the CLI to use ${apiOrigin}.`
    )
  }

  return { notebookId, apiOrigin }
}
