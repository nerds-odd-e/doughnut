import { spawn } from 'node:child_process'
import { once } from 'node:events'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  renameSync,
  watch,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { basename, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { watchCi } from './watch-ci.mjs'

export const checkoutRoot = fileURLToPath(
  new URL('../../../../', import.meta.url)
)
export const mailboxRoot = join(
  tmpdir(),
  `donut-ci-${process.getuid?.() ?? 'user'}`
)
export const receiptPrefix = 'CI_OBSERVER '

export function readMailbox(
  directory,
  root = checkoutRoot,
  storage = mailboxRoot
) {
  if (
    resolve(directory, '..') !== resolve(storage) ||
    !/^watch-/.test(basename(directory))
  ) {
    throw new Error('CI mailbox is outside the observer directory')
  }
  const request = JSON.parse(
    readFileSync(join(directory, 'request.json'), 'utf8')
  )
  if (resolve(request.root) !== resolve(root))
    throw new Error('CI mailbox belongs to another checkout')
  return request
}

export function createMailbox(
  request,
  { root = checkoutRoot, storage = mailboxRoot } = {}
) {
  mkdirSync(storage, { recursive: true, mode: 0o700 })
  const directory = mkdtempSync(join(storage, 'watch-'))
  writeFileSync(
    join(directory, 'request.json'),
    JSON.stringify({ ...request, root }),
    { mode: 0o600 }
  )
  return directory
}

function finish(directory, result) {
  writeFileSync(join(directory, 'result.tmp'), JSON.stringify(result), {
    mode: 0o600,
  })
  renameSync(join(directory, 'result.tmp'), join(directory, 'result.json'))
}

export async function runMailboxWorker(
  directory,
  { observe = watchCi, root = checkoutRoot, storage = mailboxRoot } = {}
) {
  const request = readMailbox(directory, root, storage)
  const abort = new AbortController()
  const stop = () => {
    if (existsSync(join(directory, 'stop'))) abort.abort()
  }
  const subscription = watch(directory, stop)
  stop()
  let result
  try {
    const event = await observe({ ...request, signal: abort.signal })
    result = abort.signal.aborted
      ? { status: 'stopped' }
      : { status: 'finished', event }
  } catch (error) {
    result = abort.signal.aborted
      ? { status: 'stopped' }
      : {
          status: 'finished',
          event: {
            type: 'CI_MONITOR_UNAVAILABLE',
            repo: request.repo,
            sha: request.sha,
            reason: String(error).slice(0, 600),
          },
        }
  } finally {
    subscription.close()
  }
  finish(directory, result)
}

export function stopMailbox(directory, options = {}) {
  readMailbox(directory, options.root, options.storage)
  writeFileSync(join(directory, 'stop'), '', { mode: 0o600 })
}

async function startMailbox(request) {
  if (
    !(
      /^[\w.-]+\/[\w.-]+$/.test(request.repo ?? '') &&
      /^[a-f0-9]{40}$/.test(request.sha ?? '')
    ) ||
    request.branch !== 'main'
  ) {
    throw new Error('Expected OWNER/REPO FULL_PUSHED_SHA main')
  }
  const directory = createMailbox(request)
  const child = spawn(
    process.execPath,
    [fileURLToPath(import.meta.url), 'worker', directory],
    {
      cwd: checkoutRoot,
      detached: true,
      stdio: 'ignore',
    }
  )
  await once(child, 'spawn')
  child.unref()
  return directory
}

export function probeMailbox(options = {}) {
  const directory = createMailbox({ probe: true }, options)
  finish(directory, { status: 'finished', event: { type: 'CI_MONITOR_READY' } })
  return directory
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  const [command, ...args] = process.argv.slice(2)
  if (command === 'worker') {
    await runMailboxWorker(args[0])
  } else if (command === 'start') {
    const [repo, sha, branch] = args
    const directory = await startMailbox({ repo, sha, branch })
    process.stdout.write(`${receiptPrefix}${JSON.stringify({ directory })}\n`)
  } else if (command === 'probe') {
    process.stdout.write(
      `${receiptPrefix}${JSON.stringify({ directory: probeMailbox() })}\n`
    )
  } else if (command === 'stop') {
    stopMailbox(args[0])
  } else {
    throw new Error(
      'Usage: ci-mailbox.mjs probe | start OWNER/REPO SHA main | stop DIRECTORY'
    )
  }
}
