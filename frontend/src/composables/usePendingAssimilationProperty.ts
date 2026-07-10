import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  nextTick,
  ref,
  watch,
  type ComponentPublicInstance,
  type Ref,
} from "vue"

export function usePendingAssimilationProperty(noteId: Ref<number>) {
  const propertiesSectionOpen = ref(false)
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
    propertiesSectionOpen.value = true
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

  watch(noteId, () => {
    propertiesSectionOpen.value = false
  })

  return {
    propertiesSectionOpen,
    isPendingProperty,
    setPropertyRowRef,
  }
}
