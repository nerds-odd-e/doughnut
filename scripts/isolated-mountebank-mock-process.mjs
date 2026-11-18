import path from 'node:path'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'

export function spawnOwnedMockChild(checkoutRoot, managementPort, spawnFn) {
  const pidfile = path.join(
    checkoutRoot,
    `.isolated-mb.${process.pid}.${managementPort}.pid`
  )

  return spawnFn(
    path.join(checkoutRoot, 'node_modules', '.bin', 'mb'),
    [
      'start',
      '--port',
      String(managementPort),
      '--pidfile',
      pidfile,
      '--nologfile',
      '--loglevel',
      'error',
    ],
    {
      cwd: checkoutRoot,
      detached: true,
      stdio: 'ignore',
      shell: false,
    }
  )
}

/**
 * Failure observation + stop/kill for one owned private-mock child.
 * Used by the real starter and by the spawned mock-failure proof runner.
 * The `serviceLabel` names the service in failure messages.
 */
export function observeOwnedMockChild(child, { serviceLabel = 'mock' } = {}) {
  const failState = { error: null }
  const onChildGone = (code, signal) => {
    if (failState.error) return
    failState.error = new Error(
      `Private ${serviceLabel} mock process exited unexpectedly ` +
        `(code ${code ?? 'unknown'}, signal ${signal ?? 'none'}).`
    )
  }
  child.once('exit', onChildGone)
  child.once('error', (error) => {
    failState.error = new Error(
      `Private ${serviceLabel} mock failed to start: ${error.message}`
    )
  })

  let stopped = false
  const disarm = () => child.removeListener('exit', onChildGone)

  return {
    getFailure() {
      return failState.error
    },
    async stop() {
      if (stopped) return
      stopped = true
      disarm()
      await stopOwnedSutProcessTree(child)
    },
    killSync() {
      if (stopped) return
      stopped = true
      disarm()
      const pgid = child.pid
      if (!Number.isInteger(pgid) || pgid <= 0) return
      try {
        process.kill(-pgid, 'SIGKILL')
      } catch (error) {
        if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
      }
    },
  }
}
