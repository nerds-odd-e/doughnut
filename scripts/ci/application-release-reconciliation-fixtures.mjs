import { spawn } from 'node:child_process'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const command = fileURLToPath(
  new URL('./application-release-reconciliation.mjs', import.meta.url)
)

export async function runReconciliationCommand(
  t,
  fixture,
  wakeupRef,
  runsBySha,
  existingRecord = { outcome: 'initialized-empty' }
) {
  const root = mkdtempSync(join(tmpdir(), 'release-reconciliation-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const output = join(root, 'output')
  const requests = []
  const uploads = []
  const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost')
    let body = ''
    for await (const chunk of request) body += chunk
    requests.push({ method: request.method, url })
    if (url.pathname.startsWith('/storage/v1/')) {
      response.writeHead(200, { 'Content-Type': 'application/json' })
      response.end(JSON.stringify(existingRecord))
      return
    }
    if (url.pathname.startsWith('/upload/storage/v1/')) {
      uploads.push(JSON.parse(body))
      response.writeHead(200, { 'Content-Type': 'application/json' })
      response.end('{}')
      return
    }
    const sha = url.searchParams.get('head_sha')
    const runs = runsBySha[sha] ?? []
    response.writeHead(200, { 'Content-Type': 'application/json' })
    response.end(
      JSON.stringify({ total_count: runs.length, workflow_runs: runs })
    )
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  t.after(() => server.close())

  const child = spawn(process.execPath, [command], {
    cwd: fixture.repository,
    env: {
      ...process.env,
      GCS_API_URL: `http://127.0.0.1:${server.address().port}`,
      GCS_BUCKET: 'private-backend',
      GCP_ACCESS_TOKEN: 'gcs-token',
      GITHUB_API_URL: `http://127.0.0.1:${server.address().port}`,
      GITHUB_REPOSITORY: 'nerds-odd-e/doughnut',
      GITHUB_REF: wakeupRef,
      GITHUB_TOKEN: '',
      GITHUB_OUTPUT: output,
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
  let githubOutput = ''
  try {
    githubOutput = readFileSync(output, 'utf8')
  } catch (error) {
    if (error.code !== 'ENOENT') throw error
  }
  return { status, stdout, stderr, requests, uploads, output: githubOutput }
}
