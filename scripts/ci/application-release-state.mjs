import { execFileSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'
import { classifyApplicationPublication } from './application-release-bootstrap.mjs'
import { writeReleaseOutput } from './application-release-output.mjs'
import { isApplicationTag } from './application-release-version.mjs'

const recordName = 'deploy/application-release.json'
const objectId = /^[0-9a-f]{40}$/
const positiveInteger = /^[1-9]\d*$/

function accessToken() {
  if (process.env.GCP_ACCESS_TOKEN) return process.env.GCP_ACCESS_TOKEN
  try {
    return execFileSync('gcloud', ['auth', 'print-access-token'], {
      encoding: 'utf8',
    }).trim()
  } catch (error) {
    throw new Error('GCS authentication token lookup failed', { cause: error })
  }
}

function gcsUrl(apiBase, path, query) {
  const url = new URL(path, apiBase)
  url.search = new URLSearchParams(query)
  return url
}

function stateUrls(bucket, apiBase) {
  const encodedBucket = encodeURIComponent(bucket)
  return {
    read: gcsUrl(
      apiBase,
      `/storage/v1/b/${encodedBucket}/o/${encodeURIComponent(recordName)}`,
      { alt: 'media' }
    ),
    create: gcsUrl(apiBase, `/upload/storage/v1/b/${encodedBucket}/o`, {
      uploadType: 'media',
      name: recordName,
      ifGenerationMatch: '0',
    }),
  }
}

async function request(url, token, options = {}) {
  try {
    return await fetch(url, {
      signal: AbortSignal.timeout(30_000),
      ...options,
      headers: {
        Authorization: `Bearer ${token}`,
        ...options.headers,
      },
    })
  } catch (error) {
    throw new Error(`GCS request failed: ${error.message}`, { cause: error })
  }
}

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

async function existingState(url, token) {
  const response = await request(url, token)
  if (response.status === 404) return
  if (!response.ok) {
    throw new Error(
      `Application release state read failed: HTTP ${response.status}`
    )
  }
  let record
  try {
    record = JSON.parse(await response.text())
  } catch (error) {
    throw new Error('Application release state returned invalid JSON', {
      cause: error,
    })
  }
  return validateState(record)
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
  token = accessToken(),
}) {
  if (!bucket) throw new Error('GCS_BUCKET is required')
  if (!repository) throw new Error('GITHUB_REPOSITORY is required')
  const urls = stateUrls(bucket, gcsApiBase)
  const current = await existingState(urls.read, token)
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
  const response = await request(urls.create, token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(record),
  })
  if (!response.ok) {
    throw new Error(
      `Application release state create failed: HTTP ${response.status}`
    )
  }
  return { state: 'initialized', record }
}

export async function checkApplicationReleaseState({
  bucket,
  tag,
  refOid,
  sha,
  gcsApiBase = process.env.GCS_API_URL || 'https://storage.googleapis.com',
  token = accessToken(),
}) {
  if (!bucket) throw new Error('GCS_BUCKET is required')
  if (!isApplicationTag(tag)) throw new Error('RELEASE_TAG is invalid')
  if (!objectId.test(refOid)) throw new Error('RELEASE_REF_OID is invalid')
  if (!objectId.test(sha)) throw new Error('RELEASE_SHA is invalid')

  const current = await existingState(stateUrls(bucket, gcsApiBase).read, token)
  if (!current) {
    throw new Error('Application release state is missing after initialization')
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
  return { state: 'continue' }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const checkRelease = process.argv[2] === '--check-release'
  try {
    writeReleaseOutput(
      checkRelease
        ? await checkApplicationReleaseState({
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
        : `Application release tracking initialization failed: ${error.message}. ` +
            'Identify the published application release (tag, raw refOid, peeled SHA, selected CI run ID and attempt) before retrying.'
    )
    process.exitCode = 1
  }
}
