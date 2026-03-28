import type { Readable } from 'node:stream'
import { exitCliError } from './cliExit.js'
import { formatVersionOutput } from './commands/version.js'

export async function runInteractive(
  stdin: Readable & { isTTY?: boolean } = process.stdin
): Promise<void> {
  if (!stdin.isTTY) {
    exitCliError('not a terminal (use version or update)')
  }
  console.log(formatVersionOutput())
  if (stdin.readableEnded) {
    return
  }
  await new Promise<void>((resolve, reject) => {
    const done = () => resolve()
    stdin.once('end', done)
    stdin.once('close', done)
    stdin.once('error', reject)
    stdin.resume()
  })
}
