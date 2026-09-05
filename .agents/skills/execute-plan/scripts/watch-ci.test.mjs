import assert from 'node:assert/strict'
import { test } from 'node:test'
import { watchCi } from './watch-ci.mjs'

const sha = 'a'.repeat(40)
const run = (overrides = {}) => ({
  databaseId: 42,
  attempt: 1,
  headSha: sha,
  headBranch: 'main',
  workflowName: 'donut CI',
  event: 'push',
  status: 'completed',
  conclusion: 'success',
  url: 'https://github.com/example/donut/actions/runs/42',
  ...overrides,
})

async function observe(responses, options = {}) {
  const calls = []
  const event = await watchCi({
    repo: 'example/donut',
    sha,
    branch: 'main',
    maxPolls: 6,
    sleep: () => Promise.resolve(),
    gh: async (args) => {
      calls.push(args)
      assert.ok(responses.length, 'unexpected GitHub request')
      const response = responses.shift()
      if (response instanceof Error) throw response
      return response
    },
    ...options,
  })
  return { event, calls }
}

test('discovers a pushed CI run and stays silent through pending and success', async () => {
  const { event, calls } = await observe([
    [],
    [run({ status: 'in_progress', conclusion: null })],
    [run()],
  ])
  assert.equal(event, null)
  assert.deepEqual(calls[0], [
    'run',
    'list',
    '--repo',
    'example/donut',
    '--workflow',
    'ci.yml',
    '--branch',
    'main',
    '--commit',
    sha,
    '--event',
    'push',
    '--limit',
    '20',
    '--json',
    'databaseId,attempt,headSha,headBranch,workflowName,event,status,conclusion,url',
  ])
})

test('ignores CD, another revision, another branch, and non-push runs', async () => {
  const { event } = await observe([
    [
      run({ workflowName: 'donut deploy', conclusion: 'failure' }),
      run({ headSha: 'b'.repeat(40), conclusion: 'failure' }),
      run({ headBranch: 'another', conclusion: 'failure' }),
      run({ event: 'workflow_run', conclusion: 'failure' }),
    ],
    [run()],
  ])
  assert.equal(event, null)
})

test('reports a failed attempt once, including a flaky test, without rerunning CI', async () => {
  const { event, calls } = await observe([
    [run({ conclusion: 'failure', attempt: 2 })],
    { status: 'completed', conclusion: 'success' },
    {
      jobs: [
        { name: 'Flaky Cypress test', conclusion: 'failure' },
        { name: 'Backend', conclusion: 'success' },
      ],
    },
  ])
  assert.deepEqual(event, {
    type: 'CI_FAILURE',
    repo: 'example/donut',
    sha,
    branch: 'main',
    workflow: 'ci.yml',
    runId: 42,
    attempt: 2,
    conclusion: 'failure',
    url: run().url,
    failedJobs: [{ name: 'Flaky Cypress test', conclusion: 'failure' }],
  })
  assert.deepEqual(calls[2], [
    'run',
    'view',
    '42',
    '--repo',
    'example/donut',
    '--attempt',
    '2',
    '--json',
    'jobs',
  ])
})

test('keeps a confirmed failure actionable when its job details are unavailable', async () => {
  const { event } = await observe([
    [run({ conclusion: 'timed_out' })],
    new Error('GitHub unavailable'),
  ])
  assert.equal(event.type, 'CI_FAILURE')
  assert.equal(event.detailsUnavailable, true)
})

test('recovers silently from transient observer errors', async () => {
  const { event } = await observe([new Error('HTTP 503'), [run()]])
  assert.equal(event, null)
})

test('reports lost monitoring once without classifying it as a test or server failure', async () => {
  const { event } = await observe(Array(3).fill(new Error('HTTP 403')))
  assert.equal(event.type, 'CI_MONITOR_UNAVAILABLE')
})

test('bounds discovery of absent runs', async () => {
  const { event } = await observe([[], []], { discoveryPolls: 2 })
  assert.equal(event.type, 'CI_MONITOR_UNAVAILABLE')
  assert.match(event.reason, /No matching push CI run/)
})

test('bounds observation of a CI run that never finishes', async () => {
  const { event } = await observe(
    [[run({ status: 'queued' })], [run({ status: 'queued' })]],
    { maxPolls: 2 }
  )
  assert.match(event.reason, /observation window expired/)
})

test('does not equate cancellation with success or a code failure', async () => {
  const { event } = await observe([[run({ conclusion: 'cancelled' })]])
  assert.equal(event.type, 'CI_INCOMPLETE')
})

test('reports an earlier failure even when a rerun is already green', async () => {
  const { event } = await observe([
    [run({ attempt: 2 })],
    { status: 'completed', conclusion: 'failure' },
    { jobs: [{ name: 'Cypress', conclusion: 'failure' }] },
  ])
  assert.equal(event.type, 'CI_FAILURE')
  assert.equal(event.attempt, 1)
})

test('retains distinct failed attempts so infrastructure cannot hide another defect', async () => {
  const { event } = await observe([
    [run({ attempt: 3 })],
    { status: 'completed', conclusion: 'failure' },
    { status: 'completed', conclusion: 'timed_out' },
    { jobs: [] },
  ])
  assert.deepEqual(event.relatedFailures, [
    {
      runId: 42,
      attempt: 2,
      conclusion: 'timed_out',
      url: run().url,
    },
  ])
})

test('does not claim a rerun is clean when earlier attempts cannot be inspected', async () => {
  const { event } = await observe([
    [run({ attempt: 2 })],
    new Error('attempt details unavailable'),
  ])
  assert.equal(event.type, 'CI_MONITOR_UNAVAILABLE')
})

test('retains a confirmed current failure when prior history is unavailable', async () => {
  const { event } = await observe([
    [run({ attempt: 2, conclusion: 'failure' })],
    new Error('history unavailable'),
    { jobs: [] },
  ])
  assert.equal(event.type, 'CI_FAILURE')
  assert.equal(event.attempt, 2)
  assert.match(event.historyUnavailable, /history unavailable/)
})

test('retains an earlier confirmed failure when a subsequent history lookup fails', async () => {
  const { event } = await observe([
    [run({ attempt: 3 })],
    { status: 'completed', conclusion: 'failure' },
    new Error('history unavailable'),
    { jobs: [] },
  ])
  assert.equal(event.type, 'CI_FAILURE')
  assert.equal(event.attempt, 1)
})
