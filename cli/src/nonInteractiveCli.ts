import { exitCliError } from './cliExit.js'
import { exceptionText } from './exceptionText.js'
import { runUpdate } from './commands/update.js'
import { formatVersionOutput } from './commands/version.js'
import { acquireNotebookGitCheckout } from './commands/notebook/notebookAcquisition.js'
import { resolveNotebookBinding } from './commands/notebook/notebookBinding.js'
import {
  assertLocalMainIsReadyToPublish,
  assertLocalMainIsReadyToReceive,
} from './commands/notebook/notebookCheckoutReadiness.js'
import { assertLocalMainFollowsAcceptedHistory } from './commands/notebook/notebookPublishAncestry.js'
import { uploadRequiredLfsObjectsBeforeProposal } from './commands/notebook/notebookPublishLfs.js'
import { submitNotebookGitProposal } from './commands/notebook/notebookPublishSubmission.js'
import { fillInCurrentLfsFiles } from './commands/notebook/notebookLfsFillIn.js'
import { receiveAcceptedNotebookHead } from './commands/notebook/notebookPull.js'

/**
 * Handles one-shot CLI paths (version, update, help, invalid flags). Returns `false` when the
 * process should continue into the interactive TUI.
 */
export async function completeNonInteractiveCliIfHandled(
  args: string[]
): Promise<boolean> {
  if (args.some((a) => a === '-c' || a.startsWith('-c='))) {
    exitCliError('invalid option')
  }

  const hasVersionFlag = args.includes('--version') || args.includes('-v')
  const subcommand = args.find((a) => !a.startsWith('-'))

  if (hasVersionFlag || subcommand === 'version') {
    console.log(formatVersionOutput())
    return true
  }

  if (subcommand === 'update') {
    await runUpdate()
    return true
  }

  if (subcommand === 'notebook') {
    await completeNotebookSubcommand(
      args.filter((a) => !a.startsWith('-')).slice(1)
    )
    return true
  }

  if (subcommand === 'help') {
    exitCliError('not a terminal (use version or update)')
  }

  return false
}

const NOTEBOOK_CLONE_USAGE =
  'usage: donut notebook clone <notebook-id> <destination>'
const NOTEBOOK_PUBLISH_USAGE =
  'usage: donut notebook publish <directory>\n' +
  'Publishes the unpublished single-parent commits based on the accepted main.'
const NOTEBOOK_PULL_USAGE =
  'usage: donut notebook pull <directory>\n' +
  'Receives accepted notebook history onto a clean local main and fills in current attachment files. ' +
  'Pull rebases your linear unpublished commits onto accepted history, and Git pauses only on a real conflict so you can edit, stage, and run git rebase --continue, or git rebase --abort. ' +
  'Pull does not publish. Inspect the result, then run "donut notebook publish <directory>" if unpublished work remains. ' +
  'Accepted history may not include all current web content.'

async function completeNotebookSubcommand(
  notebookArgs: string[]
): Promise<void> {
  const [action] = notebookArgs

  if (action === 'publish') {
    await completeNotebookPublish(notebookArgs)
    return
  }

  if (action === 'pull') {
    await completeNotebookPull(notebookArgs)
    return
  }

  await completeNotebookClone(notebookArgs)
}

async function completeNotebookClone(notebookArgs: string[]): Promise<void> {
  const [action, notebookIdArg, destination] = notebookArgs

  if (action !== 'clone') {
    exitCliError(NOTEBOOK_CLONE_USAGE)
  }

  const notebookId = Number(notebookIdArg)
  if (!(notebookIdArg && Number.isInteger(notebookId) && destination)) {
    exitCliError(NOTEBOOK_CLONE_USAGE)
  }

  try {
    await acquireNotebookGitCheckout(notebookId, destination)
  } catch (e) {
    exitCliError(exceptionText(e))
  }
  console.log(
    [
      `Cloned notebook ${notebookId} into ${destination}.`,
      'Edit and commit there with any Git tool.',
      `Publish with "donut notebook publish ${destination}".`,
      `Receive newer changes with "donut notebook pull ${destination}".`,
    ].join('\n')
  )
}

async function completeNotebookPublish(notebookArgs: string[]): Promise<void> {
  const [, directory] = notebookArgs

  if (!directory) {
    exitCliError(NOTEBOOK_PUBLISH_USAGE)
  }

  let acceptedHead: string
  try {
    const { notebookId } = resolveNotebookBinding(directory)
    assertLocalMainIsReadyToPublish(directory)
    const expectedHead = await assertLocalMainFollowsAcceptedHistory(
      directory,
      Number(notebookId)
    )
    uploadRequiredLfsObjectsBeforeProposal(
      directory,
      Number(notebookId),
      expectedHead
    )
    acceptedHead = await submitNotebookGitProposal(
      directory,
      Number(notebookId),
      expectedHead
    )
  } catch (e) {
    exitCliError(exceptionText(e))
  }

  console.log(`Published notebook. Accepted head: ${acceptedHead}.`)
}

async function completeNotebookPull(notebookArgs: string[]): Promise<void> {
  const [, directory] = notebookArgs

  if (!directory) {
    exitCliError(NOTEBOOK_PULL_USAGE)
  }

  let result:
    | Awaited<ReturnType<typeof receiveAcceptedNotebookHead>>
    | undefined
  try {
    const { notebookId } = resolveNotebookBinding(directory)
    assertLocalMainIsReadyToReceive(directory)
    result = await receiveAcceptedNotebookHead(directory, Number(notebookId))
    const rerunPull = 'rerun "donut notebook pull"'
    // Pull refuses during an active rebase, so a paused pull is rerun only after the rebase ends.
    fillInCurrentLfsFiles(
      directory,
      Number(notebookId),
      result.kind === 'paused'
        ? `finish ("git rebase --continue") or abort ("git rebase --abort") the rebase, then ${rerunPull}`
        : rerunPull
    )
  } catch (e) {
    // A paused conflict stays visible even when filling in attachments fails afterwards.
    const conflictGuidance =
      result?.kind === 'paused' ? [result.conflictGuidance] : []
    exitCliError([...conflictGuidance, exceptionText(e)].join('\n'))
  }

  if (result.kind === 'paused') {
    exitCliError(result.conflictGuidance)
  }

  if (result.kind === 'already-based') {
    console.log(
      `Unpublished local work is already based on the accepted history. Local head: ${result.localHead}. Accepted head: ${result.acceptedHead}. Inspect the result, then run "donut notebook publish ${directory}".`
    )
    return
  }
  if (result.kind === 'unchanged') {
    console.log(`Notebook unchanged. Accepted head: ${result.acceptedHead}`)
    return
  }
  if (result.kind === 'absorbed') {
    console.log(
      `Rebased onto the accepted history. No unpublished change remains. Accepted head: ${result.acceptedHead}.`
    )
    return
  }
  if (result.kind === 'rebased') {
    console.log(
      `Rebased onto the accepted history. Local head: ${result.localHead}. Accepted head: ${result.acceptedHead}. Inspect the result, then run "donut notebook publish ${directory}".`
    )
    return
  }
  console.log(
    `Received accepted notebook history. Accepted head: ${result.acceptedHead}. This accepted history may not include all current web content.`
  )
}
