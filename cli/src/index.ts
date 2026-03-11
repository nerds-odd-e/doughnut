import { processInput } from './interactive.js'
import { runInteractive } from './interactive.js'
import { runUpdate } from './update.js'
import { formatVersionOutput } from './version.js'
import { formatHelp } from './help.js'

export async function run(args: string[]): Promise<void> {
  const cIdx = args.findIndex((a) => a === '-c' || a.startsWith('-c='))
  if (cIdx !== -1) {
    const arg = args[cIdx]
    const value = arg.startsWith('-c=') ? arg.slice(3) : args[cIdx + 1]
    if (value === undefined) {
      console.error('doughnut: -c requires an argument')
      process.exit(1)
      return
    }
    console.log(formatVersionOutput())
    console.log()
    await processInput(value)
    process.exit(0)
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
    console.log(formatHelp())
    return
  }

  await runInteractive()
}

async function main(): Promise<void> {
  const rawArgs = process.argv.slice(2)
  const production = rawArgs.includes('-P') || rawArgs.includes('--production')
  const args = rawArgs.filter((a) => a !== '-P' && a !== '--production')

  if (production) {
    delete process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_API_BASE_URL = 'https://doughnut.odd-e.com'
  }

  await run(args)
}

main()
