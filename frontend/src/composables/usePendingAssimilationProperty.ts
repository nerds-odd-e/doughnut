import { useAssimilationView } from "@/composables/useAssimilationView"
import { nextTick, watch, type ComponentPublicInstance, type Ref } from "vue"

export function usePendingAssimilationProperty(noteId: Ref<number>) {
  const propertyRowElements = new Map<string, HTMLElement>()
  const { targetNoteId, pendingPropertyKey } = useAssimilationView()

  const isPendingProperty = (propertyKey: string) =>
    targetNoteId.value === noteId.value &&
    pendingPropertyKey.value === propertyKey

  const setPropertyRowRef = (
    propertyKey: string,
    element: Element | ComponentPublicInstance | null
  ) => {
    if (element instanceof HTMLElement) {
      propertyRowElements.set(propertyKey, element)
      return
    }
    propertyRowElements.delete(propertyKey)
  }

  const scrollPendingPropertyIntoView = async () => {
    const key = pendingPropertyKey.value
    if (targetNoteId.value !== noteId.value || !key) {
      return
    }
    await nextTick()
    propertyRowElements.get(key)?.scrollIntoView({
      behavior: "smooth",
      block: "center",
    })
  }

  watch(
    [pendingPropertyKey, targetNoteId, noteId],
    () => {
      scrollPendingPropertyIntoView().catch(() => undefined)
    },
    { immediate: true }
  )

  return {
    isPendingProperty,
    setPropertyRowRef,
  }
}
