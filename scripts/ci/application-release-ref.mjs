import { execFileSync } from 'node:child_process'

const git = (repository, ...args) =>
  execFileSync('git', args, { cwd: repository, encoding: 'utf8' }).trim()

const current = git(
  process.env.RELEASE_SOURCE_ROOT,
  'ls-remote',
  'origin',
  process.env.RELEASE_REF
)
if (current.split('\t')[0] !== process.env.RELEASE_REF_OID) {
  throw new Error(
    `Release ref changed or disappeared: ${process.env.RELEASE_REF}`
  )
}
