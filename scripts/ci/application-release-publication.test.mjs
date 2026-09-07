import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'
import {
  hash,
  makePublication,
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
    const { publish, root, sha, trace, frontend, cli, jar, startup } =
      makePublication(t, scenario)
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
    assert.deepEqual(calls.slice(0, 3), [
      `gsutil -m rsync -r ${frontend} gs://public-frontend/frontend/${sha}/`,
      `gsutil -h Cache-Control:public,max-age=60 cp ${frontend}/index.html gs://public-frontend/frontend/${sha}/index.html`,
      `gsutil cp -a public-read ${cli} gs://public-frontend/doughnut-cli-latest/doughnut`,
    ])
    assert.match(calls[3], /^gcloud compute url-maps import /)
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
      assert.equal(calls.length, 6)
    }
  })
}

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
