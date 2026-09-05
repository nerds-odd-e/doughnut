import { execFile, spawn } from 'node:child_process'
import {
  mkdirSync,
  mkdtempSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'

export const exec = promisify(execFile)
export const launcher = fileURLToPath(
  new URL('./ci-mailbox.mjs', import.meta.url)
)
const hook = fileURLToPath(new URL('./ci-host-hook.mjs', import.meta.url))
export const sha = 'a'.repeat(40)

export async function setupProcessMailbox(
  t,
  startArguments = ['--execution', 'owner/repo', 'main', '60000']
) {
  const directory = mkdtempSync(join(tmpdir(), 'ci-process-test-'))
  const bin = join(directory, 'bin')
  mkdirSync(bin)
  writeFileSync(
    join(bin, 'gh'),
    `#!${process.execPath}
const fs = require('node:fs');
const path = require('node:path');
const root = process.env.CI_TEST_ROOT;
const release = path.join(root, 'release');
if (process.argv[3] === 'list') {
  fs.writeFileSync(path.join(root, 'started'), '');
  fs.writeFileSync(path.join(root, 'worker-pid'), String(process.ppid));
  process.on('SIGTERM', () => {
    fs.writeFileSync(path.join(root, 'request-stopped'), '');
    process.exit(0);
  });
  const output = () => {
    if (!fs.existsSync(release)) return false;
    process.stdout.write(fs.readFileSync(release));
    fs.writeFileSync(path.join(root, 'observed'), '');
    return true;
  };
  if (!output()) {
    const watcher = fs.watch(root, () => { if (output()) watcher.close(); });
  }
} else { process.stdout.write(JSON.stringify({jobs: []})); }
`,
    { mode: 0o700 }
  )
  const env = {
    ...process.env,
    DONUT_CI_MAILBOX_ROOT: directory,
    TMPDIR: directory,
    CI_TEST_ROOT: directory,
    PATH: `${bin}:${process.env.PATH}`,
  }
  const { stdout } = await exec(
    process.execPath,
    [launcher, 'start', ...startArguments],
    { env, timeout: 5000 }
  )
  const mailbox = JSON.parse(stdout.slice('CI_OBSERVER '.length)).directory
  t.after(async () => {
    await exec(process.execPath, [launcher, 'stop', mailbox], { env })
    rmSync(directory, { recursive: true, force: true })
  })
  const deliver = async (host, receipt = '') => {
    const input = JSON.stringify({
      session_id: 'process-test',
      conversation_id: 'process-test',
      generation_id: 'coordinator-turn',
      transcript_path: '/test/coordinator.jsonl',
      hook_event_name: host === 'cursor' ? 'postToolUse' : 'PostToolUse',
      tool_name: host === 'cursor' ? 'Shell' : 'Bash',
      tool_output: JSON.stringify({ stdout: receipt }),
      tool_response: { stdout: receipt },
    })
    const result = exec(process.execPath, [hook, host], { env })
    result.child.stdin.end(input)
    return JSON.parse((await result).stdout)
  }
  return { directory, mailbox, stdout, deliver, env }
}

export function releaseRun(directory, overrides = {}) {
  const run = {
    databaseId: 42,
    attempt: 1,
    headSha: sha,
    headBranch: 'main',
    workflowName: 'donut CI',
    event: 'push',
    status: 'completed',
    conclusion: 'failure',
    ...overrides,
  }
  writeFileSync(join(directory, 'release.tmp'), JSON.stringify([run]))
  renameSync(join(directory, 'release.tmp'), join(directory, 'release'))
}

export async function spawnIdleNode(t, trailingArguments = []) {
  const child = spawn(
    process.execPath,
    ['-e', 'setInterval(() => {}, 1000)', ...trailingArguments],
    { stdio: 'ignore' }
  )
  await new Promise((resolve, reject) => {
    child.once('spawn', resolve)
    child.once('error', reject)
  })
  t.after(() => {
    try {
      child.kill('SIGKILL')
    } catch (error) {
      if (error.code !== 'ESRCH') throw error
    }
  })
  return child
}
