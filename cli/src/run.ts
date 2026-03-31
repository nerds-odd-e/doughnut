import { completeNonInteractiveCliIfHandled } from './nonInteractiveCli.js'
import { runInteractive } from './interactive.js'

export async function run(args: string[]): Promise<void> {
  if (!(await completeNonInteractiveCliIfHandled(args))) {
    await runInteractive()
  }
}
