import { computed, type ComputedRef, type InjectionKey } from "vue"

export const richFrontmatterIsReadmeContextKey: InjectionKey<
  ComputedRef<boolean>
> = Symbol("richFrontmatterIsReadmeContext")

export const richFrontmatterIsReadmeContextFallback = computed(() => false)
