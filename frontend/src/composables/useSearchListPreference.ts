import { computed, ref, toValue, watch, type MaybeRefOrGetter } from "vue"
import type { SearchListPreference } from "@/models/searchListPreference"

export function useSearchListPreference(opts: {
  enabled: MaybeRefOrGetter<boolean>
  trimmedSearchKey: MaybeRefOrGetter<string>
}) {
  const listPreference = ref<SearchListPreference>("auto")

  watch(
    () => toValue(opts.trimmedSearchKey),
    (next, prev) => {
      if (!toValue(opts.enabled)) return
      if (next === "" || prev === "") {
        listPreference.value = "auto"
      }
    }
  )

  const effectiveListPreference = computed<SearchListPreference>(() =>
    toValue(opts.enabled) ? listPreference.value : "auto"
  )

  return { listPreference, effectiveListPreference }
}
