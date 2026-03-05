import { formatVersionOutput } from './version.js'
import { runInteractive } from './interactive.js'
import { runUpdate } from './update.js'

async function main(): Promise<void> {
  const args = process.argv.slice(2)
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

  await runInteractive()
}

main()
