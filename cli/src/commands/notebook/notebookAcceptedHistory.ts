import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { downloadNotebookGitBundle } from './notebookAcquisition.js'
import { runSystemGitOrThrow } from './systemGit.js'

function acceptedHistoryFailure(
  detail: string | undefined,
  status: number | null
): string {
  return `failed to read the notebook's accepted history${detail ? `: ${detail}` : ` (exit code ${status})`}`
}

export async function downloadAcceptedNotebookHead(
  notebookId: number
): Promise<string> {
  const tempDir = fs.mkdtempSync(
    path.join(os.tmpdir(), `donut-notebook-accepted-history-${process.pid}-`)
  )
  try {
    const bundleFile = path.join(tempDir, 'accepted.bundle')
    await downloadNotebookGitBundle(notebookId, bundleFile)

    const acceptedRepoDir = path.join(tempDir, 'accepted.git')
    runSystemGitOrThrow(
      ['init', '--quiet', '--bare', acceptedRepoDir],
      (detail, status) =>
        `failed to prepare temporary storage for accepted notebook history${detail ? `: ${detail}` : ` (exit code ${status})`}`
    )
    runSystemGitOrThrow(
      [
        '-C',
        acceptedRepoDir,
        'fetch',
        '--quiet',
        bundleFile,
        'refs/heads/main:refs/heads/main',
      ],
      acceptedHistoryFailure
    )

    return runSystemGitOrThrow(
      ['-C', acceptedRepoDir, 'rev-parse', 'main'],
      acceptedHistoryFailure
    ).trim()
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}
