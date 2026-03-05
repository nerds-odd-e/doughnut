import { chmodSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'
import {
  getVersion,
  formatVersionOutput,
  parseVersionFromOutput,
  compareVersions,
} from './version.js'

const BASE_URL =
  process.env.BASE_URL ?? 'https://storage.googleapis.com/dough-01'
const DOWNLOAD_PATH = `${BASE_URL}/doughnut-cli-latest/doughnut`

export async function runUpdate(): Promise<void> {
  const currentVersion = getVersion()
  const currentPath = process.argv[1]
  if (!currentPath) {
    console.error('Could not determine executable path')
    process.exit(1)
  }
  const tempFile = join(tmpdir(), `doughnut-update-${Date.now()}`)

  let response: Response
  try {
    response = await fetch(DOWNLOAD_PATH)
  } catch (e) {
    console.error(`Failed to download: ${e instanceof Error ? e.message : e}`)
    process.exit(1)
  }

  if (!response.ok) {
    console.error(`Download failed: HTTP ${response.status}`)
    process.exit(1)
  }

  const buffer = await response.arrayBuffer()
  writeFileSync(tempFile, Buffer.from(buffer))
  chmodSync(tempFile, 0o755)

  const result = spawnSync(tempFile, ['version'], { encoding: 'utf8' })
  if (result.error || result.status !== 0) {
    rmSync(tempFile, { force: true })
    console.error('Downloaded binary is invalid or failed to run')
    process.exit(1)
  }

  const incomingVersion = parseVersionFromOutput(result.stdout)
  if (!incomingVersion) {
    rmSync(tempFile, { force: true })
    console.error('Could not determine version of downloaded binary')
    process.exit(1)
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
    console.error(
      `Failed to replace binary: ${e instanceof Error ? e.message : e}`
    )
    process.exit(1)
  }

  console.log(`Updated doughnut from ${currentVersion} to ${incomingVersion}`)
}
