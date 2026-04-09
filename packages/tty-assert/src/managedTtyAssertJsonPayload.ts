import type { WaitForTextInSurfaceOptions } from './waitForTextInSurface'

function regExpFromJsonRegexp(r: { source: string; flags?: string }): RegExp {
  return new RegExp(r.source, r.flags ?? '')
}

/** Same shape as {@link ManagedTtyAssertOptions} with regexp fields serialized (`RegExp` is not JSON-safe). */
export type ManagedTtyAssertJsonPayload = Omit<
  WaitForTextInSurfaceOptions,
  'raw' | 'needle' | 'startAfterAnchor'
> & {
  needle: string | { source: string; flags?: string }
  startAfterAnchor?: { source: string; flags?: string }[]
}

export function managedTtyAssertOptionsFromJson(
  p: ManagedTtyAssertJsonPayload
): Omit<WaitForTextInSurfaceOptions, 'raw'> {
  return {
    ...p,
    needle:
      typeof p.needle === 'string' ? p.needle : regExpFromJsonRegexp(p.needle),
    startAfterAnchor: p.startAfterAnchor?.map(regExpFromJsonRegexp),
  }
}
