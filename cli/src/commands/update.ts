import { chmodSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { exitCliError } from '../cliExit.js'
import {
  getVersion,
  formatVersionOutput,
  parseVersionFromOutput,
  compareVersions,
} from './version.js'

const BASE_URL =
  process.env.BASE_URL ?? 'https://storage.googleapis.com/dough-01'
const DOWNLOAD_PATH = `${BASE_URL}/doughnut-cli-latest/doughnut`

function exceptionText(e: unknown): string {
  return e instanceof Error ? e.message : String(e)
}

export async function runUpdate(): Promise<void> {
  const currentVersion = getVersion()
  const currentPath = process.argv[1]
  if (!currentPath) {
    exitCliError('could not determine executable path')
  }
  const tempFile = join(tmpdir(), `doughnut-update-${Date.now()}`)

  let response: Response
  try {
    response = await fetch(DOWNLOAD_PATH)
  } catch (e) {
    exitCliError(`failed to download: ${exceptionText(e)}`)
  }

  if (!response.ok) {
    exitCliError(`download failed: HTTP ${response.status}`)
  }

  const buffer = await response.arrayBuffer()
  writeFileSync(tempFile, Buffer.from(buffer))
  chmodSync(tempFile, 0o755)

  const result = spawnSync(tempFile, ['version'], { encoding: 'utf8' })
  if (result.error || result.status !== 0) {
    rmSync(tempFile, { force: true })
    exitCliError('downloaded binary is invalid or failed to run')
  }

  const incomingVersion = parseVersionFromOutput(result.stdout)
  if (!incomingVersion) {
    rmSync(tempFile, { force: true })
    exitCliError('could not determine version of downloaded binary')
  }

  if (compareVersions(incomingVersion, currentVersion) <= 0) {
    rmSync(tempFile, { force: true })
    console.log(`${formatVersionOutput()} is already the latest version`)
    return
  }

  try {
    renameSync(tempFile, currentPath)
    chmodSync(currentPath, 0o755)
  } catch (e) {
    rmSync(tempFile, { force: true })
    exitCliError(`failed to replace binary: ${exceptionText(e)}`)
  }

  console.log(`Updated doughnut from ${currentVersion} to ${incomingVersion}`)
}

export const updateDoc = {
  name: 'update',
  usage: 'update',
  description: 'Update CLI to latest version',
  category: 'subcommand' as const,
}
