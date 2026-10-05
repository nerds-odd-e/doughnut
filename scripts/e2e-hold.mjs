import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { startOwnedSutLifetime } from './sut-start.mjs'
import { worktreeIsolationApplies } from './browser-worktree-isolation.mjs'
import { NO_CANCEL } from './e2e-invocation-signals.mjs'
import { resolveInvocationCheckout } from './e2e-invocation-selection.mjs'
import { runOwnedE2eInvocation } from './e2e-owned-invocation.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

function holdUntilCancelled({ cancel, childExit, log, errLog }) {
  log('E2E stack is ready and held; interrupt (Ctrl-C) to stop it.')
  // The SUT supervisor is detached, so nothing else keeps this process alive.
  const keepAlive = setInterval(() => undefined, 1 << 30)
  return new Promise((resolve) => {
    cancel.onTriggered(() => resolve(0))
    childExit.exited.then(() => {
      if (cancel.isTriggered()) return
      errLog(
        'Required SUT service exited during the hold; ending with failure.'
      )
      resolve(1)
    })
  }).finally(() => clearInterval(keepAlive))
}

/**
 * Hold this checkout's owned E2E stack for manual, CLI, or HTTP observation:
 * the same owned invocation as `runE2eInteractive`, with a wait for
 * cancellation in place of Cypress. A clean interrupt is the normal end and
 * returns 0; a required service exit or a failed cleanup returns nonzero.
 * The services make paid OpenAI calls only when `paidOpenAi` is set.
 *
 * @returns {Promise<number>}
 */
export async function runE2eHold({
  checkoutRoot = repoRoot,
  startLifetime = startOwnedSutLifetime,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
  cancel = NO_CANCEL,
  isIsolatedCheckoutFn = worktreeIsolationApplies,
  paidOpenAi = false,
  ...lifetimeOpts
} = {}) {
  const { isolated, resolvedCheckoutTarget } = resolveInvocationCheckout({
    checkoutRoot,
    runtimeTarget: lifetimeOpts.runtimeTarget,
    isIsolatedCheckoutFn,
  })
  return runOwnedE2eInvocation({
    specs: [],
    approved: null,
    checkoutRoot,
    startLifetime,
    log,
    errLog,
    cancel,
    label: 'E2E hold',
    session: holdUntilCancelled,
    backendReload: true,
    paidOpenAi,
    isolated,
    resolvedCheckoutTarget,
    ...lifetimeOpts,
  })
}
