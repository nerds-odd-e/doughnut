import { exitCliError } from './cliExit.js'
import { exceptionText } from './exceptionText.js'
import { runUpdate } from './commands/update.js'
import { formatVersionOutput } from './commands/version.js'
import { acquireNotebookGitCheckout } from './commands/notebook/notebookAcquisition.js'
import { resolveNotebookPublishBinding } from './commands/notebook/notebookPublishBinding.js'
import { assertLocalMainIsCleanAndCommitted } from './commands/notebook/notebookPublishReadiness.js'
import { assertLocalMainFollowsAcceptedHistory } from './commands/notebook/notebookPublishAncestry.js'
import { submitNotebookGitProposal } from './commands/notebook/notebookPublishSubmission.js'

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
const NOTEBOOK_PUBLISH_USAGE = 'usage: donut notebook publish <directory>'

async function completeNotebookSubcommand(
  notebookArgs: string[]
): Promise<void> {
  const [action] = notebookArgs

  if (action === 'publish') {
    await completeNotebookPublish(notebookArgs)
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
    `Cloned notebook ${notebookId} into ${destination}. Open and edit the files there with any ordinary local Git tool (Obsidian, an IDE, plain git). Publishing currently accepts one new commit directly on the accepted main that changes one existing Markdown note.`
  )
}

async function completeNotebookPublish(notebookArgs: string[]): Promise<void> {
  const [, directory] = notebookArgs

  if (!directory) {
    exitCliError(NOTEBOOK_PUBLISH_USAGE)
  }

  let acceptedHead: string
  try {
    const { notebookId } = resolveNotebookPublishBinding(directory)
    assertLocalMainIsCleanAndCommitted(directory)
    const expectedHead = await assertLocalMainFollowsAcceptedHistory(
      directory,
      Number(notebookId)
    )
    acceptedHead = await submitNotebookGitProposal(
      directory,
      Number(notebookId),
      expectedHead
    )
  } catch (e) {
    exitCliError(exceptionText(e))
  }

  console.log(`Published notebook. Accepted head: ${acceptedHead}`)
}
