import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { existsSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

function sanitizedChildEnv(env) {
  const childEnv = { ...process.env }
  delete childEnv.SPRING_DATASOURCE_URL
  delete childEnv.DB_URL
  delete childEnv.SPRING_FLYWAY_URL
  delete childEnv.DONUT_WORKTREE_HANDOFF
  delete childEnv.FAKE_GRADLE_EXIT
  delete childEnv.FAKE_MYSQL_EXIT
  Object.assign(childEnv, env)
  return childEnv
}

// Puts the checkout's fake `mysql` stand-in ahead of the real one on PATH so
// the launcher's `mysql` invocation is intercepted, points JAVA_HOME at the
// recording `bin/java` (the real wrapper execs `$JAVA_HOME/bin/java` when
// JAVA_HOME is set), then applies the usual sanitization/overrides.
function launcherChildEnv(checkout, env) {
  return sanitizedChildEnv({
    PATH: `${checkout.binDir}${path.delimiter}${process.env.PATH ?? ''}`,
    JAVA_HOME: checkout.javaHome,
    ...env,
  })
}

export function runWrapper(
  checkout,
  { command, cwd, args = [], env = {} } = {}
) {
  return spawnSync(
    command ?? path.join(checkout.root, 'backend', 'gradlew'),
    args,
    {
      cwd: cwd ?? checkout.root,
      encoding: 'utf8',
      env: launcherChildEnv(checkout, env),
    }
  )
}

export function runLauncher(checkout, { env = {}, args = [] } = {}) {
  return runWrapper(checkout, { command: checkout.launcher, args, env })
}

export function outputOf(result) {
  return `${result.stdout}${result.stderr}`
}

async function waitForFile(
  filePath,
  { timeoutMs = 5000, intervalMs = 20 } = {}
) {
  const deadline = Date.now() + timeoutMs
  while (!existsSync(filePath)) {
    if (Date.now() >= deadline) {
      throw new Error(`Timed out waiting for ${filePath}`)
    }
    await delay(intervalMs)
  }
}

// Starts the launcher asynchronously against a foreground Gradle stand-in
// that reaches GRADLE_REACHED and then blocks until explicitly released.
// Returns a handle for observing that one active Gradle owner from outside.
export function runLauncherAsync(checkout, { env = {}, args = [] } = {}) {
  const child = spawn(checkout.launcher, args, {
    cwd: checkout.root,
    env: launcherChildEnv(checkout, { ...env, GRADLE_HOLD: '1' }),
  })
  // No input is ever sent. Unlike runLauncher's spawnSync (which closes an
  // unwritten stdin immediately), async spawn() leaves stdin open until
  // explicitly ended, so a descendant reading stdin (e.g. the mysql
  // stand-in's `[ ! -t 0 ]` check during provisioning) would otherwise block
  // forever waiting for EOF.
  child.stdin.end()

  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (chunk) => {
    stdout += chunk
  })
  child.stderr.on('data', (chunk) => {
    stderr += chunk
  })

  const exited = new Promise((resolve) => {
    child.on('close', (status) => {
      resolve({ status, stdout, stderr })
    })
  })

  return {
    waitForGradleReached: () => waitForFile(checkout.gradleReached),
    release: () => writeFileSync(checkout.gradleRelease, ''),
    stop: () => child.kill(),
    // Waits for the checkout's mysql stand-in to reach and hold
    // (MYSQL_HOLD), then releases it. Used to inject an external config
    // writer between MySQL provisioning succeeding and the launcher's own
    // exclusive config write.
    waitForMysqlReached: () => waitForFile(checkout.mysqlReached),
    releaseMysql: () => writeFileSync(checkout.mysqlRelease, ''),
    waitForExit: () => exited,
  }
}

export function assertRefusedBeforeGradle(checkout, result) {
  assert.equal(result.error, undefined, result.stderr)
  assert.notEqual(result.status, 0)
  assert.equal(existsSync(checkout.gradleInvocation), false)
  assert.doesNotMatch(outputOf(result), /GRADLE_REACHED/)
}

// Two already-started launchers: one must reach Gradle and keep holding; the
// other must exit. Does not release the Gradle hold.
export async function waitForOneOwnerAndOneRefusal(first, second) {
  const firstExit = first.waitForExit()
  const secondExit = second.waitForExit()
  await Promise.race([
    first.waitForGradleReached(),
    second.waitForGradleReached(),
  ])
  return Promise.race([
    firstExit.then((refused) => ({
      refused,
      owner: second,
      ownerExit: secondExit,
    })),
    secondExit.then((refused) => ({
      refused,
      owner: first,
      ownerExit: firstExit,
    })),
    delay(5000).then(() => {
      throw new Error(
        'Timed out waiting for the second launcher to refuse before Gradle'
      )
    }),
  ])
}
