import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync, writeFileSync } from 'node:fs'
import { request } from 'node:http'
import { join } from 'node:path'
import { performance } from 'node:perf_hooks'
import { resolveSutCheckoutTarget } from '../../scripts/sut-isolated-target.mjs'

export async function publishNotebookProfileHttp(
  repoRoot: string,
  directory: string,
  checkoutDir: string,
  configDir: string
) {
  const git = (...args: string[]) =>
    execFileSync('git', ['-C', checkoutDir, ...args], {
      encoding: 'utf8',
    }).trim()
  const { isolated, target } = resolveSutCheckoutTarget({
    checkoutRoot: repoRoot,
  })
  assert.ok(isolated, 'HTTP profiling requires the owned isolated SUT')
  const origin = git('config', '--local', '--get', 'donut.api-origin')
  assert.equal(origin, `http://127.0.0.1:${target.lbListenPort}`)
  const notebookId = git('config', '--local', '--get', 'donut.notebook-id')
  const expectedHead = git('rev-parse', 'HEAD^')
  const bundlePath = join(directory, 'proposal.bundle')
  git('bundle', 'create', bundlePath, 'main')
  const body = readFileSync(bundlePath)
  const { token } = JSON.parse(
    readFileSync(join(configDir, 'access-tokens.json'), 'utf8')
  )
  const url = `${origin}/api/notebooks/${notebookId}/git-bundle?expectedHead=${expectedHead}`
  const timeoutMs = Number(
    process.env.PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS ?? 3_600_000
  )
  assert.ok(
    Number.isInteger(timeoutMs) && timeoutMs > 0 && timeoutMs <= 2_147_483_647,
    'Benchmark HTTP deadline must be a positive supported timer duration'
  )
  const started = new Date().toISOString()
  const start = performance.now()
  let outcome: Record<string, unknown> = { outcome: 'incomplete' }
  try {
    const response = await new Promise<{ status: number; body: string }>(
      (resolve, reject) => {
        const req = request(
          url,
          {
            method: 'POST',
            headers: {
              Authorization: `Bearer ${token}`,
              'Content-Type': 'application/x-git-bundle',
              'Content-Length': body.length,
            },
          },
          (res) => {
            const chunks: Buffer[] = []
            res.on('data', (chunk) => chunks.push(chunk))
            res.on('error', reject)
            res.on('end', () =>
              resolve({
                status: res.statusCode!,
                body: Buffer.concat(chunks).toString('utf8'),
              })
            )
          }
        )
        const timer = setTimeout(
          () =>
            req.destroy(
              new Error('Publication benchmark HTTP deadline exceeded')
            ),
          timeoutMs
        )
        req.on('close', () => clearTimeout(timer))
        req.on('error', reject)
        req.end(body)
      }
    )
    outcome = { outcome: 'response-received', ...response }
    return response
  } catch (error) {
    outcome = { outcome: 'incomplete', error: String(error) }
    throw error
  } finally {
    const elapsedMs = performance.now() - start
    writeFileSync(
      join(directory, 'timing.json'),
      JSON.stringify(
        {
          boundary:
            'HTTP request through complete response body; bundle preparation excluded; includes transport and server commit or rollback',
          started,
          stopped: new Date().toISOString(),
          elapsedMs,
          timeoutMs,
          method: 'POST',
          url,
          bundleBytes: body.length,
          ...outcome,
        },
        null,
        2
      )
    )
  }
}
