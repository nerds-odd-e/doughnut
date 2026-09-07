import { execFileSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'
import { querySelectedCi } from './application-release-ci.mjs'
import { writeReleaseOutput } from './application-release-output.mjs'
import {
  applicationTagFromRef,
  compareApplicationVersionsDescending,
} from './application-release-version.mjs'

const git = (repository, ...args) =>
  execFileSync('git', args, { cwd: repository, encoding: 'utf8' }).trim()

function remoteApplicationTags(repository) {
  const tags = new Map()
  const output = git(
    repository,
    'ls-remote',
    '--tags',
    'origin',
    'refs/tags/v*'
  )
  for (const line of output.split('\n').filter(Boolean)) {
    const [oid, remoteRef] = line.split('\t')
    const peeled = remoteRef.endsWith('^{}')
    const ref = peeled ? remoteRef.slice(0, -3) : remoteRef
    const tag = applicationTagFromRef(ref)
    if (!tag) continue
    const release = tags.get(ref) ?? { tag, ref }
    if (peeled) release.remoteSha = oid
    else release.refOid = oid
    tags.set(ref, release)
  }
  return [...tags.values()]
    .filter((release) => release.refOid)
    .sort((left, right) =>
      compareApplicationVersionsDescending(left.tag, right.tag)
    )
}

function releaseOnMain(repository, release) {
  git(repository, 'fetch', '--no-tags', 'origin', release.ref)
  if (git(repository, 'rev-parse', 'FETCH_HEAD') !== release.refOid) {
    throw new Error(`Release ref changed during reconciliation: ${release.tag}`)
  }
  const sha = git(repository, 'rev-parse', `${release.refOid}^{commit}`)
  if (release.remoteSha && release.remoteSha !== sha) {
    throw new Error(
      `Release commit changed during reconciliation: ${release.tag}`
    )
  }
  try {
    git(repository, 'merge-base', '--is-ancestor', sha, 'origin/main')
  } catch (error) {
    if (error.status === 1) return
    throw error
  }
  return {
    tag: release.tag,
    ref: release.ref,
    refOid: release.refOid,
    sha,
  }
}

export async function reconcileApplicationRelease({
  repository,
  githubRepository,
}) {
  if (!githubRepository) throw new Error('GITHUB_REPOSITORY is required')
  git(
    repository,
    'fetch',
    'origin',
    '+refs/heads/main:refs/remotes/origin/main'
  )
  let release
  for (const candidate of remoteApplicationTags(repository)) {
    release = releaseOnMain(repository, candidate)
    if (release) break
  }
  if (!release) return { state: 'none' }

  try {
    const ci = await querySelectedCi({
      repository: githubRepository,
      sha: release.sha,
    })
    return { state: ci.state, ...release, ...ci }
  } catch (error) {
    if (error.ci) writeReleaseOutput({ ...release, ...error.ci })
    throw error
  }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  writeReleaseOutput(
    await reconcileApplicationRelease({
      repository: process.cwd(),
      githubRepository: process.env.GITHUB_REPOSITORY,
    })
  )
}
