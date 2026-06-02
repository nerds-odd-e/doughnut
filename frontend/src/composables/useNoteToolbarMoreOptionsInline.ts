import { ref, watch, type Ref } from "vue"

export const NOTE_TOOLBAR_MORE_OPTIONS_INLINE_MIN_PX = 600

export function useNoteToolbarMoreOptionsInline(
  toolbarNavRef: Ref<HTMLElement | null>
) {
  const showMoreOptionsInline = ref(false)

  const update = () => {
    const el = toolbarNavRef.value
    if (!el) return
    showMoreOptionsInline.value =
      el.clientWidth >= NOTE_TOOLBAR_MORE_OPTIONS_INLINE_MIN_PX
  }

  watch(
    toolbarNavRef,
    (el, _, onCleanup) => {
      if (!el) {
        showMoreOptionsInline.value = false
        return
      }
      update()
      if (typeof ResizeObserver === "undefined") return
      const resizeObserver = new ResizeObserver(update)
      resizeObserver.observe(el)
      onCleanup(() => resizeObserver.disconnect())
    },
    { immediate: true }
  )

  return { showMoreOptionsInline }
}
