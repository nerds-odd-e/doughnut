import { runInteractive } from './interactive.js'
import { runUpdate } from './update.js'
import { formatVersionOutput } from './version.js'

export async function run(args: string[]): Promise<void> {
  if (args.some((a) => a === '-c' || a.startsWith('-c='))) {
    console.error(
      'doughnut: -c is not supported. Run `doughnut` in a terminal for the interactive shell.'
    )
    process.exit(1)
    return
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
    console.error(
      'doughnut: there is no help subcommand. Run `doughnut` in a terminal, then type /help.'
    )
    process.exit(1)
    return
  }

  await runInteractive()
}
