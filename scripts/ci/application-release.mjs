import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { writeReleaseOutput } from './application-release-output.mjs'

const git = (repository, ...args) =>
  execFileSync('git', args, { cwd: repository, encoding: 'utf8' }).trim()

function assertReleaseRef(repository, release) {
  const current = git(repository, 'ls-remote', 'origin', release.ref)
  if (current.split('\t')[0] !== release.refOid) {
    throw new Error(`Release ref changed or disappeared: ${release.tag}`)
  }
}

function runApplicationRelease({ event, repository = process.cwd() }) {
  if (
    !/^refs\/tags\/v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/.test(event.ref)
  ) {
    throw new Error(
      'Application releases require a stable vMAJOR.MINOR.PATCH tag'
    )
  }
  if (event.deleted || event.forced) {
    throw new Error('Deleted or force-updated release tags are not accepted')
  }
  if (!/^[0-9a-f]{40}$/.test(event.after)) {
    throw new Error('Release event must identify its exact Git object')
  }

  const release = {
    tag: event.ref.slice('refs/tags/'.length),
    ref: event.ref,
    refOid: event.after,
  }
  assertReleaseRef(repository, release)
  const shallow =
    git(repository, 'rev-parse', '--is-shallow-repository') === 'true'
  git(
    repository,
    'fetch',
    ...(shallow ? ['--unshallow'] : []),
    'origin',
    '+refs/heads/main:refs/remotes/origin/main',
    `+${release.ref}:${release.ref}`
  )
  if (git(repository, 'rev-parse', release.ref) !== release.refOid) {
    throw new Error(`Release ref changed during fetch: ${release.tag}`)
  }
  release.sha = git(repository, 'rev-parse', `${release.refOid}^{commit}`)
  try {
    git(repository, 'merge-base', '--is-ancestor', release.sha, 'origin/main')
  } catch (error) {
    if (error.status !== 1) throw error
    throw new Error(`Release commit is not on main: ${release.sha}`)
  }
  return release
}

const release = runApplicationRelease({
  event: JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8')),
})
writeReleaseOutput(release)
