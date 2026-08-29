import { notePropertyKeyFromRoute } from "@/routes/noteShowLocation"
import { computed, nextTick, watch, type ComponentPublicInstance } from "vue"
import { useRoute } from "vue-router"

function scrollPropertyRowIntoView(element: HTMLElement) {
  element.scrollIntoView({
    behavior: "smooth",
    block: "center",
  })
}

export function useFocusedNoteProperty() {
  const route = useRoute()
  const propertyRowElements = new Map<string, HTMLElement>()
  const focusedPropertyKey = computed(() => notePropertyKeyFromRoute(route))

  const isFocusedProperty = (propertyKey: string) =>
    focusedPropertyKey.value === propertyKey

  const setPropertyRowRef = (
    propertyKey: string,
    element: Element | ComponentPublicInstance | null
  ) => {
    if (element instanceof HTMLElement) {
      propertyRowElements.set(propertyKey, element)
      if (focusedPropertyKey.value === propertyKey) {
        scrollPropertyRowIntoView(element)
      }
      return
    }
    propertyRowElements.delete(propertyKey)
  }

  watch(focusedPropertyKey, async (key) => {
    if (!key) {
      return
    }
    await nextTick()
    const element = propertyRowElements.get(key)
    if (element) {
      scrollPropertyRowIntoView(element)
    }
  })

  return {
    isFocusedProperty,
    setPropertyRowRef,
  }
}
