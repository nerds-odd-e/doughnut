import { spawnSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'

export function lockPaths(checkout) {
  const dir = path.join(checkout.root, '.worktree.local.lock')
  return { dir, ownerFile: path.join(dir, 'owner.pid') }
}

function makeStaleOwnerPid() {
  const result = spawnSync(process.execPath, ['-e', ''])
  return result.pid
}

// Creates the checkout's lock directory with a stale (dead-PID) owner
// record, as if a previous launcher owned it and then exited without
// cleanup.
export function writeStaleOwnerLock(checkout) {
  const { dir, ownerFile } = lockPaths(checkout)
  mkdirSync(dir)
  const pid = makeStaleOwnerPid()
  writeFileSync(ownerFile, String(pid))
  return pid
}
