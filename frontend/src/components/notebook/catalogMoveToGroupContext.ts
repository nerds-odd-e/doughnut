import type { ComputedRef, InjectionKey } from "vue"

export type CatalogMoveToGroupInjected = {
  circleId: number
  existingGroups: { id: number; name: string }[]
}

export const catalogMoveToGroupContextKey: InjectionKey<
  ComputedRef<CatalogMoveToGroupInjected | undefined>
> = Symbol("catalogMoveToGroupContext")
