import assert from 'node:assert/strict'
import { setImmediate } from 'node:timers/promises'
import { test } from 'node:test'
import { waitForSelectedCi } from './application-release-ci.mjs'
import {
  ciRun,
  repository,
  runCiCommand,
  selectedSha,
} from './application-release-ci-fixtures.mjs'

function waiting(t, responses) {
  let now = 0
  const timers = new Map()
  const requests = []
  const clock = {
    setTimeout(callback, delay) {
      const timer = { callback, at: now + delay }
      timers.set(timer, timer)
      return timer
    },
    clearTimeout(timer) {
      timers.delete(timer)
    },
  }
  t.mock.method(globalThis, 'fetch', async (url, { signal }) => {
    requests.push({ url, signal })
    const response = responses.shift()
    if (response === 'blocked') {
      return new Promise((_, reject) => {
        signal.addEventListener('abort', () => reject(signal.reason), {
          once: true,
        })
      })
    }
    return {
      ok: true,
      json: async () => ({
        total_count: response.length,
        workflow_runs: response,
      }),
    }
  })
  return {
    requests,
    timers,
    run: (options = {}) =>
      waitForSelectedCi({ repository, sha: selectedSha, clock, ...options }),
    async tick(ms = 30_000) {
      now += ms
      for (const timer of [...timers.values()]) {
        if (timer.at <= now && timers.delete(timer)) timer.callback()
      }
      await setImmediate()
    },
  }
}

test('default command waits and admits already successful CI', async (t) => {
  const result = await runCiCommand(
    t,
    [{ total_count: 1, workflow_runs: [ciRun()] }],
    []
  )
  assert.equal(result.status, 0, result.stderr)
  assert.equal(JSON.parse(result.stdout).state, 'ready')
})

test('tag before CI waits for appearance and completion using the same exact selection', async (t) => {
  const fixture = waiting(t, [[], [ciRun({ status: 'queued' })], [ciRun()]])
  const result = fixture.run()
  await setImmediate()
  await fixture.tick()
  await fixture.tick()
  assert.deepEqual(await result, {
    state: 'ready',
    sha: selectedSha,
    runId: 42,
    runAttempt: 1,
  })
  assert.equal(fixture.requests.length, 3)
  assert.ok(
    fixture.requests.every(
      ({ url }) => url.searchParams.get('head_sha') === selectedSha
    )
  )
  assert.equal(fixture.timers.size, 0)
})

test('already successful CI finishes without sleeping and releases the deadline', async (t) => {
  const fixture = waiting(t, [[ciRun()]])
  assert.equal((await fixture.run()).state, 'ready')
  assert.equal(fixture.timers.size, 0)
})

test('newer running attempt supersedes older success until that attempt succeeds', async (t) => {
  const fixture = waiting(t, [
    [ciRun(), ciRun({ run_attempt: 2, status: 'in_progress' })],
    [ciRun(), ciRun({ run_attempt: 2 })],
  ])
  const result = fixture.run()
  await setImmediate()
  await fixture.tick()
  assert.equal((await result).runAttempt, 2)
})

for (const conclusion of ['failure', 'cancelled']) {
  test(`${conclusion} terminates waiting without retry or older green fallback`, async (t) => {
    const fixture = waiting(t, [
      [ciRun({ status: 'in_progress' })],
      [ciRun(), ciRun({ run_attempt: 2, conclusion })],
    ])
    const rejected = assert.rejects(
      fixture.run(),
      new RegExp(`attempt 2.*${conclusion}`)
    )
    await setImmediate()
    await fixture.tick()
    await rejected
    assert.equal(fixture.timers.size, 0)
    assert.equal(fixture.requests.length, 2)
  })
}

test('default deadline ends an unfinished lookup after 60 minutes and cancels transport', async (t) => {
  const fixture = waiting(t, ['blocked'])
  const rejected = assert.rejects(fixture.run(), /Timed out waiting for CI/)
  await setImmediate()
  await fixture.tick(60 * 60 * 1000)
  await rejected
  assert.equal(fixture.requests[0].signal.aborted, true)
  assert.equal(fixture.timers.size, 0)
})

test('deadline cancels the pending pause without issuing another lookup', async (t) => {
  const fixture = waiting(t, [[]])
  const rejected = assert.rejects(
    fixture.run({ timeoutMs: 10_000 }),
    /Timed out waiting for CI/
  )
  await setImmediate()
  await fixture.tick(10_000)
  await rejected
  assert.equal(fixture.requests.length, 1)
  assert.equal(fixture.timers.size, 0)
})

for (const response of [[], 'blocked']) {
  test(`caller cancellation stops ${response === 'blocked' ? 'lookup' : 'pause'} and clears timers`, async (t) => {
    const fixture = waiting(t, [response])
    const controller = new AbortController()
    const rejected = assert.rejects(
      fixture.run({ signal: controller.signal }),
      /Stopped release/
    )
    await setImmediate()
    controller.abort(new Error('Stopped release'))
    await rejected
    assert.equal(fixture.timers.size, 0)
  })
}
