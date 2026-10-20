import type { InjectionKey, Ref } from "vue"

/**
 * Where a page puts its controls: inside the page bar that the page keeps
 * across its own loading, failure, and content states, so the bar is never
 * replaced.
 */
export const pageBarTargetKey: InjectionKey<Ref<HTMLElement | null>> =
  Symbol("pageBarTarget")
