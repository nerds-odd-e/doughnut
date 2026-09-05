import assert from 'node:assert/strict'
import { test } from 'node:test'
import { run, scriptedGithub } from './watch-ci-test-fixtures.mjs'
import { watchCiExecution } from './watch-ci.mjs'

test('startup reports only the newest completed run and retained attempts', async () => {
  const controller = new AbortController()
  const calls = []
  const events = []
  const responses = [
    [
      run({
        databaseId: 31,
        attempt: 2,
        createdAt: '2026-09-05T10:00:00Z',
        conclusion: 'failure',
      }),
      run({
        databaseId: 30,
        createdAt: '2026-09-05T08:00:00Z',
        conclusion: 'failure',
      }),
      run({
        databaseId: 32,
        attempt: 2,
        createdAt: '2026-09-05T09:00:00Z',
        status: 'in_progress',
        conclusion: null,
      }),
    ],
    { status: 'completed', conclusion: 'timed_out' },
    { status: 'completed', conclusion: 'failure' },
    { jobs: [{ name: 'Newest baseline', conclusion: 'failure' }] },
    [
      run({
        databaseId: 30,
        createdAt: '2026-09-05T08:00:00Z',
        conclusion: 'failure',
      }),
      run({
        databaseId: 32,
        attempt: 2,
        createdAt: '2026-09-05T09:00:00Z',
        status: 'in_progress',
        conclusion: null,
      }),
    ],
  ]
  let sleeps = 0

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    now: () => Date.parse('2026-09-05T12:00:00Z'),
    emit: (event) => events.push(event),
    sleep: async () => {
      sleeps += 1
      if (sleeps === 2) controller.abort()
    },
    gh: scriptedGithub(responses, calls),
  })

  assert.deepEqual(events, [
    {
      type: 'CI_FAILURE',
      repo: 'example/donut',
      sha: 'a'.repeat(40),
      branch: 'main',
      workflow: 'ci.yml',
      runId: 31,
      attempt: 2,
      conclusion: 'failure',
      url: 'https://github.com/example/donut/actions/runs/42',
      relatedFailures: [
        {
          runId: 31,
          attempt: 1,
          conclusion: 'timed_out',
          url: 'https://github.com/example/donut/actions/runs/42',
        },
        {
          runId: 32,
          attempt: 1,
          conclusion: 'failure',
          url: 'https://github.com/example/donut/actions/runs/42',
        },
      ],
      failedJobs: [{ name: 'Newest baseline', conclusion: 'failure' }],
    },
  ])
  assert.equal(
    calls.some((args) => args.includes('30')),
    false
  )
  assert.equal(responses.length, 0)
})

test('startup keeps a run that completes while its snapshot is pending', async () => {
  const controller = new AbortController()
  const events = []
  let releaseSnapshot
  const snapshot = new Promise((resolve) => {
    releaseSnapshot = resolve
  })
  let listCalls = 0

  const observation = watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    now: () => Date.parse('2026-09-05T12:00:00Z'),
    emit: (event) => events.push(event),
    sleep: async () => {
      if (events.length) controller.abort()
    },
    gh: async (args) => {
      if (args[1] === 'list') {
        listCalls += 1
        return listCalls === 1 ? snapshot : []
      }
      if (args.includes('--attempt')) {
        return { jobs: [{ name: 'Racing run', conclusion: 'failure' }] }
      }
      return { status: 'completed', conclusion: 'failure' }
    },
  })

  await Promise.resolve()
  assert.deepEqual(events, [])
  releaseSnapshot([
    run({
      databaseId: 41,
      createdAt: '2026-09-05T11:59:00Z',
      status: 'in_progress',
      conclusion: null,
    }),
  ])
  await observation

  assert.deepEqual(
    events.map(({ runId, conclusion }) => ({ runId, conclusion })),
    [{ runId: 41, conclusion: null }]
  )
})

test('empty startup history is quiet', async () => {
  const controller = new AbortController()
  const events = []

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    emit: (event) => events.push(event),
    sleep: async () => controller.abort(),
    gh: async () => [],
  })

  assert.deepEqual(events, [])
})
