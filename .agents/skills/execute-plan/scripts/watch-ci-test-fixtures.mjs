import { existsSync, mkdirSync, watch, writeFileSync } from 'node:fs'
import { join } from 'node:path'

export const sha = 'a'.repeat(40)

export const run = (overrides = {}) => ({
  databaseId: 42,
  attempt: 1,
  headSha: sha,
  headBranch: 'main',
  workflowName: 'donut CI',
  event: 'push',
  status: 'completed',
  conclusion: 'success',
  url: 'https://github.com/example/donut/actions/runs/42',
  ...overrides,
})

export const scriptedGithub =
  (responses, calls = []) =>
  async (args) => {
    calls.push(args)
    if (!responses.length) throw new Error('unexpected GitHub request')
    const response = responses.shift()
    if (response instanceof Error) throw response
    return response
  }

export async function waitForFile(path, timeoutMs = 5000) {
  if (existsSync(path)) return
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      subscription.close()
      reject(new Error(`Missing ${path}`))
    }, timeoutMs)
    const check = () => {
      if (!existsSync(path)) return
      clearTimeout(timer)
      subscription.close()
      resolve()
    }
    const subscription = watch(join(path, '..'), check)
    check()
  })
}

export function writeBlockingGithubListCommand(bin) {
  mkdirSync(bin, { recursive: true })
  writeFileSync(
    join(bin, 'gh'),
    `#!${process.execPath}
const fs = require('node:fs');
const path = require('node:path');
const root = process.env.CI_TEST_ROOT;
fs.writeFileSync(path.join(root, 'github-request-started'), '');
process.on('SIGTERM', () => {
  fs.writeFileSync(path.join(root, 'github-request-stopped'), '');
  process.exit(0);
});
setInterval(() => {}, 1000);
`,
    { mode: 0o700 }
  )
}
