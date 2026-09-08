import { spawn, spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'
import { runWorktreeRetire } from './worktree-retirement.mjs'

export function makeWritable() {
  let content = ''
  return {
    write(chunk) {
      content += chunk
    },
    content() {
      return content
    },
  }
}

/** Stub sessions and process inspection (isolates recorded-evidence tests). */
export const clearEvidenceDeps = {
  listDatabaseSessionsFn: async () => [],
  listProcessTableFn: async () => [],
  listJavaWorkingDirectoriesFn: async () => new Map(),
}

/** Empty sessions only; checkout-process inspection runs for real. */
export const clearSessionDeps = {
  listDatabaseSessionsFn: async () => [],
}

export async function runCheck(checkoutRoot, evidenceDeps = clearEvidenceDeps) {
  const out = makeWritable()
  const err = makeWritable()
  const code = await runWorktreeRetire({
    argv: ['--check'],
    checkoutRoot,
    out,
    err,
    evidenceDeps,
  })
  return { code, out: out.content(), err: err.content() }
}

function compileHoldClass(checkoutRoot) {
  const source = path.join(checkoutRoot, 'Hold.java')
  writeFileSync(
    source,
    `public class Hold {
  public static void main(String[] args) throws Exception {
    for (;;) Thread.sleep(1000);
  }
}
`
  )
  const compiled = spawnSync('javac', [source], {
    cwd: checkoutRoot,
    encoding: 'utf8',
  })
  if (compiled.status !== 0) {
    throw new Error(
      `javac Hold.java failed: ${compiled.stderr || compiled.stdout}`
    )
  }
}

/**
 * Spawn a supported-backend JVM stand-in under checkoutRoot, then exit the
 * parent so the JVM is reparented (PPID 1). Uses real java + cwd evidence.
 */
export async function spawnReparentedBackendJvm(t, checkoutRoot) {
  mkdirSync(checkoutRoot, { recursive: true })
  compileHoldClass(checkoutRoot)
  const pidFile = path.join(checkoutRoot, 'reparented-backend.pid')
  const launcher = path.join(checkoutRoot, 'reparent-backend-jvm.mjs')
  writeFileSync(
    launcher,
    `import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
const child = spawn(
  'java',
  ['-cp', '.', 'Hold', 'com.odde.donut.DonutApplication'],
  { cwd: ${JSON.stringify(checkoutRoot)}, stdio: 'ignore', detached: true }
)
child.unref()
writeFileSync(${JSON.stringify(pidFile)}, String(child.pid))
process.exit(0)
`
  )
  const parent = spawn(process.execPath, [launcher], {
    cwd: checkoutRoot,
    stdio: 'ignore',
  })
  await new Promise((resolve, reject) => {
    parent.on('error', reject)
    parent.on('exit', (code) => {
      if (code === 0) resolve()
      else reject(new Error(`reparent launcher exited ${code}`))
    })
  })

  const deadline = Date.now() + 5000
  while (!existsSync(pidFile)) {
    if (Date.now() > deadline) {
      throw new Error(`timed out waiting for ${pidFile}`)
    }
    await delay(20)
  }
  const pid = Number(readFileSync(pidFile, 'utf8').trim())
  if (!Number.isInteger(pid) || pid <= 0) {
    throw new Error(`invalid reparented pid in ${pidFile}`)
  }

  const ppidDeadline = Date.now() + 5000
  while (Date.now() < ppidDeadline) {
    const ppid = Number(
      spawnSync('ps', ['-o', 'ppid=', '-p', String(pid)], {
        encoding: 'utf8',
      }).stdout.trim()
    )
    if (ppid === 1) break
    await delay(20)
  }

  t.after(() => {
    try {
      process.kill(pid, 'SIGKILL')
    } catch {
      // already gone
    }
  })
  return pid
}
