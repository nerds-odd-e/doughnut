import { spawn } from 'node:child_process'

export function childExitReason(code, signal) {
  return signal ? `signal ${signal}` : `code ${code ?? 'unknown'}`
}

/**
 * Spawn a detached service group, pipe stdout/stderr to a log writer, and
 * forward SIGINT/SIGTERM to the child. Adapter-specific cleanup runs via
 * onBeforeExit before the log is closed and the process exits.
 */
export function runSupervisedServiceGroup({
  spawnFn = spawn,
  command = 'pnpm',
  serviceArgs,
  cwd,
  env,
  logWriter,
  onBeforeExit,
  exitMessage,
  startFailureMessage,
  cleanupFailurePrefix = 'supervisor cleanup failed',
} = {}) {
  const child = spawnFn(command, serviceArgs, {
    cwd,
    stdio: ['ignore', 'pipe', 'pipe'],
    env,
    shell: false,
    detached: true,
  })

  child.stdout?.on('data', (chunk) => logWriter.write(chunk))
  child.stderr?.on('data', (chunk) => logWriter.write(chunk))

  const forwardSignal = (signal) => {
    if (!child.killed) child.kill(signal)
  }

  process.once('SIGINT', forwardSignal)
  process.once('SIGTERM', forwardSignal)

  let finished = false
  const finish = async (code, signal, message) => {
    if (finished) return
    finished = true
    logWriter.write(message)
    await onBeforeExit?.(child, { code, signal })
    logWriter.close()
    process.exit(signal ? 1 : (code ?? 1))
  }

  const reportFinishFailure = (error) => {
    logWriter.write(
      `${cleanupFailurePrefix}: ${error instanceof Error ? error.message : String(error)}\n`
    )
    logWriter.close()
    process.exit(1)
  }

  child.on('error', (error) => {
    finish(1, null, startFailureMessage(error)).catch(reportFinishFailure)
  })

  child.on('close', (code, signal) => {
    finish(code, signal, exitMessage(code, signal)).catch(reportFinishFailure)
  })

  return child
}
