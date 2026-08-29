import { notePropertyKeyFromRoute } from "@/routes/noteShowLocation"
import {
  computed,
  nextTick,
  toValue,
  watch,
  type ComponentPublicInstance,
  type MaybeRefOrGetter,
} from "vue"
import { useRoute } from "vue-router"

function scrollPropertyRowIntoView(element: HTMLElement) {
  element.scrollIntoView({
    behavior: "smooth",
    block: "center",
  })
}

export function useFocusedNoteProperty(
  propertyKeys?: MaybeRefOrGetter<readonly string[]>
) {
  const route = useRoute()
  const propertyRowElements = new Map<string, HTMLElement>()
  const focusedPropertyKey = computed(() => notePropertyKeyFromRoute(route))
  const unresolvedPropertyKey = computed(() => {
    if (propertyKeys === undefined) {
      return
    }
    const key = focusedPropertyKey.value
    if (!key || toValue(propertyKeys).includes(key)) {
      return
    }
    return key
  })

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
    unresolvedPropertyKey,
  }
}
