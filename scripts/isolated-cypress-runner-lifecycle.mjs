import { verifyLiveSutOwner } from './sut-owner.mjs'

const PRIMARY_CYPRESS_ORIGIN = 'http://localhost:5173'

export function refuseConflictingCypressOrigin(env, config, isolatedOrigin) {
  const envOrigin = env.CYPRESS_baseUrl
  if (envOrigin && envOrigin !== isolatedOrigin) {
    throw new Error(
      `Conflicting CYPRESS_baseUrl=${envOrigin} does not match the configured isolated Cypress origin (${isolatedOrigin}).`
    )
  }
  const configOrigin = config.baseUrl
  if (
    configOrigin &&
    configOrigin !== isolatedOrigin &&
    configOrigin !== PRIMARY_CYPRESS_ORIGIN
  ) {
    throw new Error(
      `Conflicting Cypress baseUrl=${configOrigin} does not match the configured isolated Cypress origin (${isolatedOrigin}).`
    )
  }
}

export async function requireHealthyOwningSut(checkoutRoot, healthcheckFn) {
  const live = await verifyLiveSutOwner(checkoutRoot)
  if (!live.ok) {
    throw new Error(
      'Isolated Cypress requires a verified live SUT owner in this checkout.'
    )
  }
  const runHealth =
    healthcheckFn ??
    (async (root) => {
      const { runSutHealthcheck } = await import('./sut-healthcheck.mjs')
      return runSutHealthcheck({ checkoutRoot: root })
    })
  const health = await runHealth(checkoutRoot)
  if (!health?.ok) {
    throw new Error(
      'Isolated Cypress requires a healthy owning SUT in this checkout.'
    )
  }
}

export function failLoudly(error) {
  process.stderr.write(
    `${error instanceof Error ? error.message : String(error)}\n`
  )
  process.exit(1)
}

export function registerRunnerCleanup(cleanup) {
  let released = false
  const releaseOnce = async () => {
    if (released) return
    released = true
    await cleanup()
  }
  process.once('SIGINT', () => {
    releaseOnce().catch(failLoudly)
  })
  process.once('SIGTERM', () => {
    releaseOnce().catch(failLoudly)
  })
  return releaseOnce
}

/** Race-safe: exit may land between start resolve and listener attach. */
export function observePrivateMockFailure(mock, cleanupOnce) {
  let reacting = false
  const reactToMockFailure = () => {
    if (reacting) return
    const failure = mock.getFailure?.()
    if (!failure) return
    reacting = true
    cleanupOnce()
      .catch(() => {
        /* cleanup best-effort before failLoudly */
      })
      .finally(() => failLoudly(failure))
  }
  mock.child?.once('exit', reactToMockFailure)
  if (mock.getFailure?.()) reactToMockFailure()
}
