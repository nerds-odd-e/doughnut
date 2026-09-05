import assert from 'node:assert/strict'
import { test } from 'node:test'
import { run } from './watch-ci-test-fixtures.mjs'
import { watchCiExecution } from './watch-ci.mjs'

test('reports each failed job while its workflow remains unfinished', async () => {
  const controller = new AbortController()
  const events = []
  let poll = 0

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    emit: (event) => events.push(event),
    sleep: async () => {
      if (poll === 5) controller.abort()
    },
    gh: async (args) => {
      const fields = args[args.indexOf('--json') + 1]
      if (args[1] === 'list') {
        poll += 1
        if (poll < 4) {
          return [
            run({
              status: 'in_progress',
              conclusion: null,
              createdAt: '2026-09-05T10:00:00Z',
            }),
          ]
        }
        if (poll === 4) {
          return [
            run({
              attempt: 2,
              status: 'in_progress',
              conclusion: null,
              createdAt: '2026-09-05T10:00:00Z',
            }),
          ]
        }
        return [run({ attempt: 2, createdAt: '2026-09-05T10:00:00Z' })]
      }
      if (fields === 'status,conclusion') {
        return { status: 'completed', conclusion: 'failure' }
      }
      if (args[args.indexOf('--attempt') + 1] === '2') {
        return {
          jobs: [
            {
              databaseId: 201,
              name: 'New attempt',
              conclusion: 'failure',
            },
          ],
        }
      }
      return {
        jobs:
          poll === 1
            ? [{ databaseId: 101, name: 'Backend', conclusion: null }]
            : [
                {
                  databaseId: 101,
                  name: 'Backend',
                  conclusion: 'failure',
                },
                {
                  databaseId: 102,
                  name: 'Frontend',
                  conclusion: poll >= 3 ? 'timed_out' : null,
                },
              ],
      }
    },
  })

  assert.deepEqual(
    events.map(({ runId, attempt, failedJobs }) => ({
      runId,
      attempt,
      failedJobs,
    })),
    [
      {
        runId: 42,
        attempt: 1,
        failedJobs: [{ jobId: 101, name: 'Backend', conclusion: 'failure' }],
      },
      {
        runId: 42,
        attempt: 1,
        failedJobs: [{ jobId: 102, name: 'Frontend', conclusion: 'timed_out' }],
      },
      {
        runId: 42,
        attempt: 2,
        failedJobs: [
          { jobId: 201, name: 'New attempt', conclusion: 'failure' },
        ],
      },
    ]
  )
})

test('keeps run fallback distinct from later job evidence and cancellation', async () => {
  const controller = new AbortController()
  const events = []
  let poll = 0

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    now: () => Date.parse('2026-09-05T09:00:00Z'),
    emit: (event) => events.push(event),
    sleep: async () => {
      if (poll === 3) controller.abort()
    },
    gh: async (args) => {
      if (args[1] === 'list') {
        poll += 1
        if (poll < 3) {
          return [
            run({
              databaseId: 51,
              conclusion: 'failure',
              createdAt: '2026-09-05T10:00:00Z',
            }),
          ]
        }
        return [
          run({
            databaseId: 52,
            conclusion: 'cancelled',
            createdAt: '2026-09-05T11:00:00Z',
          }),
        ]
      }
      if (poll === 1) throw new Error('job details unavailable')
      return {
        jobs: [{ databaseId: 501, name: 'Backend', conclusion: 'failure' }],
      }
    },
  })

  assert.deepEqual(
    events.map(({ type, runId, attempt, detailsUnavailable, failedJobs }) => ({
      type,
      runId,
      attempt,
      ...(detailsUnavailable ? { detailsUnavailable } : {}),
      ...(failedJobs ? { failedJobs } : {}),
    })),
    [
      {
        type: 'CI_FAILURE',
        runId: 51,
        attempt: 1,
        detailsUnavailable: true,
      },
      {
        type: 'CI_FAILURE',
        runId: 51,
        attempt: 1,
        failedJobs: [{ jobId: 501, name: 'Backend', conclusion: 'failure' }],
      },
      { type: 'CI_INCOMPLETE', runId: 52, attempt: 1 },
    ]
  )
})
