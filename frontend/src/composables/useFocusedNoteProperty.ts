import { notePropertyKeyFromRoute } from "@/routes/noteShowLocation"
import {
  computed,
  inject,
  nextTick,
  provide,
  toValue,
  watch,
  type ComponentPublicInstance,
  type InjectionKey,
  type MaybeRefOrGetter,
} from "vue"
import { useRoute } from "vue-router"

const focusedPropertyKeyKey: InjectionKey<
  MaybeRefOrGetter<string | undefined>
> = Symbol("focusedPropertyKey")

export function provideFocusedPropertyKey(
  propertyKey: MaybeRefOrGetter<string | undefined>
) {
  provide(focusedPropertyKeyKey, propertyKey)
}

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
  const providedPropertyKey = inject(focusedPropertyKeyKey, undefined)
  const propertyRowElements = new Map<string, HTMLElement>()
  const focusedPropertyKey = computed(() =>
    providedPropertyKey === undefined
      ? notePropertyKeyFromRoute(route)
      : toValue(providedPropertyKey)
  )
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
