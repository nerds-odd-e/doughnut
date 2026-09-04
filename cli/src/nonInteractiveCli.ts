import { exitCliError } from './cliExit.js'
import { exceptionText } from './exceptionText.js'
import { runUpdate } from './commands/update.js'
import { formatVersionOutput } from './commands/version.js'
import { acquireNotebookGitCheckout } from './commands/notebook/notebookAcquisition.js'

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

async function completeNotebookSubcommand(
  notebookArgs: string[]
): Promise<void> {
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
    `Cloned notebook ${notebookId} into ${destination}. Open and edit the files there with any ordinary local Git tool (Obsidian, an IDE, plain git); publishing is not available yet — commits stay local.`
  )
}
