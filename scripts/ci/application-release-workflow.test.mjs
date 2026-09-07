import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { parse } from 'yaml'

const workflow = (name) =>
  parse(
    readFileSync(
      new URL(`../../.github/workflows/${name}.yml`, import.meta.url),
      'utf8'
    )
  )

test('main CI remains enabled and only application tag pushes trigger publication', () => {
  const ci = workflow('ci')
  const deploy = workflow('deploy')

  assert.deepEqual(ci.on.push.branches, ['main'])
  assert.equal(ci.name, 'donut CI')
  assert.deepEqual(deploy.on, { push: { tags: ['v*.*.*'] } })
  assert.equal(deploy.jobs['release-admission'].if, undefined)
  assert.equal(deploy.jobs.Deploy.needs, 'release-admission')
  assert.equal(
    deploy.jobs.Deploy.if,
    "needs.release-admission.outputs.deploy == 'true'"
  )
})

test('release pins orchestration separately from source and preserves deployment credentials', () => {
  const deploy = workflow('deploy')
  const admission = deploy.jobs['release-admission']
  const publication = deploy.jobs.Deploy
  const notify = deploy.jobs['Notify-on-failure']
  const control = admission.steps.find((step) => step.id === 'control')
  const trackingAuth = admission.steps.find(
    (step) => step.uses === './.github/gcloud_auth_n_sdk'
  )
  const trackingInitialization = admission.steps.find(
    (step) => step.run === 'node scripts/ci/application-release-state.mjs'
  )
  const identity = admission.steps.find((step) => step.id === 'identity')
  const releaseState = admission.steps.find(
    (step) => step.id === 'release_state'
  )
  const ciAdmission = admission.steps.find((step) => step.id === 'ci')
  const artifactDownloads = publication.steps.filter(
    (step) => step.uses === 'actions/download-artifact@v8'
  )
  assert.equal(admission.steps[0].with.ref, 'main')
  assert.equal(admission.steps[1], control)
  assert.equal(
    control.run,
    'echo "sha=$(git rev-parse HEAD)" >> "$GITHUB_OUTPUT"'
  )
  assert.equal(
    admission.outputs.control_sha,
    '${{ steps.control.outputs.sha }}'
  )
  assert.equal(trackingAuth.with.credentials_json, '${{ env.GCP_CREDENTIALS }}')
  assert.equal(
    trackingAuth.with.skip_install,
    "${{ runner.os == 'Linux' && vars.GCP_FORCE_GCLOUD_INSTALL != 'true' }}"
  )
  assert.equal(
    trackingInitialization.env.GITHUB_TOKEN,
    '${{ secrets.GITHUB_TOKEN }}'
  )
  assert.ok(
    admission.steps.indexOf(trackingAuth) <
      admission.steps.indexOf(trackingInitialization)
  )
  assert.ok(
    admission.steps.indexOf(trackingInitialization) <
      admission.steps.indexOf(identity)
  )
  assert.ok(
    admission.steps.indexOf(identity) < admission.steps.indexOf(ciAdmission)
  )
  assert.ok(
    admission.steps.indexOf(identity) < admission.steps.indexOf(releaseState)
  )
  assert.ok(
    admission.steps.indexOf(releaseState) < admission.steps.indexOf(ciAdmission)
  )
  assert.deepEqual(releaseState.env, {
    RELEASE_TAG: '${{ steps.identity.outputs.tag }}',
    RELEASE_REF_OID: '${{ steps.identity.outputs.refOid }}',
    RELEASE_SHA: '${{ steps.identity.outputs.sha }}',
  })
  assert.equal(
    releaseState.run,
    'node scripts/ci/application-release-state.mjs --check-release'
  )
  assert.equal(
    ciAdmission.if,
    "steps.release_state.outputs.state != 'already-released'"
  )
  assert.equal(ciAdmission.run, 'node scripts/ci/application-release-ci.mjs')
  assert.deepEqual(
    publication.steps.slice(0, 2).map((step) => step.with),
    [
      {
        ref: '${{ needs.release-admission.outputs.control_sha }}',
        'fetch-depth': 1,
      },
      {
        ref: '${{ needs.release-admission.outputs.sha }}',
        path: 'release-source',
        'fetch-depth': 1,
      },
    ]
  )
  assert.equal(
    notify.steps[0].with.ref,
    "${{ needs.release-admission.outputs.control_sha || 'main' }}"
  )
  assert.equal(
    publication.env.RELEASE_SHA,
    '${{ needs.release-admission.outputs.sha }}'
  )
  assert.equal(
    publication.env.RELEASE_SOURCE_ROOT,
    '${{ github.workspace }}/release-source'
  )
  assert.equal(
    publication.env.RELEASE_REF,
    '${{ needs.release-admission.outputs.ref }}'
  )
  assert.equal(
    publication.env.RELEASE_REF_OID,
    '${{ needs.release-admission.outputs.ref_oid }}'
  )
  assert.equal(
    publication.steps.find((step) => step.id === 'publish').run,
    'GITHUB_SHA="$RELEASE_SHA" bash infra/gcp/scripts/publish-application.sh'
  )
  assert.equal(deploy.env.GITHUB_SHA, undefined)
  assert.equal(publication.env.GITHUB_SHA, undefined)
  assert.doesNotMatch(
    JSON.stringify(deploy.jobs),
    /workflow_run|main-head-guard|head_guard/
  )
  assert.deepEqual(deploy.concurrency, {
    group: 'deploy-production',
    'cancel-in-progress': false,
  })
  assert.ok(
    Object.values(deploy.jobs).every((job) => job.concurrency === undefined)
  )
  assert.equal(admission['timeout-minutes'], 70)
  assert.equal(publication['timeout-minutes'], 60)
  assert.deepEqual(deploy.permissions, { actions: 'read', contents: 'read' })
  assert.equal(
    admission.steps.find((step) => step.id === 'ci').env.GITHUB_TOKEN,
    '${{ secrets.GITHUB_TOKEN }}'
  )
  assert.ok(
    artifactDownloads.every(
      (step) => step.with['github-token'] === '${{ secrets.GITHUB_TOKEN }}'
    )
  )
  assert.deepEqual(
    artifactDownloads.map((step) => step.id),
    ['backend_artifact', 'frontend_artifact', 'cli_artifact']
  )
  assert.equal(deploy.env.GCP_CREDENTIALS, '${{ secrets.GCP_CREDENTIALS }}')
  assert.equal(
    publication.steps.find(
      (step) => step.uses === './.github/gcloud_auth_n_sdk'
    ).with.credentials_json,
    '${{ env.GCP_CREDENTIALS }}'
  )
})

test('an already-released outcome bypasses CI and every publication operation', () => {
  const deploy = workflow('deploy')
  const admission = deploy.jobs['release-admission']
  const publication = deploy.jobs.Deploy
  const downloads = publication.steps.filter(
    (step) => step.uses === 'actions/download-artifact@v8'
  )

  assert.equal(
    admission.outputs.deploy,
    "${{ steps.release_state.outputs.state != 'already-released' && steps.ci.outputs.state == 'ready' }}"
  )
  assert.equal(
    publication.if,
    "needs.release-admission.outputs.deploy == 'true'"
  )
  assert.deepEqual(
    downloads.map((step) => step.id),
    ['backend_artifact', 'frontend_artifact', 'cli_artifact']
  )
  assert.ok(publication.steps.some((step) => step.id === 'publish'))
})

test('independent CLI tags retain their release trigger', () => {
  assert.deepEqual(workflow('cli-release').on.push.tags, ['cli-*'])
})

test('admission exposes the event identity using full Git history', () => {
  const admission = workflow('deploy').jobs['release-admission']
  assert.equal(admission.outputs.sha, '${{ steps.identity.outputs.sha }}')
  assert.equal(
    admission.outputs.ref_oid,
    '${{ steps.identity.outputs.refOid }}'
  )
  assert.equal(admission.steps[0].with['fetch-depth'], 0)
  assert.match(
    admission.steps.find((step) => step.id === 'identity').run,
    /node scripts\/ci\/application-release.mjs/
  )
})
