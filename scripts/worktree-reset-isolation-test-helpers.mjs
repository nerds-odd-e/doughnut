import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'

export async function withWorktreeResetIsolationBarrierDir(fn) {
  const dir = await mkdtemp(path.join(tmpdir(), 'worktree-reset-isolation-'))
  try {
    return await fn(dir)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
}
