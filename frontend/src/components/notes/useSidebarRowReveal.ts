import { type Ref, watch } from "vue"
import { usePeerSort } from "@/composables/usePeerSort"

/**
 * An active sidebar row scrolls the least amount that shows the whole row,
 * when it becomes active and when the sidebar is re-sorted.
 */
export function useSidebarRowReveal(
  row: Ref<HTMLElement | null>,
  isActive: () => boolean
) {
  const { peerSortSpec } = usePeerSort()

  function reveal() {
    if (isActive())
      row.value?.scrollIntoView({ block: "nearest", behavior: "smooth" })
  }

  watch(() => isActive() && row.value, reveal, { flush: "post" })
  watch(peerSortSpec, reveal, { flush: "post" })
}
