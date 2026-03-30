import type { Readable } from 'node:stream'
import React from 'react'
import { render } from 'ink'
import { exitCliError } from './cliExit.js'
import { InteractiveCliApp } from './InteractiveCliApp.js'
import { formatInteractiveWelcomeBanner } from './welcomeBanner.js'

export async function runInteractive(
  stdin: Readable & { isTTY?: boolean } = process.stdin,
  stdout: NodeJS.WriteStream = process.stdout
): Promise<void> {
  if (!stdin.isTTY) {
    exitCliError('not a terminal (use version or update)')
  }
  stdout.write(formatInteractiveWelcomeBanner())
  const { waitUntilExit } = render(React.createElement(InteractiveCliApp), {
    stdin: stdin as NodeJS.ReadStream,
    stdout,
    exitOnCtrlC: true,
    patchConsole: false,
  })
  await waitUntilExit()
}
