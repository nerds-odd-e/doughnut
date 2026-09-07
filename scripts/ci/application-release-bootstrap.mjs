import { execFileSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'
import {
  fetchAdmissionJobLog,
  fetchDeployWorkflowRuns,
  fetchWorkflowRunJobs,
} from './application-release-bootstrap-history.mjs'
import { writeReleaseOutput } from './application-release-output.mjs'

const applicationTag = /^refs\/tags\/v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/
const objectId = /^[0-9a-f]{40}$/
function applicationTags(repositoryRoot) {
  try {
    return execFileSync(
      'git',
      ['ls-remote', '--tags', 'origin', 'refs/tags/v*'],
      { cwd: repositoryRoot, encoding: 'utf8' }
    )
      .trim()
      .split('\n')
      .map((line) => line.split('\t')[1])
      .filter((ref) => applicationTag.test(ref))
  } catch (error) {
    throw new Error('Application tag lookup failed for origin', {
      cause: error,
    })
  }
}

function releaseIdentityFromLog(log) {
  const records = log.split('\n').flatMap((line) => {
    const start = line.indexOf('{')
    if (start === -1) return []
    try {
      return [JSON.parse(line.slice(start))]
    } catch {
      return []
    }
  })
  const releases = records.filter(
    (record) =>
      typeof record === 'object' &&
      record !== null &&
      applicationTag.test(record.ref) &&
      record.tag === record.ref.slice('refs/tags/'.length) &&
      objectId.test(record.refOid) &&
      objectId.test(record.sha)
  )
  const selectedCi = records.filter(
    (record) =>
      typeof record === 'object' &&
      record !== null &&
      record.state === 'ready' &&
      objectId.test(record.sha) &&
      Number.isInteger(record.runId) &&
      Number.isInteger(record.runAttempt)
  )
  if (
    releases.length !== 1 ||
    selectedCi.length !== 1 ||
    releases[0].sha !== selectedCi[0].sha
  ) {
    throw new Error(
      'Admission log does not contain one complete matching release and CI identity'
    )
  }
  return {
    state: 'published',
    tag: releases[0].tag,
    refOid: releases[0].refOid,
    sha: releases[0].sha,
    runId: selectedCi[0].runId,
    runAttempt: selectedCi[0].runAttempt,
  }
}

function isDeployPush(run, repository) {
  return (
    run.repository?.full_name === repository &&
    run.head_repository?.full_name === repository &&
    run.path?.split('@')[0] === '.github/workflows/deploy.yml' &&
    run.event === 'push' &&
    Number.isInteger(run.id) &&
    Number.isInteger(run.run_number) &&
    Number.isInteger(run.run_attempt)
  )
}

function publishedJobs(jobs) {
  const candidates = jobs.filter(
    (job) =>
      Number.isInteger(job.run_attempt) &&
      job.name === 'GCP deploy (GCS + MIG + health probe)' &&
      job.steps?.some(
        (step) =>
          step.name === 'Publish application to GCS and MIG' &&
          step.conclusion === 'success'
      )
  )
  return candidates.sort((left, right) => right.run_attempt - left.run_attempt)
}

async function publishedIdentity({ repository, run, jobs, apiBase, token }) {
  const publication = publishedJobs(jobs)[0]
  if (!publication) return
  const admission = jobs.filter(
    (job) =>
      job.name === 'Admit selected application release' &&
      job.run_attempt === publication.run_attempt
  )
  if (admission.length !== 1 || !Number.isInteger(admission[0].id)) {
    throw new Error(
      `Run ${run.id} attempt ${publication.run_attempt} has successful publication without one admission log`
    )
  }
  try {
    return releaseIdentityFromLog(
      await fetchAdmissionJobLog({
        repository,
        jobId: admission[0].id,
        run,
        runAttempt: publication.run_attempt,
        apiBase,
        token,
      })
    )
  } catch (error) {
    throw new Error(
      `Run ${run.id} attempt ${publication.run_attempt} has unverifiable admission evidence: ${error.message}`,
      { cause: error }
    )
  }
}

export async function classifyApplicationPublication({
  repository,
  repositoryRoot = process.cwd(),
  apiBase = process.env.GITHUB_API_URL || 'https://api.github.com',
  token = process.env.GITHUB_TOKEN,
  currentRunId = process.env.GITHUB_RUN_ID,
  currentRef = process.env.GITHUB_REF,
}) {
  const [observedTags, observedRuns] = await Promise.all([
    Promise.resolve(applicationTags(repositoryRoot)),
    fetchDeployWorkflowRuns({ repository, apiBase, token }),
  ])
  if (observedRuns.some((run) => !isDeployPush(run, repository))) {
    throw new Error(
      'Deploy workflow history contains an unexpected run identity'
    )
  }
  const currentRuns = observedRuns.filter(
    (run) => String(run.id) === currentRunId && run.status !== 'completed'
  )
  const hasVerifiedCurrentRun = currentRuns.length === 1
  const runs = hasVerifiedCurrentRun
    ? observedRuns.filter((run) => run !== currentRuns[0])
    : observedRuns
  const tags =
    hasVerifiedCurrentRun && applicationTag.test(currentRef)
      ? observedTags.filter((tag) => tag !== currentRef)
      : observedTags
  if (tags.length === 0 && runs.length === 0) return { state: 'empty' }
  if (runs.some((run) => run.status !== 'completed')) {
    throw new Error('Deploy workflow history contains an incomplete run')
  }

  const published = []
  for (const run of runs) {
    const jobs = await fetchWorkflowRunJobs({
      repository,
      run,
      apiBase,
      token,
    })
    const identity = await publishedIdentity({
      repository,
      run,
      jobs,
      apiBase,
      token,
    })
    if (identity) published.push({ runNumber: run.run_number, identity })
  }
  published.sort((left, right) => right.runNumber - left.runNumber)
  if (published.length > 0) return published[0].identity
  throw new Error(
    'Deploy workflow history does not verify a successful application publication'
  )
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    writeReleaseOutput(
      await classifyApplicationPublication({
        repository: process.env.GITHUB_REPOSITORY,
      })
    )
  } catch (error) {
    const reason = error.message
    writeReleaseOutput({ state: 'ambiguous', reason })
    console.error(`Ambiguous application publication history: ${reason}`)
    process.exitCode = 1
  }
}
