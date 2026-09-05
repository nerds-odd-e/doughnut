import { execFile } from 'node:child_process'
import { pathToFileURL } from 'node:url'
import { promisify } from 'node:util'
import { setTimeout as pause } from 'node:timers/promises'

const execFileAsync = promisify(execFile)

async function github(args, signal) {
  const { stdout } = await execFileAsync('gh', args, {
    timeout: 20_000,
    signal,
    maxBuffer: 1024 * 1024,
    env: { ...process.env, GH_PROMPT_DISABLED: '1' },
  })
  return JSON.parse(stdout)
}

// One pushed revision, one terminal result. Polling never calls an AI service.
export async function watchCi({
  repo,
  sha,
  branch,
  signal,
  gh = github,
  sleep = pause,
  pollMs = 30_000,
  discoveryPolls = 10,
  maxPolls = 120,
}) {
  const context = { repo, sha, branch, workflow: 'ci.yml' }
  const unavailable = (reason) => ({
    type: 'CI_MONITOR_UNAVAILABLE',
    ...context,
    reason,
  })
  let errors = 0
  let discovered = false
  const priorAttempts = new Map()

  for (let poll = 0; poll < maxPolls; poll += 1) {
    signal?.throwIfAborted()
    let runs
    try {
      runs = await gh(
        [
          'run',
          'list',
          '--repo',
          repo,
          '--workflow',
          'ci.yml',
          '--branch',
          branch,
          '--commit',
          sha,
          '--event',
          'push',
          '--limit',
          '20',
          '--json',
          'databaseId,attempt,headSha,headBranch,workflowName,event,status,conclusion,url',
        ],
        signal
      )
      errors = 0
    } catch (error) {
      errors += 1
      if (errors === 3) return unavailable(String(error.message).slice(0, 600))
      await sleep(pollMs, undefined, { signal })
      continue
    }

    const matching = runs.filter(
      (run) =>
        run.headSha === sha &&
        run.headBranch === branch &&
        run.workflowName === 'donut CI' &&
        run.event === 'push'
    )
    discovered ||= matching.length > 0
    const attempts = [...matching]
    let historyError
    history: for (const run of matching) {
      for (let attempt = 1; attempt < run.attempt; attempt += 1) {
        const key = `${run.databaseId}:${attempt}`
        if (!priorAttempts.has(key)) {
          try {
            const prior = await gh(
              [
                'run',
                'view',
                String(run.databaseId),
                '--repo',
                repo,
                '--attempt',
                String(attempt),
                '--json',
                'status,conclusion',
              ],
              signal
            )
            priorAttempts.set(key, { ...run, ...prior, attempt })
          } catch (error) {
            historyError = `Could not inspect earlier CI attempts: ${String(error.message).slice(0, 600)}`
            break history
          }
        }
        attempts.push(priorAttempts.get(key))
      }
    }
    const failures = attempts.filter(
      (run) =>
        run.status === 'completed' &&
        [
          'failure',
          'timed_out',
          'startup_failure',
          'action_required',
          'stale',
        ].includes(run.conclusion)
    )
    const failed = failures[0]
    if (failed) {
      const event = {
        type: 'CI_FAILURE',
        ...context,
        runId: failed.databaseId,
        attempt: failed.attempt,
        conclusion: failed.conclusion,
        url: failed.url,
      }
      if (historyError) event.historyUnavailable = historyError
      if (failures.length > 1) {
        event.relatedFailures = failures.slice(1).map((run) => ({
          runId: run.databaseId,
          attempt: run.attempt,
          conclusion: run.conclusion,
          url: run.url,
        }))
      }
      try {
        const { jobs } = await gh(
          [
            'run',
            'view',
            String(failed.databaseId),
            '--repo',
            repo,
            '--attempt',
            String(failed.attempt),
            '--json',
            'jobs',
          ],
          signal
        )
        event.failedJobs = jobs
          .filter(
            (job) =>
              job.conclusion &&
              !['success', 'skipped', 'neutral', 'cancelled'].includes(
                job.conclusion
              )
          )
          .map((job) => ({ name: job.name, conclusion: job.conclusion }))
      } catch {
        event.detailsUnavailable = true
      }
      return event
    }
    if (historyError) return unavailable(historyError)
    if (
      matching.length &&
      matching.every((run) => run.status === 'completed')
    ) {
      if (matching.every((run) => run.conclusion === 'success')) return null
      return {
        type: 'CI_INCOMPLETE',
        ...context,
        runs: matching.map((run) => ({
          runId: run.databaseId,
          attempt: run.attempt,
          conclusion: run.conclusion,
          url: run.url,
        })),
      }
    }
    if (!discovered && poll + 1 >= discoveryPolls) {
      return unavailable(
        'No matching push CI run appeared within the discovery window.'
      )
    }
    await sleep(pollMs, undefined, { signal })
  }
  return unavailable(
    'CI observation window expired; its result is still unknown.'
  )
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  const [repo, sha, branch] = process.argv.slice(2)
  if (!(repo && /^[a-f0-9]{40}$/.test(sha ?? '')) || branch !== 'main') {
    throw new Error('Usage: node watch-ci.mjs OWNER/REPO FULL_PUSHED_SHA main')
  }
  const event = await watchCi({ repo, sha, branch })
  if (event) process.stdout.write(`${JSON.stringify(event)}\n`)
}
