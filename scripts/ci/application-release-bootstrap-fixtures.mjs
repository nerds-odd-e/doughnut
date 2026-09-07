import { spawn } from 'node:child_process'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { makeReleaseRepository } from './application-release-fixtures.mjs'

export const githubRepository = 'nerds-odd-e/doughnut'

export function deployRun(overrides = {}) {
  return {
    id: 91,
    run_number: 17,
    run_attempt: 2,
    repository: { full_name: githubRepository },
    head_repository: { full_name: githubRepository },
    path: '.github/workflows/deploy.yml',
    event: 'push',
    status: 'completed',
    conclusion: 'success',
    ...overrides,
  }
}

export function releaseJobs(publicationConclusion = 'success') {
  return [
    {
      id: 501,
      run_attempt: 2,
      name: 'Admit selected application release',
      conclusion: 'success',
    },
    {
      id: 502,
      run_attempt: 2,
      name: 'GCP deploy (GCS + MIG + health probe)',
      conclusion: publicationConclusion,
      steps: [
        {
          name: 'Publish application to GCS and MIG',
          conclusion: publicationConclusion,
        },
      ],
    },
  ]
}

export async function runBootstrapCommand(
  t,
  { tags = [], runs = [], jobs = {}, logs = {} }
) {
  const fixture = makeReleaseRepository(t)
  for (const tag of tags) fixture.tag(tag)
  fixture.clone()
  const requests = []
  const server = createServer((request, response) => {
    const url = new URL(request.url, 'http://localhost')
    requests.push(url)
    let payload
    if (url.pathname.endsWith('/actions/workflows/deploy.yml/runs')) {
      payload = { total_count: runs.length, workflow_runs: runs }
    } else if (url.pathname.endsWith('/jobs')) {
      const runId = url.pathname.split('/')[6]
      const selected = jobs[runId] || []
      payload = { total_count: selected.length, jobs: selected }
    } else if (url.pathname.endsWith('/logs')) {
      const jobId = url.pathname.split('/')[6]
      if (!(jobId in logs)) {
        response.writeHead(404)
        response.end()
        return
      }
      response.writeHead(200, { 'Content-Type': 'text/plain' })
      response.end(logs[jobId])
      return
    } else {
      response.writeHead(404)
      response.end()
      return
    }
    response.writeHead(200, { 'Content-Type': 'application/json' })
    response.end(JSON.stringify(payload))
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  t.after(() => server.close())
  const output = join(
    mkdtempSync(join(tmpdir(), 'release-bootstrap-')),
    'output'
  )
  t.after(() => rmSync(output, { recursive: true, force: true }))
  const child = spawn(
    process.execPath,
    [
      fileURLToPath(
        new URL('./application-release-bootstrap.mjs', import.meta.url)
      ),
    ],
    {
      cwd: fixture.repository,
      env: {
        ...process.env,
        GITHUB_API_URL: `http://127.0.0.1:${server.address().port}`,
        GITHUB_REPOSITORY: githubRepository,
        GITHUB_TOKEN: '',
        GITHUB_OUTPUT: output,
      },
    }
  )
  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (data) => (stdout += data))
  child.stderr.on('data', (data) => (stderr += data))
  const status = await new Promise((resolve, reject) => {
    child.on('error', reject)
    child.on('close', resolve)
  })
  let githubOutput = ''
  try {
    githubOutput = readFileSync(output, 'utf8')
  } catch (error) {
    if (error.code !== 'ENOENT') throw error
  }
  return { status, stdout, stderr, requests, githubOutput, fixture }
}
