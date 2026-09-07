import { spawnSync } from 'node:child_process'
import { cpSync, rmSync, statSync } from 'node:fs'
import path from 'node:path'
import { makeCheckout } from './backend-test-worktree-stand-in-fixtures.mjs'

function git(cwd, args) {
  const result = spawnSync('git', args, {
    cwd,
    encoding: 'utf8',
    env: {
      ...process.env,
      GIT_AUTHOR_NAME: 'Test',
      GIT_AUTHOR_EMAIL: 'test@example.com',
      GIT_COMMITTER_NAME: 'Test',
      GIT_COMMITTER_EMAIL: 'test@example.com',
    },
  })
  if (result.status !== 0) {
    throw new Error(
      `git ${args.join(' ')} failed: ${result.stderr || result.stdout}`
    )
  }
  return result
}

function relocateCheckout(checkout, newRoot) {
  const remap = (p) => path.join(newRoot, path.relative(checkout.root, p))
  return {
    root: newRoot,
    launcher: remap(checkout.launcher),
    javaHome: remap(checkout.javaHome),
    gradleInvocation: remap(checkout.gradleInvocation),
    gradleReached: remap(checkout.gradleReached),
    gradleRelease: remap(checkout.gradleRelease),
    binDir: remap(checkout.binDir),
    mysqlStandIn: remap(checkout.mysqlStandIn),
    mysqlInvocation: remap(checkout.mysqlInvocation),
    mysqlReached: remap(checkout.mysqlReached),
    mysqlRelease: remap(checkout.mysqlRelease),
  }
}

function resolvedGitPath(cwd, revParseArg) {
  const raw = git(cwd, ['rev-parse', revParseArg]).stdout.trim()
  return path.resolve(cwd, raw)
}

// Ordinary clone / main worktree: `.git` is a directory and git-dir equals
// git-common-dir. Isolation is not selected from topology alone.
export function makePrimaryCheckout(t, options) {
  const checkout = makeCheckout(t, options)
  git(checkout.root, ['init'])
  const gitDir = path.join(checkout.root, '.git')
  if (!statSync(gitDir).isDirectory()) {
    throw new Error(`expected primary .git directory at ${gitDir}`)
  }
  const resolvedGitDir = resolvedGitPath(checkout.root, '--git-dir')
  const resolvedCommonDir = resolvedGitPath(checkout.root, '--git-common-dir')
  if (resolvedGitDir !== resolvedCommonDir) {
    throw new Error(
      `expected primary git-dir === git-common-dir, got ${resolvedGitDir} vs ${resolvedCommonDir}`
    )
  }
  return checkout
}

// Primary git repo plus `git worktree add` linked checkout. Reuses
// makeCheckout files; overlays JAVA_HOME and mysql intercepts into the
// linked tree.
export function makeLinkedWorktreeCheckout(t, options) {
  const primary = makePrimaryCheckout(t, options)
  git(primary.root, ['add', 'gradlew', 'backend', 'gradle', 'scripts'])
  git(primary.root, ['commit', '-m', 'fixture'])
  const linkedRoot = `${primary.root}-linked`
  git(primary.root, ['worktree', 'add', '--detach', linkedRoot])
  t.after(() => rmSync(linkedRoot, { recursive: true, force: true }))
  const gitFile = path.join(linkedRoot, '.git')
  if (!statSync(gitFile).isFile()) {
    throw new Error(`expected linked worktree .git file at ${gitFile}`)
  }
  cpSync(primary.javaHome, path.join(linkedRoot, 'java-home'), {
    recursive: true,
  })
  cpSync(primary.binDir, path.join(linkedRoot, 'bin'), { recursive: true })
  return relocateCheckout(primary, linkedRoot)
}
