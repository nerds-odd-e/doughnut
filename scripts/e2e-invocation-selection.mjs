import {
  assertSupportedIsolatedCypressSpecs,
  hasExplicitCypressSpecSelection,
  selectedCypressSpecs,
} from './isolated-cypress-spec-selection.mjs'
import { resolveSutCheckoutTarget } from './sut-isolated-target.mjs'

export function browserFromArgv(argv) {
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--browser' && argv[i + 1]) {
      return argv[i + 1]
    }
    if (typeof arg === 'string' && arg.startsWith('--browser=')) {
      return arg.slice('--browser='.length)
    }
  }
  return
}

export function resolveSpecs(argv, checkoutRoot, isolated) {
  if (!hasExplicitCypressSpecSelection(argv)) {
    throw new Error(
      'e2e-runner requires an explicit --spec selection of supported specs.'
    )
  }
  const selected = selectedCypressSpecs({ argv, checkoutRoot })
  if (!isolated) {
    return { specs: selected, approved: null }
  }
  const approved = assertSupportedIsolatedCypressSpecs(selected)
  return { specs: selected, approved }
}

/**
 * Resolve the invocation checkout target shared by the batch and interactive
 * entry points. `isolated` is determined from the checkout topology (via
 * `isIsolatedCheckoutFn`) without requiring a complete allocation, so a fresh
 * isolated worktree is not forced through `resolveSutCheckoutTarget` (which
 * needs an allocation) before `startOwnedSutLifetime` provisions. For the
 * primary target, the resolved target is computed up front so the wrapper
 * can refuse foreign listeners on its canonical ports before spawning.
 */
export function resolveInvocationCheckout({
  checkoutRoot,
  runtimeTarget,
  isIsolatedCheckoutFn,
}) {
  const isolated = isIsolatedCheckoutFn(checkoutRoot)
  const resolvedCheckoutTarget = isolated
    ? undefined
    : resolveSutCheckoutTarget({ checkoutRoot, runtimeTarget })
  return { isolated, resolvedCheckoutTarget }
}
