import { execFileSync } from 'node:child_process'
import { parse } from 'yaml'
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
    throw Object.assign(
      new Error(
        `CI ${latest.id} attempt ${latest.run_attempt} for ${sha} finished with ${latest.conclusion}`
      ),
      { ci: { state: 'failed', ...identity } }
    )
  }
  return { state: 'ready', ...identity }
}

// Release identity stays at the tag; CI identity identifies the artifact source.
export async function queryReleaseCi({ repositoryRoot, ...options }) {
  const identifyArtifactSource = (ci, ciSha) =>
    ci.runId ? { ...ci, sha: options.sha, ciSha } : ci
  const queryArtifactSource = async (ciSha) => {
    try {
      return identifyArtifactSource(
        await querySelectedCi({ ...options, sha: ciSha }),
        ciSha
      )
    } catch (error) {
      if (error.ci) error.ci = identifyArtifactSource(error.ci, ciSha)
      throw error
    }
  }
  const exact = await queryArtifactSource(options.sha)
  if (exact.runId) return exact
  const git = (...args) =>
    execFileSync('git', args, { cwd: repositoryRoot, encoding: 'utf8' })
  const workflowPath = '.github/workflows/ci.yml'
  if (!git('ls-tree', options.sha, '--', workflowPath).trim()) return exact
  const policy = parse(git('show', `${options.sha}:${workflowPath}`)).on?.push
  const ignored = policy?.['paths-ignore']
  if (!ignored) return exact
  if (
    !Array.isArray(ignored) ||
    ignored.some(
      (pattern) =>
        typeof pattern !== 'string' || !/^[.\w/-]+\/\*\*$/.test(pattern)
    )
  ) {
    throw new Error(
      'Release CI reuse supports literal directory paths-ignore patterns ending in /**'
    )
  }
  const directories = ignored.map((pattern) => pattern.slice(0, -2))
  const ancestors = git('rev-list', '--first-parent', options.sha)
    .trim()
    .split('\n')
    .slice(1)
  for (const sha of ancestors) {
    const changes = git(
      'diff',
      '--name-only',
      '-z',
      '--no-renames',
      sha,
      options.sha
    )
      .split('\0')
      .filter(Boolean)
    if (
      changes.some(
        (path) => !directories.some((directory) => path.startsWith(directory))
      )
    )
      break
    const ci = await queryArtifactSource(sha)
    if (ci.runId) return ci
  }
  return exact
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const result = await querySelectedCi({
      repository: process.env.GITHUB_REPOSITORY,
      sha: process.env.RELEASE_SHA,
    })
    writeReleaseOutput(result)
  } catch (error) {
    if (error.ci) writeReleaseOutput(error.ci)
    throw error
  }
}
