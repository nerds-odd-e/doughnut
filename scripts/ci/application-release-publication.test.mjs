import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'
import {
  hash,
  makePublication,
  readApplicationRecords,
} from './application-release-publication-fixtures.mjs'

const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))

for (const scenario of [
  'skip',
  'forced',
  'moved',
  'invalid-routing',
  'wrong-source',
]) {
  test(`selected-source publication: ${scenario}`, (t) => {
    const {
      publish,
      root,
      sha,
      refOid,
      trace,
      frontend,
      cli,
      jar,
      startup,
      applicationRecords,
      backendRecord,
    } = makePublication(t, scenario)
    const forced = scenario === 'forced'
    const result = publish()
    if (['moved', 'invalid-routing', 'wrong-source'].includes(scenario)) {
      assert.notEqual(result.status, 0)
      assert.equal(existsSync(trace), false, result.stdout)
      assert.match(
        result.stderr,
        scenario === 'moved'
          ? /Release ref changed/
          : scenario === 'wrong-source'
            ? /does not match selected SHA/
            : /FAILED/
      )
      return
    }
    assert.equal(result.status, 0, result.stderr)
    assert.match(
      readFileSync(join(root, 'captured-map'), 'utf8'),
      /selected-source-only/
    )
    const calls = readFileSync(trace, 'utf8').trim().split('\n')
    assert.equal(
      calls[0],
      'gsutil cp - gs://private-backend/deploy/application-release.json'
    )
    assert.deepEqual(calls.slice(1, 4), [
      `gsutil -m rsync -r ${frontend} gs://public-frontend/frontend/${sha}/`,
      `gsutil -h Cache-Control:public,max-age=60 cp ${frontend}/index.html gs://public-frontend/frontend/${sha}/index.html`,
      `gsutil cp -a public-read ${cli} gs://public-frontend/doughnut-cli-latest/doughnut`,
    ])
    assert.match(calls[4], /^gcloud compute url-maps import /)
    if (forced) {
      assert.ok(
        calls.includes(
          `gsutil cp ${jar} gs://private-backend/backend_app_jar/donut-0.0.1-SNAPSHOT.jar`
        )
      )
      assert.ok(calls.some((call) => call.includes('rolling-action replace')))
      assert.equal(
        JSON.parse(readFileSync(join(root, 'saved-record'))).git_sha,
        sha
      )
      assert.equal(
        readFileSync(join(root, 'captured-startup'), 'utf8'),
        startup
      )
      assert.equal(
        JSON.parse(readFileSync(join(root, 'saved-record')))
          .startup_script_sha256,
        hash(startup)
      )
    } else {
      assert.match(result.stdout, /Deploy skipped/)
      assert.equal(calls.length, 8)
      assert.deepEqual(readApplicationRecords(applicationRecords), [
        {
          tag: 'v1.2.3',
          ref_oid: refOid,
          sha,
          ci_run_id: '42',
          ci_run_attempt: '3',
          outcome: 'publishing',
        },
        {
          tag: 'v1.2.3',
          ref_oid: refOid,
          sha,
          ci_run_id: '42',
          ci_run_attempt: '3',
          outcome: 'succeeded',
        },
      ])
      assert.equal(existsSync(join(root, 'saved-record')), false)
      assert.deepEqual(JSON.parse(readFileSync(backendRecord)), {
        sha256: hash('selected jar'),
        startup_script_sha256: hash(startup),
      })
    }
  })
}

test('failed publication leaves the admitted application publishing', (t) => {
  const { publish, trace, applicationRecords } = makePublication(
    t,
    'failed-frontend'
  )

  const result = publish()

  assert.notEqual(result.status, 0)
  const calls = readFileSync(trace, 'utf8').trim().split('\n')
  assert.equal(calls.length, 2)
  assert.equal(
    calls[0],
    'gsutil cp - gs://private-backend/deploy/application-release.json'
  )
  assert.match(calls[1], /^gsutil -m rsync/)
  assert.deepEqual(
    readApplicationRecords(applicationRecords).map((record) => record.outcome),
    ['publishing']
  )
})

test('workflows share publication commands and retain independent CLI build version', () => {
  const workflow = (name) =>
    parse(
      readFileSync(
        join(repositoryRoot, '.github/workflows', `${name}.yml`),
        'utf8'
      )
    )
  const application = workflow('deploy').jobs.Deploy.steps
  assert.ok(application.some((step) => step.id === 'publish'))
  assert.equal(
    workflow('deploy').jobs.Deploy.env.RELEASE_CI_RUN_ID,
    '${{ needs.release-admission.outputs.run_id }}'
  )
  assert.equal(
    workflow('deploy').jobs.Deploy.env.RELEASE_CI_RUN_ATTEMPT,
    '${{ needs.release-admission.outputs.run_attempt }}'
  )
  const cli = workflow('cli-release')
  assert.deepEqual(cli.on.push.tags, ['cli-*'])
  const steps = cli.jobs['build-and-publish'].steps
  assert.ok(
    steps.some(
      (step) =>
        step.run ===
        'echo "CLI_VERSION=${GITHUB_REF#refs/tags/cli-v}" >> $GITHUB_ENV'
    )
  )
  assert.ok(steps.some((step) => step.run === 'pnpm cli:bundle'))
  assert.equal(
    steps.at(-1).run,
    'infra/gcp/scripts/upload-cli-binary-to-gcs.sh'
  )
})
