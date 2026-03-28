import { exitCliError } from './cliExit.js'

export function runInteractive(stdin = process.stdin): void {
  if (!stdin.isTTY) {
    exitCliError('not a terminal (use version or update)')
  }
}
