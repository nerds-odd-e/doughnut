import { spawn } from 'node:child_process'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

export const selectedSha = 'a'.repeat(40)
export const repository = 'nerds-odd-e/doughnut'

export function ciRun(overrides = {}) {
  return {
    id: 42,
    run_number: 12,
    run_attempt: 1,
    repository: { full_name: repository },
    head_repository: { full_name: repository },
    path: '.github/workflows/ci.yml',
    head_branch: 'main',
    event: 'push',
    head_sha: selectedSha,
    status: 'completed',
    conclusion: 'success',
    ...overrides,
  }
}

export async function runCiCommand(t, pages, args = ['--once']) {
  const root = mkdtempSync(join(tmpdir(), 'release-ci-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const output = join(root, 'output')
  const requests = []
  const server = createServer((request, response) => {
    const url = new URL(request.url, 'http://localhost')
    requests.push(url)
    const page = pages[Number(url.searchParams.get('page')) - 1]
    response.writeHead(page.status || 200, {
      'Content-Type': 'application/json',
    })
    response.end(JSON.stringify(page))
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  t.after(() => server.close())
  const child = spawn(
    process.execPath,
    [
      fileURLToPath(new URL('./application-release-ci.mjs', import.meta.url)),
      ...args,
    ],
    {
      env: {
        ...process.env,
        GITHUB_API_URL: `http://127.0.0.1:${server.address().port}`,
        GITHUB_REPOSITORY: repository,
        RELEASE_SHA: selectedSha,
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
  return { status, stdout, stderr, requests, output: githubOutput }
}
