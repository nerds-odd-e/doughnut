import { existsSync } from 'node:fs'
import { dirname } from 'node:path'
import { startManagedTtySession, type ManagedTtySession } from 'tty-assert'
import {
  CLI_E2E_MANAGED_PTY_GEOMETRY,
  NON_INTERACTIVE_CLI_EXIT_TIMEOUT_MS,
  waitForPtyExit,
  type CliE2eManagedPty,
} from './cliE2eManagedPty'
import { cliEnv } from './cliEnv'

export type RunInstalledCliTask = {
  donutPath: string
  args?: string[]
  env?: NodeJS.ProcessEnv
}

export async function runInstalledCliExpectingExit(
  pty: CliE2eManagedPty,
  { donutPath, args, env }: RunInstalledCliTask,
  expectedExitCode: 0 | 1
) {
  if (!donutPath) {
    throw new Error(
      `runInstalledCli: donutPath required, got ${JSON.stringify(donutPath)}`
    )
  }
  if (!existsSync(donutPath)) {
    throw new Error(
      `runInstalledCli: donut binary not found at ${donutPath}. Ensure prior step "I install the CLI" succeeded.`
    )
  }
  const cwd = dirname(donutPath)
  pty.dispose()
  let managed: ManagedTtySession | undefined
  try {
    managed = await startManagedTtySession(
      {
        command: process.execPath,
        args: [donutPath, ...(args ?? [])],
        cwd,
        env: { ...process.env, ...cliEnv(env) },
      },
      CLI_E2E_MANAGED_PTY_GEOMETRY
    )
    await waitForPtyExit(
      managed.session.pty,
      expectedExitCode,
      NON_INTERACTIVE_CLI_EXIT_TIMEOUT_MS
    )
    pty.setHandle(managed)
  } catch (e) {
    managed?.dispose()
    throw e
  }
  return null
}
