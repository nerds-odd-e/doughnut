import { execFileSync, spawnSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const command = fileURLToPath(
  new URL('./application-release.mjs', import.meta.url)
)

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
  const run = (event) => {
    const eventPath = join(root, 'event.json')
    const outputPath = join(root, 'output')
    writeFileSync(eventPath, JSON.stringify(event))
    writeFileSync(outputPath, '')
    const result = spawnSync(process.execPath, [command], {
      cwd: repository,
      encoding: 'utf8',
      env: {
        ...process.env,
        GITHUB_EVENT_PATH: eventPath,
        GITHUB_OUTPUT: outputPath,
      },
    })
    return { ...result, output: readFileSync(outputPath, 'utf8') }
  }
  const clone = () =>
    execFileSync(
      'git',
      ['clone', '--depth=1', `file://${origin}`, repository],
      { stdio: 'ignore' }
    )
  return { git, commit, tag, clone, run, sha, repository }
}
