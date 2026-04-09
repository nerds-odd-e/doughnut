import type { WaitForTextInSurfaceOptions } from './waitForTextInSurface'

/** JSON-safe regexp for `cy.task` and other serializers (`RegExp` is not JSON-safe). */
export type SerializableRegExp = { source: string; flags?: string }

export function regExpFromSerializable(r: SerializableRegExp): RegExp {
  return new RegExp(r.source, r.flags ?? '')
}

/** Same shape as {@link ManagedTtyAssertOptions} with regexp fields serialized. */
export type ManagedTtyAssertJsonPayload = Omit<
  WaitForTextInSurfaceOptions,
  'raw' | 'needle' | 'startAfterAnchor'
> & {
  needle: string | SerializableRegExp
  startAfterAnchor?: SerializableRegExp[]
}

export function managedTtyAssertOptionsFromJson(
  p: ManagedTtyAssertJsonPayload
): Omit<WaitForTextInSurfaceOptions, 'raw'> {
  return {
    ...p,
    needle:
      typeof p.needle === 'string'
        ? p.needle
        : regExpFromSerializable(p.needle),
    startAfterAnchor: p.startAfterAnchor?.map(regExpFromSerializable),
  }
}
