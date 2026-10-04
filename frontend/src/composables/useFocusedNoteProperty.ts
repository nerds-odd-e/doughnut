import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import {
  locationKeepingQuery,
  notePropertyKeyFromRoute,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  computed,
  inject,
  nextTick,
  provide,
  ref,
  toValue,
  watch,
  type ComponentPublicInstance,
  type InjectionKey,
  type MaybeRefOrGetter,
} from "vue"
import { useRoute, useRouter } from "vue-router"

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
  const router = useRouter()
  const providedPropertyKey = inject(focusedPropertyKeyKey, undefined)
  const propertyRowElements = new Map<string, HTMLElement>()
  // Off note routes (notebook/folder readme) there is no property location.
  const offNoteRoutePropertyKey = ref<string>()
  const focusedPropertyKey = computed(() => {
    if (providedPropertyKey !== undefined) {
      return toValue(providedPropertyKey)
    }
    return noteRouteFamilyNoteId(route) === undefined
      ? offNoteRoutePropertyKey.value
      : notePropertyKeyFromRoute(route)
  })
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

  const togglePropertyPanel = (propertyKey: string) => {
    const closing = isFocusedProperty(propertyKey)
    const noteId = noteRouteFamilyNoteId(route)
    if (noteId === undefined) {
      offNoteRoutePropertyKey.value = closing ? undefined : propertyKey
      return
    }
    return router.replace(
      locationKeepingQuery(
        route,
        closing
          ? noteShowLocation(Number(noteId))
          : notePropertyLocation(Number(noteId), propertyKey)
      )
    )
  }

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
    togglePropertyPanel,
    unresolvedPropertyKey,
  }
}
