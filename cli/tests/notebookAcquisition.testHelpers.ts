import { afterEach, beforeEach, vi } from 'vitest'
import * as childProcess from 'node:child_process'
import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

export function installAcquireNotebookGitCheckoutTest(): {
  getConfigDir: () => string
  getDestinationPath: () => string
} {
  let savedConfigDir: string | undefined
  let configDir: string
  let destinationParent: string
  let destinationPath: string

  beforeEach(() => {
    savedConfigDir = process.env.DONUT_CONFIG_DIR
    configDir = tempConfigWithToken()
    process.env.DONUT_CONFIG_DIR = configDir
    destinationParent = fs.mkdtempSync(join(tmpdir(), 'donut-notebook-dest-'))
    destinationPath = join(destinationParent, 'notebook-checkout')
  })

  afterEach(() => {
    if (savedConfigDir === undefined) delete process.env.DONUT_CONFIG_DIR
    else process.env.DONUT_CONFIG_DIR = savedConfigDir
    fs.rmSync(configDir, { recursive: true, force: true })
    fs.rmSync(destinationParent, { recursive: true, force: true })
    vi.unstubAllGlobals()
    vi.mocked(childProcess.spawnSync).mockReset()
    vi.mocked(fs.renameSync).mockClear()
  })

  return {
    getConfigDir: () => configDir,
    getDestinationPath: () => destinationPath,
  }
}

export function stubBundleFetch(): void {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok: true,
      arrayBuffer: () =>
        Promise.resolve(new TextEncoder().encode('bundle-bytes').buffer),
    })
  )
}

export const LFS_ATTRIBUTES = '* filter=lfs diff=lfs merge=lfs -text\n'

export function writeLfsCheckoutPointer(
  checkoutDir: string,
  filename: string,
  pointerBody: string
): void {
  fs.mkdirSync(checkoutDir, { recursive: true })
  fs.writeFileSync(join(checkoutDir, '.gitattributes'), LFS_ATTRIBUTES)
  fs.writeFileSync(join(checkoutDir, filename), pointerBody)
}
