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
      state: 'waiting',
      tag: 'v1.3.10',
      ref: 'refs/tags/v1.3.10',
      refOid: higherRefOid,
      sha: higherSha,
      runId: 43,
      runAttempt: 1,
    })
    assert.equal(
      result.output,
      `state=waiting\ntag=v1.3.10\nref=refs/tags/v1.3.10\nrefOid=${higherRefOid}\nsha=${higherSha}\nrunId=43\nrunAttempt=1\n`
    )
    assert.deepEqual(
      result.requests.map((request) => request.searchParams.get('head_sha')),
      [higherSha]
    )
  }
})

test('tag-first reconciliation returns waiting and a later CI wakeup selects the same release', async (t) => {
  const fixture = makeReleaseRepository(t)
  const refOid = fixture.tag('v1.2.3', true)
  fixture.clone()

  const waiting = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
    [fixture.sha]: [],
  })
  assert.equal(waiting.status, 0, waiting.stderr)
  assert.equal(JSON.parse(waiting.stdout).state, 'waiting')

  const ready = await reconcile(t, fixture, 'refs/heads/main', {
    [fixture.sha]: [ciRun({ head_sha: fixture.sha })],
  })
  assert.equal(ready.status, 0, ready.stderr)
  assert.deepEqual(JSON.parse(ready.stdout), {
    state: 'ready',
    tag: 'v1.2.3',
    ref: 'refs/tags/v1.2.3',
    refOid,
    sha: fixture.sha,
    runId: 42,
    runAttempt: 1,
  })
})

test('reconciliation ignores non-stable application tags', async (t) => {
  const fixture = makeReleaseRepository(t)
  for (const tag of [
    'v1.2',
    'v1.2.3-rc.1',
    'v1.2.3+build',
    'v01.2.3',
    'v1.02.3',
    'v1.2.03',
  ]) {
    fixture.tag(tag)
  }
  fixture.clone()

  const result = await reconcile(t, fixture, 'refs/heads/main', {})

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'none' })
  assert.deepEqual(result.requests, [])
})

test('reconciliation ignores a release commit outside main', async (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.git('checkout', '-b', 'feature')
  const offMainSha = fixture.commit('Unmerged release')
  fixture.tag('v1.2.3', false, offMainSha)
  fixture.git('checkout', 'main')
  fixture.clone()

  const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {})

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'none' })
  assert.deepEqual(result.requests, [])
})

test('failed CI blocks the current highest release without failing or selecting an older tag', async (t) => {
  const fixture = makeReleaseRepository(t)
  const lowerSha = fixture.sha
  fixture.tag('v1.3.9')
  const higherSha = fixture.commit('Higher blocked release')
  fixture.tag('v1.3.10', false, higherSha)
  fixture.clone()

  const result = await reconcile(t, fixture, 'refs/heads/main', {
    [lowerSha]: [ciRun({ head_sha: lowerSha })],
    [higherSha]: [
      ciRun({
        id: 43,
        head_sha: higherSha,
        conclusion: 'failure',
      }),
    ],
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), {
    state: 'blocked',
    tag: 'v1.3.10',
    ref: 'refs/tags/v1.3.10',
    refOid: fixture.git('rev-parse', 'refs/tags/v1.3.10'),
    sha: higherSha,
    runId: 43,
    runAttempt: 1,
    diagnostic: `CI 43 attempt 1 for ${higherSha} finished with failure`,
  })
  assert.deepEqual(
    result.requests.map((request) => request.searchParams.get('head_sha')),
    [higherSha]
  )
})
