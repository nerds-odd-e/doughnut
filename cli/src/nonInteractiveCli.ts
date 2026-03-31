import { exitCliError } from './cliExit.js'
import { runUpdate } from './commands/update.js'
import { formatVersionOutput } from './commands/version.js'

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

  if (subcommand === 'help') {
    exitCliError('not a terminal (use version or update)')
  }

  return false
}
