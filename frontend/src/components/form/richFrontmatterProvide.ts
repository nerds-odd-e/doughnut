import { computed, type ComputedRef, type InjectionKey } from "vue"

export const richFrontmatterIsIndexContextKey: InjectionKey<
  ComputedRef<boolean>
> = Symbol("richFrontmatterIsIndexContext")

export const richFrontmatterIsIndexContextFallback = computed(() => false)
