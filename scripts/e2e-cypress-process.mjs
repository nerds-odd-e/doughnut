import { spawn } from 'node:child_process'

export function defaultSpawnCypress({
  specs,
  cwd,
  env,
  cypressBin,
  configFile,
  stdio,
  browser,
  spawnFn = spawn,
}) {
  const args = ['run', '--config-file', configFile, '--spec', specs.join(',')]
  if (browser) {
    args.push('--browser', browser)
  }
  return spawnFn(process.execPath, [cypressBin, ...args], {
    cwd,
    env,
    stdio,
  })
}

/**
 * Default Cypress `open` (interactive mode) spawner. Invokes the Cypress
 * executable directly (not via a recursive pnpm call) so the wrapper remains
 * the single service owner.
 *
 * `cypress open` does NOT accept `--spec` (that flag is only valid for
 * `cypress run`); spec selection in interactive mode happens in the Cypress
 * UI. An optional preselected `--spec` is still used by the wrapper upstream
 * (via `assertSupportedIsolatedCypressSpecs`) to arrange required session
 * resources — e.g. owning the private OpenAI mock for the session — but it is
 * NOT forwarded to the `cypress open` CLI. The `specs` option is accepted
 * here only so the caller can pass it through unchanged; it is intentionally
 * ignored by the spawn args.
 */
export function defaultSpawnCypressOpen({
  cwd,
  env,
  cypressBin,
  configFile,
  stdio,
  spawnFn = spawn,
}) {
  const args = ['open', '--e2e', '--config-file', configFile]
  return spawnFn(process.execPath, [cypressBin, ...args], {
    cwd,
    env,
    stdio,
  })
}

export function runCypressOnce({
  specs,
  spawnCypress,
  cypressBin,
  cypressConfigFile,
  checkoutRoot,
  env,
  stdio,
  cancel,
  childExit,
  mockExit = null,
  errLog = (s) => process.stderr.write(`${s}\n`),
  cancelEscalationMs = 5_000,
  browser,
}) {
  return new Promise((resolve, reject) => {
    let child
    try {
      child = spawnCypress({
        specs,
        cypressBin,
        configFile: cypressConfigFile,
        cwd: checkoutRoot,
        env,
        stdio,
        browser,
      })
    } catch (error) {
      reject(error)
      return
    }
    if (!child || typeof child.on !== 'function') {
      reject(new Error('spawnCypress did not return a child process.'))
      return
    }

    let settled = false
    let childExited = false
    let escalationTimer = null
    const clearEscalation = () => {
      if (escalationTimer) {
        clearTimeout(escalationTimer)
        escalationTimer = null
      }
    }
    const settle = (value) => {
      if (settled) return
      settled = true
      clearEscalation()
      resolve(value)
    }

    // On cancellation, stop the runner (the Cypress child) first; the owned
    // descendants are settled by `lifetime.shutdown()` in the caller's
    // cleanup. `cypress open` (Electron) does NOT exit on SIGTERM — it prints
    // a graceful-exit message and keeps running — so after signalling SIGTERM,
    // escalate to SIGKILL within a bounded wait if the child has not exited.
    // This unblocks the child-exit await so `lifetime.shutdown()` can run and
    // clean up the owned tree. The normal (non-cancelled) close path is
    // unaffected: no escalation timer is armed unless cancellation fires.
    cancel.onTriggered(() => {
      try {
        child.kill('SIGTERM')
      } catch {
        // already gone
      }
      if (!childExited) {
        escalationTimer = setTimeout(() => {
          if (childExited) return
          try {
            child.kill('SIGKILL')
          } catch {
            // already gone
          }
        }, cancelEscalationMs)
      }
    })

    // A required owned service (the SUT supervisor child, or a private
    // mock) exiting during the test run ends the batch with visible
    // failure. Terminate Cypress promptly — do not wait for it to finish on
    // its own — then let the caller's `lifetime.shutdown()` settle the
    // remaining owned tree. Covers the timing window: if the child already
    // exited between readiness and this attachment, end the run now.
    const endRunOnRequiredExit = (exit, message) => {
      if (!exit) return
      const onExit = () => {
        if (settled) return
        errLog(message)
        try {
          child.kill('SIGTERM')
        } catch {
          // already gone
        }
        settle(1)
      }
      if (exit.hasExited()) {
        onExit()
      } else {
        exit.exited.then(onExit)
      }
    }
    endRunOnRequiredExit(
      childExit,
      'Required SUT service exited during the test run; ending the batch with failure.'
    )
    endRunOnRequiredExit(
      mockExit,
      'Required private mock exited during the test run; ending the batch with failure.'
    )

    child.once('error', (error) => {
      if (settled) return
      clearEscalation()
      reject(error)
    })
    child.once('exit', (code, signal) => {
      childExited = true
      clearEscalation()
      if (signal) settle(1)
      else settle(code ?? 1)
    })
  })
}
