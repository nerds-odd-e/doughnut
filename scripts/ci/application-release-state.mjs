import { pathToFileURL } from 'node:url'
import { classifyApplicationPublication } from './application-release-bootstrap.mjs'
import { writeReleaseOutput } from './application-release-output.mjs'
import {
  applicationReleaseAccessToken,
  createApplicationReleaseState,
  readApplicationReleaseState,
  writeApplicationReleaseState,
} from './application-release-state-store.mjs'
import {
  compareApplicationVersionsDescending,
  isApplicationTag,
} from './application-release-version.mjs'

const objectId = /^[0-9a-f]{40}$/
const positiveInteger = /^[1-9]\d*$/

function validateState(record) {
  if (typeof record !== 'object' || record === null || Array.isArray(record)) {
    throw new Error('Application release state is not an object')
  }
  if (
    record.outcome === 'initialized-empty' &&
    Object.keys(record).length === 1
  ) {
    return record
  }
  const selectedKeys = ['tag', 'ref_oid', 'sha', 'outcome']
  if (
    record.outcome === 'selected' &&
    selectedKeys.every((key) => Object.hasOwn(record, key)) &&
    Object.keys(record).length === selectedKeys.length &&
    isApplicationTag(record.tag) &&
    objectId.test(record.ref_oid) &&
    objectId.test(record.sha)
  ) {
    return record
  }
  const keys = [
    'tag',
    'ref_oid',
    'sha',
    'ci_run_id',
    'ci_run_attempt',
    'outcome',
  ]
  if (
    keys.every((key) => Object.hasOwn(record, key)) &&
    Object.keys(record).length === keys.length &&
    isApplicationTag(record.tag) &&
    objectId.test(record.ref_oid) &&
    objectId.test(record.sha) &&
    positiveInteger.test(record.ci_run_id) &&
    positiveInteger.test(record.ci_run_attempt) &&
    ['publishing', 'succeeded'].includes(record.outcome)
  ) {
    return record
  }
  throw new Error('Application release state has an invalid schema')
}

function initialState(classification) {
  if (classification.state === 'empty') return { outcome: 'initialized-empty' }
  if (classification.state === 'published') {
    return {
      tag: classification.tag,
      ref_oid: classification.refOid,
      sha: classification.sha,
      ci_run_id: String(classification.runId),
      ci_run_attempt: String(classification.runAttempt),
      outcome: 'succeeded',
    }
  }
  throw new Error('Application publication history was not classified')
}

export async function initializeApplicationReleaseState({
  bucket,
  repository,
  repositoryRoot = process.cwd(),
  gcsApiBase = process.env.GCS_API_URL || 'https://storage.googleapis.com',
  githubApiBase = process.env.GITHUB_API_URL || 'https://api.github.com',
  githubToken = process.env.GITHUB_TOKEN,
  token = applicationReleaseAccessToken(),
}) {
  if (!bucket) throw new Error('GCS_BUCKET is required')
  if (!repository) throw new Error('GITHUB_REPOSITORY is required')
  const stateStore = { bucket, apiBase: gcsApiBase, token }
  const existing = await readApplicationReleaseState(stateStore)
  const current = existing && validateState(existing)
  if (current) return { state: 'existing', record: current }

  let classification
  try {
    classification = await classifyApplicationPublication({
      repository,
      repositoryRoot,
      apiBase: githubApiBase,
      token: githubToken,
    })
  } catch (error) {
    throw new Error(
      `Application publication history is ambiguous: ${error.message}`,
      {
        cause: error,
      }
    )
  }
  const record = initialState(classification)
  await createApplicationReleaseState({ ...stateStore, record })
  return { state: 'initialized', record }
}

export async function selectApplicationReleaseState({
  bucket,
  tag,
  refOid,
  sha,
  gcsApiBase = process.env.GCS_API_URL || 'https://storage.googleapis.com',
  token = applicationReleaseAccessToken(),
}) {
  if (!bucket) throw new Error('GCS_BUCKET is required')
  if (!isApplicationTag(tag)) throw new Error('RELEASE_TAG is invalid')
  if (!objectId.test(refOid)) throw new Error('RELEASE_REF_OID is invalid')
  if (!objectId.test(sha)) throw new Error('RELEASE_SHA is invalid')

  const record = { tag, ref_oid: refOid, sha, outcome: 'selected' }
  await writeApplicationReleaseState({
    bucket,
    apiBase: gcsApiBase,
    token,
    record,
  })
  return { state: 'selected', record }
}

export async function checkApplicationReleaseState({
  bucket,
  tag,
  refOid,
  sha,
  gcsApiBase = process.env.GCS_API_URL || 'https://storage.googleapis.com',
  token = applicationReleaseAccessToken(),
}) {
  if (!bucket) throw new Error('GCS_BUCKET is required')
  const releaseAbsent =
    tag === undefined && refOid === undefined && sha === undefined
  if (!releaseAbsent) {
    if (!isApplicationTag(tag)) throw new Error('RELEASE_TAG is invalid')
    if (!objectId.test(refOid)) throw new Error('RELEASE_REF_OID is invalid')
    if (!objectId.test(sha)) throw new Error('RELEASE_SHA is invalid')
  }
  const existing = await readApplicationReleaseState({
    bucket,
    apiBase: gcsApiBase,
    token,
  })
  const current = existing && validateState(existing)
  if (!current) {
    throw new Error('Application release state is missing after initialization')
  }
  if (releaseAbsent) {
    if (['selected', 'publishing'].includes(current.outcome)) {
      throw new Error(`Selected release tag ${current.tag} is missing`)
    }
    return { state: 'none' }
  }
  if (current.tag === tag) {
    if (current.ref_oid !== refOid || current.sha !== sha) {
      throw new Error(
        `Application release identity mismatch for ${tag}: persisted refOid ${current.ref_oid} and SHA ${current.sha}, current refOid ${refOid} and SHA ${sha}`
      )
    }
    if (current.outcome === 'succeeded') {
      return { state: 'already-released' }
    }
    return { state: 'retry' }
  }
  if (
    current.outcome !== 'initialized-empty' &&
    compareApplicationVersionsDescending(current.tag, tag) < 0
  ) {
    return { state: 'superseded' }
  }
  return { state: 'continue' }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const checkRelease = process.argv[2] === '--check-release'
  const selectRelease = process.argv[2] === '--select-release'
  try {
    writeReleaseOutput(
      checkRelease
        ? await checkApplicationReleaseState({
            bucket: process.env.GCS_BUCKET,
            tag: process.env.RELEASE_TAG,
            refOid: process.env.RELEASE_REF_OID,
            sha: process.env.RELEASE_SHA,
          })
        : selectRelease
          ? await selectApplicationReleaseState({
              bucket: process.env.GCS_BUCKET,
              tag: process.env.RELEASE_TAG,
              refOid: process.env.RELEASE_REF_OID,
              sha: process.env.RELEASE_SHA,
            })
          : await initializeApplicationReleaseState({
              bucket: process.env.GCS_BUCKET,
              repository: process.env.GITHUB_REPOSITORY,
            })
    )
  } catch (error) {
    console.error(
      checkRelease
        ? `Application release state check failed: ${error.message}`
        : selectRelease
          ? `Application release state selection failed: ${error.message}`
          : `Application release tracking initialization failed: ${error.message}. ` +
            'Identify the published application release (tag, raw refOid, peeled SHA, selected CI run ID and attempt) before retrying.'
    )
    process.exitCode = 1
  }
}
