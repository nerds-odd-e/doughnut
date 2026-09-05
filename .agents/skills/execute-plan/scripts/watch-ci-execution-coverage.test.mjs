import assert from 'node:assert/strict'
import { test } from 'node:test'
import { run } from './watch-ci-test-fixtures.mjs'
import { executionBudgetMs, watchCiExecution } from './watch-ci-execution.mjs'

const observeUntilCoverageLoss = async (gh) => {
  const events = []
  let requestSignal

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    emit: (event) => events.push(event),
    sleep: async () => undefined,
    gh: async (args, signal) => {
      requestSignal = signal
      return gh(args)
    },
  })

  return { events, requestSignal }
}

for (const [source, gh] of [
  ['discovery', async () => Promise.reject(new Error('run listing failed'))],
  [
    'attempt history',
    async (args) => {
      if (args[1] === 'list') return [run({ attempt: 2 })]
      throw new Error('attempt history failed')
    },
  ],
  [
    'unfinished job inspection',
    async (args) => {
      if (args[1] === 'list')
        return [run({ status: 'in_progress', conclusion: null })]
      throw new Error('job inspection failed')
    },
  ],
]) {
  test(`persistent ${source} errors lose coverage exactly once and release requests`, async () => {
    const { events, requestSignal } = await observeUntilCoverageLoss(gh)

    assert.deepEqual(
      events.map(({ type }) => type),
      ['CI_MONITOR_UNAVAILABLE']
    )
    assert.match(events[0].reason, /failed/)
    assert.equal(requestSignal.aborted, true)
  })
}

test('worker failure retains prior evidence before reporting lost coverage', async () => {
  const events = []
  let poll = 0
  let requestSignal

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    emit: (event) => events.push(event),
    sleep: async () => {
      if (poll === 1) return
      throw new Error('polling worker failed')
    },
    gh: async (args, signal) => {
      requestSignal = signal
      if (args[1] === 'list') {
        poll += 1
        return poll === 1
          ? [run({ conclusion: 'failure' })]
          : [run({ status: 'queued', conclusion: null })]
      }
      return {
        jobs: [{ databaseId: 101, name: 'Backend', conclusion: 'failure' }],
      }
    },
  })

  assert.deepEqual(
    events.map(({ type }) => type),
    ['CI_FAILURE', 'CI_MONITOR_UNAVAILABLE']
  )
  assert.match(events[1].reason, /polling worker failed/)
  assert.equal(requestSignal.aborted, true)
})

test('the declared execution budget expires once and is not renewed by pushes', async () => {
  const events = []
  let elapsed = 0
  let pushes = 0

  await watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    maxDurationMs: 60_000,
    now: () => elapsed,
    emit: (event) => events.push(event),
    sleep: async (milliseconds) => {
      elapsed += milliseconds
    },
    gh: async (args) => {
      if (args[1] === 'list') {
        pushes += 1
        return [
          run({
            databaseId: pushes,
            headSha: String(pushes).padStart(40, '0'),
            status: 'queued',
            conclusion: null,
          }),
        ]
      }
      return { jobs: [] }
    },
  })

  assert.equal(executionBudgetMs, 8 * 60 * 60 * 1000)
  assert.equal(pushes, 2)
  assert.deepEqual(
    events.map(({ type }) => type),
    ['CI_MONITOR_UNAVAILABLE']
  )
  assert.match(events[0].reason, /budget.*60000 ms/i)
})

test('execution observation rejects a non-finite setup budget', async () => {
  await assert.rejects(
    watchCiExecution({
      repo: 'example/donut',
      branch: 'main',
      maxDurationMs: Number.POSITIVE_INFINITY,
    }),
    /finite positive budget/
  )
})

test('normal shutdown cancels polling immediately without a coverage-loss event', async () => {
  const controller = new AbortController()
  const events = []
  let pollingStarted
  const started = new Promise((resolve) => {
    pollingStarted = resolve
  })

  const observation = watchCiExecution({
    repo: 'example/donut',
    branch: 'main',
    signal: controller.signal,
    emit: (event) => events.push(event),
    sleep: async (_milliseconds, _value, { signal }) => {
      pollingStarted()
      await new Promise((resolve, reject) => {
        signal.addEventListener('abort', () => reject(signal.reason), {
          once: true,
        })
      })
    },
    gh: async () => [],
  })

  await started
  controller.abort()
  await observation

  assert.deepEqual(events, [])
})
