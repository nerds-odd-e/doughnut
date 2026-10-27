import { restartSutApplication } from './sut-owner-control.mjs'
import { waitForSutHealthy } from './sut-start-health-wait.mjs'

/** Restart only the application unit; the caller retains HTTPS and database ownership. */
export function ownedApplicationRestart({
  ownerRef,
  checkoutRoot,
  ...readiness
}) {
  return async (leaseToken) => {
    if (!ownerRef.owner)
      throw new Error(
        'Application restart requires an owned isolated SUT lifetime.'
      )
    const result = await restartSutApplication(
      checkoutRoot,
      leaseToken,
      ownerRef.owner
    )
    const ready = await waitForSutHealthy({ ...readiness, checkoutRoot })
    if (!ready.ok)
      throw new Error('Restarted owned application did not become healthy.')
    return result
  }
}
