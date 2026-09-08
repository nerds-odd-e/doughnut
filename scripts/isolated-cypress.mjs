import path from 'node:path'
import {
  loadCompleteIsolatedE2eAllocation,
  readPresentWorktreeLocalConfig,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import { isolatedBrowserOrigin } from './sut-runtime-target.mjs'
import {
  acquireSutRunnerLease,
  releaseSutRunnerLease,
  releaseSutRunnerLeaseSync,
  verifyLiveSutOwner,
} from './sut-owner.mjs'

export const SUPPORTED_ISOLATED_CYPRESS_SPEC =
  'e2e_test/features/note_creation_and_update/worktree_note_editing.feature'

const PRIMARY_CYPRESS_ORIGIN = 'http://localhost:5173'

function specArgsFromArgv(argv) {
  const specs = []
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--spec' && argv[i + 1]) {
      specs.push(argv[i + 1])
      i += 1
    } else if (typeof arg === 'string' && arg.startsWith('--spec=')) {
      specs.push(arg.slice('--spec='.length))
    }
  }
  return specs.flatMap((value) =>
    value
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean)
  )
}

function specPatterns(specPattern) {
  if (Array.isArray(specPattern)) return specPattern
  return specPattern ? [specPattern] : []
}

function normalizeSelectedSpec(spec, checkoutRoot) {
  const trimmed = spec.trim().replace(/\\/g, '/')
  const withoutFile = trimmed.startsWith('file:')
    ? trimmed.slice('file:'.length)
    : trimmed
  const absolute = path.isAbsolute(withoutFile)
    ? withoutFile
    : path.resolve(checkoutRoot, withoutFile)
  const relative = path.relative(checkoutRoot, absolute).replace(/\\/g, '/')
  if (!relative.startsWith('..')) {
    return relative.replace(/^\.\//, '')
  }
  return withoutFile.replace(/^\.\//, '')
}

function selectedCypressSpecs({ argv, specPattern, checkoutRoot }) {
  const fromArgv = specArgsFromArgv(argv)
  const raw = fromArgv.length > 0 ? fromArgv : specPatterns(specPattern)
  return raw.map((spec) => normalizeSelectedSpec(spec, checkoutRoot))
}

function isGlobSpecPattern(spec) {
  return spec.includes('*') || spec.includes('?')
}

function hasExplicitCypressSpecSelection(argv, specPattern) {
  if (specArgsFromArgv(argv).length > 0) return true
  const patterns = specPatterns(specPattern)
  return (
    patterns.length > 0 &&
    patterns.every((pattern) => !isGlobSpecPattern(pattern))
  )
}

function specsFromBeforeRun(details, checkoutRoot) {
  const specs = details?.specs ?? []
  return specs.map((spec) =>
    normalizeSelectedSpec(
      spec.relative ?? spec.absolute ?? spec.name ?? String(spec),
      checkoutRoot
    )
  )
}

function assertSupportedIsolatedCypressSpecs(specs) {
  if (specs.length === 1 && specs[0] === SUPPORTED_ISOLATED_CYPRESS_SPEC) {
    return
  }
  throw new Error(
    'Isolated Cypress only supports ' +
      `${SUPPORTED_ISOLATED_CYPRESS_SPEC}. ` +
      'Refusing this spec selection so it does not reset, mock, or use shared defaults.'
  )
}

function refuseConflictingCypressOrigin(env, config, isolatedOrigin) {
  const envOrigin = env.CYPRESS_baseUrl
  if (envOrigin && envOrigin !== isolatedOrigin) {
    throw new Error(
      `Conflicting CYPRESS_baseUrl=${envOrigin} does not match the configured isolated Cypress origin (${isolatedOrigin}).`
    )
  }
  const configOrigin = config.baseUrl
  if (
    configOrigin &&
    configOrigin !== isolatedOrigin &&
    configOrigin !== PRIMARY_CYPRESS_ORIGIN
  ) {
    throw new Error(
      `Conflicting Cypress baseUrl=${configOrigin} does not match the configured isolated Cypress origin (${isolatedOrigin}).`
    )
  }
}

async function requireHealthyOwningSut(checkoutRoot, healthcheckFn) {
  const live = await verifyLiveSutOwner(checkoutRoot)
  if (!live.ok) {
    throw new Error(
      'Isolated Cypress requires a verified live SUT owner in this checkout.'
    )
  }
  const runHealth =
    healthcheckFn ??
    (async (root) => {
      const { runSutHealthcheck } = await import('./sut-healthcheck.mjs')
      return runSutHealthcheck({ checkoutRoot: root })
    })
  const health = await runHealth(checkoutRoot)
  if (!health?.ok) {
    throw new Error(
      'Isolated Cypress requires a healthy owning SUT in this checkout.'
    )
  }
}

function failLoudly(error) {
  process.stderr.write(
    `${error instanceof Error ? error.message : String(error)}\n`
  )
  process.exit(1)
}

function registerRunnerLeaseRelease(release) {
  let released = false
  const releaseOnce = async () => {
    if (released) return
    released = true
    await release()
  }
  process.once('SIGINT', () => {
    releaseOnce().catch(failLoudly)
  })
  process.once('SIGTERM', () => {
    releaseOnce().catch(failLoudly)
  })
  return releaseOnce
}

export async function guardCypressNodeSetup(
  checkoutRoot,
  config = {},
  options = {}
) {
  readPresentWorktreeLocalConfig(checkoutRoot)
  if (!worktreeIsolationApplies(checkoutRoot)) {
    return
  }
  const argv = options.argv ?? process.argv
  const env = options.env ?? process.env
  const explicitSpecs = hasExplicitCypressSpecSelection(
    argv,
    config.specPattern
  )
  if (explicitSpecs) {
    assertSupportedIsolatedCypressSpecs(
      selectedCypressSpecs({
        argv,
        specPattern: config.specPattern,
        checkoutRoot,
      })
    )
  } else if (typeof options.on !== 'function') {
    assertSupportedIsolatedCypressSpecs(
      selectedCypressSpecs({
        argv,
        specPattern: config.specPattern,
        checkoutRoot,
      })
    )
  }
  const allocation = loadCompleteIsolatedE2eAllocation(checkoutRoot)
  const origin = isolatedBrowserOrigin(allocation.e2e)
  refuseConflictingCypressOrigin(env, config, origin)
  await requireHealthyOwningSut(checkoutRoot, options.healthcheckFn)
  const leaseToken = await acquireSutRunnerLease(checkoutRoot)
  const releaseOnce = registerRunnerLeaseRelease(() =>
    releaseSutRunnerLease(checkoutRoot, leaseToken)
  )
  process.once('exit', () => {
    releaseSutRunnerLeaseSync(checkoutRoot, leaseToken)
  })
  if (typeof options.on === 'function') {
    options.on('before:run', async (details) => {
      try {
        assertSupportedIsolatedCypressSpecs(
          specsFromBeforeRun(details, checkoutRoot)
        )
      } catch (error) {
        await releaseOnce()
        throw error
      }
    })
    options.on('after:spec', releaseOnce)
    options.on('after:run', releaseOnce)
  }
  config.baseUrl = origin
  return { release: releaseOnce, origin }
}
