import type { InjectionKey, Ref } from "vue"

/**
 * Where the reading view puts its book controls: inside the page bar that the
 * book reader keeps across loading, failure, and reading, so the bar is never
 * replaced.
 */
export const bookReadingBarTargetKey: InjectionKey<Ref<HTMLElement | null>> =
  Symbol("bookReadingBarTarget")
