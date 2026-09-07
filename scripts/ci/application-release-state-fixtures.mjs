import { spawn } from 'node:child_process'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import { makeReleaseRepository } from './application-release-fixtures.mjs'
import { githubRepository } from './application-release-bootstrap-fixtures.mjs'

const command = fileURLToPath(
  new URL('./application-release-state.mjs', import.meta.url)
)

export async function runStateInitialization(
  t,
  {
    tags = [],
    runs = [],
    jobs = {},
    logs = {},
    existingBody,
    readStatus = existingBody === undefined ? 404 : 200,
    createStatus = 200,
    currentRunId,
    currentRef,
    unavailable = false,
  } = {}
) {
  const fixture = makeReleaseRepository(t)
  for (const tag of tags) fixture.tag(tag)
  fixture.clone()
  const requests = []
  const uploads = []
  const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost')
    let body = ''
    for await (const chunk of request) body += chunk
    requests.push({
      method: request.method,
      url,
      authorization: request.headers.authorization,
    })
    if (url.pathname.startsWith('/storage/v1/')) {
      response.writeHead(readStatus, { 'Content-Type': 'application/json' })
      response.end(readStatus === 200 ? existingBody : '')
      return
    }
    if (url.pathname.startsWith('/upload/storage/v1/')) {
      uploads.push(body)
      response.writeHead(createStatus, { 'Content-Type': 'application/json' })
      response.end(createStatus < 300 ? '{}' : '')
      return
    }
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
  const apiBase = `http://127.0.0.1:${server.address().port}`
  if (unavailable) await new Promise((resolve) => server.close(resolve))
  const child = spawn(process.execPath, [command], {
    cwd: fixture.repository,
    env: {
      ...process.env,
      GCS_API_URL: apiBase,
      GCS_BUCKET: 'private-backend',
      GCP_ACCESS_TOKEN: 'gcs-token',
      GITHUB_API_URL: apiBase,
      GITHUB_REPOSITORY: githubRepository,
      GITHUB_TOKEN: 'github-token',
      ...(currentRunId ? { GITHUB_RUN_ID: currentRunId } : {}),
      ...(currentRef ? { GITHUB_REF: currentRef } : {}),
    },
  })
  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (data) => (stdout += data))
  child.stderr.on('data', (data) => (stderr += data))
  const status = await new Promise((resolve, reject) => {
    child.on('error', reject)
    child.on('close', resolve)
  })
  return { status, stdout, stderr, requests, uploads }
}
