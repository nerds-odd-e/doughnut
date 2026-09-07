import { pathToFileURL } from 'node:url'
import { writeReleaseOutput } from './application-release-output.mjs'

// One lookup of the selected commit's latest main-push CI, without waiting.
export async function querySelectedCi({
  repository,
  sha,
  apiUrl = process.env.GITHUB_API_URL || 'https://api.github.com',
  token = process.env.GITHUB_TOKEN,
  signal,
}) {
  const runs = []
  for (let page = 1; page <= 10; page++) {
    const url = new URL(
      `/repos/${repository}/actions/workflows/ci.yml/runs`,
      apiUrl
    )
    url.search = new URLSearchParams({
      branch: 'main',
      event: 'push',
      head_sha: sha,
      per_page: '100',
      page: String(page),
    })
    const response = await fetch(url, {
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(30_000)])
        : AbortSignal.timeout(30_000),
      headers: {
        Accept: 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
    if (!response.ok) {
      throw new Error(`CI lookup failed: HTTP ${response.status}`)
    }
    const { total_count: total, workflow_runs: batch } = await response.json()
    if (total > 1000) {
      throw new Error('CI lookup exceeds the 1000-run search limit')
    }
    runs.push(...batch)
    if (runs.length >= total) break
    if (page === 10 || batch.length === 0) {
      throw new Error('CI lookup did not return the complete bounded result')
    }
  }
  const latest = runs
    .filter(
      (run) =>
        run.repository.full_name === repository &&
        run.head_repository.full_name === repository &&
        run.path.split('@')[0] === '.github/workflows/ci.yml' &&
        run.head_branch === 'main' &&
        run.event === 'push' &&
        run.head_sha === sha
    )
    .sort(
      (left, right) =>
        right.run_number - left.run_number ||
        right.run_attempt - left.run_attempt
    )[0]
  if (!latest) return { state: 'pending', sha }
  const identity = {
    sha,
    runId: latest.id,
    runAttempt: latest.run_attempt,
  }
  if (latest.status !== 'completed') {
    return { state: 'pending', ...identity }
  }
  if (latest.conclusion !== 'success') {
    throw new Error(
      `CI ${latest.id} attempt ${latest.run_attempt} for ${sha} finished with ${latest.conclusion}`
    )
  }
  return { state: 'ready', ...identity }
}

function pause(milliseconds, signal, clock) {
  signal.throwIfAborted()
  return new Promise((resolve, reject) => {
    const finish = () => {
      signal.removeEventListener('abort', abort)
      resolve()
    }
    const timer = clock.setTimeout(finish, milliseconds)
    const abort = () => {
      clock.clearTimeout(timer)
      signal.removeEventListener('abort', abort)
      reject(signal.reason)
    }
    signal.addEventListener('abort', abort, { once: true })
  })
}

export async function waitForSelectedCi({
  timeoutMs = 60 * 60 * 1000,
  pollMs = 30_000,
  signal: callerSignal,
  clock = globalThis,
  ...selection
}) {
  const deadline = new AbortController()
  const signal = callerSignal
    ? AbortSignal.any([callerSignal, deadline.signal])
    : deadline.signal
  const timer = clock.setTimeout(
    () =>
      deadline.abort(
        new Error(`Timed out waiting for CI for ${selection.sha}`)
      ),
    timeoutMs
  )
  try {
    while (true) {
      signal.throwIfAborted()
      const result = await querySelectedCi({ ...selection, signal })
      signal.throwIfAborted()
      if (result.state === 'ready') return result
      await pause(pollMs, signal, clock)
    }
  } finally {
    clock.clearTimeout(timer)
  }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const query = process.argv.includes('--once')
    ? querySelectedCi
    : waitForSelectedCi
  const result = await query({
    repository: process.env.GITHUB_REPOSITORY,
    sha: process.env.RELEASE_SHA,
  })
  writeReleaseOutput(result)
}
