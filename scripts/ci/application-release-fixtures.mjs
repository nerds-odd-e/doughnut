import { execFileSync } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

export function makeReleaseRepository(t) {
  const root = mkdtempSync(join(tmpdir(), 'application-release-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const origin = join(root, 'origin')
  const repository = join(root, 'checkout')
  const git = (...args) =>
    execFileSync('git', args, {
      cwd: origin,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    }).trim()
  execFileSync('git', ['init', '--initial-branch=main', origin], {
    stdio: 'ignore',
  })
  git('config', 'user.name', 'Release test')
  git('config', 'user.email', 'release@example.test')
  const commit = (message) => {
    git('commit', '--allow-empty', '-m', message)
    return git('rev-parse', 'HEAD')
  }
  const sha = commit('Selected release')
  const tag = (name = 'v1.2.3', annotated = false, target = sha) => {
    git('tag', ...(annotated ? ['-a', '-m', name] : []), name, target)
    return git('rev-parse', `refs/tags/${name}`)
  }
  const release = (name = 'v1.2.3') => {
    const ref = `refs/tags/${name}`
    return {
      tag: name,
      ref,
      refOid: git('rev-parse', ref),
      sha: git('rev-parse', `${ref}^{commit}`),
    }
  }
  const clone = () =>
    execFileSync(
      'git',
      ['clone', '--depth=1', `file://${origin}`, repository],
      { stdio: 'ignore' }
    )
  return { git, commit, tag, clone, release, sha, repository }
}
