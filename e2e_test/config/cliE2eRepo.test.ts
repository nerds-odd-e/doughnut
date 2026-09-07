import assert from 'node:assert/strict'
import {
  mkdirSync,
  mkdtempSync,
  rmSync,
  utimesSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, test } from 'node:test'
import { bundleIsAtLeastAsNewAsCliSources } from './cliE2eRepo'

describe('bundleIsAtLeastAsNewAsCliSources', () => {
  const roots: string[] = []

  afterEach(() => {
    for (const root of roots.splice(0)) {
      rmSync(root, { recursive: true, force: true })
    }
  })

  function fixtureRepo(): {
    root: string
    cacheBundle: string
    source: string
  } {
    const root = mkdtempSync(join(tmpdir(), 'e2e-install-cache-'))
    roots.push(root)
    mkdirSync(join(root, 'cli/src'), { recursive: true })
    mkdirSync(join(root, 'cli/dist'), { recursive: true })
    writeFileSync(join(root, 'cli/package.json'), '{}\n')
    writeFileSync(join(root, 'cli/tsconfig.json'), '{}\n')
    const source = join(root, 'cli/src/index.ts')
    writeFileSync(source, 'export {}\n')
    const cacheBundle = join(
      root,
      'cli/dist/e2e-install-donut-cli-0.5.2.bundle.mjs'
    )
    writeFileSync(cacheBundle, '// stale install bundle\n')
    return { root, cacheBundle, source }
  }

  test('refuses a version-keyed cache older than CLI sources', () => {
    const { root, cacheBundle, source } = fixtureRepo()
    const old = new Date(1_700_000_000_000)
    const newer = new Date(1_800_000_000_000)
    utimesSync(cacheBundle, old, old)
    utimesSync(source, newer, newer)
    assert.equal(bundleIsAtLeastAsNewAsCliSources(root, cacheBundle), false)
  })

  test('reuses a version-keyed cache at least as new as CLI sources', () => {
    const { root, cacheBundle, source } = fixtureRepo()
    utimesSync(source, new Date(1_700_000_000_000), new Date(1_700_000_000_000))
    utimesSync(
      cacheBundle,
      new Date(1_800_000_000_000),
      new Date(1_800_000_000_000)
    )
    assert.equal(bundleIsAtLeastAsNewAsCliSources(root, cacheBundle), true)
  })
})
