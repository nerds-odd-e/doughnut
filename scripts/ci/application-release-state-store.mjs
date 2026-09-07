import { execFileSync } from 'node:child_process'

const recordName = 'deploy/application-release.json'

export function applicationReleaseAccessToken() {
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
  const upload = (query = {}) =>
    gcsUrl(apiBase, `/upload/storage/v1/b/${encodedBucket}/o`, {
      uploadType: 'media',
      name: recordName,
      ...query,
    })
  return {
    read: gcsUrl(
      apiBase,
      `/storage/v1/b/${encodedBucket}/o/${encodeURIComponent(recordName)}`,
      { alt: 'media' }
    ),
    create: upload({ ifGenerationMatch: '0' }),
    write: upload(),
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

export async function readApplicationReleaseState({ bucket, apiBase, token }) {
  const response = await request(stateUrls(bucket, apiBase).read, token)
  if (response.status === 404) return
  if (!response.ok) {
    throw new Error(
      `Application release state read failed: HTTP ${response.status}`
    )
  }
  try {
    return JSON.parse(await response.text())
  } catch (error) {
    throw new Error('Application release state returned invalid JSON', {
      cause: error,
    })
  }
}

async function uploadState({ bucket, apiBase, token, record, createOnly }) {
  const urls = stateUrls(bucket, apiBase)
  const response = await request(createOnly ? urls.create : urls.write, token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(record),
  })
  if (!response.ok) {
    const operation = createOnly ? 'create' : 'write'
    throw new Error(
      `Application release state ${operation} failed: HTTP ${response.status}`
    )
  }
}

export const createApplicationReleaseState = (options) =>
  uploadState({ ...options, createOnly: true })

export const writeApplicationReleaseState = (options) =>
  uploadState({ ...options, createOnly: false })
