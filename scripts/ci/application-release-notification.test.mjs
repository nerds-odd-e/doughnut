import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'
import {
  ciRun,
  runCiCommand,
  selectedSha,
} from './application-release-ci-fixtures.mjs'

function render(t, env = {}) {
  const root = mkdtempSync(join(tmpdir(), 'release-notification-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const output = join(root, 'output')
  const result = spawnSync(
    process.execPath,
    [
      fileURLToPath(
        new URL('./application-release-notification.mjs', import.meta.url)
      ),
    ],
    {
      encoding: 'utf8',
      env: {
        GITHUB_SERVER_URL: 'https://github.com',
        GITHUB_REPOSITORY: 'nerds-odd-e/doughnut',
        GITHUB_RUN_ID: '99',
        GITHUB_REF: 'refs/tags/v1.2.3',
        GITHUB_SHA: 'b'.repeat(40),
        GITHUB_OUTPUT: output,
        ...env,
      },
    }
  )
  assert.equal(result.status, 0, result.stderr)
  const written = readFileSync(output, 'utf8')
  assert.equal(written.split('\n').length, 2)
  const payload = JSON.parse(written.slice('payload='.length))
  return {
    payload,
    fields: Object.fromEntries(
      payload.attachments[0].blocks[0].fields.map(({ text }) => {
        const split = text.indexOf('\n')
        return [text.slice(0, split), text.slice(split + 1)]
      })
    ),
  }
}

test('identity failure names the raw tag without treating event SHA as a peeled commit', (t) => {
  const { fields } = render(t, { FAILURE_STAGE: 'identity' })
  assert.deepEqual(fields, {
    tag: 'v1.2.3',
    commit: 'unknown (identity not validated)',
    stage: 'identity',
    'CI run': 'unknown (CI not selected)',
    'release run': 'https://github.com/nerds-odd-e/doughnut/actions/runs/99',
  })
})

test('failed CI command preserves selected run outputs for notification even when deployment skips', async (t) => {
  const result = await runCiCommand(
    t,
    [
      {
        total_count: 1,
        workflow_runs: [ciRun({ conclusion: 'failure', run_attempt: 2 })],
      },
    ],
    []
  )
  assert.equal(result.status, 1)
  const outputs = Object.fromEntries(
    result.output
      .trim()
      .split('\n')
      .map((line) => line.split('='))
  )
  assert.equal(outputs.state, 'failed')
  const { fields } = render(t, {
    RELEASE_SHA: outputs.sha,
    RELEASE_CI_RUN_ID: outputs.runId,
    RELEASE_CI_RUN_ATTEMPT: outputs.runAttempt,
    FAILURE_STAGE: 'CI admission',
  })
  assert.equal(fields.commit, selectedSha)
  assert.equal(
    fields['CI run'],
    'https://github.com/nerds-odd-e/doughnut/actions/runs/42/attempts/2'
  )
  assert.equal(fields.stage, 'CI admission')
})

test('CI lookup failure before selecting a run preserves the validated commit', (t) => {
  const { fields } = render(t, {
    RELEASE_SHA: selectedSha,
    FAILURE_STAGE: 'CI admission',
  })
  assert.equal(fields.commit, selectedSha)
  assert.equal(fields['CI run'], 'unknown (CI not selected)')
})

for (const stage of ['artifact admission', 'publication']) {
  test(`${stage} failure retains the selected release context`, (t) => {
    const { fields } = render(t, {
      RELEASE_TAG: 'v2.3.4',
      RELEASE_SHA: selectedSha,
      RELEASE_CI_RUN_ID: '42',
      RELEASE_CI_RUN_ATTEMPT: '3',
      FAILURE_STAGE: stage,
    })
    assert.equal(fields.tag, 'v2.3.4')
    assert.equal(fields.commit, selectedSha)
    assert.equal(fields.stage, stage)
    assert.match(fields['CI run'], /42\/attempts\/3$/)
  })
}

test('notification serializes quotes and newlines as a single JSON output value', (t) => {
  const tag = 'v1.2.3"\n$(echo unsafe)'
  const { fields, payload } = render(t, { RELEASE_TAG: tag })
  assert.equal(fields.tag, tag)
  assert.equal(payload.attachments[0].blocks[0].fields[0].type, 'plain_text')
})

test('workflow notifies admission and deployment failures using selected outputs and failed step outcomes', () => {
  const workflow = parse(
    readFileSync(
      new URL('../../.github/workflows/deploy.yml', import.meta.url),
      'utf8'
    )
  )
  const admission = workflow.jobs['main-head-guard']
  const deploy = workflow.jobs.Deploy
  const notify = workflow.jobs['Notify-on-failure']
  assert.deepEqual(notify.needs, ['main-head-guard', 'Deploy'])
  assert.equal(
    notify.if,
    "always() && (needs.main-head-guard.result == 'failure' || needs.Deploy.result == 'failure')"
  )
  assert.match(
    admission.outputs.failure_stage,
    /steps.head_guard.outcome != 'success'.*'identity'.*'CI admission'/
  )
  assert.equal(
    admission.outputs.run_attempt,
    '${{ steps.ci.outputs.runAttempt }}'
  )
  for (const id of [
    'backend_artifact',
    'frontend_artifact',
    'cli_artifact',
    'payload',
    'publish',
  ]) {
    assert.ok(deploy.steps.some((step) => step.id === id))
    assert.ok(
      deploy.outputs.failure_stage.includes(`steps.${id}.outcome == 'failure'`)
    )
  }
  const renderStep = notify.steps.find((step) => step.id === 'ctx')
  assert.equal(
    renderStep.env.RELEASE_SHA,
    '${{ needs.main-head-guard.outputs.sha }}'
  )
  assert.match(
    renderStep.env.FAILURE_STAGE,
    /needs.main-head-guard.result == 'failure'/
  )
  assert.equal(
    notify.steps.at(-1).with.payload,
    '${{ steps.ctx.outputs.payload }}'
  )
  assert.doesNotMatch(JSON.stringify(notify), /workflow_run|git log/)
})
