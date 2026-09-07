import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import {
  mkdtempSync,
  mkdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
  existsSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'

const command = fileURLToPath(
  new URL('./application-release-payload.mjs', import.meta.url)
)

function makePayload(t) {
  const root = mkdtempSync(join(tmpdir(), 'release-payload-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const files = {
    backend: join(root, 'backend/donut-0.0.1-SNAPSHOT.jar'),
    frontend: join(root, 'frontend/index.html'),
    cli: join(root, 'cli/donut-cli.bundle.mjs'),
  }
  for (const [name, path] of Object.entries(files)) {
    mkdirSync(join(root, name), { recursive: true })
    writeFileSync(path, `selected CI ${name} content`)
  }
  mkdirSync(join(root, 'frontend/assets'))
  writeFileSync(join(root, 'frontend/assets/app.js'), 'selected frontend asset')
  return {
    files,
    run() {
      const trace = join(root, 'production-writes')
      const result = spawnSync(
        'bash',
        [
          '-c',
          'node "$1" && printf published > "$2"',
          'payload-test',
          command,
          trace,
        ],
        {
          encoding: 'utf8',
          env: {
            ...process.env,
            DEPLOY_JAR_PATH: files.backend,
            FRONTEND_STATIC_DIR: join(root, 'frontend'),
            CLI_BUNDLE_SOURCE: files.cli,
          },
        }
      )
      return { ...result, published: existsSync(trace) }
    },
  }
}

test('complete selected payload permits the publication boundary', (t) => {
  const result = makePayload(t).run()
  assert.equal(result.status, 0, result.stderr)
  assert.equal(result.published, true)
})

for (const component of ['backend', 'frontend', 'cli']) {
  for (const invalid of ['missing', 'empty', 'directory']) {
    test(`${invalid} ${component} stops before any production write`, (t) => {
      const payload = makePayload(t)
      const path = payload.files[component]
      if (invalid === 'empty') writeFileSync(path, '')
      else {
        rmSync(path)
        if (invalid === 'directory') mkdirSync(path)
      }
      const result = payload.run()
      assert.equal(result.status, 1)
      assert.ok(result.stderr.includes(path), result.stderr)
      assert.equal(result.published, false)
    })
  }
}

test('all admitted-run downloads and preflight precede the first production step', () => {
  const deploy = parse(
    readFileSync(
      new URL('../../.github/workflows/deploy.yml', import.meta.url),
      'utf8'
    )
  )
  const admission = deploy.jobs['release-admission']
  assert.equal(admission.outputs.run_id, '${{ steps.ci.outputs.runId }}')
  const ci = admission.steps.find((step) => step.id === 'ci')
  assert.equal(ci.env.RELEASE_SHA, '${{ steps.identity.outputs.sha }}')
  assert.match(ci.run, /node scripts\/ci\/application-release-ci.mjs/)
  const { steps, env } = deploy.jobs.Deploy
  const downloads = steps.filter(
    (step) => step.uses === 'actions/download-artifact@v8'
  )
  assert.deepEqual(
    downloads.map((step) => step.with.path),
    [
      'release-artifacts/backend',
      'release-artifacts/frontend',
      'release-artifacts/cli',
    ]
  )
  assert.deepEqual(
    downloads.map((step) => step.with.name),
    [
      '${{ env.ARTIFACT }}-${{ env.VERSION }}.jar',
      '${{ env.FRONTEND_DIST_ARTIFACT }}',
      '${{ env.CLI_DIST_ARTIFACT }}',
    ]
  )
  const preflight = steps.findIndex(
    (step) => step.run === 'node scripts/ci/application-release-payload.mjs'
  )
  for (const download of downloads) {
    assert.equal(
      download.with['run-id'],
      '${{ needs.release-admission.outputs.run_id }}'
    )
    assert.equal(download['continue-on-error'], undefined)
    assert.ok(steps.indexOf(download) < preflight)
  }
  assert.ok(preflight < steps.findIndex((step) => step.id === 'publish'))
  assert.equal(
    env.DEPLOY_JAR_PATH,
    'release-artifacts/backend/donut-0.0.1-SNAPSHOT.jar'
  )
  assert.equal(env.FRONTEND_STATIC_DIR, 'release-artifacts/frontend')
  assert.equal(
    env.CLI_BUNDLE_SOURCE,
    'release-artifacts/cli/donut-cli.bundle.mjs'
  )
})
