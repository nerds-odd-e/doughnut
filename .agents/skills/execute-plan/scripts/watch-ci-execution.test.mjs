import assert from 'node:assert/strict'
import { test } from 'node:test'
import { run, scriptedGithub } from './watch-ci-test-fixtures.mjs'
import { watchCiExecution } from './watch-ci.mjs'

test('execution observation emits only new main push CI failures', async () => {
  const controller = new AbortController()
  const calls = []
  const events = []
  const responses = [
    new Error('temporary GitHub outage'),
    [
      run({
        databaseId: 1,
        workflowName: 'donut deploy',
        conclusion: 'failure',
      }),
      run({ databaseId: 2, headBranch: 'feature', conclusion: 'failure' }),
      run({ databaseId: 3, event: 'workflow_dispatch', conclusion: 'failure' }),
      run({ databaseId: 4, status: 'queued', conclusion: null }),
      run({ databaseId: 5 }),
      run({ databaseId: 6, conclusion: 'failure' }),
    ],
    { jobs: [{ name: 'Backend', conclusion: 'failure' }] },
    [
      run({ databaseId: 6, conclusion: 'failure' }),
      run({ databaseId: 7, conclusion: 'timed_out' }),
    ],
    { status: 'completed', conclusion: 'success' },
    { jobs: [{ name: 'Frontend', conclusion: 'timed_out' }] },
  ]
  const sleeps = []

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    emit: (event) => events.push(event),
    sleep: async (milliseconds) => {
      sleeps.push(milliseconds)
      if (sleeps.length === 3) controller.abort()
    },
    gh: scriptedGithub(responses, calls),
  })

  assert.deepEqual(
    events.map(({ runId, conclusion, failedJobs }) => ({
      runId,
      conclusion,
      failedJobs,
    })),
    [
      {
        runId: 6,
        conclusion: 'failure',
        failedJobs: [{ name: 'Backend', conclusion: 'failure' }],
      },
      {
        runId: 7,
        conclusion: 'timed_out',
        failedJobs: [{ name: 'Frontend', conclusion: 'timed_out' }],
      },
    ]
  )
  assert.deepEqual(sleeps, [30_000, 30_000, 30_000])
  assert.ok(
    calls.every(
      ([resource, operation]) =>
        resource === 'run' && ['list', 'view'].includes(operation)
    )
  )
  assert.ok(
    calls
      .filter(([, operation]) => operation === 'list')
      .every((args) => !args.includes('--commit'))
  )
  assert.equal(responses.length, 0)
})

test('execution observation has one finite budget across all polls', async () => {
  let elapsed = 0
  let polls = 0

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    maxDurationMs: 60_000,
    now: () => elapsed,
    sleep: async (milliseconds) => {
      elapsed += milliseconds
    },
    gh: async (args) => {
      const [, operation] = args
      if (args[args.indexOf('--json') + 1] === 'jobs') return { jobs: [] }
      if (operation === 'view')
        return { status: 'completed', conclusion: 'success' }
      polls += 1
      return [run({ databaseId: polls, status: 'queued', conclusion: null })]
    },
  })

  assert.equal(polls, 2)
})

test('retains unfinished runs discovered beyond the newest page', async () => {
  const controller = new AbortController()
  const calls = []
  const events = []
  const oldRun = run({
    databaseId: 21,
    headSha: 'b'.repeat(40),
    status: 'in_progress',
    conclusion: null,
  })
  const externalRun = run({
    databaseId: 22,
    headSha: 'c'.repeat(40),
    status: 'in_progress',
    conclusion: null,
  })
  const responses = [
    [
      ...Array.from({ length: 20 }, (_, index) =>
        run({ databaseId: index + 1 })
      ),
      oldRun,
    ],
    { jobs: [] },
    [],
    oldRun,
    { jobs: [] },
    [externalRun],
    oldRun,
    { jobs: [] },
    { jobs: [] },
    [{ ...externalRun, status: 'completed', conclusion: 'failure' }],
    oldRun,
    { jobs: [{ name: 'External push', conclusion: 'failure' }] },
    [],
    { ...oldRun, status: 'completed', conclusion: 'failure' },
    { jobs: [{ name: 'Retained run', conclusion: 'failure' }] },
  ]
  let elapsed = 0

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    maxDurationMs: 200_000,
    now: () => elapsed,
    emit: (event) => events.push(event),
    sleep: async (milliseconds) => {
      elapsed += milliseconds
      if (events.length === 2) controller.abort()
    },
    gh: scriptedGithub(responses, calls),
  })

  assert.deepEqual(
    events.map(({ runId, sha }) => ({ runId, sha })),
    [
      { runId: 22, sha: 'c'.repeat(40) },
      { runId: 21, sha: 'b'.repeat(40) },
    ]
  )
  assert.equal(new Set(events.map(({ runId }) => runId)).size, 2)
  assert.ok(calls[0].includes('--created'))
  assert.equal(calls[0][calls[0].indexOf('--limit') + 1], '100')
  assert.ok(
    calls
      .slice(1)
      .some(
        (args) =>
          args.includes('21') &&
          args.some((arg) => arg.includes('status,conclusion'))
      )
  )
  assert.equal(elapsed, 150_000)
})
