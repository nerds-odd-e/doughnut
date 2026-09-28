import { type Ref, watch } from "vue"

/** An active sidebar row scrolls the least amount that shows the whole row. */
export function useSidebarRowReveal(
  row: Ref<HTMLElement | null>,
  isActive: () => boolean
) {
  watch(
    () => isActive() && row.value,
    (activeRow) => {
      if (activeRow)
        activeRow.scrollIntoView({ block: "nearest", behavior: "smooth" })
    },
    { flush: "post" }
  )
}
