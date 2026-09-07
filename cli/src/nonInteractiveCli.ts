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
import { submitNotebookGitProposal } from './commands/notebook/notebookPublishSubmission.js'
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
const NOTEBOOK_PUBLISH_USAGE = 'usage: donut notebook publish <directory>'
const NOTEBOOK_PULL_USAGE =
  'usage: donut notebook pull <directory>\n' +
  'Receives accepted notebook history onto a clean local main. When one unpublished commit edits one existing note at an unchanged path and accepted history advanced through ordinary-note content changes at unchanged paths, pull rebases that unpublished commit. Pull does not publish. Inspect the result, then run "donut notebook publish <directory>". Accepted history may not include all current web content.'

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
    `Cloned notebook ${notebookId} into ${destination}. Open and edit the files there with any ordinary local Git tool (Obsidian, an IDE, plain git). Publishing currently accepts one new commit directly on the accepted main containing either one or more added Markdown notes with optional edits, a single edited Markdown note, one isolated equal-content Markdown note remove/add pair that may change folder and/or filename, or one isolated Markdown note deletion that leaves existing links authored; creating a new or unrepresented folder, overwriting an existing note, moving a folder or README, and relocating or renaming together with a content edit in the same commit, are not supported yet. To preserve note identity, commit and publish the unchanged relocation or rename, wait for it to be accepted, then edit and separately commit and publish the content change. Authored referring links are not rewritten by a relocation or rename, so links to the old path may no longer resolve. Do not delete and recreate the note. Use the notebook root or existing folders represented in accepted history. Run "donut notebook pull ${destination}" to receive newer accepted history; when one unpublished commit edits one existing note at an unchanged path and accepted history advanced through ordinary-note content changes at unchanged paths, pull rebases that unpublished commit. Pull does not publish. Inspect the result, then run "donut notebook publish ${destination}". Accepted history may not include all current web content.`
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

async function completeNotebookPull(notebookArgs: string[]): Promise<void> {
  const [, directory] = notebookArgs

  if (!directory) {
    exitCliError(NOTEBOOK_PULL_USAGE)
  }

  let result: Awaited<ReturnType<typeof receiveAcceptedNotebookHead>>
  try {
    const { notebookId } = resolveNotebookBinding(directory)
    assertLocalMainIsReadyToReceive(directory)
    result = await receiveAcceptedNotebookHead(directory, Number(notebookId))
  } catch (e) {
    exitCliError(exceptionText(e))
  }

  if (result.kind === 'already-based') {
    console.log(
      `Unpublished local commit is already based on the accepted history. Local head: ${result.localHead}. Accepted head: ${result.acceptedHead}. Inspect the result, then run "donut notebook publish ${directory}".`
    )
    return
  }
  if (result.kind === 'unchanged') {
    console.log(`Notebook unchanged. Accepted head: ${result.acceptedHead}`)
    return
  }
  if (result.kind === 'rebased') {
    console.log(
      `Rebased onto the accepted history. Local head: ${result.localHead}. Accepted head: ${result.acceptedHead}. Inspect the result with "git status".`
    )
    return
  }
  console.log(
    `Received accepted notebook history. Accepted head: ${result.acceptedHead}. This accepted history may not include all current web content.`
  )
}
