import { ciRun } from './application-release-ci-fixtures.mjs'
import { makeReleaseRepository } from './application-release-fixtures.mjs'
import { runPayloadAdmission } from './application-release-payload-fixtures.mjs'
import { runReconciliationCommand as reconcile } from './application-release-reconciliation-fixtures.mjs'

export async function makeSelectedReleaseWithUnavailableArtifacts(
  t,
  annotated = true
) {
  const fixture = makeReleaseRepository(t)
  fixture.tag('v1.2.3', annotated)
  const release = fixture.release()
  fixture.clone()
  const ready = await reconcile(t, fixture, release.ref, {
    [release.sha]: [ciRun({ head_sha: release.sha, run_attempt: 3 })],
  })

  const publicationTrace = `${fixture.repository}/production-writes`
  const missingArtifacts = `${fixture.repository}/release-artifacts`
  const unavailable = runPayloadAdmission({
    backend: `${missingArtifacts}/backend/donut.jar`,
    frontend: `${missingArtifacts}/frontend`,
    cli: `${missingArtifacts}/cli/donut-cli.bundle.mjs`,
    publicationTrace,
  })

  return { fixture, release, ready, unavailable, publicationTrace }
}
