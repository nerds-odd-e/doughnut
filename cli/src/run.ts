import { exitCliError } from './cliExit.js'
import { runInteractive } from './interactive.js'
import { runUpdate } from './update.js'
import { formatVersionOutput } from './version.js'

export async function run(args: string[]): Promise<void> {
  if (args.some((a) => a === '-c' || a.startsWith('-c='))) {
    exitCliError('invalid option')
  }

  const hasVersionFlag = args.includes('--version') || args.includes('-v')
  const subcommand = args.find((a) => !a.startsWith('-'))

  if (hasVersionFlag || subcommand === 'version') {
    console.log(formatVersionOutput())
    return
  }

  if (subcommand === 'update') {
    await runUpdate()
    return
  }

  if (subcommand === 'help') {
    exitCliError('use /help in the shell')
  }

  runInteractive()
}
