const pageLimit = 10

async function fetchResponse(url, token, context) {
  const response = await fetch(url, {
    signal: AbortSignal.timeout(30_000),
    headers: {
      Accept: 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
  if (!response.ok) {
    throw new Error(`${context}: HTTP ${response.status}`)
  }
  return response
}

async function pagedGitHubCollection({
  urlForPage,
  collection,
  token,
  context,
}) {
  const items = []
  for (let page = 1; page <= pageLimit; page++) {
    const response = await fetchResponse(
      urlForPage(page),
      token,
      `${context} page ${page}`
    )
    let payload
    try {
      payload = await response.json()
    } catch (error) {
      throw new Error(`${context} page ${page} returned invalid JSON`, {
        cause: error,
      })
    }
    const total = payload.total_count
    const batch = payload[collection]
    if (!(Number.isInteger(total) && Array.isArray(batch))) {
      throw new Error(`${context} page ${page} returned an invalid result`)
    }
    if (total > pageLimit * 100) {
      throw new Error(`${context} exceeds the 1000-result search limit`)
    }
    items.push(...batch)
    if (items.length >= total) return items
    if (page === pageLimit || batch.length === 0) {
      throw new Error(`${context} did not return the complete bounded result`)
    }
  }
}

function apiUrl(base, path, query = {}) {
  const url = new URL(path, base)
  url.search = new URLSearchParams(query)
  return url
}

export async function fetchDeployWorkflowRuns({ repository, apiBase, token }) {
  return pagedGitHubCollection({
    urlForPage: (page) =>
      apiUrl(
        apiBase,
        `/repos/${repository}/actions/workflows/deploy.yml/runs`,
        { event: 'push', per_page: '100', page: String(page) }
      ),
    collection: 'workflow_runs',
    token,
    context: 'Application publication history lookup',
  })
}

export async function fetchWorkflowRunJobs({
  repository,
  run,
  apiBase,
  token,
}) {
  return pagedGitHubCollection({
    urlForPage: (page) =>
      apiUrl(apiBase, `/repos/${repository}/actions/runs/${run.id}/jobs`, {
        filter: 'all',
        per_page: '100',
        page: String(page),
      }),
    collection: 'jobs',
    token,
    context: `Application publication jobs lookup for run ${run.id} attempt ${run.run_attempt}`,
  })
}

export async function fetchAdmissionJobLog({
  repository,
  jobId,
  run,
  runAttempt,
  apiBase,
  token,
}) {
  const response = await fetchResponse(
    apiUrl(apiBase, `/repos/${repository}/actions/jobs/${jobId}/logs`),
    token,
    `Admission log lookup for run ${run.id} attempt ${runAttempt}`
  )
  return response.text()
}
