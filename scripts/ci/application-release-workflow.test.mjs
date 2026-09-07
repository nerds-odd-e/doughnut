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

const nonTerminalRelease =
  "steps.release_state.outputs.state != 'already-released' && steps.release_state.outputs.state != 'superseded'"
const selected = (field) => `\${{ steps.reconciliation.outputs.${field} }}`

test('application release starts only on application tags', () => {
  const ci = workflow('ci')
  const deploy = workflow('deploy')

  assert.deepEqual(ci.on.push.branches, ['main'])
  assert.equal(ci.name, 'donut CI')
  assert.deepEqual(deploy.on, {
    push: { tags: ['v*.*.*'] },
  })
  assert.equal(deploy.name, 'Application Release')
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
  const reconciliation = admission.steps.find(
    (step) => step.id === 'reconciliation'
  )
  const releaseState = admission.steps.find(
    (step) => step.id === 'release_state'
  )
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
      admission.steps.indexOf(reconciliation)
  )
  assert.equal(reconciliation.if, undefined)
  assert.equal(
    reconciliation.run,
    'node scripts/ci/application-release-reconciliation.mjs'
  )
  assert.equal(reconciliation.env.GITHUB_TOKEN, '${{ secrets.GITHUB_TOKEN }}')
  assert.ok(
    admission.steps.indexOf(reconciliation) <
      admission.steps.indexOf(releaseState)
  )
  assert.deepEqual(releaseState.env, {
    RELEASE_TAG: selected('tag'),
    RELEASE_REF_OID: selected('refOid'),
    RELEASE_SHA: selected('sha'),
  })
  assert.equal(releaseState.if, "steps.reconciliation.outputs.tag != ''")
  assert.equal(
    releaseState.run,
    'node scripts/ci/application-release-state.mjs --check-release'
  )
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
  assert.doesNotMatch(JSON.stringify(deploy.jobs), /main-head-guard|head_guard/)
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
    admission.steps.some((step) => step.id === 'identity'),
    false
  )
  assert.equal(
    admission.steps.some((step) => step.id === 'ci'),
    false
  )
  assert.doesNotMatch(
    JSON.stringify(admission),
    /github\.event\.workflow_run\.(conclusion|head_sha|run_started_at)/
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

test('terminal release outcomes bypass CI and every publication operation', () => {
  const deploy = workflow('deploy')
  const admission = deploy.jobs['release-admission']
  const publication = deploy.jobs.Deploy
  const downloads = publication.steps.filter(
    (step) => step.uses === 'actions/download-artifact@v8'
  )

  assert.equal(
    admission.outputs.deploy,
    `\${{ ${nonTerminalRelease} && steps.reconciliation.outputs.state == 'ready' }}`
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

test('admission exposes the selected release identity using full Git history', () => {
  const admission = workflow('deploy').jobs['release-admission']
  assert.equal(admission.outputs.tag, selected('tag'))
  assert.equal(admission.outputs.sha, selected('sha'))
  assert.equal(admission.outputs.ref, selected('ref'))
  assert.equal(admission.outputs.ref_oid, selected('refOid'))
  assert.equal(admission.outputs.run_id, selected('runId'))
  assert.equal(admission.outputs.run_attempt, selected('runAttempt'))
  assert.equal(admission.steps[0].with['fetch-depth'], 0)
  assert.equal(
    admission.steps.find((step) => step.id === 'reconciliation').run,
    'node scripts/ci/application-release-reconciliation.mjs'
  )
  assert.doesNotMatch(JSON.stringify(admission), /Wait for successful CI/)
})
