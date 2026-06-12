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
  const { pendingOnForNoteId, pendingPropertyKey } = useAssimilationView()

  const isPendingProperty = (propertyKey: string) =>
    pendingOnForNoteId.value === noteId.value &&
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
    if (pendingOnForNoteId.value !== noteId.value || !key) {
      return
    }
    propertiesSectionOpen.value = true
    await nextTick()
    propertyRowElements.get(key)?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
    })
  }

  watch(
    [pendingPropertyKey, pendingOnForNoteId, noteId],
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
