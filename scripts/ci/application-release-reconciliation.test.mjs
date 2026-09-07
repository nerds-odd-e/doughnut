import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { ciRun } from './application-release-ci-fixtures.mjs'
import { makeReleaseRepository } from './application-release-fixtures.mjs'

const command = fileURLToPath(
  new URL('./application-release-reconciliation.mjs', import.meta.url)
)

async function reconcile(t, fixture, wakeupRef, runsBySha) {
  const root = mkdtempSync(join(tmpdir(), 'release-reconciliation-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const output = join(root, 'output')
  const requests = []
  const server = createServer((request, response) => {
    const url = new URL(request.url, 'http://localhost')
    requests.push(url)
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
  return {
    status,
    stdout,
    stderr,
    requests,
    output: githubOutput,
  }
}

test('reconciliation keeps the highest numeric pending version across reversed wakeups', async (t) => {
  const fixture = makeReleaseRepository(t)
  const lowerSha = fixture.sha
  fixture.tag('v1.3.9')
  const higherSha = fixture.commit('Higher pending release')
  const higherRefOid = fixture.tag('v1.3.10', false, higherSha)
  fixture.clone()
  const runsBySha = {
    [lowerSha]: [ciRun({ head_sha: lowerSha })],
    [higherSha]: [
      ciRun({
        id: 43,
        head_sha: higherSha,
        status: 'in_progress',
        conclusion: null,
      }),
    ],
  }

  for (const wakeupRef of [
    'refs/tags/v1.3.10',
    'refs/tags/v1.3.9',
    'refs/heads/main',
  ]) {
    const result = await reconcile(t, fixture, wakeupRef, runsBySha)
    assert.equal(result.status, 0, result.stderr)
    assert.deepEqual(JSON.parse(result.stdout), {
      state: 'pending',
      tag: 'v1.3.10',
      ref: 'refs/tags/v1.3.10',
      refOid: higherRefOid,
      sha: higherSha,
      runId: 43,
      runAttempt: 1,
    })
    assert.equal(
      result.output,
      `state=pending\ntag=v1.3.10\nref=refs/tags/v1.3.10\nrefOid=${higherRefOid}\nsha=${higherSha}\nrunId=43\nrunAttempt=1\n`
    )
    assert.deepEqual(
      result.requests.map((request) => request.searchParams.get('head_sha')),
      [higherSha]
    )
  }
})
